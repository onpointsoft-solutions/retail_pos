#define MyAppName "BizFlow POS"
#define MyAppVersion "2.0.0"
#define MyAppPublisher "BizFlow"
#define MyAppExeName "BizFlowPOS.exe"
#define MyAppSourceDir "target\package-image\BizFlowPOS"

#if !FileExists(MyAppSourceDir + "\.bizflow-icon-build")
  #error "Run build-installer.bat before compiling this Inno script. The current app image is stale or has no BizFlow icon."
#endif

[Setup]
AppId={{4C6B2444-83D4-49F1-B053-390991436C6D}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\BizFlow POS
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
OutputDir=target\installer
OutputBaseFilename=BizFlowPOS-Setup-{#MyAppVersion}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
MinVersion=10.0
SetupIconFile=src\main\resources\BizFlowPOS.ico
CloseApplications=yes
RestartApplications=no
UninstallDisplayIcon={app}\{#MyAppExeName}
VersionInfoVersion={#MyAppVersion}
VersionInfoCompany={#MyAppPublisher}
VersionInfoDescription={#MyAppName} Installer
VersionInfoProductName={#MyAppName}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts:"; Flags: unchecked

[Files]
Source: "{#MyAppSourceDir}\*"; DestDir: "{app}"; Excludes: ".bizflow-icon-build"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Launch {#MyAppName}"; Flags: nowait postinstall skipifsilent
