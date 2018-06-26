package hu.trigary.simplenettyexample.packet.event;

import hu.trigary.simplenettyexample.packet.Packet;

public class LoginEventPacket implements Packet {
	private final String user;
	
	public LoginEventPacket(String user) {
		this.user = user;
	}
	
	
	
	public String getUser() {
		return user;
	}
	
	@Override
	public Type getType() {
		return Type.EVENT_LOGIN;
	}
}
