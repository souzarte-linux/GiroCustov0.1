---
name: Nocturne Courier Logistics
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#bdc8d1'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#87929a'
  outline-variant: '#3e484f'
  surface-tint: '#7bd0ff'
  primary: '#8ed5ff'
  on-primary: '#00354a'
  primary-container: '#38bdf8'
  on-primary-container: '#004965'
  inverse-primary: '#00668a'
  secondary: '#bdc2ff'
  on-secondary: '#131e8c'
  secondary-container: '#2f3aa3'
  on-secondary-container: '#a8afff'
  tertiary: '#4ee6aa'
  on-tertiary: '#003825'
  tertiary-container: '#22c990'
  on-tertiary-container: '#004e35'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#c4e7ff'
  primary-fixed-dim: '#7bd0ff'
  on-primary-fixed: '#001e2c'
  on-primary-fixed-variant: '#004c69'
  secondary-fixed: '#e0e0ff'
  secondary-fixed-dim: '#bdc2ff'
  on-secondary-fixed: '#000767'
  on-secondary-fixed-variant: '#2f3aa3'
  tertiary-fixed: '#68fcbf'
  tertiary-fixed-dim: '#45dfa4'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#005137'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  display-lg:
    fontFamily: Manrope
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.03em
  display-md:
    fontFamily: Manrope
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Manrope
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Manrope
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Manrope
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Manrope
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  title-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
  currency-hero:
    fontFamily: Manrope
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 44px
    letterSpacing: -0.03em
  currency-card:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 30px
    letterSpacing: -0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  margin-tablet: 1.5rem
  space-2xs: 0.25rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
---

## Brand & Style

This design system is engineered for multi-app gig economy drivers and delivery couriers operating across Latin American platforms (Shopee, Mercado Livre, iFood, Uber, 99). The visual character merges Material Design 3 (M3) Jetpack Compose ergonomics with an ultra-refined, high-contrast slate dark theme tuned for in-cabin phone mounts and high-speed glanceability during evening driving.

The design movement combines **Corporate Modern** with **High-Contrast Data Precision**. Surfaces are deeply muted to mitigate eye fatigue and prevent windscreen glare, while financial totals, active shift timers, and platform metrics demand instant legibility. The interface establishes absolute financial authority and efficiency: clean slate backgrounds, structured metric cards, tactile elevation surfaces, and disciplined badge systems that represent each logistics platform without muddying the overarching dark slate hierarchy.

## Colors

The palette leverages a root foundation of `#0F172A` (deep slate navy), utilizing calibrated M3 tonal surfaces to establish depth without true-black harshness:

- **Surface Base (`#0F172A`)**: Root canvas for in-vehicle OLED displays.
- **Surface Container Lowest (`#090D16`)**: Depressed inputs, recessed metric graphs.
- **Surface Container Low (`#151F36`)**: Main structural cards and background tiers.
- **Surface Container (`#1E293B`)**: Standard interactive cards, app bars, and bottom sheets.
- **Surface Container High (`#27354E`)**: Active states, hovered tiles, elevated chips.
- **Surface Container Highest (`#334155`)**: Borders, dividing rules, switch tracks.

### System Accents
- **Primary (`#38BDF8`)**: Electric sky cyan for overarching app actions, active tabs, and primary Compose interactions.
- **Secondary (`#818CF8`)**: Periwinkle indigo for secondary analytical aggregations and historical stats.
- **Tertiary (`#34D399`)**: Emerald green for net profits, positive run variances, and tips.

### Platform Logistics Signifiers
Platform identities are treated as structured metadata tokens, constrained to high-contrast badges, dot indicators, and left-rail accent strokes to preserve the dark ecosystem:
- **Shopee**: `#F97316` (Vibrant Delivery Orange)
- **Mercado Livre**: `#FFE600` (Core Yellow) with `#2563EB` (Naval Accent)
- **iFood**: `#EF4444` (Brisk Crimson)
- **Uber**: `#10B981` (Electric Emerald) on Dark
- **99**: `#F59E0B` (Amber Taxi Gold)

## Typography

The typography pairs **Manrope** for authoritative financial figures, headers, and dashboard summaries with **Inter** for clean, legible body text and UI labels.

- **Financial Hierarchy**: Primary totals utilize `display-md` or `currency-hero` with tabular numbers enabled (`tnum`) to eliminate layout jitter during live-sync updates. Currency symbols (`R$`, `$`, `€`) are rendered one step smaller than integers to balance visual weight.
- **Glanceability Rule**: Driver-critical numbers (Hourly Rate, Net Profit, Distance) must render with `Manrope` at a minimum weight of 600.
- **Micro-labels**: Platform badges, status chips, and timestamp metadata leverage `label-sm` in all-caps or title case with increased tracking (`0.04em`) to ensure instant parsing in low ambient light.

## Layout & Spacing

