"""Refresh the sponsor grid in README.md and README.en.md from afdian.

Reads AFDIAN_USER_ID and AFDIAN_TOKEN from the environment and rewrites the
block between the sponsor markers. Only names and avatars are written; amounts,
dates and messages stay private.

Local dry run without the API: python update_sponsors.py --from-json sample.json
"""

import hashlib
import html
import json
import os
import sys
import time
import urllib.request

API = "https://afdian.com/api/open/query-sponsor"
START = "<!-- sponsors:start -->"
END = "<!-- sponsors:end -->"
READMES = ["README.md", "README.en.md"]
PER_ROW = 6


def fetch_page(user_id, token, page):
    params = json.dumps({"page": page, "per_page": 100}, separators=(",", ":"))
    ts = int(time.time())
    sign = hashlib.md5(f"{token}params{params}ts{ts}user_id{user_id}".encode()).hexdigest()
    body = json.dumps({"user_id": user_id, "params": params, "ts": ts, "sign": sign}).encode()
    req = urllib.request.Request(API, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.load(resp)
    if data.get("ec") != 200:
        raise SystemExit(f"afdian error {data.get('ec')}: {data.get('em')}")
    return data["data"]


def fetch_all(user_id, token):
    sponsors, page = [], 1
    while True:
        data = fetch_page(user_id, token, page)
        sponsors += data.get("list", [])
        if page >= data.get("total_page", 1):
            return sponsors
        page += 1


def avatar(url):
    if not url:
        return "https://pic1.afdiancdn.com/default/avatar/avatar-purple.png"
    return url if "imageView2" in url else url + "?imageView2/1/w/120/h/120"


def render(sponsors):
    sponsors = sorted(sponsors, key=lambda s: s.get("first_pay_time", 0))
    seen, cells = set(), []
    for s in sponsors:
        user = s.get("user", {})
        uid = user.get("user_id")
        if uid in seen:
            continue
        seen.add(uid)
        name = html.escape(user.get("name", ""))
        src = html.escape(avatar(user.get("avatar")))
        cells.append(
            f'    <td align="center"><img src="{src}" width="64" height="64" alt="{name}"><br><sub>{name}</sub></td>'
        )
    if not cells:
        return ""
    rows = ["  <tr>\n" + "\n".join(cells[i:i + PER_ROW]) + "\n  </tr>" for i in range(0, len(cells), PER_ROW)]
    return "<table>\n" + "\n".join(rows) + "\n</table>"


def write(block):
    changed = False
    for path in READMES:
        with open(path, encoding="utf-8", newline="") as f:
            text = f.read()
        head, sep, rest = text.partition(START)
        if not sep or END not in rest:
            raise SystemExit(f"{path}: sponsor markers not found")
        tail = rest.split(END, 1)[1]
        new = f"{head}{START}\n{block}\n{END}{tail}"
        if new != text:
            with open(path, "w", encoding="utf-8", newline="") as f:
                f.write(new)
            changed = True
    return changed


def main():
    if len(sys.argv) == 3 and sys.argv[1] == "--from-json":
        with open(sys.argv[2], encoding="utf-8") as f:
            sponsors = json.load(f)["data"]["list"]
    else:
        user_id = os.environ.get("AFDIAN_USER_ID")
        token = os.environ.get("AFDIAN_TOKEN")
        if not user_id or not token:
            print("AFDIAN_USER_ID or AFDIAN_TOKEN not set, skipping")
            return
        sponsors = fetch_all(user_id, token)
    print("updated" if write(render(sponsors)) else "unchanged")


if __name__ == "__main__":
    main()
