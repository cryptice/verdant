#!/usr/bin/env python3
"""Convert collected Impecta facts into a deduplicated Verdant admin import."""

import argparse
import json
import re
import unicodedata
from pathlib import Path
from urllib.parse import unquote, urlparse


def normalized(value):
    text = unicodedata.normalize("NFKD", value or "").casefold()
    text = "".join(char for char in text if not unicodedata.combining(char))
    text = re.sub(r"\bf1\b", "", text)
    return re.sub(r"[^\w]+", "", text)


def product_path(url):
    path = unquote(urlparse(url).path)
    return path.removeprefix("/sv/").lstrip("/").rstrip("/")


def split_name(name):
    name = re.sub(r"\s+\d+\s*st\.?$", "", name, flags=re.IGNORECASE)
    match = re.fullmatch(r"(.*?)\s*['‘’](.+)['‘’]", name)
    common, variant = match.groups() if match else (name, None)
    return common.strip(), variant


MONTHS = {name: index for index, names in enumerate([
    ("jan", "januari"), ("feb", "februari"), ("mars",), ("april",),
    ("maj",), ("juni",), ("juli",), ("aug", "augusti"),
    ("sep", "sept", "september"), ("okt", "oktober"),
    ("nov", "november"), ("dec", "december"),
], 1) for name in names}


def months(value):
    if not value or value == "-":
        return []
    result = set()
    for interval in re.split(r"[/,]", value.lower()):
        bounds = [MONTHS[part.strip()] for part in re.split(r"[-–]", interval)]
        if len(bounds) == 1:
            result.add(bounds[0])
        elif len(bounds) == 2:
            start, end = bounds
            result.update(range(start, end + 1) if start <= end else list(range(start, 13)) + list(range(1, end + 1)))
        else:
            raise ValueError(f"Unknown month range: {value}")
    return sorted(result)


def numeric_range(value, unit):
    match = re.fullmatch(r"(\d+)(?:\s*[-–]\s*(\d+))?\s*" + unit, value or "")
    if not match:
        return None, None
    low = int(match[1])
    high = int(match[2]) if match[2] else low
    if high < low:
        raise ValueError(f"Reversed range: {value}")
    return low, high


def identity_keys(entry):
    variants = {normalized(entry.get(k)) for k in ("variantName", "variantNameSv") if entry.get(k)} or {""}
    keys = set()
    for field in ("commonName", "commonNameSv"):
        if entry.get(field):
            keys.update(("name", normalized(entry[field]), variant) for variant in variants)
    # A named cultivar can be matched across common-name translations. A bare
    # botanical species can have distinct horticultural forms, so keep its name.
    if entry.get("scientificName"):
        keys.update(("taxon", normalized(entry["scientificName"]), variant) for variant in variants if variant)
    return keys


def catalogue_keys(entry):
    """Do not collapse different taxa that share a generic English common name."""
    return {key for key in identity_keys(entry)
            if key[0] == "taxon" or key[1] == normalized(entry.get("commonNameSv"))}


POSITIONS = {
    "sol": ["SUNNY"], "halvskugga": ["PARTIALLY_SUNNY"],
    "sol-halvskugga": ["SUNNY", "PARTIALLY_SUNNY"],
    "halv-helskugga": ["PARTIALLY_SUNNY", "SHADOWY"],
    "helskugga": ["SHADOWY"],
}


def convert(fact):
    common_sv, variant = split_name(fact["name"])
    specs = fact["specifications"]
    lifecycle = specs.get("Årighet", "")
    tags = ["Impecta"]
    if lifecycle:
        tags.append("Årighet: " + lifecycle)
    plant_type, unit = "ANNUAL", "SEED"
    if "Ett" not in lifecycle and "Två" in lifecycle:
        plant_type = "BIENNIAL"
    elif lifecycle == "Flerårig":
        plant_type = "PERENNIAL"
    if "/lokar-och-knolar/" in fact["url"] or "/sv/lokar-och-knolar" in fact.get("categories", []):
        if (fact.get("scientificName") or "").startswith("Paeonia "):
            plant_type, unit = "PERENNIAL", "PLANT"
        elif any(name in common_sv.lower() for name in ("dahlia", "ranunk", "bukettanemon", "balkansippa")):
            plant_type, unit = "TUBER", "TUBER"
        else:
            plant_type, unit = "BULB", "BULB"
    germ_min, germ_max = numeric_range(specs.get("Grotid"), "dagar")
    height_min, height_max = numeric_range(specs.get("Höjd"), "cm")
    return {
        "commonName": specs.get("Engelskt namn") or common_sv,
        "commonNameSv": common_sv, "variantName": variant, "variantNameSv": variant,
        "scientificName": fact["scientificName"],
        "germinationTimeDaysMin": germ_min, "germinationTimeDaysMax": germ_max,
        "heightCmMin": height_min, "heightCmMax": height_max,
        "bloomMonths": months(specs.get("Blomtid/Skördetid")),
        "sowingMonths": months(specs.get("Såtid")),
        "growingPositions": POSITIONS.get(specs.get("Växtläge"), []),
        "soils": [], "groupNames": [],
        "plantType": plant_type, "defaultUnitType": unit, "tagNames": tags,
        "providers": [{"providerName": "Impecta", "providerIdentifier": "impecta",
                       "productUrl": fact.get("productUrl", fact["url"]), "unitType": unit}],
    }


