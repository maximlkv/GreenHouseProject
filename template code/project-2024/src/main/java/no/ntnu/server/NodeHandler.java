package no.ntnu.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * The NodeHandler class manages communication between a client socket and a Server.
 * It handles the initialization and setup required to handle nodes information commands for the Server.
 */
public class NodeHandler {
    private Server server;
    private Socket clientSocket;
    private BufferedReader socketReader;
    private PrintWriter socketWriter;

    public NodeHandler(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
        System.out.println("Client connected. Address: " + clientSocket.getRemoteSocketAddress()
                + ", Port: " + clientSocket.getPort());
    }

    public void run() {
        try {
            socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            sendInterfaceToRemote();
            String remoteCommand;

            while ((remoteCommand = socketReader.readLine()) != null) {
                System.out.println("Received command: " + remoteCommand);
                handleCommand(remoteCommand);

            }

        } catch (IOException e) {
            System.err.println("Error while processing the client: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }


    private void closeConnection() {
        try {
            if (socketReader != null) {
                socketReader.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Client connection closed.");
            }
        } catch (IOException e) {
            System.err.println("Error while closing the client connection: " + e.getMessage());
        }
    }

    private void handleCommand(String remoteCommand) {
        try {
            if (remoteCommand.equals("1")) {
                handleOnOfCommand();
            }
            else if (remoteCommand.equals("2") && server.getIsOn()) {
                handleChannelUpCommand();
            }
            else if (remoteCommand.equals("3") && server.getIsOn()) {
                handleChannelDownCommand();
            } else {
                socketWriter.println("Unknown command. Please use one of the following buttons: ");
            }
            sendInterfaceToRemote();

        } catch (Exception e) {
            System.err.println("Error handling Command: " + e.getMessage());
        }
    }

    private void handleChannelDownCommand() {
        server.setChannelDown();
        socketWriter.println("Channel down. New Channel: " + server.getActiveChannel());
    }

    private void handleChannelUpCommand() {
        server.setChannelUp();
        socketWriter.println("Channel up. New Channel: " + server.getActiveChannel());
    }

    private void handleOnOfCommand() {
        if (!server.getIsOn()) {
            server.setIsOn(true);
            socketWriter.println("TV turned on.");
        } else {
            server.setIsOn(false);
            socketWriter.println("TV turned off.");
        }
    }

    private void sendInterfaceToRemote() {
        try {
            if (server.getIsOn()) {
                socketWriter.println("---------------------------------------");
                socketWriter.println("Current Channel: " + server.getActiveChannel() + "\n");
                socketWriter.println("Controls:");
                socketWriter.println("1 - Turn TV on/off");
                socketWriter.println("2 - Switch one channel up");
                socketWriter.println("3 - Switch one channel down");
                socketWriter.println("---------------------------------------");
                socketWriter.println("END_OF_MESSAGE");
            } else {
                socketWriter.println("---------------------------------------");
                socketWriter.println("TV turned off. Press 1 to turn TV on.");
                socketWriter.println("---------------------------------------");
                socketWriter.println("END_OF_MESSAGE");
            }
        } catch (Exception e) {
            System.err.println("Error sending info to writer: " + e.getMessage());
        }

    }
}