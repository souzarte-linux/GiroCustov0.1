---
name: GiroCusto Electric Kinetic
colors:
  surface: '#0f141c'
  surface-dim: '#0f141c'
  surface-bright: '#343942'
  surface-container-lowest: '#090e16'
  surface-container-low: '#171c24'
  surface-container: '#1b2028'
  surface-container-high: '#252a33'
  surface-container-highest: '#30353e'
  on-surface: '#dee2ee'
  on-surface-variant: '#c0c6d6'
  inverse-surface: '#dee2ee'
  inverse-on-surface: '#2c3139'
  outline: '#8b91a0'
  outline-variant: '#414754'
  surface-tint: '#aac7ff'
  primary: '#aac7ff'
  on-primary: '#003064'
  primary-container: '#3e90ff'
  on-primary-container: '#002957'
  inverse-primary: '#005db8'
  secondary: '#a5e7ff'
  on-secondary: '#003543'
  secondary-container: '#00d2ff'
  on-secondary-container: '#00566a'
  tertiary: '#4edea3'
  on-tertiary: '#003824'
  tertiary-container: '#00a572'
  on-tertiary-container: '#00311f'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d6e3ff'
  primary-fixed-dim: '#aac7ff'
  on-primary-fixed: '#001b3e'
  on-primary-fixed-variant: '#00468d'
  secondary-fixed: '#b6ebff'
  secondary-fixed-dim: '#47d6ff'
  on-secondary-fixed: '#001f28'
  on-secondary-fixed-variant: '#004e60'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#0f141c'
  on-background: '#dee2ee'
  surface-variant: '#30353e'
typography:
  display-hero:
    fontFamily: Inter
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.03em
  display-hero-mobile:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.01em
  metric-numeral:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.02em
  label-prominent:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-compact:
    fontFamily: Inter
    fontSize: 11px
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
  margin-desktop: 2.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes a high-performance, utilitarian operating interface engineered for the intersection of fintech telemetry and extreme mobility conditions. Built specifically for on-the-move motorcycle couriers and delivery logistics, the aesthetic fuses the clean precision of modern fintech interfaces with the mission-critical legibility of automotive telemetry.

### Design Principles
- **Glare-Defiant Utility:** Visual weight, stroke thickness, and tonal values are calibrated to preserve critical financial and telemetry data under direct ambient sunlight and rapid glance conditions.
- **Dark-Mode Ergonomics:** A true deep-black foundation (`#0A0D12`) drastically conserves battery on OLED devices mounted on handlebars while eliminating nighttime blinding glare.
- **Kinetic Precision:** High-contrast accent bursts—electric blue, dynamic cyan, and profitability lime—provide instant cognition without visual noise. Data hierarchy acts like a speedometer: essential figures require microsecond comprehension.

## Colors

The palette is engineered around high luminance contrast against an ultra-dark chassis. It uses targeted functional roles: electric blues drive primary navigation, cyan signals active network states and metrics, and high-saturation emerald/lime validates financial yield and net profit.

### Functional Roles
- **Primary (`#0A84FF`):** Core action driver, active indicators, and high-priority directional cues.
- **Secondary (`#00D2FF`):** Telemetry accents, speed/distance metrics, and operational tooltips.
- **Tertiary (`#10B981`):** Net margins, live earnings, cost balance reductions, and positive fiscal yield.
- **Neutral Surface Baseline (`#0A0D12` / `#0F141C`):** Base canvas and container foundations engineered to eliminate halos and preserve battery life.
- **High-Contrast Text (`#F8FAFC`):** Pure contrast text layer delivering WCAG AAA ratios across all active operational metrics.
- **Muted Text (`#94A3B8`):** Secondary contextual cues, non-critical labels, and unit dividers.
- **Warning / Burn Rate (`#EF4444`):** Fuel depletion, maintenance overshoots, and critical operating deficits.

## Typography

Inter serves as the single universal type engine across all viewports. Its tall x-height, neutral aperture, and robust geometric skeletal structure provide uncompromised clarity through rain, sun-glare, and rapid physical vibrations.

### Numerical Legibility & Tabular Alignment
All financial numbers, fuel costs, real-time hourly rates, and trip metrics must enforce tabular figures (`font-variant-numeric: tabular-nums`). This guarantees that shifting decimal places do not induce horizontal jitter during high-velocity updates.

### Hierarchy & Scale
- **Display Hero:** Reserved for active day balance and live net run rates.
- **Metric Numeral:** Purpose-built for quick-glance split stats (e.g., R$/km, fuel burned, hourly margin).
- **Labels:** Uppercase tracking on compact labels provides instant categorical identification under glancing conditions.

## Layout & Spacing

The layout model is driven by an 8pt base grid with a tight 4pt sub-grid for data tags and telemetry items. The design system accommodates both single-handed thumb zones on mobile mounts and expansive fleet views on tablet/desktop dashboards.

