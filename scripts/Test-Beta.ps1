[CmdletBinding()]
param([string]$PackageDirectory = '')
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ([Environment]::OSVersion.Platform -ne 'Win32NT') { throw 'Installer integration tests require Windows.' }
$project = Split-Path $PSScriptRoot
if (-not $PackageDirectory) { $PackageDirectory = Join-Path $project 'build/beta/stage' }
$sandbox = Join-Path $project ('build/beta-tests/' + [Guid]::NewGuid().ToString('N') + ' test with spaces')
[void](New-Item -ItemType Directory -Path $sandbox -Force)
. (Join-Path $PackageDirectory 'Common.ps1')
$passed = 0
function Assert-Beta([bool]$Condition,[string]$Message) { if (-not $Condition) { throw $Message } }
function Reject-Beta([scriptblock]$Action) { $rejected=$false;try { & $Action | Out-Null } catch { $rejected=$true };Assert-Beta $rejected 'Unsafe operation unexpectedly succeeded.' }
function Pass-Beta([string]$Name) { $script:passed++;Write-Host ('PASS ' + $Name) }

foreach ($file in Get-ChildItem -LiteralPath (Join-Path $project 'beta/windows'),$PSScriptRoot -Filter '*.ps1') {
    $tokens=$null;$errors=$null;[void][Management.Automation.Language.Parser]::ParseFile($file.FullName,[ref]$tokens,[ref]$errors)
    Assert-Beta (@($errors).Count -eq 0) ('PowerShell syntax error: ' + $file.Name)
}
Pass-Beta 'PowerShell 5.1 script parsing'
$manifest = Read-BetaPackage $PackageDirectory
Assert-Beta ($manifest.runeLiteVersion -eq '1.13.4') 'Unexpected RuneLite version.'
Assert-Beta (-not @($manifest.files | Where-Object {$_.path -match '(?i)credentials|jagex-accounts|\.fsp$|\.env|\.log$|junit|test-fixtures'}).Count) 'Private/test files entered the distribution.'
Pass-Beta 'Manifest hashes, allowlist and pinned RuneLite'

Add-Type -AssemblyName System.IO.Compression.FileSystem
$plugin = [IO.Compression.ZipFile]::OpenRead((Get-BetaPath $PackageDirectory $manifest.pluginJar))
try { Assert-Beta (-not @($plugin.Entries | Where-Object {$_.FullName -match '(^org/junit/|Test.class$|^com/fromscratch/(beta|preview)/|META-INF/services/net.runelite.client.plugins.Plugin)'}).Count) 'Tests/bootstrap/previews/service registration entered the plugin JAR.';Assert-Beta ($null -ne $plugin.GetEntry('com/fromscratch/icon.png')) 'Missing plugin resource.' } finally {$plugin.Dispose()}
$client=[IO.Compression.ZipFile]::OpenRead((Get-BetaPath $PackageDirectory 'lib/client-1.13.4.jar'))
try { Assert-Beta ($null -ne $client.GetEntry('net/runelite/client/plugins/jagexauth/JagexAuthPlugin.class')) 'Official Jagex Accounts panel missing.' } finally {$client.Dispose()}
Pass-Beta 'Production JAR/resource isolation and official authentication panel'

$bad = Join-Path $sandbox 'tampered package';[void](New-Item -ItemType Directory -Path $bad)
Copy-Item -LiteralPath (Join-Path $PackageDirectory 'package.json') -Destination $bad
$fake = Get-Content -LiteralPath (Join-Path $bad 'package.json') -Raw | ConvertFrom-Json
$fake.files[0].path='../escape';$fake | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $bad 'package.json') -Encoding UTF8
Reject-Beta { Read-BetaPackage $bad };Reject-Beta { Get-BetaPath $PackageDirectory '../outside' };Reject-Beta { Get-BetaPath $PackageDirectory 'lib\outside.jar' }
Pass-Beta 'Traversal and malformed package rejection'

