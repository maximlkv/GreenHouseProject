package no.ntnu.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import no.ntnu.tools.Logger;

/**
 * The NodeHandler class manages communication between a client socket and a Server.
 * It handles the initialization and setup required to handle nodes information commands for the Server.
 */
public class NodeHandler implements Runnable{
    private Socket sensorSocket;
    private Socket controlSocket;
    private BufferedReader sensorReader;
    private PrintWriter sensorWriter;
    private BufferedReader controlReader;
    private PrintWriter controlWriter;
    private ExecutorService nodeThreadPool = Executors.newFixedThreadPool(2);


    public NodeHandler(Socket sensorSocket, Socket controlSocket) {
        this.sensorSocket = sensorSocket;
        this.controlSocket = controlSocket;
        System.out.println("Nodes connected");
    }

    @Override
    public void run() {
        try {
            sensorReader = new BufferedReader(new InputStreamReader(sensorSocket.getInputStream()));
            sensorWriter = new PrintWriter(sensorSocket.getOutputStream(), true);
            controlReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            controlWriter = new PrintWriter(controlSocket.getOutputStream(), true);
        } catch(IOException e) {
            Logger.error("Issue setting up input and output streams for Nodes:" + e.getMessage());
        }
        
            
        nodeThreadPool.execute(() -> receiveAndRelaySensorData());
        nodeThreadPool.execute(() -> receiveAndRelayControlCommands());
    }

    private void receiveAndRelaySensorData() {
        Logger.info("I think this works.");
        closeConnection();
    }

    private void receiveAndRelayControlCommands() {
        Logger.info("I think this still works.");
        closeConnection();
    }

    private void closeConnection() {
        try {
            if(sensorReader != null) {
                sensorReader.close();
            }
            if(controlReader != null) {
                controlReader.close();
            }   
            if(sensorWriter != null) {
                sensorWriter.close();
            } 
            if(controlWriter != null) {
                controlWriter.close();
            }
            if(sensorSocket != null && !sensorSocket.isClosed())  {
                sensorSocket.close();
            }
            if(controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.isClosed();
            }
            
        } catch (IOException e) { 
            System.err.println("Error while closing the client connections: " + e.getMessage());
        }
    }
}