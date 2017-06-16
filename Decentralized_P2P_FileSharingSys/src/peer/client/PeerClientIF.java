/*=========================================================*/
/*       					         					   */ 
/*	            Peer As Client Interface				   */
/*						       							   */
/*=========================================================*/

package peer.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Lawrence
 * PeerClientIF Implementation
 */
public interface PeerClientIF extends Remote {
	String getName() throws RemoteException;
	String getport_no() throws RemoteException;
	String getpeer_ip() throws RemoteException;
	boolean acceptFile(String name, byte[] mydata, int mylen) throws RemoteException;
	String[][] getNeighPeerServers() throws RemoteException;
	void addMsgHits(String msgID, String hitPeerIP, String hitPeerPN, String hitPeerName) throws RemoteException;
}
