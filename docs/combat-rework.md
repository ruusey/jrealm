# Combat Rework — Design Document

**Status:** Draft v0.1
**Owner:** Robert
**Last updated:** 2026-05-11

This doc designs the move from item-driven abilities to class-driven 4-ability skill kits + passives, plus the supporting party system. It is the source of truth for ability data shape, scaling math, and per-class kits. Engineering plans live elsewhere; this is the *design* layer.

Read order if skimming: §1 goals → §3 data model → §6 class kits → §7 combo graph.

---

## 1. Goals & non-goals

### Goals
- Replace the single ability-item slot with a **fixed 4-active + 1-passive kit baked into each character class**.
- Make **every stat scale something** on every class, so item choices feel meaningful regardless of class.
- Reward **party composition**: at least one ability per class either buffs allies, sets up an ally's ability, or triggers off an ally's action.
- Keep the combat loop legible: instant abilities by default, **cast times only for high-impact abilities** with telegraphs and movement lock.
- Land a vertical slice (Knight, Wizard, Archer + party MVP) before designing the other 11 classes.

### Non-goals (for v1)
- No skill-point allocation or talent trees within a class. Kits are fixed at character creation; choice happens via equipment + party comp.
- No cross-class hybrid kits, multiclassing, or runes.
- No new stats. The existing 8 (HP, MP, DEF, ATT, SPD, DEX, VIT, WIS) absorb all new scaling.
- **PvE only.** No PvP exists; all numbers, CC durations, and party synergies are tuned exclusively against enemy AI. Ignore PvP balance entirely.

---

## 2. Patterns we're borrowing from other games

A short tour of where the ideas come from, so the data model below has visible inspirations rather than feeling arbitrary.

| Game | Pattern we're stealing | Why it works |
|------|------------------------|--------------|
| **League of Legends** | 4 actives (Q/W/E/R) + 1 passive per champion. Ability damage = `base + (stat × ratio)` per stat. R is the long-CD spike ability. | Tight kit budget forces designers to make every ability matter. Multi-stat ratios mean items aren't binary "good/bad". |
| **World of Warcraft** | Spell coefficients: each ability declares which stats scale which output (damage, healing, duration). Coefficient sums to ~1.0 of cast time. | Lets a stat affect different things per ability — DEF→shield amount on one, DEF→deflect chance on another. |
| **Diablo 3 / 4** | Passives are pure stat-driven procs with clear conditions (on-hit, on-crit, while-at-full-HP, on-ally-cast). Tooltips state the condition + the effect. | Procs feel earned, not random. Easy to spec out in data. |
| **Path of Exile** | Skill data + scaling data are separate from item data. A skill is a small JSON object; items just modify the global scaling. | Decouples "what does this ability do" from "how strong are your gear stats" — the same content this doc separates. |
| **Final Fantasy XIV** | Cast bars are universal; instant abilities are the *exception*, and they exist for mobility/reaction. Cast time = power budget. | Cast time is the price you pay for a stronger ability. We use the same pricing. |
| **Risk of Rain 2** | Cooldown reduction stacks multiplicatively from gear, not flat-percent. Soft-capped via formula curve, not hard cap. | Avoids the "20% CDR cap" mess. We do this with a saturating curve on DEX. |

**The unifying principle:** an Ability is *data*, not code. Effects compose (damage + status + projectile). Stats scale numbers declared *on the ability*, not numbers buried in code. Class kits are lists of Ability IDs.

---

## 3. Data model

All schemas below live in `openrealm-data` JSON (alongside `game-items.json`, `projectile-groups.json`). The server reads them once on boot; clients ship them in the data bundle.

### 3.1 `Ability`

```jsonc
{
  "id": 1001,
  "name": "Shield Bash",
  "description": "Slam your shield forward, stunning enemies in a cone.",
  "iconKey": "ability.knight.shield_bash",
  "classId": 12,                    // Knight, for filtering/validation
  "slotHint": 0,                    // 0..3, suggested hotbar slot (Q/W/E/R)

  "mpCost": 35,
  "baseCooldownMs": 8000,
  "baseCastMs": 0,                  // 0 = instant
  "castMovementSpeedMul": 0.0,      // 0..1, multiplier on SPD while casting

  "effects": [
    {
      "type": "PROJECTILE_GROUP",
      "projectileGroupId": 4501,    // existing projectile-groups.json id
      "fanSpread": 0.12,
      "originOffset": 0.5
    },
    {
      "type": "STATUS_APPLY",
      "statusId": "STUNNED",         // existing StatusEffectType enum
      "baseDurationMs": 1500,
      "target": "ENEMIES_HIT"
    }
  ],

  "scalings": [
    { "stat": "ATT", "coeff": 0.80, "target": "DAMAGE" },
    { "stat": "VIT", "coeff": 0.30, "target": "DAMAGE" },
    { "stat": "VIT", "coeff": 10.0, "target": "STATUS_DURATION_MS", "effectIndex": 1 }
  ],

  "tags": ["physical", "melee", "cc", "aoe"]
}
```

