package com.jrealm.game.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.EffectType;
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
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.Streamable;
import com.jrealm.net.client.packet.UpdatePacket;
import com.jrealm.net.realm.Realm;
import com.jrealm.net.realm.RealmManagerClient;
import com.jrealm.net.realm.RealmManagerServer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public abstract class Enemy extends Entity {
    private static final int IDLE_FRAMES = 10;
    private static final float CHASE_SPEED = 1.25f;
    protected EnemyModel model;
    protected int chaseRange;
    protected int attackRange;
    protected int xOffset;
    protected int yOffset;

    private long lastShotTick = 0;
    private int enemyId;
    private int weaponId = -1;
    private int idleTime = 0;
    private Stats stats;

    public Enemy(long id, int enemyId, Vector2f origin, int size, int weaponId) {
        super(id, origin, size);
        this.model = GameDataManager.ENEMIES.get(enemyId);
        this.enemyId = enemyId;
        this.weaponId = weaponId;
        this.stats = this.model.getStats().clone();
//        this.health = stats.getHp();
//        this.mana = stats.getMp();
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
        this.stats = packet.getStats();
        this.health = packet.getHealth();
        this.mana = packet.getMana();
        this.setEffectIds(packet.getEffectIds());
        this.setEffectTimes(packet.getEffectTimes());
    }
    
    public void chase(Player player) {

        if ((player == null) || player.hasEffect(EffectType.INVISIBLE)) {
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
        this.move();
        if (player == null)
            return;
        
        float currentHealthPercent = (float) this.getHealth() / (float) this.getStats().getHp();
        float currentManaPercent = (float) this.getMana() / (float) this.getStats().getMp();
        this.setHealthpercent(currentHealthPercent);
        this.setManapercent(currentManaPercent);
        this.healthpercent = currentHealthPercent;
        this.chase(player);

        if (this.hasEffect(EffectType.PARALYZED)) {
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
        this.move();
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
        final boolean notInvisible = !player.hasEffect(EffectType.INVISIBLE);
        if ((this.getPos().distanceTo(player.getPos()) < this.attackRange && !this.hasEffect(EffectType.STUNNED))
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
                                    p.getAmplitude(), p.getFrequency());
                        } else if (p.getPositionMode().equals(ProjectilePositionMode.ABSOLUTE)) {
                            mgr.addProjectile(targetRealm.getRealmId(), 0l, player.getId(), this.getWeaponId(),
                                    p.getProjectileId(), source.clone(), Float.parseFloat(p.getAngle()), p.getSize(),
                                    p.getMagnitude(), p.getRange(), p.getDamage(), true, p.getFlags(), p.getAmplitude(),
                                    p.getFrequency());
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
        if (this.hasEffect(EffectType.PARALYZED)) {
            this.up = false;
            this.down = false;
            this.right = false;
            this.left = false;
            return;
        }
        this.idle(false);
        this.pos.x += this.dx;
        this.pos.y += this.dy;
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
    public void render(Graphics2D g) {

        Color c = new Color(0f, 0f, 0f, .35f);
        g.setColor(c);
        g.fillOval((int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y) + (int) (this.size / 1.5),
                this.size, this.size / 2);
        if (this.left) {
            g.drawImage(this.getSpriteSheet().getCurrentFrame(), (int) (this.pos.getWorldVar().x) + this.size,
                    (int) (this.pos.getWorldVar().y), -this.size, this.size, null);
        } else {
            g.drawImage(this.getSpriteSheet().getCurrentFrame(), (int) (this.pos.getWorldVar().x),
                    (int) (this.pos.getWorldVar().y), this.size, this.size, null);
        }

        if (this.hasEffect(EffectType.PARALYZED)) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.GRAYSCALE)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.GRAYSCALE);
            }
        }

        if (this.hasEffect(EffectType.STUNNED)) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.DECAY)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.DECAY);
            }
        }

        if (this.hasNoEffects()) {
            if (!this.getSpriteSheet().hasEffect(Sprite.EffectEnum.NORMAL)) {
                this.getSpriteSheet().setEffect(Sprite.EffectEnum.NORMAL);
            }
        }

        // Health Bar UI
        g.setColor(Color.red);
        g.fillRect((int) (this.pos.getWorldVar().x + this.bounds.getXOffset()), (int) (this.pos.getWorldVar().y - 5),
                16*(this.getSize()/16), 5);

        g.setColor(Color.green);
        g.fillRect((int) (this.pos.getWorldVar().x + this.bounds.getXOffset()), (int) (this.pos.getWorldVar().y - 5),
                (int) ((16*(this.getSize()/16)) * this.getHealthpercent()), 5);

    }

    // TODO: Add enemy type identifier
    //@Override
    public void write(DataOutputStream stream) throws Exception {
        stream.writeLong(this.getId());
        stream.writeInt(this.getEnemyId());
        stream.writeInt(this.getWeaponId());
        stream.writeShort(this.getSize());
        stream.writeFloat(this.getPos().x);
        stream.writeFloat(this.getPos().y);
        stream.writeFloat(this.dx);
        stream.writeFloat(this.dy);
    }

    //@Override
    public Enemy read(DataInputStream stream) throws Exception {
        final long id = stream.readLong();
        final int enemyId = stream.readInt();
        final int weaponId = stream.readInt();
        final short size = stream.readShort();
        final float posX = stream.readFloat();
        final float posY = stream.readFloat();
        final float dx = stream.readFloat();
        final float dy = stream.readFloat();

        final Enemy newEnemy = new Monster(id, enemyId, new Vector2f(posX, posY), size, weaponId);
        newEnemy.setDy(dy);
        newEnemy.setDx(dx);
        return newEnemy;
    }

    public static Enemy fromStream(DataInputStream stream) throws Exception {
        final long id = stream.readLong();
        final int enemyId = stream.readInt();
        final int weaponId = stream.readInt();
        final short size = stream.readShort();
        final float posX = stream.readFloat();
        final float posY = stream.readFloat();
        final float dx = stream.readFloat();
        final float dy = stream.readFloat();

        final Enemy newEnemy = new Monster(id, enemyId, new Vector2f(posX, posY), size, weaponId);
        newEnemy.setDy(dy);
        newEnemy.setDx(dx);
        return newEnemy;
    }

}