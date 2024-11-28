package no.ntnu.server;

import no.ntnu.tools.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NodeConnection {

    private final Socket socket;
    private PrintWriter socketWriter;
    private BufferedReader socketReader;

    public NodeConnection(Socket socket) {
        this.socket = socket;
        try {
            this.socketWriter = new PrintWriter(socket.getOutputStream(), true);
            this.socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }  catch (IOException e ) {
            Logger.error("Error setting up Node connection: " +e.getMessage());
        }


    }

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getSocketWriter() {
        return socketWriter;
    }

    public BufferedReader getSocketReader() {
        return socketReader;
    }

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

}
