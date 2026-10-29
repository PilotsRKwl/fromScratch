[CmdletBinding()]
param([switch]$ImageOnly,[switch]$ConfigurationOnly)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$project=Split-Path $PSScriptRoot
$pin=Get-Content -LiteralPath (Join-Path $project 'beta/windows-native/toolchain.json') -Raw|ConvertFrom-Json
$passed=0
function Assert-Native([bool]$Condition,[string]$Message){if(!$Condition){throw $Message}}
function Pass-Native([string]$Name){$script:passed++;Write-Host ('PASS '+$Name)}
function Run-Native([string]$File,[string]$Arguments,[int]$Timeout=60000,[bool]$NoSystemJava=$false){
    $start=[Diagnostics.ProcessStartInfo]::new($File,$Arguments);$start.UseShellExecute=$false;$start.WindowStyle='Hidden';$start.WorkingDirectory=$env:WINDIR
    # Change only this test process's inherited environment, never persistent Windows settings.
    $savedHome=$env:JAVA_HOME;$savedPath=$env:PATH;$savedOptions=$env:JAVA_TOOL_OPTIONS
    try {
        if($NoSystemJava){$env:JAVA_HOME='C:\missing-system-java';$env:PATH=Join-Path $env:WINDIR 'System32';$env:JAVA_TOOL_OPTIONS='-an-invalid-option-that-the-wrapper-must-ignore'}
        $process=[Diagnostics.Process]::Start($start)
        try{if(!$process.WaitForExit($Timeout)){$process.Kill();throw 'Native installer/launcher test timed out.'};return $process.ExitCode}finally{$process.Dispose()}
    } finally {$env:JAVA_HOME=$savedHome;$env:PATH=$savedPath;$env:JAVA_TOOL_OPTIONS=$savedOptions}
}
function Gui-Executable([string]$File){$bytes=[IO.File]::ReadAllBytes($File);$pe=[BitConverter]::ToInt32($bytes,0x3c);return [BitConverter]::ToUInt16($bytes,$pe+24+68) -eq 2}
function Windows-Version([string]$File){$v=[Diagnostics.FileVersionInfo]::GetVersionInfo($File);return [Version]::new($v.FileMajorPart,$v.FileMinorPart,$v.FileBuildPart,$v.FilePrivatePart)}
function Latest-LauncherLog([string]$Root){return Get-ChildItem -LiteralPath (Join-Path $Root 'verification-data/FromScratchBeta/logs') -Filter '*.log'|Sort-Object LastWriteTime -Descending|Select-Object -First 1}

