# Multi-Agent Traffic Control & Simulation System (JADE)

Decentralized urban traffic simulation and control system built in **Java** using the **JADE** (Java Agent Development Framework) and **FIPA** communication protocols.

## 🚀 Key Features & Architecture
- **Autonomous Vehicles (`VehicleAgent` & `EmergencyVehicleAgent`):** Navigate the city grid independently using the **A* (A-Star) search algorithm** with Manhattan distance heuristics, dynamically recalculating routes in real time when roadblocks occur.
- **Smart Traffic Lights & Priority Protocol (`TrafficLightAgent` & `CentralAgent`):** Coordinate intersection phases safely and process real-time **FIPA-Request** messages (`CAN_I_PASS` for regular vehicles and `EMERGENCY_PASS` for immediate priority clearance for ambulances, police, and fire trucks).
- **Online Machine Learning (`DisruptionAgent`):** Predicts and simulates traffic anomalies using **Logistic Regression** (Sigmoid activation) and **Online Learning via Stochastic Gradient Descent (SGD)** based on active roadworks and queue lengths.
- **Interactive 2D GUI (`EnvironmentGUI` & `ControlPanelGUI`):** Built with **Java Swing / AWT (`Graphics2D`)**, featuring dynamic screen scaling, real-time vehicle rendering, and a live command center to force red lights or place road barriers.
- **Automated Metrics (`Metrics`):** Tracks travel costs, waiting times, and arrival statistics, exporting a complete simulation report (`dados_do_sistema.txt`) on shutdown.

## 🛠️ How to Run
Make sure you have **Java (JDK)** installed. On Windows, simply run the automated build and execution script:

```cmd
.\run.bat
```
