package no.ntnu.controlpanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

import no.ntnu.greenhouse.SensorReading;
import no.ntnu.run.ControlPanelStarter;
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

    public ControlPanelCommunicationChannel(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.isOpen = false;
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
        // TODO - implement proper commands (this is a draft by chatgpt for now)
        if (!isOpen) {
            Logger.error("Connection is not open!");
            return;
        }

        String message = String.format("ACTUATOR_CHANGE;%d;%d;%b", nodeId, actuatorId, isOn);

        try {
            socketWriter.println(message);
            Logger.info("Sent actuator change command: " + message);
        } catch (Exception e) {
            Logger.error("Failed to send actuator change command: " + e.getMessage());
        }
    }

    public void listenForSensorData(ControlPanelLogic logic) {
    // TODO - implement listening to sensor data properly (currently chatgpt code)
        new Thread(() -> {
            String message;
            try {
                while ((message = socketReader.readLine()) != null) {
                    Logger.info("Received sensor data: " + message);
                    JSONObject jsonObject = new JSONObject(message);
                    int nodeId = jsonObject.getInt("source");
                    Logger.info("Adding node info to GUI:"+ nodeId);
                    logic.onNodeAdded(new SensorActuatorNodeInfo(nodeId));

                    // Sensor Data updates.
                    // TODO we need to update this message before
                   // List<SensorReading> sensors = parseSensorReadings(message);
                   // logic.onSensorData(nodeId, sensors);
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

        JSONArray jsonArray = new JSONArray(sensorInfo);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            String type = jsonObject.getString("type");
            double value = jsonObject.getDouble("value");
            String unit = jsonObject.getString("unit");

            SensorReading reading = new SensorReading(type, value, unit);
            sensorReadings.add(reading);
        }

        return sensorReadings;

    }
}
