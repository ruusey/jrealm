package com.jrealm.game.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.jrealm.game.contants.ProjectileEffectType;
import com.jrealm.game.contants.ProjectilePositionMode;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;
import com.jrealm.game.model.EnemyModel;
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
    
    public void applyUpdate(UpdatePacket packet, PlayState state) {
        this.name = packet.getPlayerName();
        this.stats = packet.getStats().asStats();
        this.health = packet.getHealth();
        this.mana = packet.getMana();
        this.setEffectIds(packet.getEffectIds());
        this.setEffectTimes(packet.getEffectTimes());
    }
    
    public void chase(Player player) {

        if ((player == null) || player.hasEffect(ProjectileEffectType.INVISIBLE)) {
            this.up = false;
            this.dy = 0;
            this.dx = 0;
            this.down = false;
            this.right = false;
            this.left = false;
            return;
        }

        if ((this.getPos().distanceTo(player.getPos()) < this.chaseRange)
                && (this.getPos().distanceTo(player.getPos()) >= this.attackRange)) {
            if (this.pos.y > (player.pos.y + 1)) {
                this.up = true;
                this.dy = -CHASE_SPEED;
            } else {
                this.up = false;
            }
            if (this.pos.y < (player.pos.y - 1)) {
                this.down = true;
                this.dy = CHASE_SPEED;

            } else {
                this.down = false;
            }

            if (this.pos.x > (player.pos.x + 1)) {
                this.left = true;
                this.dx = -CHASE_SPEED;
            } else {
                this.left = false;
            }
            if (this.pos.x < (player.pos.x - 1)) {
                this.right = true;
                this.dx = CHASE_SPEED;
            } else {
                this.right = false;
            }
        }
    }

    public void update(RealmManagerClient mgr, double time) {
        Player player = mgr.getClosestPlayer(this.getPos(), this.chaseRange);
        super.update(time);
        if (player == null)
            return;
        
        float currentHealthPercent = (float) this.getHealth() / (float) this.getStats().getHp();
        float currentManaPercent = (float) this.getMana() / (float) this.getStats().getMp();
        this.setHealthpercent(currentHealthPercent);
        this.setManapercent(currentManaPercent);
        this.healthpercent = currentHealthPercent;
        this.chase(player);

        if (this.hasEffect(ProjectileEffectType.PARALYZED)) {
            this.up = false;
            this.down = false;
            this.right = false;
            this.left = false;
            return;
        }
        this.pos.x += this.dx;
        this.pos.y += this.dy;

    }

    public void update(long realmId, RealmManagerServer mgr, double time) {
        final Realm targetRealm = mgr.getRealms().get(realmId);
        final Player player = mgr.getClosestPlayer(targetRealm.getRealmId(), this.getPos(), this.chaseRange);
        super.update(time);
        if (player == null) {
            //this.idle(true);
            return;
        }
        
        float currentHealthPercent = (float) this.getHealth() / (float) this.getStats().getHp();
        float currentManaPercent = (float) this.getMana() / (float) this.getStats().getMp();
        this.setHealthpercent(currentHealthPercent);
        this.setManapercent(currentManaPercent);
        this.healthpercent = currentHealthPercent;
        this.chase(player);
        final boolean notInvisible = !player.hasEffect(ProjectileEffectType.INVISIBLE);
        if ((this.getPos().distanceTo(player.getPos()) < this.attackRange && !this.hasEffect(ProjectileEffectType.STUNNED))
                && notInvisible) {
            this.attack = true;

            int dex = (int) ((6.5 * (this.model.getStats().getDex() + 17.3)) / 75);
            boolean canShoot = ((System.currentTimeMillis() - this.lastShotTick) > (1000 / dex));

            if (canShoot) {
                this.lastShotTick = System.currentTimeMillis();
                Player target = player;
                EnemyScriptBase script = mgr.getEnemyScript(this.enemyId);
                if (script == null) {
                    Vector2f dest = target.getBounds().getPos().clone(target.getSize() / 2, target.getSize() / 2);

                    Vector2f source = this.getPos().clone(this.getSize() / 2, this.getSize() / 2);
                    float angle = Bullet.getAngle(source, dest);
                    ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(this.weaponId);

                    for (Projectile p : group.getProjectiles()) {
                        if (p.getPositionMode().equals(ProjectilePositionMode.TARGET_PLAYER)) {
                            mgr.addProjectile(targetRealm.getRealmId(), 0l, player.getId(), this.getWeaponId(),
                                    p.getProjectileId(), source.clone(), angle + Float.parseFloat(p.getAngle()),
                                    p.getSize(), p.getMagnitude(), p.getRange(), p.getDamage(), true, p.getFlags(),
                                    p.getAmplitude(), p.getFrequency(), this.getId());
                        } else if (p.getPositionMode().equals(ProjectilePositionMode.ABSOLUTE)) {
                            mgr.addProjectile(targetRealm.getRealmId(), 0l, player.getId(), this.getWeaponId(),
                                    p.getProjectileId(), source.clone(), Float.parseFloat(p.getAngle()), p.getSize(),
                                    p.getMagnitude(), p.getRange(), p.getDamage(), true, p.getFlags(), p.getAmplitude(),
                                    p.getFrequency(), this.getId());
                        }
                    }
                } else {
                    final Runnable enemyAttack = () -> {
                        try {
                            script.attack(targetRealm, this, target);
                        } catch (Exception e) {
                            Enemy.log.error("Failed to invoke enemy attack script. Reason: {}", e);
                        }
                    };
                    WorkerThread.doAsync(enemyAttack);
                }
            }
        } else {
            this.attack = false;
        }
        if (this.hasEffect(ProjectileEffectType.PARALYZED)) {
            this.up = false;
            this.down = false;
            this.right = false;
            this.left = false;
            return;
        }
        this.idle(false);
        // X-axis: collision tile + map boundary + void tile
        if (!targetRealm.getTileManager().collisionTile(this, this.dx, 0)
                && !targetRealm.getTileManager().collidesXLimit(this, this.dx)
                && !targetRealm.getTileManager().isVoidTile(
                        this.pos.clone(this.getSize() / 2, this.getSize() / 2), this.dx, 0)) {
            this.pos.x += this.dx;
        }
        // Y-axis: collision tile + map boundary + void tile
        if (!targetRealm.getTileManager().collisionTile(this, 0, this.dy)
                && !targetRealm.getTileManager().collidesYLimit(this, this.dy)
                && !targetRealm.getTileManager().isVoidTile(
                        this.pos.clone(this.getSize() / 2, this.getSize() / 2), 0, this.dy)) {
            this.pos.y += this.dy;
        }
    }
    
    public void idle(boolean applyMovement) {
        if (this.idleTime >= Enemy.IDLE_FRAMES) {
            this.up = Realm.RANDOM.nextBoolean();
            this.down = Realm.RANDOM.nextBoolean();
            this.left = Realm.RANDOM.nextBoolean();
            this.right = Realm.RANDOM.nextBoolean();
            if (this.up) {
                this.dy = -CHASE_SPEED;
            }
            if (this.down) {
                this.dy = CHASE_SPEED;
            }
            if (this.right) {
                this.dx = CHASE_SPEED;
            }
            if (this.left) {
                this.dx = -CHASE_SPEED;
            }
            this.idleTime = 0;
        } else {
            this.idleTime++;
        }
        if(applyMovement) {
            this.pos.x += this.dx;
            this.pos.y += this.dy;   
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        if (this.getSpriteSheet() == null) return;

        // Update effect tags
        if (this.hasEffect(ProjectileEffectType.PARALYZED)) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.GRAYSCALE)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.GRAYSCALE);
            }
        } else if (this.hasEffect(ProjectileEffectType.STUNNED)) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.DECAY)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.DECAY);
            }
        } else if (this.hasNoEffects()) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.NORMAL)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.NORMAL);
            }
        }

        // Draw outline: 4 offset black silhouettes
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

            // Apply shader effect
            Sprite.EffectEnum currentEffect = this.getSpriteSheet().getCurrentEffect();
            com.jrealm.game.graphics.ShaderManager.applyEffect(batch, currentEffect);

            if (this.left) {
                batch.draw(frame, wx + this.size, wy, -this.size, this.size);
            } else {
                batch.draw(frame, wx, wy, this.size, this.size);
            }

            // Clear shader
            com.jrealm.game.graphics.ShaderManager.clearEffect(batch);
        }
    }

}