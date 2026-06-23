param(
    # 전국 초·중등학교 위치 표준데이터는 일반적으로 CP949 인코딩으로 배포된다.
    [string]$InputPath = "C:\Users\SSAFY\Downloads\전국초중등학교위치표준데이터.csv",
    [string]$OutputPath = "data\schools_for_import_from_national.csv",
    [string]$RejectedPath = "data\schools_rejected_from_national.csv",
    [string]$DongCodesSqlPath = "sql\load_dongcodes.sql"
)

$ErrorActionPreference = 'Stop'

<#
    전국 학교 위치 표준데이터를 schools 테이블의 적재 형식으로 바꾸는 스크립트다.

    [변환 흐름]
    1. 원본 CSV의 한글 컬럼을 schools 스키마 컬럼으로 바꾼다.
    2. 원본에는 법정동 코드가 없으므로, 지번 주소를 dongcodes의 시·군·구·동 이름과 대조한다.
    3. 법정동 코드가 단 하나로 확정되는 행만 적재 CSV에 넣는다.
    4. 주소가 없거나, 둘 이상의 코드가 일치하거나, 좌표가 잘못된 행은 제외 CSV에 사유와 함께 남긴다.

    주소 문자열만으로 법정동 코드를 추정할 때 애매한 행을 임의로 고르면 추천 점수 자체가 틀어질 수 있다.
    따라서 이 스크립트는 "많이 넣는 것"보다 "확실한 코드만 넣는 것"을 우선한다.
#>

function Write-Utf8Csv([object[]]$Rows, [string]$Path) {
    $directory = Split-Path -Parent $Path
    if ($directory) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }

    # MySQL LOAD DATA와 Excel 모두에서 한글이 깨지지 않도록 UTF-8 BOM 없는 CSV로 저장한다.
    $lines = @($Rows | ConvertTo-Csv -NoTypeInformation)
    $fullPath = Join-Path (Resolve-Path -LiteralPath $directory).Path (Split-Path -Leaf $Path)
    [System.IO.File]::WriteAllLines($fullPath, $lines, [System.Text.UTF8Encoding]::new($false))
}

function Normalize-LocationText([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ''
    }

    # 주소의 괄호 표기와 연속 공백은 행정구역 이름 비교를 방해하므로 제거한다.
    return (($Value -replace '\([^)]*\)', ' ') -replace '\s+', ' ').Trim()
}

function Test-ContainsLocationName([string]$Address, [string]$LocationName) {
    if ([string]::IsNullOrWhiteSpace($LocationName)) {
        return $true
    }
    # Windows PowerShell 5의 String.Contains는 StringComparison 인자를 지원하지 않으므로
    # IndexOf를 사용해 대소문자 변환 없이 포함 여부를 확인한다.
    return $Address.IndexOf($LocationName, [System.StringComparison]::Ordinal) -ge 0
}

