//Code created by Michael Ortega, Erik Dixon, and Rex

/*The following import statements make it so that we
can use classes for things like reading from the command
line, sending information to and from sockets, and storing
IP addresses as well as other information*/
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

public class Peer{	//Our main peer class
	/*The following fields are as follows: List of Peer information in the network,
	boolean value for whether or not we're connected, object variable to prompt user for input,
	name of the Peer, and a mutex variable*/
	public static LinkedList<PeerInfo> peerList = new LinkedList<PeerInfo>();
    public static boolean stillConnected = true;
	public static Scanner userInput = new Scanner(System.in);
	public static String name;
	public static boolean mutex = false;

	public static void main(String[] args){
		//Port number received when running program
		//java Peer Port_Number
        if(args.length != 1){
            System.out.println("Usage: java Peer Port_Number");
            System.exit(1);
        }
		//Receive name from user
		System.out.print("What is your name? ");
		name = userInput.nextLine();

		try{
            String hostIP = InetAddress.getLocalHost().getHostAddress(); //Aquire User IP
			InetAddress hostAddress = InetAddress.getByName(hostIP);
			int hostPort = Integer.parseInt(args[0]);//Take port number from command line
            System.out.println("Your IP is: " + hostIP + " and your port number is: " + hostPort);
            Runtime.getRuntime().addShutdownHook(new CloseThread(hostAddress, hostPort)); //Add CloseThread as a shutdown hook
			new SendThread(hostAddress, hostPort).start();  //Start SendThread
			new ReceiveThread(hostIP, hostPort).start();	//Start ReceiveThread
		}catch(UnknownHostException e){	//Just throw some generic error if problem
			System.out.println("Error: Unknown Host");
			System.exit(1);
		}
	}
	
	public static void lockMutex(){	//Function to lock our mutex
		while(mutex);
		mutex = true;
	}
	
	public static void unlockMutex(){ //Function to unlock our mutex
		mutex = false;
	}

	//Function to change boolean variable showing if we are connected
    public static void disconnect(){
        stillConnected = false;
    }

    //This handles sending disconnect messages to peers when closing the application
    static class CloseThread extends Thread {
		/*Variables to hold packet data, our sending socket,
		message buffer, and message variable*/
        private DatagramPacket leavePacket;
        DatagramSocket dataSocket;
        private byte[] buffer;
        String message;

		//Create leave request (Protocol 4) to be sent to Peers
        public CloseThread(InetAddress senderAddress, int senderPort){
            message = "4:" + senderPort + ":" + senderAddress;
            try{
				dataSocket = new DatagramSocket(); //Create sending socket
			}catch(IOException e){
				System.out.println("Error:" + e);
			}
        }

        public void run(){ //Send Protocol 4 message before closing
            buffer = new byte[256];
			buffer = message.getBytes();	//Store

			lockMutex();	//Lock mutex to protect our peerList variable
            for(PeerInfo peer : peerList){ //Loops through each peer in the current list to send them a leave request (protocol 4)
				leavePacket = new DatagramPacket(buffer, 0, buffer.length, peer.hostIP, peer.portNum); //creates addressed packet to the peer
                try{
    				dataSocket.send(leavePacket); //Send message
                }catch(IOException e){
    				System.out.println("Error:" + e);
    			}
			}
			unlockMutex(); //Unlock our mutex
			dataSocket.close(); //Close our sending socket
        }
    }

	//This is the thread that sends the messages to all other peers
	static class SendThread extends Thread{
		/*These fields hold the IP address of the host to initially join,
		port number of the host to intially join, IP address of the current
		Peer, port number of the current peer, variable to get user input,
		packet information, and the current Peer's message buffer*/
		private InetAddress receiverAddress;
		private int receiverPort;
		private InetAddress senderAddress;
		private int senderPort;
		private BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		DatagramSocket dataSocket;
		private DatagramPacket packet;
		private byte[] buffer;

		//Store current Peer's IP address and port number
		public SendThread(InetAddress newSenderAddress, int newSenderPort){
			senderAddress = newSenderAddress;
			senderPort = newSenderPort;
			try{
				dataSocket = new DatagramSocket();	//Create sending socket
			}catch(IOException e){
				System.out.println("Error:" + e);
			}
		}

		public void run(){
			try{
				joinNetwork();	//Ask Peer to join a network
				while(stillConnected){
					sendMessage(stdIn.readLine()); //Send user input to peers in network
				}
			}catch(IOException e){
				System.out.println("Error:" + e);
			}
		}

		public void joinNetwork() throws IOException{ //Send protocol 1 message
			String decision;
			/*Ask Peer to connect to another Peer in a network*/
			do{
				System.out.print("Would you like to connect to a peer? (Y/N) ");
			}while(!(decision = userInput.nextLine()).equals("Y") && !decision.equals("N"));
			if(decision.equals("Y")){
				System.out.print("Please give us the IP Address: ");
				receiverAddress = InetAddress.getByName(userInput.nextLine());
				System.out.print("Port number to send to: ");
				receiverPort = userInput.nextInt();

				lockMutex();
				peerList.add(new PeerInfo(receiverAddress, receiverPort));
				unlockMutex();
				//Send a protocol 1 message to the specified Peer's IP address and port number
				String message = "1:" + senderPort + ":" + senderAddress.toString();
				buffer = new byte[256];
				buffer = message.getBytes();
				packet = new DatagramPacket(buffer, 0, buffer.length, receiverAddress, receiverPort);
				dataSocket.send(packet);
			}
		}

