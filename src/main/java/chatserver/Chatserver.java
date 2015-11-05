package chatserver;

import shared.Command;
import shared.Shell;
import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Chatserver implements IChatserverCli, Runnable {

	private final String componentName;
	private final InputStream userRequestStream;
	private final PrintStream userResponseStream;

	private final ExecutorService pool;
	private final ServerSocket serverSocket;
	private final DatagramSocket datagramSocket;

	private final Config config;
	private final Config users;
	private final List<ClientHandler> clientHandlers = new LinkedList<>();

	public static final int UDP_BUFFER_SIZE = 8192;

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
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		users = new Config("user");

		ServerSocket tmpSocket = null;
		try {
			tmpSocket = new ServerSocket(config.getInt("tcp.port"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		serverSocket = tmpSocket != null ? tmpSocket : null;

		DatagramSocket tmpDatagramSocket = null;
		try {
			tmpDatagramSocket = new DatagramSocket(config.getInt("udp.port"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		datagramSocket = tmpDatagramSocket != null ? tmpDatagramSocket : null;

		pool = Executors.newCachedThreadPool();
		Shell shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		pool.execute(shell);
	}

	private class DatagramHandler implements Runnable {
		private byte[] buffer;
		private InetAddress address;
		private int port;

		@Override
		public void run() {
			String message = new String(buffer);
			if(message.trim().equals("!list")) {
				Set<String> usersOnline = getUsersOnline();
				StringBuilder builder = new StringBuilder();
				for(String user : usersOnline) {
					builder.append(user).append(System.lineSeparator());
				}
				byte[] reply = builder.toString().getBytes();
				try {
					datagramSocket.send(new DatagramPacket(reply, reply.length, address, port));
				} catch (IOException e) {
					System.out.println("Could not respond to datagram");
				}
			}
		}

		// This does not reference the outside variable, because it would break thread-safety as it could be modified at any time
		public DatagramHandler(byte[] data, InetAddress address, int port) {
			this.buffer = data;
			this.address = address;
			this.port = port;
		}
	}

	@Override
	public void run() {
		if(serverSocket == null || datagramSocket == null) {
			System.out.println("One or both sockets could not be instantiated!");
			return;
		}
		pool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					byte[] buffer = new byte[UDP_BUFFER_SIZE];
					DatagramPacket bufferPacket = new DatagramPacket(buffer, UDP_BUFFER_SIZE);
					while(!Thread.currentThread().isInterrupted()) {
						datagramSocket.receive(bufferPacket);
						pool.execute(new DatagramHandler(
								bufferPacket.getData().clone(),
								bufferPacket.getAddress(),
								bufferPacket.getPort()
						));
					}
				} catch (IOException e) {
					System.out.println("Could not receive from datagramSocket");
					return;
				}

			}
		});

		while(!Thread.currentThread().isInterrupted()) {
			try {
				Socket socket = serverSocket.accept();
				ClientHandler clientHandler = new ClientHandler(socket, this);
				pool.execute(clientHandler);
				clientHandlers.add(clientHandler);
			} catch (SocketException e) {
				userResponseStream.println("Socket closed");
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private Set<String> getUsersOnline() {
		Set<String> usersOnline = new HashSet<>();
		for(ClientHandler clientHandler : clientHandlers) {
			String username = clientHandler.getUsername();
			if(username != null) {
				usersOnline.add(username);
			}
		}
		return usersOnline;
	}

	@Override
	@Command
	public String users() throws IOException {
		Set<String> usersOnline = getUsersOnline();
		Set<String> usersOffline = users.listKeys();

		StringBuilder builder = new StringBuilder();

		for(String key : usersOffline) {
			String user = key.replace(".password", "");
			builder.append(user).append(" ");
			if(usersOnline.contains(user)) {
				builder.append("online");
			} else {
				builder.append("offline");
			}
			builder.append(System.lineSeparator());
		}

		return builder.toString();
	}

	@Override
	@Command
	public String exit() throws IOException {
		Thread.currentThread().interrupt();
		for(ClientHandler clientHandler : clientHandlers) {
			clientHandler.close();
		}
		serverSocket.close();
		datagramSocket.close();
		pool.shutdownNow();
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);

		new Thread(chatserver).start();
	}

	public Config getConfig() {
		return config;
	}

	public Config getUsers() {
		return users;
	}

	public List<ClientHandler> getClientHandlers() {
		return clientHandlers;
	}
}
