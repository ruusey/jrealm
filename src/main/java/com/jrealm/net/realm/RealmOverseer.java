package com.jrealm.net.realm;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.jrealm.game.contants.GlobalConstants;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Monster;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.OverworldZone;
import com.jrealm.game.model.TerrainGenerationParameters;
import com.jrealm.net.server.packet.TextPacket;

import lombok.extern.slf4j.Slf4j;

/**
 * Overseer AI that manages a realm's ecosystem:
 * - Monitors enemy populations per zone
 * - Spawns replacement enemies when population drops
 * - Tracks quest/boss enemies and announces kills
 * - Spawns event encounters when bosses die
 * - Broadcasts taunts and announcements
 */
@Slf4j
public class RealmOverseer {
    private static final int CHECK_INTERVAL_TICKS = 640; // ~10 seconds at 64 TPS
    private static final float REPOPULATE_THRESHOLD = 0.5f; // Repopulate when below 50% of target
    private static final float OVERPOPULATE_THRESHOLD = 1.5f; // Cull when above 150%
    private static final double EVENT_SPAWN_CHANCE = 0.25; // 25% chance of event on boss kill

    private final Realm realm;
    private final RealmManagerServer mgr;
    private int tickCounter = 0;
    private int targetPopulation = 0;
    private long lastAnnouncement = 0;

    // Track quest enemies (bosses worth announcing)
    private final Set<Integer> questEnemyIds = new HashSet<>();
    // Track per-player damage for loot credit
    private final Map<Long, Map<Long, Integer>> damageTracker = new ConcurrentHashMap<>();

    // Taunts
    private static final String[] WELCOME_TAUNTS = {
        "Welcome, mortal. You are food for my minions!",
        "Another fool enters my domain...",
        "Your bones will decorate my halls!"
    };

    private static final String[] BOSS_KILL_TAUNTS = {
        "You dare slay my %s? You will pay for this!",
        "My %s has fallen... but more will come!",
        "The death of my %s only fuels my rage!"
    };

    private static final String[] EVENT_SPAWN_TAUNTS = {
        "I have summoned a %s to destroy you!",
        "A %s emerges from the shadows to avenge the fallen!",
        "Feel the wrath of my %s!"
    };

    public RealmOverseer(Realm realm, RealmManagerServer mgr) {
        this.realm = realm;
        this.mgr = mgr;
        initQuestEnemies();
    }

    private void initQuestEnemies() {
        // Mark high-HP enemies as quest enemies (bosses worth tracking)
        for (EnemyModel model : GameDataManager.ENEMIES.values()) {
            if (model.getHealth() >= 2000) {
                questEnemyIds.add(model.getEnemyId());
            }
        }
    }

    /**
     * Called every server tick. Performs periodic ecosystem management.
     */
    public void tick() {
        tickCounter++;
        if (tickCounter % CHECK_INTERVAL_TICKS != 0) return;

        checkPopulation();
    }

    /**
     * Monitor enemy population and repopulate if needed.
     */
    private void checkPopulation() {
        if (targetPopulation == 0) {
            targetPopulation = realm.getEnemies().size();
            if (targetPopulation == 0) targetPopulation = 500; // Default
        }

        int currentPop = realm.getEnemies().size();
        float ratio = (float) currentPop / targetPopulation;

        if (ratio < REPOPULATE_THRESHOLD) {
            int toSpawn = (int) ((targetPopulation * 0.75f) - currentPop);
            spawnReplacements(Math.min(toSpawn, 50)); // Cap at 50 per cycle
            log.debug("[OVERSEER] Population low ({}/{}), spawned {} replacements",
                currentPop, targetPopulation, Math.min(toSpawn, 50));
        }
    }