		public void sendMessage(String message) throws IOException{	//Send protocol 5 message
			if(message == null) return;	//If no message, return
			message = "5:" + name + ":" + message;	//Send chat message to all Peers in network
			buffer = new byte[256];
			buffer = message.getBytes();
			lockMutex();
			for(PeerInfo peer : peerList){
				packet = new DatagramPacket(buffer, 0, buffer.length, peer.hostIP, peer.portNum);
				dataSocket.send(packet);
			}
			unlockMutex();
		}
	}

	//This is the thread that receives messages sent
	static class ReceiveThread extends Thread{
		/*These variables hold the port number of the current Peer,
		the IP address of the current Peer, the socket to receive
		incoming messages, the socket to send messages in response,
		packet information, and a message buffer*/
		private int receiverPortNumber;
		private String receiverHostIP;
		private DatagramSocket receiveSocket;
		private DatagramSocket responseSocket;
		private DatagramPacket packet;
		private byte[] buffer; //Create buffer to hold messages

		//Receive and store current Peer's port number and IP address
		ReceiveThread(String newHostIP, int newPortNumber){
			receiverPortNumber = newPortNumber;
			receiverHostIP = newHostIP;
			try{
				receiveSocket = new DatagramSocket(receiverPortNumber);	//Create receive socket
				responseSocket = new DatagramSocket();	//Create response socket
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
					receiveSocket.receive(packet);	//Receive packet
					//Get message from packet
					newCharacter = new String(packet.getData(), 0, packet.getLength());
					newCharacter = newCharacter.replace("/", "");
					partsOfMessage = newCharacter.split(":");
					//Based on message type, perform different actions
					if(partsOfMessage[0].equals("1")){
						broadcastJoin(partsOfMessage[2], partsOfMessage[1]);
					}else if(partsOfMessage[0].equals("2")){
						responseJoin(partsOfMessage[2], partsOfMessage[1]);
					}else if(partsOfMessage[0].equals("3")){
						normalJoin(partsOfMessage[2], partsOfMessage[1]);
					}else if(partsOfMessage[0].equals("5")){
                        if(partsOfMessage.length == 3)
						displayMessage(partsOfMessage[1], partsOfMessage[2]);
					}else if(partsOfMessage[0].equals("4")){
                        removePeer(partsOfMessage[2], partsOfMessage[1]);
                    }
				}
			}catch(IOException e){	//If error, tell user
				System.out.println("Error:" + e);
			}
		}

		/*If a protocol 1 message is received, send a Peer's IP address and port number
		to all other Peers in the network, and then add the Peer's information to the current
		Peer's own list of peers (peerList)*/
		public void broadcastJoin(String hostIP, String portNum) throws IOException{ //Handle protocol 1 message
			String message = "2:" + portNum + ":" + hostIP;
			buffer = new byte[256];
			buffer = message.getBytes();
			lockMutex();
			for(PeerInfo peer : peerList){
				packet = new DatagramPacket(buffer, 0, buffer.length, peer.hostIP, peer.portNum);
				responseSocket.send(packet);
			}
			unlockMutex();
			lockMutex();
			peerList.add(new PeerInfo(InetAddress.getByName(hostIP), Integer.parseInt(portNum)));
			unlockMutex();
		}
		
		/*If a protocol 2 message is received, respond to the given Peer IP and portNum with
		current Peer's own IP address and port number. Add the given IP and port number to the
		current Peer's list of peers*/
		public void responseJoin(String hostIP, String portNum) throws IOException{ //Protocol 2
			String message = "3:" + receiverPortNumber + ":" + receiverHostIP;
			buffer = new byte[256];
			buffer = message.getBytes();
			packet = new DatagramPacket(buffer, 0, buffer.length, InetAddress.getByName(hostIP), Integer.parseInt(portNum));
			responseSocket.send(packet);
			lockMutex();
			peerList.add(new PeerInfo(InetAddress.getByName(hostIP), Integer.parseInt(portNum)));
			unlockMutex();
		}
		
		/*If a protocol 3 message is received, simply add the given information to the
		current Peer's list of peers*/
		public void normalJoin(String hostIP, String portNum) throws IOException{	//Protocol 3
			lockMutex();
			peerList.add(new PeerInfo(InetAddress.getByName(hostIP), Integer.parseInt(portNum)));
			unlockMutex();
		}

		/*If a protocol 5 message is received, just display the chat message on-screen, with the given name*/
		public void displayMessage(String senderName, String senderMessage){ //Handle protocol 5 message
			System.out.println(senderName + ": " + senderMessage);
		}

		/*If a protocol 4 message is received, remove the given Peer's information from the
		current Peer's peer list*/
        public void removePeer(String leavingIP, String leavingPort){ //Handle protocol 4 message
			lockMutex();
            for(PeerInfo peer : peerList){
				boolean isPeerIpLeaverIP = peer.hostIP.toString().equals("/" + leavingIP);
				boolean isPeerPortLeaverPort = peer.portNum == Integer.parseInt(leavingPort);
				
                if(isPeerIpLeaverIP && isPeerPortLeaverPort){ //If the peer's port and IP match the leavers, remove that peer from the list
					System.out.println("System: A peer has left the chat");
                    peerList.remove(peer);
                }
            }
			unlockMutex();
        }
	}
}