function Get-DongCodeMatch([string]$JibunAddress, [string]$RoadAddress, [hashtable]$DongRowsByName) {
    # 지번 주소에는 보통 법정동명이 직접 들어가므로 도로명 주소보다 먼저 사용한다.
    # 지번 주소가 비어 있을 때만 도로명 주소로 보조 매칭한다.
    $address = Normalize-LocationText $JibunAddress
    if ([string]::IsNullOrWhiteSpace($address)) {
        $address = Normalize-LocationText $RoadAddress
    }

    if ([string]::IsNullOrWhiteSpace($address)) {
        return [pscustomobject]@{ DongCode = $null; Reason = 'MISSING_ADDRESS' }
    }

    # 주소를 공백·쉼표 기준 토큰으로 나눠 "개포동", "성수동1가"처럼 직접 나온 동 이름만 찾는다.
    # 매 학교마다 전국 법정동 전체를 순회하지 않기 위해, 아래 인덱스에서 후보만 즉시 꺼낸다.
    $candidateRows = [System.Collections.Generic.List[object]]::new()
    $addedDongCodes = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($token in ($address -split '[\s,]+')) {
        if ($DongRowsByName.ContainsKey($token)) {
            foreach ($dongRow in $DongRowsByName[$token]) {
                # 도로명에 같은 토큰이 반복되거나, 한 코드가 여러 보조 키로 들어가도 한 번만 비교한다.
                if ($addedDongCodes.Add($dongRow.dongCode)) {
                    $candidateRows.Add($dongRow)
                }
            }
        }
    }

    # 동 이름과 구·군 이름이 모두 주소에 들어가는 코드만 우선 후보로 만든다.
    # dong_name이 NULL인 시도/시군구 집계 코드는 학교 테이블의 FK 대상으로 사용할 수 없으므로 제외한다.
    $matches = @($candidateRows | Where-Object {
        # named parameter를 사용해 PowerShell 파이프라인의 $_ 속성이 함수 인자에서 누락되지 않게 한다.
        (Test-ContainsLocationName -Address $address -LocationName ([string]($_.sidoName))) -and
        (Test-ContainsLocationName -Address $address -LocationName ([string]($_.gugunName))) -and
        (Test-ContainsLocationName -Address $address -LocationName ([string]($_.dongName)))
    })

    if ($matches.Count -eq 1) {
        return [pscustomobject]@{ DongCode = $matches[0].dongCode; Reason = $null }
    }
    if ($matches.Count -gt 1) {
        # "교산리"와 "함양읍 교산리"가 함께 매칭되면 더 긴 쪽이 더 구체적인 법정동 표현이다.
        # 가장 구체적인 동 이름이 하나뿐일 때만 자동 확정하고, 같은 길이의 후보가 남으면 모호 처리한다.
        $maxDongNameLength = ($matches | ForEach-Object { $_.dongName.Length } | Measure-Object -Maximum).Maximum
        $mostSpecificMatches = @($matches | Where-Object { $_.dongName.Length -eq $maxDongNameLength })
        if ($mostSpecificMatches.Count -eq 1) {
            return [pscustomobject]@{ DongCode = $mostSpecificMatches[0].dongCode; Reason = $null }
        }
        return [pscustomobject]@{ DongCode = $null; Reason = 'AMBIGUOUS_DONG_CODE' }
    }

    # 시군구 표기가 없는 일부 주소를 위해 동 이름만 매칭하는 보조 단계다.
    # 이 단계도 코드가 정확히 하나일 때만 허용한다.
    $dongOnlyMatches = @($candidateRows | Where-Object {
        Test-ContainsLocationName -Address $address -LocationName ([string]($_.dongName))
    })
    if ($dongOnlyMatches.Count -eq 1) {
        return [pscustomobject]@{ DongCode = $dongOnlyMatches[0].dongCode; Reason = $null }
    }
    if ($dongOnlyMatches.Count -gt 1) {
        $maxDongNameLength = ($dongOnlyMatches | ForEach-Object { $_.dongName.Length } | Measure-Object -Maximum).Maximum
        $mostSpecificMatches = @($dongOnlyMatches | Where-Object { $_.dongName.Length -eq $maxDongNameLength })
        if ($mostSpecificMatches.Count -eq 1) {
            return [pscustomobject]@{ DongCode = $mostSpecificMatches[0].dongCode; Reason = $null }
        }
        return [pscustomobject]@{ DongCode = $null; Reason = 'AMBIGUOUS_DONG_CODE' }
    }

    return [pscustomobject]@{ DongCode = $null; Reason = 'UNMATCHED_DONG_CODE' }
}

if (-not (Test-Path -LiteralPath $InputPath)) {
    throw "Input file was not found: $InputPath"
}
if (-not (Test-Path -LiteralPath $DongCodesSqlPath)) {
    throw "Dong code SQL file was not found: $DongCodesSqlPath"
}

# load_dongcodes.sql의 INSERT 값에서 법정동 코드와 시·군·구·동 이름을 읽는다.
# SQL 문자열 안에 작은따옴표가 없는 행정구역명만 대상으로 하므로 이 정규식으로 충분하다.
$dongRows = [System.Collections.Generic.List[object]]::new()
$dongSql = Get-Content -LiteralPath $DongCodesSqlPath -Raw -Encoding UTF8
$pattern = "\('(?<dongCode>\d{10})','(?<sidoName>[^']*)',(?<gugunValue>NULL|'[^']*'),(?<dongValue>NULL|'[^']*')\)"
foreach ($match in [regex]::Matches($dongSql, $pattern)) {
    $gugunValue = $match.Groups['gugunValue'].Value
    $dongValue = $match.Groups['dongValue'].Value
    $dongName = if ($dongValue -eq 'NULL') { $null } else { $dongValue.Trim("'") }
    if ([string]::IsNullOrWhiteSpace($dongName)) {
        continue
    }

    $dongRows.Add([pscustomobject]@{
        dongCode = $match.Groups['dongCode'].Value
        sidoName = $match.Groups['sidoName'].Value
        gugunName = if ($gugunValue -eq 'NULL') { $null } else { $gugunValue.Trim("'") }
        dongName = $dongName
    })
}

# dong_name을 키로 하는 인덱스다. 같은 동 이름이 전국에 여러 번 있을 수 있으므로 값은 항상 목록이다.
# "조천읍 신촌리"처럼 공백이 있는 dong_name은 마지막 행정구역 단위인 "신촌리"도 보조 키로 넣는다.
# 주소는 보통 "조천읍 신촌리"를 두 토큰으로 나누므로, 이 보조 키가 없으면 정확한 행도 놓칠 수 있다.
$dongRowsByName = @{}
$dongRowsByCode = @{}
foreach ($dongRow in $dongRows) {
    $dongRowsByCode[$dongRow.dongCode] = $dongRow
    $indexNames = [System.Collections.Generic.List[string]]::new()
    $indexNames.Add($dongRow.dongName)
    $lastNamePart = ($dongRow.dongName -split '\s+')[-1]
    if ($lastNamePart -ne $dongRow.dongName) {
        $indexNames.Add($lastNamePart)
    }

    foreach ($indexName in $indexNames) {
        if (-not $dongRowsByName.ContainsKey($indexName)) {
            $dongRowsByName[$indexName] = [System.Collections.Generic.List[object]]::new()
        }
        $dongRowsByName[$indexName].Add($dongRow)
    }
}

