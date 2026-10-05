import unittest

from collect_florea import listing, product
from prepare_florea import convert, depth_mm, exclusion, prepare_florea, split_title


def flower():
    return {"name": "Vallmo - Bowling Ball", "scientificName": "Papaver somniferum",
            "url": "https://floreagarden.com/products/test", "productType": "Frö",
            "categories": ["/collections/froer-till-blommor"], "sourceTags": [],
            "specifications": {"Årighet": "Ettårig", "Grotid": "15-30 dagar", "Höjd": "70 cm",
                               "Sådjup": "0,5 cm", "Växtläge": "Sol, Halvskugga"},
            "bloomMonths": [6, 7, 8], "sowingMonths": [3, 4, 5]}


class FloreaTest(unittest.TestCase):
    def test_listing_deduplicates_links_and_ignores_promotions(self):
        html = '''<a href="/products/calendar">Calendar</a><div id="product-grid-ajax">
        <a href="/products/flower?variant=1">Flower</a><a href="/products/flower">Flower</a></div>
        <infinite-scroll data-next-url="/collections/flowers?page=2"></infinite-scroll>'''
        entries, next_url = listing(html)
        self.assertEqual(entries, [{"url": "https://floreagarden.com/products/flower", "name": "Flower"}])
        self.assertEqual(next_url, "https://floreagarden.com/collections/flowers?page=2")

    def test_calendar_reads_marks_instead_of_all_month_labels(self):
        names = ["Jan", "Feb", "Mar", "Apr", "Maj", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dec"]
        months = ''.join(f'<div class="month {"sowing" if i == 2 else "harvest" if i == 6 else ""}">{m}</div>' for i, m in enumerate(names))
        html = f'''<main><h1>Vallmo - Test</h1></main><p class="product__latin-name">Frö · Papaver somniferum</p>
        <div class="product-calendar">{months}</div><dl class="product-information"><div class="product-details">
        <div><dt>Grotid</dt><dd>15-30 dagar</dd></div></div></dl>'''
        fact = product(html, "url", ["category"])
        self.assertEqual(fact["sowingMonths"], [3])
        self.assertEqual(fact["bloomMonths"], [7])
        self.assertEqual(fact["scientificName"], "Papaver somniferum")
        self.assertEqual(fact["specifications"]["Grotid"], "15-30 dagar")

    def test_depth_uses_millimetres_without_inventing_a_value_for_ranges(self):
        self.assertEqual(depth_mm("0,5 cm"), 5)
        self.assertEqual(depth_mm("0 cm"), 0)
        self.assertIsNone(depth_mm("1-2 cm"))

    def test_title_separators_do_not_split_hyphenated_cultivars(self):
        self.assertEqual(split_title("Fjärilsranunkel – Eris"), ("Fjärilsranunkel", "Eris"))
        self.assertEqual(split_title("Blomma - Pink-White"), ("Blomma", "Pink-White"))
        self.assertEqual(split_title("Blåklint - Blue Ball EKO"), ("Blåklint", "Blue Ball"))
        self.assertEqual(split_title("Lejongap - Bronze F1 (Storpack)"), ("Lejongap", "Bronze F1"))

    def test_maps_growing_data_and_florea_provider(self):
        result = convert(flower())
        self.assertEqual(result["sowingDepthMm"], 5)
        self.assertEqual(result["growingPositions"], ["SUNNY", "PARTIALLY_SUNNY"])
        self.assertEqual(result["providers"][0]["providerIdentifier"], "florea")

    def test_seed_with_missing_lifecycle_requires_review(self):
        fact = flower()
        fact["specifications"].pop("Årighet")
        with self.assertRaises(ValueError):
            convert(fact)

    def test_same_cultivar_from_another_provider_reuses_species(self):
        existing = [{"id": 8, "isSystem": True, "commonName": "Poppy", "commonNameSv": "Vallmo",
                     "variantName": "Bowling Ball", "scientificName": "Papaver somniferum",
                     "providers": [{"productUrl": "https://www.impecta.se/products/test"}]}]
        entries, report = prepare_florea([flower()], existing)
        self.assertEqual(entries[0]["commonName"], "Poppy")
        self.assertEqual(report["matchedExisting"][0]["id"], 8)

    def test_does_not_match_another_provider_by_url_path(self):
        existing = [{"id": 8, "commonName": "Different species", "variantName": None,
                     "providers": [{"productUrl": "https://www.impecta.se/products/test"}]}]
        _, report = prepare_florea([flower()], existing)
        self.assertFalse(report["matchedExisting"])

    def test_excludes_mixed_species_packs(self):
        fact = flower()
        fact.update(name="Frömix - Pastellbukett", scientificName=None)
        self.assertEqual(exclusion(fact), "Mixed-species product")

    def test_single_species_colour_mix_is_kept(self):
        fact = flower()
        fact.update(name="Blåklintmix EKO", scientificName=None)
        self.assertIsNone(exclusion(fact))

    def test_combined_lifecycle_retains_tag_and_uses_annual_cultivation(self):
        fact = flower()
        fact["specifications"]["Årighet"] = "EttårigTvåårig"
        result = convert(fact)
        self.assertEqual(result["plantType"], "ANNUAL")
        self.assertIn("Årighet: EttårigTvåårig", result["tagNames"])

    def test_reviewed_match_requires_unique_existing_shared_species(self):
        fact = flower()
        adjustment = {fact["url"]: {"match": ["Poppy", "Existing"], "reason": "Reviewed alias"}}
        entries, report = prepare_florea([fact], [], adjustment)
        self.assertEqual(entries, [])
        self.assertEqual(len(report["errors"]), 1)
        existing = [{"commonName": "Poppy", "variantName": "Existing", "scientificName": "Papaver",
                     "isSystem": True, "id": 3}]
        entries, report = prepare_florea([fact], existing, adjustment)
        self.assertEqual(entries[0]["variantName"], "Existing")
        self.assertEqual(report["matchedExisting"][0]["id"], 3)

    def test_reviewed_corrections_do_not_modify_source_facts(self):
        fact = flower()
        fact["specifications"].pop("Årighet")
        adjustment = {fact["url"]: {"corrections": {"specifications": {"Årighet": "Ettårig"}},
                                  "reason": "Reviewed source"}}
        entries, report = prepare_florea([fact], [], adjustment)
        self.assertEqual(len(entries), 1)
        self.assertNotIn("Årighet", fact["specifications"])
        self.assertEqual(len(report["reviewedAdjustments"]), 1)


if __name__ == "__main__":
    unittest.main()
