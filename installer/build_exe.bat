@echo off
rem Builds pc-server\dist\PC-Remote-Server\PC-Remote-Server.exe (needs Python 3.9+ on Windows)
cd /d "%~dp0\..\pc-server"
if not exist .build-venv python -m venv .build-venv
call .build-venv\Scripts\activate
pip install -r requirements.txt pyinstaller
pyinstaller --noconfirm --clean --onedir --windowed --name PC-Remote-Server ^
  --collect-submodules comtypes --collect-all pycaw --hidden-import pygetwindow main.py
echo.
echo Done. The program is in pc-server\dist\PC-Remote-Server
echo To make the installer, open installer\PC-Remote-Server.iss in Inno Setup and press Compile.
pause
