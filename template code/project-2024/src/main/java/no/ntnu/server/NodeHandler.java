package no.ntnu.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import no.ntnu.controlpanel.SensorActuatorNodeInfo;
import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.Sensor;
import no.ntnu.greenhouse.SensorReading;
import no.ntnu.tools.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

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
    private Server server;

    public NodeHandler(Server server, Socket sensorSocket, Socket controlSocket) {
        this.server = server;
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