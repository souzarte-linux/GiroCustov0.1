---
name: Orchid Glint
colors:
  surface: '#17111b'
  surface-dim: '#17111b'
  surface-bright: '#3e3742'
  surface-container-lowest: '#110c15'
  surface-container-low: '#1f1923'
  surface-container: '#231d27'
  surface-container-high: '#2e2832'
  surface-container-highest: '#39323d'
  on-surface: '#eadfed'
  on-surface-variant: '#d6c0d3'
  inverse-surface: '#eadfed'
  inverse-on-surface: '#352e39'
  outline: '#9f8b9d'
  outline-variant: '#524251'
  surface-tint: '#fbabff'
  primary: '#fbabff'
  on-primary: '#580065'
  primary-container: '#e14ef6'
  on-primary-container: '#4d0059'
  inverse-primary: '#a200ba'
  secondary: '#ddb8ff'
  on-secondary: '#490081'
  secondary-container: '#62259b'
  on-secondary-container: '#d1a1ff'
  tertiary: '#45dfa4'
  on-tertiary: '#003825'
  tertiary-container: '#00a574'
  on-tertiary-container: '#003120'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffd6fd'
  primary-fixed-dim: '#fbabff'
  on-primary-fixed: '#36003e'
  on-primary-fixed-variant: '#7c008e'
  secondary-fixed: '#f0dbff'
  secondary-fixed-dim: '#ddb8ff'
  on-secondary-fixed: '#2c0051'
  on-secondary-fixed-variant: '#62259b'
  tertiary-fixed: '#68fcbf'
  tertiary-fixed-dim: '#45dfa4'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#005137'
  background: '#17111b'
  on-background: '#eadfed'
  surface-variant: '#39323d'
typography:
  display-lg:
    fontFamily: Space Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 52px
    letterSpacing: -0.03em
  display-lg-mobile:
    fontFamily: Space Grotesk
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 30px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: 0em
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-lg:
    fontFamily: Space Grotesk
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Space Grotesk
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: 0.03em
  label-sm:
    fontFamily: Space Grotesk
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
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
  gutter-tablet: 1.5rem
  gutter-desktop: 2rem
  margin: 1rem
  margin-tablet: 2rem
  margin-desktop: 3rem
  space-2xs: 0.25rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
---

## Brand & Style

This design system is engineered for dynamic, outdoor, and high-motion utility environments where split-second legibility and technical elegance are paramount. Melding the precision of brutalist data-density with a vibrant, hyper-modern orchid-fuchsia palette, the visual identity prioritizes optical punch and instantaneous comprehension under direct, blazing sunlight.

The aesthetic pairs razor-sharp technical typography with luminous electric magenta and orchid tones, offset by crisp mint highlights to immediately signal high-yield financial data. The interface feels surgical, unapologetic, and modern—cutting away visual clutter in favor of crisp boundaries, high luminance contrast, and ergonomic tap targets optimized for one-handed operation or handlebar-mounted mobile use.

## Colors

The color architecture is built around extreme contrast and high-frequency luminescent signals, rigorously tested against screen washouts in outdoor environments.

- **Primary (`#D946EF`)**: Electric Orchid-Fuchsia. Used for decisive interactive targets, high-priority state indicators, and primary visual anchors.
- **Secondary (`#C084FC`)**: Soft Lavender-Violet. Provides optical support, category tags, and secondary action highlights without competing with primary alerts.
- **Tertiary (`#34D399`)**: Hyper-Luminous Mint. Reserved strictly for positive financial delta, profit indicators, operational efficiency rates, and active affirmative states.
- **Neutral (`#09050D`)**: Deep Obsidian Violet. Forms the base canvas for the dark mode experience, absorbing glare and driving maximum contrast against light and neon foreground elements.

### Semantic Tones & Outdoor Visibility
Surface variations range from `#140C1D` (Base Surface) to `#1F142D` (Elevated Card Surface) and `#2E1B43` (Interactive Surface). Text defaults to pure `#FFFFFF` for primary metrics, `#E9D5FF` for secondary labels, and muted `#A89BB5` for low-priority timestamps. Borders utilize high-luminance alpha strokes (`rgba(217, 70, 239, 0.35)`) to maintain physical structure without solid backgrounds.

## Typography

The typographic strategy balances raw technical cadence with fluid text legibility:

- **Space Grotesk** commands all quantitative data, headers, navigation anchors, and metrics. Its geometric proportions, open apertures, and distinctive ink-trap nuances make critical values instant to parse under high vibration and rapid glance checks.
- **Inter** handles narrative copy, input placeholders, multi-line notes, and descriptive helper labels. Neutral, balanced, and optically stable at tight resolutions, Inter mitigates eye fatigue across sustained usage.

