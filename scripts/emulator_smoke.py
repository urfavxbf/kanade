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
subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "lavfi", "-i", "sine=frequency=440:duration=90", "-metadata", "title=Kanade Test Tone", "-metadata", "artist=Test Artist", "-metadata", "album=Test Album", "-y", "/tmp/kanade-test-tone.mp3"], check=True)
adb("push", "/tmp/kanade-test-tone.mp3", "/sdcard/Music/kanade-test-tone.mp3")
adb("shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", "file:///sdcard/Music/kanade-test-tone.mp3", check=False)
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
    if tap("Scan / allow", xml):
        time.sleep(2)
        xml = snapshot("04-library-permission")
        report("permission-dialog", xml)
        for permission in ("Allow", "Allow access to music and audio"):
            if tap(permission, xml):
                time.sleep(4)
                xml = snapshot("04-library-allowed")
                break
    report("library-after-permission", xml)
    if tap("Kanade Test Tone", xml) or tap("kanade-test-tone", xml):
        time.sleep(3)
        xml = snapshot("04-playing")
        report("playing", xml)
        if tap("Open now playing", xml):
            xml = snapshot("04-player")
            report("player", xml)
            adb("shell", "input", "keyevent", "KEYCODE_BACK")
            xml = snapshot("04-after-player")
        else:
            failures.append("Could not find the mini-player to open the player")
    else:
        failures.append("Synthetic local test song not found")

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


def selected(label, xml):
    for node in ET.fromstring(xml).iter("node"):
        if node.get("selected") == "true" or node.get("checked") == "true":
            if any(label in (child.get("text"), child.get("content-desc")) for child in node.iter("node")):
                return True
    return False

def verify_style(label, xml):
    if not selected(label, xml):
        failures.append(f"{label} is not the selected control style")
    else:
        print(f"Verified selected control style: {label}", flush=True)

verify_style("Glass", xml)
require_tap("Soft Raised", xml)
xml = snapshot("08-soft-raised-light")
verify_style("Soft Raised", xml)
require_tap("Dark", xml)
xml = snapshot("09-soft-raised-dark")
adb("shell", "am", "force-stop", "com.urfavxbf.kanade")
adb("shell", "am", "start", "-n", "com.urfavxbf.kanade/.MainActivity")
time.sleep(5)
xml = snapshot("10-relaunch")
if tap("Continue as guest", xml):
    xml = snapshot("10-guest")
if require_tap("Settings", xml):
    xml = snapshot("11-persisted")
    verify_style("Soft Raised", xml)
    require_tap("Glass", xml)
    verify_style("Glass", snapshot("12-restored-glass"))

(OUT / "summary.txt").write_text("\n".join(failures) if failures else "Navigation controls were found and tapped. Screenshots require visual review.\n")
if failures:
    raise SystemExit("\n".join(failures))
