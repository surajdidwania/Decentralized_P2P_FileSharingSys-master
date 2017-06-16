/*=========================================================*/
/*       					         					   */ 
/*	               Peer As a Client				           */
/*						       							   */
/*=========================================================*/

package peer.client;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import peer.server.PeerServerIF;

/**
 * @author Lawrence & Suraj
 * PeerClient Implementation
 * task: runs all peer client end functions including searching for
 * 		file in other peers and downloading file directly 
 * 		from another peer's server
 */
//to implement instances of this class as threads use 'Runnable' which will add the 'run' method
public class PeerClient extends UnicastRemoteObject implements PeerClientIF, Runnable { 
	private static final long serialVersionUID = 1L;
	private PeerServerIF peerServer;	//the object of the server side of this peer client 
	private String[][] neighPeerServers; //array holding ip & port# of all neighboring peer-servers
	private ArrayList<String[]> msgHits = new ArrayList<String[]>();	//records meta data on message hits
	private ArrayList<Long> queryhittime = new ArrayList<Long>();
	private String peerName = null;	//name of peer
	private String peer_ip; //ip address of peer
	private String port_no;	//port number of peer	
	private int msgIDsuffix = 1;	//used to assign a unique ID to each message
	final static long TIME_TO_LIVE = 9;	//sets the maximum message hops per query
	final static long QUERY_WAIT_TIME = 600;	//in milliseconds, sets time client waits for query hits
	private long sum=0;

	
	protected PeerClient() throws RemoteException {
		super();
	}
	
	/**
	 * task: PeerClient constructor initializes necessary global fields of PeerClient instance 
	 * 		to detect changes to the peer directory contents
	 * @param peerName: name of peer
	 * @param peer_ip: ip address of peer
	 * @param port_no: port number of peer
	 * @param peerServer: instance of the server associated to this peer client
	 * @param numofneighboringpeers: number of peers neighboring this peers
	 * @param neighPeerServers: array to store credentials of neighboring peers
	 * @throws RemoteException
	 */
	public PeerClient(String name,String ip_address, String  port_no, int numofneighboringpeers, PeerServerIF inst) throws RemoteException {
		this.peerName = name;
		this.peer_ip = ip_address;
		this.port_no = port_no;
		this.peerServer = inst; 	//gets object of its own server
		neighPeerServers = new String[numofneighboringpeers][2];
		peerServer.setClientInstance(this);	//sends instance of the client to the server
	}
	
	//getters for global variables
	public String getName() {
		return peerName;
	}
	public String[][] getNeighPeerServers() {
		return neighPeerServers;
	}
	public String getport_no() {
		return port_no;
	}
	public String getpeer_ip() {
		return peer_ip;
	}
	public static long getTTL() {
		return TIME_TO_LIVE;
	}
	//adds credentials of neighboring peer to array
	public void addNeighboringPeer(String nPIP, String nPPN, int index) {
		neighPeerServers[index][0] = nPIP;
		neighPeerServers[index][1] = nPPN;
	}
	//
	public void addMsgHits(String mssgID, String hitIP, String hitPNum, String hitPName) {
		String[] msgHitMetadata = {mssgID, hitIP, hitPNum, hitPName};
		msgHits.add(msgHitMetadata);
		//System.out.println("Adding time of queryhit");
		queryhittime.add(System.currentTimeMillis());
	}
	
