# Shared species groups and production defaults

Verdant supplies editable defaults for harvest targets from a versioned catalogue
in `backend/src/main/resources/planning/defaults.json`. These are **planning
estimates**, not promises of flowering on a particular date. The initial working
climate is outdoor growing in central Sweden, with lighted indoor propagation
where required. Local frost, temperature, cultivar and plant maturity still matter.

## How defaults are selected

1. An organization's saved production profile wins and is never changed by this catalogue.
2. An existing species workflow is preserved as the source of its schedule.
3. Otherwise, match botanical genus, plant type and starting unit to a crop schedule.
4. Unmatched annuals and biennials receive a draft requiring review. Unknown perennial
   flowering ages and unclassified planting stock have no invented harvest lead time;
   the grower must supply it before saving a usable profile.

A small explicit Swedish-name mapping handles records without a botanical name.
No cultivar is assigned a dahlia flower form by guessing from its cultivar name.
Existing `daysToHarvestMin`, when present and valid, replaces the generic duration;
intermediate timings are scaled proportionally and remain estimates.

## Quantity and season assumptions

- Default output is **one sellable unit per established plant on the target date**,
  not a full season's yield. Most crops use stems; nasturtiums and the annual viola
  draft use individual flowers. Both are editable.
- Establishment defaults are 70% from seed, 90% from bulbs and 85% for other stock.
  These are conservative operational assumptions, not measured supplier claims.
  A recorded seed germination percentage caps establishment at 90% of that value.
- Supplier flowering months narrow the suggested harvest season. A conflicting
  supplier window requires review instead of silently allowing every month.
- Uncustomized defaults reject out-of-season harvests and unsuitable outdoor planting
  months. The planting check uses transplanting for propagated crops and sowing for
  direct-sown crops. It is a month-level check, not a local weather/frost forecast.
- Saving a customized profile explicitly opts into that organization's chosen timing,
  including protected cultivation. Existing plans retain their original snapshots.

## Initial schedules

The exact purchase and harvest offsets below are Verdant's provisional estimates.
The sources support cultivation methods and relative seasonal requirements; they
are not presented as cultivar-specific evidence for every offset. Purchase is
normally 21 days before propagation. Routine care reminders ask the grower to
check conditions rather than prescribing fixed fertilizer or watering quantities.

| Schedule | Starting material | Start before harvest | Review required |
| --- | --- | ---: | --- |
| Zinnia från frö | SEED | 84 days | No, subject to season checks |
| Rosenskära från frö | SEED | 98 days | No, subject to season checks |
| Solros från frö | SEED | 77 days | No, subject to season checks |
| Luktärt från frö | SEED | 112 days | No, subject to season checks |
| Lejongap från frö | SEED | 140 days | No, subject to season checks |
| Prärieklocka från frö | SEED | 210 days | Yes |
| Sommaraster från frö | SEED | 126 days | No, subject to season checks |
| Risp från frö | SEED | 126 days | No, subject to season checks |
| Praktvädd från frö | SEED | 112 days | No, subject to season checks |
| Eternell från frö | SEED | 112 days | No, subject to season checks |
| Celosia från frö | SEED | 112 days | No, subject to season checks |
| Amarant från frö | SEED | 105 days | No, subject to season checks |
| Klotamarant från frö | SEED | 112 days | No, subject to season checks |
| Sommarrudbeckia från frö | SEED | 140 days | No, subject to season checks |
| Slöjsilja från frö | SEED | 126 days | No, subject to season checks |
| Blomstermorot från frö | SEED | 112 days | No, subject to season checks |
| Blomsterkörvel från frö | SEED | 98 days | No, subject to season checks |
| Nigella från frö | SEED | 98 days | No, subject to season checks |
| Ringblomma från frö | SEED | 84 days | No, subject to season checks |
| Ettårig klint från frö | SEED | 91 days | No, subject to season checks |
| Ettårig vallmo från frö | SEED | 91 days | No, subject to season checks |
| Sömntuta från frö | SEED | 84 days | No, subject to season checks |
| Atlasblomma från frö | SEED | 91 days | No, subject to season checks |
| Sommarflox från frö | SEED | 112 days | No, subject to season checks |
| Lövkoja från frö | SEED | 119 days | No, subject to season checks |
| Krasse – ätbara blommor | SEED | 84 days | No, subject to season checks |
| Violer – enskilda blommor | SEED | 140 days | Yes |
| Tagetes från frö | SEED | 98 days | No, subject to season checks |
| Ettårig riddarsporre från frö | SEED | 126 days | No, subject to season checks |
| Ettårig brudslöja från frö | SEED | 91 days | No, subject to season checks |
| Blommande basilika från frö | SEED | 98 days | No, subject to season checks |
| Dahlia från frö | SEED | 140 days | No, subject to season checks |
| Dahlia från knöl | TUBER | 112 days | No, subject to season checks |
| Ranunkel från knöl | TUBER | 126 days | No, subject to season checks |
| Bukettanemon från knöl | TUBER | 112 days | Yes |
| Höstplanterade vårblommande lökar | BULB | 210 days | No, subject to season checks |
| Gladiolus från knöl/lök | BULB | 112 days | No, subject to season checks |
| Pion – etablering från rot/planta | PLANT | Grower must set | Yes |
| Tvåårig blomma – sådd och övervintring | SEED | 420 days | Yes |
| Ettårig blomma – sortanpassning krävs | SEED | 112 days | Yes |
| Flerårig växt från frö – etablering | SEED | Grower must set | Yes |
| Etablering från lök | BULB | Grower must set | Yes |
| Etablering från knöl | TUBER | Grower must set | Yes |
| Etablering från plugg | PLUG | Grower must set | Yes |
| Etablering från planta | PLANT | Grower must set | Yes |

