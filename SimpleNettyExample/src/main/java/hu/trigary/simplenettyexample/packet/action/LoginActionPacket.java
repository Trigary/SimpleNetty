package hu.trigary.simplenettyexample.packet.action;

import hu.trigary.simplenettyexample.packet.Packet;

public class LoginActionPacket implements Packet {
	private final String user;
	
	public LoginActionPacket(String user) {
		this.user = user;
	}
	
	
	
	public String getUser() {
		return user;
	}
	
	@Override
	public Type getType() {
		return Type.ACTION_LOGIN;
	}
}
