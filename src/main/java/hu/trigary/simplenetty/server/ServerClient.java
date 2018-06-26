package hu.trigary.simplenetty.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

/**
 * A server-side class bound to a client which is connected to the server.
 * This class is expected be extended in order to store state information (eg. client ID) and add more functions.
 *
 * @param <D> the type of the data being transmitted
 */
public class ServerClient<D> {
	private volatile ChannelHandlerContext context;
	
	
	
	/**
	 * Asynchronously sends data to the client.
	 *
	 * @param data the data to send
	 */
	public void send(D data) {
		context.writeAndFlush(data);
	}
	
	/**
	 * Asynchronously sends data to the client and closes the connection as soon as the transmission is done.
	 *
	 * @param data the data to send
	 */
	public void sendAndClose(D data) {
		context.writeAndFlush(data).addListener(ChannelFutureListener.CLOSE);
	}
	
	/**
	 * Asynchronously sends data to the client and executes the specified action as soon as the transmission is done.
	 *
	 * @param data the data to send
	 * @param runnable the action to execute
	 */
	public void sendAndThen(D data, Runnable runnable) {
		context.writeAndFlush(data).addListener((ChannelFutureListener) runnable);
	}
	
	
	
	/**
	 * Asynchronously closes the connection with the client.
	 */
	public void close() {
		context.close();
	}
	
	/**
	 * Returns the connection channel's context, allowing direct interaction with Netty.
	 * Null is returned in case the client is no longer connected.
	 *
	 * @return the context or null, if the client is no longer connected
	 */
	public ChannelHandlerContext getContext() {
		return context;
	}
	
	
	
	void setContext(ChannelHandlerContext context) {
		this.context = context;
	}
	
	void sendRaw(byte[] data) {
		context.writeAndFlush(context.alloc().buffer().writeBytes(data));
	}
	
	void sendRawAndClose(byte[] data) {
		context.writeAndFlush(context.alloc().buffer().writeBytes(data))
				.addListener(ChannelFutureListener.CLOSE);
	}
	
	void sendRawAndThen(byte[] data, Runnable runnable) {
		context.writeAndFlush(context.alloc().buffer().writeBytes(data))
				.addListener((ChannelFutureListener) runnable);
	}
}