**Field notes:**
- `effects` is an ordered list. `STATUS_APPLY` with `effectIndex` references its own index in scalings.
- `castMovementSpeedMul: 0.0` is the "slow to a halt" cast — matches your "speed to 1" idea but represented as a multiplier so we can also do "60% speed while channeling" (used for Archer R).
- `tags` exist for cross-system queries: "all physical abilities", "all cc", "abilities that proc on-cast passives".

### 3.2 `PassiveAbility`

```jsonc
{
  "id": 9001,
  "name": "Deflect",
  "description": "When struck by a projectile, chance to reflect it for bonus damage.",
  "iconKey": "passive.knight.deflect",
  "classId": 12,

  "triggers": [
    {
      "event": "ON_PROJECTILE_HIT_SELF",
      "conditions": [
        { "type": "PROC_CHANCE", "scaling": { "stat": "DEF", "coeff": 0.001, "target": "PROC_CHANCE", "cap": 0.25 } }
      ],
      "effects": [
        {
          "type": "REFLECT_PROJECTILE",
          "damageMul": 1.0,
          "damageBonusScalings": [
            { "stat": "DEF", "coeff": 0.005, "target": "REFLECT_DAMAGE_MUL" }
          ]
        }
      ]
    }
  ],

  "tags": ["defensive", "reactive"]
}
```

**Trigger events** (initial enum — extend as needed):
- `ON_PROJECTILE_HIT_SELF` — incoming hit
- `ON_BASIC_ATTACK` — own basic
- `ON_ABILITY_CAST` — any of self's actives
- `ON_KILL` — self gets a kill
- `ON_ALLY_CAST` — party member casts (enables support synergy)
- `ON_ALLY_KILL` — party member gets a kill nearby
- `ON_TAKE_DAMAGE` — any damage source
- `ON_HEAL_RECEIVED` — receive heal
- `ON_TICK` — periodic, e.g. aura-style passives (interval in `triggers[i].tickMs`)
- `ON_HP_THRESHOLD` — fires once when HP crosses a band (e.g., under 30%)

Passives can have **multiple triggers** for compound behavior (e.g., a passive that procs on-crit *and* on-kill).

### 3.3 `AbilityScaling`

```jsonc
{
  "stat": "WIS",
  "coeff": 1.0,
  "target": "DAMAGE",
  "effectIndex": 0,        // optional — defaults to "apply to ability as a whole"
  "cap": 250.0,            // optional hard cap on contribution
  "curve": "LINEAR"        // LINEAR (default) | DIMINISHING | THRESHOLD
}
```

**`target` enum** (this is the heart of the system — the list grows as we design):

| Target | Applies to | Notes |
|--------|-----------|-------|
| `DAMAGE` | damage rolls of the ability | added pre-mitigation |
| `HEAL` | heal effect output | |
| `SHIELD_AMOUNT` | shield/barrier HP | |
| `STATUS_DURATION_MS` | a STATUS_APPLY effect's duration | requires `effectIndex` |
| `STATUS_MAGNITUDE` | slow %, dmg-amp %, etc. on a status | requires `effectIndex` |
| `RADIUS` | AoE radius (tiles) | |
| `RANGE` | projectile/teleport range | |
| `PROJECTILE_COUNT` | fan count | rounded floor |
| `COOLDOWN_REDUCTION_MUL` | cooldown multiplier (0..1) | global, usually DEX |
| `CAST_TIME_MUL` | cast time multiplier (0..1) | global, usually SPD |
| `PROC_CHANCE` | passive trigger chance (0..1) | passives only |
| `REFLECT_DAMAGE_MUL` | reflected projectile bonus | passives only |
| `MP_COST_REDUCTION` | flat MP off cost | |
| `CHANNEL_DURATION_MS` | channel length | for channeled abilities |
| `MARK_AMPLIFY_PCT` | bonus dmg party deals to marked target | Archer-style |
| `EMPOWER_FREQUENCY` | "every Nth attack" — inverse, higher stat → smaller N | Wizard passive |

