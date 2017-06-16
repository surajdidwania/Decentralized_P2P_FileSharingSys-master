/*=========================================================*/
/*       					         					   */ 
/*	                  Peer As a Server     		           */
/*						       							   */
/*=========================================================*/

package peer.server;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

import peer.client.PeerClient;
import peer.client.PeerClientIF;
import peer.client.PeerDirListener;

/**
 * @author Suraj & Lawrence
 * PeerClientServer Implementation
 * task: sends file in peer_client_server to requesting peer
 */
public class PeerServer extends UnicastRemoteObject implements PeerServerIF {
	private PeerClientIF peerClient;
	private String peerIP;
	private String portNo;
	private String peerRootDirectoryPath = null;	//home directory path of peer
	private String[] files;	//array to hold names of all files and directories in peer's root directory
	
	private final int QUEUE_MAX_SIZE = 10;
	private String[][] queuedRecMsgs = new String[(int) QUEUE_MAX_SIZE][3];	//keeps track of all received messages's meta data
	private int queuetracker = 0;
	
	protected PeerServer() throws RemoteException {
		super();
	}
	
	//and finally starts a new thread for event listener 'PeerDirListener(this)'
	public PeerServer(String pIP, String pN) throws RemoteException {
		this.peerIP = pIP;
		this.portNo = pN;
		//create list of files in peer root directory
		try{
			//get peer root directory pathname
			this.peerRootDirectoryPath = System.getProperty("user.dir");
		    System.out.print("Peer Directory is: "+peerRootDirectoryPath.replace("\\", "/")+"\n");
		    //get all files in peer root directory
			File f = new File(peerRootDirectoryPath);
			this.files = f.list();	//returns directory and files (with extension) in director
			System.out.println("List of files in Peer:");
			for(int i=0; i<files.length; i++){
				System.out.println(files[i]);
			}
		}catch (Exception e){
		    System.out.println("Peer path Exception caught ="+e.getMessage());
		}		
		//start thread of event listener to monitor changes in peer directory
		new Thread(new PeerDirListener(this)).start();	
	}
	
	public String getIP(){
		return peerIP;
	}
	public String getPN(){
		return portNo;
	}
	public String[] getFiles() {
		return files;
	}
	public String getPeerDir() {
		return peerRootDirectoryPath;
	}
	public void setClientInstance(PeerClientIF client){
		peerClient = client;
	}
	public void updateFileList() {
		//get all files in peer root directory
		File f = new File(peerRootDirectoryPath);
		this.files = f.list();	//returns directory and files (with extension) in directory
	}
	
	/**
		 * task: sends a requested file to the requesting peer by converting
		 * 		the file to message stream using 'FileInputStream' and calling
		 * 		'c.acceptFile(f1.getName(), mydata, mylen)'
		 * @param c: requesting peer object
		 * @param file: filename of the file to be sent from peer server to requesting peer 
		 * @return returns true if file is sent successfully
		 * @throws RemoteException
	 */
	public synchronized boolean sendFile(PeerClientIF c, String file) throws RemoteException{
		 //Sending The File...
		 try{
			 File f1 = new File(file);	//copy file to be sent	
			//convert to message stream	
			 FileInputStream in = new FileInputStream(f1);		 				 
			 byte[] mydata = new byte[1024*1024];						
			 int mylen = in.read(mydata);
			 while(mylen>0){
				 if(c.acceptFile(f1.getName(), mydata, mylen)){
					 System.out.println("   File '"+file+"' has been sent to Requesting Peer: "+c.getName());
					 mylen = in.read(mydata);
				 } else {
					 System.out.println("Fault: File was NOT sent");
				 }				 
			 }
		 }catch(Exception e){
			 e.printStackTrace(); 
		 }
		 return true;
	}
	
	/**
	 * task: spuns two threads. one to search itself for the file and the other to
	 * 		subsequently query all neighboring peers
	 * @param file: filename of the file to be found
	 * @param requestingPeer: name of the peer seeking to find the file
	 * @param flag: controls number of time message is printed
	 * @return returns the compiled lists (array) of peers that contains file if
	 * 		file is found. Else it returns 'null' signifying that the file was not 
	 * 		found and hence is not present in any peer
	 * @throws RemoteException
	 */
	public synchronized void query(String msgID, String cIP, String cPN, long timeToLive, String file, String cPName) throws RemoteException {
		System.out.println("   Peer '"+cPName+"' has requested a file: "+file);
		boolean fileNotFound = true;
		
		//record received message in logical priority queue
		//override stale/oldest elements if logical queue is full
		queuetracker = queuetracker%QUEUE_MAX_SIZE;
		queuedRecMsgs[queuetracker][0] = msgID;
		queuedRecMsgs[queuetracker][1] = cIP;
		queuedRecMsgs[queuetracker][2] = cPN;
		queuetracker++;
		
		if(Arrays.asList(this.getFiles()).contains(file)) {
			fileNotFound = false;
			System.out.println("   File found in "+peerClient.getName());
			PeerServerIF msgSender;
			try {
				//assembling unique url for each neighboring peer and searching for that unique url in register
				msgSender = (PeerServerIF) Naming.lookup("rmi://"+cIP+":"+cPN+"/peerserver");
				msgSender.queryhit(msgID, timeToLive+1, file, peerIP, portNo, peerClient.getName());
			} catch (MalformedURLException | NotBoundException e) {
				System.out.println("MalformedURLException or NotBoundException in PeerServer's query method");
				e.printStackTrace();
			}
		}
		
		//broadcast message to other neighboring peers
		String[][] neighbors =  peerClient.getNeighPeerServers();
		if(timeToLive > 0) {
			for (int l=0; l<neighbors.length; l++) {
				if(!(neighbors[l][0].equals(cIP) && neighbors[l][1].equals(cPN))) {
					PeerServerIF neighPeerServer;
					try {
						neighPeerServer = (PeerServerIF) Naming.lookup("rmi://"+neighbors[l][0]+":"+neighbors[l][1]+"/peerserver");
						neighPeerServer.query(msgID, peerIP, portNo, timeToLive-1, file, peerClient.getName());
					} catch (MalformedURLException | NotBoundException e) {
						System.out.println("MalformedURLException | NotBoundException error");
						e.printStackTrace();
					}
				}
			}
		}	
		if(fileNotFound)
			System.out.println("   File not found in "+peerClient.getName());
	}

	public void queryhit(String msgID, long timeToLive, String filename, String hitPeerIP, String hitPeerPN, String hitPeerName) throws RemoteException {
		if(timeToLive == PeerClient.getTTL()){
			peerClient.addMsgHits(msgID, hitPeerIP, hitPeerPN, hitPeerName);
		} else {
			for(int r=0; r<queuedRecMsgs.length; r++) {
				if (queuedRecMsgs[r][0] != null) {
					if(queuedRecMsgs[r][0].equals(msgID)) {
						PeerServerIF msgUpStreamSender;
						try {
							msgUpStreamSender = (PeerServerIF) Naming.lookup("rmi://"+queuedRecMsgs[r][1]+":"+queuedRecMsgs[r][2]+"/peerserver");
							msgUpStreamSender.queryhit(msgID, timeToLive+1, filename, hitPeerIP, hitPeerPN, hitPeerName);
						} catch (MalformedURLException | RemoteException | NotBoundException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}