### Viewport Adaptation & Breakpoints
- **Compact Viewport (360px – 767px):** Single-column layout. Margin set to `1rem` with safe bottom clearance (`80px`) above fixed thumb-level action bars. Gaps between metric modules are strictly `0.5rem` or `1rem` to maximize screen density without overcrowding.
- **Medium Viewport (768px – 1023px):** 6-column fluid grid. Gutters expand to `1.25rem`. Split views decouple route status from financial tallies.
- **Expanded Viewport (1024px+):** 12-column layout. Margin expands to `2.5rem`. Enables persistent telemetric data feeds, interactive route margin charts, and granular cost breakdowns side by side.

## Elevation & Depth

This system avoids heavy, atmospheric drop shadows that vanish under sunlight or wash out dark OLED screens. Elevation is established through tonal layering, crisp micro-borders, and targeted cyan/sapphire glow containment.

### Surface Tiers
- **Tier 0 (Base Canvas):** `#0A0D12` — Deep black background minimizing screen backlight bleeding.
- **Tier 1 (Metric Surfaces & Cards):** `#131924` — Recessed modular containers with a `1px` border of `rgba(255, 255, 255, 0.08)`.
- **Tier 2 (Floating Action Trays & Popovers):** `#1B2433` — Elevated thumb controls framed with `rgba(10, 132, 255, 0.25)` micro-borders.
- **Tier 3 (Active Interactivity & Alerts):** `#232E42` — Transient modal dialogs featuring an ambient glow: `0px 8px 24px rgba(10, 132, 255, 0.2)`.

### Border Accents
Borders function as structural separators. Rather than heavy dividing lines, cards use thin, low-opacity strokes (`1px solid rgba(255, 255, 255, 0.06)`), which transition to electric blue (`rgba(10, 132, 255, 0.6)`) when focused or dynamically tracking.

## Shapes

The shape architecture relies on controlled, ergonomic curvature. Level 2 roundedness provides modern visual softness without losing the technical, instrument-cluster edge required by courier telematics.

### Radii Tokens
- **Standard UI Elements (`0.5rem` / 8px):** Applied to metric chips, data cells, list row containers, and inline text inputs.
- **Cards & Primary Modules (`1rem` / 16px):** Applied to telemetry cards, map overview widgets, and profitability blocks.
- **Sheets & Trays (`1.5rem` / 24px):** Applied to bottom action drawers and critical transaction modals.
- **Action Buttons (`0.5rem` to `1rem`):** Buttons follow structural corners rather than round pill shapes, preserving maximum touch targets for gloved fingers.

## Components

Components are optimized for instant glance evaluation and glove-friendly thumb activation.

### Buttons
- **Primary Action (Start Shift / Accept Trip):** Solid `#0A84FF` background with `#FFFFFF` text. Minimum tap target height: `56px`. Font: `Inter` SemiBold `16px`. Active state introduces a high-saturation ciano edge glow.
- **Secondary Action (Fuel Log / Pause Shift):** Surface `#1B2433` with a crisp `1px` border in `#00D2FF` at 40% opacity. Text `#F8FAFC`.
- **Destructive / Stop (End Run):** Muted dark red container (`#2A1215`) with sharp coral-red text (`#F87171`) and border to prevent accidental termination.

### Telemetry Cards
- Containers use `#131924` with `1rem` border radius and `1px` stroke in `rgba(255, 255, 255, 0.07)`.
- High-contrast two-part anatomy: Top zone houses micro-label (uppercase, tracking +0.05em, text `#94A3B8`); bottom zone houses high-impact tabular metric (e.g., `R$ 4,82/km`) rendered in `#10B981` or `#F8FAFC`.

### Chips & Status Indicators
- **Profitable / Surplus:** Background `rgba(16, 185, 129, 0.12)`, border `1px solid rgba(16, 185, 129, 0.35)`, text `#10B981`.
- **Telemetry Active:** Background `rgba(0, 210, 255, 0.12)`, border `1px solid rgba(0, 210, 255, 0.35)`, text `#00D2FF`.
- **System Warning:** Background `rgba(239, 68, 68, 0.12)`, border `1px solid rgba(239, 68, 68, 0.35)`, text `#EF4444`.
- Padding: `4px 10px` with `0.5rem` border radius.

### Input Fields
- Dark input fields (`#0F141C`) framed by `1px` neutral borders (`rgba(255, 255, 255, 0.12)`).
- Input height: `52px` minimum. Numeric currency inputs use tabular `Inter` 20px Bold for effortless validation while entering odometer or fuel receipts on the bike. Focus states trigger a sharp `2px` ring in `#0A84FF`.

### Lists & Split Ledger Rows
- Continuous data rows separated by `1px solid rgba(255, 255, 255, 0.04)` dividers.
- Left side: Categorical icon badge (e.g., fuel pump, maintenance, payout) set inside a `#1B2433` container.
- Center: Transaction name and timestamp.
- Right: Monetary impact aligned right with green (`#10B981`) for inflows and neutral white (`#F8FAFC`) for expenses.

### Dynamic HUD Bar (Mobile Mount Persistent)
- A docked bottom tray anchored at the bottom edge. Houses real-time net efficiency (R$/hour, R$/km, net remaining) visible at a 60-degree angle on a phone clamp without obscuring primary app workflows.