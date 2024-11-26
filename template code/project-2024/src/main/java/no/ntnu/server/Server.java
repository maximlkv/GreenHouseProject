package no.ntnu.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.BufferedReader;
import no.ntnu.tools.Logger;


public class Server {

    public static int TCP_PORT = 1238;

    private ServerSocket serverSocket;
    private boolean running;

    // Map for nodes which haven't been paired yet. each control node will later be paired with a sensor/actuator node.
    private final Map<Integer, Socket> controlNodes = new ConcurrentHashMap<>();
    private final Map<Integer, Socket> sensorNodes = new ConcurrentHashMap<>();

    // thread pool for handling newly connected clients
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        if (args.length == 1) {
            TCP_PORT = Integer.parseInt(args[0]);
        }
        Server server = new Server();
        server.run();
    }

    private void run() {
        if (openListeningSocket()) {
            running = true;
            System.out.println("Server started...");
            while (running) {
                Socket clientSocket = acceptNextClient();
                if(clientSocket != null) {
                    threadPool.execute(() -> handleClient(clientSocket));
                }
            }
        }
        System.out.println("Server exiting...");
    }

    /**
     * Open a listening TCP socket
     * @return true on success, false on error.
     */
    private boolean openListeningSocket() {
        boolean success = false;
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            System.out.println("Server listening on port " + TCP_PORT);
            success = true;
        } catch (IOException e) {
            System.err.println("Could not open a listening socket on port " + TCP_PORT +
                    ", reason: "+ e.getMessage());
        }
        return success;
    }

    private Socket acceptNextClient() {
        Socket clientSocket = null;
        try {
            clientSocket = serverSocket.accept();
        } catch (IOException e) {
            System.err.println("Could not accept the next client: " + e.getMessage());
        }
        return clientSocket;
    }

    private void handleClient(Socket clientSocket) {
        try {
            
            // receive Node type and ID from newly connected Node and place it according map to wait for pairing

            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String handshakeMessage = reader.readLine().trim(); // Expected format: NODE_TYPE:ID

            if (handshakeMessage.trim().isEmpty()) {
                Logger.error("Received empty handshake message. Closing client socket.");
                clientSocket.close();
                return;
            }

            String[] handshakeParts = handshakeMessage.split(":");
            if(handshakeParts.length != 2) {
                Logger.error("Invalid Handshake format. Closing client socket");
                clientSocket.close();
            }
            String nodeType = handshakeParts[0];
            int nodeID;
            try {
                nodeID = Integer.parseInt(handshakeParts[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid Node ID. Closing client socket.");
                clientSocket.close();
                return;
            }
            System.out.println("New node connected. Type: " + nodeType + ", ID: " +  nodeID);

            if(nodeType.equals("CONTROL")) {
                controlNodes.put(nodeID, clientSocket);
            } else if(nodeType.equals("SENSOR")) {
                sensorNodes.put(nodeID, clientSocket);
            } else {
                System.err.println("Invalid node type. Closing connection.");
                clientSocket.close();
                return;
            }

            // pair control and sensor/actuator nodes if there are any unpaired nodes waiting. afterwards remove them from the maps.
            
            if(!controlNodes.isEmpty() && !sensorNodes.isEmpty()) {

                Integer controlNodeID = controlNodes.keySet().iterator().next();
                Integer sensorNodeID = sensorNodes.keySet().iterator().next();
                Socket controlNodeSocket = controlNodes.get(controlNodeID);
                Socket sensorNodeSocket = sensorNodes.get(sensorNodeID);
                   
                System.out.println("Pairing control and sensor node. Control Node ID: " + controlNodeID + ", Sensor Node ID: " + sensorNodeID);
                threadPool.execute(() -> new NodeHandler(controlNodeSocket, sensorNodeSocket));

                controlNodes.remove(controlNodeID);
                sensorNodes.remove(sensorNodeID);
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ioException) {
                System.err.println("Failed to close client socket: " + ioException.getMessage());
            }
        }
    }
    
}