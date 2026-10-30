---
name: GiroCusto Solar & Náutico
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#434655'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#0053db'
  primary: '#004ac6'
  on-primary: '#ffffff'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#b4c5ff'
  secondary: '#785a00'
  on-secondary: '#ffffff'
  secondary-container: '#fdc425'
  on-secondary-container: '#6d5200'
  tertiary: '#006242'
  on-tertiary: '#ffffff'
  tertiary-container: '#007d55'
  on-tertiary-container: '#bdffdb'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#ffdf9a'
  secondary-fixed-dim: '#f7be1d'
  on-secondary-fixed: '#251a00'
  on-secondary-fixed-variant: '#5a4300'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 36px
    fontWeight: '800'
    lineHeight: 44px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 30px
    fontWeight: '800'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
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
    fontWeight: '700'
    lineHeight: 20px
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
  metric-huge:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 44px
    letterSpacing: -0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-mobile: 0.75rem
  margin: 1rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system is crafted for high-velocity, glance-first mobile utility in urban delivery environments. Built primarily for delivery couriers and operators on motorcycles, the UI balances energetic solar highlights with authoritative nautical blues to ensure critical operational metrics—earnings, route fuel efficiency, and real-time net profit—are parsed in split seconds under direct sunlight or nighttime rides.

The design movement merges **Modern Material 3** tonal scaffolding with an assertive **High-Contrast Utilitarian** focus. Surfaces remain clean, structural, and distraction-free, minimizing visual noise while elevating status indicators, live telemetry, and financial totals into prominent, immediate focus.

## Colors

The palette establishes an unmistakable hierarchy built for instant situational awareness:

- **Primary (`#2563EB` Electric Deep Blue):** Anchors core brand identity, active tabs, primary call-to-action touchpoints, and stable operational structures. Supported by `#1D4ED8` for interaction states.
- **Secondary (`#EAB308` Solar Accent):** Highlights high-priority alerts, urgent route modifications, dynamic surge pricing, and live shift indicators. Paired with `#FACC15` for high-visibility badges under outdoor glare.
- **Tertiary (`#10B981` Emerald Gain):** Reserved explicitly for positive financial gains, completed deliveries, net profit spikes, and positive efficiency metrics.
- **Neutral (`#0F172A` Deep Nautical Ground):** Provides deep, glare-free foundational contrast for body typography in light mode, while supplying an OLED-saving foundation for dark mode surfaces.

### Dark Mode Architecture
Dark mode transitions background layers into a layered `#0B1120` and `#0F172A` spectrum. Text flips to crisp pure white (`#FFFFFF`) and slate tints (`#94A3B8`). Primary elements elevate to vibrant electric blue tones to prevent contrast loss against deep midnight backdrops.

## Typography

The typographic hierarchy combines the approachable, open-aperture geometry of **Plus Jakarta Sans** for headers, large numerals, and live ride telemetry with the systematic, high-legibility mechanics of **Inter** for dense transactional tables, logs, and sub-labels.

All numeric metric treatments utilize tabular figure styling to prevent layout shift during live ride metric recalculations. Kerning is kept slightly loose on small labels to maintain instant legibility on handlebar-mounted phone docks vibrating at speed.

## Layout & Spacing

Layouts follow a fluid 4-column structure on mobile devices, expanding to 8 columns on tablet devices.

- **Mobile Viewport Target:** 360px–428px standard delivery smartphones.
- **Touch Target Threshold:** All tap regions strictly respect a minimum 48px × 48px hit boundary, with crucial action anchors (Accept Ride, Finish Trip) elevated to a minimum height of 56px to accommodate gloved operation.
- **Rhythm:** Spacing follows an 8pt base grid with a 4pt sub-grid reserved for compact component groupings (e.g., metric label-to-value stacks).

## Elevation & Depth

Visual hierarchy combines Material 3 surface containers with subtle, tinted ambient shadows that keep interfaces grounded under direct daylight.

- **Level 0 (Base Surface):** `#F8FAFC` in light mode; `#0B1120` in dark mode. Completely flat.
- **Level 1 (Cards, List Groups):** Subtle elevation with `#0F172A` ambient shadow at 4% opacity with an 8px blur, bounded by a 1px border (`#E2E8F0` in light mode; `#1E293B` in dark mode).
- **Level 2 (Active Trip Sheets, Floating Metrics):** Medium elevation with `#0F172A` ambient shadow at 8% opacity, 16px blur radius, y-offset of 6px.
- **Level 3 (Modal Bottom Sheets, Sticky Navigation Overlays):** High elevation with deep blue-tinted shadow (`#1D4ED8` at 12% opacity mixed with `#0F172A` at 16%), 24px blur radius, y-offset of 12px.

## Shapes

The roundedness scale standardizes on friendly, ergonomic radii:
- Standard elements (Cards, Dialogs, Modals, Action Buttons) strictly employ **16px (`1rem` / rounded-lg)** corner geometry to meet the visual identity requirements.
- Compact controls (Chips, Badges, Micro-tags) utilize **8px (`0.5rem` / rounded)**.
- Floating status indicators and toggle pills utilize full circular boundaries (`9999px`).

## Components

### Buttons
- **Primary Action (Accept, Start Shift):** 56px height, 16px radius, solid `#2563EB` fill, `#FFFFFF` bold label. Interactive press state scales to 98% with `#1D4ED8` surface.
- **Urgent / Surge Action:** 56px height, solid `#EAB308` fill, `#0F172A` label for highest daylight contrast.
- **Secondary Action:** 48px height, outlined with 1.5px `#2563EB` border, transparent surface, `#2563EB` text.

### Financial Gain Displays (GiroCusto Gain Cards)
- Metric containers feature `#10B981` at 10% opacity surface tinting, framed by an Emerald hairline border (`#10B981` at 30%).
- Net gain figures are presented in `display-lg` typography with high-contrast `#047857` (light mode) or `#34D399` (dark mode) values.

### Chips & Route Badges
- 32px height, 8px radius.
- Filter chips carry `#F1F5F9` background with `#334155` text.
- Live surge chips use `#FEF08A` background with `#854D0E` text and an animated `#EAB308` dot indicator.

### Input Fields
- 52px height, 16px radius container.
- Light mode fill: `#F8FAFC` with `#CBD5E1` border.
- Active focus state: `#2563EB` 2px border ring with zero elevation drift.
- Placeholders and text styled in `body-lg` for ease of reading during outdoor entries.

### Checkboxes & Radios
- 24px × 24px touch-box centered within a 48px tappable bounding box.
- Checked state fills with `#2563EB` displaying a `#FFFFFF` icon.