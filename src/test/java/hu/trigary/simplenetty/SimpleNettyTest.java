package hu.trigary.simplenetty;

import hu.trigary.simplenetty.client.Client;
import hu.trigary.simplenetty.serialization.DataSerializer;
import hu.trigary.simplenetty.server.Server;
import hu.trigary.simplenetty.server.ServerClient;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;

public class SimpleNettyTest {
	@Test
	public void test() throws Throwable {
		final String host = "localhost";
		final int port = 800;
		final String clientMessage = "Hello server!";
		final String serverMessage = "Goodbye client!";
		
		DataSerializer<String> stringDataSerializer = new DataSerializer<String>() {
			@Override
			public byte[] serialize(String data) {
				return data.getBytes(StandardCharsets.US_ASCII);
			}
			
			@Override
			public String deserialize(byte[] bytes) {
				return new String(bytes, StandardCharsets.US_ASCII);
			}
			
			@Override
			public Class<String> getType() {
				return String.class;
			}
		};
		
		Server<ServerClient<String>, String> server = new Server<>(stringDataSerializer, ServerClient::new);
		server.onChannelInitialized(channel -> {
			try {
				SelfSignedCertificate certificate = new SelfSignedCertificate();
				SslContext context = SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey()).build();
				channel.pipeline().addFirst(context.newHandler(channel.alloc()));
			} catch (SSLException | CertificateException e) {
				throw new RuntimeException(e);
			}
		});
		server.onReceived((client, data) -> {
			System.out.println("Server received: " + data);
			Assert.assertEquals(clientMessage, data);
			client.send(serverMessage);
		});
		server.start(null, port);
		
		Client<String> client = new Client<>(stringDataSerializer);
		client.onChannelInitialized(channel -> {
			try {
				SslContext context = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
				channel.pipeline().addFirst(context.newHandler(channel.alloc(), host, port));
			} catch (SSLException e) {
				throw new RuntimeException(e);
			}
		});
		client.onConnected(() -> client.send(clientMessage));
		client.onReceived(data -> {
			System.out.println("Client received: " + data);
			Assert.assertEquals(serverMessage, data);
			try {
				client.disconnect();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		client.onDisconnected(() -> {
			synchronized (this) {
				notify();
			}
		});
		
		for (int i = 0; i < 3; i++) {
			if (!client.connect(host, port, 0)) {
				throw new RuntimeException("Connection timeout");
			}
			synchronized (this) {
				wait();
			}
		}
		
		System.out.println("Shutting down...");
		client.uninitialize();
		server.stop();
	}
}