## Group population and repeatability

`GET /api/admin/species/planning-defaults` previews shared species, proposed groups,
and their resolved schedules. `POST` at the same path adds the groups and missing
memberships transactionally, with a database lock to serialize repeated requests.
Both endpoints require the existing ADMIN role. Existing memberships, species
fields, providers and organization profiles are untouched; private species are excluded.

Groups are botanical genera, with Swedish names for common crops and Latin genus
names otherwise. Documented dahlia flower forms also get subgroups. The resolver
runs for future species automatically; rerun group population after later imports.
The planning group limit is 200 species, enough for the current largest genus group.

The profile editor in web and Android shows the default's climate, assumptions,
review requirement and source links. Review-required defaults cannot generate
harvest tasks until configured and saved for the organization. No tasks are created
by populating groups or by deploying the defaults.

## Production population (2026-10-06, Europe/Stockholm)

Applied to all 1,990 shared catalogue entries in `verdant-prod`: 417 groups and
2,090 memberships, including eight dahlia form subgroups. The 45 schedule templates
resolve to usable defaults for 940 entries and review-required drafts for 1,050;
544 drafts still need grower-supplied timing. Usable defaults remain subject to
target-date and planting-season validation.

The live before/after comparison preserved every species field other than the
added groups, as well as all existing memberships. Repeating population created
zero groups and added zero memberships. Counts, distributions, source hash and
deployment identifiers are recorded in [the production verification](production-2026-10-06.json).

## Sources (accessed 2026-10-05)

- [Johnny’s Flower Growing Guide](https://www.johnnyseeds.com/on/demandware.static/-/Library-Sites-JSSSharedLibrary/default/dw377bb4f5/assets/information/flower-growing-guide.pdf)
- [Johnny’s: growing zinnias](https://www.johnnyseeds.com/growers-library/flowers/zinnia/zinnia-key-growing-information.html)
- [Johnny’s: growing cosmos](https://www.johnnyseeds.com/growers-library/flowers/cosmos/cosmos-key-growing-information.html)
- [Johnny’s: growing lisianthus](https://www.johnnyseeds.com/growers-library/flowers/lisianthus/lisianthus-key-growing-information.html)
- [RHS: growing dahlias](https://www.rhs.org.uk/plants/dahlia/growing-guide)
- [RHS: planting bulbs](https://www.rhs.org.uk/plants/types/bulbs/planting)
- [Johnny’s: growing ranunculus](https://prod-na02.johnnyseeds.com/growers-library/flowers/ranunculus/ranunculus-key-growing-information.html)
- [RHS: growing perennials from seed](https://www.rhs.org.uk/membership/rhs-members-seed-scheme/germination-guide)
- [RHS: growing peonies](https://www.rhs.org.uk/plants/peony/herbaceous/growing-guide)
