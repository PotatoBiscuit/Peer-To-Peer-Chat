import java.net.InetAddress;
public class PeerInfo{
	public InetAddress hostIP;
	public int portNum;
	
	public PeerInfo(InetAddress newIP, int newPort){
		hostIP = newIP;
		portNum = newPort;
	}
}