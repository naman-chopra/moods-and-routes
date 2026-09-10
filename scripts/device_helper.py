#!/usr/bin/env python3
"""
MoodyRoutine Device Helper & Automation Diagnostic Tool
Reusable script for ADB management, one-click permission granting, and routine/mode diagnostics.
"""

import os
import sys
import argparse
import subprocess
import time
import json
import sqlite3
import math

DEFAULT_PKG = "com.ndev.moodyroutine.debug"
DB_LOCAL = "/tmp/moody_routine_diag.db"

def get_connected_device():
    res = subprocess.run("adb devices", shell=True, capture_output=True, text=True)
    lines = [line.strip().split() for line in res.stdout.strip().splitlines()[1:] if line.strip()]
    devices = [line[0] for line in lines if len(line) >= 2 and line[1] == "device"]
    if not devices:
        print("[ERROR] No ADB devices connected or authorized.")
        sys.exit(1)
    return devices[0]

def run_adb(cmd, serial=None):
    s = f"-s {serial} " if serial else ""
    full_cmd = f"adb {s}{cmd}"
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip(), res.stderr.strip(), res.returncode

def haversine_distance(lat1, lon1, lat2, lon2):
    R = 6371000  # meters
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    a = math.sin(delta_phi / 2.0) ** 2 + \
        math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2.0) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

def grant_all_permissions(serial, pkg=DEFAULT_PKG):
    print(f"[*] Granting all permissions for '{pkg}' on device {serial}...")

    # Standard runtime permissions
    runtime_perms = [
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.BLUETOOTH_CONNECT"
    ]
    for perm in runtime_perms:
        out, err, code = run_adb(f"shell pm grant {pkg} {perm}", serial)
        status = "OK" if code == 0 else f"ERR ({err})"
        print(f"  - pm grant {perm.split('.')[-1]}: {status}")

    # AppOps permissions (special access)
    appops = [
        ("WRITE_SETTINGS", "allow"),
        ("GET_USAGE_STATS", "allow"),
        ("SYSTEM_ALERT_WINDOW", "allow")
    ]
    for op, mode in appops:
        out, err, code = run_adb(f"shell appops set {pkg} {op} {mode}", serial)
        status = "OK" if code == 0 else f"ERR ({err})"
        print(f"  - appops set {op} {mode}: {status}")

    # Battery optimization exemption
    run_adb(f"shell dumpsys deviceidle whitelist +{pkg}", serial)
    out_idle, _, _ = run_adb(f"shell dumpsys deviceidle whitelist | grep {pkg}", serial)
    idle_ok = pkg in out_idle
    print(f"  - Battery optimization whitelist: {'OK (Exempt)' if idle_ok else 'WARN'}")

    print("[SUCCESS] All permissions granted! The app will not prompt for permissions again.")

