# jrealm
### Realm 2 in Java
![alt text](https://i.imgur.com/GPpcD2I.png)
![alt text](https://i.imgur.com/gf2c380.png)
![alt text](https://i.imgur.com/7Z540Mb.png)
### Running
* Server: <br />
`java -jar ./jrealm-{version}-server.jar` <br />
The server requires port 2222 to be available

* Client: <br />
`java -jar ./jrealm-{version}-client.jar {SERVER_ADDRESS} {USERNAME} {CLASS_ID}` <br />
Replace `{SERVER_ADDRESS}` with the IP Address of the server you wish to connect to <br />
Replace `{USERNAME}` with your desired in game username <br />
Replace `{CLASS_ID}` with your desired in game character class:
```	
ROGUE(0),
ARCHER(1),
WIZARD(2),
PRIEST(3),
WARRIOR(4),
KNIGHT(5),
PALLADIN(6);
```
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
