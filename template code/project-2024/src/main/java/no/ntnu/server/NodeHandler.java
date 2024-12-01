package no.ntnu.server;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import no.ntnu.tools.Logger;
import org.json.JSONObject;

/**
 * the node handler handles the communication between a Control panel and the sensor/actuator nodes which it monitors.
 * it makes sure the messages are sent to the appropriate receiver.
 */
public class NodeHandler {
    // map to maintain the sensor/actuator nodes
    private static final ConcurrentHashMap<Integer, NodeConnection> sensorNodesMap = new ConcurrentHashMap<>();
    private static NodeConnection controlNode = null;
    //thread pool is used to make sure each node's communication can run concurrently
    private final ExecutorService nodeThreadPool = Executors.newFixedThreadPool(6);
    private final Server server;


    /**
     * Constructor for node handler class
     *
     * @param server the server that created the node handler
     */
    public NodeHandler(Server server) {
        this.server = server;
    }


    /**
     * takes in control node and creates a NodeConnection object to store its socket, reader and writer
     *
     * @param socket socket of this control panel
     */
    public void addControlNode(Socket socket) {
        controlNode = new NodeConnection(socket);
    }

    /**
     * takes in sensor/actuator node, creates a NodeConnection object and stores it in a map, with
     * its id being the key
     *
     * @param sensorNodeID id of the sensor node
     * @param socket socket of the sensor node
     */
    public void addSensorNode(Integer sensorNodeID, Socket socket) {
        NodeConnection sensorNode = new NodeConnection(socket);
        sensorNode.setUnpaired(false);
        sensorNodesMap.put(sensorNodeID, sensorNode);
        Logger.info("Added Sensor node to map: " + sensorNodeID);
    }

    /**
     * starts the communication threads for every node
     */
    public void startCommunication() {
        Logger.info("Attempting to start communication");

        // start thread for sending sensor data to the control panel
        for (Integer sensorNodeID : sensorNodesMap.keySet()) {
            nodeThreadPool.execute(() -> sensorDataFlow(sensorNodeID, sensorNodesMap.get(sensorNodeID)));
        }
        // start thread for the sending control panel commands to sensor/actuator nodes
        nodeThreadPool.execute(this::controlCommandFlow);

    }

    /**
     * forwards sensor data from a sensor/actuator node to the control panel
     *
     * @param sensorID             the id of the sensor/actuator node
     * @param sensorNodeConnection NodeConnection object corresponding to the sensor/actuator node
     */
    private void sensorDataFlow(int sensorID, NodeConnection sensorNodeConnection) {
        try {
            String message;
            while ((message = sensorNodeConnection.getSocketReader().readLine()) != null) {
                synchronized (this) {
                    if(sensorNodeConnection.isUnpaired()) {
                        break;
                    }
                }
                Logger.info("Received message from sensor node " + sensorID + ": " + message);
                //Save as node in the server
                controlNode.getSocketWriter().println(message);
            }
        } catch (IOException e) {
            Logger.error("Error reading sensor data on server: " + e.getMessage());
        }
    }

    /**
     * manages the forwarding of received commands from the control panel to a sensor node
     */
    private void controlCommandFlow() {
        try {
            Logger.info("waiting for control commands");
            String message;
            while ((message = controlNode.getSocketReader().readLine()) != null) {
                Logger.info("Received actuator command from control panel: " + message);
                forwardActuatorCommand(message);
            }
        } catch (IOException e) {
            Logger.error("Error reading actuator command on the server: " + e.getMessage());
        } finally {
            Logger.info("Control panel connection lost. Closing this NodeHandler instance and passing sensor" +
                    "nodes back to server.");
            closeConnection();
        }
    }

    /**
     * forwards a command from the control panel the appropriate sensor/actuator node
     *
     * @param message command from control panel
     */
    private void forwardActuatorCommand(String message) {
        JSONObject msg = new JSONObject(message);
        int nodeID = msg.getInt("nodeId");
        if (sensorNodesMap.containsKey(nodeID)) {
            sensorNodesMap.get(nodeID).getSocketWriter().println(msg);
        } else {
            Logger.error("Invalid Sensor node id:" + nodeID + ". Actuator command cannot be forwarded.");
        }
    }


    /**
     * closes connection of the control node and passes all the connected sensors back to the server,
     * so they can be paired again
     */
    private void closeConnection() {

        if (controlNode != null) {
            controlNode.closeConnection();
            Logger.info("Closed control node connection");
        }

        for (Map.Entry<Integer, NodeConnection> entry : sensorNodesMap.entrySet()) {
            Integer sensorNodeID = entry.getKey();
            NodeConnection sensorNode = entry.getValue();
            synchronized (this){
                sensorNode.setUnpaired(true);
            }

            server.getSensorNodes().put(sensorNodeID, sensorNode.getSocket());
        }

    }

    /**
     * shuts down the thread pool once this node handler instance is no longer needed.
     * this method has been written by chatgpt
     */
    public void stopThreadPool() {
        nodeThreadPool.shutdown();
        try {
            if (!nodeThreadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                Logger.error("ExecutorService did not terminate in time, forcing shutdown...");
                nodeThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Logger.error("Shutdown interrupted: " + e.getMessage());
            nodeThreadPool.shutdownNow();
        }
        Logger.info("NodeHandler stopped successfully.");
    }


}