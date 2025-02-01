package com.jrealm.net.core.nettypes.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.core.SerializableFieldType;

public class SerializablePlayer extends SerializableFieldType<Player> {

	@Override
	public Player read(DataInputStream stream) throws Exception {
		final long id = stream.readLong();
		final String name = stream.readUTF();
		final String accountUuid = stream.readUTF();
		final String characterUuid = stream.readUTF();
		final int classId = stream.readInt();
		final short size = stream.readShort();
		final float posX = stream.readFloat();
		final float posY = stream.readFloat();
		final float dX = stream.readFloat();
		final float dY = stream.readFloat();
		final Player player = Player.fromData(id, name, new Vector2f(posX, posY), size, CharacterClass.valueOf(classId));
		player.setDx(dX);
		player.setDy(dY);
		player.setName(name);
		player.setAccountUuid(accountUuid);
		player.setCharacterUuid(characterUuid);
		return player;
	}

	@Override
	public void write(Player value, DataOutputStream stream) throws Exception {
		stream.writeLong(value.getId());
		stream.writeUTF(value.getName());
		stream.writeUTF(value.getAccountUuid());
		stream.writeUTF(value.getCharacterUuid());
		stream.writeInt(value.getClassId());
		stream.writeShort(value.getSize());
		stream.writeFloat(value.getPos().x);
		stream.writeFloat(value.getPos().y);
		stream.writeFloat(value.getDx());
		stream.writeFloat(value.getDy());
	}
}
