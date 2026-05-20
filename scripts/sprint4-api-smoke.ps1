$ErrorActionPreference = "Stop"

$BaseUrl = "http://localhost:8080/ta-recruitment-system/api/v1"

function Invoke-JsonApi {
    param(
        [Parameter(Mandatory=$true)][string]$Method,
        [Parameter(Mandatory=$true)][string]$Path,
        [object]$Body = $null,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session = $null
    )
    $options = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        ContentType = "application/json"
        TimeoutSec = 60
    }
    if ($Session) { $options.WebSession = $Session }
    if ($null -ne $Body) { $options.Body = ($Body | ConvertTo-Json -Depth 8) }
    Invoke-RestMethod @options
}

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw "Assertion failed: $Message" }
}

function New-LoginSession {
    param([string]$Email, [string]$Role)
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $login = Invoke-JsonApi -Method POST -Path "/auth/login" -Body @{ email=$Email; password="Password123!"; role=$Role } -Session $session
    Assert-True $login.success "Login failed for $Email"
    return $session
}

Write-Host "[1/8] Checking unauthenticated rejection"
try {
    Invoke-JsonApi -Method GET -Path "/ta/profile" | Out-Null
    throw "Expected unauthenticated request to fail."
} catch {
    if ($_.ErrorDetails.Message -notmatch "AUTH_NOT_LOGIN") { throw }
}

Write-Host "[2/8] TA session and profile"
$ta = New-LoginSession -Email "james@school.edu" -Role "ta"
$profile = Invoke-JsonApi -Method GET -Path "/ta/profile" -Session $ta
Assert-True $profile.success "TA profile should succeed"
Assert-True ($profile.data.profile.userId -ne $null) "TA profile should include userId"

Write-Host "[3/8] TA jobs and AI recommendations"
$jobs = Invoke-JsonApi -Method GET -Path "/ta/jobs?page=1&size=5" -Session $ta
Assert-True $jobs.success "TA job list should succeed"
$taAi = Invoke-JsonApi -Method POST -Path "/ai/ta/job-recommendations" -Body @{} -Session $ta
Assert-True $taAi.success "TA AI recommendation should succeed"
Assert-True ($taAi.data.modelView -ne $null) "TA AI should return modelView"

Write-Host "[4/8] CV file endpoint"
$tempCv = Join-Path $env:TEMP "tars-sprint4-cv-check.pdf"
$tempHeaders = Join-Path $env:TEMP "tars-sprint4-cv-headers.txt"
$cvPath = [string]$profile.data.profile.cvPath
Assert-True (-not [string]::IsNullOrWhiteSpace($cvPath)) "TA profile should include a CV path"
$cvFileName = Split-Path -Leaf ($cvPath -replace "\\", "/")
Assert-True (-not [string]::IsNullOrWhiteSpace($cvFileName)) "TA CV path should include a file name"
$sessionCookie = $ta.Cookies.GetCookies([Uri]$BaseUrl)["JSESSIONID"].Value
& curl.exe -sS -D $tempHeaders -o $tempCv -H "Cookie: JSESSIONID=$sessionCookie" "$BaseUrl/files/cv/$cvFileName"
Assert-True ($LASTEXITCODE -eq 0) "curl should download the CV file"
$headersText = Get-Content -Raw $tempHeaders
Assert-True ($headersText -match "200") "CV endpoint should return 200"
Assert-True ($headersText -match "application/pdf") "CV endpoint should return PDF"
Assert-True ((Test-Path $tempCv) -and ((Get-Item $tempCv).Length -gt 0)) "CV download should create a non-empty PDF"

Write-Host "[5/8] MO session and candidate AI"
$mo = New-LoginSession -Email "kevin.zhao@school.edu" -Role "mo"
$moJobs = Invoke-JsonApi -Method GET -Path "/mo/jobs" -Session $mo
Assert-True $moJobs.success "MO jobs should succeed"
$moApplicants = Invoke-JsonApi -Method GET -Path "/mo/applicants" -Session $mo
Assert-True (($moApplicants.data.applicants | Measure-Object).Count -gt 0) "MO should have at least one applicant in demo data"
$ownedApplicationId = $moApplicants.data.applicants[0].applicationId
$moAi = Invoke-JsonApi -Method POST -Path "/ai/mo/candidate-summary" -Body @{ applicationId=$ownedApplicationId } -Session $mo
Assert-True $moAi.success "MO AI summary should succeed"
Assert-True ($moAi.data.modelView -ne $null) "MO AI should return modelView"

Write-Host "[6/8] Admin session and workload AI"
$admin = New-LoginSession -Email "admin.chen@school.edu" -Role "admin"
$users = Invoke-JsonApi -Method GET -Path "/admin/users?page=1&size=10" -Session $admin
Assert-True $users.success "Admin users should succeed"
$adminAi = Invoke-JsonApi -Method POST -Path "/ai/admin/risk-analysis" -Body @{ riskLevel="" } -Session $admin
Assert-True $adminAi.success "Admin AI should succeed"
Assert-True ($adminAi.data.modelView -ne $null) "Admin AI should return modelView"

Write-Host "[7/8] Wrong role rejection"
try {
    Invoke-JsonApi -Method GET -Path "/admin/users?page=1&size=5" -Session $ta | Out-Null
    throw "Expected TA access to admin API to fail."
} catch {
    if ($_.ErrorDetails.Message -notmatch "AUTH_FORBIDDEN_ROLE") { throw }
}

Write-Host "[8/8] API smoke completed successfully"
