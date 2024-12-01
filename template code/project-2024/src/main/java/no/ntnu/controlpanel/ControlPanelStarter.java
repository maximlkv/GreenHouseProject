package no.ntnu.controlpanel;

import no.ntnu.gui.controlpanel.ControlPanelApplication;
import no.ntnu.tools.Logger;

/**
 * Starter class for the control panel.
 * Note: we could launch the Application class directly, but then we would have issues with the
 * debugger (JavaFX modules not found)
 */
public class ControlPanelStarter {

    CommunicationChannel channel;

    public ControlPanelStarter() {
    }

    /**
     * Entrypoint for the application.
     *
     * @param args command line arguments not used
     */
    public static void main(String[] args) {

        ControlPanelStarter starter = new ControlPanelStarter();
        starter.start();
    }

    private void start() {


        ControlPanelLogic logic = new ControlPanelLogic();
        channel = initiateCommunication(logic);
        ControlPanelApplication.startApp(logic, channel);
        // This code is reached only after the GUI-window is closed
        Logger.info("Exiting the control panel application");

        stopCommunication();

    }

    private CommunicationChannel initiateCommunication(ControlPanelLogic logic) {
        CommunicationChannel channel;
        channel = initiateSocketCommunication(logic);
        return channel;
    }

    /**
     * Sets up connection to Server. Creates a Communication Channel and if its opened successfully the communication
     * Channel gets passed to the Control Panel logic.
     *
     * @param logic ControlPanelLogic object which receives the communication channel
     * @return the CommunicationChannel, will later be used to start the ControlPanelApplication
     */
    private CommunicationChannel initiateSocketCommunication(ControlPanelLogic logic) {
        String serverAddress = "localhost";
        int portNumber = 1238;
        ControlPanelCommunicationChannel communicationChannel = new ControlPanelCommunicationChannel(logic, serverAddress, portNumber);
        if (communicationChannel.open()) {
            System.out.println();
            logic.setCommunicationChannel(communicationChannel);
            return communicationChannel;
        } else {
            Logger.error("Failed to establish connection to server.");
            return null;
        }

    }


    private void stopCommunication() {
        if (channel != null) {
            channel.close();
        }
    }

}
