package hu.trigary.simplenettyexample.packet;

import hu.trigary.simplenettyexample.packet.action.LoginActionPacket;
import hu.trigary.simplenettyexample.packet.action.MessageActionPacket;
import hu.trigary.simplenettyexample.packet.event.DisconnectEventPacket;
import hu.trigary.simplenettyexample.packet.event.LoginEventPacket;
import hu.trigary.simplenettyexample.packet.event.MessageEventPacket;
import hu.trigary.simplenettyexample.packet.response.LoginResponsePacket;

public interface Packet {
	
	Type getType();
	
	
	
	enum Type {
		ACTION_LOGIN(LoginActionPacket.class),
		ACTION_MESSAGE(MessageActionPacket.class),
		
		RESPONSE_LOGIN(LoginResponsePacket.class),
		
		EVENT_LOGIN(LoginEventPacket.class),
		EVENT_MESSAGE(MessageEventPacket.class),
		EVENT_DISCONNECT(DisconnectEventPacket.class);
		
		
		
		private final Class<? extends Packet> clazz;
		
		Type(Class<? extends Packet> clazz) {
			this.clazz = clazz;
		}
		
		
		
		public Class<? extends Packet> getClazz() {
			return clazz;
		}
	}
}
