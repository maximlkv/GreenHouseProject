package no.ntnu.controlpanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.SensorReading;
import no.ntnu.tools.Logger;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * responsible for the communication between control panel and server.
 * establishes the connection, receives and processes sensor data and sends actuator commands to the server
 */
public class ControlPanelCommunicationChannel implements CommunicationChannel {
    private Socket clientSocket;
    private PrintWriter socketWriter;
    private BufferedReader socketReader;
    private final String serverAddress;
    private final int serverPort;
    private boolean isOpen;
    private final ControlPanelLogic logic;
    private final Set<Integer> addedNodes;
    private boolean isGuiReady = false;

    /**
     * Constructor for the communication channel
     *
     * @param logic         logic object which handles received data
     * @param serverAddress server's address
     * @param serverPort    server's port
     */
    public ControlPanelCommunicationChannel(ControlPanelLogic logic, String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.isOpen = false;
        this.logic = logic;
        addedNodes = Collections.synchronizedSet(new HashSet<>());
    }

    /**
     * opens a connection to the server, sends handshake message, and starts listening for sensor data
     *
     * @return true if connection is successfully established, false if not
     */
    @Override
    public boolean open() {
        try {
            clientSocket = new Socket(serverAddress, serverPort);
            socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            isOpen = true;
            socketWriter.println("CONTROL:1:2");
            Logger.info("Connected to server at " + serverAddress + ":" + serverPort);
            listenForSensorData();
            return true;
        } catch (IOException e) {
            Logger.error("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    /**
     * sets the isGuiReady boolean to true, indicating that the control panel can start receiving and processing data
     */

    public void setGuiReady() {
        isGuiReady = true;
        Logger.info("Gui ready");
    }

    /**
     * sends a command to the server to change the state of an actuator
     *
     * @param nodeId     ID of the node to which the actuator is attached
     * @param actuatorId Node-wide unique ID of the actuator
     * @param isOn       When true, actuator must be turned on; off when false.
     */
    @Override
    public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
        if (!isOpen) {
            Logger.error("Connection is not open!");
            return;
        }

        // commands are sent in json format
        JSONObject message = new JSONObject();
        message.put("nodeId", nodeId);
        message.put("actuator", actuatorId);
        message.put("status", isOn);

        try {
            socketWriter.println(message);
            Logger.info("Sent actuator change command: " + message);
        } catch (Exception e) {
            Logger.error("Failed to send actuator change command: " + e.getMessage());
        }
    }

    /**
     * start a background thread that receives sensor data and processes it
     */
    public void listenForSensorData() {
        new Thread(() -> {
            String message;
            try {
                while ((message = socketReader.readLine()) != null) {
                    if (isGuiReady) {
                        Logger.info("Received sensor data: " + message);

                        // get id of node
                        JSONObject jsonObject = new JSONObject(message);
                        int nodeId = jsonObject.getInt("id");
                        SensorActuatorNodeInfo info = new SensorActuatorNodeInfo(nodeId);

                        // make sure that nodes are not duplicate
                        if (!addedNodes.contains(nodeId)) {
                            Logger.info("Adding node info to GUI:" + nodeId);
                            logic.onNodeAdded(info);
                            addedNodes.add(nodeId);
                        }

                        // process sensor readings and actuator updates
                        if (message.contains("sensors")) {
                            List<SensorReading> sensors = parseSensorReadings(message);
                            List<Actuator> actuators = parseActuators(message, info);
                            logic.onSensorData(nodeId, sensors);
                            for (Actuator actuator : actuators) {
                                Logger.info("Updating Actuator States: " + actuator.getId() + ", status: " + actuator.isOn());
                                logic.onActuatorStateChanged(nodeId, actuator.getId(), actuator.isOn());
                            }
                        } else {
                            // update actuators when the message only contains actuator data
                            JSONObject jsonObject2 = new JSONObject(message);
                            JSONArray actuatorsArray = jsonObject2.getJSONArray("actuators");

                            for (int i = 0; i < actuatorsArray.length(); i++) {
                                JSONObject actuatorObject = actuatorsArray.getJSONObject(i);
                                String status = actuatorObject.getString("status");
                                int actuatorId = actuatorObject.getInt("id");

                                boolean isOn = Objects.equals(actuatorObject.getString("status"), "on");
                                Logger.info("Changing actuator: " + actuatorId + " status:" + status);
                                logic.onActuatorStateChanged(nodeId, actuatorId, isOn);
                            }
                        }
                    } else {
                        Logger.info("Waiting for GUI to set up.");
                    }

                }
            } catch (IOException e) {
                Logger.error("Error reading from server: " + e.getMessage());
            }
        }).start();
    }

    /**
     * parse sensor readings from a json string
     *
     * @param sensorInfo json string which contains sensor data
     * @return list of sensor readings
     */
    private List<SensorReading> parseSensorReadings(String sensorInfo) {
        List<SensorReading> sensorReadings = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(sensorInfo);
        JSONArray sensorsArray = jsonObject.getJSONArray("sensors");

        for (int i = 0; i < sensorsArray.length(); i++) {
            JSONObject sensorObject = sensorsArray.getJSONObject(i);

            String type = sensorObject.getString("type");
            double value = sensorObject.getDouble("value");
            String unit = sensorObject.getString("unit");

            Logger.info("Received Sensor Reading: " + type + " " + value + " " + unit);
            sensorReadings.add(new SensorReading(type, value, unit));
        }

        return sensorReadings;
    }

    /**
     * parses actuator data from json string and updates them on the ui
     *
     * @param jsonMessage json string with the actuator data
     * @param info        SensorActuatorNodeInfo object that has to be updated
     * @return list of actuator objects that are parsed
     */
    private List<Actuator> parseActuators(String jsonMessage, SensorActuatorNodeInfo info) {
        List<Actuator> actuators = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(jsonMessage);
        JSONArray actuatorsArray = jsonObject.getJSONArray("actuators");

        for (int i = 0; i < actuatorsArray.length(); i++) {
            JSONObject actuatorObject = actuatorsArray.getJSONObject(i);
            String type = actuatorObject.getString("type");
            int actuatorId = actuatorObject.getInt("id");
            String status = actuatorObject.getString("status");

            Actuator actuator = new Actuator(type, info.getId(), actuatorId, status);
            actuator.setListener(logic);
            info.addActuator(actuator);
            Logger.info("Adding actuator: " + actuator.getId() + " status:" + actuator.isOn());
            actuators.add(actuator);
        }

        return actuators;
    }

    /**
     * closes connection to server
     */
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