foreach($file in Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.ps1'){$tokens=$null;$errors=$null;[void][Management.Automation.Language.Parser]::ParseFile($file.FullName,[ref]$tokens,[ref]$errors);Assert-Native ($errors.Count -eq 0) ('Script syntax: '+$file.Name)}
Pass-Native 'PowerShell packaging syntax'
Assert-Native ($pin.format -eq 1 -and $pin.releaseVersion -cmatch '^v\d+\.\d+\.\d+(?:\.\d+)?-[a-z0-9-]+$' -and $pin.packageVersion -cmatch '^\d+\.\d+\.\d+-beta\.\d+$' -and $pin.windowsVersion -match '^\d+\.\d+\.\d+\.\d+$') 'Invalid pinned release/package/Windows version.'
$baseVersion=($pin.packageVersion -split '-')[0]
$releaseNumber=($pin.releaseVersion.Substring(1) -split '-')[0]
$releaseWindows=if(($releaseNumber -split '\.').Count -eq 3){$releaseNumber+'.0'}else{$releaseNumber}
Assert-Native ($releaseWindows -eq $pin.windowsVersion -and $pin.windowsVersion.StartsWith($baseVersion+'.')) 'Pinned release, package and Windows versions disagree.'
Assert-Native ($pin.runtime.sha256 -cmatch '^[a-f0-9]{64}$' -and $pin.inno.sha256 -cmatch '^[a-f0-9]{64}$') 'Pinned toolchain checksums are invalid.'
Assert-Native ($pin.runtime.url.StartsWith('https://github.com/adoptium/temurin11-binaries/releases/download/') -and $pin.inno.url.StartsWith('https://github.com/jrsoftware/issrc/releases/download/')) 'Pinned toolchain sources are not the official release repositories.'
Pass-Native ('Pinned configuration: '+$pin.releaseVersion+' / '+$pin.packageVersion+' / '+$pin.windowsVersion)
if($ConfigurationOnly){Write-Host 'Configuration checks only. No application was built, installed or launched.';return}
$build=Get-Content -LiteralPath (Join-Path $project 'build/native-installer/latest-build.json') -Raw|ConvertFrom-Json
Assert-Native ($build.releaseVersion -eq $pin.releaseVersion -and $build.packageVersion -eq $pin.packageVersion -and $build.runtimeVersion -eq $pin.runtime.version -and $build.runtimeSha256 -eq $pin.runtime.sha256) 'Latest build differs from the pinned current release/toolchain. Rebuild it before testing.'
$test=Join-Path $project ('build/native-installer/tests/'+[Guid]::NewGuid().ToString('N')+' paths with spaces')
[void][IO.Directory]::CreateDirectory($test)
$release=Get-Content -LiteralPath (Join-Path $build.image 'release.json') -Raw|ConvertFrom-Json
$payload=Join-Path $build.image $release.payload
$manifestPath=Join-Path $payload 'payload.json'
Assert-Native ((Get-FileHash $manifestPath -Algorithm SHA256).Hash.ToLowerInvariant() -eq $release.manifestSha256) 'Payload manifest hash differs.'
$manifest=Get-Content -LiteralPath $manifestPath -Raw|ConvertFrom-Json
Assert-Native ($release.releaseVersion -eq $pin.releaseVersion -and $manifest.releaseVersion -eq $pin.releaseVersion -and $release.packageVersion -eq $pin.packageVersion -and $manifest.packageVersion -eq $pin.packageVersion -and $release.sourceRevision -eq $build.sourceRevision -and $manifest.sourceRevision -eq $build.sourceRevision) 'Release/package/source metadata differs from the pinned current release.'
foreach($entry in $manifest.files){$path=Join-Path $payload $entry.path;Assert-Native ((Get-Item -LiteralPath $path).Length -eq $entry.bytes -and (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() -eq $entry.sha256) ('Payload differs: '+$entry.path)}
$private=@($manifest.files|Where-Object{$_.path -match '(?i)(^|/)(credentials\.properties|jagex-accounts\.dat|\.env|current\.json|[^/]+\.fsp|[^/]+\.log|bank-(ledger|checkpoint|pending)-[^/]+\.json)$'})
Assert-Native ($private.Count -eq 0) 'Private/test data entered the image.'
Assert-Native (@($manifest.files|Where-Object{$_.path -match '^lib/.*(junit|shadow|preview|test-fixture).*\.jar$'}).Count -eq 0) 'Unrelated Java artifacts entered the image.'
Pass-Native 'Complete payload hashes, release/source metadata and private-data exclusion'
$source=Get-Content -LiteralPath (Join-Path $payload 'SOURCE.json') -Raw|ConvertFrom-Json
$native=Get-Content -LiteralPath (Join-Path $payload 'native-build.json') -Raw|ConvertFrom-Json
$revision=(& git -C $project rev-parse HEAD | Out-String).Trim();Assert-Native ($LASTEXITCODE -eq 0) 'Cannot verify source revision.'
$tree=(& git -C $project rev-parse HEAD:src/main | Out-String).Trim();Assert-Native ($LASTEXITCODE -eq 0) 'Cannot verify production source tree.'
Assert-Native ($source.revision -eq $revision -and $native.sourceRevision -eq $revision -and $build.sourceRevision -eq $revision -and $source.gameplayTree -eq $tree -and $native.gameplayTree -eq $tree) 'Installer was not built from the current source commit/tree.'
Assert-Native ($source.releaseVersion -eq $pin.releaseVersion -and $native.releaseVersion -eq $pin.releaseVersion -and $native.packageVersion -eq $pin.packageVersion) 'Embedded source/native version metadata differs.'
if(!$build.localTestCandidate){Assert-Native (!$build.sourceWorktreeDirty -and !$source.releaseWorktreeDirty -and !$source.localTestCandidate -and !$native.packagingWorktreeDirty -and !$native.localTestCandidate) 'A final installer contains uncommitted or local-test source.'}
$dependencies=Get-Content -LiteralPath (Join-Path $payload 'DEPENDENCIES.json') -Raw|ConvertFrom-Json
foreach($dependency in $dependencies.artifacts){Assert-Native ((Get-FileHash -LiteralPath (Join-Path $payload $dependency.path) -Algorithm SHA256).Hash.ToLowerInvariant() -eq $dependency.sha256) ('Dependency manifest differs: '+$dependency.coordinate)}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$pluginPath=Join-Path $payload ('lib/fromScratch-'+$pin.packageVersion+'.jar')
$plugin=[IO.Compression.ZipFile]::OpenRead($pluginPath)
try{Assert-Native (@($plugin.Entries|Where-Object{$_.FullName -match '(^org/junit/|Test.class$|^com/fromscratch/(beta|preview)/|META-INF/services/net.runelite.client.plugins.Plugin)'}).Count -eq 0) 'Development code entered the production plugin JAR.';Assert-Native ($null -ne $plugin.GetEntry('com/fromscratch/icon.png')) 'Production navigation icon is missing.'}finally{$plugin.Dispose()}
Pass-Native 'Committed source provenance, dependency manifests and production JAR isolation'
$icon=[IO.File]::ReadAllBytes((Join-Path $build.image 'FromScratch.ico'))
Assert-Native ([BitConverter]::ToUInt16($icon,2) -eq 1 -and [BitConverter]::ToUInt16($icon,4) -eq 5) 'ICO frame count invalid.'
$sizes=@();for($i=0;$i -lt 5;$i++){$n=[int]$icon[6+16*$i];$sizes+= $(if($n -eq 0){256}else{$n})}
Assert-Native (($sizes -join ',') -eq '16,32,48,64,256') 'Required icon sizes missing.'
Pass-Native 'Original-brand ICO has 16/32/48/64/256 frames'
$launcher=Join-Path $build.image 'FromScratchBeta.exe';Assert-Native (Gui-Executable $launcher) 'Launcher has a console subsystem.'
Assert-Native ((Windows-Version $launcher) -eq [Version]$pin.windowsVersion) 'Launcher file version differs from the pinned current Windows version.'
Add-Type -AssemblyName System.Drawing
$embedded=[Drawing.Icon]::ExtractAssociatedIcon($launcher);try{Assert-Native ($null -ne $embedded) 'Launcher has no embedded icon.'}finally{$embedded.Dispose()}
Pass-Native 'Native Windows GUI executable and embedded icon'
$runtimeRelease=[IO.File]::ReadAllText((Join-Path $payload 'runtime/release'))
$runtimeVersion=($pin.runtime.version -split '\+')[0]
Assert-Native ($runtimeRelease.Contains('JAVA_VERSION="'+$runtimeVersion+'"') -and (Test-Path -LiteralPath (Join-Path $payload 'runtime/legal/java.base/LICENSE'))) 'Wrong private runtime or missing licenses.'
Pass-Native 'Pinned private Temurin 11 runtime and retained legal notices'
$copy=Join-Path $test 'Launcher from Downloads';Copy-Item -LiteralPath $build.image -Destination $copy -Recurse
Assert-Native ((Run-Native (Join-Path $copy 'FromScratchBeta.exe') '--verify-only' 45000 $true) -eq 0) 'Private-runtime verification failed with system Java unavailable.'
$log=Latest-LauncherLog $copy;Assert-Native ([IO.File]::ReadAllText($log.FullName).Contains('Verification passed')) 'Verification log missing.'
Pass-Native 'Launcher works from an unrelated working directory and spaced path, without PATH/JAVA_HOME Java'
$copyPayload=Join-Path $copy $release.payload;$jar=Get-ChildItem -LiteralPath (Join-Path $copyPayload 'lib') -Filter 'fromScratch-*.jar'|Select-Object -First 1
$bytes=[IO.File]::ReadAllBytes($jar.FullName);[IO.File]::AppendAllText($jar.FullName,'tampered')
try{Assert-Native ((Run-Native (Join-Path $copy 'FromScratchBeta.exe') '--verify-only') -eq 2) 'Tampered JAR was accepted.'}finally{[IO.File]::WriteAllBytes($jar.FullName,$bytes)}
Pass-Native 'Damaged library is rejected and a diagnostic is logged'
$javaw=Join-Path $copyPayload 'runtime/bin/javaw.exe';$held=Join-Path $test 'held-javaw.exe'
Move-Item -LiteralPath $javaw -Destination $held
try{Assert-Native ((Run-Native (Join-Path $copy 'FromScratchBeta.exe') '--verify-only') -eq 2) 'Missing runtime file was accepted.'}finally{Move-Item -LiteralPath $held -Destination $javaw}
Pass-Native 'Missing bundled runtime is rejected before client launch'
$extra=Join-Path $copyPayload 'lib/unexpected.jar';[IO.File]::WriteAllBytes($extra,@(1,2,3))
try{Assert-Native ((Run-Native (Join-Path $copy 'FromScratchBeta.exe') '--verify-only') -eq 2) 'Unexpected classpath JAR was accepted.'}finally{Remove-Item -LiteralPath $extra}
Pass-Native 'Unexpected classpath additions are rejected'
if(!$ImageOnly){
    Assert-Native ($build.installer -and (Test-Path -LiteralPath $build.installer)) 'Setup EXE is missing.'
    Assert-Native (Gui-Executable $build.installer) 'Setup is not a GUI executable.'
    Assert-Native ((Windows-Version $build.installer) -eq [Version]$pin.windowsVersion) 'Setup file version differs from the pinned current Windows version.'
    Assert-Native ((Get-FileHash -LiteralPath $build.installer -Algorithm SHA256).Hash.ToLowerInvariant() -eq $build.sha256) 'Setup checksum differs.'
    $setupIcon=[Drawing.Icon]::ExtractAssociatedIcon($build.installer);$appIcon=[Drawing.Icon]::ExtractAssociatedIcon($launcher)
    try{
        $setupBitmap=$setupIcon.ToBitmap();$appBitmap=$appIcon.ToBitmap()
        try{
            Assert-Native ($setupBitmap.Size -eq $appBitmap.Size) 'Setup and launcher icon sizes differ.'
            for($x=0;$x -lt $appBitmap.Width;$x++){for($y=0;$y -lt $appBitmap.Height;$y++){Assert-Native ($setupBitmap.GetPixel($x,$y).ToArgb() -eq $appBitmap.GetPixel($x,$y).ToArgb()) 'Setup branding differs from the native launcher.'}}
        }finally{$setupBitmap.Dispose();$appBitmap.Dispose()}
    }finally{$setupIcon.Dispose();$appIcon.Dispose()}
    Pass-Native 'Setup checksum, GUI executable and matching embedded From Scratch icon'
    $downloads=Join-Path $test 'Downloads';[void][IO.Directory]::CreateDirectory($downloads)
    $setup=Join-Path $downloads 'FromScratch-Beta-Setup.exe'
    [IO.File]::WriteAllText((Join-Path $test '.from-scratch-installer-test'),'Isolated From Scratch installer test')
    # Compile only an older installer-version fixture. It uses this exact candidate payload,
    # a distinct test AppId and isolated paths; the real v0.10.7 release is never touched.
    $inno=Join-Path $project 'build/native-installer/tools/inno/ISCC.exe'
    & $inno '/Q' ('/DImageDir='+$build.image) ('/DOutputDir='+$downloads) '/DPackageVersion=0.10.7' '/DWindowsVersion=0.10.7.0' '/DReleaseVersion=v0.10.7-upgrade-fixture' (Join-Path $project 'beta/windows-native/FromScratch.iss')
    Assert-Native ($LASTEXITCODE -eq 0) 'Older installer-version fixture compilation failed.'
    $sentinel=Join-Path $test 'User/.runelite/plugin-data/from_scratch/progression-fixture.txt';[void][IO.Directory]::CreateDirectory((Split-Path $sentinel));[IO.File]::WriteAllText($sentinel,'preserve progression fixture')
    $settings=Join-Path $test 'User/.runelite/settings.properties';[IO.File]::WriteAllText($settings,'synthetic settings, preserve unchanged')
    $unrelated=Join-Path $test 'User/.runelite/sideloaded-plugins/unrelated.txt';[void][IO.Directory]::CreateDirectory((Split-Path $unrelated));[IO.File]::WriteAllText($unrelated,'preserve unrelated fixture')
    $export=Join-Path $test 'User/private-backup.fsp';[IO.File]::WriteAllText($export,'synthetic backup, not real progression')
    $args='/VERYSILENT /SUPPRESSMSGBOXES /NORESTART /TestRoot="'+$test+'" /LOG="'+(Join-Path $test 'setup.log')+'"'
    Assert-Native ((Run-Native $setup $args 60000) -eq 0) 'Older installer-version fixture setup failed.'
    Copy-Item -LiteralPath $build.installer -Destination $setup -Force
    Assert-Native ((Run-Native $setup $args 60000) -eq 0) 'Actual setup failed in isolated Downloads fixture.'
    $setupLog=[IO.File]::ReadAllText((Join-Path $test 'setup.log'))
    Assert-Native ($setupLog.Contains('Administrative install mode: No') -and $setupLog.Contains('Install mode root key: HKEY_CURRENT_USER')) 'Setup unexpectedly used administrator or machine-wide installation.'
    $installed=Join-Path $test 'Installed App With Spaces';$exe=Join-Path $installed 'FromScratchBeta.exe'
    Assert-Native (Test-Path -LiteralPath $exe) 'Installed executable missing.'
    foreach($entry in (Get-Content -LiteralPath (Join-Path $build.output 'installer-files.json') -Raw|ConvertFrom-Json)){Assert-Native ((Get-FileHash -LiteralPath (Join-Path $installed $entry.path) -Algorithm SHA256).Hash.ToLowerInvariant() -eq $entry.sha256) ('Installed payload differs: '+$entry.path)}
    Pass-Native 'Actual setup upgrades older-version fixture from Downloads; every installed payload file matches'
    $shell=New-Object -ComObject WScript.Shell
    try{
        foreach($relative in @('Desktop/From Scratch Beta.lnk','Start Menu/From Scratch Beta/From Scratch Beta.lnk')){
            $linkPath=Join-Path $test $relative;Assert-Native (Test-Path -LiteralPath $linkPath) 'Shortcut missing.'
            $link=$shell.CreateShortcut($linkPath)
            try{Assert-Native ($link.TargetPath -eq $exe -and $link.WorkingDirectory -eq $installed -and $link.IconLocation.StartsWith($exe)) 'Shortcut target, folder or branded icon differs.'}finally{[void][Runtime.InteropServices.Marshal]::ReleaseComObject($link)}
        }
    }finally{[void][Runtime.InteropServices.Marshal]::ReleaseComObject($shell)}
    # Inno shortens long AppIds with a hash. Locate only this fixture's exact install path.
    $keys=@(Get-ChildItem HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall|Where-Object{$_.PSChildName -like 'FromScratchBeta-InstallerTest-*'}|Where-Object{(Get-ItemProperty $_.PSPath).InstallLocation.TrimEnd('\') -eq $installed})
    Assert-Native ($keys.Count -eq 1) 'Windows Installed apps registration missing or duplicated.'
    $uninstallKey=$keys[0].PSPath
    Assert-Native ((Get-ItemProperty $uninstallKey).DisplayVersion -eq $releaseNumber) 'Upgrade did not update Installed apps to the pinned current release version.'
    Pass-Native 'Desktop/Start Menu native shortcuts, icon references and Installed apps entry'
    Assert-Native ((Run-Native $setup $args 60000) -eq 0) 'Reinstall failed.'
    Assert-Native (([IO.File]::ReadAllText($sentinel)) -eq 'preserve progression fixture') 'Reinstall changed progression fixture.'
    Assert-Native ((Run-Native $exe '--verify-only' 45000 $true) -eq 0) 'Installed application private-runtime verification failed.'
    Pass-Native 'Reinstall retains data and installed app uses private Java'
    $unknown=Join-Path $installed 'owner-note.txt';[IO.File]::WriteAllText($unknown,'keep unrelated file')
    $menu=Join-Path $test 'Start Menu/From Scratch Beta/From Scratch Beta.lnk';$shell=New-Object -ComObject WScript.Shell
    try{$link=$shell.CreateShortcut($menu);$link.TargetPath=Join-Path $env:WINDIR 'notepad.exe';$link.Save();[void][Runtime.InteropServices.Marshal]::ReleaseComObject($link)}finally{[void][Runtime.InteropServices.Marshal]::ReleaseComObject($shell)}
    Assert-Native ((Run-Native (Join-Path $installed 'unins000.exe') '/VERYSILENT /SUPPRESSMSGBOXES /NORESTART' 60000) -eq 0) 'Uninstall failed.'
    Assert-Native (!(Test-Path -LiteralPath $exe) -and !(Test-Path (Join-Path $test 'Desktop/From Scratch Beta.lnk')) -and !(Test-Path $uninstallKey)) 'Owned application/shortcut/registration remains.'
    Assert-Native ((Test-Path -LiteralPath $unknown) -and (Test-Path -LiteralPath $menu) -and (Test-Path -LiteralPath $export) -and (Test-Path -LiteralPath $unrelated) -and ([IO.File]::ReadAllText($settings) -eq 'synthetic settings, preserve unchanged') -and ([IO.File]::ReadAllText($sentinel) -eq 'preserve progression fixture')) 'Uninstall removed unrelated files or data.'
    Pass-Native 'Windows uninstaller removes owned files/shortcut/registration and preserves data, extras and changed shortcuts'
}
$result=[ordered]@{passed=$passed;imageOnly=[bool]$ImageOnly;fixture=$test;sourceRevision=$build.sourceRevision;releaseVersion=$build.releaseVersion;liveLoginTested=$false;graphicalWizardTested=$false}
$result|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $project 'build/native-installer/test-results.json') -Encoding UTF8
Write-Host ('Native packaging checks: '+$passed+' passed. Actual wizard visuals, normal launch and login require owner acceptance. No real progression was used.')
