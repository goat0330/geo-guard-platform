# geo-guard-fe Design Guide

Version: 1.0  
Baseline: 1920×1080

## 1. Overall direction

Product character:

**清新 / 专业 / 克制 / GIS**

The UI is an operational geological-disaster decision-support product, not a decorative exhibition dashboard.

Priority:

1. risk / task / decision state
2. location and evidence
3. primary action
4. supporting metrics
5. decoration

Default direction:

- light surfaces;
- clear information hierarchy;
- subtle borders and shadows;
- restrained blue as the primary action color;
- semantic red/orange/blue for risk and layer meaning;
- Apple-like translucent material mainly for map overlays;
- limited glow only where emphasis is necessary.

Avoid:

- generic dark-blue command-center UI;
- cyberpunk neon;
- strong glow around every marker;
- oversized gradients;
- glass-on-glass nesting;
- a separate style system for every new page.

---

## 2. Typography

Use the system font stack defined by `--gg-font-ui` in `tokens.css` for UI text
and metrics. Vendor font binaries are not included in the public source release.

Recommended hierarchy:

| Role | Size | Weight |
|---|---:|---:|
| Panel title | 18px | 800 |
| Section title | 16px | 700–800 |
| Component title | 14–16px | 600–700 |
| Body | 14px | 400 |
| Supporting | 12px | 400 |
| Dense map/chart caption | 10px | 400 |
| KPI | 22–26px | 600–800 |

Do not shrink important text simply to make it fit.

---

## 3. Core colors

Primary:
- action / selected / link: `#007BFF`
- hover: `#3395FF`
- soft blue: `#EFF6FF`

Text:
- primary: `#222527`
- strong secondary: `#383C41`
- secondary: `#617185`
- muted: `#9096A2`
- quiet: `#A6ACB8`

Semantic:
- danger: `#DD4739`
- danger strong: `#E23030`
- warning / orange: `#FF922C`
- amber: `#FFBD63`
- success: `#34A46F`

Use `tokens.css` rather than creating slightly different replacements.

---

## 4. Spacing / radius / shadow

Preferred spacing:

`4 / 8 / 12 / 16 / 20 / 24 / 28 / 32`

Typical:
- icon ↔ text: 6–8px
- card padding: 12–20px
- panel padding: 20–24px
- section gap: 20–28px

Radius:
- 4px: small tag
- 6px: compact map control
- 8px: standard card/tab
- 10px: popup/button/panel
- 12px: prominent card
- 999px: pill only

Shadows:
- keep subtle;
- normal cards often need border **or** shadow, not both heavily;
- no large black halo.

---

## 5. Cards / panels

Standard side panel:
- white/light surface;
- 20–24px main padding;
- 18px title;
- 16px section title;
- internal scrolling where needed.

Standard card:
- white or soft neutral fill;
- 1px quiet border;
- 8–12px radius;
- 12–20px padding.

Do not turn every information row into another bordered card.

---

## 6. Buttons / tags / tabs

Primary button:
- blue or restrained blue gradient;
- white text;
- 8–10px radius;
- normal height 40–44px.

Secondary:
- light blue or white;
- primary border/text.

Text action:
- transparent;
- primary text;
- small icon/arrow optional.

Tags:
- compact;
- 4px radius for rectangular tags;
- pill only for status chips;
- semantic color must carry meaning.

Segmented tabs:
- 8px outer radius;
- selected = primary blue;
- unselected = white/light blue.

---

## 7. Map glass

Use glass mainly for map overlays.

Light information popup:

```css
background: rgba(241, 243, 252, .86);
border: 1px solid rgba(255, 255, 255, .72);
backdrop-filter: blur(8px);
box-shadow: 0 4px 14px rgba(23, 43, 77, .10);
border-radius: 10px;
```

Compact control on imagery:

```css
background: rgba(69, 76, 88, .42);
border: 1px solid rgba(255, 255, 255, .42);
backdrop-filter: blur(10px);
border-radius: 6px;
```

Rules:
- light glass for information/detail;
- darker neutral glass is acceptable for compact map controls;
- no glow by default;
- no multiple nested glass cards.

---

## 8. GIS marker hierarchy

Current hazard-review source types:

1. 隐患点 — danger red
2. 基层自治风险点 — orange
3. 大排查风险点 — primary blue

Default marker:
- restrained size;
- clear silhouette;
- little or no glow;
- visible on imagery.

Hover:
- small contrast/scale increase.

Selected:
- stronger ring/halo allowed;
- selected popup/detail may appear.

Warning:
- use semantic warning/danger;
- avoid continuous flashing.

Do not use color as the only way to distinguish source types when marker shape/icon can help.

---

## 9. Relation lines

Current labels:

- `关联关系`
- `疑似关联`

Only draw a line when an actual review relation exists.

For `疑似关联`:
- moderate opacity;
- ~1.5–2px;
- dashed line is appropriate;
- increase emphasis only on hover/selected.

No decorative network lines.

---

## 10. Map popup

Typical detailed popup:
- about 360–420px desktop width;
- light glass;
- title + compact status tag;
- grouped fields;
- visible source/provenance when relevant.

Hazard-review detail hierarchy:

```text
Point name + status/level
↓
Threat / stability key facts
↓
Core identifiers / dates
↓
Source / evidence
↓
Review relation / result
↓
Action
```

Do not duplicate the same information in both a snapshot module and result module.

Do not show debug coordinates or implementation metadata in normal product UI.

---

## 11. Data provenance

For multi-source hazard review, source is first-class information.

Possible sources include:
- existing/historical hazard records;
- 141 / grassroots governance reporting;
- 百日攻坚 / large-scale screening data;
- applicable standards / policy evidence.

The user should understand where a conclusion came from without deciphering raw filenames.

---

## 12. Charts

Charts support decisions; they are not decoration.

- use semantic series colors;
- keep axes/grid quiet;
- avoid 3D effects;
- use gradients only when they improve readability;
- use the project's existing ECharts scaling helper.

---

## 13. Responsive / rem

Existing system:

```text
1920 design px
→ postcss-pxtorem
→ rem
→ src/utils/rem.js adjusts root font-size
```

Rules:

- author CSS in design `px`;
- never manually calculate rem;
- do not add a second global responsive scaling system;
- use `.norem` only for genuine fixed-pixel/GIS requirements.

---

## 14. Motion

Recommended:
- hover/color: 150–200ms
- popup/panel: 200–240ms
- expand/collapse: 240–300ms

Avoid:
- bounce;
- unnecessary pulsing;
- decorative looping animation.

---

## 15. Final rule

When adding UI:

**Reuse the nearest approved production pattern first.  
Use this file for visual direction.  
Use `tokens.css` for reusable values.  
Consistency and task clarity are more important than novelty.**