**Curves:**
- `LINEAR`: `contribution = stat × coeff` (clamped to cap)
- `DIMINISHING`: `contribution = cap × (1 - exp(-stat × coeff / cap))` — saturating, good for CDR/cast speed where we want soft caps
- `THRESHOLD`: `contribution = coeff if stat >= cap else 0` — discrete (rare; used for "at 50 DEX, unlock extra arrow")

**Why this shape:** WoW does this with hardcoded coefficients per spell. We do it with declarative `scalings[]` so designers (you) can tune in JSON without touching server code. Caps live next to scalings so each ability can have its own ceiling.

### 3.4 `AbilityTree` on `CharacterClassModel` *(revised)*

```jsonc
{
  "classId": 12,
  "className": "KNIGHT",
  // ... existing fields (stats, sprite, startingEquipment) ...
  "abilityTree": {
    "pool":          [1001, 1002, 1003, 1004, 1005, 1006],  // any id the class CAN bind
    "defaultHotbar": [1001, 1002, 1003, 1004],               // initial 4 (player can hotswap)
    "passive":        9001                                    // always-on, NOT bindable
  }
}
```

**Revised semantics (per design clarifications during Phase 2A):**

- **`pool`** is the full set of abilities the class can put on its hotbar. May
  hold a mix of `Ability` (active) ids and `PassiveAbility` (slotted passive)
  ids — IDs are looked up first in `ABILITIES`, then in `PASSIVES`. Disjoint
  id namespaces; designers must not reuse ids across the two tables.

- **`defaultHotbar`** is exactly 4 ids drawn from `pool` — the designer's
  recommended starting binding.

- **`passive`** is the class's *always-on* passive, **not** in `pool` and
  **not** bindable. Sits visually in the far-left cell of the ability bar but
  is not pressable. 0 means "no class passive."

**Per-player hotbar bindings** live on `Player.hotbarBindings: int[4]`
(transient in Phase 2A, persistent in Phase 2B via `CharacterStatsDto`).
Initialized from `defaultHotbar` at character creation. Mutated at runtime by
`HotbarSwapPacket` (Shift+N input): cycles the slot forward through `pool`
to the next id not currently bound to another slot.

### 3.5 HUD layout (Phase 2C target)

Bottom-middle bar previously held the 4-slot equipment hotbar. Phase 2C
replaces it with a **5-cell ability bar**:

```
+-------------------+-------------+-------------+-------------+-------------+
|  CLASS PASSIVE    | active Q    | active W    | active E    | active R    |
|  (always-on)      | key 1       | key 2       | key 3       | key 4       |
+-------------------+-------------+-------------+-------------+-------------+
   panel.hud.ability.0  .1           .2            .3            .4
```

- **Cell 0 (far-left)** — class passive icon. Not clickable. Hover tooltip
  shows the passive's name + description + scaling preview.
- **Cells 1..4** — hotbar bindings. Click / key 1..4 to cast. Hover tooltip
  shows: ability name, current rank (skill points invested), MP cost,
  cooldown, damage with current scalings, status effects applied,
  range/radius. `Shift+N` hotswaps the slot.

Equipment slots (`panel.hud.main.equip.0..4` from Phase 1B) move into the
**upper-right inventory panel** alongside the backpack grid — same visual
group, no separate bottom-middle equipment view.

### 3.6 Skill points (Phase 2D placeholder)

Each unlocked ability has a per-character "skill points invested" value.
Points come from leveling (and/or fame conversion — TBD). Tooltips display
current rank and the contribution it adds to scalings.

Implementation outline (full spec in Phase 2D):

- Add `CharacterStatsDto.abilitySkillPoints: Map<Integer, Integer>` (ability id → invested).
- New `Player.abilitySkillPoints: Map<Integer, Integer>` field — persisted.
- Synthetic `"SKILL_POINTS"` stat input usable in `AbilityScaling.stat`:
  ```
  { "stat": "SKILL_POINTS", "coeff": 5.0, "target": "DAMAGE" }
  ```
  resolves to the current invested-points value for the ability the scaling
  belongs to. (No additional resolution machinery needed — each scaling
  already knows its parent ability id at apply time.)
- New `InvestSkillPointPacket` (client → server). Server validates pool
  (player has free points, ability is unlocked, cap-per-ability respected).
- Server echoes via the next inventory/state broadcast.

This is **not built in Phase 2A** — the data model lands in Phase 2D once
the framework is otherwise running.

### 3.4-bis (legacy) — old fixed `actives[4]` shape

Removed in favor of the pool / defaultHotbar split above. Any earlier
references in this doc to `tree.actives[i]` should be read as
`hotbarBindings[i]` (per-player runtime state) — the IDs come from
`pool` via `defaultHotbar` at character spawn.