For high-speed readouts, tabular numerical formatting (`font-variant-numeric: tabular-nums`) must be enforced on all financial tickers and speed/cost calculations.

## Layout & Spacing

A strictly paced 8-point base grid governs layout proportions, with 4-point micro-adjustments reserved for tight indicators and chip metrics.

### Grid & Breakpoints
- **Mobile (Default Canvas, < 640px)**: 4-column fluid layout with `margin: 1rem` and `gutter: 1rem`. Controls prioritize oversized touch zones (minimum 52px height) for accessible interaction while mobile.
- **Tablet (641px - 1024px)**: 8-column layout with `margin-tablet: 2rem` and `gutter-tablet: 1.5rem`. Enables persistent split panels (live summary alongside action feeds).
- **Desktop (> 1025px)**: 12-column layout with maximum content width capped at 1280px, flanked by `margin-desktop: 3rem`.

All component paddings step strictly through `space-xs` (8px) for badges, `space-md` (16px) for interior card structures, and `space-lg` (24px) for major sectional separations.

## Elevation & Depth

To prevent visual muddying in direct sunlight, this system relies on **Tonal Stratification and High-Luminance Structural Borders** rather than standard diffuse, murky drop-shadows.

1. **Level 0 (Base Floor - `#09050D`)**: The primary deep-violet dark abyss canvas.
2. **Level 1 (Card & Module Layer - `#140C1D`)**: Inset surfaces with a defined 1px perimeter border of `rgba(192, 132, 252, 0.15)`.
3. **Level 2 (Active/Floating Panels - `#1F142D`)**: Elevated modals, action trays, and running trackers. Framed by a 1px solid stroke of `rgba(217, 70, 239, 0.4)` and a localized neon backlight glow: `0 8px 32px -4px rgba(217, 70, 239, 0.25)`.
4. **Level 3 (Sticky Overlays & Critical Alerts - `#2E1B43`)**: 1.5px solid boundary of `#D946EF` with an ambient glow radius: `0 0 20px 2px rgba(217, 70, 239, 0.45)`.

## Shapes

The geometric signature uses a refined 8px standard corner radius (`roundedness: 2`), projecting technical discipline without the coldness of sharp boxes or the playfulness of circular capsules.

- **Standard Elements (8px / `0.5rem`)**: Buttons, inputs, standard cards, contextual dialogs, and alert banners.
- **Compound Modules (16px / `1rem`)**: Primary dashboard containers and bottom sheets.
- **Micro-Indicators (4px / `0.25rem`)**: Status indicator tags, mini-pills, and metric charts.

## Components

### Buttons
- **Primary**: Solid `#D946EF` fill, pure white `#FFFFFF` Space Grotesk 600 text, 8px radius, minimum 52px height. Resting state features an ambient neon aura; pressed state drops luminance to `#C026D3` with an inset micro-scale transform (0.98).
- **Secondary (Ghost Neon)**: Transparent body with a 1.5px border of `#C084FC`, text in `#E9D5FF`. Focus/Hover states trigger a subtle fill overlay of `rgba(192, 132, 252, 0.1)`.
- **Tertiary (Financial Confirm)**: Solid `#34D399` fill with obsidian-dark text (`#09050D`) for maximum optical contrast when confirming earnings, deliveries, or profit-generating runs.

### Cards & Telemetry Modules
- Built on `#140C1D` with 8px radius and a 1px border of `rgba(217, 70, 239, 0.2)`.
- Internal metrics feature label-sm text in muted lavender (`#A89BB5`) stacked directly over high-contrast Space Grotesk display values.

### Chips & Badges
- **Status / Dynamic Indicators**: Height capped at 32px with 4px border-radius.
- Positive ROI / Net Positive values use a darkened emerald-mint backing (`rgba(52, 211, 153, 0.12)`) paired with solid `#34D399` text.
- Cost / Expense metrics use soft violet-tinted backdrops (`rgba(192, 132, 252, 0.1)`) with `#C084FC` text.

### Form Inputs & Cost Trackers
- Background filled with `#1F142D`, zero bottom blur, framed by a 1.5px stroke of `rgba(192, 132, 252, 0.25)`.
- Focused state transforms the border to `#D946EF` with an instant outer bloom (`0 0 12px rgba(217, 70, 239, 0.35)`). Text entry rendered in high-contrast crisp `#FFFFFF`.

### Checkboxes & Toggle Controls
- Checkboxes: 24x24px with 4px radius. Unselected uses a 2px stroke in `#C084FC`. Selected uses a solid `#D946EF` fill with an optical-white Space Grotesk check icon.
- Toggles: 52x30px track with an 8px radius casing, switching between muted slate-purple off-state and high-visibility `#34D399` affirmative on-state.