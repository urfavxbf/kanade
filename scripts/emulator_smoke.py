#!/usr/bin/env python3
"""Exercise basic screens on a clean Android emulator with a synthetic local song."""
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

OUT = Path("emulator-results")
OUT.mkdir(exist_ok=True)
failures = []

def adb(*args, binary=False, check=True):
    result = subprocess.run(["adb", *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=check)
    return result.stdout if binary else result.stdout.decode("utf-8", "replace")

def snapshot(name):
    time.sleep(2)
    data = adb("exec-out", "screencap", "-p", binary=True)
    if not data.startswith(b"\x89PNG"):
        raise RuntimeError("No PNG screenshot from emulator")
    (OUT / f"{name}.png").write_bytes(data)
    adb("shell", "uiautomator", "dump", "/sdcard/window.xml")
    xml = adb("exec-out", "cat", "/sdcard/window.xml")
    (OUT / f"{name}.xml").write_text(xml)
    return xml

def tap(label, xml):
    root = ET.fromstring(xml)
    nodes = []
    for node in root.iter("node"):
        if any(label.lower() == node.get(attr, "").strip().lower() for attr in ("text", "content-desc")):
            match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
            if match:
                x1, y1, x2, y2 = map(int, match.groups())
                if x2 > x1 and y2 > y1:
                    nodes.append((x1, y1, x2, y2))
    if not nodes:
        print(f"NOT FOUND: {label}", flush=True)
        return False
    x1, y1, x2, y2 = nodes[0]
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    print(f"TAPPED: {label}", flush=True)
    return True

def require_tap(label, xml):
    if not tap(label, xml):
        failures.append(f"Could not locate {label}")
        return False
    return True

def report(name, xml):
    nodes = ET.fromstring(xml).iter("node")
    labels = [n.get("text") or n.get("content-desc") for n in nodes]
    print(f"{name}: " + " | ".join(x for x in labels if x)[:1800], flush=True)

adb("shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
adb("shell", "wm", "dismiss-keyguard", check=False)
adb("install", "-r", "target-app/app/build/outputs/apk/debug/app-debug.apk")
adb("shell", "am", "start", "-n", "com.urfavxbf.kanade/.MainActivity")
time.sleep(10)
xml = snapshot("01-launch")
report("launch", xml)
if tap("Continue as guest", xml) or tap("Continue as Guest", xml):
    time.sleep(4)
xml = snapshot("02-home")
report("home", xml)
if require_tap("Search", xml):
    xml = snapshot("03-search")
    report("search", xml)
if require_tap("Library", xml):
    xml = snapshot("04-library")
    report("library", xml)
    for permission in ("Grant music permission", "Allow", "Allow access to music and audio"):
        if tap(permission, xml):
            time.sleep(2)
            xml = snapshot("04-library-permission")
    report("library-after-permission", xml)
if require_tap("Playlists", xml):
    xml = snapshot("05-playlists")
    report("playlists", xml)
if require_tap("Home", xml):
    xml = snapshot("06-home")
    if tap("Settings", xml):
        xml = snapshot("07-settings")
        report("settings", xml)
    else:
        failures.append("Could not locate Settings")

(OUT / "summary.txt").write_text("\n".join(failures) if failures else "Navigation controls were found and tapped. Screenshots require visual review.\n")
if failures:
    raise SystemExit("\n".join(failures))
