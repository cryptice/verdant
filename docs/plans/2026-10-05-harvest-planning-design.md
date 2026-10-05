# Harvest targets and backward production planning

Status: first implementation added on 2026-10-05. Targets count sellable units, and Verdant suggests an editable species mix. The implemented scope is described below; the rest of this document remains the longer-term design.

## Implemented increment

Web and Android now support dated species/group targets, organization-specific production profiles, previewing editable output allocations, and saving linked lifecycle tasks. Profiles import timed main workflow steps when possible, otherwise use known days-to-harvest for the starting step. Purchase lead time and date-specific yield require configuration; seasonal yield is never silently reused. Existing weekly targets remain available.

The first mix rule allocates equal output shares among compatible, fully configured species whose steps do not fall in the past. Fixed quantities, including zero exclusions, are preserved. Input quantities round up separately: `plants = ceil(output / outputPerPlant)`; `starting = ceil(plants × 100 / establishmentPercent)`. Profiles and steps are snapshotted when the plan is saved.

`V45__harvest_plans.sql` adds profiles, plans, and task links without rewriting existing targets. Plan creation and task insertion use one transaction and an organization-scoped idempotency key. A preview fingerprint detects changed assumptions before saving. Partial completion uses an expected remaining quantity to prevent retry double-counting. Cancellation retains completed/partial progress and marks pending tasks cancelled. Saved plans are immutable; cancel and create a replacement to change dates or quantities. Deleting a season detaches its plan relation and retains the snapshot.

Task completion in this increment records **planning progress only**. Inventory reservations, existing-batch allocation, automatic replanning, and links to purchase/plant/harvest events remain future work. In particular, `FLOWER` is supported as an explicit planning output unit, but actual flower harvest/sales integration below is still unimplemented. Do not treat completed plan tasks as harvested stock or sellable inventory. The separate production-profile editor currently configures a backward schedule initialized from existing workflows; changes do not rewrite those workflows.