def diagnose(serial, pkg=DEFAULT_PKG):
    print("=" * 65)
    print(f"MOODY ROUTINE DIAGNOSTICS: Device={serial}, Package={pkg}")
    print("=" * 65)

    # 1. Permissions & Battery
    print("\n--- 1. Permission & Battery Whitelist State ---")
    dumpsys_pkg, _, _ = run_adb(f"shell dumpsys package {pkg}", serial)
    runtime_perms = [
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.BLUETOOTH_CONNECT"
    ]
    for p in runtime_perms:
        name = p.split(".")[-1]
        granted = f"{p}: granted=true" in dumpsys_pkg
        print(f"  {name:<30} : {'GRANTED' if granted else 'DENIED'}")

    appops = ["WRITE_SETTINGS", "GET_USAGE_STATS", "SYSTEM_ALERT_WINDOW"]
    for op in appops:
        out, _, _ = run_adb(f"shell appops get {pkg} {op}", serial)
        allowed = "allow" in out.lower()
        print(f"  {op:<30} : {'ALLOWED' if allowed else 'DENIED'}")

    ignoring_batt, _, _ = run_adb(f"shell dumpsys deviceidle whitelist | grep {pkg}", serial)
    print(f"  {'Ignoring Battery Optimizations':<30} : {'YES (Exempt)' if pkg in ignoring_batt else 'NO (Restricted)'}")

    # 2. Device Location
    print("\n--- 2. Current Device GPS Location ---")
    loc_out, _, _ = run_adb("shell dumpsys location | grep -E 'last location|fused.*Location\\[' | head -n 3", serial)
    print("  Dumpsys location snippet:")
    for line in loc_out.splitlines():
        print("   ", line.strip())

    # 3. Pull database
    print("\n--- 3. Database Inspection ---")
    run_adb(f"shell run-as {pkg} cat databases/moody_routine_database > {DB_LOCAL}", serial)
    run_adb(f"shell run-as {pkg} cat databases/moody_routine_database-wal > {DB_LOCAL}-wal", serial)
    run_adb(f"shell run-as {pkg} cat databases/moody_routine_database-shm > {DB_LOCAL}-shm", serial)

    if not os.path.exists(DB_LOCAL) or os.path.getsize(DB_LOCAL) == 0:
        print("  [WARN] Database file not found or empty.")
        return

    conn = sqlite3.connect(DB_LOCAL)
    cur = conn.cursor()

    cur.execute("SELECT id, name, isEnabled, isActive, triggers, triggerMatchType FROM routines")
    routines = cur.fetchall()
    print(f"Found {len(routines)} Routine(s):")
    for r in routines:
        r_id, r_name, r_en, r_act, r_trig, r_match = r
        print(f"  * Routine #{r_id} '{r_name}' (Enabled: {bool(r_en)}, Active: {bool(r_act)}, Match: {r_match})")
        trigs = json.loads(r_trig)
        for t in trigs:
            params = t.get("params", {})
            lat = params.get("latitude")
            lng = params.get("longitude")
            loc_name = params.get("locationName")
            print(f"    - Trigger: {t.get('type')}, Location: '{loc_name}', Lat/Lng: {lat}, {lng}, Params: {params}")

    cur.execute("SELECT id, name, isEnabled, isActive, autoTriggers FROM modes")
    modes = cur.fetchall()
    print(f"\nFound {len(modes)} Mode(s):")
    for m in modes:
        m_id, m_name, m_en, m_act, m_trig = m
        print(f"  * Mode #{m_id} '{m_name}' (Enabled: {bool(m_en)}, Active: {bool(m_act)})")
        trigs = json.loads(m_trig)
        for t in trigs:
            params = t.get("params", {})
            lat = params.get("latitude")
            lng = params.get("longitude")
            loc_name = params.get("locationName")
            print(f"    - Trigger: {t.get('type')}, Location: '{loc_name}', Lat/Lng: {lat}, {lng}, Params: {params}")

    # Delhi Kartavya Path coords: 28.6143, 77.2090
    print("\n--- 4. Distance to Target Locations ---")
    delhi_lat, delhi_lng = 28.6143, 77.2090
    for r in routines:
        for t in json.loads(r[4]):
            p = t.get("params", {})
            if "latitude" in p and "longitude" in p:
                lat = float(p["latitude"])
                lng = float(p["longitude"])
                name = p.get("locationName", "")
                dist_delhi = haversine_distance(lat, lng, delhi_lat, delhi_lng) / 1000.0
                print(f"  Routine '{r[1]}' target '{name}': Lat={lat:.5f}, Lng={lng:.5f}")
                print(f"    Distance from Kartavya Path (Delhi): {dist_delhi:.1f} km")

    for m in modes:
        for t in json.loads(m[4]):
            p = t.get("params", {})
            if "latitude" in p and "longitude" in p:
                lat = float(p["latitude"])
                lng = float(p["longitude"])
                name = p.get("locationName", "")
                dist_delhi = haversine_distance(lat, lng, delhi_lat, delhi_lng) / 1000.0
                print(f"  Mode '{m[1]}' target '{name}': Lat={lat:.5f}, Lng={lng:.5f}")
                print(f"    Distance from Kartavya Path (Delhi): {dist_delhi:.1f} km")

