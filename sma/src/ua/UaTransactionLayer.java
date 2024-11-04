package ua;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.SocketException;
import javax.swing.Timer;


import mensajesSIP.InviteMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.SIPMessage;
import mensajesSIP.TryingMessage;
import mensajesSIP.OKMessage;

public class UaTransactionLayer {
	
	private static final int FREE = 0;
	private static final int NotRegistered = 0;	
	private static final int Registering = 1;
	private static final int Registered = 2;

	private int invite_state = FREE;
	private int register_state = NotRegistered;
	
	private UaUserLayer userLayer;
	private UaTransportLayer transportLayer;
	
	private Timer registerTimer;
	private Timer expirationTimer;
	
	
	//1. CONSTRUCTOR CAPA TRANSACTION

	public UaTransactionLayer(int listenPort, String proxyAddress, int proxyPort, String nombreUser, String dominio, UaUserLayer userLayer)
			throws SocketException {
		this.userLayer = userLayer;
		this.transportLayer = new UaTransportLayer(listenPort, proxyAddress, proxyPort, nombreUser, dominio, this);
	}

	
   //2. RECIBO MENSAJES
	
	public void onMessageReceived(SIPMessage sipMessage) throws IOException {
		
		//INVITE
		
		if (sipMessage instanceof InviteMessage) {
			
			InviteMessage inviteMessage = (InviteMessage) sipMessage;
			switch (invite_state) {
			
			case FREE:
				userLayer.onInviteReceived(inviteMessage);
				break;
				
			default:
				System.err.println("Unexpected message, throwing awayHOLA");
				break;
			}
		} 
		
		//OK MESSAGE
		
		else if (sipMessage instanceof OKMessage) {
			
			OKMessage OKmessage = (OKMessage) sipMessage;
			System.out.println(register_state);
			switch (register_state) {
			
				
			    case NotRegistered:
					break;
					
				case Registering:
					register_state=Registered;
					
					userLayer.on200OKReceived(OKmessage);
					registerTimer.stop();
					expirationTimer.restart();
					break;
					
				case Registered:
					break;
					
				default:
					System.err.println("Unexpected message, throwing away3");
					break;
			}
		}
		
		//TRYING MESSAGE
		
		else if (sipMessage instanceof TryingMessage) {
			
			TryingMessage TryingMessage = (TryingMessage) sipMessage;
			userLayer.on100TryingReceived(TryingMessage);
	
		}

		//NOT FOUND 
		
		else if (sipMessage instanceof NotFoundMessage) {
			
			NotFoundMessage notFoundMessage = (NotFoundMessage) sipMessage;
			registerTimer.stop();
			userLayer.on404Received(notFoundMessage);
		}
		
		
		//CUALQUIER OTRA COSA DESCARTO

		else {
			System.err.println("Unexpected message, throwing away");
		}
		
	}

	
	// COMIENZO A ESCUCHAR LA RED
	
	public void startListeningNetwork() {
		transportLayer.startListening();
	}
	
	
	//MANDO MENSAJES

	//1. INVITE
	
	public void call(InviteMessage inviteMessage) throws IOException {
		
		transportLayer.sendToProxy(inviteMessage);	// Añadir proxyAddress como otro parámetro o extraerlo luego desde el mensaje
	}
	
	
	//2. REGISTER
	
	public void call(RegisterMessage registerMessage) throws IOException {
		switch (register_state) {
		
			case NotRegistered:
								
				System.err.println("OK, sending message");
				register_state = Registering;
				transportLayer.sendToProxy(registerMessage);
				
				//TIMER DE 2 SEGUNDOS
				registerTimer = new Timer(2000, new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							System.out.println("REGISTER reenviado.");
							transportLayer.sendToProxy(registerMessage);
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				});
				
				registerTimer.setRepeats(true);
				
				//TIMER TIEMPO DE EXPIRACION
				expirationTimer = new Timer(Integer.parseInt(registerMessage.getExpires())*1000, new ActionListener() {
					
					public void actionPerformed(ActionEvent e) {
						try {
							System.out.println("FIN del register, expirado, reenvio");
							register_state = Registering;
					
							transportLayer.sendToProxy(registerMessage);
							//userLayer.commandRegister("REGISTER xxx");

						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				});
				
				expirationTimer.setRepeats(true);
				registerTimer.restart();
				
			
				break;
				
			case Registering:
				System.err.println("Throwing away, already registering");
				break;
				
			case Registered:
				System.err.println("Throwing away, already registered");
				break;
				
			default:
				System.err.println("Unexpected message, throwing away");
				break;
		} 
	}
}
