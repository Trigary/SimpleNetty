package hu.trigary.simplenettyexample.packet.event;

import hu.trigary.simplenettyexample.packet.Packet;

public class MessageEventPacket implements Packet {
	private final String user;
	private final String message;
	
	public MessageEventPacket(String user, String message) {
		this.user = user;
		this.message = message;
	}
	
	
	
	public String getUser() {
		return user;
	}
	
	public String getMessage() {
		return message;
	}
	
	@Override
	public Type getType() {
		return Type.EVENT_MESSAGE;
	}
}
