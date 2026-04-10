# OpenRealm

### A Multiplayer 8-Bit Bullet Hell Dungeon Crawler — Play Now at [OpenRealm.Net](http://openrealm.net/)

<div>
    <img src="https://github.com/ruusey/openrealm/blob/main/banner.png" width="100%">
    <img src="https://github.com/ruusey/openrealm/blob/main/MobileView.png" width="100%">
</div>

---

## About

**OpenRealm** is a real-time multiplayer bullet hell dungeon crawler inspired by classic 8-bit action RPGs. Players explore a persistent overworld, fight through procedurally generated dungeons, collect loot, and battle bosses — all rendered in pixel art and playable directly in the browser or via a native Java client.

The game features a server-authoritative architecture with client-side prediction, supporting dozens of concurrent players with responsive movement even at higher latencies. The server runs at a fixed 64Hz tick rate with deterministic physics, ensuring fair and consistent gameplay for all players.

### Key Features

- **Browser & Desktop Play** — Full-featured HTML5 web client (PixiJS) with mobile touch controls, plus a native Java desktop client (LibGDX)
- **Persistent World** — Shared overworld with four biome zones of increasing difficulty, from the Beach to the Summit
- **Procedural Dungeons** — Over 20 unique dungeon types with generated room layouts, corridor systems, and boss encounters
- **13 Character Classes** — Each with unique abilities, stat progressions, and equipment types
- **129+ Enemy Types** — From beach crabs to realm gods, with multi-phase AI, burst patterns, and status effects
- **Real-Time Combat** — Dodge hundreds of projectiles with precise movement, fire weapons with client-predicted instant feedback
- **Permadeath & Loot** — Characters die permanently unless equipped with an Amulet of Resurrection. Tiered loot drops from enemies and dungeon bosses
- **Vault Storage** — Personal vault accessible from the Nexus hub for storing items across characters
- **Trading System** — Player-to-player item trading with confirmation UI
- **Guest Accounts** — Play instantly without registration; upgrade to a full account anytime
- **Game Data Editor** — Full-featured web-based editor for tiles, enemies, items, animations, maps, projectiles, portals, loot tables, and more
- **Projectile Simulator** — Interactive canvas playground for testing and visualizing projectile patterns and enemy attack behaviors

---

## Architecture

OpenRealm consists of two services:

| Service | Description | Stack |
|---------|-------------|-------|
| **openrealm** (this repo) | Game server + desktop client | Java 11+, LibGDX, WebSocket (NIO) |
| **[openrealm-data](https://github.com/ruusey/openrealm-data)** | Data service, REST API, web client, game editor | Java 11+, Spring Boot, MongoDB, PixiJS |

### Networking

- **Server tick rate**: 64Hz with fixed-timestep simulation
- **Client prediction**: Sequence-numbered input commands (64Hz) with server reconciliation
- **Entity interpolation**: Smooth rendering of remote players and enemies between server snapshots
- **Packet transport**: Binary WebSocket protocol with 26+ packet types
- **Bandwidth**: ~2-3 KB/s per player during active gameplay

---

## Getting Started

### Prerequisites

- Java JDK 11+
- Apache Maven 3.8.3+
- MongoDB Server (for the data service)

### Running the Data Service

```bash
cd openrealm-data
mvn clean package
java -jar target/openrealm-data-{version}.jar
```

The data service starts on port 80 by default and provides:
- REST API for account management and game data
- Web client at `/game-data/webclient/index.html`
- Game data editor at `/game-data/editor/index.html`
- Static sprite sheet serving

### Running the Game Server

```bash
cd openrealm
mvn clean package
java -jar target/openrealm-{version}.jar -server {DATA_SERVER_ADDR}
```

The game server listens on:
- Port 2222 — Native TCP client connections
- Port 2223 — WebSocket connections (browser client)

### Running the Desktop Client

```bash
java -jar target/openrealm-{version}.jar -client {DATA_SERVER_ADDR}
```

---

## Controls

### Keyboard (Desktop & Browser)

| Key | Action |
|-----|--------|
| **W/A/S/D** | Move Up/Left/Down/Right |
| **Left Click** | Shoot / Pick up loot |
| **Right Click** | Use ability |
| **1-8** | Use inventory slot |
| **R / F1** | Return to Nexus |
| **F / F2 / Space** | Use nearest portal |
| **Enter** | Open chat / send message |
| **Escape** | Return to character select |

### Mobile (Touch)

- **Left joystick** — Movement
- **Right joystick** — Aim and shoot
- **Vault button** — Access vault storage
- **Double-tap** — Use ability at tap location

---

## Server Commands

Commands are entered in chat with a `/` prefix. Some require admin privileges.

| Command | Description | Access |
|---------|-------------|--------|
| `/pos` | Show current world position | All |
| `/help` | List available commands | All |
| `/about` | Server information | All |
| `/stat max` | Max all stats | Admin |
| `/stat {name} {value}` | Set a specific stat | Admin |
| `/spawn {enemyId}` | Spawn an enemy | Moderator |
| `/item {itemId} [count]` | Spawn an item | Admin |
| `/portal {mapName}` | Open a portal | Admin |
| `/tp {name or x,y}` | Teleport | Admin |
| `/op {playerName}` | Promote/demote operator | Sys Admin |
| `/godmode` | Toggle invincibility | Admin |
| `/heal` | Restore HP and MP | Admin |

---

## Scripting & Extensibility

OpenRealm provides a reflection-based scripting system for extending game behavior without modifying core code. Scripts are discovered automatically at runtime.

### Enemy Scripts

Custom attack patterns for specific enemies. Extend `EnemyScriptBase` and implement `attack()`.

### Item Scripts

Custom item use and ability behavior. Extend `UseableItemScriptBase` and implement `invokeUseItem()` / `invokeItemAbility()`.

### Terrain Decorators

Post-processing hooks for realm generation. Extend `RealmDecoratorBase` and implement `decorate()` to modify tiles, spawn structures, or place entities.

### Packet Handlers

Register custom packet handlers via annotation (`@PacketHandlerServer`) or direct callback registration for both server and client.

### Command Handlers

Register custom chat commands via `@CommandHandler` annotation with optional `@AdminRestrictedCommand` access control.

---

## Game Data

All game content is defined in JSON files served by the data service. Edit them directly or use the built-in web editor at `/game-data/editor/index.html`.

| File | Contents |
|------|----------|
| `character-classes.json` | Class definitions, base/max stats, starting equipment |
| `enemies.json` | Enemy stats, sprites, phase behaviors, attack patterns |
| `game-items.json` | Weapons, armor, abilities, consumables, stat bonuses |
| `projectile-groups.json` | Bullet patterns — speed, range, amplitude, flags |
| `animations.json` | Character sprite animations (idle, walk, attack) |
| `maps.json` | Static and procedural map definitions with tile layers |
| `terrains.json` | Procedural terrain parameters, biome zones, enemy groups |
| `tiles.json` | Tile definitions — collision, slowing, damaging flags |
| `portals.json` | Portal types and dungeon graph routing |
| `dungeon-graph.json` | Dungeon node graph — shared/instanced realms |
| `loot-groups.json` | Item drop groupings by tier |
| `loot-tables.json` | Per-enemy drop tables with probabilities |

---

## License

Copyright (c) 2024-2026 Robert Usey. All rights reserved.

This software and associated documentation files (the "Software") are the exclusive property of Robert Usey. No part of this Software may be copied, modified, merged, published, distributed, sublicensed, sold, or otherwise made available to any third party, in whole or in part, in any form or by any means, without the prior express written permission of the copyright holder.

Unauthorized reproduction or distribution of this Software, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible under the law.

For licensing inquiries, contact: **ruusey@gmail.com**
