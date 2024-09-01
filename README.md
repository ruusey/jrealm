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

### Game Controls
* **W** - Up <br/>
* **A** - Left <br/>
* **S** - Down <br/>
* **D** - Right <br/>

* **1-8** - Consume/Equip corresponding inventory slot <br/>

* **Left Click** - Shoot/Pick up loot <br/>
* **Right Click** - Use Ability/Drop item <br/>

* **F1** - Teleport to vault (safe zone) <br/>
* **F2** - Use nearest portal <br/>

* **Enter** - Chat/Use command
### Running
#### *NOTE: As of release 0.3.0 You are now required to run JRealm-Data alongside JRealm-Server see: https://github.com/ruusey/jrealm-data*

* **General**: <br/>
```java -jar ./jrealm-{version}.jar {-client | -server | -embedded} {SERVER_ADDR} {PLAYER_EMAIL} {PLAYER_PASSWORD} {CHARACTER_UUID}```

* **Server**: <br />
```java -jar ./jrealm-{version}.jar -server {SERVER_ADDR}``` <br />
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

### Available Scripted Behaviors
**JRealm** provides developers with an ever expanding arsenal of tools for implementing game content. The existing toolkit is 
centered around providing script-like features for modifying the games state and data on the server side. 
The current script features are:

### Enemy Scripts
EnemyScripts are small classes that currently allow a developer to implement the `attack` method to provide custom attack behavior. EnemyScripts
will replace the target enemies default attack pattern with the contents of your script. EnemyScripts are run concurrently and thus support delays and
long running attack patterns. Any class extending `EnemyScriptBase` will be loaded as an EnemyScript at runtime.

**Example**
```java
public class Enemy10Script extends EnemyScriptBase {
    // Default constructor
    public Enemy10Script(RealmManagerServer mgr) {
        super(mgr);
    }
    
    // Target enemy ID
    @Override
    public int getTargetEnemyId() {
        return 10;
    }
    
    @Override
    public void attack(Realm targetRealm, Enemy enemy, Player targetPlayer) throws Exception {
    
        Player target = targetPlayer;
        Vector2f dest = target.getBounds().getPos().clone(target.getSize() / 2, target.getSize() / 2);
    
        Vector2f source = enemy.getPos().clone(target.getSize() / 2, target.getSize() / 2);
        float angle = Bullet.getAngle(source, dest);
        // Get the projectiles for attack ID 2
        ProjectileGroup group = GameDataManager.PROJECTILE_GROUPS.get(2);
        Projectile p = group.getProjectiles().get(0);
        // Create two enemy projectiles with the given data with a 100ms delay in between
        this.createProjectile(p, targetRealm.getRealmId(), target.getId(), source.clone(), angle, group);
        this.sleep(100);
        this.createProjectile(p, targetRealm.getRealmId(), target.getId(), source.clone(), angle, group);
    }
}
```

### UseableItem Scripts
UseableItem scripts are small classes that currently allow developers to implement custom item on use and on consume behavior. Currently useable item scripts allow the 
developer to implement the `invokeUseItem` and `invokeItemAbility` which will respectively override the behavior of an items on-equip action and its use while equipped in the
players ability slot. Any class extending `UseableItemScriptBase` will be loaded as a UseableItem script at runtime.

**Example**
```java 
// Item script that adds the ability items effect
// to surrounding players
public class Item156Script extends UseableItemScriptBase{
    
    public Item156Script(RealmManagerServer mgr) {
        super(mgr);
    }
    
    @Override
    public void invokeUseItem(Realm targetRealm, Player player, GameItem item) {
    }
    
    @Override
    public void invokeItemAbility(Realm targetRealm, Player player, GameItem abilityItem) {
        for (final Player other : targetRealm
                .getPlayersInBounds(targetRealm.getTileManager().getRenderViewPort(player))) {
            other.addEffect(abilityItem.getEffect().getEffectId(), abilityItem.getEffect().getDuration());
        }
    }
    
    @Override
    public int getTargetItemId() {
        return 156;
    }
}
```

### Terrain Decorator Scripts
TerrainDecorator scripts are small classes that currently developers to implement custom terrain post processing for Realms during their generation process. TerrainDecorators currently
allow the developer to implement the `decorate` method to modify world tiles, spawn enemies or generate structures. Any class extending `RealmDecoratorBase` will be loaded as a TerrainDecorator
script at runtime.

**Example**
```java
// Creates slowing water pool decorations in the Beach Realm
public class Beach0Decorator extends RealmDecoratorBase {
    private static final Integer MIN_WATER_POOL_COUNT = 15;
    private static final Integer MAX_WATER_POOL_COUNT = 25;
    private static final TileModel WATER_TILE = GameDataManager.TILES.get(41);
    private static final TileModel WATER_TILE_DEEP = GameDataManager.TILES.get(42);
    
    public Beach0Decorator(RealmManagerServer mgr) {
        super(mgr);
    }
    
    @Override
    public void decorate(final Realm input) {
        for (int i = 0; i < (Beach0Decorator.MIN_WATER_POOL_COUNT + Realm.RANDOM
                .nextInt(Beach0Decorator.MAX_WATER_POOL_COUNT - Beach0Decorator.MIN_WATER_POOL_COUNT)); i++) {
            final Vector2f pos = input.getTileManager().randomPos();
            final TileMap baseLayer = input.getTileManager().getBaseLayer();
            final int centerX = (int) (pos.x / baseLayer.getTileSize());
            final int centerY = (int) (pos.y / baseLayer.getTileSize());
    
            baseLayer.setBlockAt(centerX, centerY, (short) Beach0Decorator.WATER_TILE_DEEP.getTileId(),
                    Beach0Decorator.WATER_TILE_DEEP.getData());
            baseLayer.setBlockAt(centerX, (centerY - 1) > -1 ? centerY - 1 : 0,
                    (short) Beach0Decorator.WATER_TILE.getTileId(),
                    Beach0Decorator.WATER_TILE.getData());
            baseLayer.setBlockAt(centerX,
                    (centerY + 1) >= baseLayer.getHeight() ? baseLayer.getHeight() - 1 : centerY + 1,
                            (short) Beach0Decorator.WATER_TILE.getTileId(),
                            Beach0Decorator.WATER_TILE.getData());
            baseLayer.setBlockAt((centerX - 1) > -1 ? centerX - 1  : 0, centerY, (short) Beach0Decorator.WATER_TILE.getTileId(),
                    Beach0Decorator.WATER_TILE.getData());
            baseLayer.setBlockAt((centerX + 1) >= baseLayer.getWidth() ? baseLayer.getWidth()-1 : centerX + 1 , centerY, (short) Beach0Decorator.WATER_TILE.getTileId(),
                    Beach0Decorator.WATER_TILE.getData());
    
        }
    }
    
    @Override
    public Integer getTargetMapId() {
        return 2;
    }
}
```


# Important classes:

* `com.jrealm.game.states.PlayState`
* `com.jrealm.game.realm.Realm`
* `com.jrealm.game.realm.tile.TileManager`
* `com.jrealm.net.server.ServerCommandHandler`
* `com.jrealm.net.server.ServerItemHandler`
* `com.jrealm.net.server.ProcessingThread`
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
