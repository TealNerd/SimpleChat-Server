package com.biggestnerd.simplechat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;

public class SimpleChatServer {

	private static final int DEFAULT_PORT = 9002;
	private ServerSocket server;
	private HashSet<MessageThread> connections = new HashSet<MessageThread>();
	
	public static void main(String[] args) {
		try {
			new SimpleChatServer(args.length != 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT);
		} catch (NumberFormatException e) {
			new SimpleChatServer(DEFAULT_PORT);
		}
	}
	
	public SimpleChatServer(int port) {
		try {
			server = new ServerSocket(port);
			System.out.println("Server started on port: " + port);
			while(true) {
				Socket socket = server.accept();
				MessageThread thread = new MessageThread(socket);
				thread.start();
				connections.add(thread);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void processMessage(String msg) {
		System.out.println("Processing message: \"" + msg + "\"");
		for(MessageThread mt : connections) {
			mt.sendMessage(msg);
		}
	}
	
	class MessageThread extends Thread {
		
		private Socket socket;
		private String name = "";
		private DataInputStream in = null;
		private DataOutputStream out = null;
		
		public MessageThread(Socket socket) {
			System.out.println("Socket connected at: " + socket.getInetAddress());
			this.socket = socket;
		}
		
		public void run() {
			try {
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			String line;
			while(socket.isConnected()) {
				try {
					line = in.readUTF();
					if(line != null) {
						if(line.startsWith("NAME")) {
							name = line.split("\\:")[1];
							System.out.println("Set name to " + name);
						} else {
							processMessage(name + ": " + line);
						}
					}
				} catch (IOException e) {
					if(e instanceof SocketException) break;
					if(e instanceof EOFException) break;
					e.printStackTrace();
				} catch (Exception e) {
					try {socket.close();} catch (IOException e1) {}
				}
			}
			System.out.println(socket.getInetAddress() + " disconnected");
			connections.remove(this);
		}
		
		public void sendMessage(String msg) {
			try {
				out.writeUTF(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public String getUsername() {
			return name;
		}
		
		public int hashCode() {
			return socket.getInetAddress().hashCode();
		}
		
		public boolean equals(Object other) {
			if(!(other instanceof MessageThread)) return false;
			return ((MessageThread)other).socket.getInetAddress().equals(socket.getInetAddress());
		}
	}
}