The layout is built upon an 8dp grid (with a 4dp micro-step), tailored for single-handed portrait mobile interaction and vehicle dashboard mounts.

- **Mobile Canvas**: 4-column layout on standard devices (&lt;600dp), operating with a `16dp` (`1rem`) outer margin and `16dp` gutter. This ensures maximum horizontal surface area for complex financial comparisons.
- **Tablet / Landscape Automotive Screens**: 8-column layout (&gt;600dp) with `24dp` outer margins, allowing a two-pane layout (left: real-time platform earnings feed; right: consolidated day breakdown and metrics).
- **Driver Safe Area**: The bottom `80dp` of the screen is reserved for persistent stats or anchored touch targets, keeping primary controls within the natural thumb zone.
- **Touch Targets**: All interactive elements (filters, chips, collapse triggers) observe a minimum hit box of `48dp x 48dp`, regardless of visual bounding boxes.

## Elevation & Depth

Visual hierarchy uses Material 3 tonal layering supplemented by subtle, tinted ambient shadows:

1. **Elevation Level 0 (Base)**: `Surface Base` (`#0F172A`). No shadow. Holds layout wrappers and sticky navigation headers.
2. **Elevation Level 1 (Default Containers)**: `Surface Container Low` (`#151F36`) with a 1dp stroke of `#334155` (30% opacity). Soft ambient shadow: `0px 2px 8px rgba(0, 0, 0, 0.35)`. Used for passive summary lists.
3. **Elevation Level 2 (Interactive & Collapsible Cards)**: `Surface Container` (`#1E293B`) with ambient drop: `0px 6px 16px rgba(0, 0, 0, 0.45)`. Outline is lightened to 40% opacity.
4. **Elevation Level 3 (Floating Headers & Modals)**: `Surface Container High` (`#27354E`) with depth shadow: `0px 12px 32px rgba(0, 0, 0, 0.6)`.
5. **Platform Badge Elevation**: Badges sit 1 level above their parent container, outlined by a 1dp inner border rather than external dropshadows to keep the interface crisp.

## Shapes

The design uses level `2` roundedness to balance modern Material 3 geometry with dense data structures:

- **Base Cards & Containers**: `16dp` (`rounded-lg`) border radius creates comfortable, modern card groupings for individual trips and platform summaries.
- **Collapsible Master Containers**: `24dp` (`rounded-xl`) border radius encapsulates entire shifts or daily earnings overviews.
- **Input Fields & Search Bars**: `16dp` corner radius.
- **Filter Chips & Platform Badges**: `8dp` to `12dp` radius, preventing them from appearing as fully rounded stadium pills and keeping them distinct from action buttons.
- **Action Buttons (FAB / Shift Start)**: `16dp` to `20dp` squircle radius, aligning with Compose `RoundedCornerShape` standards.

## Components

### Buttons
- **Primary (Start Shift / Sync Accounts)**: Full-bleed or floating button with `#38BDF8` background, `#090D16` bold text, `16dp` radius, and 56dp height for rapid finger access.
- **Secondary (Export / Detail)**: Transparent fill with a 1dp stroke of `#334155`, active hover/touch tinting to `#1E293B`.

### Platform Badges & Chips
- **Logistics Indicators**: Compact indicators featuring a 6dp circular status dot tinted with the respective platform color (`#F97316` for Shopee, `#FFE600` for Mercado Livre, `#EF4444` for iFood, `#10B981` for Uber, `#F59E0B` for 99), placed next to the platform initials or monochrome glyph on a `Surface Container High` background.
- **Filter Chips**: 36dp height, `8dp` radius. Selected state uses a subtle 15% tint of the primary cyan with a 1dp cyan boundary.

### Collapsible Earning Cards
- **Header Slot**: Left-aligned platform dot indicator and title (`title-md`), right-aligned gross value (`currency-card`) and an animated chevron.
- **Body Slot**: Displays net per hour, completed drops, fuel deductibles, and platform fees in a two-column key-value format using `label-md` and `body-sm`.
- **Accent Indicator**: A 4dp vertical accent strip aligned to the card's left border, colored in the corresponding logistics brand tone.

### Lists & Activity Feeds
- **Trip Items**: 72dp single-row items with leading iconography reflecting delivery type (Package vs. Food vs. Passenger), central block with timestamp and platform label, trailing block with formatted earnings and payout status (Pending vs. Cleared). Separated by a 1dp divider (`#334155` at 40% opacity).

### Input Fields & Controls
- **Text Inputs**: Outlined Jetpack Compose style, 56dp height, `12dp` corner radius, base surface `#151F36`, border `#334155`. Label transitions from `body-md` inside the field to `label-sm` floating above the boundary on focus.
- **Switches & Radios**: Platform active/inactive toggles use `#38BDF8` active tracks with `#0F172A` thumbs, recessed when inactive to `#334155`.