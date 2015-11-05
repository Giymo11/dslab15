package client;

import shared.Command;
import shared.CommandInterpreter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by benja on 02.11.2015.
 */
public class ServerHandler extends CommandInterpreter {

    private final Client callback;
    private String lastMessage;

    public ServerHandler(InputStream in, OutputStream out, Client callback) {
        super(in, out);
        this.callback = callback;
        lastMessage = "No message received yet!";
    }

    @Command
    public String register(String username, String address) {
        callback.getUserAddressMap().put(username, address);
        List<String> pending = callback.getPendingMessages().get(username);
        if(pending != null && !pending.isEmpty()) {
            try {
                for(String message : pending) {
                    callback.msg(username, message);
                }
                writeLine("Pending messages for " + username + " are being sent.");
            } catch (IOException ex) {
                try {
                    writeLine("Pending messages for " + username + " could not be sent.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return username + " registered under " + address;
    }

    @Command
    public String send(String username, String message) {
        String out = username + ": " + message;
        lastMessage = out;
        return out;
    }

    public String getLastMessage() {
        return lastMessage;
    }
}
