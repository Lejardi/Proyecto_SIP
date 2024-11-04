package proxy;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

import common.FindMyIPv4;
import mensajesSIP.InviteMessage;
import mensajesSIP.TryingMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.RegisterMessage;

public class ProxyUserLayer {
	
	
	ArrayList<HashMap<String, Object>> UsuariosPermitidos = listaUsuarios();
	private ProxyTransactionLayer transactionLayer;
    
	// CONSTRUCTOR DE LA CAPA DE USUARIO
	public ProxyUserLayer(int listenPort) throws SocketException {
		this.transactionLayer = new ProxyTransactionLayer(listenPort, this);
		
	}
	

	//RECIBO MENSAJES
	
	// 1.INVITE
	public void onInviteReceived(InviteMessage inviteMessage) throws IOException {
		
		System.out.println("Received INVITE from " + inviteMessage.getFromName());
		
		ArrayList<String> vias = inviteMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
		String uriDestino = inviteMessage.getToUri();
		String uriOrigen = inviteMessage.getFromUri();
		String nombreUserDestino = inviteMessage.getToName();
		String nombreUserOrigen = inviteMessage.getFromName();
		String cseq = inviteMessage.getcSeqNumber();
		
		HashMap<String, Object> destino =getDestino(UsuariosPermitidos, uriDestino);
		
		//MODIFICACIONES
		inviteMessage.setcSeqNumber(String.valueOf(Integer.parseInt(inviteMessage.getcSeqNumber())+1));
		inviteMessage.setMaxForwards(inviteMessage.getMaxForwards()-1);
		// inviteMessage.addVia((FindMyIPv4.findMyIPv4Address().getHostAddress() + ":" + ));

		
		if ( destino.equals(null)) 
		{
			System.out.println("Destino no encontrado");
			
		} else {
			
			TryingMessage tr = new TryingMessage();
			
			tr.setVias(vias);
			tr.setFromName(nombreUserOrigen);
			tr.setFromUri(uriOrigen);
			tr.setToName(nombreUserDestino);
			tr.setToUri(uriDestino);
			tr.setCallId(inviteMessage.getCallId());
			tr.setcSeqNumber(String.valueOf(Integer.parseInt(inviteMessage.getcSeqNumber())+1));
			tr.setcSeqStr("ACK");
			tr.setContentLength(0);
			transactionLayer.send100Trying(tr,originAddress,originPort);
		
			
			//Al mismo tiempo compongo el mensaje a destino
			transactionLayer.forwardCall(inviteMessage,(String)destino.get("direccion origen"), (int)destino.get("puerto origen"));
			
		}
		
		
		
	}
	
	//2. REGISTER
	
	public void onRegisterReceived(RegisterMessage registerMessage) throws IOException {
		
		System.out.println("Received REGISTER from " + registerMessage.getFromName());
		
		ArrayList<String> vias = registerMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
		int expires =  Integer.parseInt(registerMessage.getExpires());
		String uri = registerMessage.getToUri();
		String nombreUser = registerMessage.getToName();
		String cseq = registerMessage.getcSeqNumber();
		
		if (ChecklistaUsuarios(UsuariosPermitidos, uri, originAddress, originPort, expires)==true) {
			
			OKMessage OKMessage = new OKMessage();
			OKMessage.setVias(vias);
			OKMessage.setFromName(nombreUser);
			OKMessage.setFromUri(uri);
			OKMessage.setToName(nombreUser);
			OKMessage.setToUri(uri);
			OKMessage.setCallId(registerMessage.getCallId());
			OKMessage.setcSeqNumber(cseq);
			OKMessage.setcSeqStr("REGISTER");
			OKMessage.setContact(originAddress + ":" + originPort);
			OKMessage.setContentLength(0);
			transactionLayer.send200OK(OKMessage,originAddress,originPort);
			
		} 	else {
			
			NotFoundMessage nf = new NotFoundMessage();

			nf.setVias(vias);
            nf.setToName(nombreUser);
            nf.setToUri(uri);
            nf.setFromName(nombreUser);
            nf.setFromUri(uri);
            nf.setCallId(registerMessage.getCallId());
            nf.setcSeqNumber(cseq);
            nf.setcSeqStr("REGISTER");
            nf.setContact(originAddress + ":" + originPort);
            nf.setContentLength(0);    
			transactionLayer.send404(nf, originAddress, originPort);
		}
	}

	
	//COMIENZO A ESTUDIAR
	
	public void startListening() {
		transactionLayer.startListening();
	}
	
	
	//FUNCIONES AUXILIARES REGISTER
	public ArrayList<HashMap<String, Object>> listaUsuarios() {	
	    ArrayList<HashMap<String, Object>> UsuariosPermitidos = new ArrayList<>();
	    HashMap<String, Object> elemento1 = new HashMap<>();
	    elemento1.put("uri", "sip:alice@SMA");
	    elemento1.put("direccion origen", null);
	    elemento1.put("puerto origen", null);
	    elemento1.put("tiempo de expiracion", null);
	
	    /*Bob*/
	    HashMap<String, Object> elemento2 = new HashMap<>();
	    elemento2.put("uri", "sip:bob@SMA");
	    elemento2.put("direccion origen", null);
	    elemento2.put("puerto origen", null);
	    elemento2.put("tiempo de expiracion",null);
	    
	    UsuariosPermitidos.add(elemento1);
	    UsuariosPermitidos.add(elemento2);
	    
	    return UsuariosPermitidos;    
	}
	
	public boolean ChecklistaUsuarios(ArrayList<HashMap<String, Object>> lista, String uri, String originAddress, int originPort, int tiempoExp) {
		
		boolean respuesta = false;
		
		for (HashMap<String, Object> elemento : lista) {
			
			if (elemento.get("uri").equals(uri))
			{
				long currentMillis = System.currentTimeMillis();
			    elemento.put("direccion origen", originAddress);
			    elemento.put("puerto origen", originPort);
			    elemento.put("tiempo de expiracion",(tiempoExp*1000)+currentMillis);
				respuesta = true;
				//System.out.print(elemento);
			}
		}
		
	    if (respuesta==false) {      
			System.out.print("No autorizado");
		} else {
				System.out.print("Autorizado");
		}
		return respuesta;
	}
		
	
	public HashMap<String, Object> getDestino(ArrayList<HashMap<String, Object>> lista, String uriDestino) {
			
			HashMap<String, Object> destino = null;

			for (HashMap<String, Object> elemento : lista) {
				
				
				if (elemento.get("uri").equals(uriDestino))
				{
					destino=elemento;
				}	
			}
			
			return destino;
	}
	
	
	
	
	
	
	
	
	
}


