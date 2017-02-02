import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.lang.String;
import java.lang.Thread;

public class Peer{
	public static void main(String[] args){
		String hostIP = args[0];
		int portNumber = Integer.parseInt(args[1]);
		try{
			InetAddress hostAddress = InetAddress.getByName(hostIP);
			new SendThread().start();
			new ReceiveThread(hostAddress, portNumber).start();
		}catch(UnknownHostException e){
			System.out.println("Error: Unknown Host");
			System.exit(1);
		}
	}
	
	static class SendThread extends Thread{
		public void run(){
			System.out.println("This is the thread that sends stuff");
		}
	}
	
	static class ReceiveThread extends Thread{
		private InetAddress hostAddress;
		private int portNumber;
		ReceiveThread(InetAddress newHostAddress, int newPortNumber){
			hostAddress = newHostAddress;
			portNumber = newPortNumber;
		}
		public void run(){
			System.out.println("This is the thread that receives stuff");
		}
	}
}