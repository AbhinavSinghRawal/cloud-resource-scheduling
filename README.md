# Cloud Resource Allocation and VM Scheduling Using CloudSim Plus

## What this project does
This program creates a small simulated cloud datacenter and compares two VM cloudlet scheduling policies: time-shared and space-shared. Both runs use the same hosts, VMs, and tasks so their outputs can be compared fairly.

## Requirements
- Java JDK 17 or newer
- Apache Maven 3.8 or newer
- Internet connection for Maven to download the CloudSim Plus dependency

## Run the experiments
Open this folder in VS Code or IntelliJ IDEA. In a terminal opened at this folder, run each command separately:

```bash
mvn clean compile exec:java -Dexec.args="timeshared"
mvn clean compile exec:java -Dexec.args="spaceshared"
```

The program prints task-level information and writes `results-timeshared.csv` or `results-spaceshared.csv` in the project folder. If a command fails, keep the full error message; it is useful for troubleshooting.

## Configuration
The current setup has two hosts, four VMs, and twelve cloudlets. Task lengths vary from 20,000 to 35,000 million instructions. The source file contains the configuration constants near the beginning of the class.

## Interpreting the output
Compare how many tasks completed, their finish times, and the overall makespan. Do not assume one policy is always better: results depend on the task sizes and the available processing capacity. This is a simulation, not a measurement from a commercial cloud provider.

## Main source file
`src/main/java/com/example/cloudsim/CloudResourceSchedulingSimulation.java`
