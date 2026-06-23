param(
    # 1차 주소 매칭에서 확정된 학교와 제외된 학교 파일을 함께 사용한다.
    [string]$BaseImportPath = "data\schools_for_import_from_national.csv",
    [string]$RejectedPath = "data\schools_rejected_from_national.csv",
    [string]$OutputPath = "data\schools_for_import_national_geocoded.csv",
    [string]$RecoveryAuditPath = "data\schools_geocoding_recovery_audit.csv",
    [string]$UnresolvedPath = "data\schools_rejected_after_geocoding.csv",
    [string]$DongCodesSqlPath = "sql\load_dongcodes.sql",
    [string]$SecretPath = "backend\src\main\resources\application-secret.properties",
    [int]$RequestDelayMilliseconds = 120
)

$ErrorActionPreference = 'Stop'

<#
    주소만으로 법정동을 확정하지 못했던 전국 학교 데이터를 카카오 좌표 역지오코딩으로 복구한다.

    [중요한 안전 규칙]
    - coord2regioncode 응답에서 region_type=B(법정동)인 결과만 사용한다.
    - 받은 법정동 코드가 프로젝트 dongcodes에 없으면 적재하지 않는다.
    - API 오류와 좌표 오류도 억지로 추정하지 않고 unresolved CSV에 남긴다.
    - 1차 주소 매칭 성공본은 그대로 보존하고, 역지오코딩 성공본만 합쳐 새 파일을 만든다.

    최종 OutputPath는 schools 테이블의 LOAD DATA 컬럼 순서와 정확히 일치한다.
#>

function Write-Utf8Csv([object[]]$Rows, [string]$Path) {
    $directory = Split-Path -Parent $Path
    if ($directory) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }
    $fullPath = Join-Path (Resolve-Path -LiteralPath $directory).Path (Split-Path -Leaf $Path)
    $lines = @($Rows | ConvertTo-Csv -NoTypeInformation)
    [System.IO.File]::WriteAllLines($fullPath, $lines, [System.Text.UTF8Encoding]::new($false))
}

function Get-KakaoRestApiKey([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Secret file was not found: $Path"
    }

    # 키는 콘솔에 출력하지 않는다. 등호 뒤의 값만 메모리에 읽어 HTTP Authorization 헤더에 사용한다.
    $line = Get-Content -LiteralPath $Path | Where-Object {
        $_ -match '^\s*KAKAO_REST_API_KEY\s*='
    } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($line)) {
        throw "KAKAO_REST_API_KEY was not found in $Path"
    }

    $key = $line.Substring($line.IndexOf('=') + 1).Trim()
    if ([string]::IsNullOrWhiteSpace($key)) {
        throw 'KAKAO_REST_API_KEY is empty'
    }
    return $key
}

function Get-KakaoLegalDongCode([double]$Latitude, [double]$Longitude, [string]$ApiKey) {
    # Kakao Local API에서 x=경도, y=위도이며 WGS84 좌표계를 명시한다.
    # PowerShell의 줄바꿈 결합 우선순위 문제를 피하기 위해 좌표 문자열을 먼저 분리한다.
    $longitudeText = [string]::Format([Globalization.CultureInfo]::InvariantCulture, '{0:R}', $Longitude)
    $latitudeText = [string]::Format([Globalization.CultureInfo]::InvariantCulture, '{0:R}', $Latitude)
    $encodedLongitude = [uri]::EscapeDataString($longitudeText)
    $encodedLatitude = [uri]::EscapeDataString($latitudeText)
    $uri = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x=$encodedLongitude&y=$encodedLatitude&input_coord=WGS84&output_coord=WGS84"

    try {
        $response = Invoke-RestMethod -Method Get -Uri $uri -Headers @{ Authorization = "KakaoAK $ApiKey" } -TimeoutSec 30
        # H는 행정동, B는 법정동이다. schools.dong_code에는 B 코드만 저장한다.
        $legalDong = @($response.documents | Where-Object { $_.region_type -eq 'B' }) | Select-Object -First 1
        if ($null -eq $legalDong -or [string]::IsNullOrWhiteSpace($legalDong.code)) {
            return [pscustomobject]@{ DongCode = $null; Result = 'NO_LEGAL_DONG_RESULT' }
        }
        return [pscustomobject]@{ DongCode = [string]$legalDong.code; Result = 'RESOLVED' }
    } catch {
        # 네트워크, 인증, 호출 한도 등 실패 원인을 결과 파일에서 추적할 수 있게 간단히 남긴다.
        return [pscustomobject]@{ DongCode = $null; Result = "KAKAO_API_ERROR:$($_.Exception.Message)" }
    }
}

