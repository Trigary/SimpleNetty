package hu.trigary.simplenettyexample;

import hu.trigary.simplenettyexample.client.ExampleClient;
import hu.trigary.simplenettyexample.server.ExampleServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {
	public static void main(String[] args) throws InterruptedException, UnknownHostException {
		try (Scanner scanner = new Scanner(System.in)) {
			if (isServer(scanner)) {
				ExampleServer.startServer(scanner, 800);
			} else {
				ExampleClient.startClient(scanner, InetAddress.getLocalHost().getHostName(), 800);
			}
		}
	}
	
	private static boolean isServer(Scanner scanner) {
		System.out.println("What do you want to be? [Server | Client]");
		while (true) {
			switch (scanner.nextLine().toLowerCase()) {
				case "sever":
				case "s":
					return true;
				case "client":
				case "c":
					return false;
				default:
					System.out.println("Invalid input!");
			}
		}
	}
}
