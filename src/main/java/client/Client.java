package client;

import chatserver.Chatserver;
import shared.Command;
import shared.CommandInterpreter;
import util.Config;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Client implements IClientCli, Runnable {

	private final String componentName;
	private final Config config;
	private final InputStream userRequestStream;
	private final PrintStream userResponseStream;

	private Socket serverSocket;
	private ServerSocket socket;
	private CommandInterpreter userToServer;
	private ServerHandler serverToUser;

	private InetAddress serverAddress;
	private final int serverPort;
	private final int serverDatagramPort;
	private final int port;
	private final int datagramPort;

	private String name;

	private final Map<String, String> userAddressMap = new HashMap<>();
	private final Map<String, List<String>> pendingMessages = new HashMap<>();

	public Map<String, String> getUserAddressMap() {
		return userAddressMap;
	}

	public Map<String, List<String>> getPendingMessages() {
		return pendingMessages;
	}

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		try {
			this.serverAddress = InetAddress.getByName(config.getString("chatserver.host"));
		} catch (UnknownHostException e) {
			System.out.println("Server not found!");
		}
		this.serverPort = config.getInt("chatserver.tcp.port");
		this.serverDatagramPort = config.getInt("chatserver.udp.port");
		this.port = config.getInt("tcp.port");
		this.datagramPort = config.getInt("udp.port");
	}

	@Override
	public void run() {
		if(serverAddress == null) {
			return;
		}
		try {
			serverSocket = new Socket(serverAddress, serverPort);
			userToServer = new CommandInterpreter(userRequestStream, serverSocket.getOutputStream());
			userToServer.register(this);
			new Thread(userToServer).start();
			serverToUser = new ServerHandler(serverSocket.getInputStream(), userResponseStream, this);
			serverToUser.register(serverToUser);
			new Thread(serverToUser).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		this.name = username;
		return "!login " + username + " " + password;
	}

	@Override
	public String logout() throws IOException {
		return null;
	}

	@Override
	public String send(String message) throws IOException {
		return null;
	}

	@Override
	public String lookup(String username) throws IOException {
		return null;
	}

	private class ClientHandler implements Runnable {
		private final Socket otherClient;

		public ClientHandler(Socket otherClient) {
			this.otherClient = otherClient;
		}

		@Override
		public void run() {
			try {
				String message = new BufferedReader(new InputStreamReader(
						otherClient.getInputStream())).readLine();

				serverToUser.writeLine(message);

				Writer out = new OutputStreamWriter(otherClient.getOutputStream());
				out.write("!ack" + System.lineSeparator());
				out.flush();
			} catch (IOException e) {
				try {
					serverToUser.writeLine("Could not read the private message");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {
		socket = new ServerSocket(port);

		new Thread(new Runnable() {
			@Override
			public void run() {
				while(!Thread.currentThread().isInterrupted()) {
					try {
						Socket otherClient = socket.accept();
						new Thread(new ClientHandler(otherClient)).start();
					} catch (IOException e) {
						try {
							serverToUser.writeLine("Cannot accept any inbound connections anymore.");
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						return;
					}
				}
			}
		}).start();

		return "!register " + privateAddress;
	}

	@Override
	@Command
	public String list() throws IOException {
		DatagramSocket datagramSocket = new DatagramSocket(datagramPort);
		String command = "!list";
		DatagramPacket packet = new DatagramPacket(
				command.getBytes(),
				command.getBytes().length,
				serverAddress,
				serverDatagramPort);
		datagramSocket.connect(
				serverAddress, serverDatagramPort);
		datagramSocket.send(packet);

		byte[] buffer = new byte[Chatserver.UDP_BUFFER_SIZE];
		packet = new DatagramPacket(buffer, Chatserver.UDP_BUFFER_SIZE);
		datagramSocket.receive(packet);

		datagramSocket.close();

		return new String(packet.getData());
	}

	@Override
	@Command
	public String msg(final String username, final String message) throws IOException {

		new Thread(new Runnable() {
			@Override
			public void run() {
				sendMsg(username, message);
			}
		}).start();
		return "";
	}

	void sendMsg(String username, String message) {

		final String address = userAddressMap.get(username);

		if(address == null) {
			if(pendingMessages.get(username) == null)
				pendingMessages.put(username, new LinkedList<String>());

			synchronized (pendingMessages.get(username)) {
				List<String> messages = pendingMessages.get(username);
				messages.add("[" + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date())
						+ "] " + message);
			}

			try {
				userToServer.writeLine("!lookup " + username);
				serverToUser.writeLine("Could not send message, will deliver as soon as address to " + username + " is known.");
			} catch (IOException e) {
				e.printStackTrace();
			}

			return;
		}

		synchronized (pendingMessages.get(username)) {
			try {
				Socket otherClient = new Socket(InetAddress.getByName(address), port);
				otherClient.getOutputStream().write((
						name + ": " + message + System.lineSeparator()).getBytes()
				);
				String response = new BufferedReader(new InputStreamReader(
						otherClient.getInputStream())).readLine();
				if(response.trim().equals("!ack")) {
					serverToUser.writeLine(username + " replied with !ack");
				}
				otherClient.close();
			} catch (Exception ex) {
				try {
					serverToUser.writeLine("Could not communicate to " + username);
				} catch (IOException e) {
					e.printStackTrace();
				}
				userAddressMap.remove(username);
			}
		}
	}
	
	@Override
	@Command
	public String lastMsg() throws IOException {
		return serverToUser.getLastMessage();
	}

	@Override
	@Command
	public String exit() throws IOException {
		serverSocket.close();
		socket.close();
		userToServer.close();
		serverToUser.close();
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		new Thread(client).start();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
