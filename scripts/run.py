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

import secrets
import base64

def setup_release_signing():
    keystore_path = "app/moodyroutine-release.jks"
    alias = "moodyroutine"
    
    # Check if already exists or generate
    if not os.path.exists(keystore_path):
        password = secrets.token_urlsafe(24)
        print("[*] Generating release keystore...")
        dname = "CN=ndev-hoster, OU=MoodyRoutine, O=ndev, L=Bangalore, ST=Karnataka, C=IN"
        cmd = f'keytool -genkeypair -v -keystore {keystore_path} -alias {alias} -keyalg RSA -keysize 2048 -validity 10000 -storepass "{password}" -keypass "{password}" -dname "{dname}"'
        res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        if res.returncode != 0:
            print(f"[ERROR] keytool failed: {res.stderr}")
            return
        print("[*] Keystore generated successfully.")
        
        # Write keystore.properties locally
        with open("keystore.properties", "w") as f:
            f.write(f"storeFile=moodyroutine-release.jks\nstorePassword={password}\nkeyAlias={alias}\nkeyPassword={password}\n")
        print("[*] Saved local keystore.properties")
    else:
        print("[*] Keystore already exists at", keystore_path)
        password = None
        if os.path.exists("keystore.properties"):
            with open("keystore.properties") as f:
                for line in f:
                    if line.startswith("storePassword="):
                        password = line.strip().split("=", 1)[1]
    
    if not password:
        print("[ERROR] Password not found.")
        return

    # Base64 encode
    with open(keystore_path, "rb") as f:
        b64_keystore = base64.b64encode(f.read()).decode("utf-8")

    # Set GitHub Secrets
    print("[*] Setting GitHub Secrets via gh CLI...")
    subprocess.run(f'gh secret set KEYSTORE_BASE64 -b"{b64_keystore}"', shell=True, check=True)
    subprocess.run(f'gh secret set KEYSTORE_PASSWORD -b"{password}"', shell=True, check=True)
    subprocess.run(f'gh secret set KEY_ALIAS -b"{alias}"', shell=True, check=True)
    subprocess.run(f'gh secret set KEY_PASSWORD -b"{password}"', shell=True, check=True)
    print("[SUCCESS] All 4 GitHub Secrets configured successfully!")

def main():
    print("[*] Testing onboarding dialog with Modify System Settings...")
    adb(f"shell pm clear {PKG}")
    time.sleep(1.0)
    adb(f"shell am start -n {PKG}/com.ndev.moodyroutine.MainActivity")
    time.sleep(2.0)
    print("[*] UI on fresh launch:")
    dump_ui()
    screenshot("onboarding_with_write_settings.png")

if __name__ == "__main__":
    main()
