package main.java;

import java.net.*;
import java.util.Scanner;
public class Sender {
	public static int inventory;

	public static void main(String[] args) throws Exception {
		inventory=500;
		// for send command, follows "SEND<200>"
		// for CHECK, either "check" or "CHECK" is fine
		// For this stage, assume only the sender input commands, and client would just reply(no two-way)
		// run this program, run the receiver first(run as java application), then the sender(run as java application)
		//input Command, the client will reply base on the sender input	
			//send
		DatagramSocket ds=new DatagramSocket();
		
		Scanner input= new Scanner(System.in);
		System.out.println("Input your request:");
		//send store the string of command
		String send=input.next();
		byte[] b = send.getBytes();
		InetAddress ia=  InetAddress.getLocalHost();
		DatagramPacket dp=new DatagramPacket(b,b.length,ia,9999);
		ds.send(dp);
		
		//receive
		byte [] b1 = new byte[1024];
		DatagramPacket dp1=new DatagramPacket(b1,b1.length);
		ds.receive(dp1);
		// get reply from other computer
		String str=new String(dp1.getData(),dp.getOffset(),dp.getLength());
		
		
		// receiver reply his new amount inventory after sender sent inventory to him
		if(send.contains("SEND")){
			String get=send.substring(4,send.length());
			int sendinventory = Integer.parseInt(get);
			//deduct his own inventory
			inventory-=sendinventory;
			System.out.println("Receiver current inventory:\t"+str);
		}
		else if(send.equalsIgnoreCase("CHECK")){
			System.out.println("Receiver current inventory:\t"+str);
		}
		// Receiver reply insufficient or send commands depends on his current amount inventory
		else if(send.contains("REQUEST")){
			System.out.println("Receiver's reply:\t"+str);
			
		}
	
		

		
		
		
		
		
		
	}//main
		
}

