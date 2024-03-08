# JRealm
### An 8bit Dungeon Crawler Rogue-Like Game written entirely in Java

<div dsplay="inline">
<img src="https://i.imgur.com/GPpcD2I.png" width="312">
<img src="https://i.imgur.com/9xfBgZ4.png" width="312">
<img src="https://i.imgur.com/ln8uzmg.png" width="312">
<img src="https://i.imgur.com/0Dy4bNX.png" width="312">
<img src="https://i.imgur.com/fa6dt5V.png" width="312">
<img src="https://i.imgur.com/2dLIVu6.png" width="312">

</div>
<br>

### Running
#### *NOTE: As of release 0.3.0 You are now required to run JRealm-Data alongside JRealm-Server see: https://github.com/ruusey/jrealm-data*

* **General**: <br/>
```java -jar ./jrealm-{version}.jar {-client | -server | -embedded} {SERVER_ADDR} {PLAYER_EMAIL} {PLAYER_PASSWORD} {CHARACTER_UUID}```

* **Server**: <br />
```java -jar ./jrealm-{version}.jar -server``` <br />
The server requires port 2222 to be available

* **Client**: <br />
```java -jar ./jrealm-{version}.jar -client {SERVER_ADDR} {PLAYER_EMAIL} {PLAYER_PASSWORD} {CHARACTER_UUID}``` <br />
Replace `{SERVER_ADDRESS}` with the IP Address of the server you wish to connect to <br />
Replace `{PLAYER_EMAIL}` with your account's email <br />
Replace `{PLAYER_PASSWORD}` with your account's password <br />
Replace `{CHARACTER_UUID}` with the UUID of your character 

### Developing
This section will cover local development of JRealm

#### Game Data
See [JRealm-Data](https://github.com/ruusey/jrealm-data) for information on modifying the game's .json data

#### Packet Handlers
Applicable classes: 
`com.jrealm.game.realm.RealmManagerServer, com.jrealm.game.realm.RealmManagerClient, com.jrealm.net.server.ServerGameLogic, com.jrealm.net.server.ClientGameLogic`

**JRealm** packet handlers exist on both the server and client realm managers to hook callbacks into recieved packets. In general packet callbacks are registered during the `registerPacketCallbacks()`
routine of `RealmManagerClient` and `RealmManagerServer`. Packet callbacks methods will typically be a static `BiConsumer<RealmManager, Packet>` that is passed the target packet and Realm Manager on receiving the packet, although
any method matching this signature can be used as a packet callback.

**Example**:
```java
// RealmManagerServer.java
private void registerPacketCallbacks() {
    this.registerPacketCallback(PacketType.PLAYER_MOVE.getPacketId(), ServerGameLogic::handlePlayerMoveServer);
}

// ServerGameLogic.java
public static void handlePlayerMoveServer(RealmManagerServer mgr, Packet packet) {
    final PlayerMovePacket playerMovePacket = (PlayerMovePacket) packet;
    final Realm realm = mgr.searchRealmsForPlayer(playerMovePacket.getEntityId());
    if (realm == null) {
        ServerGameLogic.log.error("Failed to get realm for player {}", playerMovePacket.getEntityId());
        return;
    }
    final Player toMove = realm.getPlayer(playerMovePacket.getEntityId());
    if (toMove.hasEffect(EffectType.PARALYZED))
        return;
    boolean doMove = playerMovePacket.isMove();
    float spd = (float) ((5.6 * (toMove.getComputedStats().getSpd() + 53.5)) / 75.0f);
    spd = spd/1.5f;
    if (playerMovePacket.getDirection().equals(Cardinality.NORTH)) {
        toMove.setUp(doMove);
        toMove.setDy(doMove ? -spd : 0.0f);
    }
}
```

#### Command Handlers
Applicable classes: 
`com.jrealm.game.messaging.*, com.jrealm.net.server.ServerGameLogic, com.jrealm.net.server.ServerCommandHandler`

**JRealm** command handlers are a similar subset of functionality to the Packet Callbacks mentioned in the previous section
that allow users to embed  server commands in the packets they send to **JRealm-Server**. CommandPackets consist of a `byte commandId`
and a `UTF JSON String command`. When the server recieves a Command Packet it will attempt to deserialize the JSON payload into the model
targeted by this Command `(defined in com.jrealm.game.messaging.CommandType)`

The class `ServerCommandHandler` is responsible for handling individual Command functionality. Each command callback is registered dynamically
at runtime

**Example**:
```java

    @CommandHandler("setstats")
    private static void invokeSetStats(RealmManagerServer mgr, Player target, ServerCommandMessage message) {
        if (message.getArgs() == null || message.getArgs().size() != 2)
            throw new IllegalArgumentException("Usage: /setstat {STAT_NAME} {STAT_VALUE}");
        final short valueToSet = Short.parseShort(message.getArgs().get(1));
        log.info("Player {} set stat {} to {}", target.getName(), message.getArgs().get(0), valueToSet);
        switch (message.getArgs().get(0)) {
        case "hp":
            target.getStats().setHp(valueToSet);
            break;
        case "mp":
            target.getStats().setMp(valueToSet);
            break;
        ...
    }
```
Note: Command Handler methods are allowed to throw `Exception` to control flow through the handler. By default if an Exception is thrown, its message
will be tranformed into a 502 error code message that is returned to the client.


#### Creating Maps & Terrains
//TODO: Write this



# Important classes:

* `com.jrealm.game.states.PlayState`
* `com.jrealm.game.realm.Realm`
* `com.jrealm.game.realm.tile.TileManager`
* `com.jrealm.game.realm.RealmManagerServer`
* `com.jrealm.game.realm.RealmManagerClient`
* `com.jrealm.game.entity.Enemy`
* `com.jrealm.game.entity.Portal`
* `com.jrealm.game.entity.LootContainer`
* `com.jrealm.game.entity.Player`
* `com.jrealm.game.entity.Bullet`
* `com.jrealm.net.client.SocketClient`
* `com.jrealm.net.client.ClientGameLogic`
* `com.jrealm.net.server.SocketServer`
* `com.jrealm.net.server.ServerGameLogic`
