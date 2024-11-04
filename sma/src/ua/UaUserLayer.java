package ua;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;
import javax.swing.Timer;

import common.FindMyIPv4;
import mensajesSIP.InviteMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.SDPMessage;
import mensajesSIP.TryingMessage;

public class UaUserLayer {
	
	private static final int IDLE = 0;
	private int state = IDLE;

	public static final ArrayList<Integer> RTPFLOWS = new ArrayList<Integer>(
			Arrays.asList(new Integer[] { 96, 97, 98 }));

	private UaTransactionLayer transactionLayer;
	
	private String myAddress = FindMyIPv4.findMyIPv4Address().getHostAddress();
	private String dominio;
	private String nombreUser;
	private String proxyAddress;
	private int proxyPort;
	private int rtpPort;
	private int listenPort;
	private int tiempoExpiracion;
	

	private Process vitextClient = null;
	private Process vitextServer = null;
	
	
	//CONSTRUCTOR DEL USER LAYER

	public UaUserLayer(int listenPort, String proxyAddress, int proxyPort, String nombreUser, String dominio, int tiempoExpiracion)
			throws SocketException, UnknownHostException {
		
		this.transactionLayer = new UaTransactionLayer(listenPort, proxyAddress, proxyPort, nombreUser, dominio,this);
		this.dominio = dominio;
		this.nombreUser=nombreUser;
		this.proxyAddress=proxyAddress;
		this.proxyPort=proxyPort;
		this.listenPort = listenPort;
		this.rtpPort = listenPort + 1;
		this.tiempoExpiracion=tiempoExpiracion;

	}

	
	//RECIBO MENSAJES 
	
	//1. INVITE
	public void onInviteReceived(InviteMessage inviteMessage) throws IOException {
		System.out.println("Received INVITE from " + inviteMessage.getFromName());
		runVitextServer();
		
		
		
	}
	
	//2.REGISTER
	public void onRegisterReceived(RegisterMessage registerMessage) throws IOException {
		System.out.println("Received REGISTER from " + registerMessage.getFromName());
		runVitextServer();
	}

	//3. 200 OK
	public void on200OKReceived(OKMessage okmessage) throws IOException {
		System.out.println("Received 200OK");    
		runVitextServer();
	}

	//4. 404 NOT FOUND
	public void on404Received(NotFoundMessage notFoundMessage) throws IOException {
		System.out.println("No Autorizado");
		runVitextServer();
	}
	
	//5.TRYING 
		
	public void on100TryingReceived(TryingMessage TryingMessage) throws IOException {
		System.out.println("Trying");
		runVitextServer();
	}
	
	
	
	//EMPIEZO A ESCUCHAR LA RED
	public void startListeningNetwork() {
		transactionLayer.startListeningNetwork();
	}

	
	//EMPIEZO A ESCUCHAR TECLADO
	public void startListeningKeyboard() {
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				prompt();
				String line = scanner.nextLine();
				if (!line.isEmpty()) {
					command(line);
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	
   /*MAQUINA DE ESTADOS*/
	
	private void prompt() {
		System.out.println("");
		
		switch (state) {
		
		case IDLE:
			promptIdle();
			break;
		default:
			throw new IllegalStateException("Unexpected state: " + state);
		}
	
		System.out.print("> ");
	}

	private void promptIdle() {
		System.out.println("INVITE xxx");
		System.out.println("REGISTER xxx");
	}

	private void command(String line) throws IOException {
		if (line.startsWith("INVITE")) {
			commandInvite(line);
			
		} 
		if (line.startsWith("REGISTER")){
			commandRegister(line);
			
		}else {
			System.out.println("Bad command");
		}
	}

	/*MENSAJE INVITE*/
	
	private void commandInvite(String line) throws IOException {
		
		String[] palabras = line.split(" ");
		stopVitextServer();
		stopVitextClient();
		
		System.out.println("Inviting...");

		runVitextClient();

		String callId = UUID.randomUUID().toString();

		SDPMessage sdpMessage = new SDPMessage();
		sdpMessage.setIp(this.myAddress);
		sdpMessage.setPort(this.rtpPort);
		sdpMessage.setOptions(RTPFLOWS);

		InviteMessage inviteMessage = new InviteMessage();
		String nombreDestino= palabras[1];
		inviteMessage.setDestination("sip:" + nombreDestino + "@" + dominio);
		inviteMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		inviteMessage.setMaxForwards(70);
		inviteMessage.setToName(nombreDestino);
		inviteMessage.setToUri("sip:" + nombreDestino + "@" + dominio);
		inviteMessage.setFromName(nombreUser);
		inviteMessage.setFromUri("sip:" + nombreUser + "@" + dominio);
		inviteMessage.setCallId(callId);
		inviteMessage.setcSeqNumber("1");
		inviteMessage.setcSeqStr("INVITE");
		inviteMessage.setContact(myAddress + ":" + listenPort);
		inviteMessage.setContentType("application/sdp");
		inviteMessage.setContentLength(sdpMessage.toStringMessage().getBytes().length);
		inviteMessage.setSdp(sdpMessage);

		transactionLayer.call(inviteMessage);
	}
	
	/*REGISTER*/
	public void commandRegister(String line) throws IOException {
		
		stopVitextServer();
		stopVitextClient();
		
		System.out.println("Registering...");

		runVitextClient();
		
		String callId = UUID.randomUUID().toString();

		SDPMessage sdpMessage = new SDPMessage();
		sdpMessage.setIp(this.myAddress);
		sdpMessage.setPort(this.rtpPort);
		sdpMessage.setOptions(RTPFLOWS);

		RegisterMessage registerMessage = new RegisterMessage();
		registerMessage.setDestination("sip:"+ proxyAddress);
		registerMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		registerMessage.setMaxForwards(70);
		registerMessage.setToName(nombreUser);
		registerMessage.setToUri("sip:"+nombreUser+"@"+dominio);
		registerMessage.setFromName(nombreUser);
		registerMessage.setFromUri("sip:"+nombreUser+"@"+dominio);
		registerMessage.setCallId(callId);
		registerMessage.setcSeqNumber("1");
		registerMessage.setcSeqStr("REGISTER");
		registerMessage.setContact(myAddress + ":" + listenPort);
		registerMessage.setExpires(String.valueOf(tiempoExpiracion));
		registerMessage.setContentLength(0);

		transactionLayer.call(registerMessage);
	}

	private void runVitextClient() throws IOException {
		/*vitextClient = Runtime.getRuntime().exec("xterm -e vitext/vitextclient -p 5000 239.1.2.3");*/
	}

	private void stopVitextClient() {
		if (vitextClient != null) {
			vitextClient.destroy();
		}
	}

	private void runVitextServer() throws IOException {
		/*vitextServer = Runtime.getRuntime().exec("xterm -iconic -e vitext/vitextserver -r 10 -p 5000 vitext/1.vtx 239.1.2.3");*/
	}

	private void stopVitextServer() {
		if (vitextServer != null) {
			vitextServer.destroy();
		}
	}

}
