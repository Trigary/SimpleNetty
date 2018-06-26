package hu.trigary.simplenettyexample.packet.event;

import hu.trigary.simplenettyexample.packet.Packet;

public class DisconnectEventPacket implements Packet {
	private final String user;
	
	public DisconnectEventPacket(String user) {
		this.user = user;
	}
	
	
	
	public String getUser() {
		return user;
	}
	
	@Override
	public Type getType() {
		return Type.EVENT_DISCONNECT;
	}
}
