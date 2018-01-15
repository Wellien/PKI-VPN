package fr.smb.univ.acy.iut.webserv;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


public abstract class Server {
	

	private ServerSocket serverSocket;
	
	public Server(InetSocketAddress bindpoint, int timeout) throws IOException {
		try {
			this.serverSocket = new ServerSocket();
			this.serverSocket.bind(bindpoint);
			this.serverSocket.setSoTimeout(timeout);
	
			while (true) {
				try {
					waitForConnection();
				} catch (SocketTimeoutException e) {
					System.out.println("Erreur d'attente sur le server socket.");
				} catch (IOException e) {
					System.out.println("Erreur d'entrée/sortie sur une socket cliente.");
				}
			}
		} catch (IOException e) {
			System.out.println("Erreur d'entrée/sortie sur le server socket.");
		} finally {
			this.serverSocket.close();
		}
	}
	
	public abstract byte[] handle(byte[] request);
	
	public void waitForConnection() throws IOException {
		Socket s =  this.serverSocket.accept();
		new Connection(s, this).start();
	}
	


}
