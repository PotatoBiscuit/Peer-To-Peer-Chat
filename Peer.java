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
		//IP and port number received when running program
		//java Peer IP_Address Port_Number
		String hostIP = args[0];	//Take user IP from command line
		int portNumber = Integer.parseInt(args[1]);//Take port number from command line
		try{
			//HostIP converted into address format
			InetAddress hostAddress = InetAddress.getByName(hostIP);
			new SendThread(hostAddress, portNumber).start();//Start SendThread
			new ReceiveThread(portNumber).start();	//Start ReceiveThread
		}catch(UnknownHostException e){	//Just throw some generic error if problem
			System.out.println("Error: Unknown Host");
			System.exit(1);
		}
	}
	
	//This is the thread that sends the messages to all other peers
	static class SendThread extends Thread{
		private InetAddress hostAddress;
		private int portNumber;
		//Receive and store hostAddress and portNumber
		SendThread(InetAddress newHostAddress, int newPortNumber){
			hostAddress = newHostAddress;
			portNumber = newPortNumber;
		}
		public void run(){
			System.out.println("This is the thread that sends stuff");
		}
	}
	
	//This thread will help Receive Thread handle join and leave requests
	static class IntermediateThread extends Thread{
		private InetAddress hostAddress;
		private int portNumber;
		IntermediateThread(InetAddress newHostAddress, int newPortNumber){
			hostAddress = newHostAddress;
			portNumber = newPortNumber;
		}
		public void run(){
			System.out.println("This is a helper thread");
		}
	}
	
	//This is the thread that receives messages sent
	static class ReceiveThread extends Thread{
		private int portNumber;
		//Receive and store portNumber
		ReceiveThread(int newPortNumber){
			portNumber = newPortNumber;
		}
		public void run(){
			System.out.println("This is the thread that receives stuff");
			DatagramPacket packet;
			//Create buffer to hold messages
			byte[] buffer = new byte[100];
			try{
				//Create listening port
				DatagramSocket dataSocket = new DatagramSocket(portNumber);
				while(true){	//Listen for messages infinitely
					packet = new DatagramPacket(buffer, buffer.length);
					dataSocket.receive(packet);
				}
				
			}catch(IOException e){	//If error, tell user
				System.out.println("Error:" + e);
			}
		}
	}
}