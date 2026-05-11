package com.openrealm.game.model.ability;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The class-defined kit a player draws from. Phase 2A shape:
 *
 *   pool          — every ability id the class can put on its hotbar.
 *                   IDs may resolve to either {@link Ability} (active) or
 *                   {@link PassiveAbility} (slotted passive). Disjoint ID
 *                   namespaces — same id never appears in both tables.
 *
 *   defaultHotbar — initial binding (exactly 4 ids drawn from pool). Players
 *                   start each character with this layout; Shift+N hotswap
 *                   cycles a slot forward through the pool to the next id
 *                   not already bound.
 *
 *   passive       — class-wide always-on passive. NOT in the pool, NOT
 *                   bindable to the hotbar. 0 = no passive.
 *
 * See combat-rework.md §3.4 (updated).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbilityTree {
    /** Full class pool — every id the player can bind. May be larger than 4. */
    private int[] pool = new int[0];
    /** Initial hotbar binding (exactly 4 ids). 0 = empty slot. */
    private int[] defaultHotbar = new int[]{0, 0, 0, 0};
    /** Always-on class passive id, or 0 for no passive. */
    private int passive = 0;

    /** Nullsafe accessor — never throws OOB. */
    public int getDefaultHotbarSlot(int slot) {
        if (this.defaultHotbar == null || slot < 0 || slot >= this.defaultHotbar.length) return 0;
        return this.defaultHotbar[slot];
    }
}
