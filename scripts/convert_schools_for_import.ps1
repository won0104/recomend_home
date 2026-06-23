param(
    [string]$InputPath = "C:\Users\SSAFY\Downloads\schools.csv",
    [string]$OutputPath = "data\schools_for_import.csv",
    [string]$RejectedPath = "data\schools_rejected.csv",
    [string]$DongCodesSqlPath = "sql\load_dongcodes.sql"
)

$ErrorActionPreference = 'Stop'

function Write-Utf8Csv([object[]]$Rows, [string]$Path) {
    $directory = Split-Path -Parent $Path
    if ($directory) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }

    $lines = @($Rows | ConvertTo-Csv -NoTypeInformation)
    [System.IO.File]::WriteAllLines(
        (Resolve-Path -LiteralPath $directory).Path + [System.IO.Path]::DirectorySeparatorChar + (Split-Path -Leaf $Path),
        $lines,
        [System.Text.UTF8Encoding]::new($false)
    )
}

if (-not (Test-Path -LiteralPath $InputPath)) {
    throw "Input file was not found: $InputPath"
}
if (-not (Test-Path -LiteralPath $DongCodesSqlPath)) {
    throw "Dong code SQL file was not found: $DongCodesSqlPath"
}

$projectDongCodes = [System.Collections.Generic.HashSet[string]]::new()
[regex]::Matches((Get-Content -LiteralPath $DongCodesSqlPath -Raw), "\('([0-9]{10})'") |
    ForEach-Object { [void]$projectDongCodes.Add($_.Groups[1].Value) }

$validRows = [System.Collections.Generic.List[object]]::new()
$rejectedRows = [System.Collections.Generic.List[object]]::new()

foreach ($row in (Import-Csv -LiteralPath $InputPath -Encoding UTF8)) {
    $dongCode = if ($row.lawd_cd -match '^([0-9]{10})(?:\.0)?$') { $Matches[1] } else { $null }
    $missingField = @('school_id', 'school_name', 'school_type', 'establishment', 'facility_type', 'operation_status', 'latitude', 'longitude') |
        Where-Object { [string]::IsNullOrWhiteSpace($row.$_) }

    $latitude = 0.0
    $longitude = 0.0
    $hasCoordinates =
        [double]::TryParse($row.latitude, [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref]$latitude) -and
        [double]::TryParse($row.longitude, [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref]$longitude) -and
        $latitude -ge -90 -and $latitude -le 90 -and $longitude -ge -180 -and $longitude -le 180

    $reason = if (-not $dongCode) {
        'INVALID_LAWD_CODE'
    } elseif (-not $projectDongCodes.Contains($dongCode)) {
        'UNKNOWN_DONG_CODE'
    } elseif ($missingField) {
        "MISSING_REQUIRED_FIELD:$($missingField -join '|')"
    } elseif (-not $hasCoordinates) {
        'INVALID_COORDINATES'
    } else {
        $null
    }

    if ($reason) {
        $rejectedRows.Add([pscustomobject][ordered]@{
            school_id = $row.school_id
            school_name = $row.school_name
            sido = $row.sido
            sigungu = $row.sigungu
            lawd_cd = $row.lawd_cd
            dong_name = $row.dong_name
            school_type = $row.school_type
            establishment = $row.establishment
            facility_type = $row.facility_type
            operation_status = $row.operation_status
            latitude = $row.latitude
            longitude = $row.longitude
            rejection_reason = $reason
        })
        continue
    }

    $validRows.Add([pscustomobject][ordered]@{
        school_id = $row.school_id
        school_name = $row.school_name
        dong_code = $dongCode
        school_type = $row.school_type
        establishment = $row.establishment
        facility_type = $row.facility_type
        operation_status = $row.operation_status
        latitude = $row.latitude
        longitude = $row.longitude
    })
}

Write-Utf8Csv $validRows $OutputPath
Write-Utf8Csv $rejectedRows $RejectedPath

Write-Host "Converted: $($validRows.Count) rows -> $OutputPath"
Write-Host "Rejected:  $($rejectedRows.Count) rows -> $RejectedPath"
