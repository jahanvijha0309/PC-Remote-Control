import json
import os
import sys
if getattr(sys, "frozen", False):
    # Installed .exe: keep settings, uploads and the log in a folder the user can write to.
    DATA_DIR = os.path.join(os.environ.get("LOCALAPPDATA", os.path.expanduser("~")), "PC-Remote-Server")
    os.makedirs(DATA_DIR, exist_ok=True)
    os.chdir(DATA_DIR)
    if sys.stdout is None or sys.stderr is None:
        sys.stdout = sys.stderr = open(os.path.join(DATA_DIR, "server.log"), "a", buffering=1, encoding="utf-8")
else:
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
from flask import Flask, request as req,  send_file
from werkzeug.utils import safe_join
import pyautogui
import os
from PIL import ImageGrab 
import time 
import datetime
import threading

from classes import net, er, spot, dis,you

from functions import set_volume,move_cursor_relative
from functions import mouseClick, keyStroke, fullscreen, is_request_type
from functions import load_json_file, openProgramm, onStartup, mouseScroll
from gui import create_gui, update_terminal
import system_actions
from io import BytesIO
import json as _json
import time


app = Flask(__name__)
UPLOAD_FOLDER = 'for_uploads/'
SCREENSHOT_FOLDER = "for_screenshots/"
DOWNLOAD_FOLDER = 'for_downloads/'

last_mouse_move_time = None
update_terminal_lock = threading.Lock()




@app.route("/scroll", methods=['POST']) 
@is_request_type("POST")                 
def Scroll():
    data = req.get_data(as_text=True)
    mouseScroll(int(data))
    update_terminal("scrolled mouse")
    return "200"


@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in req.files:
        return 'No file part', 400
    file = req.files['file']
    if file.filename == '':
        return 'No selected file', 400
    if file:
        # Keep only the file name so a phone can never write outside the upload folder.
        filename = os.path.basename(file.filename.replace("\\", "/"))
        if not filename:
            return 'No selected file', 400
        save_path = os.path.join(os.getcwd(), "data_folder", UPLOAD_FOLDER, filename)
        os.makedirs(os.path.dirname(save_path), exist_ok=True)
        file.save(save_path)
        update_terminal("file uploaded")
        return 'File uploaded successfully', 200

@app.route('/download', methods=['POST'])
def download_file():
    data = req.get_json()
    filename = data.get('filename')
    print(data)
    if not filename:
        return 'No filename provided', 400
    
    
    # Only files inside data_folder/for_downloads can be fetched (no ../ tricks).
    download_dir = os.path.join(os.getcwd(), "data_folder", DOWNLOAD_FOLDER)
    file_path = safe_join(download_dir, filename)
    
    if file_path is None or not os.path.isfile(file_path):
        print("File not found")
        return 'File not found', 404
    update_terminal("file downloaded")
    return send_file(file_path, as_attachment=True)


@app.route("/screenshot")
def screenshot():
    screenshot = ImageGrab.grab()
    current_time = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S") 
    save_path = os.path.join("data_folder//", SCREENSHOT_FOLDER, f"{current_time}.png")
    screenshot.save(save_path, "PNG")  
    update_terminal("took a screenshot")
    return "200"




@app.route("/downloadfolder")
def downloadfolder():
    folder_path = os.path.join(os.getcwd(), "data_folder", DOWNLOAD_FOLDER)
    os.makedirs(folder_path, exist_ok=True)
    download_files = [f for f in os.listdir(folder_path) if os.path.isfile(os.path.join(folder_path, f))]
    return json.dumps(download_files)  
    
        


@app.route("/sendKeys", methods=['POST'])
@is_request_type("POST")
def sendKeys():
    data = req.get_data(as_text=True)
    keyStroke(data)
    update_terminal("sent some key input")
    return "200"




@app.route("/spo") 
def spotify():
    openProgramm(spot.location)
    fullscreen("Spotify")
    update_terminal("opened spotify")
    return "200"

@app.route("/dis") 
def disney():
    openProgramm(dis.location)
    update_terminal("opened disney")
    return "200"


@app.route("/net")
def netflix():
    openProgramm(net.location)
    fullscreen("Netflix")
    update_terminal("opened netflix")
    return "200"

@app.route("/you")
def youtube():
    openProgramm(you.location)
    update_terminal("youtube opened")
    return "200"

@app.route("/name")
def name():
    devicename = load_json_file("devicename.json")
    return devicename["device_name"]

@app.route("/password", methods=['POST']) 
@is_request_type("POST")                      
def password():
    if os.path.exists("password.json"):
        password_data = load_json_file("password.json")
        data = req.get_data(as_text=True)
        if data == password_data.get("password"):
            update_terminal("connected to device")
            return "200"
        else:
            return "400"
    else:
        update_terminal("connected to device")
        return "200"



@app.route("/fuScr") 
def fullscreenpress():
    try:
        pyautogui.hotkey("F")
        update_terminal("entered fullscreen")
        return er.r0
    except:
        return er.r3

@app.route("/left") 
def back():
    try:
        pyautogui.hotkey("left")
        update_terminal("sent left key")
        return er.r0
    except:
        return er.r0

