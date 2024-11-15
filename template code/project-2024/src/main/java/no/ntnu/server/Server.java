package no.ntnu.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static int TCP_PORT = 1238;
    public static final int NUMBER_OF_CHANNELS = 20;
    private boolean isOn = false;
    private int activeChannel = 1;

    private ServerSocket serverSocket;
    private boolean running;

    public static void main(String[] args) {
        if (args.length == 1) {
            TCP_PORT = Integer.valueOf(args[0]);
        }
        Server server = new Server();
        server.run();
    }

    private void run() {
        if (openListeningSocket()) {
            running = true;
            while (running) {
                Socket clientSocket = acceptNextClient();
                NodeHandler nodeHandler = new NodeHandler(this, clientSocket);
                nodeHandler.run();
            }
        }

        System.out.println("Server exiting...");
    }

    /**
     * Open a listening TCP socket
     * @return true on success, false on error.
     */
    private boolean openListeningSocket() {
        boolean success = false;
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            System.out.println("Server listening on port " + TCP_PORT);
            success = true;
        } catch (IOException e) {
            System.err.println("Could not open a listening socket on port " + TCP_PORT +
                    ", reason: "+ e.getMessage());
        }
        return success;
    }

    private Socket acceptNextClient() {
        Socket clientSocket = null;
        try {
            clientSocket = serverSocket.accept();
        } catch (IOException e) {
            System.err.println("Could not accept the next client: " + e.getMessage());
        }
        return clientSocket;
    }

    public int getActiveChannel() {
        return activeChannel;
    }

    private void setActiveChannel(int channel) {
        activeChannel = channel;
    }

    private int getAvailableChannels() {
        return NUMBER_OF_CHANNELS;
    }

    /**
     * Returns true if channel is greater than 1 but lower than NUMBER_OF_CHANNELS
     * @param channel
     * @return
     */
    private boolean isInChannelsRange(int channel) {
        return channel >= 1 && channel <= getAvailableChannels();
    }

    public boolean getIsOn() {
        return isOn;
    }

    public void setIsOn(boolean isOn) {
        this.isOn = isOn;
    }

    public void setChannelUp() {
        int currentChannel = getActiveChannel();
        int newChannel = currentChannel +1;
        if (isInChannelsRange(newChannel)) {
            setActiveChannel(newChannel);
        }else{
            setActiveChannel(1); //Default rule. If if out of range, we return 1 as next channel.
        }
    }

    public void setChannelDown() {
        int currentChannel = getActiveChannel();
        int newChannel = currentChannel - 1;
        if (isInChannelsRange(newChannel)) {
            setActiveChannel(newChannel);
        }else{
            setActiveChannel(getAvailableChannels()); //Default rule. If is out of range,
            // we return max as next channel.
        }
    }
}