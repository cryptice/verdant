#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.11"
# dependencies = ["beautifulsoup4==4.14.3"]
# ///
"""Collect factual Impecta flower catalogue data; never writes to Verdant's API."""

import argparse
import hashlib
import json
import random
import time
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, quote, urljoin, urlparse
from urllib.request import HTTPRedirectHandler, Request, build_opener

from bs4 import BeautifulSoup

BASE = "https://www.impecta.se"
CATEGORIES = [
    "/sv/froer/ettariga-blommor",
    "/sv/froer/perenner",
    "/sv/froer/nordiska-vildblommor",
    "/sv/froer/krukvaxter/blommande-krukvaxter",
    "/sv/froer/buskar-och-trad/blommande",
    "/sv/lokar-och-knolar",
]


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Fetcher:
    """Sequential, cached requests with 1/2/3 seconds after the last response."""

    def __init__(self, cache):
        self.cache = cache
        cache.mkdir(parents=True, exist_ok=True)
        self.opener = build_opener(NoRedirect())
        self.has_requested = False

    def get(self, url, redirects=0):
        if urlparse(url).netloc != "www.impecta.se" or redirects > 5:
            raise ValueError(f"Unexpected URL/redirect: {url}")
        path = self.cache / (hashlib.sha256(url.encode()).hexdigest() + ".html")
        if path.exists():
            return path.read_text()
        for attempt in range(3):
            delay = random.choice((1, 2, 3)) if self.has_requested else 0
            time.sleep(delay)
            with (self.cache / "requests.jsonl").open("a") as log:
                log.write(json.dumps({"time": time.time(), "delaySeconds": delay, "url": url}) + "\n")
            self.has_requested = True
            try:
                request = Request(quote(url, safe=":/?=&%"), headers={"User-Agent": "Verdant catalogue import (sequential requests)"})
                with self.opener.open(request, timeout=45) as response:
                    html = response.read().decode("utf-8")
                path.write_text(html)
                return html
            except HTTPError as error:
                if error.code in (301, 302, 303, 307, 308):
                    html = self.get(urljoin(url, error.headers["Location"]), redirects + 1)
                    path.write_text(html)
                    return html
                if error.code not in (429, 500, 502, 503, 504) or attempt == 2:
                    raise
                time.sleep(max(10, int(error.headers.get("Retry-After", "10"))))
            except (URLError, TimeoutError):
                if attempt == 2:
                    raise
                time.sleep(10)
        raise RuntimeError(f"Unable to fetch {url}")


def listing(html):
    soup = BeautifulSoup(html, "html.parser")
    grid = soup.select_one(".PT_Wrapper_All.filter_loader")
    if grid is None:
        raise ValueError("Missing catalogue grid")
    products = []
    for card in grid.select(":scope > .PT_Wrapper"):
        link = card.select_one("a.box[href]")
        name = card.select_one(".PT_Beskr")
        if link and name:
            products.append({"url": urljoin(BASE, link["href"]), "name": name.get_text(" ", strip=True)})
    pages = max([1] + [int(parse_qs(urlparse(a["href"]).query).get("page", [1])[0])
                       for a in soup.select("a.pagination__item[href]")])
    return products, pages


def product(html, url, categories):
    soup = BeautifulSoup(html, "html.parser")
    name = soup.select_one("#ArtikelnamnFalt")
    if name is None:
        raise ValueError("Missing product name")
    specs = {}
    for column in soup.select("#propsIconTable > .column"):
        spans = column.find_all("span", recursive=False)
        if len(spans) == 2:
            specs[spans[0].get_text(" ", strip=True)] = spans[1].get_text(" ", strip=True)
    latin = soup.select_one(".propLation")
    sku = soup.select_one("#ArtnrFalt")
    return {
        "url": url,
        "name": name.get_text(" ", strip=True),
        "scientificName": latin.get_text(" ", strip=True) if latin else None,
        "sku": sku.get_text(strip=True) if sku else None,
        "categories": sorted(categories),
        "specifications": specs,
    }


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n")
    temporary.replace(path)


def collect(cache, output):
    fetcher = Fetcher(cache)
    products = {}
    for category in CATEGORIES:
        first, pages = listing(fetcher.get(BASE + category))
        for page in range(1, pages + 1):
            entries = first if page == 1 else listing(fetcher.get(f"{BASE}{category}?page={page}"))[0]
            if not entries:
                raise ValueError(f"Empty catalogue page: {category} page {page}")
            for entry in entries:
                products.setdefault(entry["url"], {**entry, "categories": set()})["categories"].add(category)
        print(f"{category}: {pages} pages; {len(products)} unique products so far", flush=True)
    save(cache / "catalogue.json", [{**p, "categories": sorted(p["categories"])} for p in products.values()])
    results, failures = [], []
    for index, (url, entry) in enumerate(sorted(products.items()), 1):
        try:
            results.append(product(fetcher.get(url), url, entry["categories"]))
        except (HTTPError, URLError, TimeoutError, ValueError) as error:
            failures.append({"url": url, "error": str(error)})
            print(f"Failed {url}: {error}", flush=True)
        if index % 20 == 0 or index == len(products):
            save(output, results)
            save(cache / "failures.json", failures)
            print(f"Products {index}/{len(products)}; parsed {len(results)}; failures {len(failures)}", flush=True)
    if failures:
        raise SystemExit("Some products failed; rerun to retry uncached pages before importing.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    collect(args.cache, args.output)
