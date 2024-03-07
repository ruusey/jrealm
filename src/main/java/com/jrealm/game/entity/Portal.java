package com.jrealm.game.entity;

import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.time.Instant;

import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.game.graphics.Sprite;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Streamable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Portal implements Streamable<Portal> {
	private long id;
	private short portalId;
	private long expires;
	private Vector2f pos;
	private Sprite sprite;

	public Portal(long id, short portalId, Vector2f pos) {
		this.id = id;
		this.portalId = portalId;
		this.pos = pos;
		this.expires = Instant.now().toEpochMilli() + 35000;
		this.sprite = GameSpriteManager.loadSprite(GameDataManager.PORTALS.get((int) portalId));
	}

	public Portal(short portalId, Vector2f pos) {
		this.portalId = portalId;
		this.pos = pos;
		this.expires = Instant.now().toEpochMilli() + 35000;
		this.sprite = GameSpriteManager.loadSprite(GameDataManager.PORTALS.get((int) portalId));
	}

	public short getPortalId() {
		return this.portalId;
	}

	public boolean isExpired() {
		return Instant.now().toEpochMilli() >= this.expires;
	}

	public void setNeverExpires() {
		this.expires = Long.MAX_VALUE;
	}

	@Override
	public void write(DataOutputStream stream) throws Exception {
		stream.writeLong(this.id);
		stream.writeShort(this.portalId);
		stream.writeLong(this.expires);
		stream.writeFloat(this.pos.x);
		stream.writeFloat(this.pos.y);
	}

	@Override
	public Portal read(DataInputStream stream) throws Exception {
		final long id = stream.readLong();
		final short portalId = stream.readShort();
		final long expires = stream.readLong();
		final float posX = stream.readFloat();
		final float posY = stream.readFloat();
		final Portal newPortal = new Portal(id, portalId, new Vector2f(posX, posY));
		newPortal.setExpires(expires);
		return newPortal;
	}

	public boolean equals(Portal other) {
		return (this.id == other.getId()) && (this.portalId == other.getPortalId())
				&& this.getPos().equals(other.getPos()) && (this.expires == other.getExpires());
	}

	public void render(Graphics2D g) {
		g.drawImage(this.sprite.image, (int) (this.pos.getWorldVar().x), (int) (this.pos.getWorldVar().y), 32, 32,
				null);

	}
}
