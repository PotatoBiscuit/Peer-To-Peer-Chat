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
	public static String name;

	public static void main(String[] args){
		//IP and port number received when running program
		//java Peer IP_Address Port_Number
        if(args.length != 1){
            System.out.println("Usage: java Peer Port_Number");
            System.exit(1);
        }

		System.out.print("What is your name? ");
		name = userInput.nextLine();

		try{
            String hostIP = InetAddress.getLocalHost().getHostAddress(); //Aquire User IP
			InetAddress hostAddress = InetAddress.getByName(hostIP);
			int hostPort = Integer.parseInt(args[0]);//Take port number from command line
            System.out.println("Your IP is: " + hostIP + " and your port number is: " + hostPort);
            Runtime.getRuntime().addShutdownHook(new CloseThread(hostAddress, hostPort)); //Add CloseThread as a shutdown hook
			new SendThread(hostAddress, hostPort).start();  //Start SendThread
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

    //This handles sending disconnect messages to peers when closing the application
    static class CloseThread extends Thread {
        private DatagramPacket leavePacket;
        DatagramSocket dataSocket;
        private byte[] buffer;
        String message;

        public CloseThread(InetAddress senderAddress, int senderPort){
            message = "4:" + senderPort + ":" + senderAddress;
            try{
				dataSocket = new DatagramSocket();
			}catch(IOException e){
				System.out.println("Error:" + e);
			}
        }

        public void run(){
            disconnect();
            for(PeerInfo peer : peerList){
                buffer = new byte[256];
                buffer = message.getBytes();
				leavePacket = new DatagramPacket(buffer, 0, buffer.length, peer.hostIP, peer.portNum);
                try{
    				dataSocket.send(leavePacket);
                }catch(IOException e){
    				System.out.println("Error:" + e);
    			}
                    dataSocket.close();
			}
        }
    }

	//This is the thread that sends the messages to all other peers
	static class SendThread extends Thread{
		private InetAddress receiverAddress;
		private int receiverPort;
		private InetAddress senderAddress;
		private int senderPort;
		private BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		DatagramSocket dataSocket;
		private DatagramPacket packet;
		private byte[] buffer;

		public SendThread(InetAddress newSenderAddress, int newSenderPort){
			senderAddress = newSenderAddress;
			senderPort = newSenderPort;
			try{
				dataSocket = new DatagramSocket();
			}catch(IOException e){
				System.out.println("Error:" + e);
			}
		}

		public void run(){
			try{
				joinNetwork();
				while(stillConnected){
					sendMessage(stdIn.readLine());
				}
			}catch(IOException e){
				System.out.println("Error:" + e);
			}
		}

		public void joinNetwork() throws IOException{ //Send protocol 1 message
			String decision;
			do{
				System.out.print("Would you like to connect to a peer? (Y/N) ");
			}while(!(decision = userInput.nextLine()).equals("Y") && !decision.equals("N"));
			if(decision.equals("Y")){
				System.out.print("Please give us the IP Address: ");
				receiverAddress = InetAddress.getByName(userInput.nextLine());
				System.out.print("Port number to send to: ");
				receiverPort = userInput.nextInt();

				peerList.add(new PeerInfo(receiverAddress, receiverPort));

				String message = "1:" + senderPort + ":" + senderAddress.toString();
				buffer = new byte[256];
				buffer = message.getBytes();
				packet = new DatagramPacket(buffer, 0, buffer.length, receiverAddress, receiverPort);
				dataSocket.send(packet);
			}
		}

		public void sendMessage(String message) throws IOException{	//Send protocol 5 message
			message = "5:" + name + ":" + message;
			buffer = new byte[256];
			buffer = message.getBytes();
			for(PeerInfo peer : peerList){
				packet = new DatagramPacket(buffer, 0, buffer.length, peer.hostIP, peer.portNum);
				dataSocket.send(packet);
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
			;
		}
	}

	//This is the thread that receives messages sent
	static class ReceiveThread extends Thread{
		private int portNumber;
		private DatagramSocket receiveSocket;
		private DatagramSocket responseSocket;
		private DatagramPacket packet;
		private byte[] buffer; //Create buffer to hold messages

		//Receive and store portNumber
		ReceiveThread(int newPortNumber){
			portNumber = newPortNumber;
			try{
				receiveSocket = new DatagramSocket(portNumber);
				responseSocket = new DatagramSocket();
			}catch(IOException e){
				System.out.println("Error:" + e);
			}
		}

		public void run(){
			String newCharacter;
			String[] partsOfMessage;
			try{
				//Create listening port
				while(stillConnected){	//Listen for messages while still connected
					buffer = new byte[256];
					packet = new DatagramPacket(buffer, buffer.length);
					receiveSocket.receive(packet);
					newCharacter = new String(packet.getData(), 0, packet.getLength());
					newCharacter = newCharacter.replace("/", "");
					partsOfMessage = newCharacter.split(":");
					if(partsOfMessage[0].equals("1")){
						broadcastJoin(partsOfMessage[2], partsOfMessage[1]);
					}
					else if(partsOfMessage[0].equals("5")){
						displayMessage(partsOfMessage[1], partsOfMessage[2]);
					}
                    else if(partsOfMessage[0].equals("4")){
                        removePeer(partsOfMessage[2], partsOfMessage[1]);
                    }
				}

			}catch(IOException e){	//If error, tell user
				System.out.println("Error:" + e);
			}
		}

		public void broadcastJoin(String hostIP, String portNum) throws IOException{ //Handle protocol 1 message
			String message = "2:" + portNum + ":" + hostIP;
			buffer = new byte[256];
			buffer = message.getBytes();
			for(PeerInfo peer : peerList){
				packet = new DatagramPacket(buffer, 0, buffer.length, peer.hostIP, peer.portNum);
				responseSocket.send(packet);
			}
			peerList.add(new PeerInfo(InetAddress.getByName(hostIP), Integer.parseInt(portNum)));
		}

		public void displayMessage(String senderName, String senderMessage){ //Handle protocol 5 message
			System.out.println(senderName + ":" + senderMessage);
		}

        public void removePeer(String leavingIP, String leavingPort){ //Handle protocol 4 message
            for(PeerInfo peer : peerList){
                if((peer.hostIP.toString()).equals(leavingIP) && peer.portNum == Integer.parseInt(leavingPort)){
                    peerList.remove(peer);
                    System.out.println("Peer has left the chat");
                }
            }
        }
	}
}
