package no.ntnu.connection;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;

import no.ntnu.controlpanel.SensorActuatorNodeInfo;
import no.ntnu.tools.Logger;


public class Server {

    public static int TCP_PORT = 1238;

    private ServerSocket serverSocket;
    private boolean running;

    // Map for nodes which haven't been paired yet. each control node will later be paired with a sensor/actuator node.
    private final Map<Integer, Socket> controlNodes = new ConcurrentHashMap<>();
    private final Map<Integer, Socket> sensorNodes = new ConcurrentHashMap<>();
    private Map<Integer, SensorActuatorNodeInfo> nodesInfo =  new ConcurrentHashMap<>();

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
            Logger.info("Server started...");
            while (running) {
                Socket clientSocket = acceptNextClient();
                if(clientSocket != null) {
                    threadPool.execute(() -> handleClient(clientSocket));
                }
            }
        }
        Logger.info("Server exiting...");
    }

    /**
     * Open a listening TCP socket
     * @return true on success, false on error.
     */
    private boolean openListeningSocket() {
        boolean success = false;
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            Logger.info("Server listening on port " + TCP_PORT);
            success = true;
        } catch (IOException e) {
            Logger.error("Could not open a listening socket on port " + TCP_PORT +
                    ", reason: "+ e.getMessage());
        }
        return success;
    }

    private Socket acceptNextClient() {
        Socket clientSocket = null;
        try {
            clientSocket = serverSocket.accept();
        } catch (IOException e) {
            Logger.error("Could not accept the next client: " + e.getMessage());
        }
        return clientSocket;
    }

    private void handleClient(Socket clientSocket) {
        // receive Node type and ID from newly connected Node and place it according map to wait for pairing
        String handshakeMessage = receiveHandshakeMessageFromClient(clientSocket); // Format NODETYPE:ID
        String[] handshakeParts = splitHandShakeMessage(handshakeMessage, clientSocket);
        if (handshakeParts[0].equals("SENSOR")) {
            parseSensorHandshake(handshakeParts, clientSocket);
        } else if (handshakeParts[0].equals("CONTROL")) {
            parseControlHandShake(handshakeParts, clientSocket);
        }

            /**synchronized (this) {
             if(nodeType.equals("CONTROL")) {
             controlNodes.put(nodeID, clientSocket);
             } else if(nodeType.equals("SENSOR")) {
             if (sensorNodes.containsKey(nodeID)) {
             Logger.error("Duplicate sensor node ID: " + nodeID);
             } else {
             sensorNodes.put(nodeID, clientSocket);
             }

             } else {
             Logger.error("Invalid node type. Closing connection.");
             closeClientSocket(clientSocket);
             return;
             }
             }*/

    }

    private synchronized void parseControlHandShake(String[] handshakeParts, Socket clientSocket) {
        Integer[] wantedSensorNodeIDs = new Integer[handshakeParts.length-1];
        for(int i = 1; i < handshakeParts.length; ++i) {
            wantedSensorNodeIDs[i-1] = Integer.valueOf(handshakeParts[i]);
        }
        for (int id : wantedSensorNodeIDs) {
            if (!sensorNodes.containsKey(id)) {
                Logger.error("Sensor node with requested id is not connected to server: " + id + ". Closing Control Socket.");

                closeClientSocket(clientSocket);
                return;
            }
        }
        passSocketsToNodeHandler(clientSocket, wantedSensorNodeIDs);
    }

    private void passSocketsToNodeHandler(Socket clientSocket, Integer[] wantedSensorNodeIDs) {
        NodeHandler handler = new NodeHandler(this);
        handler.addControlNode(clientSocket);
        for (int id : wantedSensorNodeIDs) {
            handler.addSensorNode(id, sensorNodes.get(id));
        }
        handler.startCommunication();
    }


    private synchronized void parseSensorHandshake(String[] handshakeParts, Socket clientSocket) {
    Integer nodeID = Integer.valueOf(handshakeParts[1]);
    synchronized (sensorNodes) {
        if (sensorNodes.containsKey(nodeID)) {
            Logger.error("Duplicate sensor node ID: " + nodeID + ". Discarding this node.");
        } else {
            sensorNodes.put(nodeID, clientSocket);
        }
    }

}



/** private void pairWaitingNodes() {
 Integer controlNodeID = controlNodes.keySet().iterator().next();
 Integer sensorNodeID = sensorNodes.keySet().iterator().next();
 Socket controlNodeSocket = controlNodes.get(controlNodeID);
 Socket sensorNodeSocket = sensorNodes.get(sensorNodeID);
 System.out.println("Pairing control and sensor node. Control Node ID: " + controlNodeID + ", Sensor Node ID: " + sensorNodeID);
 threadPool.execute(() -> new NodeHandler(this,sensorNodeSocket, controlNodeSocket).run());
 controlNodes.remove(controlNodeID);
 sensorNodes.remove(sensorNodeID);
 }*/

private String[] splitHandShakeMessage(String message, Socket clientSocket) {

    if (message == null || message.trim().isEmpty()) {
        Logger.error("Received invalid handshake message. Closing client socket.");
        closeClientSocket(clientSocket);
        return null;
    }
    String[] parts = message.split(":");
    if (!parts[0].equals("CONTROL") && !parts[0].equals("SENSOR")) {
        Logger.error("Unknown Node Type: " + parts[0] + ". Closing client socket.");
        closeClientSocket(clientSocket);
        return null;
    }
    if(parts[0].equals("SENSOR") && parts.length != 2) {
        Logger.error("Invalid Handshake format. Closing client socket");
        closeClientSocket(clientSocket);
        return null;
    }
    return parts;
}


private String receiveHandshakeMessageFromClient(Socket clientSocket) {
    try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String handshakeMessage = reader.readLine();
        if (handshakeMessage == null || handshakeMessage.trim().isEmpty()) {
            Logger.error("Received empty handshake message. Closing client socket.");
            closeClientSocket(clientSocket);
        }
        return handshakeMessage;
    } catch (IOException e) {
        Logger.error("Error receiving handshake message: " + e.getMessage());
        return null;
    }
}

    private void closeClientSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            Logger.error("Failed to close client socket: " + e.getMessage());
        }
    }

public void addSensorDataNode(SensorActuatorNodeInfo node){
    this.nodesInfo.put(node.getId(), node);
}

}