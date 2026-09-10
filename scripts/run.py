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
def generate_all_icons():
    output_dir = os.path.expanduser("~/Downloads/moody-routine-icons")
    os.makedirs(output_dir, exist_ok=True)
    
    # Clean up previous files
    for f in os.listdir(output_dir):
        if f.endswith(".svg") or f == "index.html":
            os.remove(os.path.join(output_dir, f))
    print(f"[*] Cleaned old files. Generating 7 SVG icons (2 kept + 5 innovative solid-color concepts) in {output_dir}...")

    icons = {}

    # 1. KEPT: The Topographic "M"
    icons["01-topographic-m.svg"] = {
        "title": "1. The Topographic 'M' (Kept)",
        "concept": "Elevation Contour Monogram",
        "desc": "Ultra-sleek letter 'M' sculpted from flowing elevation contour lines like a trail map, anchored by a summit waypoint.",
        "colors": ["#18181B", "#EA580C", "#FB923C", "#F8FAFC"],
        "is_solid": False,
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <defs>
    <linearGradient id="bg01" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#18181B"/>
      <stop offset="100%" stop-color="#09090B"/>
    </linearGradient>
    <linearGradient id="topoGrad01" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FB923C"/>
      <stop offset="100%" stop-color="#EA580C"/>
    </linearGradient>
    <filter id="glow01" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="10" stdDeviation="12" flood-color="#EA580C" flood-opacity="0.3"/>
    </filter>
  </defs>
  <rect width="512" height="512" rx="115" fill="url(#bg01)"/>
  <path d="M 120 370 L 120 180 Q 120 150 150 150 Q 175 150 195 180 L 256 270 L 317 180 Q 337 150 362 150 Q 392 150 392 180 L 392 370" 
        fill="none" stroke="#3F3F46" stroke-width="12" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M 150 370 L 150 205 Q 150 185 168 185 Q 185 185 200 205 L 256 290 L 312 205 Q 327 185 344 185 Q 362 185 362 205 L 362 370" 
        fill="none" stroke="#71717A" stroke-width="14" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M 180 370 L 180 230 Q 180 215 195 215 Q 210 215 220 230 L 256 285 L 292 230 Q 302 215 317 215 Q 332 215 332 230 L 332 370" 
        fill="none" stroke="url(#topoGrad01)" stroke-width="18" stroke-linecap="round" stroke-linejoin="round" filter="url(#glow01)"/>
  <circle cx="256" cy="285" r="10" fill="#FFFFFF"/>
  <circle cx="256" cy="285" r="5" fill="#EA580C"/>
</svg>"""
    }

    # 2. KEPT: The Zen Nexus
    icons["02-zen-nexus.svg"] = {
        "title": "2. The Zen Nexus (Kept)",
        "concept": "Mindfulness Lotus + Connected Nodes",
        "desc": "Geometric 3-petal lotus flower whose petals connect to circuit terminals, merging peaceful mindfulness with smart triggers.",
        "colors": ["#0D1117", "#818CF8", "#34D399", "#F1F5F9"],
        "is_solid": False,
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <defs>
    <linearGradient id="bg02" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0D1117"/>
      <stop offset="100%" stop-color="#161B22"/>
    </linearGradient>
    <linearGradient id="petalGrad02" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#818CF8"/>
      <stop offset="100%" stop-color="#34D399"/>
    </linearGradient>
    <filter id="glow02" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="8" result="blur"/>
      <feComposite in="SourceGraphic" in2="blur" operator="over"/>
    </filter>
  </defs>
  <rect width="512" height="512" rx="115" fill="url(#bg02)"/>
  <path d="M 256 120 C 220 180 220 280 256 340 C 292 280 292 180 256 120 Z" 
        fill="url(#petalGrad02)" opacity="0.85" filter="url(#glow02)"/>
  <path d="M 140 210 C 180 220 230 270 256 340 C 200 320 150 270 140 210 Z" 
        fill="#818CF8" opacity="0.75"/>
  <path d="M 372 210 C 332 220 282 270 256 340 C 312 320 362 270 372 210 Z" 
        fill="#34D399" opacity="0.75"/>
  <line x1="256" y1="120" x2="256" y2="85" stroke="#F1F5F9" stroke-width="4" stroke-linecap="round"/>
  <circle cx="256" cy="85" r="10" fill="#FFFFFF" filter="url(#glow02)"/>
  <line x1="140" y1="210" x2="105" y2="190" stroke="#818CF8" stroke-width="4" stroke-linecap="round"/>
  <circle cx="105" cy="190" r="9" fill="#818CF8"/>
  <line x1="372" y1="210" x2="407" y2="190" stroke="#34D399" stroke-width="4" stroke-linecap="round"/>
  <circle cx="407" cy="190" r="9" fill="#34D399"/>
  <circle cx="256" cy="340" r="14" fill="#FFFFFF"/>
  <circle cx="256" cy="340" r="7" fill="#161B22"/>
</svg>"""
    }

    # 3. NEW: The Dual-State Eclipse (SOLID FLAT)
    icons["03-dual-state-eclipse.svg"] = {
        "title": "3. The Dual-State Eclipse",
        "concept": "Negative Space / Day & Night Horizon",
        "desc": "Bold circular disc split dynamically by an organic S-route curve into Day and Night routine states, connected by a waypoint pivot.",
        "colors": ["#0F172A", "#1E293B", "#FBBF24", "#2563EB", "#FFFFFF"],
        "is_solid": True,
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <!-- Base Squircle -->
  <rect width="512" height="512" rx="115" fill="#0B0F17"/>

  <!-- Outer Dial Track -->
  <circle cx="256" cy="256" r="185" fill="none" stroke="#1E293B" stroke-width="4"/>
  <circle cx="256" cy="256" r="205" fill="none" stroke="#334155" stroke-width="2" stroke-dasharray="6 10"/>

  <!-- The Mathematically Balanced S-Curve Eclipse Disc -->
  <!-- Left Side: Night / Rest (Dark Slate) -->
  <path d="M 256 106 A 150 150 0 0 0 256 406 A 75 75 0 0 1 256 256 A 75 75 0 0 0 256 106 Z" fill="#1E293B"/>

  <!-- Right Side: Day / Routine (Golden Amber) -->
  <path d="M 256 106 A 150 150 0 0 1 256 406 A 75 75 0 0 1 256 256 A 75 75 0 0 0 256 106 Z" fill="#FBBF24"/>

  <!-- Night Sanctuary Waypoint (Silver Ring) -->
  <circle cx="256" cy="181" r="16" fill="#94A3B8"/>
  <circle cx="256" cy="181" r="7" fill="#0B0F17"/>

  <!-- Day Active Waypoint (Sun Ring) -->
  <circle cx="256" cy="331" r="16" fill="#EA580C"/>
  <circle cx="256" cy="331" r="7" fill="#FFFFFF"/>

  <!-- Center Pivot / Trigger Gateway -->
  <circle cx="256" cy="256" r="26" fill="#2563EB"/>
  <circle cx="256" cy="256" r="12" fill="#FFFFFF"/>
</svg>"""
    }

    # 4. NEW: The Crossroad Aperture (SOLID FLAT)
    icons["04-crossroad-aperture.svg"] = {
        "title": "4. The Crossroad Aperture",
        "concept": "Bauhaus Camera Iris / Geofence Intersection",
        "desc": "Four solid geometric chevron route blocks converging to form a camera-shutter aperture that opens upon entering a geofence.",
        "colors": ["#18181B", "#06B6D4", "#F59E0B", "#F43F5E", "#10B981", "#FFFFFF"],
        "is_solid": True,
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <!-- Solid Base Squircle -->
  <rect width="512" height="512" rx="115" fill="#18181B"/>

  <!-- Background Subtle Grid Cross -->
  <line x1="80" y1="256" x2="432" y2="256" stroke="#27272A" stroke-width="2"/>
  <line x1="256" y1="80" x2="256" y2="432" stroke="#27272A" stroke-width="2"/>

  <!-- 4 Converging Solid Route Blocks -->
  <!-- Top Block (Cyan - North / Focus) -->
  <polygon points="256,100 330,174 256,248 182,174" fill="#06B6D4"/>
  
  <!-- Right Block (Amber - East / Energy) -->
  <polygon points="412,256 338,330 264,256 338,182" fill="#F59E0B"/>

  <!-- Bottom Block (Rose - South / Relax) -->
  <polygon points="256,412 182,338 256,264 330,338" fill="#F43F5E"/>

  <!-- Left Block (Emerald - West / Routine Return) -->
  <polygon points="100,256 174,182 248,256 174,330" fill="#10B981"/>

  <!-- Central Nexus Core Aperture -->
  <!-- Dark Bevel Layer -->
  <rect x="220" y="220" width="72" height="72" rx="16" fill="#18181B" transform="rotate(45 256 256)"/>
  <!-- Pure Solid White Center Diamond -->
  <rect x="232" y="232" width="48" height="48" rx="8" fill="#FFFFFF" transform="rotate(45 256 256)"/>
  <!-- Target Dot in center -->
  <circle cx="256" cy="256" r="8" fill="#18181B"/>
</svg>"""
    }

    # 5. NEW: The Stepping Isometric (SOLID FLAT)
    icons["05-stepping-isometric.svg"] = {
        "title": "5. The Stepping Isometric",
        "concept": "Architectural Progress / Routine Milestones",
        "desc": "Three interlocking solid isometric step platforms rising toward a summit beacon, symbolizing daily routine progression.",
        "colors": ["#0D1117", "#3B82F6", "#10B981", "#F59E0B", "#FFFFFF"],
        "is_solid": True,
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <!-- Solid Base Squircle -->
  <rect width="512" height="512" rx="115" fill="#0D1117"/>

  <!-- Isometric Block 1 (Lower Left - Home / Start) -->
  <!-- Top Face -->
  <polygon points="170,270 230,235 170,200 110,235" fill="#60A5FA"/>
  <!-- Left Face -->
  <polygon points="110,235 170,270 170,330 110,295" fill="#1D4ED8"/>
  <!-- Right Face -->
  <polygon points="170,270 230,235 230,295 170,330" fill="#2563EB"/>

  <!-- Isometric Block 2 (Middle - Commute / Route) -->
  <!-- Top Face -->
  <polygon points="256,220 316,185 256,150 196,185" fill="#34D399"/>
  <!-- Left Face -->
  <polygon points="196,185 256,220 256,280 196,245" fill="#047857"/>
  <!-- Right Face -->
  <polygon points="256,220 316,185 316,245 256,280" fill="#059669"/>

  <!-- Isometric Block 3 (Peak - Work / Goal) -->
  <!-- Top Face -->
  <polygon points="342,170 402,135 342,100 282,135" fill="#FCD34D"/>
  <!-- Left Face -->
  <polygon points="282,135 342,170 342,230 282,195" fill="#B45309"/>
  <!-- Right Face -->
  <polygon points="342,170 402,135 402,195 342,230" fill="#D97706"/>

  <!-- Connecting Pathway Lines between steps -->
  <polyline points="170,200 256,150 342,100" fill="none" stroke="#FFFFFF" stroke-width="4" stroke-dasharray="6 6"/>

  <!-- Floating Peak Beacon Cube -->
  <polygon points="342,60 366,46 342,32 318,46" fill="#FFFFFF"/>
  <polygon points="318,46 342,60 342,80 318,66" fill="#CBD5E1"/>
  <polygon points="342,60 366,46 366,66 342,80" fill="#94A3B8"/>

  <!-- Anchor Waypoint Ring -->
  <circle cx="170" cy="235" r="8" fill="#FFFFFF"/>
</svg>"""
    }

    # 6. NEW: The Tangram Switchback (SOLID FLAT)
    icons["06-tangram-switchback.svg"] = {
        "title": "6. The Tangram Switchback",
        "concept": "Folded Ribbon Road / Mountain Route",
        "desc": "Crisp, flat-folded geometric route facets inspired by tangram origami, tracing sharp switchback turns to an arrival pin.",
        "colors": ["#111827", "#4338CA", "#7C3AED", "#EC4899", "#F59E0B", "#FFFFFF"],
        "is_solid": True,
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <!-- Solid Base Squircle -->
  <rect width="512" height="512" rx="115" fill="#111827"/>

  <!-- Ground Route Trace -->
  <polyline points="110,400 190,400 330,260 190,260 330,120 400,120" 
            fill="none" stroke="#1F2937" stroke-width="50" stroke-linecap="round" stroke-linejoin="round"/>

  <!-- Solid Folded Geometric Facets -->
  <!-- Segment 1: Bottom Entry (Indigo) -->
  <polygon points="120,420 220,420 180,380 120,380" fill="#4338CA"/>

  <!-- Segment 2: First Ascending Slope (Violet) -->
  <polygon points="180,380 220,420 350,290 310,250" fill="#7C3AED"/>

  <!-- Segment 3: Middle Switchback Plateau (Magenta / Rose) -->
  <polygon points="310,250 350,290 190,290 170,250" fill="#DB2777"/>

  <!-- Segment 4: Second Ascending Slope (Coral) -->
  <polygon points="170,250 190,290 350,130 310,90" fill="#F43F5E"/>

  <!-- Segment 5: Summit Straight (Warm Gold) -->
  <polygon points="310,90 350,130 400,130 400,90" fill="#F59E0B"/>

  <!-- Arrival Beacon Pin at End of Route -->
  <circle cx="400" cy="110" r="24" fill="#FFFFFF"/>
  <circle cx="400" cy="110" r="10" fill="#111827"/>

  <!-- Start Waypoint Dot -->
  <circle cx="120" cy="400" r="12" fill="#FFFFFF"/>
</svg>"""
    }

    # 7. NEW: The Zen Cadence Pendulum (SOLID FLAT)
    icons["07-zen-cadence-pendulum.svg"] = {
        "title": "7. The Zen Cadence Pendulum",
        "concept": "Metronome Rhythm / Harmonic Cycle",
        "desc": "Minimalist kinetic pendulum swinging in perfect cadence between daily routine waypoints, capturing the rhythm of automation.",
        "colors": ["#131722", "#1E293B", "#F97316", "#38BDF8", "#34D399", "#FFFFFF"],
        "is_solid": True,
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <!-- Solid Base Squircle -->
  <rect width="512" height="512" rx="115" fill="#131722"/>

  <!-- Outer Triangular Cadence Arch -->
  <polygon points="256,110 390,380 122,380" fill="#1A202C" stroke="#2D3748" stroke-width="4"/>

  <!-- Swing Arc Track -->
  <path d="M 170 340 A 180 180 0 0 0 342 340" fill="none" stroke="#4A5568" stroke-width="4" stroke-dasharray="6 8"/>

  <!-- Left Swing Target (Start / Cyan) -->
  <circle cx="170" cy="340" r="16" fill="#0284C7"/>
  <circle cx="170" cy="340" r="6" fill="#FFFFFF"/>

  <!-- Right Swing Target (Arrival / Mint) -->
  <circle cx="342" cy="340" r="16" fill="#059669"/>
  <circle cx="342" cy="340" r="6" fill="#FFFFFF"/>

  <!-- Pivoting Center Arm (White) -->
  <line x1="256" y1="140" x2="285" y2="330" stroke="#FFFFFF" stroke-width="8" stroke-linecap="round"/>

  <!-- Active Swinging Metronome Bob (Signal Orange) -->
  <circle cx="285" cy="330" r="32" fill="#F97316"/>
  <circle cx="285" cy="330" r="12" fill="#FFFFFF"/>

  <!-- Top Anchor Pivot -->
  <circle cx="256" cy="140" r="18" fill="#FFFFFF"/>
  <circle cx="256" cy="140" r="8" fill="#131722"/>
</svg>"""
    }

    # Write each SVG
    for filename, data in icons.items():
        filepath = os.path.join(output_dir, filename)
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(data["svg"].strip())
        print(f"  [+] Wrote {filename}")

    # Write interactive HTML preview gallery
    cards_html = ""
    for filename, data in icons.items():
        color_pills = "".join([f'<span style="display:inline-block; width:20px; height:20px; border-radius:50%; background:{c}; border:1px solid rgba(255,255,255,0.2); margin-right:6px;"></span>' for c in data["colors"]])
        if "Kept" in data["title"]:
            badge = '<span class="concept-badge" style="background: rgba(16, 185, 129, 0.2); color: #34D399; border: 1px solid rgba(16, 185, 129, 0.4);">FAVORITE (KEPT)</span>'
        else:
            badge = '<span class="concept-badge" style="background: rgba(249, 115, 22, 0.2); color: #FB923C; border: 1px solid rgba(249, 115, 22, 0.4);">NEW • SOLID COLOR</span>'
        
        cards_html += f"""
        <div class="card">
            <div class="icon-wrap">
                <img src="{filename}" alt="{data['title']}" width="180" height="180"/>
            </div>
            <div class="card-content">
                <h3>{data['title']}</h3>
                <div class="badge-wrap">{badge}</div>
                <div class="concept-title">{data['concept']}</div>
                <p>{data['desc']}</p>
                <div class="palette-row">
                    {color_pills}
                </div>
                <a href="{filename}" target="_blank" class="open-btn">Open SVG</a>
            </div>
        </div>
        """

    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MoodyRoutine - 7 Curated App Icon Options</title>
    <style>
        * {{ box-sizing: border-box; margin: 0; padding: 0; }}
        body {{
            background: #0B0F17;
            color: #F1F5F9;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            padding: 40px 24px;
            line-height: 1.5;
        }}
        .header {{
            text-align: center;
            max-width: 800px;
            margin: 0 auto 40px auto;
        }}
        .header h1 {{
            font-size: 2.4rem;
            font-weight: 800;
            background: linear-gradient(135deg, #38BDF8, #818CF8, #F97316);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 12px;
        }}
        .header p {{
            color: #94A3B8;
            font-size: 1.05rem;
        }}
        .grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
            gap: 28px;
            max-width: 1400px;
            margin: 0 auto;
        }}
        .card {{
            background: #161B26;
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 24px;
            padding: 24px;
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
            transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
        }}
        .card:hover {{
            transform: translateY(-4px);
            border-color: rgba(99, 102, 241, 0.5);
            box-shadow: 0 16px 32px rgba(0, 0, 0, 0.4);
        }}
        .icon-wrap {{
            margin-bottom: 20px;
            filter: drop-shadow(0 12px 24px rgba(0,0,0,0.5));
        }}
        .card-content {{
            width: 100%;
            display: flex;
            flex-direction: column;
            align-items: center;
            flex-grow: 1;
        }}
        .card h3 {{
            font-size: 1.22rem;
            margin-bottom: 8px;
            color: #FFFFFF;
        }}
        .badge-wrap {{
            margin-bottom: 6px;
        }}
        .concept-badge {{
            display: inline-block;
            border-radius: 12px;
            font-size: 0.76rem;
            font-weight: 700;
            letter-spacing: 0.5px;
            padding: 3px 10px;
        }}
        .concept-title {{
            font-size: 0.88rem;
            color: #CBD5E1;
            font-weight: 600;
            margin-bottom: 10px;
        }}
        .card p {{
            color: #94A3B8;
            font-size: 0.90rem;
            margin-bottom: 16px;
            flex-grow: 1;
        }}
        .palette-row {{
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 16px;
        }}
        .open-btn {{
            display: inline-block;
            background: #222938;
            color: #E2E8F0;
            text-decoration: none;
            padding: 8px 20px;
            border-radius: 12px;
            font-size: 0.88rem;
            font-weight: 600;
            border: 1px solid rgba(255, 255, 255, 0.1);
            transition: all 0.2s;
        }}
        .open-btn:hover {{
            background: #6366F1;
            color: #FFFFFF;
            border-color: #6366F1;
        }}
    </style>
</head>
<body>
    <div class="header">
        <h1>MoodyRoutine Icon Exploration</h1>
        <p>Featuring your 2 favorites + 5 brand new innovative concepts designed in <strong>crisp solid flat colors</strong> (zero gradients).</p>
    </div>
    <div class="grid">
        {cards_html}
    </div>
</body>
</html>
"""
    with open(os.path.join(output_dir, "index.html"), "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"  [+] Created interactive gallery: {os.path.join(output_dir, 'index.html')}")
    print("[SUCCESS] All 7 icons and gallery generated successfully!")

def generate_theme_studio():
    output_dir = os.path.expanduser("~/Downloads/moody-routine-theme-studio")
    os.makedirs(output_dir, exist_ok=True)
    alt_dir = os.path.expanduser("~/Downloads/moody-routine-icons")
    os.makedirs(alt_dir, exist_ok=True)

    html_code = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MoodyRoutine - Live Theme Studio & Color Picker</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;600&display=swap" rel="stylesheet">
    <style>
        :root {
            --app-bg: #08080A;
            --card-bg: #18181B;
            --card-border: #2A2A32;
            --popup-bg: #0C0C0E;
            --popup-border: #24242C;
            --bottom-bar-bg: #111115;
            --primary: #FF3B00;
            --primary-light: #FF6838;
            --primary-dark: #C42600;
            --contour-grey: #71717A;
            --text-main: #FFFFFF;
            --text-muted: #94949E;
            --switch-track-off: #2E2E38;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }
        
        body {
            background: #090B10;
            color: #E2E8F0;
            font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        /* Header Bar */
        .top-navbar {
            background: #11141D;
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);
            padding: 16px 28px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: sticky;
            top: 0;
            z-index: 100;
        }
        .brand {
            display: flex;
            align-items: center;
            gap: 14px;
        }
        .brand-icon-mini {
            width: 38px;
            height: 38px;
            border-radius: 10px;
            overflow: hidden;
            border: 1px solid rgba(255, 255, 255, 0.12);
        }
        .brand h1 {
            font-size: 1.25rem;
            font-weight: 800;
            color: #FFFFFF;
            letter-spacing: -0.3px;
        }
        .brand span {
            font-size: 0.8rem;
            font-weight: 600;
            background: rgba(255, 59, 0, 0.16);
            color: #FF6838;
            border: 1px solid rgba(255, 59, 0, 0.3);
            padding: 3px 10px;
            border-radius: 20px;
            margin-left: 8px;
        }

        .header-actions {
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .btn-action {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 9px 18px;
            border-radius: 12px;
            font-size: 0.88rem;
            font-weight: 700;
            cursor: pointer;
            transition: all 0.2s;
            border: none;
        }
        .btn-primary-action {
            background: linear-gradient(135deg, #FF3B00, #E62E00);
            color: #FFFFFF;
            box-shadow: 0 4px 16px rgba(255, 59, 0, 0.35);
        }
        .btn-primary-action:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 20px rgba(255, 59, 0, 0.5);
        }
        .btn-secondary-action {
            background: #1E2330;
            color: #CBD5E1;
            border: 1px solid rgba(255, 255, 255, 0.1);
        }
        .btn-secondary-action:hover {
            background: #282F40;
            color: #FFFFFF;
        }

        /* Main Workspace Layout */
        .workspace {
            display: grid;
            grid-template-columns: 380px 1fr;
            flex-grow: 1;
            height: calc(100vh - 71px);
            overflow: hidden;
        }

        /* Sidebar Controls */
        .control-panel {
            background: #0E1118;
            border-right: 1px solid rgba(255, 255, 255, 0.08);
            overflow-y: auto;
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 24px;
        }
        .section-header {
            font-size: 0.78rem;
            font-weight: 800;
            text-transform: uppercase;
            letter-spacing: 1.2px;
            color: #71717A;
            margin-bottom: 12px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        /* Presets */
        .presets-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
        }
        .preset-chip {
            background: #161A23;
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 12px;
            padding: 10px 12px;
            cursor: pointer;
            transition: all 0.2s;
            text-align: left;
        }
        .preset-chip:hover {
            border-color: #FF3B00;
            background: #1C2230;
        }
        .preset-chip.active {
            border-color: #FF3B00;
            background: rgba(255, 59, 0, 0.12);
        }
        .preset-name {
            font-size: 0.82rem;
            font-weight: 700;
            color: #F1F5F9;
            margin-bottom: 6px;
        }
        .preset-dots {
            display: flex;
            gap: 5px;
        }
        .preset-dot {
            width: 14px;
            height: 14px;
            border-radius: 50%;
            border: 1px solid rgba(255, 255, 255, 0.2);
        }

        /* Color Control Items */
        .color-group {
            display: flex;
            flex-direction: column;
            gap: 14px;
        }
        .color-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: #141822;
            border: 1px solid rgba(255, 255, 255, 0.06);
            border-radius: 14px;
            padding: 12px 14px;
            transition: border-color 0.2s;
        }
        .color-row:hover {
            border-color: rgba(255, 255, 255, 0.15);
        }
        .color-info {
            display: flex;
            flex-direction: column;
            gap: 3px;
        }
        .color-title {
            font-size: 0.88rem;
            font-weight: 700;
            color: #FFFFFF;
        }
        .color-desc {
            font-size: 0.74rem;
            color: #8E96A5;
        }
        .picker-box {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .color-input-wrapper {
            width: 36px;
            height: 36px;
            border-radius: 10px;
            overflow: hidden;
            border: 2px solid rgba(255, 255, 255, 0.2);
            cursor: pointer;
            position: relative;
            flex-shrink: 0;
        }
        .color-input-wrapper input[type="color"] {
            position: absolute;
            top: -10px;
            left: -10px;
            width: 60px;
            height: 60px;
            border: none;
            cursor: pointer;
        }
        .hex-input {
            width: 82px;
            background: #0B0E14;
            border: 1px solid rgba(255, 255, 255, 0.12);
            border-radius: 8px;
            padding: 6px 8px;
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.82rem;
            color: #F8FAFC;
            text-align: center;
            text-transform: uppercase;
        }
        .hex-input:focus {
            outline: none;
            border-color: #FF3B00;
        }

        /* Right Canvas Area */
        .preview-canvas {
            background: #07090D;
            overflow-y: auto;
            padding: 32px;
            display: flex;
            flex-direction: column;
            gap: 36px;
        }
        .canvas-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 8px;
        }
        .canvas-title {
            font-size: 1.35rem;
            font-weight: 800;
            color: #FFFFFF;
        }
        .canvas-subtitle {
            font-size: 0.88rem;
            color: #94A3B8;
        }

        /* Mockup Devices Row */
        .devices-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 28px;
            align-items: start;
        }

        /* Realistic Phone Frame */
        .phone-frame {
            background: #000000;
            border: 8px solid #20242D;
            border-radius: 46px;
            box-shadow: 0 24px 50px rgba(0, 0, 0, 0.7), 0 0 0 1px rgba(255, 255, 255, 0.08);
            overflow: hidden;
            display: flex;
            flex-direction: column;
            height: 640px;
            position: relative;
            user-select: none;
        }
        .device-label {
            font-size: 0.82rem;
            font-weight: 700;
            color: #94A3B8;
            margin-bottom: 10px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        /* Android System Bar */
        .phone-status-bar {
            height: 36px;
            background: var(--app-bg);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 22px;
            font-size: 0.76rem;
            font-weight: 700;
            color: var(--text-main);
            flex-shrink: 0;
            z-index: 10;
        }
        .status-icons {
            display: flex;
            align-items: center;
            gap: 6px;
        }

        /* Phone Screen Content */
        .screen-content {
            background: var(--app-bg);
            flex-grow: 1;
            overflow-y: auto;
            padding: 16px 18px 80px 18px;
            display: flex;
            flex-direction: column;
            gap: 14px;
            position: relative;
        }
        .screen-title {
            font-size: 1.8rem;
            font-weight: 800;
            color: var(--text-main);
            margin: 4px 0 10px 0;
            letter-spacing: -0.5px;
        }

        /* Moody Card (Pure Neutral Grey) */
        .ui-card {
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 20px;
            padding: 16px;
            display: flex;
            align-items: center;
            gap: 14px;
            transition: background 0.15s, border-color 0.15s;
        }
        .card-icon-wrap {
            width: 44px;
            height: 44px;
            border-radius: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
            font-size: 1.15rem;
        }
        .card-body {
            flex-grow: 1;
        }
        .card-title {
            font-size: 0.95rem;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 2px;
        }
        .card-desc {
            font-size: 0.74rem;
            color: var(--text-muted);
            line-height: 1.35;
        }

        /* Toggle Switch in Highlight */
        .ui-switch {
            width: 48px;
            height: 28px;
            border-radius: 14px;
            background: var(--switch-track-off);
            position: relative;
            cursor: pointer;
            flex-shrink: 0;
            transition: background 0.2s;
        }
        .ui-switch.active {
            background: var(--primary);
        }
        .ui-switch-thumb {
            width: 22px;
            height: 22px;
            border-radius: 50%;
            background: #FFFFFF;
            position: absolute;
            top: 3px;
            left: 3px;
            transition: transform 0.2s;
            box-shadow: 0 2px 4px rgba(0,0,0,0.3);
        }
        .ui-switch.active .ui-switch-thumb {
            transform: translateX(20px);
        }

        /* FAB Button */
        .ui-fab {
            position: absolute;
            bottom: 74px;
            right: 18px;
            width: 52px;
            height: 52px;
            border-radius: 50%;
            background: var(--primary);
            color: #FFFFFF;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.7rem;
            font-weight: 400;
            box-shadow: 0 8px 24px rgba(0,0,0,0.5);
            z-index: 15;
            cursor: pointer;
            transition: transform 0.2s, background 0.15s;
        }
        .ui-fab:hover {
            transform: scale(1.05);
        }

        /* Bottom Bar */
        .ui-bottom-bar {
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            height: 64px;
            background: var(--bottom-bar-bg);
            border-top: 1px solid var(--card-border);
            display: flex;
            align-items: center;
            justify-content: space-around;
            padding: 0 10px;
            z-index: 20;
        }
        .tab-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 3px;
            font-size: 0.72rem;
            font-weight: 600;
            color: var(--contour-grey);
            cursor: pointer;
            transition: color 0.15s;
        }
        .tab-item.active {
            color: var(--primary);
            font-weight: 800;
        }
        .tab-icon {
            font-size: 1.25rem;
        }

        /* Routines Empty State */
        .empty-routines-wrap {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            text-align: center;
            padding: 80px 20px 20px 20px;
            flex-grow: 1;
        }
        .empty-sparkle-circle {
            width: 80px;
            height: 80px;
            border-radius: 50%;
            background: rgba(255, 59, 0, 0.15);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 2.2rem;
            color: var(--primary);
            margin-bottom: 24px;
        }
        .empty-title {
            font-size: 1.25rem;
            font-weight: 800;
            color: var(--text-main);
            margin-bottom: 8px;
        }
        .empty-desc {
            font-size: 0.8rem;
            color: var(--text-muted);
            line-height: 1.45;
            max-width: 240px;
            margin-bottom: 24px;
        }
        .btn-add-routine {
            background: var(--primary);
            color: #FFFFFF;
            border: none;
            padding: 12px 28px;
            border-radius: 24px;
            font-size: 0.90rem;
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            cursor: pointer;
            box-shadow: 0 4px 14px rgba(0,0,0,0.3);
            transition: all 0.2s;
        }

        /* Settings Specific */
        .section-label-settings {
            font-size: 0.82rem;
            font-weight: 700;
            color: var(--primary);
            margin-top: 10px;
            margin-bottom: -4px;
        }
        .badge-status {
            font-size: 0.68rem;
            font-weight: 700;
            padding: 3px 8px;
            border-radius: 8px;
        }
        .badge-action {
            background: rgba(239, 68, 68, 0.18);
            color: #EF4444;
            border: 1px solid rgba(239, 68, 68, 0.3);
        }
        .badge-granted {
            background: rgba(16, 185, 129, 0.18);
            color: #10B981;
            border: 1px solid rgba(16, 185, 129, 0.3);
        }

        /* Popup / Dialog Mockup */
        .dialog-backdrop {
            position: absolute;
            inset: 0;
            background: rgba(0, 0, 0, 0.75);
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 16px;
            z-index: 30;
        }
        .dialog-card {
            background: var(--popup-bg);
            border: 1px solid var(--popup-border);
            border-radius: 28px;
            padding: 20px;
            width: 100%;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.85);
            display: flex;
            flex-direction: column;
            gap: 14px;
        }
        .dialog-header {
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .dialog-header-icon {
            width: 42px;
            height: 42px;
            border-radius: 12px;
            background: rgba(255, 59, 0, 0.16);
            color: var(--primary);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.25rem;
        }
        .dialog-item {
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 14px;
            padding: 10px 12px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .btn-dialog-primary {
            background: var(--primary);
            color: #FFFFFF;
            border: none;
            padding: 12px;
            border-radius: 14px;
            font-size: 0.88rem;
            font-weight: 700;
            width: 100%;
            cursor: pointer;
            text-align: center;
        }
        .btn-dialog-skip {
            color: var(--text-muted);
            font-size: 0.78rem;
            font-weight: 600;
            text-align: center;
            cursor: pointer;
            padding-top: 4px;
        }

        /* Topographic M Icon Preview Box */
        .icon-preview-card {
            background: #11141D;
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 24px;
            padding: 24px;
            display: flex;
            align-items: center;
            gap: 28px;
        }
        .icon-svg-container {
            width: 110px;
            height: 110px;
            border-radius: 24px;
            overflow: hidden;
            box-shadow: 0 12px 32px rgba(0, 0, 0, 0.6);
            flex-shrink: 0;
        }
        .icon-meta h3 {
            font-size: 1.15rem;
            font-weight: 800;
            color: #FFFFFF;
            margin-bottom: 6px;
        }
        .icon-meta p {
            font-size: 0.85rem;
            color: #94A3B8;
            max-width: 480px;
            line-height: 1.45;
        }

        /* Toast notification */
        .toast {
            position: fixed;
            bottom: 24px;
            right: 24px;
            background: #10B981;
            color: #FFFFFF;
            padding: 12px 22px;
            border-radius: 12px;
            font-weight: 700;
            font-size: 0.90rem;
            box-shadow: 0 10px 25px rgba(16, 185, 129, 0.4);
            transform: translateY(100px);
            opacity: 0;
            transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
            z-index: 1000;
        }
        .toast.show {
            transform: translateY(0);
            opacity: 1;
        }

        /* Code export modal */
        .code-snippet-box {
            background: #0B0E14;
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 14px;
            padding: 14px;
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.78rem;
            color: #CBD5E1;
            white-space: pre;
            overflow-x: auto;
            max-height: 150px;
        }
    </style>
</head>
<body>

    <!-- Top Navigation Header -->
    <div class="top-navbar">
        <div class="brand">
            <div class="brand-icon-mini">
                <svg viewBox="0 0 512 512" width="100%" height="100%">
                    <rect width="512" height="512" rx="115" id="navIconBg" fill="#18181B"/>
                    <path d="M 120 370 L 120 180 Q 120 150 150 150 Q 175 150 195 180 L 256 270 L 317 180 Q 337 150 362 150 Q 392 150 392 180 L 392 370" fill="none" stroke="#3F3F46" stroke-width="20" stroke-linecap="round"/>
                    <path d="M 180 370 L 180 230 Q 180 215 195 215 Q 210 215 220 230 L 256 285 L 292 230 Q 302 215 317 215 Q 332 215 332 230 L 332 370" fill="none" id="navIconM" stroke="#FF3B00" stroke-width="32" stroke-linecap="round"/>
                    <circle cx="256" cy="285" r="16" fill="#FFFFFF"/>
                    <circle cx="256" cy="285" r="8" id="navIconBeacon" fill="#FF3B00"/>
                </svg>
            </div>
            <h1>MoodyRoutine <span>THEME STUDIO</span></h1>
        </div>
        <div class="header-actions">
            <button class="btn-action btn-secondary-action" onclick="resetToDefaults()">Reset Defaults</button>
            <button class="btn-action btn-primary-action" onclick="copyConfig()">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>
                Copy Color Config
            </button>
        </div>
    </div>

    <!-- Main Workspace -->
    <div class="workspace">

        <!-- Controls Sidebar -->
        <div class="control-panel">
            
            <!-- Curated Presets -->
            <div>
                <div class="section-header">
                    <span>Curated Presets</span>
                    <span style="font-size:0.7rem; color:#A1A1AA;">1-Click Apply</span>
                </div>
                <div class="presets-grid">
                    <div class="preset-chip active" id="chip-preset1" onclick="applyPreset('preset1')">
                        <div class="preset-name">Crimson Obsidian</div>
                        <div class="preset-dots">
                            <div class="preset-dot" style="background:#08080A;"></div>
                            <div class="preset-dot" style="background:#18181B;"></div>
                            <div class="preset-dot" style="background:#FF3B00;"></div>
                            <div class="preset-dot" style="background:#FF6838;"></div>
                        </div>
                    </div>
                    <div class="preset-chip" id="chip-preset2" onclick="applyPreset('preset2')">
                        <div class="preset-name">Blood Orange AMOLED</div>
                        <div class="preset-dots">
                            <div class="preset-dot" style="background:#000000;"></div>
                            <div class="preset-dot" style="background:#141417;"></div>
                            <div class="preset-dot" style="background:#E62E00;"></div>
                            <div class="preset-dot" style="background:#FF5722;"></div>
                        </div>
                    </div>
                    <div class="preset-chip" id="chip-preset3" onclick="applyPreset('preset3')">
                        <div class="preset-name">Sunset Flame Titanium</div>
                        <div class="preset-dots">
                            <div class="preset-dot" style="background:#0A0A0D;"></div>
                            <div class="preset-dot" style="background:#1E1E24;"></div>
                            <div class="preset-dot" style="background:#F93800;"></div>
                            <div class="preset-dot" style="background:#FF7043;"></div>
                        </div>
                    </div>
                    <div class="preset-chip" id="chip-preset4" onclick="applyPreset('preset4')">
                        <div class="preset-name">Terracotta Charcoal</div>
                        <div class="preset-dots">
                            <div class="preset-dot" style="background:#09090C;"></div>
                            <div class="preset-dot" style="background:#1A1A1E;"></div>
                            <div class="preset-dot" style="background:#EA380C;"></div>
                            <div class="preset-dot" style="background:#F87171;"></div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Color Inputs: Surfaces & Backgrounds -->
            <div>
                <div class="section-header">
                    <span>Surfaces & Backgrounds</span>
                </div>
                <div class="color-group">
                    
                    <!-- App Canvas BG -->
                    <div class="color-row">
                        <div class="color-info">
                            <div class="color-title">App Canvas Background</div>
                            <div class="color-desc">Deep/AMOLED black base</div>
                        </div>
                        <div class="picker-box">
                            <div class="color-input-wrapper">
                                <input type="color" id="picker-app-bg" value="#08080A" oninput="updateColor('app-bg', this.value)">
                            </div>
                            <input type="text" class="hex-input" id="hex-app-bg" value="#08080A" onchange="updateFromHex('app-bg', this.value)">
                        </div>
                    </div>

                    <!-- Card Surface (Pure Grey) -->
                    <div class="color-row">
                        <div class="color-info">
                            <div class="color-title">Card Surface (Neutral Grey)</div>
                            <div class="color-desc">Pure grey, zero orange tint</div>
                        </div>
                        <div class="picker-box">
                            <div class="color-input-wrapper">
                                <input type="color" id="picker-card-bg" value="#18181B" oninput="updateColor('card-bg', this.value)">
                            </div>
                            <input type="text" class="hex-input" id="hex-card-bg" value="#18181B" onchange="updateFromHex('card-bg', this.value)">
                        </div>
                    </div>

                    <!-- Card Border / Outline -->
                    <div class="color-row">
                        <div class="color-info">
                            <div class="color-title">Card Outline Border</div>
                            <div class="color-desc">Crisp contour definition</div>
                        </div>
                        <div class="picker-box">
                            <div class="color-input-wrapper">
                                <input type="color" id="picker-card-border" value="#2A2A32" oninput="updateColor('card-border', this.value)">
                            </div>
                            <input type="text" class="hex-input" id="hex-card-border" value="#2A2A32" onchange="updateFromHex('card-border', this.value)">
                        </div>
                    </div>

                    <!-- Popup Dialog Background -->
                    <div class="color-row">
                        <div class="color-info">
                            <div class="color-title">Popup / Dialog Background</div>
                            <div class="color-desc">Darker than cards (pure dark)</div>
                        </div>
                        <div class="picker-box">
                            <div class="color-input-wrapper">
                                <input type="color" id="picker-popup-bg" value="#0C0C0E" oninput="updateColor('popup-bg', this.value)">
                            </div>
                            <input type="text" class="hex-input" id="hex-popup-bg" value="#0C0C0E" onchange="updateFromHex('popup-bg', this.value)">
                        </div>
                    </div>

                    <!-- Bottom Nav Bar BG -->
                    <div class="color-row">
                        <div class="color-info">
                            <div class="color-title">Bottom Bar Background</div>
                            <div class="color-desc">Dock background surface</div>
                        </div>
                        <div class="picker-box">
                            <div class="color-input-wrapper">
                                <input type="color" id="picker-bottom-bar-bg" value="#111115" oninput="updateColor('bottom-bar-bg', this.value)">
                            </div>
                            <input type="text" class="hex-input" id="hex-bottom-bar-bg" value="#111115" onchange="updateFromHex('bottom-bar-bg', this.value)">
                        </div>
                    </div>

                </div>
            </div>

            <!-- Color Inputs: Reddish Orange Highlights -->
            <div>
                <div class="section-header">
                    <span>Highlights & Reddish Orange</span>
                </div>
                <div class="color-group">
                    
                    <!-- Primary Highlight -->
                    <div class="color-row">
                        <div class="color-info">
                            <div class="color-title">Primary Reddish Orange</div>
                            <div class="color-desc">Buttons, switches, active tabs</div>
                        </div>
                        <div class="picker-box">
                            <div class="color-input-wrapper">
                                <input type="color" id="picker-primary" value="#FF3B00" oninput="updateColor('primary', this.value)">
                            </div>
                            <input type="text" class="hex-input" id="hex-primary" value="#FF3B00" onchange="updateFromHex('primary', this.value)">
                        </div>
                    </div>

                    <!-- Secondary Ember Glow -->
                    <div class="color-row">
                        <div class="color-info">
                            <div class="color-title">Secondary Ember Light</div>
                            <div class="color-desc">Icon gradient & active glow</div>
                        </div>
                        <div class="picker-box">
                            <div class="color-input-wrapper">
                                <input type="color" id="picker-primary-light" value="#FF6838" oninput="updateColor('primary-light', this.value)">
                            </div>
                            <input type="text" class="hex-input" id="hex-primary-light" value="#FF6838" onchange="updateFromHex('primary-light', this.value)">
                        </div>
                    </div>

                    <!-- Contour Grey -->
                    <div class="color-row">
                        <div class="color-info">
                            <div class="color-title">Contour Slate Grey</div>
                            <div class="color-desc">Mid elevation lines & idle tabs</div>
                        </div>
                        <div class="picker-box">
                            <div class="color-input-wrapper">
                                <input type="color" id="picker-contour-grey" value="#71717A" oninput="updateColor('contour-grey', this.value)">
                            </div>
                            <input type="text" class="hex-input" id="hex-contour-grey" value="#71717A" onchange="updateFromHex('contour-grey', this.value)">
                        </div>
                    </div>

                    <!-- Text Colors -->
                    <div class="color-row">
                        <div class="color-info">
                            <div class="color-title">Main Text Color</div>
                            <div class="color-desc">Headers, titles, switches</div>
                        </div>
                        <div class="picker-box">
                            <div class="color-input-wrapper">
                                <input type="color" id="picker-text-main" value="#FFFFFF" oninput="updateColor('text-main', this.value)">
                            </div>
                            <input type="text" class="hex-input" id="hex-text-main" value="#FFFFFF" onchange="updateFromHex('text-main', this.value)">
                        </div>
                    </div>

                    <div class="color-row">
                        <div class="color-info">
                            <div class="color-title">Muted Subtitle Text</div>
                            <div class="color-desc">Secondary details, chevrons</div>
                        </div>
                        <div class="picker-box">
                            <div class="color-input-wrapper">
                                <input type="color" id="picker-text-muted" value="#94949E" oninput="updateColor('text-muted', this.value)">
                            </div>
                            <input type="text" class="hex-input" id="hex-text-muted" value="#94949E" onchange="updateFromHex('text-muted', this.value)">
                        </div>
                    </div>

                </div>
            </div>

            <!-- Live JSON config snippet -->
            <div>
                <div class="section-header">
                    <span>Generated JSON Config</span>
                </div>
                <div class="code-snippet-box" id="jsonSnippetBox"></div>
            </div>

        </div>

        <!-- Live Preview Canvas -->
        <div class="preview-canvas">
            
            <div class="canvas-header">
                <div>
                    <div class="canvas-title">Real-Time Mobile Screen Previews</div>
                    <div class="canvas-subtitle">Every color change updates all 3 app tabs, the modal popup, and the Topographic "M" icon simultaneously.</div>
                </div>
            </div>

            <!-- Topographic M Icon Preview Card -->
            <div class="icon-preview-card">
                <div class="icon-svg-container">
                    <svg id="liveSvgIcon" viewBox="0 0 512 512" width="100%" height="100%">
                        <defs>
                            <linearGradient id="liveMGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                                <stop offset="0%" id="stopGradLight" stop-color="var(--primary-light)"/>
                                <stop offset="100%" id="stopGradPrimary" stop-color="var(--primary)"/>
                            </linearGradient>
                            <filter id="iconGlow" x="-20%" y="-20%" width="140%" height="140%">
                                <feDropShadow dx="0" dy="10" stdDeviation="12" flood-color="var(--primary)" flood-opacity="0.3"/>
                            </filter>
                        </defs>
                        <rect width="512" height="512" rx="115" id="liveIconBg" fill="var(--card-bg)"/>
                        <!-- Contour 1 (Outer M) -->
                        <path d="M 120 370 L 120 180 Q 120 150 150 150 Q 175 150 195 180 L 256 270 L 317 180 Q 337 150 362 150 Q 392 150 392 180 L 392 370" 
                              fill="none" stroke="var(--card-border)" stroke-width="12" stroke-linecap="round" stroke-linejoin="round"/>
                        <!-- Contour 2 (Mid M) -->
                        <path d="M 150 370 L 150 205 Q 150 185 168 185 Q 185 185 200 205 L 256 290 L 312 205 Q 327 185 344 185 Q 362 185 362 205 L 362 370" 
                              fill="none" stroke="var(--contour-grey)" stroke-width="14" stroke-linecap="round" stroke-linejoin="round"/>
                        <!-- Contour 3 (Glowing Reddish Orange M) -->
                        <path d="M 180 370 L 180 230 Q 180 215 195 215 Q 210 215 220 230 L 256 285 L 292 230 Q 302 215 317 215 Q 332 215 332 230 L 332 370" 
                              fill="none" stroke="url(#liveMGrad)" stroke-width="18" stroke-linecap="round" stroke-linejoin="round" filter="url(#iconGlow)"/>
                        <!-- Beacon Waypoint -->
                        <circle cx="256" cy="285" r="10" fill="#FFFFFF"/>
                        <circle cx="256" cy="285" r="5" fill="var(--primary)"/>
                    </svg>
                </div>
                <div class="icon-meta">
                    <h3>Topographic "M" Launcher Icon (Live Sync)</h3>
                    <p>The icon's contour lines and summit beacon automatically sync with your reddish-orange highlight and neutral grey contour elevation lines. No orange bleed on the background.</p>
                </div>
            </div>

            <!-- 4 Realistic Phone Mockups Grid -->
            <div class="devices-grid">

                <!-- 1. MODES SCREEN -->
                <div>
                    <div class="device-label">
                        <span>SCREEN 1: MODES TAB</span>
                        <span>ACTIVE HERO TAB</span>
                    </div>
                    <div class="phone-frame">
                        <div class="phone-status-bar">
                            <span>2:27</span>
                            <div class="status-icons">
                                <span>5G</span>
                                <span>100%</span>
                            </div>
                        </div>
                        <div class="screen-content">
                            <div class="screen-title">Modes</div>

                            <!-- Sleep Card -->
                            <div class="ui-card">
                                <div class="card-icon-wrap" style="background: rgba(127, 86, 217, 0.2); color: #A78BFA;">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M12.3 2a10 10 0 0 0-1.9 20 10 10 0 0 0 9.8-7.8 8 8 0 0 1-2.9.5 8 8 0 0 1-8-8c0-1.8.6-3.5 1.7-4.9A10 10 0 0 0 12.3 2z"/></svg>
                                </div>
                                <div class="card-body">
                                    <div class="card-title">Sleep</div>
                                    <div class="card-desc">Turn on sleep mode to get ready for bed</div>
                                </div>
                                <div class="ui-switch" onclick="toggleSwitch(this)">
                                    <div class="ui-switch-thumb"></div>
                                </div>
                            </div>

                            <!-- Exercise Card (Active Highlight) -->
                            <div class="ui-card">
                                <div class="card-icon-wrap" style="background: rgba(255, 59, 0, 0.2); color: var(--primary);">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M6 4v16M18 4v16M2 8v8M22 8v8M6 12h12"/></svg>
                                </div>
                                <div class="card-body">
                                    <div class="card-title">Exercise</div>
                                    <div class="card-desc">Focus on your workout and track your health</div>
                                </div>
                                <div class="ui-switch active" onclick="toggleSwitch(this)">
                                    <div class="ui-switch-thumb"></div>
                                </div>
                            </div>

                            <!-- Relax Card -->
                            <div class="ui-card">
                                <div class="card-icon-wrap" style="background: rgba(6, 182, 212, 0.2); color: #22D3EE;">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3a9 9 0 0 0-9 9c0 4.97 4.03 9 9 9s9-4.03 9-9c0-4.97-4.03-9-9-9zm0 16c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"/></svg>
                                </div>
                                <div class="card-body">
                                    <div class="card-title">Relax</div>
                                    <div class="card-desc">Take time to unwind and disconnect</div>
                                </div>
                                <div class="ui-switch active" onclick="toggleSwitch(this)">
                                    <div class="ui-switch-thumb"></div>
                                </div>
                            </div>

                            <!-- Work Card -->
                            <div class="ui-card">
                                <div class="card-icon-wrap" style="background: rgba(59, 130, 246, 0.2); color: #60A5FA;">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z"/></svg>
                                </div>
                                <div class="card-body">
                                    <div class="card-title">Work</div>
                                    <div class="card-desc">Stay focused and minimize distractions</div>
                                </div>
                                <div class="ui-switch" onclick="toggleSwitch(this)">
                                    <div class="ui-switch-thumb"></div>
                                </div>
                            </div>

                            <!-- FAB -->
                            <div class="ui-fab">+</div>
                        </div>

                        <!-- Bottom Nav Bar -->
                        <div class="ui-bottom-bar">
                            <div class="tab-item active">
                                <span class="tab-icon">■</span>
                                <span>Modes</span>
                            </div>
                            <div class="tab-item">
                                <span class="tab-icon">⇄</span>
                                <span>Routines</span>
                            </div>
                            <div class="tab-item">
                                <span class="tab-icon">⚙</span>
                                <span>Settings</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 2. ROUTINES SCREEN -->
                <div>
                    <div class="device-label">
                        <span>SCREEN 2: ROUTINES TAB</span>
                        <span>HERO ACCENT BUTTON</span>
                    </div>
                    <div class="phone-frame">
                        <div class="phone-status-bar">
                            <span>2:27</span>
                            <div class="status-icons">
                                <span>5G</span>
                                <span>100%</span>
                            </div>
                        </div>
                        <div class="screen-content" style="padding-bottom: 70px;">
                            <div class="screen-title">Routines</div>
                            
                            <div class="empty-routines-wrap">
                                <div class="empty-sparkle-circle">
                                    ✦
                                </div>
                                <div class="empty-title">No routines yet</div>
                                <div class="empty-desc">Create routines to automate device settings based on what you do, where you go, or battery.</div>
                                <button class="btn-add-routine">
                                    <span>+</span> Add routine
                                </button>
                            </div>

                            <!-- FAB -->
                            <div class="ui-fab">+</div>
                        </div>

                        <!-- Bottom Nav Bar -->
                        <div class="ui-bottom-bar">
                            <div class="tab-item">
                                <span class="tab-icon">■</span>
                                <span>Modes</span>
                            </div>
                            <div class="tab-item active">
                                <span class="tab-icon">⇄</span>
                                <span>Routines</span>
                            </div>
                            <div class="tab-item">
                                <span class="tab-icon">⚙</span>
                                <span>Settings</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 3. SETTINGS SCREEN -->
                <div>
                    <div class="device-label">
                        <span>SCREEN 3: SETTINGS TAB</span>
                        <span>SECTION HEADERS & TOGGLES</span>
                    </div>
                    <div class="phone-frame">
                        <div class="phone-status-bar">
                            <span>2:27</span>
                            <div class="status-icons">
                                <span>5G</span>
                                <span>100%</span>
                            </div>
                        </div>
                        <div class="screen-content">
                            <div class="screen-title">Settings</div>

                            <!-- Automation Service -->
                            <div class="ui-card">
                                <div class="card-icon-wrap" style="background: rgba(255, 59, 0, 0.16); color: var(--primary);">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/></svg>
                                </div>
                                <div class="card-body">
                                    <div class="card-title">Automation Service</div>
                                    <div class="card-desc">Running in background</div>
                                </div>
                                <div class="ui-switch active" onclick="toggleSwitch(this)">
                                    <div class="ui-switch-thumb"></div>
                                </div>
                            </div>

                            <!-- Battery Optimization -->
                            <div class="ui-card">
                                <div class="card-icon-wrap" style="background: rgba(255, 255, 255, 0.08); color: var(--text-main);">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M15.67 4H14V2h-4v2H8.33C7.6 4 7 4.6 7 5.33v15.33C7 21.4 7.6 22 8.33 22h7.33c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4z"/></svg>
                                </div>
                                <div class="card-body">
                                    <div class="card-title">Battery Optimization</div>
                                    <div class="card-desc">Unrestricted (recommended)</div>
                                </div>
                                <div style="color: var(--text-muted); font-size: 1rem;">&rsaquo;</div>
                            </div>

                            <div class="section-label-settings">Required Permissions</div>

                            <!-- Background Location -->
                            <div class="ui-card">
                                <div class="card-icon-wrap" style="background: rgba(239, 68, 68, 0.16); color: #EF4444;">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
                                </div>
                                <div class="card-body">
                                    <div style="display:flex; align-items:center; gap:8px;">
                                        <div class="card-title">Background Location</div>
                                        <span class="badge-status badge-action">Action Needed</span>
                                    </div>
                                    <div class="card-desc">Set location permission to 'Allow all the time'</div>
                                </div>
                                <div style="color: var(--text-muted); font-size: 1rem;">&rsaquo;</div>
                            </div>

                            <!-- Modify System Settings -->
                            <div class="ui-card">
                                <div class="card-icon-wrap" style="background: rgba(16, 185, 129, 0.16); color: #10B981;">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>
                                </div>
                                <div class="card-body">
                                    <div style="display:flex; align-items:center; gap:8px;">
                                        <div class="card-title">Modify Settings</div>
                                        <span class="badge-status badge-granted">Granted</span>
                                    </div>
                                    <div class="card-desc">Brightness & auto-rotate actions active</div>
                                </div>
                                <div style="color: var(--text-muted); font-size: 1rem;">&rsaquo;</div>
                            </div>
                        </div>

                        <!-- Bottom Nav Bar -->
                        <div class="ui-bottom-bar">
                            <div class="tab-item">
                                <span class="tab-icon">■</span>
                                <span>Modes</span>
                            </div>
                            <div class="tab-item">
                                <span class="tab-icon">&#8646;</span>
                                <span>Routines</span>
                            </div>
                            <div class="tab-item active">
                                <span class="tab-icon">&#9881;</span>
                                <span>Settings</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 4. MODAL POPUP PREVIEW -->
                <div>
                    <div class="device-label">
                        <span>MODAL / POPUP DIALOG</span>
                        <span>DARKER BACKDROP (NO ORANGE BLEED)</span>
                    </div>
                    <div class="phone-frame">
                        <div class="phone-status-bar">
                            <span>2:27</span>
                            <div class="status-icons">
                                <span>5G</span>
                                <span>100%</span>
                            </div>
                        </div>
                        <div class="screen-content" style="position:relative;">
                            <div class="screen-title">Modes</div>
                            <div class="ui-card" style="opacity: 0.3;">
                                <div class="card-body"><div class="card-title">Sleep</div></div>
                            </div>
                            <div class="ui-card" style="opacity: 0.3;">
                                <div class="card-body"><div class="card-title">Exercise</div></div>
                            </div>
                            
                            <!-- Dialog Overlay -->
                            <div class="dialog-backdrop">
                                <div class="dialog-card">
                                    <div class="dialog-header">
                                        <div class="dialog-header-icon">
                                            <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/></svg>
                                        </div>
                                        <div>
                                            <div style="font-size: 1.05rem; font-weight: 800; color: var(--text-main);">Welcome to Moody</div>
                                            <div style="font-size: 0.72rem; color: var(--text-muted);">Background automation permissions</div>
                                        </div>
                                    </div>
                                    <div style="font-size: 0.75rem; color: var(--text-muted); line-height: 1.4;">
                                        Grant the following permissions to ensure modes and routines run reliably:
                                    </div>
                                    
                                    <!-- Dialog Item 1 -->
                                    <div class="dialog-item">
                                        <div style="display:flex; align-items:center; gap:8px;">
                                            <div style="color:var(--text-muted); display:flex; align-items:center;">
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>
                                            </div>
                                            <div>
                                                <div style="font-size:0.82rem; font-weight:700; color:var(--text-main);">Notifications</div>
                                                <div style="font-size:0.68rem; color:var(--text-muted);">Alerts on trigger</div>
                                            </div>
                                        </div>
                                        <button style="background:var(--card-border); color:var(--text-main); border:none; border-radius:8px; padding:4px 10px; font-size:0.75rem; font-weight:700;">Allow</button>
                                    </div>

                                    <!-- Dialog Item 2 -->
                                    <div class="dialog-item">
                                        <div style="display:flex; align-items:center; gap:8px;">
                                            <div style="color:var(--text-muted); display:flex; align-items:center;">
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M15.67 4H14V2h-4v2H8.33C7.6 4 7 4.6 7 5.33v15.33C7 21.4 7.6 22 8.33 22h7.33c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4z"/></svg>
                                            </div>
                                            <div>
                                                <div style="font-size:0.82rem; font-weight:700; color:var(--text-main);">Battery Unrestricted</div>
                                                <div style="font-size:0.68rem; color:var(--text-muted);">Exempts from sleep</div>
                                            </div>
                                        </div>
                                        <span style="color:var(--primary); font-size:1.1rem; font-weight:800;">&#10003;</span>
                                    </div>

                                    <button class="btn-dialog-primary">Get Started</button>
                                    <div class="btn-dialog-skip">Skip for now</div>
                                </div>
                            </div>

                        </div>
                    </div>
                </div>

            </div>

        </div>
    </div>

    <!-- Toast -->
    <div class="toast" id="toast">Config copied to clipboard! Paste it directly into the chat.</div>

    <script>
        const presets = {
            preset1: {
                name: "Crimson Obsidian (Default)",
                'app-bg': '#08080A',
                'card-bg': '#18181B',
                'card-border': '#2A2A32',
                'popup-bg': '#0C0C0E',
                'bottom-bar-bg': '#111115',
                'primary': '#FF3B00',
                'primary-light': '#FF6838',
                'contour-grey': '#71717A',
                'text-main': '#FFFFFF',
                'text-muted': '#94949E'
            },
            preset2: {
                name: "Blood Orange AMOLED",
                'app-bg': '#000000',
                'card-bg': '#141417',
                'card-border': '#222228',
                'popup-bg': '#070709',
                'bottom-bar-bg': '#0A0A0E',
                'primary': '#E62E00',
                'primary-light': '#FF5722',
                'contour-grey': '#6B7280',
                'text-main': '#FFFFFF',
                'text-muted': '#8E96A5'
            },
            preset3: {
                name: "Sunset Flame Titanium",
                'app-bg': '#0A0A0D',
                'card-bg': '#1E1E24',
                'card-border': '#32323C',
                'popup-bg': '#0F0F13',
                'bottom-bar-bg': '#14141A',
                'primary': '#F93800',
                'primary-light': '#FF7043',
                'contour-grey': '#78716C',
                'text-main': '#F8FAFC',
                'text-muted': '#A1A1AA'
            },
            preset4: {
                name: "Terracotta Charcoal",
                'app-bg': '#09090C',
                'card-bg': '#1A1A1E',
                'card-border': '#2F2F38',
                'popup-bg': '#0D0D11',
                'bottom-bar-bg': '#131317',
                'primary': '#EA380C',
                'primary-light': '#F87171',
                'contour-grey': '#64748B',
                'text-main': '#F8FAFC',
                'text-muted': '#94A3B8'
            }
        };

        const colorKeys = [
            'app-bg', 'card-bg', 'card-border', 'popup-bg', 'bottom-bar-bg',
            'primary', 'primary-light', 'contour-grey', 'text-main', 'text-muted'
        ];

        function updateColor(key, value) {
            value = value.toUpperCase();
            document.documentElement.style.setProperty(`--${key}`, value);
            
            const picker = document.getElementById(`picker-${key}`);
            const hex = document.getElementById(`hex-${key}`);
            if (picker && picker.value.toUpperCase() !== value) picker.value = value;
            if (hex && hex.value.toUpperCase() !== value) hex.value = value;

            // Sync SVG stops if primary or primary-light
            if (key === 'primary') {
                const navM = document.getElementById('navIconM');
                const navBeacon = document.getElementById('navIconBeacon');
                const stopPrimary = document.getElementById('stopGradPrimary');
                if (navM) navM.setAttribute('stroke', value);
                if (navBeacon) navBeacon.setAttribute('fill', value);
                if (stopPrimary) stopPrimary.setAttribute('stop-color', value);
            }
            if (key === 'primary-light') {
                const stopLight = document.getElementById('stopGradLight');
                if (stopLight) stopLight.setAttribute('stop-color', value);
            }
            if (key === 'card-bg') {
                const navBg = document.getElementById('navIconBg');
                const liveBg = document.getElementById('liveIconBg');
                if (navBg) navBg.setAttribute('fill', value);
                if (liveBg) liveBg.setAttribute('fill', value);
            }

            renderJsonConfig();
            saveState();
        }

        function updateFromHex(key, value) {
            if (!value.startsWith('#')) value = '#' + value;
            if (/^#[0-9A-F]{6}$/i.test(value)) {
                updateColor(key, value);
            }
        }

        function applyPreset(presetKey) {
            const p = presets[presetKey];
            if (!p) return;

            document.querySelectorAll('.preset-chip').forEach(c => c.classList.remove('active'));
            const activeChip = document.getElementById(`chip-${presetKey}`);
            if (activeChip) activeChip.classList.add('active');

            colorKeys.forEach(k => {
                if (p[k]) updateColor(k, p[k]);
            });
        }

        function toggleSwitch(el) {
            el.classList.toggle('active');
        }

        function getFullConfig() {
            const cfg = {};
            colorKeys.forEach(k => {
                const hex = document.getElementById(`hex-${k}`);
                cfg[k] = hex ? hex.value : document.documentElement.style.getPropertyValue(`--${k}`);
            });
            return cfg;
        }

        function renderJsonConfig() {
            const cfg = getFullConfig();
            const jsonText = JSON.stringify(cfg, null, 2);
            const box = document.getElementById('jsonSnippetBox');
            if (box) box.textContent = jsonText;
        }

        function copyConfig() {
            const cfg = getFullConfig();
            const textToCopy = "MOODY_THEME_CONFIG:\\n```json\\n" + JSON.stringify(cfg, null, 2) + "\\n```";
            navigator.clipboard.writeText(textToCopy).then(() => {
                showToast();
            }).catch(() => {
                // Fallback
                const ta = document.createElement('textarea');
                ta.value = textToCopy;
                document.body.appendChild(ta);
                ta.select();
                document.execCommand('copy');
                document.body.removeChild(ta);
                showToast();
            });
        }

        function showToast() {
            const toast = document.getElementById('toast');
            toast.classList.add('show');
            setTimeout(() => toast.classList.remove('show'), 3500);
        }

        function resetToDefaults() {
            applyPreset('preset1');
        }

        function saveState() {
            try {
                localStorage.setItem('moody_theme_studio_cfg', JSON.stringify(getFullConfig()));
            } catch(e) {}
        }

        function loadSavedState() {
            try {
                const saved = localStorage.getItem('moody_theme_studio_cfg');
                if (saved) {
                    const cfg = JSON.parse(saved);
                    colorKeys.forEach(k => {
                        if (cfg[k]) updateColor(k, cfg[k]);
                    });
                    return;
                }
            } catch(e) {}
            applyPreset('preset1');
        }

        // Initialize on load
        loadSavedState();
    </script>
</body>
</html>
"""

    studio_path = os.path.join(output_dir, "index.html")
    with open(studio_path, "w", encoding="utf-8") as f:
        f.write(html_code.strip())
    print(f"[*] Created Theme Studio: {studio_path}")

    alt_path = os.path.join(alt_dir, "theme_studio.html")
    with open(alt_path, "w", encoding="utf-8") as f:
        f.write(html_code.strip())
    print(f"[*] Created copy at: {alt_path}")
    print("[SUCCESS] Theme Studio HTML generated successfully!")

def test_theme_switcher():
    print("[*] Waking device...")
    adb("shell input keyevent KEYCODE_WAKEUP")
    adb("shell wm dismiss-keyguard")
    time.sleep(1)

    print("[*] Stopping and launching app...")
    adb(f"shell am force-stop {PKG}")
    time.sleep(1)
    adb(f"shell am start -n {PKG}/com.ndev.moodyroutine.MainActivity")
    time.sleep(3)

    # Check for onboarding or permission dialog and dismiss if present
    elements = dump_ui()
    is_welcome = any("welcome to moodyroutine" in (el["text"] or "").lower() for el in elements)
    if is_welcome:
        print("[*] Welcome dialog detected, scrolling down to dismiss...")
        adb("shell input swipe 500 1300 500 400 300")
        time.sleep(1)
        if not tap_query("Skip for now"):
            tap_query("Get Started")
        time.sleep(1)
    else:
        for el in elements:
            if "get started" in el["text"].lower() or "got it" in el["text"].lower() or "continue" in el["text"].lower() or "later" in el["text"].lower() or "skip" in el["text"].lower():
                tap(el["cx"], el["cy"])
                time.sleep(1)
                break

    # Tap on Modes tab to be sure
    print("[*] Checking Modes screen switches...")
    tap(133, 1500)
    time.sleep(2)
    screenshot("modes_orange_switches.png")

    # Tap on Routines tab
    print("[*] Checking Routines screen switches...")
    tap(352, 1500)
    time.sleep(2)
    screenshot("routines_orange_switches.png")

    # Tap on Settings tab
    print("[*] Navigating to Settings...")
    tap(578, 1500)
    time.sleep(2)

    # Scroll incrementally until Theme Preset is visible
    print("[*] Scrolling to find Theme Preset...")
    for _ in range(6):
        elements = dump_ui()
        if any("theme preset" in el["text"].lower() for el in elements):
            break
        adb("shell input swipe 500 1100 500 750 250")
        time.sleep(1)

    time.sleep(1)
    screenshot("theme_picker_collapsed.png")

    # Tap Theme Preset to expand
    print("[*] Tapping Theme Preset to expand...")
    tap_query("Theme Preset")
    time.sleep(1)
    screenshot("theme_picker_expanded.png")

    print("[SUCCESS] All screenshots captured!")

def main():
    test_theme_switcher()

if __name__ == "__main__":
    main()




