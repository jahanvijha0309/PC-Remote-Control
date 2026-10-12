import ctypes
import os
import platform
import socket
import subprocess
import threading
import time

import psutil


def get_status():
    battery = psutil.sensors_battery()
    disk_root = os.path.abspath(os.sep)
    return {
        "hostname": socket.gethostname(),
        "os": platform.system() + " " + platform.release(),
        "cpu": round(psutil.cpu_percent(interval=0.3), 1),
        "ram": round(psutil.virtual_memory().percent, 1),
        "disk": round(psutil.disk_usage(disk_root).percent, 1),
        "battery": None if battery is None else round(battery.percent),
        "charging": None if battery is None else bool(battery.power_plugged),
        "uptime_minutes": int((time.time() - psutil.boot_time()) // 60),
    }


def _run(cmd):
    subprocess.Popen(cmd, shell=False)


def lock_pc():
    ctypes.windll.user32.LockWorkStation()


_last_sleep = 0


def sleep_pc():
    # Ignore repeat taps for 15 seconds so several queued sleeps don't fire after waking up.
    global _last_sleep
    now = time.time()
    if now - _last_sleep < 15:
        print("[sleep] ignored: sleep already requested a moment ago")
        return
    _last_sleep = now
    print("[sleep] request received")

    command = (
        "Add-Type -A System.Windows.Forms; "
        "[System.Windows.Forms.Application]::SetSuspendState('Suspend',$false,$false)"
    )

    def _sleep():
        time.sleep(1)
        ok = False
        try:
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", command],
                capture_output=True, text=True,
                creationflags=0x08000000,  # CREATE_NO_WINDOW
            )
            out = (result.stdout or "").strip()
            err = (result.stderr or "").strip()
            print("[sleep] powershell exit code:", result.returncode, "| output:", out, "| error:", err)
            ok = result.returncode == 0 and "False" not in out
        except Exception as e:
            print("[sleep] powershell failed:", e)
        if not ok:
            print("[sleep] trying direct Windows call")
            try:
                ctypes.windll.powrprof.SetSuspendState(False, True, False)
            except Exception as e:
                print("[sleep] direct call failed:", e)

    threading.Thread(target=_sleep, daemon=True).start()


def restart_pc(delay=5):
    _run(["shutdown", "/r", "/t", str(delay)])


def shutdown_pc(delay=5):
    _run(["shutdown", "/s", "/t", str(delay)])


def cancel_shutdown():
    _run(["shutdown", "/a"])