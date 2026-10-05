#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.11"
# dependencies = ["beautifulsoup4==4.14.3"]
# ///
"""Collect factual Florea Garden flower data with sequential, paced requests."""

import argparse
import json
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin, urlparse

from bs4 import BeautifulSoup

from collect_impecta import Fetcher, save

BASE = "https://floreagarden.com"
HOSTS = ("floreagarden.com", "www.floreagarden.com")
CATEGORIES = (
    "/collections/froer-till-blommor",
    "/collections/blomsterlok-knolar",
    "/collections/barrotade-pioner-pionrotter",
    # The broad catalogues omit some products shown in their subcategories.
    *("/collections/" + slug for slug in (
        "lok-knolmixar", "lok-till-allium", "amaryllis", "anemoner", "lok-till-hyacinter",
        "lokar-till-narcisser", "ranunkel", "snodroppar", "lokar-till-tulpaner",
        "lokar-till-gladiolus", "dahlia", "klocklilja-lok-till-allium", "lok-till-krokus",
        "prydnadsgras", "sommarsnoklocka-lok-till-sommarsnoklocka", "solhatt",
        "froer-till-amarant", "froer-till-aster", "froer-till-atlasblomma",
        "froer-till-blomsterkrasse", "froer-till-blaklint", "froer-till-bukettmixar",
        "utfyllnadsblommor", "froer-till-celosia", "froer-till-eucalyptus",
        "froer-till-fingerborgsblomma", "honungsfacelia", "froer-till-jatteeternell",
        "froer-till-kinesisk-forgatmigej", "froer-till-klockranka", "froer-till-lavendel",
        "froer-till-lejongap", "froer-till-luktarter", "froer-till-lovkoja",
        "froer-till-mattram", "froer-till-petunia", "froer-till-prarieklocka",
        "froer-till-riddarsporre", "froer-till-risp", "froer-till-rosenskara",
        "froer-till-sommarrudbeckia", "froer-till-rysk-martorn", "sibirisk-vallmo",
        "froer-till-solrosor", "froer-till-sommarmalva", "froer-till-sommarflox",
        "froer-till-spetsblomma", "froer-till-stockrosor", "svenska-froer",
        "froer-till-somntuta", "froer-till-vallmo", "froer-till-vadd",
        "froer-till-zinnia", "froer-till-angsblandning",
    )),
)
MONTH_LABELS = ["Jan", "Feb", "Mar", "Apr", "Maj", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dec"]


def listing(html):
    soup = BeautifulSoup(html, "html.parser")
    grid = soup.select_one("#product-grid-ajax")
    if grid is None:
        raise ValueError("Missing catalogue grid")
    products = {}
    for link in grid.select('a[href*="/products/"]'):
        path = urlparse(link["href"]).path
        path = "/products/" + path.split("/products/", 1)[1]
        url = BASE + path
        name = link.get_text(" ", strip=True)
        if url not in products or name:
            products[url] = {"url": url, "name": name}
    next_page = soup.select_one("infinite-scroll[data-next-url]")
    return list(products.values()), urljoin(BASE, next_page["data-next-url"]) if next_page else None


def product(html, url, categories):
    soup = BeautifulSoup(html, "html.parser")
    title = soup.select_one("main h1")
    if title is None:
        raise ValueError("Missing product title")
    marker = "var productInfo = "
    metadata = {}
    if marker in html:
        metadata, _ = json.JSONDecoder().raw_decode(html.split(marker, 1)[1])
    specs = {}
    for term in soup.select("dl.product-information .product-details dt"):
        value = term.find_next_sibling("dd")
        if value:
            specs[term.get_text(" ", strip=True)] = value.get_text(" ", strip=True)
    calendar = soup.select(".product-calendar .month")
    if calendar and [m.get_text(strip=True) for m in calendar] != MONTH_LABELS:
        raise ValueError("Unexpected month calendar")
    latin = soup.select_one(".product__latin-name")
    kind, separator, botanical = latin.get_text(" ", strip=True).partition("·") if latin else ("", "", "")
    return {
        "url": url, "name": title.get_text(" ", strip=True),
        "scientificName": botanical.strip() if separator else None,
        "productType": metadata.get("type") or kind.strip(),
        "sourceTags": metadata.get("tags", []),
        "skus": [v["sku"] for v in metadata.get("variants", []) if v.get("sku")],
        "categories": sorted(categories), "specifications": specs,
        "sowingMonths": [i for i, m in enumerate(calendar, 1) if "sowing" in m.get("class", [])],
        "bloomMonths": [i for i, m in enumerate(calendar, 1) if "harvest" in m.get("class", [])],
    }


def collect(cache, output):
    fetcher = Fetcher(cache, HOSTS)
    products = {}
    for category in CATEGORIES:
        url, visited = BASE + category, set()
        while url:
            if url in visited:
                raise ValueError(f"Repeated pagination URL: {url}")
            visited.add(url)
            entries, url = listing(fetcher.get(url))
            if not entries and len(visited) > 1:
                raise ValueError(f"Empty catalogue page: {category}")
            for entry in entries:
                products.setdefault(entry["url"], {**entry, "categories": set()})["categories"].add(category)
        print(f"{category}: {len(visited)} pages; {len(products)} unique products so far", flush=True)
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
        raise SystemExit("Resolve failed products before importing; cached pages are reusable.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    collect(args.cache, args.output)
