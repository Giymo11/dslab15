package client;

import chatserver.Chatserver;
import shared.Command;
import shared.CommandInterpreter;
import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private Socket socket;
	private CommandInterpreter userToServer;
	private ServerHandler serverToUser;

	private InetAddress serverAddress;
	private int serverPort;
	private int serverDatagramPort;
	private int port;
	private int datagramPort;

	private Map<String, String> userAddressMap = new HashMap<>();

	public Map<String, String> getUserAddressMap() {
		return userAddressMap;
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
			socket = new Socket(serverAddress, serverPort);
			userToServer = new CommandInterpreter(userRequestStream, socket.getOutputStream());
			userToServer.register(this);
			new Thread(userToServer).start();
			serverToUser = new ServerHandler(socket.getInputStream(), userResponseStream, this);
			serverToUser.register(serverToUser);
			new Thread(serverToUser).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String login(String username, String password) throws IOException {
		return null;
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

	@Override
	public String register(String privateAddress) throws IOException {
		return null;
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
	public String msg(String username, String message) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@Command
	public String lastMsg() throws IOException {
		return serverToUser.getLastMessage();
	}

	@Override
	@Command
	public String exit() throws IOException {
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
