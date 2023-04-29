# UdaSecurity

![image](https://user-images.githubusercontent.com/29601694/235325906-3e1dce5d-4867-4c25-805c-3fc0b74300cf.png)

The UdaSecurity project is a basic GUI application for managing home security systems. This project is in need of revisions to prepare it for scaling. The goal is to refactor the program into a multi-module Maven project and write unit tests to verify its functionality using JUnit 5 and Mockito libraries.

## Refactoring
The first step in preparing the UdaSecurity project for scaling is to refactor it into a multi-module Maven project. This will make it easier to manage dependencies and to build and package the project.

![image](https://user-images.githubusercontent.com/29601694/235325974-b9a3a4ee-eb97-41ca-afe9-84981838682a.png)


Split into three modules GUI, Security and Image


GUI            |  Image | Security
:-------------------------:|:-------------------------:|:----------------:
![image](https://user-images.githubusercontent.com/29601694/235326057-f6e086aa-1e66-4273-8595-d98b651f9ca0.png)  |  ![image](https://user-images.githubusercontent.com/29601694/235326144-ce85d412-38c5-4f2d-ac64-1a4e906988b3.png) | ![image](https://user-images.githubusercontent.com/29601694/235326194-26b7e988-60d1-4ae6-9c43-e1df548c1ada.png)
 

## Unit Testing
Unit testing is an essential part of software development. Unit tests ensure that each component of the program performs as expected. For the UdaSecurity project, we will be using JUnit 5 and Mockito libraries to write a full unit test suite.

## Application Requirements to Test:
1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
3. If pending alarm and all sensors are inactive, return to no alarm state.
4. If alarm is active, change in sensor state should not affect the alarm state.
5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
9. If the system is disarmed, set the status to no alarm.
10. If the system is armed, reset all sensors to inactive.
11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.

## Commands

`mvn package`

`java -jar GUI/target/GUI-1.0-SNAPSHOT-jar-with-dependencies.jar`
