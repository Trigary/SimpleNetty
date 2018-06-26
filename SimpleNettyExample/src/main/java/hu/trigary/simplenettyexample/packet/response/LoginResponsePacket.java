package hu.trigary.simplenettyexample.packet.response;

import hu.trigary.simplenettyexample.packet.Packet;

public class LoginResponsePacket implements Packet {
	private final boolean success;
	
	public LoginResponsePacket(boolean success) {
		this.success = success;
	}
	
	
	
	public boolean isSuccess() {
		return success;
	}
	
	@Override
	public Type getType() {
		return Type.RESPONSE_LOGIN;
	}
}