def fix_target_coordinates(serial, pkg=DEFAULT_PKG):
    print(f"[*] Updating 'Kartavya Path' coordinates in {pkg} database on device {serial}...")
    run_adb(f"shell run-as {pkg} cat databases/moody_routine_database > {DB_LOCAL}", serial)
    run_adb(f"shell run-as {pkg} cat databases/moody_routine_database-wal > {DB_LOCAL}-wal", serial)
    run_adb(f"shell run-as {pkg} cat databases/moody_routine_database-shm > {DB_LOCAL}-shm", serial)

    conn = sqlite3.connect(DB_LOCAL)
    cur = conn.cursor()

    delhi_lat = 28.6143
    delhi_lng = 77.2090

    # Fix routines
    cur.execute("SELECT id, triggers FROM routines")
    routines = cur.fetchall()
    updated_routines = 0
    for r_id, r_trig in routines:
        trigs = json.loads(r_trig)
        changed = False
        for t in trigs:
            p = t.get("params", {})
            if "Kartavya Path" in p.get("locationName", ""):
                p["latitude"] = str(delhi_lat)
                p["longitude"] = str(delhi_lng)
                p["address"] = "Kartavya Path, New Delhi, Delhi, India"
                changed = True
        if changed:
            cur.execute("UPDATE routines SET triggers = ?, isActive = 0 WHERE id = ?", (json.dumps(trigs), r_id))
            updated_routines += 1

    # Fix modes
    cur.execute("SELECT id, autoTriggers FROM modes")
    modes = cur.fetchall()
    updated_modes = 0
    for m_id, m_trig in modes:
        trigs = json.loads(m_trig)
        changed = False
        for t in trigs:
            p = t.get("params", {})
            if "Kartavya Path" in p.get("locationName", ""):
                p["latitude"] = str(delhi_lat)
                p["longitude"] = str(delhi_lng)
                p["address"] = "Kartavya Path, New Delhi, Delhi, India"
                changed = True
        if changed:
            cur.execute("UPDATE modes SET autoTriggers = ?, isActive = 0 WHERE id = ?", (json.dumps(trigs), m_id))
            updated_modes += 1

    conn.commit()
    conn.close()

    # Push back to device
    print(f"[*] Updated {updated_routines} routine(s) and {updated_modes} mode(s). Pushing to device...")
    run_adb(f"push {DB_LOCAL} /data/local/tmp/moody_routine_database", serial)
    run_adb(f"shell run-as {pkg} cp /data/local/tmp/moody_routine_database databases/moody_routine_database", serial)
    run_adb(f"shell run-as {pkg} rm -f databases/moody_routine_database-wal databases/moody_routine_database-shm", serial)
    run_adb(f"shell am force-stop {pkg}", serial)
    time.sleep(0.5)
    run_adb(f"shell monkey -p {pkg} -c android.intent.category.LAUNCHER 1", serial)
    print("[SUCCESS] Database updated with Delhi coordinates (28.6143, 77.2090) and app restarted.")

def toggle_service(serial, pkg=DEFAULT_PKG):
    print(f"[*] Testing Automation Service toggle on {serial}...")
    run_adb("logcat -c", serial)
    print("  - Toggling OFF...")
    run_adb(f"shell am start-foreground-service -a com.ndev.moodyroutine.TOGGLE_SERVICE {pkg}/com.ndev.moodyroutine.service.AutomationService", serial)
    time.sleep(1.0)
    print("  - Toggling ON...")
    run_adb(f"shell am start-foreground-service -a com.ndev.moodyroutine.TOGGLE_SERVICE {pkg}/com.ndev.moodyroutine.service.AutomationService", serial)
    time.sleep(2.5)

    print("\n--- Service & Automation Logs ---")
    logs, _, _ = run_adb("logcat -d -v time | grep -E 'AutomationService|AutomationEngine|LocationTracker|ActionExecutor|ConditionEvaluator'", serial)
    lines = logs.splitlines()
    if lines:
        for line in lines[-25:]:
            print("  ", line)
    else:
        print("  (No automation logs captured)")

def parse_bounds(bounds_str):
    # e.g. [68,1438][652,1532]
    import re
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds_str)
    if m:
        x1, y1, x2, y2 = map(int, m.groups())
        return (x1 + x2) // 2, (y1 + y2) // 2, x1, y1, x2, y2
    return None, None, 0, 0, 0, 0

