---
name: Titan Amber Telematics
colors:
  surface: '#121316'
  surface-dim: '#121316'
  surface-bright: '#38393c'
  surface-container-lowest: '#0d0e11'
  surface-container-low: '#1b1b1f'
  surface-container: '#1f1f23'
  surface-container-high: '#292a2d'
  surface-container-highest: '#343538'
  on-surface: '#e3e2e6'
  on-surface-variant: '#d8c3ad'
  inverse-surface: '#e3e2e6'
  inverse-on-surface: '#2f3034'
  outline: '#a08e7a'
  outline-variant: '#534434'
  surface-tint: '#ffb95f'
  primary: '#ffc174'
  on-primary: '#472a00'
  primary-container: '#f59e0b'
  on-primary-container: '#613b00'
  inverse-primary: '#855300'
  secondary: '#ffb690'
  on-secondary: '#552100'
  secondary-container: '#ec6a06'
  on-secondary-container: '#4a1c00'
  tertiary: '#56e5a9'
  on-tertiary: '#003824'
  tertiary-container: '#30c88f'
  on-tertiary-container: '#004e34'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffddb8'
  primary-fixed-dim: '#ffb95f'
  on-primary-fixed: '#2a1700'
  on-primary-fixed-variant: '#653e00'
  secondary-fixed: '#ffdbca'
  secondary-fixed-dim: '#ffb690'
  on-secondary-fixed: '#341100'
  on-secondary-fixed-variant: '#783200'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#121316'
  on-background: '#e3e2e6'
  surface-variant: '#343538'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 56px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Inter
    fontSize: 44px
    fontWeight: '700'
    lineHeight: 52px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: 0em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: 0em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: 0em
  title-lg:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0em
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: 0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.01em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.03em
  label-sm:
    fontFamily: Inter
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system delivers an industrial, high-precision visual environment tailored for rugged automotive diagnostics, fleet telematics, and heavy-duty mobility cockpits. Designed around a dark titanium aesthetic, the experience prioritizes instantaneous glanceability under direct sunlight, low-light cabin environments, and high-vibration operator conditions.

### Design Movements & Tone
- **Utilitarian Material 3:** Built strictly to comply with modern Material You guidelines, translated into Jetpack Compose design primitives.
- **Instrument-Grade Functionalism:** Eliminates purely decorative fluff; elements communicate system state, mechanical health, operational costs, and trajectory data with zero visual ambiguity.
- **High-Contrast Dark Canvas:** Matte charcoal and titanium-infused surfaces form an unyielding technical baseline, allowing saturated amber alerts and performance metrics to pop without creating optical glare.

## Colors

The palette leverages a technical dark baseline with high-chroma functional indicators, engineered to meet rigorous accessibility and contrast standards for rapid field scanning.

### Core Roles
- **Primary (`#F59E0B` - Energetic Amber):** The principal operational focal point. Drives primary actions, system status highlights, active track states, and high-priority telematics gauges.
- **Secondary (`#F97316` - Racing Orange):** Used for elevated warnings, real-time acceleration vectors, dynamic alerts, and time-critical operational updates.
- **Tertiary (`#10B981` - Mint/Lime Metric):** Dedicated strictly to positive performance deltas, profit indicators, range efficiency gains, and normal subsystem operation.
- **Neutral Base (`#121316` - Deep Charcoal / `#1A1C23` - Titanium Dark):** Layered background and container foundations engineered to prevent eye strain and preserve night-adapted vision.

### Functional Mapping & States
- **Surface Dim:** `#0E0F12` (Base ground layer, maps, viewport containers)
- **Surface Default:** `#121316` (Screen canvas)
- **Surface Container (Tier 1):** `#1A1C23` (Standard cards and table modules)
- **Surface Container High (Tier 2):** `#232630` (Modals, popovers, active inputs)
- **On-Surface:** `#F3F4F6` (High-contrast neutral white text and primary vector iconography)
- **On-Surface Variant:** `#9CA3AF` (Secondary metadata, inactive units, mechanical timestamps)
- **Outline / Border:** `#2D313E` (Subtle boundary delineation under ambient lighting)

## Typography

The typographic hierarchy relies entirely on `Inter` across all functional roles. As a workhorse geometric neo-grotesque, `Inter` features tall x-heights, distinct open counters, and disambiguated forms critical for telemetry readings where `0`, `O`, `1`, `I`, and `l` must be deciphered at a split-second glance.

### Numerical Data & Telematics Output
- Use tabular figures (`tnum`) for all instrument readout displays, speedometer counters, cost tallies, and fuel percentages to avoid jitter during real-time value updates.
- Micro-labels (`label-sm`) default to uppercase transformation with widened tracking (`letterSpacing: 0.05em`) to ensure legibility when identifying sensor channels and units of measurement.

## Layout & Spacing

The layout is built around a rigorous fluid grid optimized for automotive heads-up displays, rugged in-cab tablets, and mobile field hand-helds.

### Grid System & Responsive Adaptations
- **Mobile (Compact: < 600dp):** 4 columns, `margin: 1rem` (16dp), `gutter: 1rem` (16dp). Dense vertical stacks for immediate single-thumb accessibility. Touch targets never drop below 48dp height.
- **Tablet / In-Dash Display (Medium: 600dp - 839dp):** 8 columns, `margin-tablet: 1.5rem` (24dp), `gutter: 1rem` (16dp). Two-column modular cards; persistent navigation rail pinned to the driver's side.
- **Desktop / Dispatch Console (Expanded: >= 840dp):** 12 columns, `margin-desktop: 2rem` (32dp), `gutter-desktop: 1.5rem` (24dp). High-density operational view with multi-pane telemetry grids and persistent telemetry split-views.

