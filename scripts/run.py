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

def main():
    print("[*] MoodyRoutine helper script ready.")

if __name__ == "__main__":
    main()


