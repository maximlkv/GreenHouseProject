package no.ntnu.connection;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import no.ntnu.controlpanel.SensorActuatorNodeInfo;
import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.Sensor;
import no.ntnu.tools.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The NodeHandler class manages communication between a client socket and a Server.
 * It handles the initialization and setup required to handle nodes information commands for the Server.
 */
public class NodeHandler {
    private static final ConcurrentHashMap<Integer, NodeConnection> sensorNodesMap = new ConcurrentHashMap<>();
    private static NodeConnection controlNode = null;
    private final ExecutorService nodeThreadPool = Executors.newFixedThreadPool(6);
    private final Server server;

    public NodeHandler(Server server) {
        this.server = server;
    }

    public void addControlNode(Socket socket) {
        controlNode = new NodeConnection(socket);
    }

    public void addSensorNode(Integer sensorNodeID, Socket socket) {
        NodeConnection sensorNode = new NodeConnection(socket);
        sensorNodesMap.put(sensorNodeID, sensorNode);
        Logger.info("Added Sensor node to map: " + sensorNodeID);
    }


    public void startCommunication() {
        Logger.info("Attempting to start communication");

        for (Integer sensorNodeID : sensorNodesMap.keySet()) {
            nodeThreadPool.execute(() -> sensorDataFlow(sensorNodeID, sensorNodesMap.get(sensorNodeID)));
        }
        nodeThreadPool.execute(() -> controlCommandFlow());

    }

    private void controlCommandFlow() {
        try {
            String message;
            while ((message = controlNode.getSocketReader().readLine()) != null) {
                Logger.info("Received actuator command from control panel: " + message);
                forwardActuatorCommand(message);
            }
        }  catch (IOException e) {
            Logger.error("Error reading actuator command on the server: " + e.getMessage());
        }
    }

    private void forwardActuatorCommand(String message) {
        controlNode.getSocketWriter().println(message);
    }

    private void sensorDataFlow(int sensorID, NodeConnection sensorNodeConnection) {
        try {
            String message;
            while ((message = sensorNodeConnection.getSocketReader().readLine()) != null) {
                Logger.info("Received message from sensor node " + sensorID + ": " + message);
                //Save as node in the server
                SensorActuatorNodeInfo sensorActuatorNodeInfo = new SensorActuatorNodeInfo(sensorID);
                updateNodeInfo(sensorActuatorNodeInfo,message);
                server.addSensorDataNode(sensorActuatorNodeInfo);
                controlNode.getSocketWriter().println(message);
            }
        } catch (IOException e) {
            Logger.error("Error reading sensor data on server: " + e.getMessage());
        }
    }


    /**@Override
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
        Logger.info("I think this works: receiveAndRelaySensorData");
        try {
            String sensorDataMessage;
            while ((sensorDataMessage = sensorReader.readLine()) != null) {
                Logger.info("Received data from Sensor Node: " + sensorDataMessage);
                handleSensorDataCommand(sensorDataMessage);

                // Reenviar los datos al Control Node
                controlWriter.println(sensorDataMessage);
                Logger.info("Relayed data to Control Node");
            }
        } catch (IOException e) {
            Logger.error("Error while receiving or relaying sensor data: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }


    private void receiveAndRelayControlCommands() {
        Logger.info("I think this still works.");
        try {
            String commandMessage;
            while ((commandMessage = controlReader.readLine()) != null) {
                Logger.info("Received command from Control Node: " + commandMessage);
                // Reenviar el comando al Sensor Node
                sensorWriter.println(commandMessage);
                Logger.info("Relayed command to Sensor Node");
            }
        } catch (IOException e) {
            Logger.error("Error while receiving or relaying commands: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void handleSensorDataCommand(String commandMessage) {
        JSONObject jsonObject = new JSONObject(commandMessage);
        int nodeId = jsonObject.getInt("id");
        SensorActuatorNodeInfo sensorActuatorNodeInfo = new SensorActuatorNodeInfo(nodeId);

        // Sensor Data updates.
        List<SensorReading> sensors = parseSensorReadings(commandMessage);
        for (SensorReading sensorReading : sensors) {
            sensorActuatorNodeInfo.addSensor(sensorReading);
        }
        List<Actuator> actuators = parseActuators(commandMessage,sensorActuatorNodeInfo);
        for (Actuator actuator : actuators) {
            sensorActuatorNodeInfo.addActuator(actuator);
        }
        server.addSensorDataNode(sensorActuatorNodeInfo);
    }

    private List<Actuator> parseActuators(String commandMessage, SensorActuatorNodeInfo info) {
        List<Actuator> actuators = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(commandMessage);
        JSONArray actuatorsArray = jsonObject.getJSONArray("actuators");

        for (int i = 0; i < actuatorsArray.length(); i++) {
            JSONObject actuatorObject = actuatorsArray.getJSONObject(i);
            String type = actuatorObject.getString("type");

            Actuator actuator = new Actuator(type, info.getId());
            actuators.add(actuator);
        }

        return actuators;
    }

    private List<SensorReading> parseSensorReadings(String commandMessage) {
        List<SensorReading> sensorReadings = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(commandMessage);
        JSONArray sensorsArray = jsonObject.getJSONArray("sensors");
        for (int i = 0; i < sensorsArray.length(); i++) {
            JSONObject sensorObject = sensorsArray.getJSONObject(i);

            String type = sensorObject.getString("type");
            double value = sensorObject.getDouble("value");
            String unit = sensorObject.getString("unit");
            sensorReadings.add(new SensorReading(type, value, unit));
        }
        return sensorReadings;
    }
*/

    private void closeConnection() {
        // TODO -- closing of connection for each NodeConnection
    }

    public void updateNodeInfo(SensorActuatorNodeInfo nodeInfo, String message){
        JSONObject jsonObject = new JSONObject(message);
        JSONArray actuatorsArray = jsonObject.getJSONArray("actuators");

        for (int i = 0; i < actuatorsArray.length(); i++) {
            JSONObject actuatorObject = actuatorsArray.getJSONObject(i);
            String type = actuatorObject.getString("type");
            int actuatorId = actuatorObject.getInt("id");
            String status = actuatorObject.getString("status");

            Actuator actuator = new Actuator(type, nodeInfo.getId(),actuatorId, status);
            Logger.info("Adding actuator: " + actuator.getId() + " status:" + actuator.isOn());
            nodeInfo.addActuator(actuator);
        }
    }


}