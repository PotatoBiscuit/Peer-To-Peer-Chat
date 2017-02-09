import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.lang.String;
import java.lang.Thread;
import java.io.IOException;

//New comment

public class Peer{
	public static void main(String[] args){
		String hostIP = args[0];
		int portNumber = Integer.parseInt(args[1]);
		try{
			InetAddress hostAddress = InetAddress.getByName(hostIP);
			new SendThread(hostAddress, portNumber).start();
			new ReceiveThread(portNumber).start();
		}catch(UnknownHostException e){
			System.out.println("Error: Unknown Host");
			System.exit(1);
		}
	}
	
	static class SendThread extends Thread{
		private InetAddress hostAddress;
		private int portNumber;
		SendThread(InetAddress newHostAddress, int newPortNumber){
			hostAddress = newHostAddress;
			portNumber = newPortNumber;
		}
		public void run(){
			System.out.println("This is the thread that sends stuff");
		}
	}
	
	static class ReceiveThread extends Thread{
		private int portNumber;
		ReceiveThread(int newPortNumber){
			portNumber = newPortNumber;
		}
		public void run(){
			System.out.println("This is the thread that receives stuff");
			DatagramPacket packet;
			byte[] buffer = new byte[100];
			try{
				DatagramSocket dataSocket = new DatagramSocket(portNumber);
				while(true){
					packet = new DatagramPacket(buffer, buffer.length);
					dataSocket.receive(packet);
				}
				
			}catch(IOException e){
				System.out.println("Error:" + e);
			}
		}
	}
}