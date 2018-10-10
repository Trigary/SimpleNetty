# SimpleNetty [![](https://jitpack.io/v/Trigary/SimpleNetty.svg)](https://jitpack.io/#Trigary/SimpleNetty)

A wrapper around the Netty framework, which makes it easy to use
while maintaining its most powerful aspects:
it is asynchronous, event driven and highly performant.

## Main differences

There are three main features this wrapper adds to Netty:
 - Straight forward server/client initialization and
 binding/connecting
 - An extendable class instance is bound to each connected client
 allowing easy state management (eg. login checks)
 - Provide the base class of all transmitted data and a
 serializer, deserializer for it and worry about
 bytes, serialization no more
 
## Switching to / from Netty

Switching between plain Netty and this wrapper is extremely easy.
The same thread model is used, there is no need to redo the event
listeners. There is always a simple way in this wrapper to access
Netty directly, giving you the same freedom as Netty provides.

## Documentation

 - [JavaDocs](http://trigary.hu/javadocs/simple-netty/)
 - [Server reference](server.md)
 - [Client reference](client.md)

## Importing

You can add SimpleNetty as a dependency as follows:

```xml
<repositories>
  <repository>
   <id>jitpack.io</id>
   <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
 <dependency>
  <groupId>com.github.Trigary</groupId>
  <artifactId>SimpleNetty</artifactId>
  <version>1.0</version>
 </dependency>
</dependencies>
```

Please note that Netty is not shaded into SimpleNetty,
you have to add it yourself.

## Example

You can check out the SimpleNettyExample folder for a
minimalistic, but complete chat client-server implementation.

The following tiny code snippet aims to show you what it's like
to be using SimpleNetty. It's a simple text ECHO server.

```java
//A serializer, deserializer for the type of the transmitted data: String
DataSerializer<String> stringSerializer = new DataSerializer<String>() {
	@Override
	public byte[] serialize(String data) {
		return data.getBytes(StandardCharsets.US_ASCII);
	}
	
	@Override
	public String deserialize(byte[] bytes) {
		return new String(bytes, StandardCharsets.US_ASCII);
	}
	
	@Override
	public Class<String> getType() {
		return String.class;
	}
};

//Instantiate and initialize a new Server.
//ServerClient is the class which is bound to all connected clients.
Server<ServerClient<String>, String> server = new Server<>(stringSerializer, ServerClient::new);
server.onReceived((client, data) -> client.send(data));

//Bind the server to no specified address, but to the port 800.
server.start(null, 800);

//Instantiate and initialize a new Client.
Client<String> client = new Client<>(stringSerializer);
client.onConnected(() -> client.send("Hello Server!"));
client.onReceived(System.out::println);

//Connect to the localhost address on the port 800 without any timeout.
client.connect("localhost", 800, 0);
```

##

This project was inspired by
[SimpleNet](https://github.com/jhg023/SimpleNet/),
ty [Jacob](https://github.com/jhg023)