def dump_ui(serial):
    import xml.etree.ElementTree as ET
    print(f"[*] Dumping UI hierarchy from device {serial}...")
    run_adb("shell uiautomator dump /sdcard/ui_dump.xml", serial)
    run_adb("pull /sdcard/ui_dump.xml /tmp/ui_dump.xml", serial)
    if not os.path.exists("/tmp/ui_dump.xml"):
        print("[ERROR] Failed to pull /tmp/ui_dump.xml")
        return []
    tree = ET.parse("/tmp/ui_dump.xml")
    elements = []
    for node in tree.getroot().iter():
        text = node.attrib.get("text", "")
        desc = node.attrib.get("content-desc", "")
        bounds = node.attrib.get("bounds", "")
        clazz = node.attrib.get("class", "").split(".")[-1]
        cx, cy, x1, y1, x2, y2 = parse_bounds(bounds)
        if text or desc or "Button" in clazz:
            elements.append({"text": text, "desc": desc, "bounds": bounds, "class": clazz, "cx": cx, "cy": cy})
            print(f"[{clazz}] text='{text}' desc='{desc}' center=({cx}, {cy}) bounds={bounds}")
    return elements

def take_screenshot(serial, out_path="/tmp/device_screenshot.png"):
    print(f"[*] Capturing screenshot from device {serial}...")
    run_adb("shell screencap -p /sdcard/screen.png", serial)
    run_adb(f"pull /sdcard/screen.png {out_path}", serial)
    print(f"[SUCCESS] Screenshot saved to {out_path}")
    # Also copy to brain artifacts directory if exists
    artifacts_dir = "/home/kratoes/.gemini/antigravity-cli/brain/62a5724a-2255-4747-8dce-08d7df130e7f"
    if os.path.exists(artifacts_dir):
        dest = os.path.join(artifacts_dir, os.path.basename(out_path))
        subprocess.run(f"cp {out_path} {dest}", shell=True)
        print(f"[*] Copied to artifacts: {dest}")
    return out_path

def tap_coords(serial, x, y):
    print(f"[*] Tapping coordinates ({x}, {y}) on device {serial}...")
    out, err, code = run_adb(f"shell input tap {x} {y}", serial)
    return code == 0

def tap_element(serial, query):
    elements = dump_ui(serial)
    query_lower = query.lower()
    for el in elements:
        if query_lower in el["text"].lower() or query_lower in el["desc"].lower():
            cx, cy = el["cx"], el["cy"]
            print(f"[*] Found match for '{query}': '{el['text'] or el['desc']}' at ({cx}, {cy})")
            tap_coords(serial, cx, cy)
            return True
    print(f"[WARN] No element found matching '{query}'")
    return False

def main():
    parser = argparse.ArgumentParser(description="MoodyRoutine Device Management & Automation Diagnostics")
    parser.add_argument("action", choices=[
        "grant-permissions", "diagnose", "fix-coords", "toggle-service",
        "dump-ui", "screenshot", "tap", "tap-element"
    ], help="Action to perform")
    parser.add_argument("--serial", "-s", help="ADB device serial", default=None)
    parser.add_argument("--pkg", "-p", help="Package name", default=DEFAULT_PKG)
    parser.add_argument("--out", "-o", help="Output file path for screenshot", default="/tmp/device_screenshot.png")
    parser.add_argument("--query", "-q", help="Query text/desc for tap-element", default="")
    parser.add_argument("coords", nargs="*", type=int, help="X Y coordinates for tap")

    args = parser.parse_args()
    serial = args.serial or get_connected_device()

    if args.action == "grant-permissions":
        grant_all_permissions(serial, args.pkg)
    elif args.action == "diagnose":
        diagnose(serial, args.pkg)
    elif args.action == "fix-coords":
        fix_target_coordinates(serial, args.pkg)
    elif args.action == "toggle-service":
        toggle_service(serial, args.pkg)
    elif args.action == "dump-ui":
        dump_ui(serial)
    elif args.action == "screenshot":
        take_screenshot(serial, args.out)
    elif args.action == "tap":
        if len(args.coords) >= 2:
            tap_coords(serial, args.coords[0], args.coords[1])
        else:
            print("[ERROR] 'tap' requires X and Y coordinates: tap X Y")
    elif args.action == "tap-element":
        if args.query:
            tap_element(serial, args.query)
        elif args.coords and len(args.coords) >= 1:
            tap_element(serial, str(args.coords[0]))
        else:
            print("[ERROR] 'tap-element' requires --query <text>")

if __name__ == "__main__":
    main()

