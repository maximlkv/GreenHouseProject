package no.ntnu.server;

import no.ntnu.tools.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


/**
 * represents a connection to the server. stores the socket, writer, and reader and provides methods to access these
 */
public class NodeConnection {


    private boolean isUnpaired;
    private final Socket socket;
    private PrintWriter socketWriter;
    private BufferedReader socketReader;

    /**
     * Constructor for a NodeConnection object. initializes input and output streams
     *
     * @param socket socket representing the connection
     */
    public NodeConnection(Socket socket) {
        this.socket = socket;
        try {
            this.socketWriter = new PrintWriter(socket.getOutputStream(), true);
            this.socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            Logger.error("Error setting up Node connection: " + e.getMessage());
        }


    }

    /**
     * getter for the socket
     *
     * @return socket of this connection
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * getter for the output stream
     *
     * @return PrintWriter, which represents this sockets output stream
     */
    public PrintWriter getSocketWriter() {
        return socketWriter;
    }

    /**
     * getter for input stream
     *
     * @return BufferedReader, which represents this sockets input stream
     */
    public BufferedReader getSocketReader() {
        return socketReader;
    }

    /**
     * close input and output stream and then close the socket
     */
    public void closeConnection() {
        try {
            if (socketWriter != null) {
                socketWriter.close();
            }
        } catch (Exception e) {
            Logger.error("Error closing socket output stream: " + e.getMessage());
        }

        try {
            if (socketReader != null) {
                socketReader.close();
            }
        } catch (IOException e) {
            Logger.error("Error closing socket input stream: " + e.getMessage());
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Logger.error("Error closing socket: " + e.getMessage());
        }

        Logger.info("NodeConnection closed successfully.");
    }

    public boolean isUnpaired() {
        return isUnpaired;
    }

    public void setUnpaired(boolean unpaired) {
        isUnpaired = unpaired;
    }

}
