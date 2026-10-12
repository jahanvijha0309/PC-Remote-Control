# Changelog

## v1.1.0
- One-click install: Windows installer for the server (starts by itself, opens the firewall for the phone).
- Server fixes: files listed for download can now actually be downloaded; uploads and downloads can no longer reach files outside their folders.
- Server now stores its data in `%LOCALAPPDATA%\PC-Remote-Server` when installed, and closing the window stops the server.
- Android source rewritten to match the released app: Home, Media, Free-Mode, Files, Settings, PC Status, Quick Actions and QR scanner.
- New **Touchpad** screen (drag, tap, two-finger tap, two-finger scroll).
- New **Screen** screen: live PC screen with touch-to-move and tap-to-click (`GET /screen`, `POST /mouseTo`).
- New **Shortcuts** screen with one-tap key combinations (`POST /hotkey`).
- New joystick **dead zone** setting.
- PC Status and Quick Actions (lock, sleep, restart, shut down, cancel).
- Light and dark colour themes.

## v1.0.0
- First version: mouse, keyboard, volume, media keys, file transfer, screenshots.