def impecta_exclusion(fact):
    name = fact["name"].lower()
    if any(part in fact["url"] for part in ("/gronsaker/", "/vitlok", "/potatis", "/sattlok")) or name.startswith("paprika "):
        return "Vegetable seeds or planting stock"
    if (name in ("prydnadsgräs", "kaktusmix") or re.fullmatch(r"\S+\s+(?:mix|spp\.?)", fact["scientificName"] or "", re.IGNORECASE)
            or "blandade arter" in name or "/" in split_name(name)[0]
            or name.startswith(("sommarblom ", "fröpaket ", "dahliakit ", "tulpanmix ", "narcissmix "))):
        return "Mixed-species product or missing botanical identity"
    return None


def prepare(facts, existing, *, converter=convert, exclusion=impecta_exclusion,
            provider_hosts=("impecta.se", "www.impecta.se"), match_overrides=None):
    records, review = [], {"matchedExisting": [], "duplicateProducts": [], "disambiguatedNames": [], "excluded": [], "errors": []}
    by_identity, by_url = {}, {}
    for entry in existing:
        if not entry.get("isSystem", True):
            continue
        for key in identity_keys(entry):
            by_identity.setdefault(key, []).append(entry)
        for provider in entry.get("providers", []):
            if provider.get("productUrl"):
                if urlparse(provider["productUrl"]).netloc in provider_hosts:
                    by_url[product_path(provider["productUrl"])] = entry
    pending = {}
    existing_import_keys = {(e["commonName"], e.get("variantName")) for e in existing}
    for fact in facts:
        reason = exclusion(fact)
        if reason:
            review["excluded"].append({"url": fact["url"], "reason": reason})
            continue
        try:
            entry = converter(fact)
        except (KeyError, ValueError) as error:
            review["errors"].append({"url": fact["url"], "error": str(error)})
            continue
        keys = identity_keys(entry)
        matches = {e.get("id", id(e)): e for key in keys for e in by_identity.get(key, [])}
        # A generic English name alone must not join two different botanical
        # species. Exact Swedish names can still identify an existing entry
        # whose old botanical name differs from the supplier's classification.
        matches = {identifier: e for identifier, e in matches.items()
                   if not e.get("scientificName") or not entry.get("scientificName")
                   or normalized(e["scientificName"]) == normalized(entry["scientificName"])
                   or normalized(entry["commonNameSv"]) in {normalized(e.get("commonName")), normalized(e.get("commonNameSv"))}}
        swedish_matches = {identifier: e for identifier, e in matches.items()
                           if normalized(e.get("commonNameSv")) == normalized(entry["commonNameSv"])}
        if swedish_matches:
            matches = swedish_matches
        url_match = by_url.get(product_path(fact["url"]))
        if url_match:
            matches = {url_match.get("id", id(url_match)): url_match}
        override = (match_overrides or {}).get(fact["url"])
        if override:
            matches = {e.get("id", id(e)): e for e in existing
                       if e.get("isSystem", True)
                       and (e["commonName"], e.get("variantName")) == tuple(override)}
            if len(matches) != 1:
                review["errors"].append({"url": fact["url"], "error": "Reviewed match is missing or ambiguous"})
                continue
        if len(matches) > 1:
            exact_keys = {(e["commonName"], e.get("variantName")) for e in matches.values()}
            if len(exact_keys) == 1:
                review["excluded"].append({"url": fact["url"], "reason": "Already exists more than once in Verdant; left untouched", "ids": list(matches)})
                continue
            review["errors"].append({"url": fact["url"], "error": "Ambiguous existing species", "ids": list(matches)})
            continue
        if matches:
            match = next(iter(matches.values()))
            # The admin importer only matches exact English common/variant names.
            entry["commonName"], entry["variantName"] = match["commonName"], match.get("variantName")
            review["matchedExisting"].append({"url": fact["url"], "id": match.get("id"), "name": match["commonName"]})
        source_keys = catalogue_keys(entry)
        duplicate = next((pending[key] for key in source_keys if key in pending), None)
        exact = (entry["commonName"], entry["variantName"])
        if not matches and exact in existing_import_keys:
            review["errors"].append({"url": fact["url"], "error": "Import key collides with a different existing species", "name": exact})
            continue
        if duplicate is not None:
            review["duplicateProducts"].append({"url": fact["url"], "reason": "Same species/variety already included"})
            continue
        if any((e["commonName"], e["variantName"]) == exact for e in records):
            entry["commonName"] = f"{entry['commonName']} ({entry['commonNameSv']})"
            review["disambiguatedNames"].append({"url": fact["url"], "original": exact[0], "commonName": entry["commonName"]})
            disambiguated = (entry["commonName"], entry["variantName"])
            if disambiguated in existing_import_keys or any((e["commonName"], e["variantName"]) == disambiguated for e in records):
                review["errors"].append({"url": fact["url"], "error": "Distinct species still share an import name", "name": disambiguated})
                continue
        records.append(entry)
        for key in source_keys:
            pending[key] = entry
    review["totalProducts"] = len(facts)
    review["importEntries"] = len(records)
    review["listingOnly"] = [{"name": f["name"], "url": f["url"], "categories": f["categories"]}
                             for f in facts if f.get("sourceStatus") == "catalogue-listing-only"]
    return records, review


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--facts", required=True, type=Path)
    parser.add_argument("--existing", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    args = parser.parse_args()
    records, review = prepare(json.loads(args.facts.read_text()), json.loads(args.existing.read_text()))
    for path, data in ((args.output, records), (args.report, review)):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({key: len(value) if isinstance(value, list) else value for key, value in review.items()}))
    if review["errors"]:
        raise SystemExit("Resolve the review errors before importing.")