    /**
     * Spawn replacement enemies in appropriate zones.
     */
    private void spawnReplacements(int count) {
        final TerrainGenerationParameters params = getTerrainParams();
        if (params == null || params.getEnemyGroups() == null) return;

        final boolean hasZones = params.getZones() != null && !params.getZones().isEmpty();
        final Map<Integer, List<EnemyModel>> enemiesByGroup = new HashMap<>();
        for (var group : params.getEnemyGroups()) {
            List<EnemyModel> models = new ArrayList<>();
            for (int enemyId : group.getEnemyIds()) {
                EnemyModel m = GameDataManager.ENEMIES.get(enemyId);
                if (m != null) models.add(m);
            }
            enemiesByGroup.put(group.getOrdinal(), models);
        }

        final List<EnemyModel> defaultList = enemiesByGroup.values().iterator().next();
        final int tileSize = realm.getTileManager().getMapLayers().get(0).getTileSize();

        for (int i = 0; i < count; i++) {
            Vector2f spawnPos = realm.getTileManager().getSafePosition();
            if (spawnPos == null) continue;

            List<EnemyModel> spawnList = defaultList;
            int healthMult = realm.getDifficultyMultiplier();

            if (hasZones) {
                OverworldZone zone = realm.getTileManager().getZoneForPosition(spawnPos.x, spawnPos.y);
                if (zone != null) {
                    spawnList = enemiesByGroup.getOrDefault(zone.getEnemyGroupOrdinal(), defaultList);
                    healthMult = Math.max(1, zone.getDifficulty());
                }
            }

            if (spawnList.isEmpty()) continue;
            EnemyModel toSpawn = spawnList.get(Realm.RANDOM.nextInt(spawnList.size()));

            Enemy enemy = new Monster(Realm.RANDOM.nextLong(), toSpawn.getEnemyId(),
                spawnPos.clone(), toSpawn.getSize(), toSpawn.getAttackId());
            enemy.setSpriteSheet(GameSpriteManager.getSpriteSheet(toSpawn));
            enemy.setHealth(enemy.getHealth() * healthMult);
            enemy.setHealthMultiplier(healthMult);
            enemy.getStats().setHp((short) (enemy.getStats().getHp() * healthMult));
            enemy.setPos(spawnPos);
            realm.addEnemy(enemy);
        }
    }

    /**
     * Called when an enemy dies. Handles quest tracking and event spawning.
     */
    public void onEnemyKilled(Enemy enemy, long killerPlayerId) {
        EnemyModel model = GameDataManager.ENEMIES.get(enemy.getEnemyId());
        if (model == null) return;

        // Check if this was a quest enemy (boss)
        if (questEnemyIds.contains(enemy.getEnemyId())) {
            // Announce the kill
            String killerName = getPlayerName(killerPlayerId);
            broadcastTaunt(String.format(
                randomChoice(BOSS_KILL_TAUNTS), model.getName()));

            // Chance to spawn event encounter
            if (Realm.RANDOM.nextDouble() < EVENT_SPAWN_CHANCE) {
                spawnEventEncounter(model);
            }
        }
    }

    /**
     * Spawn a special event encounter in response to a boss kill.
     */
    private void spawnEventEncounter(EnemyModel killedBoss) {
        // Pick a random boss-tier enemy to spawn as an event
        List<EnemyModel> bossEnemies = new ArrayList<>();
        for (EnemyModel m : GameDataManager.ENEMIES.values()) {
            if (m.getHealth() >= 3000 && m.getEnemyId() != killedBoss.getEnemyId()) {
                bossEnemies.add(m);
            }
        }
        if (bossEnemies.isEmpty()) return;

        EnemyModel eventBoss = bossEnemies.get(Realm.RANDOM.nextInt(bossEnemies.size()));
        Vector2f spawnPos = realm.getTileManager().getSafePosition();
        if (spawnPos == null) return;

        // Spawn with 2x health multiplier for event difficulty
        int healthMult = realm.getDifficultyMultiplier() * 2;
        Enemy boss = new Monster(Realm.RANDOM.nextLong(), eventBoss.getEnemyId(),
            spawnPos.clone(), eventBoss.getSize(), eventBoss.getAttackId());
        boss.setSpriteSheet(GameSpriteManager.getSpriteSheet(eventBoss));
        boss.setHealth(boss.getHealth() * healthMult);
        boss.setHealthMultiplier(healthMult);
        boss.getStats().setHp((short) (boss.getStats().getHp() * healthMult));
        boss.setPos(spawnPos);
        realm.addEnemy(boss);

        broadcastTaunt(String.format(randomChoice(EVENT_SPAWN_TAUNTS), eventBoss.getName()));
        log.info("[OVERSEER] Spawned event encounter: {} at ({}, {})",
            eventBoss.getName(), spawnPos.x, spawnPos.y);
    }