if (-not (Test-Path -LiteralPath $BaseImportPath)) { throw "Base import file was not found: $BaseImportPath" }
if (-not (Test-Path -LiteralPath $RejectedPath)) { throw "Rejected file was not found: $RejectedPath" }
if (-not (Test-Path -LiteralPath $DongCodesSqlPath)) { throw "Dong code SQL file was not found: $DongCodesSqlPath" }

$apiKey = Get-KakaoRestApiKey $SecretPath

# 프로젝트에서 허용하는 법정동 코드만 Set에 담아 API 결과를 마지막으로 검증한다.
$projectDongCodes = [System.Collections.Generic.HashSet[string]]::new()
[regex]::Matches((Get-Content -LiteralPath $DongCodesSqlPath -Raw -Encoding UTF8), "\('([0-9]{10})'") |
    ForEach-Object { [void]$projectDongCodes.Add($_.Groups[1].Value) }

$baseRows = @(Import-Csv -LiteralPath $BaseImportPath -Encoding UTF8)
$rejectedRows = @(Import-Csv -LiteralPath $RejectedPath -Encoding UTF8)
$recoveredRows = [System.Collections.Generic.List[object]]::new()
$unresolvedRows = [System.Collections.Generic.List[object]]::new()
$auditRows = [System.Collections.Generic.List[object]]::new()

for ($index = 0; $index -lt $rejectedRows.Count; $index++) {
    $row = $rejectedRows[$index]
    $latitude = 0.0
    $longitude = 0.0
    $coordinatesValid =
        [double]::TryParse($row.latitude, [Globalization.NumberStyles]::Float,
            [Globalization.CultureInfo]::InvariantCulture, [ref]$latitude) -and
        [double]::TryParse($row.longitude, [Globalization.NumberStyles]::Float,
            [Globalization.CultureInfo]::InvariantCulture, [ref]$longitude)

    if (-not $coordinatesValid) {
        $result = [pscustomobject]@{ DongCode = $null; Result = 'INVALID_COORDINATES' }
    } else {
        $result = Get-KakaoLegalDongCode $latitude $longitude $apiKey
    }

    $finalReason = $result.Result
    if ($result.Result -eq 'RESOLVED' -and -not $projectDongCodes.Contains($result.DongCode)) {
        $finalReason = 'REVERSE_DONG_CODE_NOT_IN_PROJECT'
    }

    $auditRows.Add([pscustomobject][ordered]@{
        school_id = $row.school_id
        school_name = $row.school_name
        latitude = $row.latitude
        longitude = $row.longitude
        original_rejection_reason = $row.rejection_reason
        reverse_dong_code = $result.DongCode
        geocoding_result = $finalReason
    })

    if ($finalReason -eq 'RESOLVED') {
        $recoveredRows.Add([pscustomobject][ordered]@{
            school_id = $row.school_id
            school_name = $row.school_name
            dong_code = $result.DongCode
            school_type = $row.school_type
            establishment = $row.establishment
            facility_type = $row.facility_type
            operation_status = $row.operation_status
            latitude = $row.latitude
            longitude = $row.longitude
        })
    } else {
        # 기존 제외 정보와 역지오코딩 실패 원인을 함께 보존한다.
        $unresolvedRows.Add([pscustomobject][ordered]@{
            school_id = $row.school_id
            school_name = $row.school_name
            jibun_address = $row.jibun_address
            road_address = $row.road_address
            latitude = $row.latitude
            longitude = $row.longitude
            original_rejection_reason = $row.rejection_reason
            geocoding_rejection_reason = $finalReason
        })
    }

    # 공개 API 호출량을 안정적으로 유지하고, 긴 작업의 진행률도 확인할 수 있게 한다.
    if (($index + 1) % 100 -eq 0 -or $index -eq $rejectedRows.Count - 1) {
        Write-Host "Geocoding progress: $($index + 1)/$($rejectedRows.Count), recovered=$($recoveredRows.Count), unresolved=$($unresolvedRows.Count)"
    }
    Start-Sleep -Milliseconds $RequestDelayMilliseconds
}

# 학교 ID는 PK이므로 혹시 모를 중복을 방지해 1차 매칭본을 우선 보존한다.
$seenSchoolIds = [System.Collections.Generic.HashSet[string]]::new()
$finalRows = [System.Collections.Generic.List[object]]::new()
foreach ($row in @($baseRows) + @($recoveredRows)) {
    if ($seenSchoolIds.Add([string]$row.school_id)) {
        $finalRows.Add($row)
    }
}

Write-Utf8Csv $finalRows $OutputPath
Write-Utf8Csv $auditRows $RecoveryAuditPath
Write-Utf8Csv $unresolvedRows $UnresolvedPath

Write-Host "Base rows:       $($baseRows.Count)"
Write-Host "Recovered rows:  $($recoveredRows.Count)"
Write-Host "Final rows:      $($finalRows.Count) -> $OutputPath"
Write-Host "Still unresolved:$($unresolvedRows.Count) -> $UnresolvedPath"
