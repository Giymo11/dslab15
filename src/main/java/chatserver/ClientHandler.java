package chatserver;

import shared.Command;
import shared.CommandInterpreter;
import shared.IClientForChatserver;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

/**
 * Created by benja on 01.11.2015.
 */
public class ClientHandler extends CommandInterpreter implements IClientForChatserver {

    private final Socket socket;
    private final Chatserver callback;
    private String username;
    private String privateAddress;

    public ClientHandler(Socket socket, Chatserver callback) throws IOException {
        super(socket.getInputStream(), socket.getOutputStream());
        this.socket = socket;
        this.callback = callback;
        this.register(this);
    }

    private String checkForLogin() {
        if(username != null)
            return null;
        return "You have to log in!";
    }

    @Override
    @Command
    public String login(String username, String password) throws IOException {
        String tmp = checkForLogin();
        if(tmp == null) {
            return "You are already logged in!";
        } else if(Objects.equals(callback.getUsers().getString(username + ".password"), password)) {
            this.username = username;
            return "Successfully logged in.";
        } else {
            return "Wrong username or password.";
        }
    }

    @Override
    @Command
    public String logout() throws IOException {
        String tmp = checkForLogin();
        if(tmp == null) {
            username = null;
            return "Successfully logged out.";
        }
        return tmp;
    }

    @Override
    @Command
    public String send(String message) throws IOException {
        String tmp = checkForLogin();
        if(tmp == null) {
            for(ClientHandler clientHandler : callback.getClientHandlers()) {
                if(clientHandler.username != null && !clientHandler.username.equals(this.username)) {
                    clientHandler.writeLine("!send " + username + " " + message);
                }
            }
        }
        return tmp;
    }

    @Override
    @Command
    public String lookup(String username) throws IOException {
        String tmp = checkForLogin();
        if(tmp == null) {
            for(ClientHandler clientHandler : callback.getClientHandlers()) {
                System.out.println("User: " + clientHandler.username);
                if(clientHandler.username != null && clientHandler.username.equals(username)) {
                    if(clientHandler.privateAddress != null) {
                        return "!register " + username + " " + clientHandler.privateAddress;
                    } else {
                        return username + " didn't register yet!";
                    }
                }
            }
            return "No user with name " + username + " found.";
        }
        return tmp;
    }

    @Override
    @Command
    public String register(String privateAddress) throws IOException {
        String tmp = checkForLogin();
        if(tmp == null) {
            this.privateAddress = privateAddress;
            return "Successfully registered private IP address.";
        }
        return tmp;
    }

    @Override
    public void close() {
        super.close();
        try {
            logout();
            socket.close();
        } catch (IOException e) {
            System.err.println("Could not close socket");
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPrivateAddress() {
        return privateAddress;
    }
}