$validRows = [System.Collections.Generic.List[object]]::new()
$rejectedRows = [System.Collections.Generic.List[object]]::new()

# 공공데이터포털 원본은 CP949이므로 명시적으로 읽는다. UTF-8로 읽으면 한글 헤더가 깨진다.
foreach ($row in (Import-Csv -LiteralPath $InputPath -Encoding Default)) {
    # Windows PowerShell 5는 UTF-8 BOM 없는 ps1의 한글 속성명을 잘못 해석할 수 있다.
    # 원본 표준데이터의 고정 열 순서(학교ID=0, ..., 지번주소=7, 도로명주소=8, 위도=15, 경도=16)를
    # 사용하면 스크립트 파일 인코딩과 무관하게 같은 값을 읽을 수 있다.
    $values = @($row.PSObject.Properties | ForEach-Object { $_.Value })
    $schoolId = $values[0]
    $schoolName = $values[1]
    $schoolType = $values[2]
    $establishment = $values[4]
    $facilityType = $values[5]
    $operationStatus = $values[6]
    $jibunAddress = $values[7]
    $roadAddress = $values[8]
    $rawLatitude = $values[15]
    $rawLongitude = $values[16]

    $matchResult = Get-DongCodeMatch $jibunAddress $roadAddress $dongRowsByName

    # 마지막 안전장치: 동 이름이 전국에 중복돼도 다른 시도의 코드가 들어가면 안 된다.
    # 주소 첫 토큰(예: "경상남도")과 선택된 dongcodes의 sido_name이 다르면 자동 매칭을 취소한다.
    if ($matchResult.DongCode) {
        $addressForSidoValidation = Normalize-LocationText $jibunAddress
        if ([string]::IsNullOrWhiteSpace($addressForSidoValidation)) {
            $addressForSidoValidation = Normalize-LocationText $roadAddress
        }
        $addressSido = ($addressForSidoValidation -split '\s+')[0]
        $matchedSido = $dongRowsByCode[$matchResult.DongCode].sidoName
        if ($addressSido -ne $matchedSido) {
            $matchResult = [pscustomobject]@{ DongCode = $null; Reason = 'MISMATCHED_SIDO_NAME' }
        }
    }

    $latitude = 0.0
    $longitude = 0.0
    $hasCoordinates =
        [double]::TryParse($rawLatitude, [Globalization.NumberStyles]::Float,
            [Globalization.CultureInfo]::InvariantCulture, [ref]$latitude) -and
        [double]::TryParse($rawLongitude, [Globalization.NumberStyles]::Float,
            [Globalization.CultureInfo]::InvariantCulture, [ref]$longitude) -and
        $latitude -ge -90 -and $latitude -le 90 -and
        $longitude -ge -180 -and $longitude -le 180

    $requiredFields = ([ordered]@{
        school_id = $schoolId
        school_name = $schoolName
        school_type = $schoolType
        establishment = $establishment
        facility_type = $facilityType
        operation_status = $operationStatus
    }).GetEnumerator() | Where-Object { [string]::IsNullOrWhiteSpace($_.Value) } |
        ForEach-Object { $_.Key }

    $reason = if ($matchResult.Reason) {
        $matchResult.Reason
    } elseif ($requiredFields) {
        "MISSING_REQUIRED_FIELD:$($requiredFields -join '|')"
    } elseif (-not $hasCoordinates) {
        'INVALID_COORDINATES'
    } else {
        $null
    }

    if ($reason) {
        $rejectedRows.Add([pscustomobject][ordered]@{
            school_id = $schoolId
            school_name = $schoolName
            jibun_address = $jibunAddress
            road_address = $roadAddress
            school_type = $schoolType
            establishment = $establishment
            facility_type = $facilityType
            operation_status = $operationStatus
            latitude = $rawLatitude
            longitude = $rawLongitude
            rejection_reason = $reason
        })
        continue
    }

    # 이 열 순서는 sql/schools.sql의 LOAD DATA LOCAL INFILE 컬럼 순서와 일치한다.
    $validRows.Add([pscustomobject][ordered]@{
        school_id = $schoolId
        school_name = $schoolName
        dong_code = $matchResult.DongCode
        school_type = $schoolType
        establishment = $establishment
        facility_type = $facilityType
        operation_status = $operationStatus
        latitude = $rawLatitude
        longitude = $rawLongitude
    })
}

Write-Utf8Csv $validRows $OutputPath
Write-Utf8Csv $rejectedRows $RejectedPath

Write-Host "Converted: $($validRows.Count) rows -> $OutputPath"
Write-Host "Rejected:  $($rejectedRows.Count) rows -> $RejectedPath"
