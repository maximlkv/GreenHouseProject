# Communication protocol

This document describes the protocol used for communication between the different nodes of the
distributed application.

## Terminology

* Sensor - a device which senses the environment and describes it with a value (an integer value in
  the context of this project). Examples: temperature sensor, humidity sensor.
* Actuator - a device which can influence the environment. Examples: a fan, a window opener/closer,
  door opener/closer, heater.
* Sensor and actuator node - a computer which has direct access to a set of sensors, a set of
  actuators and is connected to the Internet.
* Control-panel node - a device connected to the Internet which visualizes status of sensor and
  actuator nodes and sends control commands to them.
* Graphical User Interface (GUI) - A graphical interface where users of the system can interact with
  it.

## The underlying transport protocol

We chose TCP as our transport-layer-protocol, because it provides secure data transmission, which 
we considered was important for this application. We used port number 1238, however the servers
listening port can be set as command line argument. On the client side the port number cannot be 
set dynamically as of now.

## The architecture

We decided to create a server that takes the role of a mediator between the clients, the clients
being the control panels and the sensor/actuator nodes. The server handles the initial connection 
and pairing the control nodes with sensor/actuator nodes. The communication itself is being handled 
by a NodeHandler, which receives the messages and forwards them properly.


## The flow of information and events

Control panel nodes:  
The control panel nodes connect to the server and send a handshake message, in which they identify
themselves as control panel nodes and specify the IDs of the sensor/actuator nodes which they want to 
access. The format for that message is the word "CONTROL" followed by the IDs of the requested nodes, 
each seperated by colons. To give an example, a control panel which wants to connect to the nodes with the IDs 1 and 2
would send the following message: "CONTROL:1:2".  
If these nodes are available, they get paired with them and the communication begins. A control panel
continuously listens for incoming sensor data, parses that data and hands it to the Logic layer, which updates the UI.
When checking a box next to the actuators on the GUI, it creates an actuator update message and sends it to the server,
which then forwards it to the appropriate actuator.

Sensor/Actuator nodes:  
Sensor/Actuator nodes connect to the server and also send an initial handshake message, which however looks a bit
different to a control panels message. Theirs only consists of the word "SENSOR" followed by their own  unique ID. 
A sensor with the ID 1 would send the following message: "SENSOR:1".  Once the connection is established,
the node sends a message containing both sensor and actuator state information to the server, 
whenever it takes a periodic reading. It also continuously listens for incoming commands from the control
panel and changes the state of its actuators accordingly.

## Connection and state

As we are using TCP, our communication is both connection-oriented and stateful.


## Message format

We use standardised handshake messages, when the connection is established. These have already
been explained in the section "The flow of information and events".  
Other than that, the communication protocol uses specific value types in its messages.
Sensor readings are structured as JSON objects consisting of the type (e.g., "temperature"),
value (e.g., 23.5), and unit (e.g., "°C"). Actuator states are also transmitted in JSON,
including the fields id (the actuator ID), type (e.g., "fan"), and status (either "on" or "off").
This way we can create a JSON object containing both sensor data as a JSON array
and actuator states as another JSON array. These standardised data types ensure consistent
communication between the sensor nodes and the server.  
Control panels also use JSON objects for their commands. These consist of the fields "nodeID",
representing the ID of the sensor/actuator node, "actuator", which contains the ID of the actuator
that is being addressed and "state", which is either "on" or "off".

### Error messages
In our current implementation, error messages are logged to the terminal using
the Logger class, ensuring that issues can be diagnosed during development and 
maintenance. This approach allows us to trace errors at every step of the 
communication process, greatly simplifying debugging. While this system is effective
for developers, the errors should also be sent to the user for further reliability.
This is currently not implemented.

## An example scenario

1. A sensor node with ID=1 is started. It has a temperature sensor, two humidity sensors. It can
   also open a window.
2. A sensor node with ID=2 is started. It has a single temperature sensor and can control two fans
   and a heater.
3. A control panel node is started. It requests to be connected to the node with ID=1.
4. The control panel is paired with the sensor node and starts receiving the sensor data.
5. The user on that control panel presses the "on" button for the window. 
6. A second control panel is connected and requests the sensor node with ID=2.
7. It gets paired with the requested node and starts receiving sensor data.
8. The user of the second control panel pressed the button "on" for the first fan.
9. The user of the second control panel presses the button "on" for the heater.

## Reliability and security

The system is designed with basic reliability in mind, focusing on error handling,
logging, and data consistency. Security mechanisms like encryption and authentication are
not part of the current implementation.