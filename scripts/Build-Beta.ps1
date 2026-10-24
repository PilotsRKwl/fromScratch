[CmdletBinding()]
param(
    [ValidatePattern('^\d+\.\d+\.\d+-beta\.\d+$')][string]$PackageVersion = '',
    [ValidatePattern('^v\d+\.\d+\.\d+-[a-z0-9-]+$')][string]$ReleaseVersion = '',
    [switch]$StageOnly,
    [switch]$LocalTestCandidate,
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
Push-Location $projectRoot
try {
    $arguments = @($(if ($StageOnly) {'betaStage'} else {'betaChecksum'}), '--console=plain')
    if ($PackageVersion) { $arguments += '-PbetaPackageVersion=' + $PackageVersion }
    if ($ReleaseVersion) { $arguments += '-PbetaReleaseVersion=' + $ReleaseVersion }
    if ($LocalTestCandidate) { $arguments += '-PbetaLocalTestCandidate=true' }
    if ($Offline) { $arguments += '--offline' }
    & (Join-Path $projectRoot 'gradlew.bat') @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Beta packaging failed. No candidate should be distributed.' }
    Write-Host $(if ($StageOnly) {'Verified beta files staged under build/beta/stage.'} else {'Candidate ZIP and SHA-256 are in build/distributions.'})
    Write-Host 'This did not publish or install anything.'
} finally { Pop-Location }
