# Impecta flower catalogue

Imported into `verdant-prod` on 2026-10-05: **1,522 new shared species/varieties**
and **111 Impecta provider links** added to existing species. The catalogue grew
from 165 to 1,687 entries. All original species data was preserved, and every
imported field on the new entries was checked against the prepared input.
The dated result JSON records counts, created IDs, and input hashes.

The import covers the public Swedish catalogue's annual flowers, perennials,
Nordic wildflowers, flowering houseplants, flowering shrubs/trees, and flower
bulbs/tubers. Named cultivars remain separate Verdant species entries. Pack sizes
of the same species/cultivar are deduplicated. Mixed-species products and vegetable
planting stock are excluded and listed in the review report.

## Collect and prepare

```sh
uv run scripts/collect_impecta.py \
  --cache /private/tmp/verdant-impecta-cache \
  --output /private/tmp/verdant-impecta-facts.json

python3 scripts/prepare_impecta.py \
  --facts /private/tmp/verdant-impecta-facts.json \
  --existing /private/tmp/verdant-impecta-existing.json \
  --output docs/imports/impecta-species-2026-10-05.json \
  --report docs/imports/impecta-review-2026-10-05.json

uv run --with beautifulsoup4 python -m unittest discover -s scripts -p test_impecta.py
```

The collector makes one request at a time and randomly waits **1, 2, or 3 seconds**
before each subsequent request, including redirects and retries. Completed pages
are cached; rerunning resumes without requesting cached pages. HTTP throttling
and transient server errors receive additional backoff. Request timestamps and
chosen pauses are recorded in the cache's `requests.jsonl`.

Supply a fresh `/api/admin/species` response as `--existing`. Save an admin species
export as a backup before importing. The preparation command never modifies
production. Resolve any review errors before uploading the generated JSON using
the admin Species import action (or its authenticated API).

## Mapping and limits

- Names, botanical names, germination ranges stated in days, heights, sowing and
  flowering months, and sun exposure come from product specifications. Each
  entry links to its source product under provider **Impecta**.
  Two unavailable product pages (Kvicklök and Prydnadslök 'Cameleon') have only
  catalogue-confirmed names and bulb classification; their growing details and
  product links remain blank. They are identified under `listingOnly` in the
  review report. Tulpan 'Tabledance' has no botanical name in the source.
- English names are used when supplied; otherwise the Swedish name is retained
  as the required primary name. When distinct species share the same English
  import key, the Swedish name is appended to distinguish them. Unknown values are left blank. Marketing text,
  reviews, and photos are not copied.
- `Årighet: …` tags preserve the source lifecycle. Biennials use `BIENNIAL`;
  plants sold for either annual or longer cultivation use `ANNUAL` when the source
  includes `Ett`. Bulbs, tubers, and bare-root peonies receive their matching
  plant/unit types. Existing species keep their current values.
- Germination given in months is not converted into an invented number of days.
  Flowering months are not treated as days to harvest. Yield, vase life, prices,
  soil requirements, and workflow schedules are not inferred.
- Existing entries are matched using product links, common names, and named
  cultivars/botanical names. Their exact import key is retained, allowing the
  current server importer to add a missing provider without overwriting growing
  data. Ambiguous matches require review; existing exact duplicates are left
  untouched.
- The converter detects duplicate records within the input because the server's
  importer only checks species that existed before each import request.

The dated `impecta-facts` file preserves the factual input used for this import,
including the two manually verified listing-only entries. Use that file instead
of the temporary facts path to reproduce the import without fetching pages.
