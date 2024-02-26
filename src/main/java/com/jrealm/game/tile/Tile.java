package com.jrealm.game.tile;

import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Rectangle;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Streamable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Tile implements Streamable<Tile> {
	private short tileId;
	private boolean discovered = false;
	private short size;
	private Vector2f pos;
	private TileData data;

	public Sprite img;

	public Tile(short tileId, Sprite img, Vector2f pos, TileData data, short size, boolean discovered) {
		this.tileId = tileId;
		this.img = img;
		this.pos = pos;
		this.size = size;
		this.data = data;
		this.discovered = discovered;
	}

	public Tile(short tileId, Vector2f pos, TileData data, short size, boolean discovered) {
		this.tileId = tileId;
		this.pos = pos;
		this.size = size;
		this.data = data;
		this.discovered = discovered;
		this.img = GameDataManager.getSubSprite(GameDataManager.TILES.get((int)this.tileId), 8);
		if (this.img == null) {
			System.out.println();
		}
	}

	public boolean update(Rectangle bounds) {
		return false;
	}

	public int getWidth() {
		return this.size;
	}

	public int getHeight() {
		return this.size;
	}

	public Vector2f getPos() {
		return this.pos;
	}

	public boolean isVoid() {
		return this.tileId == 0;
	}

	public void render(Graphics2D g) {
		g.drawImage(this.img.image, (int) this.pos.getWorldVar().x, (int) this.pos.getWorldVar().y, this.size,
				this.size, null);
	}

	@Override
	public void write(DataOutputStream stream) throws Exception {
		stream.writeShort(this.tileId);
		stream.writeBoolean(this.discovered);
		stream.writeShort(this.size);
		stream.writeFloat(this.pos.x);
		stream.writeFloat(this.pos.y);
		stream.writeByte(this.data.getHasCollision());
		stream.writeByte(this.data.getSlows());
		stream.writeByte(this.data.getDamaging());
	}

	@Override
	public Tile read(DataInputStream stream) throws Exception {
		final short tileId = stream.readShort();
		final boolean discovered = stream.readBoolean();
		final short size = stream.readShort();
		final float posX = stream.readFloat();
		final float posY = stream.readFloat();
		final byte hasCollision = stream.readByte();
		final byte slows = stream.readByte();
		final byte damaging = stream.readByte();

		return new Tile(tileId, new Vector2f(posX, posY), new TileData(hasCollision, slows, damaging), size,
				discovered);
	}
}
