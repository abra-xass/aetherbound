#!/usr/bin/env python3
"""
Daily-pack builder for Aetherbound.

Runs hourly via GitHub Actions cron (.github/workflows/daily-pack.yml).
Fetches Zurich weather from a free public API + summarises into a small
JSON file the Android app polls once per launch.

Output:
  content/daily-pack.json — committed back into the repo, pulled by
  the app via DailyPackFetcher.kt.

Schema (kept tiny — <2 KB):
  {
    "ts_utc": "2026-04-30T15:00:00Z",
    "weather":   "RAIN",            // matches com.aetherbound.game.core.Weather
    "weather_de": "Regen",
    "temp_c":    14.2,
    "humidity":  76,
    "city":      "Zurich",
    "world_event": {                // NEW: optional flavor blurb for NPCs
        "tag": "rain_burst",
        "text": "Regen ueber Zuerich — Aether-Otter raus."
    }
  }

Sources:
  - Open-Meteo (api.open-meteo.com) — completely free, no auth needed.
    Provides temp_c + humidity + weathercode for Zurich (lat 47.37, lon 8.55).
"""

import json
import os
import sys
import urllib.request
import urllib.error
from datetime import datetime, timezone


ZURICH_LAT = 47.3769
ZURICH_LON = 8.5417

# Open-Meteo weathercode -> Aetherbound Weather enum.
# https://open-meteo.com/en/docs#weathervariables
WEATHERCODE_MAP = {
    0:  ("CLEAR",   "klar"),
    1:  ("CLEAR",   "meist klar"),
    2:  ("CLOUDY",  "bewoelkt"),
    3:  ("CLOUDY",  "bedeckt"),
    45: ("FOG",     "Nebel"),
    48: ("FOG",     "Reifnebel"),
    51: ("RAIN",    "Niesel"),
    53: ("RAIN",    "Niesel"),
    55: ("RAIN",    "starker Niesel"),
    61: ("RAIN",    "leichter Regen"),
    63: ("RAIN",    "Regen"),
    65: ("RAIN",    "starker Regen"),
    71: ("SNOW",    "leichter Schnee"),
    73: ("SNOW",    "Schnee"),
    75: ("SNOW",    "starker Schnee"),
    80: ("RAIN",    "Regenschauer"),
    81: ("RAIN",    "kraeftige Schauer"),
    82: ("STORM",   "Gewitterschauer"),
    95: ("STORM",   "Gewitter"),
    96: ("STORM",   "Gewitter mit Hagel"),
    99: ("STORM",   "schweres Gewitter"),
}


def fetch_open_meteo() -> dict:
    """Return current weather for Zurich. Falls back to CLEAR on any error."""
    url = (
        "https://api.open-meteo.com/v1/forecast"
        f"?latitude={ZURICH_LAT}&longitude={ZURICH_LON}"
        "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
        "&timezone=Europe%2FZurich"
    )
    req = urllib.request.Request(url, headers={"User-Agent": "aetherbound-daily-pack/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return json.loads(r.read().decode("utf-8"))
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError) as e:
        print(f"[warn] open-meteo fetch failed: {e}", file=sys.stderr)
        return {}


def detect_foehn(temp_c: float, humidity: int, wind_kmh: float) -> bool:
    """Crude Foehn detector — warm + dry + windy in spring/autumn.

    Real Foehn detection needs upstream pressure data, but for game-flavor
    purposes this heuristic catches the dramatic days that matter for
    Echoform unrest hints."""
    return temp_c >= 18.0 and humidity <= 50 and wind_kmh >= 25.0


def weathercode_to_aether(code: int) -> tuple[str, str]:
    return WEATHERCODE_MAP.get(code, ("CLEAR", "klar"))


def world_event_for_weather(weather: str) -> dict | None:
    """Tiny flavor-text bundle. NPCs reference this via Tracery slots later."""
    pool = {
        "RAIN":    ("rain_burst",  "Regen ueber Zuerich — Aether-Otter raus."),
        "STORM":   ("storm_warn",  "Gewitter im Anmarsch — Lightning-Echoforms aktiv."),
        "SNOW":    ("snow_blanket", "Schnee in den Alpen — Frost-Echoforms unterwegs."),
        "FOG":     ("fog_wrap",    "Nebel im Mittelland — Schatten-Wesen wandern."),
        "FOEHN":   ("foehn_alert", "Foehn drueckt vom Suedhang — Echoforms unruhig."),
        "HEAT":    ("heat_wave",   "Sommerhitze — Fire-Echoforms in Hochform."),
    }
    if weather in pool:
        tag, text = pool[weather]
        return {"tag": tag, "text": text}
    return None


def build_pack() -> dict:
    raw = fetch_open_meteo()
    cur = raw.get("current", {})
    temp = float(cur.get("temperature_2m", 12.0))
    humidity = int(cur.get("relative_humidity_2m", 70))
    wind = float(cur.get("wind_speed_10m", 5.0))
    code = int(cur.get("weather_code", 0))

    weather, weather_de = weathercode_to_aether(code)
    # Foehn override — heuristic supersedes weathercode when applicable.
    if detect_foehn(temp, humidity, wind):
        weather, weather_de = "FOEHN", "Foehn"
    # Heat override — open-meteo doesn't have a HEAT code; we add it.
    if weather == "CLEAR" and temp >= 27.0:
        weather, weather_de = "HEAT", "Sommerhitze"

    return {
        "ts_utc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "weather": weather,
        "weather_de": weather_de,
        "temp_c": round(temp, 1),
        "humidity": humidity,
        "wind_kmh": round(wind, 1),
        "city": "Zurich",
        "world_event": world_event_for_weather(weather),
    }


def main() -> int:
    pack = build_pack()
    out_path = os.path.join("content", "daily-pack.json")
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(pack, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"[ok] wrote {out_path}: {pack}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
