package com.openrealm.game.model.ability;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One effect on an {@link Ability} or a {@link PassiveAbility} trigger.
 *
 * Flat-shape (no Jackson polymorphism). Unused fields stay at their zero/null
 * defaults — the ability-resolution code reads only the fields relevant to
 * {@code type}. Keep the type enum lowercase-friendly: parsing is case-insensitive
 * via {@code type.equalsIgnoreCase(...)} where consumed.
 *
 * Recognized types:
 *   PROJECTILE_GROUP       — fire {@code projectileGroupId} (uses fan/origin offsets)
 *   STATUS_APPLY           — apply {@code statusId} for {@code baseDurationMs}
 *   HEAL                   — restore HP to {@code target} ("SELF" | "ALLIES_HIT")
 *   SHIELD                 — grant a temporary HP shield
 *   TELEPORT               — move the caster to the targeted position (validated)
 *   REFLECT_PROJECTILE     — passive-only; bounce incoming bullet back
 *   EMPOWER_NEXT_BASIC     — passive-only; flag next basic attack as empowered
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbilityEffect {
    private String type;
    /** PROJECTILE_GROUP / TELEPORT — projectile-groups.json id. */
    private int projectileGroupId = -1;
    /** PROJECTILE_GROUP — symmetric fan spread in radians per extra bullet. */
    private float fanSpread = 0f;
    private float originOffset = 0f;
    /** STATUS_APPLY — name matching StatusEffectType enum. */
    private String statusId;
    private long baseDurationMs = 0L;
    /** Whom an effect applies to: SELF | ENEMIES_HIT | ALLIES_HIT | TARGET. */
    private String target;
    /** HEAL / SHIELD base magnitude before scalings. */
    private int baseMagnitude = 0;
    /** REFLECT_PROJECTILE — base reflected-damage multiplier (scaling adds to this). */
    private float damageMul = 1.0f;
}
