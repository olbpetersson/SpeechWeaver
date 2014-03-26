package com.siriforreq.speech;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.util.Log;

public class SocketNetworkHelper {
	Socket socket;
	InetAddress iAddr;
	String ip;
	int port;
	PrintWriter out;
	BufferedReader in;
	private boolean isAlive = false;
	static SocketNetworkHelper socketNetworkHelper; 
	String username, password;
	
	private SocketNetworkHelper(String ip,int port) {
			Log.i("ip:port", ip+":"+port);
			this.ip = ip;
			this.port = port;
			if(socket == null || socket.isClosed())
				this.socket = new Socket();
			
	}
	
	public void connect(String username, String password) throws IOException {
			
		
			if(socket.isClosed())
				setIsAlive(false);
				socket = new Socket();

				socket.setTcpNoDelay(true);
				socket.connect(SocketNetworkHelper.getInetSocketAddress(ip, port), 8000);
				out = new PrintWriter(socket.getOutputStream());
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				setIsAlive(true);
				write(username+"<LOGIN>"+password);
				System.out.println("success");

	}
	
	public static SocketNetworkHelper getSocketNetworkHelper(String ip, int port){
		if(socketNetworkHelper == null){
			socketNetworkHelper = new SocketNetworkHelper(ip, port);
		}	
			socketNetworkHelper.setIP(ip);
			socketNetworkHelper.setPort(port);
			return socketNetworkHelper;
	}
	
	public static InetSocketAddress getInetSocketAddress(String ip, int port){
		return new InetSocketAddress(ip, port);
	}
	
	public void write(String send){
		System.out.println("wrote \"" +send +"\" to server");
		out.write(send);
		out.flush();
		System.out.println("flushed");
	}
	
	public void writeStringArray(ArrayList<String> inputResultList, boolean hasTag){
		StringBuilder outStringBuilder = new StringBuilder();
		if(hasTag)
			outStringBuilder.append(inputResultList.get(0));
		for(int i = hasTag ? 1 : 0;
		i < inputResultList.size()-1; i++){
			outStringBuilder.append(inputResultList.get(i)).append("<EOL>");
		}
		outStringBuilder.append(inputResultList.get(inputResultList.size()-1));
		System.out.println("Wrote to server: "+outStringBuilder.toString());
		out.write(outStringBuilder.toString());
		out.flush();
	}
	
	public String read() {
		try{	
			System.out.println("wating for input");
			String returnValue = in.readLine();
			if(returnValue == null){
				setIsAlive(false);
			}
				return returnValue;
			}catch(IOException e){
			e.printStackTrace();
		}
		
		return "";
	}
	
	public void close(){
		System.out.println("CLOSED SOCKET");
		try {
			if(socket.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown()){
				socket.shutdownInput();
				socket.shutdownOutput();
				in.close();
				out.close();
				socket.close();
				setIsAlive(false);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isAlive(){
		return this.isAlive;
	}
	
	private void setIP(String ip){
		this.ip = ip;
	}
	
	private void setPort(int port){
		this.port = port;
	}
	
	private void setIsAlive(boolean state){
		this.isAlive = state;
	}

	public void writeFile(byte[] buffer) throws IOException {
			socket.getOutputStream().write(buffer);		
	}
}