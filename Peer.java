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
	public static Scanner userInput = new Scanner(System.in);

	public static void main(String[] args){
		//IP and port number received when running program
		//java Peer IP_Address Port_Number
        if(args.length != 2){
            System.out.println("Usage: java Peer IP_Address Port_Number");
            System.exit(1);
        }
		String hostIP = args[0];	//Take user IP from command line
		try{
			//HostIP converted into address format
			System.out.print("Host IP you would like to send messages to: ");
			InetAddress receiverAddress = InetAddress.getByName(userInput.nextLine());
			System.out.print("Port number to send to: ");
			int receiverPort = userInput.nextInt();
			
			InetAddress hostAddress = InetAddress.getByName(hostIP);
			int hostPort = Integer.parseInt(args[1]);//Take port number from command line
			new SendThread(receiverAddress, receiverPort).start();  //Start SendThread
			new ReceiveThread(hostPort).start();	//Start ReceiveThread
			new IntermediateThread(hostAddress, hostPort).start(); //Start IntermediateThread
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
		private InetAddress receiverAddress;
		private int receiverPort;
		//Receive and store receiverAddress and receiverPort
		SendThread(InetAddress newReceiverAddress, int newReceiverPort){
			receiverAddress = newReceiverAddress;
			receiverPort = newReceiverPort;
		}

		public void run(){
			byte[] buffer = new byte[256];
			DatagramPacket packet;
			String currentMessage;
			System.out.println("This is the thread that sends stuff");
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            try{
				while(true){
					currentMessage = stdIn.readLine();	//Take user input from system
					buffer = currentMessage.getBytes();
					DatagramSocket dataSocket = new DatagramSocket();
					//THING TO CHANGE: This function is going to have to use a linkedlist of addresses
					packet = new DatagramPacket(buffer, 0, buffer.length, receiverAddress, receiverPort);
					dataSocket.send(packet);
				}

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
					System.out.println(newCharacter);
				}

			}catch(IOException e){	//If error, tell user
				System.out.println("Error:" + e);
			}
		}
	}
}
