"""Downloads the built-in station catalog (app/src/main/assets) from Radio Browser.

The app ships this snapshot so the list works where radio-browser.info is unreachable (e.g. Russia
without a VPN). Run before a release:  python tools/update_catalog.py

Two files:
- stations.json — every Russian station plus the world top 4000; read at startup and replaced in the app
  by the same daily online snapshot (RadioApi.snapshot);
- stations_world.json — rarer world stations (up to the world top 15000), read after the list is shown
  (StationCatalog.loadWorldExtras) so a bigger catalog doesn't slow the startup.
"""
import json
import os
import urllib.parse
import urllib.request

HOSTS = ["de1.api.radio-browser.info", "all.api.radio-browser.info"]
WORLD_TOP = 4000
WORLD_EXTRA_TOP = 15000
ASSETS = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")


def fetch(params):
    query = urllib.parse.urlencode({**params, "hidebroken": "true", "order": "clickcount", "reverse": "true"})
    last = None
    for host in HOSTS:
        try:
            req = urllib.request.Request(f"https://{host}/json/stations/search?{query}",
                                         headers={"User-Agent": "RadioJeka/1.1"})
            with urllib.request.urlopen(req, timeout=180) as r:
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


def unique(raw, seen):
    out = []
    for s in raw:
        c = compact(s)
        if c and c["u"] not in seen:
            seen.add(c["u"])
            out.append(c)
    out.sort(key=lambda s: -s["k"])
    return out


def save(name, stations):
    path = os.path.join(ASSETS, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(stations, f, ensure_ascii=False, separators=(",", ":"))
    print(f"{len(stations)} stations, {os.path.getsize(path) // 1024} KB -> {os.path.normpath(path)}")


def main():
    russia = fetch({"countrycode": "RU", "limit": 100000})
    world = fetch({"limit": WORLD_EXTRA_TOP})
    seen = set()
    core = unique(russia + world[:WORLD_TOP], seen)
    extra = unique(world[WORLD_TOP:], seen)
    save("stations.json", core)
    save("stations_world.json", extra)
    ru = sum(1 for s in core if s["cc"] == "RU")
    countries = len({s["cc"] for s in core + extra})
    print(f"total {len(core) + len(extra)} stations ({ru} RU) from {countries} countries")


if __name__ == "__main__":
    main()
