import java.net.InetAddress;
//Class to hold IP addresses and port numbers of peers
public class PeerInfo{
	//Fields to hold the information
	public InetAddress hostIP;
	public int portNum;
	
	//Add information to fields when PeerInfo object is created
	public PeerInfo(InetAddress newIP, int newPort){
		hostIP = newIP;
		portNum = newPort;
	}
}