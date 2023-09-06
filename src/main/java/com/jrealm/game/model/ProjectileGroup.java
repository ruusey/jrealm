package com.jrealm.game.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectileGroup {
	private int projectileGroupId;

	private List<Projectile> projectiles;
}
