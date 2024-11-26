package no.ntnu.controlpanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import no.ntnu.run.ControlPanelStarter;
import no.ntnu.tools.Logger;

public class ControlPanelCommunicationChannel implements CommunicationChannel {
    private Socket clientSocket;
    private PrintWriter socketWriter;
    private BufferedReader socketReader;
    private final String serverAddress;
    private final int serverPort;
    private boolean isOpen;

    public ControlPanelCommunicationChannel(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.isOpen = false;
    }

    @Override
    public boolean open() {
        try {
            clientSocket = new Socket(serverAddress, serverPort);
            socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            isOpen = true;
            socketWriter.println("CONTROL:1");
            Logger.info("Connected to server at " + serverAddress + ":" + serverPort);
            return true;
        } catch (IOException e) {
            Logger.error("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
        // TODO - implement proper commands (this is a draft by chatgpt for now)
        if (!isOpen) {
            Logger.error("Connection is not open!");
            return;
        }

        String message = String.format("ACTUATOR_CHANGE;%d;%d;%b", nodeId, actuatorId, isOn);

        try {
            socketWriter.println(message);
            Logger.info("Sent actuator change command: " + message);
        } catch (Exception e) {
            Logger.error("Failed to send actuator change command: " + e.getMessage());
        }
    }

    public void listenForSensorData() {
    // TODO - implement listening to sensor data properly (currently chatgpt code)
        new Thread(() -> {
            String message;
            try {
                while ((message = socketReader.readLine()) != null) {
                    Logger.info("Received sensor data: " + message);
                }
            } catch (IOException e) {
                Logger.error("Error reading from server: " + e.getMessage());
            }
        }).start();
    }

    public void close() {
        try {
            if (socketWriter != null) socketWriter.close();
            if (socketReader != null) socketReader.close();
            if (clientSocket != null) clientSocket.close();
            isOpen = false;
            Logger.info("Connection closed.");
        } catch (IOException e) {
            Logger.error("Error closing the connection: " + e.getMessage());
        }
    }
}
