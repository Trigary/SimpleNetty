package hu.trigary.simplenettyexample.server;

import hu.trigary.simplenetty.server.Server;
import hu.trigary.simplenettyexample.packet.Packet;
import hu.trigary.simplenettyexample.packet.PacketSerializer;
import hu.trigary.simplenettyexample.packet.action.LoginActionPacket;
import hu.trigary.simplenettyexample.packet.action.MessageActionPacket;
import hu.trigary.simplenettyexample.packet.event.DisconnectEventPacket;
import hu.trigary.simplenettyexample.packet.event.LoginEventPacket;
import hu.trigary.simplenettyexample.packet.event.MessageEventPacket;
import hu.trigary.simplenettyexample.packet.response.LoginResponsePacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ExampleServer extends Server<ExampleServerClient, Packet> {
	public static void startServer(Scanner scanner, int port) throws InterruptedException {
		System.out.println("Starting server...");
		ExampleServer server = new ExampleServer();
		server.start(null, port);
		System.out.println("The server has been started, it is now ready to receive connections.");
		
		System.out.println("Type 'stop' to shut down the server.");
		while (true) {
			if (!scanner.nextLine().equalsIgnoreCase("stop")) {
				continue;
			}
			
			System.out.println("Stopping the server...");
			server.stop();
			System.out.println("The server has been stopped, exiting...");
			return;
		}
	}
	
	
	
	private final Map<String, ExampleServerClient> clients = new HashMap<>();
	
	public ExampleServer() {
		super(new PacketSerializer(), ExampleServerClient::new);
		onChannelInitialized(this::onChannelInitialized);
		onConnected(this::onConnected);
		onReceived(this::onReceived);
		onDisconnected(this::onDisconnected);
	}
	
	
	
	private void onChannelInitialized(SocketChannel channel) {
		try {
			SelfSignedCertificate certificate = new SelfSignedCertificate();
			SslContext context = SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey()).build();
			channel.pipeline().addFirst(context.newHandler(channel.alloc()));
		} catch (SSLException | CertificateException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void onConnected(ExampleServerClient client) {
		System.out.println("A new client connected.");
	}
	
	private void onReceived(ExampleServerClient client, Packet data) {
		System.out.println("Received packet of type: " + data.getType());
		
		synchronized (clients) {
			if (client.isLoggedIn()) {
				if (data.getType() == Packet.Type.ACTION_MESSAGE) {
					String message = ((MessageActionPacket) data).getMessage();
					clients.remove(client.getUser());
					sendTo(new MessageEventPacket(client.getUser(), message), clients.values());
					clients.put(client.getUser(), client);
				}
			} else {
				if (data.getType() == Packet.Type.ACTION_LOGIN) {
					LoginActionPacket loginPacket = (LoginActionPacket) data;
					if (clients.containsKey(loginPacket.getUser())) {
						client.send(new LoginResponsePacket(false));
					} else {
						client.setUser(loginPacket.getUser());
						sendTo(new LoginEventPacket(client.getUser()), clients.values());
						clients.put(client.getUser(), client);
						client.send(new LoginResponsePacket(true));
					}
				}
			}
		}
	}
	
	private void onDisconnected(ExampleServerClient client) {
		System.out.println("A client disconnected.");
		synchronized (clients) {
			if (client.isLoggedIn()) {
				clients.remove(client.getUser());
				sendTo(new DisconnectEventPacket(client.getUser()), clients.values());
			}
		}
	}
}
