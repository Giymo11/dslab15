package client;

import shared.IClientForChatserver;

import java.io.IOException;

public interface IClientCli extends IClientForChatserver {

	// --- Commands needed for Lab 1 ---

	/**
	 * Sends a private message to the given user. In order to establish a
	 * private connection to the other user an implicit lookup has to be
	 * performed.
	 *
	 * @param username
	 *            user that should receive the private message
	 * @param message
	 *            message to be sent to all online users
	 * 
	 * @return message stating whether the sending was successful
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String msg(String username, String message) throws IOException;

	/**
	 * Prints the last received message, considering only public
	 * messages.
	 * 
	 * @return a string containing the last received message
	 * @throws IOException
	 */
	public String lastMsg() throws IOException;

	/**
	 * Performs a shutdown of the client and release all resources.<br/>
	 * Shutting down an already terminated client has no effect.
	 * <p/>
	 * Logout the user if necessary and be sure to releases all resources, stop
	 * all threads and close any open sockets.
	 *
	 * @return exit message
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String exit() throws IOException;

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	/**
	 * Authenticates the client with the provided username and key.
	 *
	 * @param username
	 *            the name of the user
	 * @return status whether the authentication was successful or not
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String authenticate(String username) throws IOException;

	/**
	 * Lists all online users. This command is the only command
	 * that does not require a logged in user. Additionally, this command is
	 * transmitted and received via UDP.
	 *
	 * @return a string containing all the known users.
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String list() throws IOException;

}
