package com.jrealm.net.client.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.List;

import com.jrealm.game.contants.PacketType;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.entity.Enemy;
import com.jrealm.game.entity.Player;
import com.jrealm.game.entity.item.GameItem;
import com.jrealm.game.entity.item.Stats;
import com.jrealm.net.Packet;
import com.jrealm.net.core.SerializableField;
import com.jrealm.net.core.SerializableFieldType;
import com.jrealm.net.core.nettypes.*;
import com.jrealm.net.core.nettypes.game.SerializableStats;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class UpdatePacket extends Packet {
	@SerializableField(order = 0, type = SerializableLong.class)
    private long playerId;
	@SerializableField(order = 0, type = SerializableString.class)
    private String playerName;
	@SerializableField(order = 0, type = SerializableStats.class)
    private Stats stats;
    private int health;
    private int mana;
    private GameItem[] inventory;
    private short[] effectIds;
    private long[] effectTimes;
    private long experience;

    // TODO: Rewrite this to only include delta data within the character not the entire character
    public UpdatePacket() {

    }

    public UpdatePacket(byte id, byte[] data) {
        super(id, data);
        try {
            this.readData(data);
        } catch (Exception e) {
            e.printStackTrace();
            UpdatePacket.log.error("Failed to build Stats Packet. Reason: {}", e.getMessage());
        }
    }

    @Override
    public void serializeWrite(DataOutputStream stream) throws Exception {
        if ((this.getId() < 1) || (this.getData() == null) || (this.getData().length < 5))
            throw new IllegalStateException("No Packet data available to write to DataOutputStream");
        this.addHeader(stream);

        stream.writeLong(this.playerId);
        stream.writeInt(this.health);
        stream.writeInt(this.mana);
        stream.writeUTF(this.playerName);

        if (this.stats != null) {
            this.stats.write(this.stats, stream);
        }

        int invSize = 0;
        if (this.inventory != null) {
            invSize = this.inventory.length;
        }
        stream.writeShort(invSize);

        for (int i = 0; i < invSize; i++) {
            if (this.inventory[i] != null) {
                this.inventory[i].write(stream);
            } else {
                stream.writeInt(-1);
            }
        }
        stream.writeShort(this.effectIds.length);
        for (int i = 0; i < this.effectIds.length; i++) {
            stream.writeShort(this.effectIds[i]);
        }

        stream.writeShort(this.effectTimes.length);
        for (int i = 0; i < this.effectTimes.length; i++) {
            stream.writeLong(this.effectTimes[i]);
        }

        stream.writeLong(this.experience);
    }

    @Override
    public void readData(byte[] data) throws Exception {
    	final ByteArrayInputStream bis = new ByteArrayInputStream(data);
    	final DataInputStream dis = new DataInputStream(bis);
        if ((dis == null) || (dis.available() < 5))
            throw new IllegalStateException("No Packet data available to read from DataInputStream");
        this.playerId = dis.readLong();
        this.health = dis.readInt();
        this.mana = dis.readInt();
        this.playerName = dis.readUTF();

        this.stats = new Stats().read(dis);
        int invSize = dis.readShort();

        if (invSize > 0) {
            this.inventory = new GameItem[invSize];
            for (int i = 0; i < invSize; i++) {
                this.inventory[i] = new GameItem().read(dis);
            }
        } else {
            this.inventory = new GameItem[20];
        }

        int effectsSize = dis.readShort();
        this.effectIds = new short[effectsSize];
        for (int i = 0; i < effectsSize; i++) {
            this.effectIds[i] = dis.readShort();
        }

        int effectTimesSize = dis.readShort();
        this.effectTimes = new long[effectTimesSize];
        for (int i = 0; i < effectsSize; i++) {
            this.effectTimes[i] = dis.readLong();
        }

        this.experience = dis.readLong();
    }
    
    public static UpdatePacket from(Enemy enemy) throws Exception {
    	final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    	final DataOutputStream stream = new DataOutputStream(byteStream);
    	if(enemy==null) return null;
        stream.writeLong(enemy.getId());
        stream.writeInt(enemy.getHealth());
        stream.writeInt(enemy.getMana());
        stream.writeUTF("enemy["+enemy.getId()+"]");

        if (enemy.getStats() != null) {
            enemy.getStats().write(enemy.getStats(), stream);
        }

        final List<GameItem> lootToDrop = Arrays.asList(GameDataManager.GAME_ITEMS.get(48));

        int invSize = lootToDrop.size();
        stream.writeShort(invSize);
        for (int i = 0; i < invSize; i++) {
        	final GameItem item = lootToDrop.get(i);
            if (item != null) {
            	item.write(stream);
            } else {
                stream.writeInt(-1);
            }
        }
        
        stream.writeShort(enemy.getEffectIds().length);
        for (int i = 0; i < enemy.getEffectIds().length; i++) {
            stream.writeShort(enemy.getEffectIds()[i]);
        }

        stream.writeShort(enemy.getEffectTimes().length);
        for (int i = 0; i < enemy.getEffectTimes().length; i++) {
            stream.writeLong(enemy.getEffectTimes()[i]);
        }

        stream.writeLong(1l);
        return new UpdatePacket(PacketType.UPDATE.getPacketId(), byteStream.toByteArray());
    }

    public static UpdatePacket from(Player player) throws Exception {
    	final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    	final DataOutputStream stream = new DataOutputStream(byteStream);
    	if(player==null) return null;
        stream.writeLong(player.getId());
        stream.writeInt(player.getHealth());
        stream.writeInt(player.getMana());
        stream.writeUTF(player.getName());

        if (player.getStats() != null) {
            player.getStats().write(player.getStats(), stream);
        }

        int invSize = 0;
        if (player.getInventory() != null) {
            invSize = player.getInventory().length;
        }
        stream.writeShort(invSize);

        for (int i = 0; i < invSize; i++) {
        	final GameItem item = player.getInventory()[i];
            if (item != null) {
                player.getInventory()[i].write(stream);
            } else {
                stream.writeInt(-1);
            }
        }

        stream.writeShort(player.getEffectIds().length);
        for (int i = 0; i < player.getEffectIds().length; i++) {
            stream.writeShort(player.getEffectIds()[i]);
        }

        stream.writeShort(player.getEffectTimes().length);
        for (int i = 0; i < player.getEffectTimes().length; i++) {
            stream.writeLong(player.getEffectTimes()[i]);
        }

        stream.writeLong(player.getExperience());
        return new UpdatePacket(PacketType.UPDATE.getPacketId(), byteStream.toByteArray());
    }

    public boolean equals(UpdatePacket other, boolean thinMatch) {
        boolean basic = (this.playerId == other.getPlayerId()) && this.playerName.equals(other.getPlayerName())
                && (this.health == other.getHealth()) && (this.mana == other.getMana());

        boolean stats = this.stats.equals(other.getStats());
        if(thinMatch) stats=true;
        if(inventory.length==1) {
        	System.out.print("");
        }
        boolean inv = true;
        for (int i = 0; i < this.inventory.length; i++) {
            if ((this.inventory[i] != null) && (other.getInventory()[i] != null)) {
                if (this.inventory[i].equals(other.getInventory()[i])) {
                    continue;
                }
                inv = false;
                break;
            }
            if ((this.inventory[i] == null) && (other.getInventory()[i] == null)) {
                continue;
            }
            inv = false;
            break;
        }
        if(thinMatch) inv=true;


        boolean effects = true;
        for (int i = 0; i < this.effectIds.length; i++) {
            if ((this.effectIds[i] != other.getEffectIds()[i]) || (this.effectTimes[i] != other.getEffectTimes()[i])) {
                effects = false;
                break;
            }
        }
        if(thinMatch) effects=true;
        boolean expEqual = this.experience == other.getExperience();
        boolean result = basic && stats && inv && effects && expEqual;
        if(!result) {
        	int i = 0;
        }
        return result;
    }
    
    public boolean equals(UpdatePacket other) {
        boolean basic = (this.playerId == other.getPlayerId()) && this.playerName.equals(other.getPlayerName())
                && (this.health == other.getHealth()) && (this.mana == other.getMana());

        boolean stats = this.stats.equals(other.getStats());
        if(inventory.length==1) {
        	//System.out.println("");
        }
        boolean inv = true;
        for (int i = 0; i < this.inventory.length; i++) {
            if ((this.inventory[i] != null) && (other.getInventory()[i] != null)) {
                if (this.inventory[i].equals(other.getInventory()[i])) {
                    continue;
                }
                inv = false;
                break;
            }
            if ((this.inventory[i] == null) && (other.getInventory()[i] == null)) {
                continue;
            }
            inv = false;
            break;
        }

        boolean effects = true;
        for (int i = 0; i < this.effectIds.length; i++) {
            if ((this.effectIds[i] != other.getEffectIds()[i]) || (this.effectTimes[i] != other.getEffectTimes()[i])) {
                effects = false;
                break;
            }
        }
        boolean expEqual = this.experience == other.getExperience();
        boolean result = basic && stats && inv && effects && expEqual;
        if(!result) {
        	int i = 0;
        }
        return result;
    }
}
