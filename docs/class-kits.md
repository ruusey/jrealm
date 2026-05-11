# OpenRealm — Class Kit Reference

_Auto-generated from `abilities.json` / `passives.json` / `character-classes.json`. Slots map to hotbar keys 1-4 (Q/W/E/R is just designer shorthand)._

## Status effect IDs (referenced below)

| ID | Name | Mechanical effect |
|---|---|---|
| 0 | INVISIBLE | Hidden from enemy targeting |
| 1 | HEALING | Periodic HP regen |
| 2 | PARALYZED | Can't move |
| 3 | STUNNED | Can't move or act |
| 4 | SPEEDY | Movement speed up |
| 6 | INVINCIBLE | Take no damage |
| 9 | TELEPORT | Move to target spot |
| 14 | DAMAGING | +50% damage output |
| 15 | STASIS | Frozen, untargetable, takes no damage |
| 16 | CURSED | Takes +25% damage |
| 17 | POISONED | DoT damage over time |
| 18 | ARMORED | 2× DEF |
| 19 | BERSERK | High attack speed, lose DEF |
| 21 | SLOWED | Movement speed down |
| 22 | ARMOR_BROKEN | DEF = 0 |
| 23 | TAUNT_TARGET | Enemies prioritize you for targeted projectiles |
| 24 | BRACED | 1.5× DEF |
| 25 | PROTECTED | +5 VIT (Priest aura) |
| 26 | PHALANX_DOME | Enemy bullets entering caster's 96px sphere are destroyed |

---

## Rogue  *(classId 0)*

### Passive — **Shadow Step**
*On taking damage you have a chance to vanish briefly.*

_Tags: flavor_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Smoke Bomb**
*Drop a smoke bomb. INVISIBLE for 2.5s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 50 | 6s | — | visual_at_self:9, self_buff |

- **Effects:** `INVISIBLE` 2.5s → self
- **Scaling:** —

#### W (2) — **Shadowstep**
*Streak forward. SPEEDY 3s + INVISIBLE 1.5s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 10s | — | dash_trail, self_buff |

- **Effects:** `SPEEDY` 3s → self; `INVISIBLE` 1.5s → self
- **Scaling:** —

#### E (3) — **Caltrops**
*Scatter caltrops. SLOW + small damage.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 50 | 8s | 150 | curse, aoe_targeted |

- **Effects:** `SLOWED` 3.5s → enemies hit
- **Scaling:** +6/DEX over 75 → DAMAGE

#### R (4) — ultimate — **Assassinate**
*A surgical strike. Armor-piercing burst.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 175 | 1 min 15s | 1500 | curse, aoe_targeted, armor_pierce, ultimate |

- **Effects:** —
- **Scaling:** +25/DEX over 75 → DAMAGE

---

## Archer  *(classId 1)*

### Passive — **Hawkeye**
*Every Nth basic attack is empowered: deals 1.5× damage and emits a flash. N drops as DEX rises (every 5th down to every 2nd).*

_Tags: offensive, opener_

**Triggers:**
- ON_BASIC_ATTACK → `EMPOWER_NEXT_BASIC` → self; scaling: +0.033×DEX → EMPOWER_FREQUENCY

#### Q (1) — **Multi-Shot**
*Fan of paralyzing arrows ahead of you. Premium burst window — costs 60 MP.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 1s | — | legacy_port |

- **Effects:** fires projectile group 23; `PARALYZED` 3s → enemies hit
- **Scaling:** +1×ATT → DAMAGE

#### W (2) — **Hunter's Mark**
*Mark every enemy in a clearly outlined zone. CURSED for 8s — they take 25% more damage from every source.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 40 | 14s | — | aoe_targeted, curse, outline_ring |

- **Effects:** `CURSED` 8s → enemies hit
- **Scaling:** —

#### E (3) — **Haste**
*A burst of focus. Gain SPEEDY for 5s. No teleport — just pure footwork.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 50 | 10s | — | self_buff, visual_at_self:12 |

- **Effects:** `SPEEDY` 5s → self
- **Scaling:** —

