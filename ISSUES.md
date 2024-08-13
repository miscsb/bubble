# Issues

### RedisConnectionException thrown when tests are completed

This seems to be an error with the current Testcontainers setup.

```
...ConnectionWatchdog : Reconnecting, last destination was localhost/127.0.0.1:<port>
...ConnectionWatchdog : Cannot reconnect to [localhost/<unresolved>:<port>]: Connection closed prematurely

io.lettuce.core.RedisConnectionException: Connection closed prematurely
        at io.lettuce.core.protocol.RedisHandshakeHandler.channelInactive(RedisHandshakeHandler.java:91) ~[lettuce-core-6.3.2.RELEASE.jar:6.3.2.RELEASE/8941aea]
```

### Is AGPL too restrictive of a license? Should a simpler license be used?
