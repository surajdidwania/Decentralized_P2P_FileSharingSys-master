/*=========================================================*/
/*       					         					   */ 
/*	          Peer As a Server Interface		           */
/*						       							   */
/*=========================================================*/

package peer.server;

//import java.net.URI;
import java.rmi.Remote;
import java.rmi.RemoteException;

import peer.client.PeerClientIF;

/**
 * @author Suraj
 * PeerClientServerIF Implementation
 */
public interface PeerServerIF  extends Remote{
	boolean sendFile(PeerClientIF peerClient, String filename) throws RemoteException;
	void query(String msgID, String cIP, String cPortNo, long timeToLive, String filename, String cPeerName) throws RemoteException;
	void queryhit(String msgID, long timeToLive, String filename, String hitPeerIP, String hitPeerPN, String hitPeerName) throws RemoteException;
	String getPeerDir() throws RemoteException;
	void updateFileList() throws RemoteException;
	String[] getFiles() throws RemoteException;
	void setClientInstance(PeerClientIF peerClient) throws RemoteException;
	String getIP() throws RemoteException;
	String getPN() throws RemoteException;
}
