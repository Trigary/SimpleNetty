package hu.trigary.simplenetty.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Connects the {@link DataSerializer} to Netty's {@link MessageToByteEncoder}.
 */
public class PacketEncoder<D> extends MessageToByteEncoder<D> {
	private final DataSerializer<D> serializer;
	
	public PacketEncoder(DataSerializer<D> serializer) {
		super(serializer.getType());
		this.serializer = serializer;
	}
	
	
	
	@Override
	protected void encode(ChannelHandlerContext context, D data, ByteBuf outputBuffer) {
		outputBuffer.writeBytes(serializer.serialize(data));
	}
}