#### R (4) — ultimate — **Rain of Arrows**
*Call down a barrage of arrows from the sky into the targeted zone. Armor-piercing damage + PARALYZE 1.5s. Scales with DEX.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 90 | 45s | 1500 | aoe_targeted, ultimate, armor_pierce, rain_arrows |

- **Effects:** `PARALYZED` 1.5s → enemies hit
- **Scaling:** +25/DEX over 75 → DAMAGE

---

## Wizard  *(classId 2)*

### Passive — **Arcane Surge**
*Every Nth basic attack is empowered: deals 1.5× damage and emits an arcane burst. N drops as WIS rises (every 5th down to every 2nd).*

_Tags: offensive, sustain_

**Triggers:**
- ON_BASIC_ATTACK → `EMPOWER_NEXT_BASIC` → self; scaling: +0.033×WIS → EMPOWER_FREQUENCY

#### Q (1) — **Fire Spray Spell**
*A simple spell that releases a burst of fire.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 90 | 500 ms | — | legacy_port |

- **Effects:** fires projectile group 15; `DAMAGING` instant → enemies hit
- **Scaling:** +1×ATT → DAMAGE

#### W (2) — **Frost Nova**
*An expanding ring of cold at your cursor. Enemies caught in it are slowed for 2.5s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 35 | 10s | — | aoe, cc, frost, aoe_targeted |

- **Effects:** `SLOWED` 2.5s → enemies hit
- **Scaling:** +0.005×WIS → STATUS_MAGNITUDE; +0.04×SPD → RADIUS

#### E (3) — **Blink**
*Teleport instantly to your cursor.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 30 | 14s | — | mobility, instant |

- **Effects:** `TELEPORT` instant → self
- **Scaling:** +0.04×SPD → RANGE

#### R (4) — ultimate — **Meteor**
*A meteor crashes down. Massive armor-piercing area damage.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 70 | 1 min | 2000 | aoe, burst, ultimate, from_sky, aoe_targeted, armor_pierce |

- **Effects:** —
- **Scaling:** +100/WIS over 75 → DAMAGE

---

## Priest  *(classId 3)*

### Passive — **Protective Aura**
*Every ally within a 5-tile radius (the priest included) gains +5 VIT. Effect refreshes continuously while in range.*

_Tags: support, aura_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Holy Heal**
*A pulse of holy light heals you and nearby allies. Heal amount and radius scale with WIS.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 50 | 10s | — | aoe_ally, heal |

- **Effects:** heal 60 → allies hit
- **Scaling:** +0.6×WIS → HEAL; +0.3×WIS → RADIUS

#### W (2) — **Cleanse**
*Strip negative status effects from up to 4 nearby allies. Range scales with WIS. (Skill points will raise the cap.)*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 65 | 30s | — | aoe_ally, cleanse |

- **Effects:** cleanse status effects (up to 4) → allies hit
- **Scaling:** +0.4×WIS → RADIUS

#### E (3) — **Holy Beam**
*A heavenly beam crashes down on the targeted location. Armor-piercing high damage.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 90 | 1 min | 1000 | aoe_targeted, armor_pierce, from_sky, holy |

- **Effects:** —
- **Scaling:** +30/WIS over 75 → DAMAGE

#### R (4) — ultimate — **Sanctuary**
*Bless every ally in a wide radius — INVINCIBLE for 5s. WIS widens the radius. (Skill points reduce CD, max 3.)*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 200 | 5 min | — | aoe_ally, bless, outline_ring, ultimate |

- **Effects:** `INVINCIBLE` 5s → allies hit
- **Scaling:** +0.8×WIS → RADIUS

---

## Warrior  *(classId 4)*

### Passive — **Bloodlust**
*Killing an enemy temporarily boosts your damage.*

_Tags: flavor_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Rally**
*Steel yourself. DAMAGING + SPEEDY 4s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 50 | 6s | — | visual_at_self:12, self_buff |

- **Effects:** `DAMAGING` 4s → self; `SPEEDY` 4s → self
- **Scaling:** —

#### W (2) — **War Cry**
*Roar. DAMAGING 5s + SPEEDY 5s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 70 | 14s | — | taunt_visual, self_buff |

