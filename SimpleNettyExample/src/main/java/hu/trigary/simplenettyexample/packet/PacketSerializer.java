package hu.trigary.simplenettyexample.packet;

import com.google.gson.Gson;
import hu.trigary.simplenetty.serialization.DataSerializer;

import java.nio.charset.StandardCharsets;

public class PacketSerializer implements DataSerializer<Packet> {
	private static final Gson GSON = new Gson();
	private static final Packet.Type[] PACKET_TYPES = Packet.Type.values();
	
	@Override
	public byte[] serialize(Packet data) {
		byte[] json = GSON.toJson(data).getBytes(StandardCharsets.US_ASCII);
		byte[] bytes = new byte[json.length + 1];
		bytes[0] = (byte) data.getType().ordinal();
		System.arraycopy(json, 0, bytes, 1, json.length);
		return bytes;
	}
	
	@Override
	public Packet deserialize(byte[] bytes) {
		return GSON.fromJson(
				new String(bytes, 1, bytes.length - 1, StandardCharsets.US_ASCII),
				PACKET_TYPES[bytes[0]].getClazz()
		);
	}
	
	@Override
	public Class<Packet> getType() {
		return Packet.class;
	}
}