	/**
	 * task: downloads chosen file directly from peer by calling 
	 * 		'peerWithFile.sendFile(this,filename)'
	 * @param peerWithFile: RMI peer server object of the peer with the file
	 * @param filename: name of the file to be downloaded
	 */
	private synchronized void downloadFile(PeerServerIF peerWithFile, String filename) {
		//request file directly from Peer
		try {
			if(peerWithFile.sendFile(this, filename)){
				System.out.println("   File has been downloaded");
			} else {
				System.out.println("Fault: File was NOT downloaded");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}		
	}
		
	/**
	 * task: searches the entire network for the peers that contains the file intended
	 * 		for download by calling 'peerServer.query(...)'
	 * 		then lists the peers that was returned and prompts
	 * 		user to choose which of the peers to download from
	 * @param filename: name of the file to be searched in central server
	 * @return returns an array(list) of all peers that contains the file
	 * 		or returns 'null' if file is not found in any peer
	 * @throws NotBoundException 
	 * @throws MalformedURLException 
	 */
	/**
	 * @param filename
	 * @return
	 * @throws InterruptedException 
	 * @throws MalformedURLException
	 * @throws NotBoundException
	 */
	public synchronized boolean findFile(String filename) throws InterruptedException {
		try {
			System.out.println("   Query message sent, waiting for response from network...");
			//start the series of query from its server
			//returns an array of peers with file
			msgIDsuffix++;
			peerServer.query(peer_ip+"-"+port_no+"-"+msgIDsuffix , peer_ip, port_no, TIME_TO_LIVE, filename, peerName);
			Thread.sleep(QUERY_WAIT_TIME);	//pause and wait for response to be propagated back [throws InterruptedException]
			
			if (!msgHits.isEmpty()) {
				return true;
			} else {
				System.out.println("File not found. No Peer in network returned a hit within set wait time.");
				return false;
			}
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}	
		return false;
	}
	/*public synchronized void responsefile(String filename) throws RemoteException
	{
		msgIDsuffix++;
		peerServer.query(peer_ip+"-"+port_no+"-"+msgIDsuffix , peer_ip, port_no, TIME_TO_LIVE, filename, peerName);
	}*/
	/**
	 * task: accepts the file_input_stream coming from another peer's server
	 * 		and saves the file in the peers directory. Hence accepts download
	 * @param filename: name of the file that is being sent from another peer's server
	 * @param data: byte array of file_input_stream sent from another peer's server
	 * @param len: length of file_input_stream sent from another peer's server
	 * @return returns true if file is successfully written to peer's directory
	 * @throws RemoteException
	 */
	public boolean acceptFile(String filename, byte[] data, int len) throws RemoteException{
		System.out.println("   File downloading...");
        try{
        	File f=new File(filename);	//create file
        	f.createNewFile();
        	FileOutputStream out=new FileOutputStream(f,true);
        	out.write(data,0,len);	//write to file
        	out.flush();
        	out.close();
        }catch(Exception e){
        	e.printStackTrace();
        }
		return true;
	}
	
	/**
	 * task: calculates the average response time of central server search function
	 * @param filename: name of file to be searched in central server
	 * @throws RemoteException
	 * @throws MalformedURLException
	 * @throws NotBoundException
	 * @throws InterruptedException 
	 */
	public synchronized void responsetime(String filename) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException{
		long starttime=0;
		long endtime=0;
		PeerClientIF[] peer;
		//run 200 times and average
		for(int i=0;i<200;i++)
		{
			sum=0;
			//start the series of query from its server
			//returns an array of peers with file
			msgIDsuffix++;
			starttime = System.currentTimeMillis();
			peerServer.query(peer_ip+"-"+port_no+"-"+msgIDsuffix , peer_ip, port_no, TIME_TO_LIVE, filename, peerName);
			Thread.sleep(QUERY_WAIT_TIME);	//pause and wait for response to be propagated back [throws InterruptedException]
			for(int j=0;j<queryhittime.size();j++)
			{
				sum+=(queryhittime.get(j)-starttime);
				System.out.println("      Queryhit "+(i+1)+" response time is "+((queryhittime.get(j)-starttime))+"ms");
			}
			System.out.println("      Average response time of the Peer is " + sum/(queryhittime.size()) + "ms");
			endtime+=(sum/(queryhittime.size()));
			queryhittime.clear();
			msgHits.clear();
		}
		System.out.println("   Average response time of the Peer measured 200 time is " + endtime/200 + "ms");
		sum=0;
	}
	
	/*
	 * run peer thread and display user interface to communicate in command line
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		//read messages from command line
		Scanner cmdline = new Scanner(System.in);
		String command, task, filename;
		long start, endtime;
		System.out.println("||========================================================================||");
		System.out.println("||                    PEER-TO-PEER FILE SHARING SYSTEM                    ||");
		System.out.println("||                ========================================                ||");
		System.out.println("||                                  MENU:                                 ||");
		System.out.println("||========================================================================||");
		System.out.println("Enter The Option and filename/Peer name:\n==================\n"
				+ "1. Download File from Peer Server\n"
				+ "2. Calculate Average Response time\n"
				+ "3. Exit");	
		while (true) {	//continue reading commands
			command = cmdline.nextLine();
			CharSequence symbol = " ";
			//wait till command is received and validate command
			if (command.contains(symbol)) {	
				//retrieve command line inputs separated by char " "
				task = command.substring(0, command.indexOf(' '));
				filename = command.substring(command.indexOf(' ')+1);
				
				/**
				 * search central server for peers that contain a particular file
				 * connect peer_client with user chosen peer_client_server to download file from
				 * download file and
				 * calculate the average runtime of the search-central-server functionality
				 */
				if (task.equals("1")) {
					try {
						//verify that file peer is seeking to download is not already in the peer
						if(Arrays.asList(peerServer.getFiles()).contains(filename)) {
							System.out.println("Please enter the filename which you don't possess");
							continue;
						} else {	
							queryhittime.clear();
							msgHits.clear();
							start = System.currentTimeMillis();
							if (findFile(filename)) {	//returns true if file is found
								for(int i=0;i<queryhittime.size();i++)
								{
									sum+=(queryhittime.get(i)-start);
									System.out.println("   Queryhit "+(i+1)+ " from Message ID "+(peer_ip+"-"+port_no+"-"+msgIDsuffix)+
											" response time is "+((queryhittime.get(i)-start))+"ms");
								}
								System.out.println("   Average response time of the Peer is " + sum/(queryhittime.size()) + "ms\n");
								
								//list peers with file
								System.out.println("   The following Peers have the file you want:");
								for (int i=0; i<msgHits.size(); i++) {
									System.out.println("     "+(i+1)+". "+msgHits.get(i)[3]);
								}
								//prompt user to choose Peer to download from
								System.out.println("   Enter number matching the Peer you will like to download from");
												
								int choice = cmdline.nextInt();	//initializes 'choice' with index matching the user's choice of peer to download file from
									if(choice>msgHits.size())
									{
										System.out.println("Please enter the Peer Number from above list");
										continue;
									}
								
								PeerServerIF peerServerIF;
								try {
									//connect peer directly to another peer server through RMI in order to download file
									String peerServerURL = "rmi://"+msgHits.get(choice-1)[1]+":"+msgHits.get(choice-1)[2]+"/peerserver";
									peerServerIF = (PeerServerIF) Naming.lookup(peerServerURL);
									downloadFile(peerServerIF, filename);	//download file from chosen peer
									msgHits.clear();	//remove message hits information
								} catch (RemoteException e) {
									e.printStackTrace();
								} catch (MalformedURLException e) {
									e.printStackTrace();
								} catch (NotBoundException e) {
									e.printStackTrace();
								}
							}
						}
					} catch (InterruptedException | RemoteException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
						
				} else if (task.equals("2")) {
					try {
						//verify that file peer is seeking to download is not already in the peer
						if(Arrays.asList(peerServer.getFiles()).contains(filename)) {
							System.out.println("Please enter the filename which you don't possess");
							continue;
						} else {
							try {
								queryhittime.clear();
								msgHits.clear();
								sum=0;
								//calculate average response time
								responsetime(filename);
							} catch (MalformedURLException | NotBoundException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					} catch (InterruptedException | RemoteException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else if (task.equals("3")) {
					System.exit(0);	//close program and exit
				} else {
					System.out.println("Usage: <task #> <filename or Peer_name>");
				}
			}
		}
	}
}
