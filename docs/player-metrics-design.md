# Player Lifetime Metrics — Design

Per-character lifetime data collection. Tracks every meaningful action a
character performs across their entire existence, queryable as a report.

## Goals

- **Per character, not per account.** Each character has its own lifetime
  metrics document. Account-level rollups can come later as an aggregation
  on top.
- **Lossless during normal play.** Deltas are batched and flushed
  frequently; a crash can lose at most one flush window of activity (~12s),
  matching the existing character persistence cadence.
- **Hot-path safe.** Recording an event is a single in-memory increment.
  No HTTP, no DB round-trip on the gameplay thread.
- **Extensible.** New counters can be added without breaking existing
  documents (Mongo $inc on a missing field defaults to 0).

## Non-goals (v1)

- Event-sourced log (one row per event). The volume at scale is too high
  for v1; aggregations like "first time you killed enemy X" stay deferred.
- PvP metrics. No PvP mechanic exists today.
- Time-windowed rollups (last-7-days, etc.). Counters are all-time only.
- Cross-character account totals. A separate aggregation can fold these in
  later.

## Event taxonomy

The set of events we record, grouped:

### Combat
- `projectilesFired` — every bullet the player launches (basic + ability)
- `projectilesHit` — bullets that connected with an entity
- `projectilesExpired` — bullets that timed out without hitting anything
  (≈ "missed" — derived as `fired − hit − expired_in_flight`)
- `damageDealtTotal` — sum of all damage applied to enemies
- `damageTakenTotal` — sum of all damage taken
- `deaths` — character death count (relevant for non-permadeath modes)
- `killsTotal` — kill count credited to this player
- `killsByEnemyId` — `Map<enemyId, count>` for "X kobolds killed"
- `bossKills` — count of kills against enemies flagged as bosses

### Items
- `hpPotionsDrank` / `mpPotionsDrank` — separate so we can see playstyle
- `itemsPickedUp` — every loot pull
- `itemsConsumedByItemId` — `Map<itemId, count>` for any consumable
- `itemsTradedSent` / `itemsTradedReceived` — `Map<itemId, count>`
- `goldEarned` — if/when currency lands

### Progression
- `xpEarned` — total XP ever gained
- `xpFromKills` — narrower bucket; nice for "killer vs. quester" reads
- `levelsGained` — derived from XP but useful to record discretely so
  level-up events can populate other counters in future
- `skillPointsEarned` — total SP awarded
- `skillPointsSpentByAbility` — `Map<abilityId, count>`

### Abilities
- `castsStartedByAbility` — `Map<abilityId, count>` (count of cast-time gates entered)
- `castsCompletedByAbility` — `Map<abilityId, count>` (count of completions)
- `castsCanceledByAbility` — cancellations (death mid-cast, etc.)
- `damageByAbility` — `Map<abilityId, sum>` (requires bullet→ability provenance, see Phase 2)

### Social
- `tradesCompleted` — count of successful trades
- `tradePartners` — `Map<otherCharacterUuid, count>` so "I traded with X 12 times"
- `partyInvitesSent`, `partyInvitesAccepted`, `partyInvitesDeclined`
- `partyKills` — kills shared while in a party (subset of `killsTotal`)
- `chatMessagesSent`

### Movement / world
- `tilesTraveled` — accumulated euclidean distance, divided by tile size
- `portalsUsed`
- `realmsEntered` — `Map<realmTypeId, count>`
- `dungeonsCleared` — count of dungeons fully completed (boss kill marker)

### Time
- `firstSeenAt` — epoch ms of character creation
- `lastSeenAt` — epoch ms of most recent activity
- `totalPlayMs` — accumulated active session ms (capped per logout event)
- `sessionsCount` — login count

### Meta
- `schemaVersion` — bumped when we change the shape; migrations are
  forward-only via $set defaults.
- `updatedAt` — epoch ms of last flush

## Architecture

```
┌───────────────────────────┐     ┌──────────────────────────────┐
│  Game server (openrealm)  │     │ Data service (openrealm-data)│
│                           │     │                              │
│  ┌─────────────────────┐  │     │  ┌─────────────────────────┐ │
│  │  PlayerMetrics      │  │HTTP │  │ CharacterMetricsService │ │
│  │  (per Player,       │──┼────▶│  │  Mongo $inc updates     │ │
│  │   in-memory longs)  │  │POST │  └─────────────────────────┘ │
│  └─────────────────────┘  │     │              │               │
│           ▲               │     │              ▼               │
│           │ event hooks   │     │   ┌──────────────────────┐   │
│   ┌───────┴────────┐      │     │   │ player_character_    │   │
│   │ enemyDeath()   │      │     │   │ metrics  (Mongo doc) │   │
│   │ useAbility()   │      │     │   └──────────────────────┘   │
│   │ Bullet damage  │      │     │                              │
│   │ Potion consume │      │     │              ▲               │
│   │ Trade complete │      │     │              │ GET           │
│   └────────────────┘      │     │   ┌──────────┴───────────┐   │
│                           │     │   │ /metrics/{charUuid}  │   │
└───────────────────────────┘     │   │ → report DTO         │   │
                                  │   └──────────────────────┘   │
                                  └──────────────────────────────┘
```

