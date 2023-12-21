package com.jrealm.game.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class PortalModel extends SpriteModel {
	private int portalId;
	private int mapId;

}
