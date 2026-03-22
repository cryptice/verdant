# Verdant User Guide

Verdant is a planning and tracking tool for commercial flower production. It helps you manage every stage of your growing operation — from planning what to grow and when, through sowing and cultivation, to harvest tracking and season-over-season analysis.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Core Concepts](#core-concepts)
3. [Features](#features)
   - [Dashboard](#dashboard)
   - [Seasons](#seasons)
   - [Gardens & Beds](#gardens--beds)
   - [Species](#species)
   - [Sowing](#sowing)
   - [Plants & Plant Events](#plants--plant-events)
   - [Tasks](#tasks)
   - [Seed Inventory](#seed-inventory)
   - [Succession Planting](#succession-planting)
   - [Production Targets & Forecasting](#production-targets--forecasting)
   - [Crop Calendar](#crop-calendar)
   - [Customers](#customers)
   - [Bouquet Recipes](#bouquet-recipes)
   - [Variety Trials](#variety-trials)
   - [Pest & Disease Log](#pest--disease-log)
   - [Analytics](#analytics)
   - [Account](#account)
4. [Tutorial: Your First Season](#tutorial-your-first-season)
5. [Available Platforms](#available-platforms)

---

## Getting Started

1. Go to [verdantplanner.com](https://verdantplanner.com) and sign in with your Google account.
2. Create your first **season** (e.g., "2026").
3. Create a **garden** and add **beds** to match your physical layout.
4. Add the **species** you plan to grow (or search the built-in species database).
5. Start **sowing** and tracking your plants.

---

## Core Concepts

**Season** — A growing year or period. Everything in Verdant is organized by season, enabling you to compare performance year over year. Typically one season per year (e.g., "2026"), but you can create multiple (e.g., "Spring 2026", "Fall 2026").

**Garden** — A physical location where you grow. A garden contains one or more beds.

**Bed** — A growing area within a garden. Beds can have dimensions (length and width) for yield-per-area calculations. Plants are assigned to beds, or to a portable tray if not yet planted out.

**Species** — A type of plant, with common name, Swedish name, variant/cultivar name, and scientific name. Species carry growing information (days to sprout, days to harvest, bloom months, sowing months) and production data (expected stems per plant, expected vase life, cost per seed).

**Plant** — An individual plant or batch of plants of a given species. A plant moves through lifecycle stages: Seeded, Potted Up, Planted Out, Growing, Harvested, Recovered, Removed, or Dormant (for bulbs/tubers over winter).

**Plant Event** — A timestamped record of something that happened to a plant: sowing, potting up, planting out, blooming, harvesting, etc. Harvest events can include stem count, stem length, quality grade, and destination customer.

---

## Features

### Dashboard

Your home screen. Shows:
- A greeting with your name
- Summary counts (gardens, plants)
- Garden cards — click to view garden details
- Plants in trays — species currently growing in portable trays
- Harvest stats — total weight, quantity, and harvest count per species

### Seasons

Manage your growing seasons. Each season has:
- **Name and year** (e.g., "2026")
- **Frost dates** — last spring frost and first fall frost, used for calculating safe sowing windows
- **Start and end dates** (optional)
- **Active flag** — one season can be marked as the active/current season

Most features can be filtered by season to show only data relevant to the current growing period.

### Gardens & Beds

**Gardens** represent your physical growing locations. Each garden has:
- A name and optional description
- An emoji icon
- Optional GPS coordinates and address

**Beds** are growing areas within a garden:
- A name and optional description
- Optional dimensions (length and width in meters) for yield calculations
- A list of plants currently growing in the bed
- **Bed history** — view what was grown in each bed across previous seasons, useful for crop rotation planning

From a garden page, you can create new beds and navigate to individual beds. Each bed shows its current plants grouped by species, with the ability to add new plants or sow seeds directly.

**Danger zone** at the bottom of garden and bed pages allows deletion (with confirmation).

### Species

The species database contains all plant varieties you work with. Each species record includes:

- **Names**: Common name, Swedish name, variant/cultivar name (in both languages), and scientific name
- **Growing info**: Days to sprout, days to harvest, sowing depth, height, germination rate, bloom months, sowing months, growing positions (sun requirements), and soil types
- **Production data**: Expected stems per plant, expected vase life (days), cost per seed
- **Plant type**: Annual, Perennial, Bulb, or Tuber
- **Photos**: Multiple photos can be attached
- **Tags and groups**: Organize species into groups and apply tags for filtering
- **Providers**: Link to seed/bulb vendors with product URLs and per-unit cost

The species list shows Swedish names and variants, sorted alphabetically, with a search that queries across all name fields and scientific name.

### Sowing

The sow screen lets you plant seeds in bulk:

1. **Select a species** (autocomplete search)
2. **Choose a destination**: a specific bed or a portable tray
3. **Enter seed count**
4. **Optionally select a seed batch** — if you have seeds in inventory, selecting a batch auto-decrements the quantity after sowing
5. **Add notes** (optional)

Sowing creates a plant record with "Seeded" status and records a SEEDED event with the date.

### Plants & Plant Events

Each plant has a **timeline** of events. Click a plant to see its full history and add new events.

**Lifecycle stages and event types:**

| Event | Description | Status Change |
|-------|-------------|---------------|
| Seeded | Seeds planted | Seeded |
| Potted Up | Transplanted to pots | Potted Up |
| Planted Out | Moved to final bed | Planted Out / Growing |
| Budding | Flower buds forming | (no change) |
| First Bloom | First flowers open | (no change) |
| Peak Bloom | Maximum flowering | (no change) |
| Last Bloom | Final flowers of the season | (no change) |
| Pinched | Growing tip removed to encourage branching | (no change) |
| Disbudded | Side buds removed for larger central flower | (no change) |
| Harvested | Stems or flowers cut | Harvested |
| Recovered | Plant regrows after harvest | Recovered |
| Lifted | Bulb/tuber dug up for storage | Dormant |
| Divided | Bulb/tuber divided into multiple | (no change) |
| Stored | Placed in winter storage | Dormant |
| Removed | Plant discarded or died | Removed |
| Note | Free-text observation | (no change) |

**Harvest events** include flower-specific fields:
- **Stem count** — number of stems harvested
- **Stem length (cm)** — average length of cut stems
- **Quality grade** — A, B, or C
- **Destination** — which customer receives these stems

**Bloom events** (Budding, First Bloom, Peak Bloom, Last Bloom) are key dates for tracking when each variety blooms, which feeds into the crop calendar and year-over-year analytics.

### Tasks

Schedule garden activities with deadlines:

- **Activity type**: Sow, Pot Up, Plant Out, Harvest, Recover, or Discard
- **Species**: Which species the task is for
- **Deadline**: When it needs to be done
- **Target count**: How many plants to process
- **Progress tracking**: As you perform the task (via the Sow screen), the remaining count decreases automatically

Tasks can be filtered by season. Overdue tasks are highlighted in red, today's tasks in orange.

### Seed Inventory

Track your seed and bulb stock:

- **Species and quantity** — how many seeds/bulbs/plugs you have
- **Unit type** — Seed, Plug, Bulb, Tuber, or Plant
- **Cost per unit** (in ore) — for calculating total investment
- **Collection date** — when seeds were collected or purchased
- **Expiration date** — when seeds lose viability
- **Season** — which season the inventory belongs to

When you sow using a seed batch, the quantity is automatically decremented. The inventory table shows Swedish species names, quantity, cost, and expiration date.

### Succession Planting

Plan staggered sowings for continuous bloom:

- **Species**: What to sow
- **First sow date**: When to start
- **Interval (days)**: Time between sowings (e.g., 14 for biweekly)
- **Total rounds**: How many succession sowings
- **Seeds per round**: How many seeds each time
- **Bed** (optional): Where to plant

**Generate tasks**: Click the button to automatically create a scheduled task for each succession round. For example, a schedule with first sow April 1, interval 14 days, and 6 rounds generates tasks on April 1, April 15, April 29, May 13, May 27, and June 10.

### Production Targets & Forecasting

Set delivery targets and let Verdant calculate what you need to grow:

**Target**: "I need 200 stems of dahlias per week from July 1 to September 30."

**Forecast** (calculated automatically):
- **Total stems needed**: stems/week x number of weeks
- **Plants needed**: total stems / expected stems per plant
- **Seeds needed**: plants needed / germination rate
- **Suggested sow date**: delivery start date minus days to harvest
- **Warnings**: If species data is missing (e.g., no expected stems/plant set), defaults are used and a warning is shown

This is the core planning loop: set your sales target, and Verdant tells you how many seeds to sow and when.

### Crop Calendar

A visual timeline of your season showing lifecycle phases per species:

- **Horizontal axis**: 12 months (January through December)
- **Vertical axis**: One row per species you're growing
- **Colored bars**: Sow window (blue), Bloom window (pink), Harvest window (orange)
- **Season filter**: View different seasons

The calendar reads actual event data (sowing dates, bloom dates, harvest dates) and displays them as bars. If no events exist yet, it falls back to species bloom month data to show expected windows. This gives you a bird's-eye view of your entire season.

### Customers

Track where your flowers go:

- **Name**: Customer or venue name
- **Channel**: Florist, Farmers Market, CSA, Wedding, Wholesale, Direct, or Other
- **Contact info**: Address, phone, email
- **Notes**: Any additional info

Customers appear as destinations when recording harvest events, enabling you to track which varieties go to which customers.

### Bouquet Recipes

Save your signature arrangements:

- **Name**: Recipe name (e.g., "Summer Garden Mix")
- **Description**: Notes about the arrangement
- **Price** (in ore): What you charge
- **Items**: A list of species with stem counts and roles:
  - **Flower** — the main blooms
  - **Foliage** — greenery and leaves
  - **Filler** — small flowers to fill gaps
  - **Accent** — unique or contrasting elements

Recipes are displayed as cards showing the name, price, and total stem count. Click to view or edit the full composition.

### Variety Trials

Track how new varieties perform side by side:

- **Season and species**: Which variety in which year
- **Bed**: Where it was grown
- **Plant count**: How many were trialed
- **Stem yield**: Total stems harvested
- **Average stem length (cm)** and **vase life (days)**
- **Quality score**: Your subjective 1-10 rating
- **Customer reception**: Loved, Liked, Neutral, or Disliked
- **Verdict**: Keep, Expand, Reduce, Drop, or Undecided

Trials are filterable by season. Color-coded verdict badges make it easy to scan: green for Keep/Expand, yellow for Reduce, red for Drop, gray for Undecided.

Over multiple seasons, trials build a record of which varieties are worth growing and which to drop.

### Pest & Disease Log

Structured logging for pest and disease observations:

- **Date**: When observed
- **Category**: Pest, Disease, Deficiency, or Other
- **Name**: What was found (e.g., "Aphids", "Powdery mildew")
- **Severity**: Low, Moderate, High, or Critical
- **Bed and species**: Where and on what
- **Treatment**: What action was taken
- **Outcome**: Resolved, Ongoing, Crop Loss, or Monitoring
- **Notes and photo**: Additional documentation

Filterable by season. Over time, this builds a knowledge base of what problems occur, which beds are affected, and which treatments work.

### Analytics

Year-over-year performance dashboard with three views:

**Season Overview** — One card per season showing:
- Total stems harvested
- Total plants grown
- Number of species
- Top-performing species

**Species Comparison** — Select a species to see its performance across all seasons:
- Plant count, stems harvested, stems per plant
- Average stem length and vase life
- Visual bars for quick comparison

**Yield per Bed** — Bed-level productivity:
- Total stems per bed per season
- Stems per square meter (if bed dimensions are set)
- Helps identify which beds are most productive and plan rotation

### Account

View your profile and sign out. Delete your account and all associated data if needed.

---

## Tutorial: Your First Season

This walkthrough takes you through setting up Verdant for your first growing season.

### Step 1: Create a season

Go to **Seasons** (📅) and click **+ New season**.

- Name: `2026`
- Year: `2026`
- Last frost: Enter your local last spring frost date (e.g., May 15)
- First frost: Enter your local first fall frost date (e.g., October 1)

Click **Save**. Your season is now the active season.

### Step 2: Set up your garden

Go to **My world** (🌍) and click **+ New garden**.

- Name: `Main Garden`
- Pick an emoji (e.g., 🌸)

Click **Create Garden**. Now click into the garden and add beds:

- Click **+ New bed** and create beds matching your physical layout:
  - `Bed 1` (e.g., dahlia bed)
  - `Bed 2` (e.g., annual cuts)
  - `Bed 3` (e.g., foliage)

### Step 3: Add species

Go to **Species** (🌿) and click **+ New species** to add your varieties. For each:

- Common name: `Dahlia`
- Variant name: `Cafe au Lait`
- Swedish name: `Dahlia` / Variant: `Cafe au Lait`
- Plant type: `Tuber`
- Days to harvest: `90`
- Expected stems per plant: `8`
- Cost per seed: `2500` (25 kr per tuber, in ore)

Repeat for each variety you plan to grow.

### Step 4: Add seed/bulb inventory

Go to **Seeds** (🫘) and click **+ Add seeds**.

- Search for your species
- Quantity: `50`
- Unit type: `Tuber`
- Cost per unit: `2500`
- Expiration date: (if applicable)

### Step 5: Plan your successions

Go to **Successions** (🔄) and click **+ New schedule**.

For a succession-sown cut flower like zinnias:
- Species: `Zinnia`
- First sow: `April 1`
- Interval: `14` days
- Rounds: `6`
- Seeds per round: `50`

Click **Generate tasks** to create 6 sowing tasks automatically.

### Step 6: Set production targets

Go to **Targets** (🎯) and click **+ New target**.

- Species: `Dahlia — Cafe au Lait`
- Stems per week: `100`
- Start: `July 1`
- End: `September 30`

Click the target row to see the **forecast**: how many plants and tubers you need, and when to plant.

### Step 7: Start sowing

When it's time to sow, go to **Tasks** (📋). You'll see your upcoming tasks. Click **Perform** on a sowing task — this takes you to the sow screen with the species and count pre-filled.

Choose a bed (or tray), confirm the seed count, and click **Sow**. The plants are created and your seed inventory is decremented.

### Step 8: Track growth

As your plants grow, add events from the plant detail page:
- **Potted Up** when you transplant seedlings
- **Planted Out** when they go in the ground
- **Budding** when you see first buds
- **First Bloom** when flowers open
- **Pinched** if you pinch for branching

### Step 9: Record harvests

When you cut stems, add a **Harvested** event:
- Stem count: `12`
- Stem length: `60` cm
- Quality: `A`
- Destination: Select the customer (set up customers first in the Customers page)

### Step 10: Review the season

At the end of the season:

- **Variety Trials** (🔬): Score each variety and record your verdict (Keep, Expand, Drop)
- **Analytics** (📈): Compare your seasons, see which species performed best, check yield per bed
- **Crop Calendar** (📊): See the visual timeline of what happened when
- **Pest & Disease** (🐛): Review any issues that came up

Use these insights to plan an even better next season.

---

## Available Platforms

| Platform | URL / Install |
|----------|--------------|
| Web app | [verdantplanner.com](https://verdantplanner.com) |
| Admin panel | [verdantplanner.com/admin](https://verdantplanner.com/admin) |
| Android app | Available on request (APK) |

The web app and Android app share the same backend and data. Sign in with the same Google account on both to access your gardens, plants, and planning data.

Both the web app and Android app support **Swedish** and **English**. Swedish is the default language. Switch languages from the sidebar (web) or app settings (Android).
