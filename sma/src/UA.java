import ua.UaUserLayer;

public class UA {
	public static void main(String[] args) throws Exception {
		
		/*Capturamos los argumentos del usuario*/
		
		System.out.println("UA launching with args: " + String.join(", ", args));

		String nombreUser = args[0];
		String dominio = "SMA";
		int listenPort = Integer.parseInt(args[1]);
		String proxyAddress = args[2];
		int proxyPort = Integer.parseInt(args[3]);
		int tiempoExpiracion= Integer.parseInt(args[5]);
		UaUserLayer userLayer = new UaUserLayer(listenPort, proxyAddress, proxyPort, nombreUser, dominio,tiempoExpiracion);
		
		
		userLayer.commandRegister("REGISTER hola");
		
		new Thread() {
			@Override
			public void run() {
				userLayer.startListeningNetwork();
			}
		}.start();
		
		userLayer.startListeningKeyboard();
	}
}
