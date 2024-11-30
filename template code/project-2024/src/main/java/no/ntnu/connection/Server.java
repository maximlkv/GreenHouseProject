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

import no.ntnu.tools.Logger;


/**
 * A class for our server, which serves as mediator between control panels and the sensor/actuator nodes.
 * It accepts clients, saves unpaired sensor/actuator nodes in a map and pairs them with control nodes upon request.
 * Paired nodes are passed to a nodehandler that manages their communication.
 */
public class Server {

    //default TCP-port
    public static int TCP_PORT = 1238;
    private ServerSocket serverSocket;
    private boolean running;

    // Map for sensor/actuator nodes which haven't been paired yet. their nodeIds serve as keys
    private final Map<Integer, Socket> sensorNodes = new ConcurrentHashMap<>();

    // thread pool for handling newly connected clients concurrently
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);


    /**
     * main-method that starts the server. TCP-port can be set via command line argument.
     *
     * @param args command-line arguments, where the first argument can specify the TCP port.
     */
    public static void main(String[] args) {
        if (args.length == 1) {
            TCP_PORT = Integer.parseInt(args[0]);
        }
        Server server = new Server();
        server.run();
    }

    /**
     * opens a listening socket and continuously accepts clients, if any try to connect to server.
     * each connection is handled in a separate thread from the thread pool
     */
    private void run() {
        if (openListeningSocket()) {
            running = true;
            Logger.info("Server started...");
            while (running) {
                Socket clientSocket = acceptNextClient();
                if (clientSocket != null) {
                    // handle client connection in separate thread
                    threadPool.execute(() -> handleClient(clientSocket));
                }
            }
        }
        Logger.info("Server exiting...");
    }

    /**
     * Open a listening TCP socket
     *
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
                    ", reason: " + e.getMessage());
        }
        return success;
    }

    /**
     * accepts the next client connection
     *
     * @return The client socket or null if it fails
     */
    private Socket acceptNextClient() {
        Socket clientSocket = null;
        try {
            clientSocket = serverSocket.accept();
        } catch (IOException e) {
            Logger.error("Could not accept the next client: " + e.getMessage());
        }
        return clientSocket;
    }

    /**
     * handles newly connected client by receiving a handshake message and routing it to appropriate method for
     * parsing
     *
     * @param clientSocket socket of client
     */
    private void handleClient(Socket clientSocket) {
        // receive and split handshake message
        String handshakeMessage = receiveHandshakeMessageFromClient(clientSocket);
        String[] handshakeParts = splitHandShakeMessage(handshakeMessage, clientSocket);
        // discern between sensor nodes and control nodes to route them to correct method
        if (handshakeParts[0].equals("SENSOR")) {
            parseSensorHandshake(handshakeParts, clientSocket);
        } else if (handshakeParts[0].equals("CONTROL")) {
            parseControlHandShake(handshakeParts, clientSocket);
        }
    }

    /**
     * receives the handshake message from the client
     *
     * @param clientSocket client which sends handshake message
     * @return handshake message or null if an exception is thrown
     */
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

    /**
     * Splits handshake message and makes sure that it is in the expected format/not empty
     *
     * @param message      handshake message
     * @param clientSocket socket of connected node
     * @return String array with parts of handshake message or null if invalid handshake message
     */
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
        if (parts[0].equals("SENSOR") && parts.length != 2) {
            Logger.error("Invalid Handshake format. Closing client socket");
            closeClientSocket(clientSocket);
            return null;
        }
        return parts;
    }

    /**
     * parses the handshake message of a sensor/actuator node and adds it to the unpaired sensorNodes map
     *
     * @param handshakeParts parts of the handshake message
     * @param clientSocket   socket of the node
     */
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

    /**
     * parse handshake message from a control node and attempt to establish connection between control node
     * and its requested sensor/actuator nodes
     * handshake has the following format -> CONTROL:1:2:3 (amount of requested sensor/actuator nodes
     * can be more or less than 3)
     *
     * @param handshakeParts parts of handshake message
     * @param clientSocket   socket of the control node
     */
    private synchronized void parseControlHandShake(String[] handshakeParts, Socket clientSocket) {
        Integer[] wantedSensorNodeIDs = new Integer[handshakeParts.length - 1];
        for (int i = 1; i < handshakeParts.length; ++i) {
            wantedSensorNodeIDs[i - 1] = Integer.valueOf(handshakeParts[i]);
        }
        // ensure that requested sensor/actuator nodes are connected to the server
        for (int id : wantedSensorNodeIDs) {
            if (!sensorNodes.containsKey(id)) {
                Logger.error("Sensor node with requested id is not connected to server: " + id + ". Closing Control Socket.");

                closeClientSocket(clientSocket);
                return;
            }
        }
        passSocketsToNodeHandler(clientSocket, wantedSensorNodeIDs);
    }

    /**
     * Creates a node handler, which is responsible for managing the communication between
     * a control node and its connected sensor/actuator nodes
     *
     * @param clientSocket        socket of control node
     * @param wantedSensorNodeIDs ids of the sensor nodes that the control panel wants to connect to
     */
    private void passSocketsToNodeHandler(Socket clientSocket, Integer[] wantedSensorNodeIDs) {
        NodeHandler handler = new NodeHandler(this);
        handler.addControlNode(clientSocket);
        for (int id : wantedSensorNodeIDs) {
            handler.addSensorNode(id, sensorNodes.get(id));
        }
        handler.startCommunication();
    }

    /**
     * closes the client socket
     *
     * @param socket socket which is supposed to be closed
     */
    private void closeClientSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            Logger.error("Failed to close client socket: " + e.getMessage());
        }
    }


}