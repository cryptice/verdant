import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import MagicMock, patch

from collect_impecta import Fetcher, listing, product
from prepare_impecta import convert, months, numeric_range, prepare, split_name


def flower(name="Testblomma 'Blue'", scientific="Exemplum caeruleum", url=None):
    return {
        "name": name, "scientificName": scientific,
        "url": url or "https://www.impecta.se/froer/perenner/test-blue",
        "specifications": {"Årighet": "Tvåårig", "Grotid": "10-30 dagar", "Höjd": "50-90 cm",
                           "Såtid": "mars-maj/okt-nov", "Blomtid/Skördetid": "juni-aug",
                           "Växtläge": "sol-halvskugga", "Engelskt namn": "Test Flower"},
    }


class ImpectaTest(unittest.TestCase):
    def test_fetcher_delays_between_live_requests_but_reuses_cache(self):
        with TemporaryDirectory() as directory, patch("collect_impecta.time.sleep") as sleep, patch("collect_impecta.random.choice", return_value=3) as choice:
            fetcher = Fetcher(Path(directory))
            response = MagicMock()
            response.__enter__.return_value.read.return_value = b"<html>test</html>"
            fetcher.opener.open = MagicMock(return_value=response)
            fetcher.get("https://www.impecta.se/first")
            fetcher.get("https://www.impecta.se/first")
            fetcher.get("https://www.impecta.se/luktärt")
            self.assertEqual([call.args[0] for call in sleep.call_args_list], [0, 3])
            choice.assert_called_once_with((1, 2, 3))
            self.assertEqual(fetcher.opener.open.call_count, 2)
            request = fetcher.opener.open.call_args.args[0]
            self.assertIn("lukt%C3%A4rt", request.full_url)

    def test_product_reads_only_specification_columns(self):
        html = '''<h1 id="ArtikelnamnFalt">Test 'Blue'</h1><div class="propLation">Exemplum</div>
        <div id="propsIconTable"><div class="column"><span>Höjd</span><span>80 cm</span></div></div>
        <div class="column"><span>Höjd</span><span>999 cm</span></div>'''
        self.assertEqual(product(html, "url", ["category"])["specifications"], {"Höjd": "80 cm"})

    def test_listing_ignores_recommendations(self):
        html = '''<div class="PT_Wrapper_All filter_loader"><div class="PT_Wrapper">
        <a class="box" href="/froer/test"></a><div class="PT_Beskr">Test</div></div></div>
        <div class="PT_Wrapper"><a class="box" href="/not-a-result"></a></div>
        <a class="pagination__item" href="?page=19">19</a>'''
        entries, pages = listing(html)
        self.assertEqual(len(entries), 1)
        self.assertEqual(pages, 19)

    def test_disjoint_and_year_crossing_month_ranges(self):
        self.assertEqual(months("mars-maj/okt-nov"), [3, 4, 5, 10, 11])
        self.assertEqual(months("nov-feb"), [1, 2, 11, 12])
        with self.assertRaises(KeyError):
            months("våren")

    def test_does_not_invent_days_from_months(self):
        self.assertEqual(numeric_range("1-3 mån", "dagar"), (None, None))
        self.assertEqual(numeric_range("90 cm", "cm"), (90, 90))

    def test_cultivar_with_apostrophe(self):
        self.assertEqual(split_name("Test 'Queen's Choice'"), ("Test", "Queen's Choice"))

    def test_pack_sizes_do_not_become_species_or_cultivar_names(self):
        self.assertEqual(split_name("Tulpan 'Blue' 7 st"), ("Tulpan", "Blue"))
        self.assertEqual(split_name("Klotlök 100 st"), ("Klotlök", None))

    def test_biennial_and_tuber_types(self):
        entry = convert(flower())
        self.assertEqual(entry["plantType"], "BIENNIAL")
        self.assertEqual(entry["germinationTimeDaysMax"], 30)
        self.assertEqual(entry["soils"], [])
        self.assertEqual(entry["groupNames"], [])
        entry = convert(flower("Dahlia 'Blue'", url="https://www.impecta.se/lokar-och-knolar/dahlia-blue"))
        self.assertEqual((entry["plantType"], entry["defaultUnitType"]), ("TUBER", "TUBER"))

    def test_anemone_tulip_is_a_bulb_not_an_anemone_tuber(self):
        entry = convert(flower("Anemontulpan 'Blue' 10 st", url="https://www.impecta.se/lokar-och-knolar/anemontulpan-blue"))
        self.assertEqual(entry["plantType"], "BULB")

    def test_peony_flowered_dahlia_is_still_a_tuber(self):
        entry = convert(flower("Piondahlia 'Blue' 1 st", "Dahlia x pinnata", "https://www.impecta.se/lokar-och-knolar/piondahlia-blue"))
        self.assertEqual((entry["plantType"], entry["defaultUnitType"]), ("TUBER", "TUBER"))

    def test_matches_translated_names_and_preserves_import_key(self):
        existing = [{"id": 5, "commonName": "Original Name", "commonNameSv": "Testblomma",
                     "variantName": "blue", "isSystem": True}]
        entries, report = prepare([flower()], existing)
        self.assertEqual((entries[0]["commonName"], entries[0]["variantName"]), ("Original Name", "blue"))
        self.assertEqual(report["matchedExisting"][0]["id"], 5)

    def test_matches_localized_provider_url(self):
        existing = [{"id": 6, "commonName": "Old Name", "variantName": None, "isSystem": True,
                     "providers": [{"productUrl": "https://www.impecta.se/sv/froer/perenner/test-blue"}]}]
        entries, report = prepare([flower()], existing)
        self.assertEqual(entries[0]["commonName"], "Old Name")
        self.assertFalse(report["errors"])

    def test_matches_cultivar_accent_and_hyphen_variations(self):
        existing = [{"id": 5, "commonName": "Dahlia", "commonNameSv": "Dahlia",
                     "variantName": "Cafe-au-Lait", "scientificName": "Dahlia x pinnata"}]
        entries, report = prepare([flower("Dahlia 'Café au Lait'", "Dahlia x pinnata")], existing)
        self.assertEqual(entries[0]["variantName"], "Cafe-au-Lait")
        self.assertEqual(report["matchedExisting"][0]["id"], 5)

    def test_rejects_ambiguous_existing_matches(self):
        existing = [{"id": i, "commonName": f"Test Flower {i}", "commonNameSv": "Testblomma", "variantName": "Blue"} for i in [1, 2]]
        entries, report = prepare([flower()], existing)
        self.assertFalse(entries)
        self.assertEqual(len(report["errors"]), 1)

    def test_leaves_preexisting_duplicates_untouched(self):
        existing = [{"id": i, "commonName": "Test Flower", "variantName": "Blue"} for i in [1, 2]]
        entries, report = prepare([flower()], existing)
        self.assertFalse(entries)
        self.assertFalse(report["errors"])
        self.assertEqual(report["excluded"][0]["ids"], [1, 2])

    def test_same_cultivar_deduplicates_but_different_variety_survives(self):
        entries, report = prepare([flower(), flower(), flower("Testblomma 'Red'")], [])
        self.assertEqual(len(entries), 2)
        self.assertEqual(len(report["duplicateProducts"]), 1)

    def test_excludes_mixed_species_and_vegetables(self):
        entries, report = prepare([flower("Sommarblom Blandning", scientific=None), flower(url="https://www.impecta.se/lokar-och-knolar/vitlok-test")], [])
        self.assertFalse(entries)
        self.assertEqual(len(report["excluded"]), 2)

    def test_named_flower_can_have_an_unknown_botanical_name(self):
        entries, report = prepare([flower("Tulpan 'Tabledance' 7 st", scientific=None)], [])
        self.assertEqual(entries[0]["variantName"], "Tabledance")
        self.assertIsNone(entries[0]["scientificName"])
        self.assertFalse(report["excluded"])

    def test_listing_only_record_does_not_link_to_a_broken_product_page(self):
        fact = flower("Kvicklök 25 st", None, "https://www.impecta.se/lokar-och-knolar/missing")
        fact.update({"productUrl": None, "specifications": {}, "categories": ["/sv/lokar-och-knolar"]})
        entry = convert(fact)
        self.assertIsNone(entry["providers"][0]["productUrl"])
        self.assertIsNone(entry["heightCmMin"])
        self.assertEqual(entry["plantType"], "BULB")

    def test_does_not_collapse_distinct_species_with_same_english_name(self):
        entries, report = prepare([flower(), flower("Annan blomma 'Blue'", "Alterum caeruleum")], [])
        self.assertEqual(len(entries), 2)
        self.assertFalse(report["errors"])
        self.assertFalse(report["duplicateProducts"])
        self.assertEqual(entries[1]["commonName"], "Test Flower (Annan blomma)")
        self.assertEqual(len(report["disambiguatedNames"]), 1)

    def test_english_name_alone_does_not_match_another_existing_taxon(self):
        existing = [{"id": 1, "commonName": "Test Flower", "commonNameSv": "Annan blomma",
                     "variantName": "Blue", "scientificName": "Alterum caeruleum"}]
        entries, report = prepare([flower()], existing)
        self.assertFalse(report["matchedExisting"])
        self.assertFalse(entries)
        self.assertEqual(len(report["errors"]), 1)

    def test_reimport_prefers_exact_swedish_name_over_generic_english(self):
        existing = [
            {"id": 1, "commonName": "Test Flower", "commonNameSv": "Annan blomma", "variantName": "Blue", "scientificName": "Exemplum caeruleum"},
            {"id": 2, "commonName": "Test Flower (Testblomma)", "commonNameSv": "Testblomma", "variantName": "Blue", "scientificName": "Exemplum caeruleum"},
        ]
        entries, report = prepare([flower()], existing)
        self.assertFalse(report["errors"])
        self.assertEqual(entries[0]["commonName"], "Test Flower (Testblomma)")


if __name__ == "__main__":
    unittest.main()
