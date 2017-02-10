import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.String;
import java.lang.Thread;
import java.util.LinkedList;
import java.util.Scanner;


public class Peer{
	public static LinkedList<PeerInfo> peerList = new LinkedList<PeerInfo>();
    public static boolean stillConnected = true;

	public static void main(String[] args){
		//IP and port number received when running program
		//java Peer IP_Address Port_Number
        if(args.length != 2){
            System.out.println("Usage: java Peer IP_Address Port_Number");
            System.exit(1);
        }
		String hostIP = args[0];	//Take user IP from command line
		int portNumber = Integer.parseInt(args[1]);//Take port number from command line
		try{
			//HostIP converted into address format
			InetAddress hostAddress = InetAddress.getByName(hostIP);
			new SendThread(hostAddress, portNumber).start();  //Start SendThread
			new ReceiveThread(portNumber).start();	//Start ReceiveThread
			new IntermediateThread(hostAddress, portNumber).start(); //Start IntermediateThread
		}catch(UnknownHostException e){	//Just throw some generic error if problem
			System.out.println("Error: Unknown Host");
			System.exit(1);
		}
	}

    public static void disconnect(){
        stillConnected = false;
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
			byte[] buffer = new byte[256];
			byte[] ender = new byte[1];
			DatagramPacket packet;
			String currentMessage;
			System.out.println("This is the thread that sends stuff");
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            try{
    			currentMessage = stdIn.readLine();	//Take user input from system
    			buffer = currentMessage.getBytes();
    			ender[0] = (byte) '\n';
    			DatagramSocket dataSocket = new DatagramSocket(portNumber);
    			//THING TO CHANGE: This function is going to have to use a linkedlist of addresses
    			for(int i = 0; i < buffer.length; i++){
    				packet = new DatagramPacket(buffer, i, 1, hostAddress, portNumber);
    				dataSocket.send(packet);
    			}

    			packet = new DatagramPacket(ender, 0, 1, hostAddress, portNumber);
    			dataSocket.send(packet);
            }catch(IOException e){ //If error, tell user
                System.out.println("Error:" + e);
            }

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
			String newCharacter;
			DatagramPacket packet;
			byte[] buffer = new byte[256]; //Create buffer to hold messages
			try{
				//Create listening port
				DatagramSocket dataSocket = new DatagramSocket(portNumber);
				while(stillConnected){	//Listen for messages while still connected
					packet = new DatagramPacket(buffer, buffer.length);
					dataSocket.receive(packet);
					newCharacter = new String(packet.getData(), 0, packet.getLength());
					System.out.print(newCharacter);
				}

			}catch(IOException e){	//If error, tell user
				System.out.println("Error:" + e);
			}
		}
	}
}
