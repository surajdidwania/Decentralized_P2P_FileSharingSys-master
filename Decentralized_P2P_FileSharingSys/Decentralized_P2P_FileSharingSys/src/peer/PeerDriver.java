/*=========================================================*/
/*       					         					   */ 
/*	                  Peer As a Driver					   */
/*						       							   */
/*=========================================================*/

package peer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

import peer.client.PeerClient;
import peer.server.PeerServer;
import peer.server.PeerServerIF;

/**
 * @author Lawrence & Suraj
 * PeerClientDriver Implementation
 * task: create RMI connections for central server and peer server
 * 		and starts two threads to run peer_client and peer_client_server
 */
public class PeerDriver {
	/*
	 * args[0] -> peer's name
	 * args[1] -> peer's ip-address
	 * args[2] -> peer's port #
	 * args[3] -> neighboring peer's ip-address
	 * args[4] -> neighboring peer's port #
	 */
	static String[] arrgs;
	static PeerServer instance;
	public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
		if (args.length >= 3) {
			arrgs = new String[args.length];
			for (int i=0; i<arrgs.length; i++)
				arrgs[i] = args[i];
			
			int numofneighbors = (args.length - 3)/2;
			new Thread(new lunchPeerserver()).start();
			
			Scanner cmdline = new Scanner(System.in);
			String cmd = cmdline.nextLine();
			if(cmd.equals("y")){
				int count=0;
				//creating peer-client object
				PeerClient clientserver = new PeerClient(arrgs[0],arrgs[1],arrgs[2],numofneighbors, instance);
				for (int i = 3; i < arrgs.length; i += 2){
					//assembling unique url for each neighboring peer
					String neighboringPeerURL = "rmi://"+arrgs[i]+":"+arrgs[i+1]+"/peerserver";	
					//searching for that unique url in register (but how does it know which register to search)???
					PeerServerIF neighPeerServer = (PeerServerIF) Naming.lookup(neighboringPeerURL);	
					//adding neighboring-peer to current peer's client object
					clientserver.addNeighboringPeer(arrgs[i], arrgs[i+1], count);
					count++;
					System.out.println("Connected to neighboring peer: "+arrgs[i]+":"+arrgs[i+1]);
				}
				new Thread(clientserver).start();
			} else {
				System.err.println("Your input was invalid. Please start afresh");
				//System.exit(0);
			}
		} else {
			System.err.println("Usage: PeerClientDriver <peer_name> < peer_ip> < peer_port_#> "
					+ "<neighboring-peer_ip>  <neighboring-peer_port_#>");
		}
	}
	
	static class lunchPeerserver implements Runnable
	{
		public void run()
		{			
			try {
				System.setProperty("java.rmi.server.hostname",arrgs[1]);	//set server property
				PeerServer peerserver = new PeerServer(arrgs[1], arrgs[2]);
				instance = peerserver;
				//rebind server to ip(localhost) and args[1](port_#)
				Naming.rebind("rmi://"+arrgs[1]+":"+arrgs[2]+"/peerserver",peerserver);
				
				System.out.println("||========================================================================||");
				System.out.println("||                    PEER-TO-PEER FILE SHARING SYSTEM                    ||");
				System.out.println("||               ========================================                 ||");
				System.out.println("||========================================================================||");
				System.out.println("        		      <PEER SERVER IS UP AND RUNNING>                       ");
				System.out.println("============================================================================");
				System.out.println("\nEnter 'y' to lunch peer client."
						+ "\nNOTE: All peer servers should be up and running before lunching client");
			} catch (RemoteException | MalformedURLException e) {
				System.out.println("Error running PeerServer Thread");
				e.printStackTrace();
			}
		}
	}
}
