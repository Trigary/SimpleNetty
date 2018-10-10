package hu.trigary.simplenettyexample.client;

import hu.trigary.simplenetty.client.Client;
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
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.util.Scanner;

public class ExampleClient extends Client<Packet> {
	public static void startClient(Scanner scanner, String host, int port) throws InterruptedException {
		System.out.println("Starting client...");
		ExampleClient client = new ExampleClient(scanner, host, port);
		if (!client.connect(host, port, 5000)) {
			System.out.println("Connection timed out, uninitializing...");
			client.uninitialize();
			System.out.println("The client has been uninitialized, exiting...");
			return;
		}
		
		System.out.println("Choose your username:");
		client.send(new LoginActionPacket(scanner.nextLine()));
		synchronized (client) {
			client.wait();
		}
		
		System.out.println("You have logged in, you are now able to send messages.");
		System.out.println("Type /stop to stop.");
		while (true) {
			String input = scanner.nextLine();
			if (input.equalsIgnoreCase("/stop")) {
				break;
			} else {
				client.send(new MessageActionPacket(input));
			}
		}
		
		System.out.println("Uninitializing the client...");
		client.setStopping();
		client.uninitialize();
		System.out.println("The client has been uninitialized, exiting...");
	}
	
	
	
	private final Scanner scanner;
	private final String host;
	private final int port;
	private boolean loggedIn = false;
	private volatile boolean stopping = false;
	
	public ExampleClient(Scanner scanner, String host, int port) {
		super(new PacketSerializer());
		this.scanner = scanner;
		this.host = host;
		this.port = port;
		
		onChannelInitialized(this::onChannelInitialized);
		onReceived(this::onReceived);
		onDisconnected(this::onDisconnected);
	}
	
	
	
	private void onChannelInitialized(SocketChannel channel) {
		try {
			SslContext context = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
			channel.pipeline().addFirst(context.newHandler(channel.alloc(), host, port));
		} catch (SSLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void onReceived(Packet data) {
		if (!loggedIn) {
			if (((LoginResponsePacket) data).isSuccess()) {
				loggedIn = true;
				synchronized (this) {
					notify();
				}
			} else {
				System.out.println("That username is already taken, please enter another one:");
				send(new LoginActionPacket(scanner.nextLine()));
			}
			return;
		}
		
		switch (data.getType()) {
			case EVENT_LOGIN:
				System.out.println("[LOGIN] " + ((LoginEventPacket) data).getUser() + " has logged in.");
				break;
			case EVENT_MESSAGE:
				MessageEventPacket messagePacket = (MessageEventPacket) data;
				System.out.println("[MESSAGE] " + messagePacket.getUser() + " > " + messagePacket.getMessage());
				break;
			case EVENT_DISCONNECT:
				System.out.println("[DISCONNECT] " + ((DisconnectEventPacket) data).getUser() + " has disconnected.");
				break;
		}
	}
	
	private void onDisconnected() {
		if (!stopping) {
			System.out.println("Lost connection to the server.");
		}
	}
	
	
	
	public void setStopping() {
		stopping = true;
	}
}