@app.route("/right") 
def forward():
    try:
        pyautogui.hotkey("right")
        update_terminal("sent right key")
        return er.r0
    except:
        return er.r0
    
@app.route("/backspace") 
def delete():
    try:
        pyautogui.hotkey("backspace")
        update_terminal("sent backspace key")
        return er.r0
    except:
        return er.r0

@app.route("/enter") 
def enter():
    try:
        pyautogui.hotkey("enter")
        update_terminal("sent enter key")
        return er.r0
    except:
        return er.r0

@app.route("/clP") 
def closeProgramm():
    try:
        pyautogui.hotkey("alt","f4")
        update_terminal("closed program")
        return er.r0
    except:
        return er.r3


def update_terminal_after_delay():
    global last_mouse_move_time
    with update_terminal_lock:
        current_time = time.time()
        if last_mouse_move_time is not None and current_time - last_mouse_move_time >= 1:
            update_terminal("moved mouse")
            last_mouse_move_time = None



def schedule_terminal_update():
    threading.Timer(1, update_terminal_after_delay).start()

@app.route("/mouseMove", methods=['POST'])
@is_request_type("POST")
def mouseMove():
    global last_mouse_move_time
    data = req.get_data(as_text=True)
    x, y = map(int, data.split("/"))
    
    try:
        move_cursor_relative(x, y)
    except Exception as e:
        print(f"Error: {e}")
        return er.r5
    
    last_mouse_move_time = time.time()
    schedule_terminal_update()
    
    return er.r0


@app.route("/lc")
def leftClick():
 mouseClick("left" ,1  )
 update_terminal("sent left click")
 return "200"

@app.route("/rc") 
def rightClick():
    mouseClick("right",1)
    update_terminal("sent right click")
    return "200"

@app.route("/space") 
def spaceBar():
    keyStroke(" ")
    update_terminal("sent space bar")
    return "200"

@app.route("/sound",methods=['POST']) 
@is_request_type("POST")                          
def changeSound():
    volume = req.get_data(as_text=True)
    set_volume(float(volume))
    update_terminal("adjusted sound")
    return "200"


@app.route("/screen")
def screen():
    """JPEG of the PC screen, scaled down for the phone. ?w=<width px>&q=<jpeg quality>"""
    try:
        width = max(240, min(1920, int(req.args.get("w", 720))))
        quality = max(20, min(90, int(req.args.get("q", 50))))
        image = ImageGrab.grab().convert("RGB")
        height = max(1, int(image.height * width / image.width))
        image = image.resize((width, height))
        buffer = BytesIO()
        image.save(buffer, "JPEG", quality=quality)
        buffer.seek(0)
        return send_file(buffer, mimetype="image/jpeg")
    except Exception as e:
        print(f"Screen error: {e}")
        return er.r5, 500


@app.route("/mouseTo", methods=['POST'])
@is_request_type("POST")
def mouseTo():
    """Move the cursor to a spot on the screen. Body: "fx/fy" with fractions between 0 and 1."""
    try:
        fx, fy = map(float, req.get_data(as_text=True).split("/"))
        fx = min(max(fx, 0.0), 1.0)
        fy = min(max(fy, 0.0), 1.0)
        width, height = pyautogui.size()
        pyautogui.moveTo(int(fx * (width - 1)), int(fy * (height - 1)))
        return er.r0
    except Exception as e:
        print(f"mouseTo error: {e}")
        return er.r5, 400


@app.route("/hotkey", methods=['POST'])
@is_request_type("POST")
def hotkey():
    """Press a key combination. Body example: "ctrl+shift+esc" (max 4 keys, known key names only)."""
    keys = [k.strip().lower() for k in req.get_data(as_text=True).split("+") if k.strip()]
    if not keys or len(keys) > 4 or any(k not in pyautogui.KEYBOARD_KEYS for k in keys):
        return er.r5, 400
    try:
        pyautogui.hotkey(*keys)
        update_terminal("pressed " + "+".join(keys))
        return "200"
    except Exception as e:
        print(f"hotkey error: {e}")
        return er.r3, 500


@app.route("/status")
def status():
    try:
        return app.response_class(_json.dumps(system_actions.get_status()), mimetype="application/json")
    except Exception as e:
        print(f"Status error: {e}")
        return er.r5, 500


def _power_action(label, action):
    try:
        action()
        update_terminal(label)
        return "200"
    except Exception as e:
        print(f"{label} failed: {e}")
        return er.r3, 500


@app.route("/lock", methods=['POST'])
def lock_route():
    return _power_action("locked PC", system_actions.lock_pc)


@app.route("/sleep", methods=['POST'])
def sleep_route():
    return _power_action("sent PC to sleep", system_actions.sleep_pc)


@app.route("/restart", methods=['POST'])
def restart_route():
    return _power_action("restart in 5 seconds", system_actions.restart_pc)


@app.route("/shutdown", methods=['POST'])
def shutdown_route():
    return _power_action("shutdown in 5 seconds", system_actions.shutdown_pc)


@app.route("/cancel", methods=['POST'])
def cancel_route():
    return _power_action("cancelled restart/shutdown", system_actions.cancel_shutdown)


if __name__ == '__main__':
    
    onStartup()
    create_gui(app)
