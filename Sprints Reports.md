# Sprints Reports

We divide the projects in one week sprints. We start with this project on November 2th and we 

We divided the project into one-week sprints. The project started on November 2, 2024, and was structured into 4 sprints with the following dates:

Sprint 1: November 2, 2024 → November 9, 2024

Sprint 2: November 9, 2024 → November 16, 2024

Sprint 3: November 16, 2024 → November 23, 2024

Sprint 4: November 23, 2024 → December 1, 2024



---

## Sprint Overview

Sprint Duration: November 2, 2024 → November 9, 2024

Sprint Number: #1

## Sprint Goals

- Familiarize with the template code to identify where custom code needs to be added.

## Completed Items

- [x]  Create github repository with template code - Owner: Maxim
- [x]  Get the template-code running in your computer - Owner: Maxim
- [x]  Get the template-code running in your computer - Owner: Sol
- [x]  Internal meeting to identify who does what - Owner: Sol & Maxim

## Retrospective

## Blockers & Challenges

- We had to go find the professor to ask him how certain parts of the code were connected.

## Key Learnings

- Meeting in person and code together let us work better.

## Action Items

- [ ]  Meet in person more frequent

## Additional Notes

We realize the project was bigger than we thought and the template-code was not that easy to understand because it is a lot of code.

---

## Sprint Overview

Sprint Duration: November 9, 2024 → November 16, 2024

Sprint Number: #2

## Sprint Goals

- Connect the server to the project with a handler for the clients.
- Define the communication protocol.

## Completed Items

- [x]  Create Server and NodeHandler class. Make sure one node connects to the server. - Owner: Maxim
- [x]  Define protocol communication messages. This should work for sensors sending data to control panel but also control panel sending actuators status. Owner: Sol
- [x]  Show in ControlPanel (GUI Window) the information about sensors. Owner: Sol

## Retrospective

## Blockers & Challenges

- The server is not prepared to escale with more than one node (each tab in the control panel window)

## Key Learnings

- -

## Action Items

- [ ]  Refactor Connection to be able to connect all nodes in the same server.

## Additional Notes

None.

---

## Sprint Overview

Sprint Duration: November 16, 2024 → November 23, 2024

Sprint Number: #3

## Sprint Goals

- Get the project working with each node in one tab in ControlPanel window

## Completed Items

- [x]  Refactor communication adding a list of nodes for the connection to server. Owner: Maxim
- [x]  Get the status from actuators into the window. Owner: Sol

## Retrospective

## Blockers & Challenges

- We just realize we need to change the server again because is not prepared to have more than one control panel.

## Key Learnings

- -

## Action Items

- [ ]  Refactor Connection to be able to connect several control panel nodes to server.

## Additional Notes

None.

---

## Sprint Overview

Sprint Duration: November 23, 2024 → December 1, 2024

Sprint Number: #4

## Sprint Goals

- Finish  project

## Completed Items

- [x]  Overhaul Server and NodeHandler class to be able to handle multiple control panels. Owner: Maxim
- [x]  Bug: Fix Actuators id (They are sending the wrong id in the communication so are not being updated in the UI). Owner: Sol
- [x]  Bugfix: Actuators didn't receive Commands properly. Owner: Maxim
- [x]  Upload sprints reports to Github. Owner: Sol
- [x]  Write the communication Protocol. Owner: Maxim
- [x]  Make video. Owner: Maxim

## Retrospective

## Blockers & Challenges

- -

## Key Learnings

- Plan better next time! Sprint 4 was too big.

## Action Items

-

## Additional Notes

None.