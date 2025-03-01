package com.jrealm.net.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.jrealm.game.entity.Portal;
import com.jrealm.game.math.Vector2f;
import com.jrealm.net.Streamable;
import com.jrealm.net.core.IOService;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.SerializableLong;
import com.jrealm.net.core.nettypes.SerializableShort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Streamable
public class NetPortal extends SerializableFieldType<NetPortal> {
	@SerializableField(order = 0, type = SerializableLong.class)
	private long id;
	@SerializableField(order = 1, type = SerializableShort.class)
	private short portalId;
	@SerializableField(order = 2, type = SerializableLong.class)
	private long fromRealmId;
	@SerializableField(order = 3, type = SerializableLong.class)
	private long toRealmId;
	@SerializableField(order = 4, type = SerializableLong.class)
	private long expires;
	@SerializableField(order = 5, type = Vector2f.class)
	private Vector2f pos;

	@Override
	public NetPortal read(DataInputStream stream) throws Exception {
		return IOService.readStream(getClass(), stream);
	}

	@Override
	public int write(NetPortal value, DataOutputStream stream) throws Exception {
		return IOService.writeStream(value, stream);
	}
	
	public Portal asPortal() {
		Portal p = new Portal();
		p.setId(this.getId());
		p.setPortalId(this.getPortalId());
		p.setFromRealmId(this.getFromRealmId());
		p.setToRealmId(this.getToRealmId());
		p.setExpires(this.getExpires());
		p.setPos(this.getPos());
		
		return p;
		
	}

}
