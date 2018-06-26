package hu.trigary.simplenettyexample.server;

import hu.trigary.simplenetty.server.ServerClient;
import hu.trigary.simplenettyexample.packet.Packet;

public class ExampleServerClient extends ServerClient<Packet> {
	private String user;
	
	
	
	public boolean isLoggedIn() {
		return user != null;
	}
	
	public String getUser() {
		return user;
	}
	
	
	
	void setUser(String user) {
		this.user = user;
	}
}