- **Effects:** `DAMAGING` 5s → self; `SPEEDY` 5s → self
- **Scaling:** —

#### E (3) — **Reckless Swing**
*Wild sweep. STUN nearby enemies.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 9s | 250 | fire, force_push, aoe_targeted |

- **Effects:** `STUNNED` 1.5s → enemies hit
- **Scaling:** +1.5/ATT over 75 → DAMAGE

#### R (4) — ultimate — **Rampage**
*Lose yourself. BERSERK + DAMAGING + SPEEDY 6s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 175 | 1 min 30s | — | ultimate, visual_at_self:12, self_buff |

- **Effects:** `BERSERK` 6s → self; `DAMAGING` 6s → self; `SPEEDY` 6s → self
- **Scaling:** —

---

## Knight  *(classId 5)*

### Passive — **Deflect**
*When struck by a projectile, chance to reflect it back at the attacker for extra damage. Proc chance scales with DEF (up to 25%); reflected damage scales with DEF.*

_Tags: defensive, reactive_

**Triggers:**
- ON_PROJECTILE_HIT_SELF → —; scaling: —

#### Q (1) — **Shield Slam**
*Surge forward with a thundering shield slam at the targeted spot. Stuns enemies in the impact radius.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 4s | 250 | aoe_targeted, force_push |

- **Effects:** `STUNNED` 1.5s → enemies hit
- **Scaling:** +1.5×ATT → DAMAGE

#### W (2) — **Taunt**
*Roar a defiant challenge. Mark yourself as a priority target — enemies in range prioritize you for targeted attacks. Lasts 6s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 12s | — | self_buff, taunt, taunt_visual |

- **Effects:** `TAUNT_TARGET` 6s → self
- **Scaling:** —

#### E (3) — **Brace**
*Plant your shield. Gain BRACED (1.5× DEF) for 5s, but you're SLOWED for the duration.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 70 | 12s | — | self_buff, defensive, brace_visual |

- **Effects:** `BRACED` 5s → self; `SLOWED` 5s → self
- **Scaling:** —

#### R (4) — ultimate — **Phalanx**
*Conjure an impenetrable spherical shield around yourself. Any enemy bullet that enters the dome is destroyed — anyone standing inside is sheltered. Lasts 3.5s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 200 | 2 min | — | self_buff, ultimate, phalanx |

- **Effects:** `PHALANX_DOME` 3.5s → self
- **Scaling:** —

---

## Paladin  *(classId 6)*

### Passive — **Holy Resolve**
*Below half HP your defenses tighten.*

_Tags: flavor_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Smite**
*Holy light at the target. Armor-piercing.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 50 | 4s | 200 | holy, aoe_targeted, armor_pierce |

- **Effects:** —
- **Scaling:** +12/WIS over 75 → DAMAGE

#### W (2) — **Inspire**
*Heal nearby allies + SPEEDY 3s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 12s | — | bless, aoe_ally, heal |

- **Effects:** heal 60 → allies hit; `SPEEDY` 3s → allies hit
- **Scaling:** +0.6×WIS → HEAL; +0.3×WIS → RADIUS

#### E (3) — **Holy Shield**
*Brace your faith. BRACED 5s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 70 | 12s | — | brace_visual, self_buff |

- **Effects:** `BRACED` 5s → self
- **Scaling:** —

#### R (4) — ultimate — **Divine Verdict**
*Heaven's judgment. Armor-piercing + STUN.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 175 | 1 min 30s | 1500 | from_sky, holy, aoe_targeted, armor_pierce, ultimate |

- **Effects:** `STUNNED` 2s → enemies hit
- **Scaling:** +25/WIS over 75 → DAMAGE

---

## Assassin  *(classId 7)*

### Passive — **Lethal Wound**
*Your basic attacks have a chance to apply POISONED.*

_Tags: flavor_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Poison Strike**
*Venomous flick. POISON enemies.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 40 | 3.5s | 100 | curse, aoe_targeted |

- **Effects:** `POISONED` 4s → enemies hit
- **Scaling:** +5/DEX over 75 → DAMAGE

#### W (2) — **Toxic Cloud**
*Lingering poison. POISON + SLOW.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 65 | 14s | 200 | curse, aoe_targeted |

