// Validate the actual compiler; official ISCC.exe does not expose a useful PE version.
#if DecodeVer(Ver) != "6.7.3"
  #error Use the pinned Inno Setup 6.7.3 compiler.
#endif
#pragma message "Verified Inno Setup " + DecodeVer(Ver)
#ifndef ImageDir
  #error ImageDir must point to the verified native application image.
#endif
#ifndef OutputDir
  #error OutputDir must be specified.
#endif
#ifndef PackageVersion
  #error PackageVersion must be supplied from the pinned release configuration.
#endif
#ifndef WindowsVersion
  #error WindowsVersion must be supplied from the pinned release configuration.
#endif
#ifndef ReleaseVersion
  #error ReleaseVersion must be supplied from the pinned release configuration.
#endif
#define Product "From Scratch Beta"

[Setup]
AppId={code:ApplicationId}
UsePreviousLanguage=no
AppName={#Product}
AppVersion={#PackageVersion}
AppVerName={#Product} {#PackageVersion}
AppPublisher=Pilots r kwl
AppPublisherURL=https://github.com/PilotsRKwl/fromScratch
DefaultDirName={code:ApplicationDirectory}
DefaultGroupName={#Product}
PrivilegesRequired=lowest
ArchitecturesAllowed=x64os
ArchitecturesInstallIn64BitMode=x64os
MinVersion=10.0
DisableDirPage=yes
DisableProgramGroupPage=yes
DisableReadyPage=yes
DisableWelcomePage=no
AllowNoIcons=no
UninstallDisplayIcon={app}\FromScratchBeta.exe
SetupIconFile=FromScratch.ico
LicenseFile=..\..\LICENSE
OutputDir={#OutputDir}
OutputBaseFilename=FromScratch-Beta-Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
CloseApplications=no
RestartApplications=no
AppMutex=FromScratchBeta.NativeLauncher
VersionInfoVersion={#WindowsVersion}
VersionInfoProductVersion={#WindowsVersion}
VersionInfoProductTextVersion={#ReleaseVersion}
VersionInfoDescription=From Scratch Beta Setup
ChangesEnvironment=no
ChangesAssociations=no

[Messages]
WelcomeLabel1=Welcome to From Scratch Beta
WelcomeLabel2=Install From Scratch and begin a fresh Old School RuneScape challenge using your existing character.%n%nJava is included. Your RuneLite settings and challenge saves stay where they are.
FinishedLabel=From Scratch Beta is installed. Open the desktop or Start Menu shortcut to play. Sign in through RuneLite's official Jagex Accounts panel.

[Files]
Source: "{#ImageDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{code:DesktopDirectory}\From Scratch Beta"; Filename: "{app}\FromScratchBeta.exe"; WorkingDir: "{app}"; IconFilename: "{app}\FromScratchBeta.exe"; Flags: uninsneveruninstall
Name: "{code:MenuDirectory}\From Scratch Beta"; Filename: "{app}\FromScratchBeta.exe"; WorkingDir: "{app}"; IconFilename: "{app}\FromScratchBeta.exe"; Flags: uninsneveruninstall

[Run]
Filename: "{app}\FromScratchBeta.exe"; Description: "Open From Scratch Beta"; Flags: nowait postinstall skipifsilent

[Code]
// The explicit developer test mode redirects shortcuts and uninstall identity as well as files.
// It cannot activate accidentally: its marker and Downloads source location must both match.
function TestRoot: String;
begin
  Result := ExpandConstant('{param:TestRoot|}');
end;

function ApplicationId(Param: String): String;
begin
  if TestRoot = '' then Result := '{3B482781-CE31-4BD2-8C72-2B952496E61F}'
  else Result := 'FromScratchBeta-InstallerTest-' + ExtractFileName(TestRoot);
end;

function ApplicationDirectory(Param: String): String;
begin
  if TestRoot = '' then Result := ExpandConstant('{localappdata}\Programs\FromScratchBeta')
  else Result := TestRoot + '\Installed App With Spaces';
end;

function DesktopDirectory(Param: String): String;
begin
  if TestRoot = '' then Result := ExpandConstant('{userdesktop}')
  else Result := TestRoot + '\Desktop';
end;

function MenuDirectory(Param: String): String;
begin
  if TestRoot = '' then Result := ExpandConstant('{userprograms}\From Scratch Beta')
  else Result := TestRoot + '\Start Menu\From Scratch Beta';
end;

function ExistingShortcutOwned(Path: String): Boolean;
var Shell, Link: Variant; Target, Arguments, Legacy: String; P: Integer;
begin
  Result := True;
  if not FileExists(Path) then Exit;
  Result := False;
  try
    Shell := CreateOleObject('WScript.Shell');
    Link := Shell.CreateShortcut(Path);
    Target := Link.TargetPath;
    Arguments := Link.Arguments;
    if CompareText(Target, ApplicationDirectory('') + '\FromScratchBeta.exe') = 0 then Result := True;
    // A known M10.5 shortcut may be replaced; its old program files are retained.
    Legacy := ExpandConstant('{localappdata}\FromScratchBeta\versions\');
    P := Pos(Lowercase(Legacy), Lowercase(Arguments));
    if (CompareText(Target, ExpandConstant('{sys}\WindowsPowerShell\v1.0\powershell.exe')) = 0) and (P > 0) then
    begin
      Arguments := Copy(Arguments, P, Length(Arguments));
      P := Pos('"', Arguments);
      if P > 0 then Arguments := Copy(Arguments, 1, P - 1);
      if (CompareText(ExtractFileName(Arguments), 'Launch.ps1') = 0) and
        FileExists(ExtractFilePath(Arguments) + '.from-scratch-beta-owned.json') then Result := True;
    end;
  except
    Result := False;
  end;
end;

function InitializeSetup: Boolean;
var TestPath: String; Marker: AnsiString;
begin
  Result := False;
  TestPath := TestRoot;
  if TestPath <> '' then
  begin
    if (ExtractFileDrive(TestPath) = '') or (Pos('..', TestPath) > 0) or
      (CompareText(RemoveBackslashUnlessRoot(ExpandConstant('{src}')), TestPath + '\Downloads') <> 0) or
      not LoadStringFromFile(TestPath + '\.from-scratch-installer-test', Marker) or
      (Trim(Marker) <> 'Isolated From Scratch installer test') then
    begin
      MsgBox('The isolated installer test folder is not valid.', mbError, MB_OK); Exit;
    end;
  end;
  if not ExistingShortcutOwned(DesktopDirectory('') + '\From Scratch Beta.lnk') or
    not ExistingShortcutOwned(MenuDirectory('') + '\From Scratch Beta.lnk') then
  begin
    MsgBox('Another shortcut already uses the name From Scratch Beta. It has been preserved. Rename that shortcut before installing.', mbError, MB_OK); Exit;
  end;
  Result := True;
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  Result := '';
  if CompareText(RemoveBackslashUnlessRoot(WizardDirValue), ApplicationDirectory('')) <> 0 then
    Result := 'From Scratch Beta must use its own per-user application folder. No RuneLite or other program folder will be overwritten.';
end;

procedure DeleteOwnedShortcut(Path: String);
var Shell, Link: Variant; Target: String;
begin
  if not FileExists(Path) then Exit;
  try
    Shell := CreateOleObject('WScript.Shell'); Link := Shell.CreateShortcut(Path); Target := Link.TargetPath;
    if CompareText(Target, ExpandConstant('{app}\FromScratchBeta.exe')) = 0 then DeleteFile(Path);
  except
    // An unreadable/changed shortcut is retained rather than deleting another program's link.
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var Root: String;
begin
  if CurUninstallStep = usUninstall then
  begin
    Root := ExtractFileDir(ExpandConstant('{app}'));
    if FileExists(Root + '\.from-scratch-installer-test') then
    begin
      DeleteOwnedShortcut(Root + '\Desktop\From Scratch Beta.lnk');
      DeleteOwnedShortcut(Root + '\Start Menu\From Scratch Beta\From Scratch Beta.lnk');
    end
    else
    begin
      DeleteOwnedShortcut(ExpandConstant('{userdesktop}\From Scratch Beta.lnk'));
      DeleteOwnedShortcut(ExpandConstant('{userprograms}\From Scratch Beta\From Scratch Beta.lnk'));
      RemoveDir(ExpandConstant('{userprograms}\From Scratch Beta'));
    end;
  end;
end;
