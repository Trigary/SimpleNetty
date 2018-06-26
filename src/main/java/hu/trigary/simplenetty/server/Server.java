package hu.trigary.simplenetty.server;

import hu.trigary.simplenetty.serialization.DataSerializer;
import hu.trigary.simplenetty.serialization.PacketDecoder;
import hu.trigary.simplenetty.serialization.PacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A server which can accept connections and communicate with its clients.
 *
 * @param <C> the type of the object which is bound to all connected clients
 * @param <D> the type of the data being transmitted
 */
public class Server<C extends ServerClient<D>, D> {
	private final Set<C> clients = new HashSet<>();
	private final DataSerializer<D> serializer;
	private final Supplier<C> clientSupplier;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	
	private volatile Consumer<ServerBootstrap> onConfigured;
	private volatile Consumer<SocketChannel> onChannelInitialized;
	private volatile Consumer<C> onConnected;
	private volatile BiConsumer<C, D> onReceived;
	private volatile Consumer<C> onDisconnected;
	private volatile BiConsumer<C, Throwable> onException = (client, cause) -> cause.printStackTrace();
	
	/**
	 * Create a new instance with the specified {@link DataSerializer} and {@link ServerClient} instantiator.
	 *
	 * @param serializer the serializer and deserializer of the transmitted data
	 * @param clientSupplier the supplier which creates {@link C} instances
	 */
	public Server(DataSerializer<D> serializer, Supplier<C> clientSupplier) {
		this.serializer = serializer;
		this.clientSupplier = clientSupplier;
	}
	
	
	
	/**
	 * Called when the {@link ServerBootstrap} has been configured.
	 *
	 * @param onConfigured the code to execute, can be null
	 */
	public void onConfigured(Consumer<ServerBootstrap> onConfigured) {
		this.onConfigured = onConfigured;
	}
	
	/**
	 * Called when a new channel has been created (leading to a client) and its initialization has been completed.
	 *
	 * @param onChannelInitialized the code to execute, can be null
	 */
	public void onChannelInitialized(Consumer<SocketChannel> onChannelInitialized) {
		this.onChannelInitialized = onChannelInitialized;
	}
	
	/**
	 * Called when a new client has connected and is not ready to receive data.
	 *
	 * @param onConnected the code to execute, can be null
	 */
	public void onConnected(Consumer<C> onConnected) {
		this.onConnected = onConnected;
	}
	
	/**
	 * Called when data has been received from a client.
	 *
	 * @param onReceived the code to execute, can be null
	 */
	public void onReceived(BiConsumer<C, D> onReceived) {
		this.onReceived = onReceived;
	}
	
	/**
	 * Called when a client has disconnected.
	 *
	 * @param onDisconnected the code to execute, can be null
	 */
	public void onDisconnected(Consumer<C> onDisconnected) {
		this.onDisconnected = onDisconnected;
	}
	
	/**
	 * Called when an uncaught exception occurs in the client's pipeline.
	 *
	 * @param onException the code to execute, can be null
	 */
	public void onException(BiConsumer<C, Throwable> onException) {
		this.onException = onException;
	}
	
	
	