    /**
     * Track damage dealt by a player to an enemy for loot credit.
     */
    public void trackDamage(long enemyId, long playerId, int damage) {
        damageTracker.computeIfAbsent(enemyId, k -> new ConcurrentHashMap<>())
            .merge(playerId, damage, Integer::sum);
    }

    /**
     * Get the player who dealt the most damage to an enemy.
     * Returns -1 if no damage tracked.
     */
    public long getTopDamageDealer(long enemyId) {
        Map<Long, Integer> dmgMap = damageTracker.get(enemyId);
        if (dmgMap == null || dmgMap.isEmpty()) return -1;
        return dmgMap.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(-1L);
    }

    /**
     * Check if a player dealt enough damage to qualify for loot.
     * Threshold: must deal at least 10% of total damage.
     */
    public boolean qualifiesForLoot(long enemyId, long playerId) {
        Map<Long, Integer> dmgMap = damageTracker.get(enemyId);
        if (dmgMap == null) return true; // No tracking = everyone qualifies
        int playerDmg = dmgMap.getOrDefault(playerId, 0);
        int totalDmg = dmgMap.values().stream().mapToInt(Integer::intValue).sum();
        if (totalDmg == 0) return true;
        return (float) playerDmg / totalDmg >= 0.1f; // 10% threshold
    }

    /**
     * Clean up damage tracking for a dead enemy.
     */
    public void clearDamageTracking(long enemyId) {
        damageTracker.remove(enemyId);
    }

    /**
     * Send a welcome message to a player joining this realm.
     */
    public void welcomePlayer(com.jrealm.game.entity.Player player) {
        String taunt = randomChoice(WELCOME_TAUNTS);
        try {
            mgr.enqueueServerPacket(player,
                TextPacket.create("Overseer", player.getName(), taunt));
        } catch (Exception e) {
            log.error("[OVERSEER] Failed to send welcome: {}", e.getMessage());
        }
    }

    /**
     * Broadcast a taunt to all players in the realm.
     */
    private void broadcastTaunt(String message) {
        long now = Instant.now().toEpochMilli();
        if (now - lastAnnouncement < 3000) return; // Throttle: max 1 every 3 seconds
        lastAnnouncement = now;

        for (var player : realm.getPlayers().values()) {
            try {
                mgr.enqueueServerPacket(player,
                    TextPacket.create("Overseer", player.getName(), message));
            } catch (Exception e) {
                // Skip failed sends
            }
        }
    }

    private String getPlayerName(long playerId) {
        var player = realm.getPlayers().get(playerId);
        return player != null ? player.getName() : "Unknown";
    }

    private TerrainGenerationParameters getTerrainParams() {
        if (GameDataManager.TERRAINS == null) return null;
        var mapModel = GameDataManager.MAPS.get(realm.getMapId());
        if (mapModel != null && mapModel.getTerrainId() >= 0) {
            return GameDataManager.TERRAINS.get(mapModel.getTerrainId());
        }
        return GameDataManager.TERRAINS.get(0);
    }

    private String randomChoice(String[] arr) {
        return arr[Realm.RANDOM.nextInt(arr.length)];
    }
}
