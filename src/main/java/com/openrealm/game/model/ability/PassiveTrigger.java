package com.openrealm.game.model.ability;

import java.util.ArrayList;
import java.util.List;

import com.openrealm.game.contants.PassiveTriggerEvent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One trigger on a {@link PassiveAbility}. A passive may have several triggers
 * for compound behavior (e.g., proc on-crit AND on-kill).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassiveTrigger {
    /** Event name; resolved to {@link PassiveTriggerEvent} at apply-time. */
    private String event;
    /** For ON_TICK triggers — period between fires in milliseconds. */
    private long tickMs = 0L;
    /** Conditions required to fire (e.g. proc-chance, target hp threshold). */
    private List<AbilityScaling> conditions = new ArrayList<>();
    /** What happens when the trigger fires. */
    private List<AbilityEffect> effects = new ArrayList<>();
    /** Scalings applied to the effects (statusduration, dmg multiplier, etc.). */
    private List<AbilityScaling> scalings = new ArrayList<>();

    public PassiveTriggerEvent eventEnum() {
        return PassiveTriggerEvent.parse(this.event);
    }
}
