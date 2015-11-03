package chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import shared.Command;
import shared.CommandInterpreter;
import shared.Shell;
import util.Config;

public class Chatserver implements IChatserverCli, Runnable {

	private final String componentName;
	private final InputStream userRequestStream;
	private final PrintStream userResponseStream;

	private final ExecutorService pool;
	private final ServerSocket serverSocket;

	private final Config config;
	private final Config users;
	private final List<ClientHandler> clientHandlers = new LinkedList<>();

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

		ServerSocket tmp = null;
		try {
			tmp = new ServerSocket(config.getInt("tcp.port"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		serverSocket = tmp != null ? tmp : null;

		pool = Executors.newCachedThreadPool();
		Shell shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		pool.execute(shell);
	}

	@Override
	public void run() {
		if(serverSocket == null) {
			System.out.println("Serversocket is not instantiated!");
			return;
		}
		while(true) {
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

	@Override
	@Command
	public String users() throws IOException {
		Set<String> usersOnline = new HashSet<>();
		for(ClientHandler clientHandler : clientHandlers) {
			String username = clientHandler.getUsername();
			if(username != null) {
				usersOnline.add(username);
			}
		}
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
