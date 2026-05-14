package com.openrealm.game.contants;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Status effects applied to entities (players, enemies) on-hit or from abilities.
 *
 * Applied via {@code entity.addEffect(type, duration)}. Stored in
 * {@code Projectile.effects} as {@link com.openrealm.game.model.ProjectileEffect}.
 *
 * Projectile behavior flags (stored in {@code Projectile.flags} /
 * {@code Bullet.flags}) live in {@link ProjectileFlag} and use the same
 * numeric ID space — never mix the two enums.
 */
public enum StatusEffectType {
    INVISIBLE((short) 0),
    HEALING((short) 1),
    PARALYZED((short) 2),
    STUNNED((short) 3),
    SPEEDY((short) 4),
    HEAL((short) 5),
    INVINCIBLE((short) 6),
    NONE((short) 8),
    TELEPORT((short) 9),
    DAZED((short) 11),
    DAMAGING((short) 14),
    STASIS((short) 15),
    CURSED((short) 16),
    POISONED((short) 17),
    ARMORED((short) 18),
    BERSERK((short) 19),
    SLOWED((short) 21),
    ARMOR_BROKEN((short) 22),
    /** Marks the entity as a priority target. Enemies that fire targeted
     *  projectiles will pick taunted players over untaunted ones in the same
     *  acquisition radius. Applied by the Knight's Taunt ability. */
    TAUNT_TARGET((short) 23),
    /** 1.5× DEF buff (vs ARMORED's 2×). Applied by the Knight's Brace ability,
     *  usually together with SLOWED to lock the player into a defensive stance. */
    BRACED((short) 24),
    /** +5 VIT aura buff. Applied by the Priest's Protective Aura passive to all
     *  players within 5 tiles of the priest (including the priest itself). The
     *  passive refreshes the status every server tick so it stays active as
     *  long as the ally is in range, and decays once they leave. */
    PROTECTED((short) 25),
    /** Phalanx Dome — spherical bullet-blocking field around the caster.
     *  Server tick destroys every enemy bullet that enters the radius so any
     *  player standing inside is shielded without being flagged INVINCIBLE. */
    PHALANX_DOME((short) 26),
    /** Weakens outgoing damage. While active, the affected entity deals
     *  35% less damage with basic attacks and ability bullets. Stacks
     *  multiplicatively with other damage modifiers. */
    WEAKEN((short) 27),
    /** Tunnel-vision debuff. Client-side: shrinks the affected player's
     *  visible render distance to ~3 tiles so enemies and bullets outside
     *  that radius vanish from view. Server-authoritative on positions
     *  still — the player just can't see what's coming. */
    BLIND((short) 28),
    /** Anti-debuff bubble. While active, new debuff applications on this
     *  entity are silently dropped (existing debuffs continue ticking).
     *  Pairs with healer / support kits as a proactive "save". */
    WARDED((short) 29),
    /** Mana regen amplifier — wis-based MP regen runs at 2× speed for the
     *  duration. Mirrors HEALING for HP. */
    MANA_FOUNT((short) 30),
    /** Debuff amplifier — any new debuff applied to this entity has its
     *  duration doubled. Used to set up burst windows on bosses. */
    VULNERABLE((short) 31),
    /** Movement lock — SLOWED scale plus a hard veto on TELEPORT / dash
     *  abilities. Keeps slippery classes pinned for setup plays. */
    GROUNDED((short) 32),
    /** Trickster passive marker — when an enemy carrying this dies, an
     *  extra loot-tier roll bias is applied for whichever player tagged
     *  it. Pure server-side bookkeeping; no visible aura. */
    MARKED_FOR_LOOT((short) 33);

    public static Map<Short, StatusEffectType> map = new HashMap<>();
    static {
        for (StatusEffectType e : StatusEffectType.values()) {
            map.put(e.effectId, e);
        }
    }

    public short effectId;

    StatusEffectType(short effectId) {
        this.effectId = effectId;
    }

    @JsonCreator
    public static StatusEffectType valueOf(short effectId) {
        return map.get(effectId);
    }
}