- **Effects:** `POISONED` 5s → enemies hit; `SLOWED` 4s → enemies hit
- **Scaling:** +8/DEX over 75 → DAMAGE

#### E (3) — **Vanish**
*Slip into shadow. INVISIBLE 3s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 16s | — | visual_at_self:9, self_buff |

- **Effects:** `INVISIBLE` 3s → self
- **Scaling:** —

#### R (4) — ultimate — **Venom Burst**
*Concentrated venom. Armor-pierce + POISON.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 175 | 1 min 15s | 1400 | curse, aoe_targeted, armor_pierce, ultimate |

- **Effects:** `POISONED` 6s → enemies hit
- **Scaling:** +22/DEX over 75 → DAMAGE

---

## Necromancer  *(classId 8)*

### Passive — **Necrotic Aura**
*Nearby enemies feel a creeping CURSE.*

_Tags: flavor_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Drain Touch**
*Curse the ground. CURSED enemies in radius.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 45 | 5s | 120 | curse, aoe_targeted |

- **Effects:** `CURSED` 5s → enemies hit
- **Scaling:** +6/WIS over 75 → DAMAGE

#### W (2) — **Bone Spikes**
*Spikes erupt. Damage + PARALYZE.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 12s | 300 | fire, aoe_targeted |

- **Effects:** `PARALYZED` 1.5s → enemies hit
- **Scaling:** +8/WIS over 75 → DAMAGE

#### E (3) — **Death Pact**
*Bargain. DAMAGING + HEALING 5s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 70 | 18s | — | visual_at_self:4, self_buff |

- **Effects:** `DAMAGING` 5s → self; `HEALING` 5s → self
- **Scaling:** —

#### R (4) — ultimate — **Soul Harvest**
*Rip souls. Armor-pierce + long CURSE.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 175 | 1 min 30s | 1500 | curse, aoe_targeted, armor_pierce, ultimate |

- **Effects:** `CURSED` 8s → enemies hit
- **Scaling:** +25/WIS over 75 → DAMAGE

---

## Sorcerer  *(classId 9)*

### Passive — **Arcane Mastery**
*Your abilities cost slightly less mana.*

_Tags: flavor_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Mana Bolt**
*Focused mana bolt.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 35 | 2.5s | 150 | fire, aoe_targeted |

- **Effects:** —
- **Scaling:** +8/WIS over 75 → DAMAGE

#### W (2) — **Frost Field**
*Freeze the ground. SLOW + brief STASIS.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 12s | 100 | frost, aoe_targeted |

- **Effects:** `SLOWED` 4s → enemies hit; `STASIS` 1.2s → enemies hit
- **Scaling:** +5/WIS over 75 → DAMAGE

#### E (3) — **Mana Shield**
*Weave a barrier. ARMORED 4s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 70 | 14s | — | brace_visual, self_buff |

- **Effects:** `ARMORED` 4s → self
- **Scaling:** —

#### R (4) — ultimate — **Time Stop**
*Halt time. Huge STASIS 2s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 175 | 2 min | — | outline_ring, aoe_targeted, ultimate |

- **Effects:** `STASIS` 2s → enemies hit
- **Scaling:** +0/WIS over 75 → DAMAGE

---

## Huntress  *(classId 10)*

### Passive — **Wild Instinct**
*Above 75% HP your DEX is sharpened.*

_Tags: flavor_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Snare**
*Lay a snare. SLOW + small damage.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 40 | 4s | 120 | curse, aoe_targeted |

- **Effects:** `SLOWED` 3.5s → enemies hit
- **Scaling:** +6/DEX over 75 → DAMAGE

#### W (2) — **Track**
*Mark a zone. CURSED enemies take +25% damage.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 55 | 14s | — | curse, outline_ring, aoe_targeted |

- **Effects:** `CURSED` 8s → enemies hit
- **Scaling:** +0/DEX over 75 → DAMAGE

#### E (3) — **Wildform**
*Embrace the wild. SPEEDY + DAMAGING 4s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 65 | 14s | — | visual_at_self:12, self_buff |