### Rhythm Principles
Interior component spacing follows an absolute 4dp base scale (`space-xs` = 4dp, `space-sm` = 8dp, `space-md` = 16dp, `space-lg` = 24dp, `space-xl` = 32dp). Group related telemetry indicators tightly using `space-xs` and `space-sm`, reserving `space-lg` and above for isolating discrete vehicle sub-assemblies.

## Elevation & Depth

Rather than conventional drop shadows, which dilute contrast in dark interfaces, this design system achieves structural depth through Material 3 surface container layering combined with crisp structural bounding strokes.

### Tonal Surface Tiers
- **Level 0 (Canvas Base):** `#121316`. The foundational void for map canvasses and root page containers.
- **Level 1 (Card & Module Foundation):** `#1A1C23`. Primary surface for telematics cards, instrument docks, and persistent modules. Bound by a 1dp low-contrast outline of `#2D313E`.
- **Level 2 (Active Control / Floating Shelf):** `#232630`. Modals, floating action panels, and expanded trays. Features a 1dp border tinted with `rgba(245, 158, 11, 0.25)` to designate operational priority.
- **Level 3 (Overlays & Dialogs):** `#2B2F3D`. Critical emergency stops and prompt windows, accompanied by an ambient, non-directional dark vignette (`rgba(0, 0, 0, 0.65)`).

### Active Element Illumination
Interactive primary elements incorporate a soft, ambient glow rather than a standard shadow: `box-shadow: 0px 0px 16px rgba(245, 158, 11, 0.35)`. This creates a tactile, illuminated switchboard feel suitable for hardware-adjacent telematics software.

## Shapes

The design system enforces a balanced `roundedness: 2` geometry. This avoids both hyper-futuristic pill elements and brittle sharp corners, balancing modern software ergonomics with rugged industrial enclosure aesthetics.

### Corner Radius Mapping
- **Default Base (`rounded`):** `0.5rem` (8dp). Applied to standard buttons, text input fields, status chips, and table row groupings.
- **Large Container (`rounded-lg`):** `1rem` (16dp). Applied to high-level metric modules, vehicle status cards, and dashboard tiles.
- **Extra Large / Modal (`rounded-xl`):** `1.5rem` (24dp). Applied to bottom navigation sheets, full diagnostics drawers, and alert modals.
- **Micro Radius:** `0.25rem` (4dp). Dedicated to micro-badges, segment tabs, and telemetry status pips.

## Components

### Buttons
- **Primary Action:** Solid Amber (`#F59E0B`) fill with high-contrast `#121316` text/icons (`label-lg`, weight 600). Height: 48dp minimum for field-glove compatibility. Corners: 8dp (`rounded`). Focus/Hover states trigger a subtle racing orange glow (`rgba(249, 115, 22, 0.4)`).
- **Secondary / Outlined:** Transparent surface with a 1.5dp `#2D313E` boundary. Text colored in `#F3F4F6`. Pressed state shifts surface fill to `#232630`.
- **Destructive / Emergency:** Solid `#DC2626` or `#F97316` surface, triggering immediate tactical prominence.

### Chips & Badges
- **Status Filter Chips:** Surface `#1A1C23` with 1dp border `#2D313E`. Height: 32dp. When active, background shifts to `rgba(245, 158, 11, 0.15)` with a solid `#F59E0B` border and typography.
- **Telemetry Readout Badges:** Compact 20dp height containers with 4dp corners. Background is a tinted wash (`rgba(16, 185, 129, 0.15)` for profit metrics, paired with `#10B981` text).

### Lists & Telematics Rows
- **Dense Data Rows:** Alternating micro-gutters with 8dp inner padding. Separated by hairline 1dp divider `#1F222B`. Left slot reserved for monospaced channel IDs or sensor icons; right slot reserved for tabular metric values and directional trend indicators.

### Checkboxes & Radio Controls
- **Unselected:** 20dp box/ring bounded by 2dp `#4B5563` against `#1A1C23`.
- **Selected:** Filled with `#F59E0B`, displaying an `#121316` checkmark or center pip. Target touch area padded out to 48dp invisible hitboxes.

### Input Fields
- **Container:** Filled surface `#1A1C23` with an explicit bottom or perimeter border of `#2D313E` (1dp). Height: 52dp.
- **Active / Focused:** 2dp border in `#F59E0B`. Label animates to `label-sm` in matching amber. Supporting text or units (e.g., `km/h`, `L/100km`, `$ / mi`) pinned to trailing slot in `#9CA3AF`.

### Telematics Cards & Gauges
- **Container Structure:** 16dp rounded corners, `#1A1C23` surface, with a 1dp outline of `#2D313E`.
- **Card Header:** Houses sensor name (`title-md` in `#F3F4F6`) alongside real-time status LED pips (Green: `#10B981`, Amber: `#F59E0B`, Danger: `#EF4444`).
- **Telemetry Gauge Tile:** Prominent numerical figures rendered in tabular `display-md` or `headline-lg` with the unit positioned adjacent in `label-md` `#9CA3AF`.