	/**
	 * Starts the server synchronously. Once it is completed, the server is ready to receive connections.
	 *
	 * @param host the address of the server, can be null
	 * @param port the port of the server
	 * @throws InterruptedException if the thread gets interrupted while
	 * the {@code host} and {@code port} are being bound
	 */
	public void start(String host, int port) throws InterruptedException {
		ServerBootstrap bootstrap = new ServerBootstrap();
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		
		bootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel channel) {
						channel.pipeline().addLast(new PacketDecoder<>(serializer),
								new LengthFieldPrepender(4),
								new PacketEncoder<>(serializer),
								new ServerChannelHandler());
						
						Consumer<SocketChannel> consumer = onChannelInitialized;
						if (consumer != null) {
							consumer.accept(channel);
						}
					}
				})
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true);
		
		Consumer<ServerBootstrap> consumer = onConfigured;
		if (consumer != null) {
			consumer.accept(bootstrap);
		}
		
		ChannelFuture future = host == null ? bootstrap.bind(port) : bootstrap.bind(host, port);
		future.sync();
	}
	
	/**
	 * Stops the server synchronously, freeing up all resources.
	 *
	 * @throws InterruptedException if the thread gets interrupted while the {@link EventLoopGroup}s are being shut down
	 */
	public void stop() throws InterruptedException {
		synchronized (clients) {
			clients.clear();
		}
		bossGroup.shutdownGracefully().sync();
		workerGroup.shutdownGracefully().sync();
	}
	
	
	
	/**
	 * Send the specified data to the specified clients.
	 * The data is only serialized once, therefore using this method is better than
	 * calling {@link ServerClient#send(Object)} on each client.
	 *
	 * @param data the data to send
	 * @param clients the recipients
	 */
	public void sendTo(D data, Collection<C> clients) {
		byte[] serialized = serializer.serialize(data);
		clients.forEach(client -> client.sendRaw(serialized));
	}
	
	/**
	 * Send the specified data to the specified clients,
	 * while also closing the connections directly after sending.
	 * The data is only serialized once, therefore using this method is better than
	 * calling {@link ServerClient#send(Object)} on each client.
	 *
	 * @param data the data to send
	 * @param clients the recipients
	 */
	public void sendToAndClose(D data, Collection<C> clients) {
		byte[] serialized = serializer.serialize(data);
		clients.forEach(client -> client.sendRawAndClose(serialized));
	}
	
	/**
	 * Send the specified data to the specified clients,
	 * while also executing the specified action directly after sending.
	 * The data is only serialized once, therefore using this method is better than
	 * calling {@link ServerClient#send(Object)} on each client.
	 *
	 * @param data the data to send
	 * @param clients the recipients
	 * @param runnable the action to execute
	 */
	public void sendToAndThen(D data, Collection<C> clients, Runnable runnable) {
		byte[] serialized = serializer.serialize(data);
		clients.forEach(client -> client.sendRawAndThen(serialized, runnable));
	}
	
	
	
	/**
	 * Gets all connected clients.
	 *
	 * @return all connected clients
	 */
	public Collection<C> getAllClients() {
		synchronized (clients) {
			return new ArrayList<>(clients);
		}
	}
	
	/**
	 * Gets all connected clients, excluding the specified one.
	 *
	 * @param excluding the client to exclude
	 * @return all connected clients, excluding one
	 */
	public Collection<C> getAllClientsExcept(C excluding) {
		Set<C> set;
		synchronized (clients) {
			set = new HashSet<>(clients);
		}
		set.remove(excluding);
		return set;
	}
	
	/**
	 * Gets all connected clients, excluding the specified ones.
	 *
	 * @param excluding the clients to exclude
	 * @return all connected clients, excluding some
	 */
	public Collection<C> getAllClientsExcept(Collection<C> excluding) {
		Set<C> set;
		synchronized (clients) {
			set = new HashSet<>(clients);
		}
		set.removeAll(excluding);
		return set;
	}
	
	
	
	private class ServerChannelHandler extends ChannelInboundHandlerAdapter {
		private C client;
		
		@Override
		public void channelActive(ChannelHandlerContext context) {
			client = clientSupplier.get();
			client.setContext(context);
			synchronized (clients) {
				clients.add(client);
			}
			Consumer<C> consumer = onConnected;
			if (consumer != null) {
				consumer.accept(client);
			}
		}
		
		@Override
		public void channelRead(ChannelHandlerContext context, Object message) {
			BiConsumer<C, D> consumer = onReceived;
			if (consumer != null) {
				//noinspection unchecked
				consumer.accept(client, (D) message);
			}
		}
		
		@Override
		public void channelInactive(ChannelHandlerContext context) {
			synchronized (clients) {
				clients.remove(client);
			}
			Consumer<C> consumer = onDisconnected;
			if (consumer != null) {
				consumer.accept(client);
			}
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
			BiConsumer<C, Throwable> consumer = onException;
			if (consumer != null) {
				consumer.accept(client, cause);
			}
		}
	}
}
