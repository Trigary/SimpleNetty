package hu.trigary.simplenetty.client;

import hu.trigary.simplenetty.serialization.DataSerializer;
import hu.trigary.simplenetty.serialization.PacketDecoder;
import hu.trigary.simplenetty.serialization.PacketEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.internal.PlatformDependent;

import java.util.function.Consumer;

/**
 * A client which can connect to and communicate with a server.
 *
 * @param <D> the type of the data being transmitted
 */
public class Client<D> {
	private final DataSerializer<D> serializer;
	private Bootstrap bootstrap;
	private EventLoopGroup workerGroup;
	private volatile ChannelHandlerContext context;
	private volatile boolean initialized = false;
	
	private volatile Consumer<Bootstrap> onConfigured;
	private volatile Consumer<SocketChannel> onChannelInitialized;
	private volatile Runnable onConnected;
	private volatile Consumer<D> onReceived;
	private volatile Runnable onDisconnected;
	private volatile Consumer<Throwable> onException = Throwable::printStackTrace;
	
	/**
	 * Create a new instance with the specified {@link DataSerializer}.
	 *
	 * @param serializer the serializer and deserializer of the transmitted data
	 */
	public Client(DataSerializer<D> serializer) {
		this.serializer = serializer;
	}
	
	
	
	/**
	 * Called when the {@link Bootstrap} has been configured.
	 *
	 * @param onConfigured the code to execute, can be null
	 */
	public void onConfigured(Consumer<Bootstrap> onConfigured) {
		this.onConfigured = onConfigured;
	}
	
	/**
	 * Called when the channel has been created (leading to the server) and its initialization has been completed.
	 *
	 * @param onChannelInitialized the code to execute, can be null
	 */
	public void onChannelInitialized(Consumer<SocketChannel> onChannelInitialized) {
		this.onChannelInitialized = onChannelInitialized;
	}
	
	/**
	 * Called when the connection has been established and data
	 * transmission between the server and this client becomes possible.
	 *
	 * @param onConnected the code to execute, can be null
	 */
	public void onConnected(Runnable onConnected) {
		this.onConnected = onConnected;
	}
	
	/**
	 * Called when data has been received from the server.
	 *
	 * @param onReceived the code to execute, can be null
	 */
	public void onReceived(Consumer<D> onReceived) {
		this.onReceived = onReceived;
	}
	
	/**
	 * Called when this client gets disconnected from the server.
	 *
	 * @param onDisconnected the code to execute, can be null
	 */
	public void onDisconnected(Runnable onDisconnected) {
		this.onDisconnected = onDisconnected;
	}
	
	/**
	 * Called when an uncaught exception occurs in the pipeline.
	 *
	 * @param onException the code to execute, can be null
	 */
	public void onException(Consumer<Throwable> onException) {
		this.onException = onException;
	}
	
	
	
	/**
	 * Connect to the server synchronously. Once it is completed, the client is ready to send and receive data.
	 *
	 * @param host the address of the server
	 * @param port the port of the server
	 * @param timeoutMillis the timeout for the connection in millis, or a non-positive value for no timeout
	 * @return true if the connection was successful
	 * @throws InterruptedException if the thread gets interrupted while connecting
	 */
	public boolean connect(String host, int port, long timeoutMillis) throws InterruptedException {
		if (!initialized) {
			initialized = true;
			bootstrap = new Bootstrap();
			workerGroup = new NioEventLoopGroup();
			
			bootstrap.group(workerGroup)
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel channel) {
							channel.pipeline().addLast(new PacketDecoder<>(serializer),
									new LengthFieldPrepender(4),
									new PacketEncoder<>(serializer),
									new ClientChannelHandler());
							
							Consumer<SocketChannel> consumer = onChannelInitialized;
							if (consumer != null) {
								consumer.accept(channel);
							}
						}
					})
					.option(ChannelOption.SO_KEEPALIVE, true);
			
			Consumer<Bootstrap> consumer = onConfigured;
			if (consumer != null) {
				consumer.accept(bootstrap);
			}
		}
		
		ChannelFuture future = bootstrap.connect(host, port);
		if (timeoutMillis <= 0) {
			future.sync();
			return true;
		}
		
		boolean inTime = future.await(timeoutMillis);
		if (future.cause() != null) {
			System.out.println("Exception...");
			PlatformDependent.throwException(future.cause());
		}
		return inTime;
	}
	
	/**
	 * Synchronously disconnect from the server.
	 *
	 * @throws InterruptedException if the thread gets interrupted while disconnecting
	 */
	public void disconnect() throws InterruptedException {
		context.close().sync();
	}
	
	/**
	 * Synchronously uninitialize the client, freeing up all resources.
	 *
	 * @throws InterruptedException if the thread gets interrupted while the {@link EventLoopGroup} is being shut down
	 */
	public void uninitialize() throws InterruptedException {
		initialized = false;
		workerGroup.shutdownGracefully().sync();
	}
	
	/**
	 * Returns the connection channel's context, allowing direct interaction with Netty.
	 * Null is returned in case the client is not connected.
	 *
	 * @return the context or null, if the client is not connected
	 */
	public ChannelHandlerContext getContext() {
		return context;
	}
	
	
	
	/**
	 * Asynchronously sends data to the server.
	 *
	 * @param data the data to send
	 */
	public void send(D data) {
		context.writeAndFlush(data);
	}
	
	/**
	 * Asynchronously sends data to the server and closes the connection as soon as the transmission is done.
	 *
	 * @param data the data to send
	 */
	public void sendAndClose(D data) {
		context.writeAndFlush(data).addListener(ChannelFutureListener.CLOSE);
	}
	
	/**
	 * Asynchronously sends data to the server and executes the specified action as soon as the transmission is done.
	 *
	 * @param data the data to send
	 * @param runnable the action to execute
	 */
	public void sendAndThen(D data, Runnable runnable) {
		context.writeAndFlush(data).addListener((ChannelFutureListener) runnable);
	}
	
	
	
	private class ClientChannelHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelActive(ChannelHandlerContext context) {
			Client.this.context = context;
			Runnable runnable = onConnected;
			if (runnable != null) {
				runnable.run();
			}
		}
		
		@Override
		public void channelRead(ChannelHandlerContext context, Object message) {
			Consumer<D> consumer = onReceived;
			if (consumer != null) {
				//noinspection unchecked
				consumer.accept((D) message);
			}
		}
		
		@Override
		public void channelInactive(ChannelHandlerContext context) {
			Client.this.context = null;
			Runnable runnable = onDisconnected;
			if (runnable != null) {
				runnable.run();
			}
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
			Consumer<Throwable> consumer = onException;
			if (consumer != null) {
				consumer.accept(cause);
			}
		}
	}
}
