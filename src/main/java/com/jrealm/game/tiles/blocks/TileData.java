package com.jrealm.game.tiles.blocks;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.net.Streamable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TileData implements Streamable<TileData> {
	private byte hasCollision;
	private byte slows;
	private byte damaging;


	public boolean hasCollision() {
		return this.hasCollision != 0;
	}

	public boolean slows() {
		return this.slows != 0;
	}

	public boolean damaging() {
		return this.damaging!=0;
	}

	public static TileData withCollision() {
		return new TileData((byte) 1, (byte) 0, (byte) 0);
	}

	public static TileData withoutCollision() {
		return new TileData((byte) 0, (byte) 0, (byte) 0);
	}

	@Override
	public void write(DataOutputStream stream) throws Exception {
		stream.writeByte(this.hasCollision);
		stream.writeByte(this.slows);
		stream.writeByte(this.damaging);
	}

	@Override
	public TileData read(DataInputStream stream) throws Exception {
		final byte hasCollision = stream.readByte();
		final byte slows = stream.readByte();
		final byte damaging = stream.readByte();

		return new TileData(hasCollision, slows, damaging);
	}
}