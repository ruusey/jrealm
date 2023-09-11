package com.jrealm.game.entity.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stats {
	private short hp;
	private short mp;
	private short def;
	private short att;
	private short spd;
	private short dex;
	private short vit;
	private short wis;

	//Stat calculation chaining
	public Stats concat(Stats other) {
		if (other == null)
			return this;
		return Stats.builder()
				.hp((short)(this.hp+other.getHp()))
				.mp((short)(this.mp+other.getMp()))
				.def((short)(this.def+other.getDef()))
				.att((short)(this.att+other.getAtt()))
				.spd((short)(this.spd+other.getSpd()))
				.dex((short)(this.dex+other.getDex()))
				.vit((short)(this.vit+other.getVit()))
				.wis((short)(this.wis+other.getWis()))
				.build();
	}

	@Override
	public Stats clone() {
		return Stats.builder().hp((short) (this.hp)).mp((short) (this.mp)).def((short) (this.def))
				.att((short) (this.att)).spd((short) (this.spd)).dex((short) (this.dex)).vit((short) (this.vit))
				.wis((short) (this.wis)).build();
	}
}