### Server side (openrealm)

`com.openrealm.game.metrics.PlayerMetrics`:
- Held on `Player`, lazy-allocated on first event.
- Stores **deltas** since last flush, not absolute lifetime totals. The
  data service holds the lifetime running total; the game server is
  stateless between flushes.
- All increments via `LongAdder` so they're concurrent-safe (bullet
  damage application runs from the bullet update thread; ability cast
  from the realm tick; we don't want lost updates).
- Mutators: `recordKill(short enemyId, boolean isBoss)`,
  `recordProjectileFired()`, `recordProjectileHit()`,
  `recordPotion(boolean isHp)`, `recordXpEarned(long amount, boolean fromKill)`,
  `recordCastStart(int abilityId)`, `recordCastComplete(int abilityId)`,
  `recordTrade(String otherCharUuid)`, ...
- `MetricsDelta drainAndReset()` — returns the accumulated deltas as a
  POJO suitable for serialization, zeros all counters. Called by the
  flush scheduler.

Flush points:
1. **Periodic** — the existing character-persistence tick (~12s in
   `persistRealm`). When we save the character, we also drain the
   metrics delta and POST it.
2. **On disconnect** — final flush before the player slot is freed.
3. **On death** — flush before the death-ack roundtrip so the death
   contributes to lifetime totals.
4. **On server shutdown** — best-effort flush in the shutdown hook.

### Data service side (openrealm-data)

New Mongo collection: `player_character_metrics`. One document per
character, keyed by `characterUuid`.

`CharacterMetricsEntity`:
- All the counter fields above
- Map fields stored as Mongo embedded documents

`CharacterMetricsRepository extends MongoRepository`:
- `findByCharacterUuid(String)`

`CharacterMetricsService`:
- `applyDelta(String characterUuid, MetricsDelta delta)` — translates
  delta into a single Mongo `update` with `$inc` for scalar counters and
  `$inc` for each map entry under `mapField.<key>`.
- Idempotent against partial failures: $inc is per-field atomic; a
  retry just adds the delta again, so we must not retry blindly. The
  game server's flush returns success/failure synchronously; on
  failure the delta stays in the in-memory PlayerMetrics for next
  flush (no double-count).
- `getReport(String characterUuid)` — read the entire document, return
  as DTO for the UI.

`CharacterMetricsController`:
- `POST /data/account/character/{uuid}/metrics/delta` — admin-restricted,
  takes a `MetricsDelta`, applies it.
- `GET /data/account/character/{uuid}/metrics` — owner or admin, returns
  full lifetime report DTO.

### Webclient / native client report UI

Deferred to a follow-up. The data is captured first; the UI consumes it
later. A simple "Stats" tab on the character select screen and / or
pause menu is the natural fit — shows totals + map breakdowns sorted by
count.

## Concurrency model

- All event recording on the game server is single-writer per character
  (the realm tick thread + the bullet thread, which we treat as
  effectively co-located via `LongAdder`).
- Flush from the persistence thread reads via `LongAdder.sumThenReset()`.
- The data service applies deltas under Mongo's per-field atomicity;
  multiple concurrent deltas for the same character (e.g. shard +
  retry) compose correctly.

## Failure model

- **Game server crash mid-window**: lose up to one flush window of
  events (~12s). Acceptable; no corruption.
- **Data service down during flush**: HTTP call fails, delta stays in
  `PlayerMetrics`, accumulates with next window's events. Same retry
  semantics as character persistence today.
- **Mongo down**: data service returns 5xx, same as above.
- **Schema drift**: documents missing new fields default to 0 on read.
  No migration needed for additive changes.

## Phase plan

### Phase 1 — MVP (this overnight session)
- Add `PlayerMetrics` class + `MetricsDelta` POJO on game server.
- Add field to `Player`.
- Wire 5-6 high-value event hooks: kills (by enemy id), deaths, xp
  earned, potions, ability casts started + completed.
- Add data service collection + entity + repo + service + controller.
- Add HTTP call from game server persistence flow → `applyDelta`.
- Compile-green end-to-end. Live data accumulates on test cast.

### Phase 2 — full coverage
- Wire remaining hooks: projectiles fired/hit, damage dealt/taken,
  trades, items consumed, party stats, chat, movement, portals,
  dungeons cleared.
- Connect bullet damage → ability provenance (track which ability
  spawned each bullet so per-ability damage works).

### Phase 3 — UI
- Webclient "Stats" tab on character select.
- Native client equivalent in PauseState.
- Sortable breakdowns (kills by enemy id, items consumed, etc.).

### Phase 4 — analytics extras
- Account-level rollup (sum metrics across all chars on an account).
- Server-wide leaderboards (most kills of enemy X, most potions drank).
- Optional event log for select milestones (first kill of a boss, first
  level 20, etc.).

## Open questions

- **Permadeath wipe**: when a character dies in permadeath, do we wipe
  the metrics or keep them as a memorial? Default: keep. UI marks
  character as deceased.
- **Bot / headless characters**: do we record metrics for headless test
  bots? Default: yes, but the report UI hides them.
- **Backfill**: do existing characters get a backfilled metrics doc
  from current XP / level? Default: no. Counters start at 0 from the
  first event after deploy.