$installRoot=Join-Path $sandbox 'Local AppData/From Scratch Beta';$desktop=Join-Path $sandbox 'Desktop'
$sentinel=Join-Path $sandbox '.runelite/sideloaded-plugins/unrelated.txt';[void](New-Item -ItemType Directory -Path (Split-Path $sentinel) -Force);'untouched' | Set-Content -LiteralPath $sentinel
$installed = & (Join-Path $PackageDirectory 'Install.ps1') -InstallRoot $installRoot -DesktopDirectory $desktop
Assert-Beta (Test-Path -LiteralPath $installed.Shortcut) 'Desktop shortcut missing.'
Assert-Beta (Test-BetaOwnedShortcut $installed.Shortcut $installRoot) 'Shortcut target not owned.'
Pass-Beta 'Isolated installation and real Windows shortcut with spaces'
$again=& (Join-Path $PackageDirectory 'Install.ps1') -InstallRoot $installRoot -DesktopDirectory $desktop
Assert-Beta ($again.Directory -eq $installed.Directory) 'Repeated installation duplicated the program version.'
$checked=& (Join-Path $installed.Directory 'Launch.ps1') -CheckOnly
Assert-Beta ($checked.JavaMajor -eq 11) 'Java prerequisite check failed.'
Pass-Beta 'Idempotent install and noninteractive launch checks'

$classPath=Join-Path $installed.Directory 'lib/*';$java=Find-BetaJava
$previousAction=$ErrorActionPreference
try {$ErrorActionPreference='Continue';$help=(& $java '-ea' ('-Duser.home='+(Join-Path $sandbox 'empty client home')) '-cp' $classPath $manifest.mainClass '--help' 2>&1 | Out-String);$code=$LASTEXITCODE}finally{$ErrorActionPreference=$previousAction}
Assert-Beta ($code -eq 0 -and $help -match 'developer-mode') 'Actual packaged Java help launch failed.'
Pass-Beta 'Packaged bootstrap launched outside IntelliJ with isolated empty home (help only)'

$jar=Get-BetaPath $installed.Directory $manifest.pluginJar
$original=[IO.File]::ReadAllBytes($jar);[IO.File]::AppendAllText($jar,'tampered')
Reject-Beta { & (Join-Path $installed.Directory 'Launch.ps1') -CheckOnly };[IO.File]::WriteAllBytes($jar,$original)
$extraJar=Join-Path $installed.Directory 'lib/unexpected.jar';[IO.File]::WriteAllBytes($extraJar,@(0,1,2))
Reject-Beta { & (Join-Path $installed.Directory 'Launch.ps1') -CheckOnly };Remove-Item -LiteralPath $extraJar
Pass-Beta 'Tampered or unexpected classpath JAR blocks launching'

$update=Join-Path $sandbox 'new package';Copy-Item -LiteralPath $PackageDirectory -Destination $update -Recurse
$updatedManifest=Get-Content -LiteralPath (Join-Path $update 'package.json') -Raw | ConvertFrom-Json
$updatedManifest.version='0.10.0-beta.2';$updatedManifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $update 'package.json') -Encoding UTF8
$new=& (Join-Path $update 'Install.ps1') -InstallRoot $installRoot -DesktopDirectory $desktop
Assert-Beta ($new.Directory -ne $installed.Directory) 'Update overwrote the old version.'
& (Join-Path $installed.Directory 'Uninstall.ps1') -Confirm:$false
Assert-Beta (Test-Path -LiteralPath $new.Shortcut) 'Old-version uninstall removed the new shortcut.'
Pass-Beta 'Versioned update and old-version uninstall preserve current shortcut'

$extra=Join-Path $new.Directory 'owner-note.txt';'keep me' | Set-Content -LiteralPath $extra
& (Join-Path $new.Directory 'Uninstall.ps1') -Confirm:$false
Assert-Beta (Test-Path -LiteralPath $extra) 'Uninstall deleted an unrelated file.'
Assert-Beta ((Get-Content -LiteralPath $sentinel -Raw).Trim() -eq 'untouched') 'Unrelated RuneLite fixture changed.'
Assert-Beta (-not (Test-Path -LiteralPath $new.Shortcut)) 'Current-version shortcut was not removed.'
Pass-Beta 'Uninstall preserves unknown files and unrelated RuneLite/progression fixture'
Write-Host ('Beta tests: ' + $passed + ' passed. No real desktop, RuneLite profile, credentials or progression were used. Fixtures retained under build/beta-tests.')
