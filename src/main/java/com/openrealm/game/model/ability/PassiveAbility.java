package com.openrealm.game.model.ability;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A class-bound passive ability. Loaded from {@code passives.json}. Each class
 * references at most one via {@link AbilityTree#getPassive()}.
 *
 * See combat-rework.md §3.2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassiveAbility {
    private int id;
    private String name;
    private String description;
    private String iconKey;
    private int classId;

    private List<PassiveTrigger> triggers = new ArrayList<>();
    private List<String> tags = new ArrayList<>();

    public List<PassiveTrigger> triggerList() {
        return this.triggers == null ? new ArrayList<>() : this.triggers;
    }
}
