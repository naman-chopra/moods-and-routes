#!/usr/bin/env python3
import subprocess
import os
import time
import xml.etree.ElementTree as ET

SERIAL = "192.168.0.5:5555"
PKG = "com.ndev.moodyroutine.debug"
ARTIFACTS_DIR = "/home/kratoes/.gemini/antigravity-cli/brain/62a5724a-2255-4747-8dce-08d7df130e7f"

def adb(cmd):
    full = f"adb -s {SERIAL} {cmd}"
    res = subprocess.run(full, shell=True, capture_output=True, text=True)
    return res.stdout.strip(), res.stderr.strip(), res.returncode

def parse_bounds(bounds_str):
    import re
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds_str)
    if m:
        x1, y1, x2, y2 = map(int, m.groups())
        return (x1 + x2) // 2, (y1 + y2) // 2
    return None, None

def dump_ui():
    adb("shell uiautomator dump /sdcard/ui_dump.xml")
    adb("pull /sdcard/ui_dump.xml /tmp/ui_dump.xml")
    if not os.path.exists("/tmp/ui_dump.xml"):
        return []
    tree = ET.parse("/tmp/ui_dump.xml")
    elements = []
    for node in tree.getroot().iter():
        text = node.attrib.get("text", "")
        desc = node.attrib.get("content-desc", "")
        bounds = node.attrib.get("bounds", "")
        cx, cy = parse_bounds(bounds)
        if text or desc:
            elements.append({"text": text, "desc": desc, "bounds": bounds, "cx": cx, "cy": cy})
            print(f"  text='{text}' desc='{desc}' center=({cx}, {cy})")
    return elements

def tap(x, y):
    print(f"[*] Tapping ({x}, {y})...")
    adb(f"shell input tap {x} {y}")

def tap_query(query):
    print(f"[*] Searching for '{query}' in UI...")
    elements = dump_ui()
    for el in elements:
        if query.lower() in el["text"].lower() or query.lower() in el["desc"].lower():
            print(f"[*] Found '{el['text'] or el['desc']}' at ({el['cx']}, {el['cy']})")
            tap(el["cx"], el["cy"])
            return True
    print(f"[WARN] Not found: '{query}'")
    return False

def screenshot(name="screen.png"):
    adb("shell screencap -p /sdcard/screen.png")
    dest = f"/tmp/{name}"
    adb(f"pull /sdcard/screen.png {dest}")
    if os.path.exists(ARTIFACTS_DIR):
        art_dest = os.path.join(ARTIFACTS_DIR, name)
        subprocess.run(f"cp {dest} {art_dest}", shell=True)
    print(f"[*] Screenshot saved: {dest}")

def main():
    print("[*] Device Helper ready.")
    dump_ui()

if __name__ == "__main__":
    main()
