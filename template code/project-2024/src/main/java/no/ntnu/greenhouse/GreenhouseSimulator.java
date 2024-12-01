package no.ntnu.greenhouse;

import java.util.HashMap;
import java.util.Map;

import no.ntnu.listeners.greenhouse.NodeStateListener;
import no.ntnu.tools.Logger;

/**
 * Application entrypoint - a simulator for a greenhouse.
 */
public class GreenhouseSimulator {
    private final Map<Integer, SensorActuatorNode> nodes = new HashMap<>();


    public static void main(String[] args) {
        GreenhouseSimulator greenhouseSimulator = new GreenhouseSimulator();
        greenhouseSimulator.initialize();
        greenhouseSimulator.start(); // Start the greenhouse simulation
        Logger.info("GreenhouseSimulator started");
    }

    /**
     * Create a greenhouse simulator.
     */
    public GreenhouseSimulator() {
    }

    /**
     * Initialise the greenhouse but don't start the simulation just yet.
     */
    public void initialize() {
        createNode(1, 1, 1, 1, 0);
        createNode(1, 0, 0, 2, 1);
        //createNode(2, 0, 0, 0, 0);
        Logger.info("Greenhouse initialized");
    }

    private void createNode(int temperature, int humidity, int windows, int fans, int heaters) {
        SensorActuatorNode node = DeviceFactory.createNode(
                temperature, humidity, windows, fans, heaters);
        nodes.put(node.getId(), node);
    }

    /**
     * Start a simulation of a greenhouse - all the sensor and actuator nodes inside it.
     */
    public void start() {
        initiateCommunication();
        for (SensorActuatorNode node : nodes.values()) {
            node.start();
        }

        Logger.info("Simulator started");
    }


    /**
     * Runs the connectToServer() method for every Node that has been created for this simulator.
     * Passes the sever and port number to the every node.
     */
    private void initiateCommunication() {
        for (SensorActuatorNode node : nodes.values()) {
            // variables for TCP connection
            String serverAddress = "localhost";
            int portNumber = 1238;
            node.connectToServer(serverAddress, portNumber);
        }
    }

    /**
     * Stop the simulation of the greenhouse - all the nodes in it.
     */
    public void stop() {
        for (SensorActuatorNode node : nodes.values()) {
            node.stop();
        }
    }


    /**
     * Add a listener for notification of node staring and stopping.
     *
     * @param listener The listener which will receive notifications
     */
    public void subscribeToLifecycleUpdates(NodeStateListener listener) {
        for (SensorActuatorNode node : nodes.values()) {
            node.addStateListener(listener);
        }
    }
}