- **Effects:** `SPEEDY` 4s → self; `DAMAGING` 4s → self
- **Scaling:** —

#### R (4) — ultimate — **Beast Unleashed**
*Fully shift. DAMAGING + ARMORED + SPEEDY 6s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 180 | 1 min 40s | — | ultimate, visual_at_self:12, self_buff |

- **Effects:** `DAMAGING` 6s → self; `ARMORED` 6s → self; `SPEEDY` 6s → self
- **Scaling:** —

---

## Mystic  *(classId 11)*

### Passive — **Static Charge**
*Your basic attacks crackle, occasionally stunning.*

_Tags: flavor_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Spark**
*A jolt of static.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 35 | 2.5s | 150 | fire, aoe_targeted |

- **Effects:** —
- **Scaling:** +8/WIS over 75 → DAMAGE

#### W (2) — **Chain Bolt**
*Arcing lightning. Damage + brief STUN.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 11s | 250 | fire, aoe_targeted |

- **Effects:** `STUNNED` 1.2s → enemies hit
- **Scaling:** +10/WIS over 75 → DAMAGE

#### E (3) — **Storm Brew**
*Crackling power. DAMAGING 5s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 14s | — | visual_at_self:10, self_buff |

- **Effects:** `DAMAGING` 5s → self
- **Scaling:** —

#### R (4) — ultimate — **Thunderstorm**
*A localized storm. Big damage + STUN.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 175 | 1 min 30s | 1400 | fire, aoe_targeted, ultimate |

- **Effects:** `STUNNED` 2s → enemies hit
- **Scaling:** +22/WIS over 75 → DAMAGE

---

## Trickster  *(classId 12)*

### Passive — **Sleight of Hand**
*Casting a trap occasionally hastens you.*

_Tags: flavor_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Smoke Trap**
*Drop a smoke trap. SLOW nearby enemies.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 40 | 4s | 100 | curse, aoe_targeted |

- **Effects:** `SLOWED` 4s → enemies hit
- **Scaling:** +5/DEX over 75 → DAMAGE

#### W (2) — **Snare Trap**
*Plant a snare. PARALYZE briefly.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 55 | 13s | 120 | fire, aoe_targeted |

- **Effects:** `PARALYZED` 2s → enemies hit
- **Scaling:** +5/DEX over 75 → DAMAGE

#### E (3) — **Misdirection**
*Confuse pursuers. INVISIBLE 3s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 16s | — | visual_at_self:9, self_buff |

- **Effects:** `INVISIBLE` 3s → self
- **Scaling:** —

#### R (4) — ultimate — **Combustion Trap**
*Detonate a hidden charge. Armor-pierce + STUN.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 175 | 1 min 30s | 1500 | fire, aoe_targeted, armor_pierce, ultimate |

- **Effects:** `STUNNED` 2s → enemies hit
- **Scaling:** +22/DEX over 75 → DAMAGE

---

## Ninja  *(classId 13)*

### Passive — **Reflexes**
*On dodge you gain a flash of invisibility.*

_Tags: flavor_

_(flavor passive — no scripted trigger wired yet)_

#### Q (1) — **Star Throw**
*A spinning shuriken volley.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 35 | 2.5s | 180 | fire, aoe_targeted |

- **Effects:** —
- **Scaling:** +7/DEX over 75 → DAMAGE

#### W (2) — **Shadow Dash**
*Burst forward. SPEEDY 4s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 50 | 9s | — | dash_trail, self_buff |

- **Effects:** `SPEEDY` 4s → self
- **Scaling:** —

#### E (3) — **Blade Storm**
*Whirling blades. DAMAGING 5s.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 60 | 14s | — | visual_at_self:13, self_buff |

- **Effects:** `DAMAGING` 5s → self
- **Scaling:** —

#### R (4) — ultimate — **Death Blossom**
*Unleash every blade. Armor-piercing burst.*

| MP | Cooldown | Base damage | Tags |
|---|---|---|---|
| 175 | 1 min 30s | 1600 | fire, aoe_targeted, armor_pierce, ultimate |

- **Effects:** —
- **Scaling:** +24/DEX over 75 → DAMAGE

---
