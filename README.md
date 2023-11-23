# jrealm
### Realm 2 in Java
### Running
* Server: <br />
`java -jar ./jrealm-{version}-server.jar` <br />
The server requires port 2222 to be available

* Client: <br />
`java -jar ./jrealm-{version}-client.jar {SERVER_ADDRESS} {USERNAME}` <br />
Replace `{SERVER_ADDRESS}` with the IP Address of the server you wish to connect to <br />
Replace `{USERNAME}` with your desired in game username

# Important classes:

* `com.jrealm.game.states.PlayState`
* `com.jrealm.game.realm.Realm`
* `com.jrealm.game.realm.RealmManagerServer`
* `com.jrealm.game.realm.RealmManagerClient`
* `com.jrealm.game.entity.Enemy`
* `com.jrealm.game.entity.Player`
* `com.jrealm.game.entity.Bullet`
* `com.jrealm.net.client.SocketClient`
* `com.jrealm.net.server.SocketServer`
