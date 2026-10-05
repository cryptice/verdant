#!/usr/bin/env python3
"""Prepare Florea Garden flowers for Verdant's shared species import."""

import argparse
import copy
import json
import re
from decimal import Decimal
from pathlib import Path

from prepare_impecta import numeric_range, prepare


def split_title(title):
    title = re.sub(r"\s*\(Storpack\)$", "", title, flags=re.IGNORECASE)
    title = re.sub(r"\s+EKO$", "", title)
    parts = re.split(r"\s+[-–—]\s+", re.sub(r"\s+\d+\s*(?:st|pack)$", "", title), maxsplit=1)
    return parts[0].strip(), parts[1].strip(" '\"‘’") if len(parts) == 2 else None


def exclusion(fact):
    if fact.get("excludeReason"):
        return fact["excludeReason"]
    common, _ = split_title(fact["name"])
    if any(word in common.casefold() for word in ("skärböna", "sockerärt")):
        return "Vegetable seeds"
    if any(word in common.casefold() for word in ("frömix", "bukettmix", "ängsblandning", "blomsterblandning", "fröpaket", "lökpaket", "fjärilsblandning")):
        return "Mixed-species product"
    if not fact["scientificName"] and "mix" in common.casefold() and "blåklint" not in common.casefold():
        return "Mixed-species product"
    return None


def depth_mm(value):
    match = re.fullmatch(r"(\d+(?:[.,]\d+)?)\s*(cm|mm)", value or "")
    if not match:
        return None
    depth = Decimal(match[1].replace(",", ".")) * (10 if match[2] == "cm" else 1)
    return int(depth) if depth == depth.to_integral_value() else None


def convert(fact):
    common, variant = split_title(fact["name"])
    specs = fact["specifications"]
    lifecycle = specs.get("Årighet", "")
    tags = ["Florea"] + (["Årighet: " + lifecycle] if lifecycle else [])
    kind = fact["productType"].casefold()
    if kind in ("frö", "fröer"):
        unit = "SEED"
        types = {"Ettårig": "ANNUAL", "Tvåårig": "BIENNIAL", "Flerårig": "PERENNIAL"}
        if "Ettårig" in lifecycle:
            plant_type = "ANNUAL"
        elif lifecycle in types:
            plant_type = types[lifecycle]
        else:
            raise ValueError(f"Unknown seed lifecycle: {lifecycle!r}")
    elif "/collections/barrotade-pioner-pionrotter" in fact["categories"]:
        plant_type, unit = "PERENNIAL", "PLANT"
    elif kind in ("rot", "planta") and lifecycle == "Flerårig":
        plant_type, unit = "PERENNIAL", "PLANT"
    elif kind in ("lök", "blomsterlök"):
        plant_type, unit = "BULB", "BULB"
    elif kind in ("knöl", "dahliaknöl"):
        plant_type, unit = "TUBER", "TUBER"
    else:
        raise ValueError(f"Unknown product type: {fact['productType']!r}")
    germ_min, germ_max = numeric_range(specs.get("Grotid"), "dagar")
    height_min, height_max = numeric_range(specs.get("Höjd"), "cm")
    positions = {"sol": "SUNNY", "halvskugga": "PARTIALLY_SUNNY", "skugga": "SHADOWY"}
    light = [p.strip().casefold() for p in specs.get("Växtläge", "").split(",") if p.strip()]
    if any(p not in positions for p in light):
        raise ValueError(f"Unknown growing position: {light}")
    return {
        "commonName": common, "commonNameSv": common,
        "variantName": variant, "variantNameSv": variant,
        "scientificName": fact["scientificName"],
        "germinationTimeDaysMin": germ_min, "germinationTimeDaysMax": germ_max,
        "heightCmMin": height_min, "heightCmMax": height_max,
        "sowingDepthMm": depth_mm(specs.get("Sådjup")),
        "bloomMonths": fact["bloomMonths"], "sowingMonths": fact["sowingMonths"],
        "growingPositions": [positions[p] for p in light], "soils": [], "groupNames": [],
        "plantType": plant_type, "defaultUnitType": unit, "tagNames": tags,
        "providers": [{"providerName": "Florea", "providerIdentifier": "florea",
                       "productUrl": fact["url"], "unitType": unit}],
    }


def prepare_florea(facts, existing, adjustments=None):
    facts = copy.deepcopy(facts)
    adjustments = adjustments or {}
    overrides, applied = {}, []
    for fact in facts:
        adjustment = adjustments.get(fact["url"])
        if not adjustment:
            continue
        if adjustment.get("match"):
            overrides[fact["url"]] = adjustment["match"]
        changes = copy.deepcopy(adjustment.get("corrections", {}))
        fact["specifications"].update(changes.pop("specifications", {}))
        fact.update(changes)
        applied.append({"url": fact["url"], **adjustment})
    records, report = prepare(facts, existing, converter=convert, exclusion=exclusion,
                              provider_hosts=("floreagarden.com", "www.floreagarden.com"),
                              match_overrides=overrides)
    report["reviewedAdjustments"] = applied
    return records, report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--facts", required=True, type=Path)
    parser.add_argument("--existing", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    parser.add_argument("--adjustments", type=Path, help="Reviewed corrections and exact existing-species matches")
    args = parser.parse_args()
    adjustments = json.loads(args.adjustments.read_text()) if args.adjustments else None
    records, report = prepare_florea(json.loads(args.facts.read_text()), json.loads(args.existing.read_text()), adjustments)
    for path, data in ((args.output, records), (args.report, report)):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({key: len(value) if isinstance(value, list) else value for key, value in report.items()}))
    if report["errors"]:
        raise SystemExit("Resolve the review errors before importing.")
