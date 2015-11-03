package shared;

import java.io.IOException;

/**
 * Created by benja on 01.11.2015.
 */
public interface IClientForChatserver {

    /**
     * Authenticates the client with the provided username and password.
     *
     * @param username
     *            the name of the user
     * @param password
     *            the password
     * @return status whether the authentication was successful or not
     * @throws IOException
     *             if an I/O error occurs
     */
    public String login(String username, String password) throws IOException;

    /**
     * Performs a logout if necessary and closes open connections between client
     * and chatserver.
     *
     * @return message stating whether the logout was successful
     * @throws IOException
     *             if an I/O error occurs
     */
    public String logout() throws IOException;

    /**
     * Sends a public message to all users that are currently online.
     *
     * @param message
     *            message to be sent to all online users
     *
     * @return message stating whether the sending was successful
     * @throws IOException
     *             if an I/O error occurs
     */
    public String send(String message) throws IOException;

    /**
     * Performs a lookup of the given username and returns the address (IP:port)
     * that has to be used to establish a private conversation.
     *
     * @param username
     *            communication partner of private conversation.
     *
     * @return a string containing the address (IP:port)
     * @throws IOException
     *             if an I/O error occurs
     */
    public String lookup(String username) throws IOException;

    /**
     * Registers the private address (IP:port) that can be used by another user
     * to establish a private conversation. Furthermore, the client creates a
     * new ServerSocket for the given port and listens for incoming connections
     * from other clients.
     *
     * @param privateAddress
     *            address consisting of 'IP:port' that is used for creating a
     *            TCP connection
     *
     * @return message stating whether the registration was successful
     * @throws IOException
     *             if an I/O error occurs
     */
    public String register(String privateAddress) throws IOException;

}
