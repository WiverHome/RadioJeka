"""Downloads the built-in station catalog (app/src/main/assets/stations.json) from Radio Browser.

The app ships this snapshot so the list works where radio-browser.info is unreachable (e.g. Russia
without a VPN). Run before a release:  python tools/update_catalog.py
"""
import json
import os
import urllib.parse
import urllib.request

HOSTS = ["de1.api.radio-browser.info", "all.api.radio-browser.info"]
WORLD_TOP = 4000
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "stations.json")


def fetch(params):
    query = urllib.parse.urlencode({**params, "hidebroken": "true", "order": "clickcount", "reverse": "true"})
    last = None
    for host in HOSTS:
        try:
            req = urllib.request.Request(f"https://{host}/json/stations/search?{query}",
                                         headers={"User-Agent": "RadioJeka/1.0"})
            with urllib.request.urlopen(req, timeout=60) as r:
                return json.load(r)
        except Exception as e:  # try the next mirror
            last = e
    raise last


def compact(s):
    url = (s.get("url_resolved") or s.get("url") or "").strip()
    name = (s.get("name") or "").strip()
    if not url or not name:
        return None
    return {
        "id": s["stationuuid"], "n": name, "u": url, "f": (s.get("favicon") or "").strip(),
        "c": (s.get("country") or "").strip(), "cc": (s.get("countrycode") or "").strip(),
        "t": s.get("tags") or "", "b": s.get("bitrate") or 0, "k": s.get("clickcount") or 0,
    }


def main():
    raw = fetch({"countrycode": "RU", "limit": 100000}) + fetch({"limit": WORLD_TOP})
    seen, stations = set(), []
    for s in raw:
        c = compact(s)
        if c and c["u"] not in seen:
            seen.add(c["u"])
            stations.append(c)
    stations.sort(key=lambda s: -s["k"])
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(stations, f, ensure_ascii=False, separators=(",", ":"))
    ru = sum(1 for s in stations if s["cc"] == "RU")
    print(f"{len(stations)} stations ({ru} RU), {os.path.getsize(OUT) // 1024} KB -> {os.path.normpath(OUT)}")


if __name__ == "__main__":
    main()
