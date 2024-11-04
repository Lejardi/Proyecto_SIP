package proxy;

import java.io.IOException;
import java.net.SocketException;

import mensajesSIP.InviteMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.TryingMessage;
import mensajesSIP.SIPMessage;

public class ProxyTransactionLayer {
	private static final int IDLE = 0;
	private static final int NotRegistered = 0;
	private static final int Registered = 1;
	
	private int state = IDLE;
	private int register_state = NotRegistered;

	private ProxyUserLayer userLayer;
	private ProxyTransportLayer transportLayer;

	// CONSTRUCTOR TRANSECTON LAYER
	
	public ProxyTransactionLayer(int listenPort, ProxyUserLayer userLayer) throws SocketException {
		this.userLayer = userLayer;
		this.transportLayer = new ProxyTransportLayer(listenPort, this);
	}

	//RECIBO MENSAJES
	
	public void onMessageReceived(SIPMessage sipMessage) throws IOException {
		
		//1. INVITE
		
		if (sipMessage instanceof InviteMessage) {
			
			System.out.println("\n -Proxy transaction layer");
		
			InviteMessage inviteMessage = (InviteMessage) sipMessage;
			
			
			switch (state) {
			
			case IDLE:
				userLayer.onInviteReceived(inviteMessage);
				break;
			default:
				System.err.println("Unexpected message, throwing away PT1");
				break;
			}
			
		//2. REGISTER
			
		}else if (sipMessage instanceof RegisterMessage) {
			
			RegisterMessage registerMessage = (RegisterMessage) sipMessage;
		
			switch (register_state) {
				case NotRegistered:
					register_state = Registered;
					userLayer.onRegisterReceived(registerMessage);
					break;
					
				case Registered:
					userLayer.onRegisterReceived(registerMessage);

					if (sipMessage instanceof OKMessage) {
						System.out.println("Raro");
						register_state = Registered;
						userLayer.onRegisterReceived(registerMessage);
					}else {
						/*Comprobar timer??*/
					}
					break;
				default:
					System.out.println("ESTADO ACTUAL: " + register_state);
					System.err.println("Unexpected message, throwing away PT2");
					
					break;
			}
		} 
		else {
			System.err.println("Unexpected message, throwing away PT3");
		}
	}
	
	
	
	//ENVIO MENSAJES
	
	public void send100Trying(TryingMessage TryingMessage, String originAdress, int originPort) throws IOException {
		transportLayer.send100Trying(TryingMessage,originAdress, originPort);
	}
	
	public void send200OK(OKMessage OKMessage, String originAdress, int originPort) throws IOException {
		transportLayer.send200OK(OKMessage,originAdress, originPort);
	}
	
	public void send404 (NotFoundMessage NFMessage, String originAddress, int originPort) throws IOException {
		transportLayer.send404(NFMessage, originAddress, originPort);
	}
	
	public void forwardCall(InviteMessage invite, String originAddress, int originPort) throws IOException {
		transportLayer.forwardCall(invite, originAddress, originPort);
	}
	
	
	
	
	//COMIENZO A ESCUCHAR 
	
	public void startListening() {
		transportLayer.startListening();
	}
}