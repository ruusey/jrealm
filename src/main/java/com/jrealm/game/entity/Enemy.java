package com.jrealm.game.entity;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.jrealm.game.contants.ProjectileEffectType;
import com.jrealm.game.contants.ProjectilePositionMode;
import com.jrealm.game.entity.Bullet;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.AttackPattern;
import com.jrealm.game.model.EnemyModel;
import com.jrealm.game.model.EnemyPhase;
import com.jrealm.game.model.MovementPattern;
import com.jrealm.game.model.Projectile;
import com.jrealm.game.model.ProjectileGroup;
import com.jrealm.game.script.EnemyScriptBase;
import com.jrealm.game.state.PlayState;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerClient;
import com.jrealm.net.realm.RealmManagerServer;
import com.jrealm.util.WorkerThread;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class Enemy extends Entity {
    private static final int IDLE_FRAMES = 10;
    private static final float CHASE_SPEED = 1.4f;
    protected EnemyModel model;
    protected int chaseRange;
    protected int attackRange;
    protected int xOffset;
    protected int yOffset;

    private long lastShotTick = 0;
    private int enemyId;
    private int weaponId = -1;
    private int idleTime = 0;
    private int healthMultiplier = 1;
    private Stats stats;

    // Phase-based state
    private float orbitAngle = 0f;
    private boolean charging = false;
    private long chargePauseUntil = 0;
    private Vector2f spawnPos = null;
    private long[] attackCooldowns = null;

    public Enemy() {
        super(0, null, 0);
    }

    public Enemy(long id, int enemyId, Vector2f origin, int size, int weaponId) {
        super(id, origin, size);
        this.model = GameDataManager.ENEMIES.get(enemyId);
        this.enemyId = enemyId;
        this.weaponId = weaponId;
        this.stats = this.model.getStats().clone();
        this.health = stats.getHp();
        this.mana = stats.getMp();
        if (origin != null) {
            this.spawnPos = origin.clone();
        }
    }

    public void applyStats(Stats stats) {
        this.health = stats.getHp();
        this.mana = stats.getMp();
        this.stats.setHp(stats.getHp());
        this.stats.setMp(stats.getMp());
        this.stats.setDef(stats.getDef());
        this.stats.setAtt(stats.getAtt());
        this.stats.setSpd(stats.getSpd());
        this.stats.setDex(stats.getDex());
        this.stats.setVit(stats.getVit());
        this.stats.setWis(stats.getWis());
    }

    @Override
    public int getHealth() {
        return this.health;
    }

    @Override
    public int getMana() {
        return this.mana;
    }

    public int getMaxHealth() {
        return (this.stats != null) ? this.stats.getHp() : this.health;
    }

    public void applyUpdate(UpdatePacket packet, PlayState state) {
        this.name = packet.getPlayerName();
        this.stats = packet.getStats().asStats();
        this.health = packet.getHealth();
        this.mana = packet.getMana();
        this.setEffectIds(packet.getEffectIds());
        this.setEffectTimes(packet.getEffectTimes());
        if (this.stats != null && this.stats.getHp() > 0) {
            this.healthpercent = (float) this.health / (float) this.stats.getHp();
        }
    }

    // ========== PHASE RESOLUTION ==========

    private EnemyPhase getActivePhase() {
        List<EnemyPhase> phases = this.model.getPhases();
        if (phases == null || phases.isEmpty()) return null;
        float hpPct = (this.stats.getHp() > 0) ? (float) this.health / (float) this.stats.getHp() : 0f;
        // Phases ordered by hpThreshold descending in JSON. Active = first phase where hpPct <= threshold
        for (int i = phases.size() - 1; i >= 0; i--) {
            if (hpPct <= phases.get(i).getHpThreshold()) {
                return phases.get(i);
            }
        }
        return phases.get(0);
    }

    private float getPhaseSpeed(EnemyPhase phase) {
        if (phase != null && phase.getMovement() != null) {
            return phase.getMovement().getSpeed();
        }
        return (this.model.getMaxSpeed() > 0) ? this.model.getMaxSpeed() : CHASE_SPEED;
    }

    // ========== MOVEMENT PATTERNS ==========

    private void applyMovement(Player player, EnemyPhase phase) {
        if (player == null || player.hasEffect(ProjectileEffectType.INVISIBLE)) {
            this.dx = 0;
            this.dy = 0;
            this.up = false;
            this.down = false;
            this.left = false;
            this.right = false;
            return;
        }

        MovementPattern mv = (phase != null) ? phase.getMovement() : null;
        String type = (mv != null) ? mv.getType() : "CHASE";
        float speed = getPhaseSpeed(phase);
        float dist = this.pos.distanceTo(player.pos);

        switch (type) {
            case "ORBIT":
                moveOrbit(player, speed, mv.getRadius(), mv.getDirection());
                break;
            case "STRAFE":
                moveStrafe(player, speed, mv.getPreferredRange());
                break;
            case "CHARGE":
                moveCharge(player, speed, mv.getChargeDistanceMin(), mv.getPauseMs());
                break;
            case "FLEE":
                moveFlee(player, speed, mv.getFleeRange());
                break;
            case "WANDER":
                moveWander(speed);
                break;
            case "ANCHOR":
                moveAnchor(player, speed, mv.getAnchorRadius());
                break;
            case "FIGURE_EIGHT":
                moveFigureEight(player, speed, mv.getRadius());
                break;
            case "CHASE":
            default:
                moveChase(player, speed);
                break;
        }
    }

    private void moveChase(Player player, float speed) {
        float dist = this.pos.distanceTo(player.pos);
        if (dist < this.chaseRange && dist >= this.attackRange) {
            // Add slight zigzag — offset angle by a wobble so enemies don't beeline
            float angle = (float) Math.atan2(player.pos.y - this.pos.y, player.pos.x - this.pos.x);
            float wobble = (float) Math.sin(this.orbitAngle * 3) * 0.4f;
            this.orbitAngle += 0.05f;
            angle += wobble;
            this.dx = (float) Math.cos(angle) * speed;
            this.dy = (float) Math.sin(angle) * speed;
            updateDirectionFlags();
        } else if (dist < this.attackRange) {
            // In attack range — circle slowly instead of standing still
            float angle = (float) Math.atan2(this.pos.y - player.pos.y, this.pos.x - player.pos.x);
            float tangent = angle + (float) (Math.PI / 2);
            this.dx = (float) Math.cos(tangent) * speed * 0.3f;
            this.dy = (float) Math.sin(tangent) * speed * 0.3f;
            updateDirectionFlags();
        }
    }

    private void moveOrbit(Player player, float speed, float radius, String dir) {
        float dist = this.pos.distanceTo(player.pos);
        boolean cw = "CW".equalsIgnoreCase(dir);

        if (Math.abs(dist - radius) > 10) {
            // Move toward/away from orbit radius
            if (dist > radius) {
                setDirectionToward(player.pos, speed);
            } else {
                setDirectionAway(player.pos, speed);
            }
        }

        // Tangential movement
        float angleToPlayer = (float) Math.atan2(this.pos.y - player.pos.y, this.pos.x - player.pos.x);
        float tangentAngle = cw ? (angleToPlayer + (float) (Math.PI / 2)) : (angleToPlayer - (float) (Math.PI / 2));
        float tangentX = (float) Math.cos(tangentAngle) * speed;
        float tangentY = (float) Math.sin(tangentAngle) * speed;

        // Blend: mostly tangent, some radial correction
        float radialWeight = Math.min(1f, Math.abs(dist - radius) / 30f);
        this.dx = this.dx * radialWeight + tangentX * (1f - radialWeight);
        this.dy = this.dy * radialWeight + tangentY * (1f - radialWeight);
        updateDirectionFlags();
    }

    private void moveStrafe(Player player, float speed, float preferredRange) {
        float dist = this.pos.distanceTo(player.pos);

        // Maintain preferred range
        if (dist > preferredRange + 20) {
            setDirectionToward(player.pos, speed * 0.6f);
        } else if (dist < preferredRange - 20) {
            setDirectionAway(player.pos, speed * 0.6f);
        } else {
            // At preferred range — strafe with periodic direction reversal
            float angle = (float) Math.atan2(this.pos.y - player.pos.y, this.pos.x - player.pos.x);
            // Reverse strafe direction every ~3 seconds
            boolean strafeRight = ((int)(this.orbitAngle / 3.0f) % 2) == 0;
            float strafeAngle = angle + (strafeRight ? (float)(Math.PI / 2) : (float)(-Math.PI / 2));
            this.orbitAngle += 0.016f; // ~1 second per unit
            this.dx = (float) Math.cos(strafeAngle) * speed;
            this.dy = (float) Math.sin(strafeAngle) * speed;
            updateDirectionFlags();
        }
    }

    private void moveCharge(Player player, float speed, float chargeDistMin, int pauseMs) {
        long now = System.currentTimeMillis();
        if (now < this.chargePauseUntil) {
            this.dx = 0;
            this.dy = 0;
            return;
        }

        float dist = this.pos.distanceTo(player.pos);
        if (!this.charging && dist < this.chaseRange) {
            this.charging = true;
        }

        if (this.charging) {
            setDirectionToward(player.pos, speed * 2f);
            if (dist < chargeDistMin) {
                this.charging = false;
                this.chargePauseUntil = now + pauseMs;
                this.dx = 0;
                this.dy = 0;
            }
        }
    }

    private void moveFlee(Player player, float speed, float fleeRange) {
        float dist = this.pos.distanceTo(player.pos);
        if (dist < fleeRange) {
            setDirectionAway(player.pos, speed);
        } else if (dist < this.chaseRange && dist >= this.attackRange) {
            setDirectionToward(player.pos, speed * 0.5f);
        }
    }

    private void moveWander(float speed) {
        // Change direction at random intervals (30-90 ticks) for natural feel
        int wanderInterval = 30 + (int)(Math.abs(this.getId()) % 60);
        if (this.idleTime >= wanderInterval) {
            float angle = Realm.RANDOM.nextFloat() * (float) (Math.PI * 2);
            this.dx = (float) Math.cos(angle) * speed;
            this.dy = (float) Math.sin(angle) * speed;
            updateDirectionFlags();
            this.idleTime = 0;
        } else {
            this.idleTime++;
            // Slight curve to current path (not perfectly straight)
            float curve = (float) Math.sin(this.idleTime * 0.1f) * speed * 0.15f;
            this.dx += curve * 0.1f;
            updateDirectionFlags();
        }
    }

    private void moveAnchor(Player player, float speed, float anchorRadius) {
        if (this.spawnPos == null) {
            this.spawnPos = this.pos.clone();
        }
        float distFromSpawn = this.pos.distanceTo(this.spawnPos);
        float distToPlayer = this.pos.distanceTo(player.pos);

        if (distFromSpawn > anchorRadius) {
            // Too far from spawn — return home
            setDirectionToward(this.spawnPos, speed * 1.2f);
        } else if (distToPlayer < this.attackRange * 1.5f) {
            // Player is close — orbit around spawn point while attacking
            float angleFromSpawn = (float) Math.atan2(this.pos.y - this.spawnPos.y, this.pos.x - this.spawnPos.x);
            this.orbitAngle += speed * 0.015f;
            float targetX = this.spawnPos.x + (float) Math.cos(angleFromSpawn + 0.05f) * anchorRadius * 0.6f;
            float targetY = this.spawnPos.y + (float) Math.sin(angleFromSpawn + 0.05f) * anchorRadius * 0.6f;
            float ddx = targetX - this.pos.x, ddy = targetY - this.pos.y;
            float len = (float) Math.sqrt(ddx * ddx + ddy * ddy);
            if (len > 1) { this.dx = (ddx / len) * speed; this.dy = (ddy / len) * speed; }
            updateDirectionFlags();
        } else if (distToPlayer < this.chaseRange) {
            // Player in chase range — approach but not past anchor boundary
            setDirectionToward(player.pos, speed * 0.7f);
        } else {
            moveWander(speed * 0.4f);
        }
    }

    private void moveFigureEight(Player player, float speed, float radius) {
        this.orbitAngle += speed * 0.02f;
        float fig8X = (float) (Math.sin(this.orbitAngle) * radius);
        float fig8Y = (float) (Math.sin(this.orbitAngle * 2) * radius * 0.5f);

        float targetX = player.pos.x + fig8X;
        float targetY = player.pos.y + fig8Y;

        float ddx = targetX - this.pos.x;
        float ddy = targetY - this.pos.y;
        float len = (float) Math.sqrt(ddx * ddx + ddy * ddy);
        if (len > 0) {
            this.dx = (ddx / len) * speed;
            this.dy = (ddy / len) * speed;
        }
        updateDirectionFlags();
    }

    // ========== MOVEMENT HELPERS ==========

    private void setDirectionToward(Vector2f target, float speed) {
        float ddx = target.x - this.pos.x;
        float ddy = target.y - this.pos.y;
        float len = (float) Math.sqrt(ddx * ddx + ddy * ddy);
        if (len > 0) {
            this.dx = (ddx / len) * speed;
            this.dy = (ddy / len) * speed;
        }
        updateDirectionFlags();
    }

    private void setDirectionAway(Vector2f target, float speed) {
        float ddx = this.pos.x - target.x;
        float ddy = this.pos.y - target.y;
        float len = (float) Math.sqrt(ddx * ddx + ddy * ddy);
        if (len > 0) {
            this.dx = (ddx / len) * speed;
            this.dy = (ddy / len) * speed;
        }
        updateDirectionFlags();
    }

    private void updateDirectionFlags() {
        this.up = this.dy < -0.1f;
        this.down = this.dy > 0.1f;
        this.left = this.dx < -0.1f;
        this.right = this.dx > 0.1f;
    }

    // ========== ATTACK PATTERNS ==========

    private void processAttacks(Player player, EnemyPhase phase, RealmManagerServer mgr, Realm targetRealm) {
        if (player.hasEffect(ProjectileEffectType.INVISIBLE)) return;
        if (this.hasEffect(ProjectileEffectType.STUNNED)) return;

        float dist = this.pos.distanceTo(player.pos);

        // Script-based attacks override everything
        EnemyScriptBase script = mgr.getEnemyScript(this.enemyId);
        if (script != null) {
            if (dist < this.attackRange) {
                int dex = (int) ((6.5 * (this.model.getStats().getDex() + 17.3)) / 75);
                boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > (1000 / dex));
                if (canShoot) {
                    this.lastShotTick = System.currentTimeMillis();
                    this.attack = true;
                    final Player target = player;
                    WorkerThread.doAsync(() -> {
                        try {
                            script.attack(targetRealm, this, target);
                        } catch (Exception e) {
                            Enemy.log.error("Failed to invoke enemy attack script. Reason: {}", e);
                        }
                    });
                }
            }
            return;
        }

        // Phase-based attacks
        List<AttackPattern> attacks = (phase != null && phase.getAttacks() != null) ? phase.getAttacks() : null;

        if (attacks != null && !attacks.isEmpty()) {
            if (this.attackCooldowns == null || this.attackCooldowns.length != attacks.size()) {
                this.attackCooldowns = new long[attacks.size()];
            }
            long now = System.currentTimeMillis();
            boolean anyAttacked = false;
            for (int i = 0; i < attacks.size(); i++) {
                AttackPattern ap = attacks.get(i);
                if (dist < ap.getMinRange() || dist > ap.getMaxRange()) continue;
                if ((now - this.attackCooldowns[i]) < ap.getCooldownMs()) continue;

                this.attackCooldowns[i] = now;
                anyAttacked = true;
                fireAttackPattern(ap, player, mgr, targetRealm);
            }
            this.attack = anyAttacked;
        } else {
            // Legacy: single attackId
            if (dist < this.attackRange && this.weaponId >= 0) {
                int dex = Math.max(1, (int) ((6.5 * (this.model.getStats().getDex() + 17.3)) / 75));
                boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > (1000 / dex));
                if (canShoot) {
                    this.lastShotTick = System.currentTimeMillis();
                    this.attack = true;
                    fireLegacyAttack(player, mgr, targetRealm);
                }
            } else {
                this.attack = false;
            }
        }
    }

    private void fireAttackPattern(AttackPattern ap, Player player, RealmManagerServer mgr, Realm targetRealm) {
        ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(ap.getProjectileGroupId());
        if (group == null) return;

        Vector2f source = this.getPos().clone(this.getSize() / 2, this.getSize() / 2);
        Vector2f dest = player.getBounds().getPos().clone(player.getSize() / 2, player.getSize() / 2);

        if (ap.isPredictive() && player.getDx() != 0 || player.getDy() != 0) {
            float travelTime = source.distanceTo(dest) / 5f; // approximate
            dest.x += player.getDx() * travelTime;
            dest.y += player.getDy() * travelTime;
        }

        float baseAngle = Bullet.getAngle(source, dest);

        if (ap.getBurstCount() <= 1) {
            fireProjectileGroup(group, source, baseAngle, player, mgr, targetRealm);
        } else {
            // Burst fire - async with delays
            final float angle = baseAngle;
            WorkerThread.doAsync(() -> {
                try {
                    for (int b = 0; b < ap.getBurstCount(); b++) {
                        float burstAngle = angle + (ap.getAngleOffsetPerBurst() * b);
                        fireProjectileGroup(group, source.clone(), burstAngle, player, mgr, targetRealm);
                        if (b < ap.getBurstCount() - 1 && ap.getBurstDelayMs() > 0) {
                            Thread.sleep(ap.getBurstDelayMs());
                        }
                    }
                } catch (Exception e) {
                    Enemy.log.error("Failed burst fire. Reason: {}", e);
                }
            });
        }
    }

    private void fireProjectileGroup(ProjectileGroup group, Vector2f source, float baseAngle,
            Player player, RealmManagerServer mgr, Realm targetRealm) {
        for (Projectile p : group.getProjectiles()) {
            float angle;
            if (p.getPositionMode().equals(ProjectilePositionMode.TARGET_PLAYER)) {
                angle = baseAngle + Float.parseFloat(p.getAngle());
            } else {
                angle = Float.parseFloat(p.getAngle());
            }
            Bullet b = mgr.addProjectile(targetRealm.getRealmId(), 0l, player.getId(), group.getProjectileGroupId(),
                    p.getProjectileId(), source.clone(), angle, p.getSize(), p.getMagnitude(), p.getRange(),
                    p.getDamage(), true, p.getFlags(), p.getAmplitude(), p.getFrequency(), this.getId());
            if (b != null && p.getEffects() != null) b.setEffects(p.getEffects());
        }
    }

    private void fireLegacyAttack(Player player, RealmManagerServer mgr, Realm targetRealm) {
        Vector2f dest = player.getBounds().getPos().clone(player.getSize() / 2, player.getSize() / 2);
        Vector2f source = this.getPos().clone(this.getSize() / 2, this.getSize() / 2);
        float angle = Bullet.getAngle(source, dest);
        ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(this.weaponId);
        if (group == null) return;
        fireProjectileGroup(group, source, angle, player, mgr, targetRealm);
    }

    // ========== LEGACY CHASE (for backwards compat without phases) ==========

    public void chase(Player player) {
        if (player == null || player.hasEffect(ProjectileEffectType.INVISIBLE)) {
            this.up = false;
            this.dy = 0;
            this.dx = 0;
            this.down = false;
            this.right = false;
            this.left = false;
            return;
        }

        if (this.getPos().distanceTo(player.getPos()) < this.chaseRange
                && this.getPos().distanceTo(player.getPos()) >= this.attackRange) {
            if (this.pos.y > player.pos.y + 1) { this.up = true; this.dy = -CHASE_SPEED; } else { this.up = false; }
            if (this.pos.y < player.pos.y - 1) { this.down = true; this.dy = CHASE_SPEED; } else { this.down = false; }
            if (this.pos.x > player.pos.x + 1) { this.left = true; this.dx = -CHASE_SPEED; } else { this.left = false; }
            if (this.pos.x < player.pos.x - 1) { this.right = true; this.dx = CHASE_SPEED; } else { this.right = false; }
        }
    }

    // ========== UPDATE LOOPS ==========

    public void update(RealmManagerClient mgr, double time) {
        super.update(time);
        if (this.stats != null && this.stats.getHp() > 0) {
            this.healthpercent = (float) this.getHealth() / (float) this.stats.getHp();
        }
        if (this.stats != null && this.stats.getMp() > 0) {
            this.manapercent = (float) this.getMana() / (float) this.stats.getMp();
        }
        // Enemy positions are purely server-authoritative via ObjectMovePacket lerp.
        // No client-side extrapolation - movement patterns change direction too frequently.
    }

    public void update(long realmId, RealmManagerServer mgr, double time) {
        final Realm targetRealm = mgr.getRealms().get(realmId);
        final Player player = mgr.getClosestPlayer(targetRealm.getRealmId(), this.getPos(), this.chaseRange);
        super.update(time);
        if (player == null) {
            return;
        }

        float currentHealthPercent = (float) this.getHealth() / (float) this.getStats().getHp();
        float currentManaPercent = (this.getStats().getMp() > 0) ? (float) this.getMana() / (float) this.getStats().getMp() : 0;
        this.setHealthpercent(currentHealthPercent);
        this.setManapercent(currentManaPercent);
        this.healthpercent = currentHealthPercent;

        // Resolve active phase
        EnemyPhase phase = this.getActivePhase();

        // Movement — STASIS freezes movement AND attacks (like PARALYZED + STUNNED combined)
        final boolean frozen = this.hasEffect(ProjectileEffectType.PARALYZED) || this.hasEffect(ProjectileEffectType.STASIS);
        if (frozen) {
            this.up = false;
            this.down = false;
            this.right = false;
            this.left = false;
            this.dx = 0;
            this.dy = 0;
        } else if (phase != null && phase.getMovement() != null) {
            this.applyMovement(player, phase);
        } else {
            this.chase(player);
        }

        // Attacks — STASIS also prevents attacking
        final boolean notInvisible = !player.hasEffect(ProjectileEffectType.INVISIBLE);
        if (notInvisible && !this.hasEffect(ProjectileEffectType.STUNNED) && !this.hasEffect(ProjectileEffectType.STASIS)) {
            this.processAttacks(player, phase, mgr, targetRealm);
        } else {
            this.attack = false;
        }

        // Apply movement with collision checks
        if (!frozen) {
            if (!targetRealm.getTileManager().collisionTile(this, this.dx, 0)
                    && !targetRealm.getTileManager().collidesXLimit(this, this.dx)
                    && !targetRealm.getTileManager().isVoidTile(
                            this.pos.clone(this.getSize() / 2, this.getSize() / 2), this.dx, 0)) {
                this.pos.x += this.dx;
            }
            if (!targetRealm.getTileManager().collisionTile(this, 0, this.dy)
                    && !targetRealm.getTileManager().collidesYLimit(this, this.dy)
                    && !targetRealm.getTileManager().isVoidTile(
                            this.pos.clone(this.getSize() / 2, this.getSize() / 2), 0, this.dy)) {
                this.pos.y += this.dy;
            }
        }
    }

    public void idle(boolean applyMovement) {
        if (this.idleTime >= Enemy.IDLE_FRAMES) {
            this.up = Realm.RANDOM.nextBoolean();
            this.down = Realm.RANDOM.nextBoolean();
            this.left = Realm.RANDOM.nextBoolean();
            this.right = Realm.RANDOM.nextBoolean();
            if (this.up) this.dy = -CHASE_SPEED;
            if (this.down) this.dy = CHASE_SPEED;
            if (this.right) this.dx = CHASE_SPEED;
            if (this.left) this.dx = -CHASE_SPEED;
            this.idleTime = 0;
        } else {
            this.idleTime++;
        }
        if (applyMovement) {
            this.pos.x += this.dx;
            this.pos.y += this.dy;
        }
    }

    @Override
    public void updateEffectState() {
        if (this.getSpriteSheet() == null) return;
        if (this.hasEffect(ProjectileEffectType.STASIS)) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.STASIS)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.STASIS);
            }
        } else if (this.hasEffect(ProjectileEffectType.PARALYZED)) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.GRAYSCALE)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.GRAYSCALE);
            }
        } else if (this.hasEffect(ProjectileEffectType.STUNNED)) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.DECAY)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.DECAY);
            }
        } else if (this.hasEffect(ProjectileEffectType.CURSED)) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.CURSED)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.CURSED);
            }
        } else if (this.hasEffect(ProjectileEffectType.POISONED)) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.POISONED)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.POISONED);
            }
        } else if (this.hasNoEffects()) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.NORMAL)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.NORMAL);
            }
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        if (this.getSpriteSheet() == null) return;
        this.updateEffectState();
        TextureRegion frame = this.getSpriteSheet().getCurrentFrame();
        if (frame != null) {
            float wx = this.pos.getWorldVar().x;
            float wy = this.pos.getWorldVar().y;
            float ox = 2.5f;
            com.jrealm.game.graphics.ShaderManager.applyEffect(batch, Sprite.EffectEnum.SILHOUETTE);
            if (this.left) {
                batch.draw(frame, wx + this.size + ox, wy, -this.size, this.size);
                batch.draw(frame, wx + this.size - ox, wy, -this.size, this.size);
                batch.draw(frame, wx + this.size, wy + ox, -this.size, this.size);
                batch.draw(frame, wx + this.size, wy - ox, -this.size, this.size);
            } else {
                batch.draw(frame, wx + ox, wy, this.size, this.size);
                batch.draw(frame, wx - ox, wy, this.size, this.size);
                batch.draw(frame, wx, wy + ox, this.size, this.size);
                batch.draw(frame, wx, wy - ox, this.size, this.size);
            }
            com.jrealm.game.graphics.ShaderManager.clearEffect(batch);
            Sprite.EffectEnum currentEffect = this.getSpriteSheet().getCurrentEffect();
            com.jrealm.game.graphics.ShaderManager.applyEffect(batch, currentEffect);
            if (this.left) {
                batch.draw(frame, wx + this.size, wy, -this.size, this.size);
            } else {
                batch.draw(frame, wx, wy, this.size, this.size);
            }
            com.jrealm.game.graphics.ShaderManager.clearEffect(batch);
        }
    }
}
