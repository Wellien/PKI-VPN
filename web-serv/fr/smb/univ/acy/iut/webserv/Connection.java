package fr.smb.univ.acy.iut.webserv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Connection extends Thread {
	
	private Socket socket;
	private Server server;
	
	public Connection (Socket s, Server server) {
		this.socket = s;
		this.server = server;
	}
	
	@Override
    public void run() {
		byte[] request;
		byte[] response;
		try {
			request = read(this.socket);
			response = this.server.handle(request);
			write(socket, response);
		} catch (IOException e) {
			System.out.println("Erreur d'entrée/sortie dans la méthode run de la classe Connection.");
		}
		finally {
			if (this.socket != null) {
				try {
					this.socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
    }
	
    
	private byte[] read(Socket socket) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String l;
		String result = "";
		int content_length = 0;	
		
		while (true) {
			l = br.readLine();
			
			if (l.length() == 0) { 
				break;
			}
			
			if (l.startsWith("Content-Length: ")) {
				System.out.println("Trying to parse content length :" + l.replaceAll("Content-Length: ", ""));
				content_length = Integer.parseInt(l.replaceAll("Content-Length: ", ""));
			}
			
			result += l + "\n";
			
		}
		
		char[] params = new char[content_length];
		br.read(params);
		result += new String(params) + "\n";
		
		System.out.println("Result is finally " + result);
		return result.getBytes();

	}
		
	private void write(Socket socket, byte[] response) throws IOException {
		PrintWriter writer = new PrintWriter(socket.getOutputStream());
		writer.write(new String(response));
		writer.flush();
	}
	
    
}
