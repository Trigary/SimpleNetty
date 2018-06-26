package hu.trigary.simplenettyexample.packet.action;

import hu.trigary.simplenettyexample.packet.Packet;

public class MessageActionPacket implements Packet {
	private final String message;
	
	public MessageActionPacket(String message) {
		this.message = message;
	}
	
	
	
	public String getMessage() {
		return message;
	}
	
	@Override
	public Type getType() {
		return Type.ACTION_MESSAGE;
	}
}
