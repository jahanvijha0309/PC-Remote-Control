; Inno Setup script - builds PC-Remote-Server-Setup-<version>.exe
; Build from the command line with:  iscc /DMyAppVersion=1.1.0 PC-Remote-Server.iss
#ifndef MyAppVersion
  #define MyAppVersion "1.1.0"
#endif
#define MyAppName "PC Remote Server"
#define MyAppExe "PC-Remote-Server.exe"

[Setup]
AppId={{B6E2C5D4-7A1F-4C0E-9B53-2F8D1A4E6C71}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
OutputDir=Output
OutputBaseFilename=PC-Remote-Server-Setup-{#MyAppVersion}
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayIcon={app}\{#MyAppExe}
DisableProgramGroupPage=yes

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Shortcuts:"
Name: "autostart"; Description: "Start PC Remote Server when Windows starts"; GroupDescription: "Options:"; Flags: unchecked

[Files]
Source: "..\pc-server\dist\PC-Remote-Server\*"; DestDir: "{app}"; Flags: recursesubdirs ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExe}"; Tasks: desktopicon
Name: "{commonstartup}\{#MyAppName}"; Filename: "{app}\{#MyAppExe}"; Tasks: autostart

[Run]
; Let the phone reach the server (port 5500) on home / work networks
Filename: "{sys}\netsh.exe"; Parameters: "advfirewall firewall add rule name=""PC Remote Server"" dir=in action=allow protocol=TCP localport=5500 profile=private,domain program=""{app}\{#MyAppExe}"""; Flags: runhidden
Filename: "{app}\{#MyAppExe}"; Description: "Start {#MyAppName} now"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "{sys}\netsh.exe"; Parameters: "advfirewall firewall delete rule name=""PC Remote Server"""; Flags: runhidden
