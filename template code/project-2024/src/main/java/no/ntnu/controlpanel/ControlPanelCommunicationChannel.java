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

public class ControlPanelCommunicationChannel implements CommunicationChannel {
    private Socket clientSocket;
    private PrintWriter socketWriter;
    private BufferedReader socketReader;
    private final String serverAddress;
    private final int serverPort;
    private boolean isOpen;
    private final ControlPanelLogic logic;


    public ControlPanelCommunicationChannel(ControlPanelLogic logic,String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.isOpen = false;
        this.logic = logic;
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
        if (!isOpen) {
            Logger.error("Connection is not open!");
            return;
        }
                JSONObject message = new JSONObject();
                message.put("id", nodeId);

                JSONArray actuatorsArray = new JSONArray();
                JSONObject actuator = new JSONObject();
                actuator.put("id", actuatorId);
                actuator.put("status", isOn ? "on" : "off");

                actuatorsArray.put(actuator);
                message.put("actuators", actuatorsArray);

        try {
            socketWriter.println(message);
            Logger.info("Sent actuator change command: " + message);
        } catch (Exception e) {
            Logger.error("Failed to send actuator change command: " + e.getMessage());
        }
    }

    public void listenForSensorData() {
        new Thread(() -> {
            String message;
            try {
                while ((message = socketReader.readLine()) != null) {
                    Logger.info("Received sensor data: " + message);
                    JSONObject jsonObject = new JSONObject(message);
                    int nodeId = jsonObject.getInt("id");
                    Logger.info("Adding node info to GUI:"+ nodeId);

                    // Sensor Data updates.
                    if(message.contains("sensors")){
                        SensorActuatorNodeInfo sensorActuatorNodeInfo = new SensorActuatorNodeInfo(nodeId);
                        logic.onNodeAdded(sensorActuatorNodeInfo);

                        List<SensorReading> sensors = parseSensorReadings(message);
                        List<Actuator> actuators = parseActuators(message,sensorActuatorNodeInfo);
                        logic.onSensorData(nodeId, sensors);
                        for (Actuator actuator : actuators) {
                            Logger.info("Sensors info update actuator: " + actuator.getId() + " status:" + actuator.isOn());
                            logic.onActuatorStateChanged(nodeId, actuator.getId(), actuator.isOn());
                        }
                    }else{

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
    private List<SensorReading> parseSensorReadings(String sensorInfo) {
        List<SensorReading> sensorReadings = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(sensorInfo);
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
    private List<Actuator> parseActuators(String jsonMessage, SensorActuatorNodeInfo info) {
        List<Actuator> actuators = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(jsonMessage);
        JSONArray actuatorsArray = jsonObject.getJSONArray("actuators");

        for (int i = 0; i < actuatorsArray.length(); i++) {
            JSONObject actuatorObject = actuatorsArray.getJSONObject(i);
            String type = actuatorObject.getString("type");
            int actuatorId = actuatorObject.getInt("id");
            String status = actuatorObject.getString("status");

            Actuator actuator = new Actuator(type, info.getId(),actuatorId, status);
            actuator.setListener(logic);
            info.addActuator(actuator);
            Logger.info("Adding actuator: " + actuator.getId() + " status:" + actuator.isOn());
            actuators.add(actuator);
        }

        return actuators;
    }



    }
