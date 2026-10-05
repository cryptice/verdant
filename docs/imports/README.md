# Flower catalogue imports

## Impecta — 2026-10-05

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

## Florea Garden — 2026-10-05

Imported into `verdant-prod`: **303 new shared species/varieties** and **126
Florea provider links** added to existing species, taking the catalogue from
1,687 to 1,990 entries. All previous species fields/providers were preserved;
every new field and all imported provider links were verified against the input.
The 458 collected products produced 429 import entries, with two duplicate packs
collapsed and 27 mixed products, assorted bundles, or vegetables excluded.
The dated result JSON records counts, created IDs, input hashes, and request pacing.

The Florea collector covers flower seeds, bulbs/tubers, bare-root peonies, and
all flower subcategories linked in the Swedish catalogue navigation. The broad
bulb catalogue omits dahlias and some perennials, so those subcategories must
also be collected. Vegetable seeds encountered in the Swedish-grown collection,
assorted bulb/tuber bundles, seed kits, and mixed-species seed packs are excluded. Named colour
mixtures of one flower species are retained.

```sh
uv run scripts/collect_florea.py \
  --cache /private/tmp/verdant-florea-cache \
  --output /private/tmp/verdant-florea-facts.json

python3 scripts/prepare_florea.py \
  --facts docs/imports/florea-facts-2026-10-05.json \
  --existing /private/tmp/verdant-florea-existing.json \
  --adjustments docs/imports/florea-adjustments-2026-10-05.json \
  --output docs/imports/florea-species-2026-10-05.json \
  --report docs/imports/florea-review-2026-10-05.json

uv run --with beautifulsoup4==4.14.3 python -m unittest discover -s scripts -p 'test_*.py'
```

The same sequential collector and randomized 1–3 second pauses are used for
Florea. Raw factual input is retained in the dated `florea-facts` file. The
`florea-adjustments` file records reviewed classifications, exclusions, and exact
matches to existing catalogue names; the preparer validates that each reviewed
match identifies one shared species. Original source facts remain unchanged.

- Swedish names are used as the primary names because English names are not
  supplied. Organic and pack-size labels do not create separate cultivars.
- Calendar months are read from active sowing/flowering cells, not from the
  twelve month labels printed on every calendar. Sowing depth is converted from
  centimetres to millimetres; a depth range is left blank because Verdant stores
  a single value. Bulb planting depth is not treated as seed sowing depth.
- Missing lifecycle classifications require review. The few reviewed
  defaults follow other Florea cultivars of the same species, or an explicit
  overwintering statement in the product description. Combined annual/biennial
  or annual/perennial labels use annual cultivation and retain the original
  lifecycle tag. Other missing growing details remain blank.
- Existing species receive a **Florea** provider link while retaining their
  growing data and previous providers. The import does not create lifecycle
  schedules or estimate days to harvest from calendar months.