See [the grower guide](../guide.md#harvest-plans) for the flow and [the API notes](../harvest-planning.md) for contracts and validation.

## Outcome

A grower enters a target such as “300 large pom pom dahlias ready for harvest on 15 August,” selecting either a species or a grower-defined species group. Verdant works backward through the relevant lifecycle schedules and automatically creates dated, quantity-bearing tasks from purchasing propagation material through harvesting.

Dates and quantities are planning estimates with visible assumptions. The system should expose an infeasible target or missing information rather than fill the gaps with arbitrary agronomic defaults.

## Confirmed requirements

1. **Count sellable units.** A target is an output commitment, not a count of plants. Some species are sold as stems, others as individual flowers. Keep target output units separate from the plants and propagation material needed to produce them.
2. **Verdant suggests the species mix.** A group target opens with a proposed allocation; the grower can adjust or exclude varieties before saving. Manual allocation must not be a prerequisite for obtaining a plan.
3. **Use the requested harvest date.** The original request specifies a particular date, so the initial interface defaults to that date. A future date-window option can use the same model, with a single date represented as an inclusive window whose start and end match.

## Sellable-unit rules

- Configure a default sellable unit for each species, initially supporting at least `STEM` and `FLOWER`. The target records the selected unit explicitly and preserves it when defaults later change.
- The target’s quantity, suggested species contributions, forecast, and harvest completion all use this output unit.
- Yield means expected **sellable units per productive plant in the target window**, including the intended quality threshold. Plants, seeds, plugs, bulbs, and tubers remain distinct input/stage units.
- A species group supplies acceptable species, but does not make incompatible output units interchangeable. Suggest the common unit when the group has one. If its members have different defaults, require an explicit target unit and show which species can contribute; never silently add stems and individual flowers.
- Do not automatically assume that one stem equals one sellable flower. Any supported conversion must be explicit, species-specific, and copied into the plan.
- The existing sales `UnitKind` has `STEM` but no `FLOWER`. Add flower units consistently to planning, harvest recording, and sales contracts/UI rather than introducing a planning-only label that cannot be recorded or sold.
- Preserve existing stem-based data as stems. Do not reinterpret earlier production targets, harvests, or sales when adding flower support.

## Proposed default mix

Start with a transparent rule, not a hidden optimization score:

1. Resolve the group’s eligible species and compatible output units.
2. Check the effective lifecycle, yield assumptions, and whether a new batch or an explicitly allocated existing batch can meet the target date. Show excluded or incomplete candidates and the reason; do not invent missing timings or yields.
3. Suggest an equal share of **sellable output** across the feasible species. For a 300-flower target with three feasible species, suggest 100 flowers from each, then calculate each species’ required plants and starting material separately.
4. Allocate integer remainders deterministically using species ID order, so refreshing a preview does not reshuffle the plan. The contributions must total the requested quantity exactly, before separately disclosed production buffers.
5. Use suitable unreserved stock to reduce purchases within each suggested allocation once reservation support is available. Show shortages and procurement deadlines. Stock availability must not silently distort the intended variety mix.
6. Let the grower edit quantities or percentages, pin particular contributions, exclude a species, and redistribute the remainder across unpinned feasible species. Preserve pinned contributions on regeneration. Reject pinned totals above the target; if nothing can receive a remaining quantity, show it as unallocated demand.

Equal output share is a proposed first-version default, not an assumption that all species have equal yield or growing time. A later “prefer existing stock” strategy can make a different tradeoff explicit. A target with no feasible mix remains a draft with the unmet quantity visible and no misleading ready-to-harvest promise.

## Existing foundations and gaps

- `ProductionTargetService` currently models one species, stems per week, and a date range. Its forecast uses whole weeks, a species yield assumption, germination rate, and minimum days to harvest. It neither supports one-off group demand nor creates a complete lifecycle plan. A same-day range currently yields zero demand, so the existing forecast must not be reused for dated quantities.
- `WorkflowService` already supports templates, species-specific steps, per-plant copies, timing offsets, optional steps, and side branches. This is the foundation for configurable lifecycle schedules; avoid a second competing workflow editor.
- `SuccessionScheduleService.generateTasks` creates sowing tasks only. It has no production-target linkage and repeated calls create additional tasks. New planning must use idempotent generation rather than copying this behavior.
- `ScheduledTask` supports quantities, earliest dates, deadlines, and species groups, but has no production-plan step linkage or cancellation state. Generic `TODO` tasks cannot currently reference species or groups.
- Seed inventory distinguishes seeds, plugs, bulbs, tubers, and plants. Purchase planning should preserve those units instead of forcing every lifecycle to start with seeds.
- Plant actions already record workflow progress. Connect planning to these actual events rather than asking the grower to mark the same work complete in two places.

## Grower experience

1. Enter the output quantity, species/group, harvest date or window, and season.
2. View Verdant’s suggested sellable-unit allocations, eligible species, existing stock, required new production, and missing assumptions; adjust the mix if desired.
3. Inspect a backward timeline for each planned batch. Override a duration, quantity, propagation method, or date for this plan without changing every future plan.
4. Save the plan to create its tasks automatically in the existing task list and calendar. A purchase task is a reminder, not an automatic supplier order.
5. Record actual purchases/receipts, sowings, plant movements, losses, and harvests through existing activities. Show their effects on the target's forecast and remaining demand.

Each generated task shows its target, species, batch, quantity/unit, date window, and prerequisite. Example task labels should use actual calculated quantities: “Order [N] tubers,” “Sow [N] seeds,” “Pot up [N] plants,” and “Harvest [N] stems” or “Harvest [N] flowers.” Use one task per batch/step, not hundreds of per-plant reminders.

## Lifecycle configuration

Use the existing template → species → individual production hierarchy, extended with a versioned plan snapshot:

- A reusable template supplies the default ordered lifecycle.
- Species settings override timings, propagation method, default sellable unit, losses, and yield assumptions.
- Organization-specific settings are needed for shared catalog species; one grower's changes must not mutate another grower's schedule. Existing workflow mutation checks currently require species ownership.
- A plan copies the effective settings and records their source. Changes to defaults affect new plans; updating an active plan is an explicit replan operation with a visible diff.

Steps distinguish an actionable task from a waiting interval or readiness milestone. Purchase lead time, receipt, sowing/start, potting up, planting out, optional care steps, harvest readiness, and harvesting can be represented without assuming every species needs every step.

For the first version, use an ordered main lifecycle with explicit inclusion/exclusion of optional steps. Do not automatically treat side branches as mandatory sequential delays or additional yield.

## Backward calculation

Anchor the plan to the requested harvest window. Calculate the preceding required milestones using the configured intervals. A missing interval blocks that portion of the schedule; a date in the past is shown as infeasible/overdue, not silently moved to today.

Quantities are calculated for each allocated species and batch:

- Determine expected harvestable output per productive plant **in the requested window**. The existing unqualified `expectedStemsPerPlant` is not enough to promise date-specific output.
- Divide allocated demand by that yield and round up to productive plants.
- Work backward through configured establishment/survival/germination rates, rounding up at each stage. Validate rates and yields, and make any deliberate safety buffer visible rather than applying it twice.
- Subtract suitable, unreserved stock from the required starting material. Purchase only the remaining shortage, respecting the selected stock unit and configured supplier lead time.
- Distinguish ordered material from received material. Do not increase usable stock when a purchase reminder is merely marked complete.

Existing growing batches may contribute only when they have an explicit expected contribution to the harvest window and that output has not been allocated elsewhere. Seed stock and future harvest output need separate reservations. Do not count the same batch's entire seasonal yield against several date-specific targets.

For group targets, calculate each species using its own lifecycle and yield assumptions, then aggregate the forecast. Never average all species into a single sowing date. Suggested allocations must be transparent and editable; missing data should prevent a false claim that the mix is achievable.

## Execution and replanning

Keep three separate values: the target commitment, the saved plan, and the current forecast from actual progress. Finishing a task late must not silently change the customer's required date.

- Bind actual batches and activity records to plan steps, including partial completion and quantities.
- Use actual progress to update remaining work and flag a likely shortfall or late harvest.
- Preview changes before replacing an active schedule; preserve completed work and explicitly pinned dates.
- Regeneration updates the same pending steps, cancels superseded work, and never duplicates tasks.
- Cancellation releases unused reservations and cancels future tasks while retaining production and harvest history.
- Changing a group's membership does not rewrite active plans. Keep the saved eligible-species set and allocation until replanning.

## Implementation shape

Extend production targets with a distinct dated-quantity mode while retaining existing weekly targets and their data. Use additive migrations and update backend, web, and Android contracts together.

Introduce organization-owned plan records, species/batch allocations, snapshotted steps, inventory reservations, and actual-event links. Tasks reference stable plan-step IDs with a database uniqueness constraint. Save/replan operations must be transactional, version-checked, and safe to retry; overlapping stock reservations must be checked under concurrency.

Put calculations in a deterministic planning service that accepts the target, lifecycle snapshots, stock/allocations, and an explicit current date. Keep date arithmetic and quantity calculations out of the clients. Preview and save must use the same calculation rules, with stock availability revalidated when saving.

Roll out in usable increments:

1. Dated species/group targets in sellable units, configurable lifecycle snapshots, a default suggested mix with editable allocations, backward-plan preview, and idempotent generation of purchase-to-harvest tasks. Include consistent flower/stem units across harvest and sales. Add typed procurement tasks and client routing instead of disguising them as sowing events.
2. Stock reservations and linked actual batches/events, partial completion, remaining-demand forecasts, and safe replanning. These are required before presenting existing stock/output as committed to a target.
3. More advanced mix strategies, allocation across competing targets, succession recommendations, and optional space/labor constraints.

Weather-derived timing and the unfinished weather experiment are outside this scope. Initial schedules use explicit grower-configured assumptions; optional seasonal constraints can be entered directly.

## Acceptance scenarios

- A 300-flower target and a 300-stem target retain their respective units through planning, harvest completion, and sales.
- Three feasible species receive a default 100/100/100 sellable-unit allocation for a target of 300, even when their required plant counts differ.
- Non-divisible target quantities are allocated deterministically with no fractional sellable items and no missing/excess demand.
- Mixed-unit groups cannot silently count incompatible outputs toward the same target.
- Changing one species allocation preserves pinned quantities and redistributes only the remaining demand.

- A same-day target retains the full requested quantity instead of becoming zero weeks of demand.
- A group with two differently timed species produces separate schedules and a combined output forecast.
- Missing timing/yield information is explained without an invented promise of readiness.
- Changing germination, survival, lead time, or harvest date changes the relevant quantities/dates predictably.
- Seed-, plug-, and tuber-started plans retain the correct units and selected lifecycle steps.
- Repeated saves or concurrent generation cannot create duplicate tasks or over-reserve stock.
- Completing half a sowing task creates/links only the actual production and leaves the correct remaining work.
- Actual losses or delays change the forecast and expose a shortfall without changing the original target.
- Replanning preserves completed work; cancellation releases only unused reservations.
- Group/default-schedule edits do not silently alter existing plans.
- Users cannot read, plan with, reserve, or mutate another organization's stock, targets, workflows, or tasks.