### 3.5 Per-player runtime state

On `Player`:

```java
long[] abilityCooldowns = new long[4];    // server-tick timestamp when CD ends
CastState currentCast;                    // null if not casting
short[] passiveCharges;                   // small int per passive trigger (stacks, etc.)
```

`CastState`:
```java
class CastState {
  int abilityId;
  long startTickMs;
  long endTickMs;
  float worldTargetX, worldTargetY;       // for ground-targeted casts
  boolean cancelable;                     // movement/damage cancels?
}
```

Cancel rules:
- Taking damage while casting: by default does NOT cancel (you can tank through). Tag `cast_cancel_on_damage` on the ability flips it. (Why default off: getting griefed out of every cast feels terrible. WoW's pushback model is too fiddly for this scope.)
- Moving while cast-locked: blocked by `castMovementSpeedMul: 0.0`. For `>0` values the player can drift.
- Stun/Paralyze/Daze applied during cast: always cancels.

---

## 4. Cast & cooldown math

### 4.1 Cooldown
```
effectiveCdMs = baseCooldownMs × (1 - cdr)
cdr = saturating(DEX, cap=0.40, k=0.005)
    = 0.40 × (1 - exp(-DEX × 0.005 / 0.40))
```
At DEX=0: 0% CDR. At DEX=50: ~22%. At DEX=100: ~36%. Asymptotic at 40%.

Per-ability `target: COOLDOWN_REDUCTION_MUL` scalings *stack additively* on top of this (used rarely, e.g. a passive that reduces only one ability's CD).

### 4.2 Cast time
```
effectiveCastMs = baseCastMs × (1 - castSpeed)
castSpeed = saturating(SPD, cap=0.30, k=0.004)
```
Same curve shape, lower cap. Casts have a hard floor of 200ms (no zero-cast exploits via SPD stacking).

### 4.3 Damage roll
```
abilityDamage = baseDamage(from effect's projectile) + sum(scaling.contribution for target=DAMAGE)
              → then applied through existing applyCombatDamageMods() pipeline (crit, lifesteal, etc.)
```
The existing `applyCombatDamageMods` stays; abilities just feed it a richer base number.

### 4.4 Status duration
```
durationMs = baseDurationMs + sum(scaling.contribution for target=STATUS_DURATION_MS, effectIndex=i)
```
Diminishing returns on hard CC (stun, paralyze) capped at 3000ms regardless of scaling.

---

## 5. Equipment & item modifier additions

### 5.1 Slots (final)

| Index | Slot | Notes |
|-------|------|-------|
| 0 | Weapon | drives basic attack |
| 1 | Armor | leather / heavy / robe — class-gated |
| 2 | Gauntlets | new — hand protection |
| 3 | Boots | new — foot protection |
| 4 | Ring | existing |

Old ability-item slot is **removed**. On first login after deploy, equipped ability items are auto-converted to **Ability Dust** (a stackable currency) deposited in the first empty backpack slot. Existing ability items in inventory or storage become legacy loot, also convertible to dust at NPC vendors. No item is silently deleted.

### 5.2 Ability-aware enchantments

We extend `Enchantment.effectType` with new variants targeting abilities:

| New effectType | Meaning | Example |
|----------------|---------|---------|
| `ABILITY_DAMAGE_PCT` | +N% damage to all abilities | gauntlet enchant |
| `ABILITY_CDR_PCT` | +N% global CDR (stacks with DEX) | ring enchant |
| `CAST_SPEED_PCT` | +N% cast speed (stacks with SPD) | boots enchant |
| `STATUS_DURATION_PCT` | +N% duration on applied statuses | knight-flavored ring |
| `PASSIVE_PROC_PCT` | +N% proc chance on passive | high-end enchant |
| `MANA_COST_REDUCTION` | -N flat MP cost | gauntlet enchant |
| `EMPOWERED_ABILITY_INDEX` | next cast of ability N costs 0 MP after kill | rare suffix |

Existing `STAT_DELTA` / `STAT_SCALE` enchantments still work and feed the per-ability scalings indirectly (more DEX → more CDR → all abilities benefit).

### 5.3 Tooltip requirements (memory: tooltips must surface mods)

Tooltips on items show, in order:
1. Name (rarity-colored)
2. Item type + slot + tier
3. Base stats grid (the 8 stats)
4. Enchantments list, each line showing target system in plain English (e.g., "Gauntlets: +8% Ability Damage")
5. Gem slots (filled gems shown with their effects)

No change to tooltip structure, just new effectType labels. Both clients render the same fields.

---

## 6. Vertical-slice class kits

Formatting per ability: **Name** (slot, cast time, cooldown) — flavor / scalings / effect.

Scaling format: `STAT × coeff → target`.

### 6.1 Knight (heavy armor, tank/initiator)

**Identity:** front-line peel, hard CC, projectile denial. Favors VIT, DEF, ATT.

**Passive — Deflect**
- Trigger: `ON_PROJECTILE_HIT_SELF`
- Proc chance: `DEF × 0.001 → PROC_CHANCE` (DIMINISHING, cap 0.25)
- On proc: reflect the projectile back at owner with `1.0 + DEF × 0.005` damage multiplier
- Tags: defensive, reactive

**Q — Shield Bash** (instant, 8s CD)
- Forward cone, ~3 tiles, hits up to 5 enemies
- `ATT × 0.80 → DAMAGE`
- `VIT × 0.30 → DAMAGE`
- Applies STUNNED, `1500ms + VIT × 10 → STATUS_DURATION_MS` (capped 3000ms by §4.4)
- 35 MP

**W — Taunt** (instant, 12s CD)
- Radius 4 tiles. Forces enemies to target self for 3s.
- `SPD × 0.05 → RADIUS` (final radius = 4 + scaled)
- Self gains ARMORED for duration, `WIS × 0.005 → STATUS_MAGNITUDE` on the DEF buff
- 40 MP

**E — Phalanx** (0.5s cast, immobile during cast, 20s CD)
- Plants a shield behind the knight (8-tile-wide rectangle, projectile-blocking only on enemy side)
- Shield HP: `VIT × 2.0 + DEF × 3.0 → SHIELD_AMOUNT`
- Duration: `3000ms + DEX × 20 → STATUS_DURATION_MS`
- 60 MP

**R — Last Stand** (instant, passive trigger, 90s CD)
- When self would take fatal damage, instead drop to 1 HP and gain INVINCIBLE.
- Invincibility duration: `2000ms + (1 - currentHpPct) × VIT × 30 → STATUS_DURATION_MS` (bigger payoff when closer to dying)
- 0 MP (triggered, not cast)

**Stat coverage check:** ATT✓ DEF✓ VIT✓ WIS✓ SPD✓ DEX✓ HP(indirect)✓ MP✓. Pass.

---

### 6.2 Wizard (robe, ranged burst)

**Identity:** ranged single-target + AoE burst. Favors WIS, DEX, MP.

**Passive — Arcane Surge**
- Trigger: `ON_BASIC_ATTACK`
- Every Nth basic attack becomes empowered (free MP, pierces, 1.5× dmg)
- `WIS × 0.033 → EMPOWER_FREQUENCY` (formula: `N = max(2, 5 - floor(contribution))`)
  - WIS 0 → every 5th, WIS 30 → every 4th, WIS 60 → every 3rd, WIS 90+ → every 2nd
- Tags: offensive, sustain

**Q — Fireball** (instant, 4s CD)
- Single projectile, splash on hit
- `WIS × 1.0 → DAMAGE`
- `ATT × 0.30 → DAMAGE`
- `WIS × 0.02 → RADIUS` (splash)
- 25 MP

**W — Frost Nova** (instant, 10s CD)
- PBAoE 3-tile radius, applies SLOWED + FROZEN tag (custom status)
- `WIS × 0.005 → STATUS_MAGNITUDE` (slow strength)
- `SPD × 0.04 → RADIUS`
- Frozen enemies hit by your Q within 3s take +25% damage ("shatter" — intra-kit combo)
- 35 MP

**E — Blink** (instant, 14s CD)
- Short-range teleport. Reuses existing teleport plumbing in `useAbility()`.
- `4 + SPD × 0.04 → RANGE` (tiles)
- 30 MP

**R — Meteor** (1.5s cast, immobile, 60s CD, telegraphed)
- Ground-targeted large AoE, telegraphed circle visible to all
- `WIS × 2.0 → DAMAGE`
- Bonus: consumes 50% of current MP on cast, adds `(MP_spent) × 0.5 → DAMAGE` — rewards casting at full mana, makes WIS gear pool valuable beyond regen
- `5.0 base + WIS × 0.03 → RADIUS`
- 50 MP base (then 50% of remainder)

**Stat coverage:** ATT✓ WIS✓ DEX✓ SPD✓ MP✓ VIT(survival while cast-locked)✓ DEF(same)✓ HP(same)✓.

---

### 6.3 Archer (leather, sustained DPS / utility)

**Identity:** burst openers vs. full-HP, party damage amp, kite. Favors DEX, SPD, ATT.

**Passive — Hawkeye**
- Trigger: `ON_BASIC_ATTACK` with condition `target.hpPct >= 1.0`
- Auto-crit (existing crit pipeline) on full-HP enemies
- Crit chance contribution: `DEX × 0.005 → PROC_CHANCE` (DIMINISHING, cap 1.0)
- Crit damage `1.5 + ATT × 0.002` multiplier
- Tags: offensive, opener

**Q — Multi-shot** (instant, 6s CD)
- Fan of arrows forward
- `PROJECTILE_COUNT = 3 + floor(DEX / 25) → max 7`
- Per-arrow: `ATT × 0.60 → DAMAGE`
- 30 MP

**W — Hunter's Mark** (instant, 15s CD)
- Single-target debuff
- Duration: `5000ms + WIS × 50 → STATUS_DURATION_MS`
- Marked target takes +N% damage from **all party members**: `10 + DEX × 0.1 → MARK_AMPLIFY_PCT`
- 25 MP

**E — Dash** (instant, 10s CD)
- Backward dash, then SPEEDY buff
- Distance: `3 + SPD × 0.02 → RANGE`
- SPEEDY duration: `1500ms + VIT × 15 → STATUS_DURATION_MS`
- 20 MP

**R — Rain of Arrows** (1s cast, 50% move speed during channel, 75s CD)
- Channeled for 3s, drops arrows in target zone, ticks every 250ms
- Per-tick: `ATT × 0.40 + DEX × 0.30 → DAMAGE`
- `4.0 + SPD × 0.03 → RADIUS`
- Channel duration: `3000ms + WIS × 25 → CHANNEL_DURATION_MS` (cap 5000ms)
- 70 MP

**Stat coverage:** all 8 touched.

---

## 7. Combo graph (slice)

The point of these three kits being designed together: they slot into each other. Visualizing:

```
                ┌──────────────────┐
                │  Knight Q (stun) │──────┐
                └──────────────────┘      │ enemy stunned
                                          ▼
                            ┌──────────────────────────┐
                            │  Archer W (Hunter's Mark)│
                            └──────────────────────────┘
                                          │ +10–20% party dmg
                                          ▼
                              ┌────────────────────┐
                              │  Wizard R (Meteor) │ ← landed because target is stunned
                              └────────────────────┘
```

**Survival combo:**
```
Wizard W (Frost Nova) ─ slow incoming melee
         │
         ▼
Knight E (Phalanx) ─ block projectiles
         │
         ▼
Archer E (Dash) ─ disengage with speed
```

**Cleanup combo (boss execute):**
```
Knight R (Last Stand) ─ tank the wipe-threat hit
         │
         ▼
Archer W (Mark) on boss
         │
         ▼
Wizard Q (Fireball) at full empowered stacks
```

When we design the other 11 classes, each one needs to plug into this graph in at least one place. (Necromancer's "kill-mark" passive could re-enable Hunter's Mark on kill, etc.)

---

## 8. Party system (MVP shape)

Detail this in its own doc; here are the constraints the combat rework imposes on it:

- Party members need to see each other's: HP, MP, current cast (ability + remaining ms), active status effects.
- `MARK_AMPLIFY_PCT` and `ON_ALLY_*` passive triggers require server-side knowledge of party membership at damage/cast resolution time.
- Single broadcast packet `PartyStatePacket` at ~5 Hz to party members only — enough for frame UI without flooding.
- Party max 4, mirrors the slice. Cross-realm sync deferred.

---

## 9. Balance heuristics

Rules of thumb for designing the other 11 classes' abilities so we don't blow up the spreadsheet:

1. **R abilities should hit 2.0–2.5× the damage of Q at the same point in the game.** R has 5–10× the cooldown; it must feel like a hammer.
2. **Total kit DPS (assuming hitting all abilities on CD) should be within ±10% across classes.** Class identity comes from *how* you do damage, not *how much*.
3. **Hard CC durations cap at 3s, soft CC at 5s.** Beyond that, players can't react.
4. **Cast times are between 0.5s and 2.0s.** Below 0.5 isn't worth the cast bar; above 2.0 feels miserable in PvE without a good payoff. R-level only.
5. **Passives should be 5–15% of a class's total power budget.** Strong enough to matter, weak enough that gear still drives the build.
6. **Every ability touches at least 2 stats in scalings.** Single-stat scaling is a balance dead-end (one ring of WIS dominates).
7. **MP costs sum to: kit fully on CD should drain ~60% of base MP pool per minute.** Forces some basic-attack downtime in long fights, which is where Arcane-Surge-style passives shine.

---

## 10. Remaining 11 classes — design stubs

To be expanded in v0.2. Identity sketches only:

- **Priest** (robe): party heals + cleanses. Passive: chain-heal on overheal.
- **Necromancer** (robe): summons + DoTs. Passive: marks targets; ally kill on marked = free skull projectile.
- **Mystic** (robe): debuffs + control. Passive: stasis lock duration extends on ally CC.
- **Sorcerer** (robe): chain lightning + multi-target. Passive: chains bounce more with more WIS.
- **Rogue** (leather): single-target burst from stealth. Passive: first hit from invisibility crits + applies bleed.
- **Assassin** (leather): execute + repositioning. Passive: below-30%-HP enemies take +25% dmg from you.
- **Trickster** (leather): clones + decoys. Passive: dropping below 50% HP swaps you with a decoy (long ICD).
- **Huntress** (leather): traps + zone control. Passive: enemies entering your AoEs take a snare.
- **Ninja** (leather): mobility + shuriken. Passive: every dash refreshes one ability charge.
- **Warrior** (heavy): sustained melee DPS. Passive: stacking ATT on consecutive hits.
- **Paladin** (heavy): party buffs + minor heals. Passive: aura — nearby allies gain DEF.

Each gets the same full treatment as Knight/Wizard/Archer in v0.2.

---

## 11. Open questions

These need answers before implementation, but not before v0.2 of this doc:

1. **Ability unlocking** — all 4 unlocked at level 1, or gated by level (Q at 1, W at 4, E at 7, R at 10)? Recommendation: level-gated, encourages re-rolling and gives leveling a sense of growth.
2. **Animations** — do we have the sprite budget for unique cast animations per class, or do we reuse a generic "casting" pose? Recommendation: generic pose + per-class FX overlay for v1.
3. **Cast-cancel UX** — does pressing the same button again cancel a cast? Or right-click? Recommendation: same hotkey cancels, refunds 50% MP.
4. **Ability item loot replacement** — what drops in their place? Recommendation: gem fragments + boot/gauntlet drops weighted higher.
5. **Reflected damage from Deflect** — does it scale with attacker's DEF (their mitigation) or bypass? Recommendation: bypass, otherwise the passive is dead vs. high-DEF enemies.
6. **MP regen during cast** — paused, normal, or accelerated? Recommendation: paused, encourages weaving basics between casts.

---

## 12. Implementation order (recap from brainstorm)

Owned by engineering plan, repeated here for context:

1. **Phase 1A — Data pipeline scaffolding.** Empty `abilities.json` + `passives.json` registered across all three repos (editor whitelist, mtime list, server + native loaders, editor.js tab). Zero gameplay change. Unblocks Phase 2.
2. **Phase 1B — Equipment slots.** Expand inventory array from 20 → 22. Add gauntlets (slot 4) + boots (slot 5). **Keep legacy ability slot (1) intact** until Phase 2 replaces it. Backpack stays 16 slots, just shifts from 4..19 → 6..21. No item loss on existing saves.
3. **Phase 2 — Ability framework.** Build the data model + packets + hotbar UI + cooldown/cast system. Port each class's existing ability-item into the Q slot of its new tree. **At end of Phase 2: remove legacy ability slot.** Inventory becomes 5 equipment + backpack (final layout).
4. **Phase 3 — Vertical slice.** Knight/Wizard/Archer full 4+1 kits.
5. **Phase 4 — Party MVP** (parallelizable with Phase 3).
6. **Phase 5 — Remaining 11 class kits.**

### 12.1 Slot migration strategy

The slot-index shift is the riskiest data change. The two-stage path:

**Intermediate state (after Phase 1B, before Phase 2 ships):**
| Slot | Contents |
|------|----------|
| 0 | Weapon |
| 1 | Ability item (legacy, still used by existing useAbility code path) |
| 2 | Armor |
| 3 | Ring |
| 4 | Gauntlets (new) |
| 5 | Boots (new) |
| 6..21 | Backpack (16 slots — unchanged size, just shifted) |

Inventory array length: **22**. Existing saves migrate by widening the array: `new[0..5] = old[0..3] + [null, null]`; `new[6..21] = old[4..19]`. No item loss.

**Final state (after Phase 2):**
| Slot | Contents |
|------|----------|
| 0 | Weapon |
| 1 | Armor |
| 2 | Gauntlets |
| 3 | Boots |
| 4 | Ring |
| 5..20 | Backpack (16 slots) |

Inventory array length: **21**. Phase-2 migration: legacy ability-item at intermediate slot 1 gets converted to a stack of "Ability Dust" currency (or salvaged for gem fragments — decision pending §11 Q4) and dropped into backpack. Remaining slots compact accordingly. Item 817 (Ability Essence) similarly converted.

### 12.2 Files with hardcoded slot indices (full list from research)

For Phase 1B, every `< 4` boundary, `getSlots(0, 4)`, and equipment-slot constant in the codebase needs to become `< 6` / `getSlots(0, 6)`. Migration checklist:

**Server (`openrealm`):**
- `Player.java:181` (array size 20→22), `:281` (getSlots(0,4)→(0,6)), `:543/561/605` (backpack ranges shift)
- `ServerItemHelper.java:37` (canEquipInSlot bound), `:73/114/166/187` (boundary checks)
- `LootContainer.java:159–171` (cyan tier switch — add cases for new slots 4, 5)
- `GlobalConstants.java:19–23` (add CYAN_BAG_MIN_GAUNTLET_TIER, CYAN_BAG_MIN_BOOTS_TIER)
- `ServerForgeHelper.java:79/188/210` (forgeSlotId comparisons — new essence types for gauntlet=4, boot=5)
- `UpdatePacket.java:120–148` (toEquipmentOnly loop bound 4→6)
- `MoveItemPacket.isEquipment()` boundary

**Data (`openrealm-data`):**
- `character-classes.json` — startingEquipment map keys unchanged for existing items; can optionally add gauntlets/boots starts later
- `game-items.json` — no targetSlot changes needed for intermediate state (existing items keep slot 0/1/2/3). New gauntlet/boot items added with targetSlot 4/5.
- `ui-components.json` — add `panel.hud.main.equip.4`, `.5` and `panel.hud.equipment_with_stats.4`, `.5`

**Native client (`openrealm-native-client`):**
- `Player.java:293/298` (mirror server)
- `PlayerUI.java:245–256` (slotX layout for 6 cells), `:291–305` (setEquipment bound), `:487–493` (buildEquipmentSlots), `:967/1893` (getSlots ranges)

**Web client:** mirror native — hotbar UI grid `cols: 4 → 6` plus matching component refs in canvas renderer.

### 12.3 Phase 2 deferred work

These changes are deferred to Phase 2 (when ability slot is removed) so Phase 1B can land cleanly:

- `Player.getAbility()` (slot 1 lookup) — stays as-is during intermediate state.
- `LootContainer` ability-tier case — stays as-is.
- `ServerForgeHelper` ability essence (forgeSlotId=1) — stays as-is.
- `game-items.json` item 817 (Ability Essence) — stays as-is, deprecated after Phase 2.
- All `useAbility()` server logic — stays as-is.

---

## Appendix A — Existing-system touchpoints

For implementers, the files this design lands in:

**Server (`openrealm`):**
- `CharacterClass.java` (11–50), `CharacterClassModel.java` — add `abilityTree`
- `RealmManagerServer.useAbility()` (1956–2068) — replace item lookup with `abilityTree.actives[index]`
- `ServerGameLogic.handleUseAbilityServer()` (471–480) — read `abilityIndex` from packet
- `Player.java` (38–182) — equipment slot indices change; add `currentCast`, `abilityCooldowns[]`
- New: `Ability.java`, `PassiveAbility.java`, `AbilityScaling.java`, `AbilityTree.java`, `CastState.java`
- New packets: `AbilityCastStartPacket`, `AbilityCastFinishPacket`, `PartyStatePacket`
- Modified: `UseAbilityPacket` schema (`abilityIndex` field)

**Native client (`openrealm-native-client`):**
- `PlayState.input()` (1249–1277) — rebind right-click + 1..4 keys to hotbar
- `PlayerUI.java` — new hotbar component, party frames, layout shift for 5-slot equipment
- `Slots.java` — no structural changes, just new layout positions
- New: `AbilityHotbar.java`, `CastBar.java`, `PartyFrame.java`
- Protocol classes duplicated from server (per repo policy)

**Web client (`openrealm-data/src`):**
- Mirror native client changes; hotbar + cast bar + party frames in canvas UI
- `ui-components.json` — new entries `panel.hud.ability.0..3`, `panel.hud.cast_bar`, `panel.hud.party.0..3`
- Protocol classes duplicated

**Data (`openrealm-data`):**
- New: `abilities.json`, `passives.json`
- Modified: `character-classes.json` (add `abilityTree`), `game-items.json` (gauntlets + boots categories, remove ability-item entries or migrate)

---
*End v0.1. Next revision: fill in §10 stubs once v0.1 is reviewed.*
