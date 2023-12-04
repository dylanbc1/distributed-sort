![ICESI University Logo](https://www.icesi.edu.co/launiversidad/images/La_universidad/logo_icesi.png)

## Distributed Sorting. Software Architecture

### **Authors** ✒️

- Dylan Bermúdez Cardona
- Santiago Escobar León
- Daniel Montezuma Sevillano
- Diego Fernando Mueses Zúñiga

### **Project Objectives**

OT1. Explain the fundamental elements involved in the design of the software architecture of a computing system, including complementary aspects of the hardware architecture (processing and storage) and interconnection, and how these affect the achievement of quality attributes in the system; correctly use the standardized UML notation to specify these design elements.

This approach must recognize the responsibility assumed when designing the system architecture, in the scope of the proposed solution and the tradeoffs involved, considering professional ethics.

OT2. Derive a first version of software architecture from the analysis of given requirements, specifying it in a standardized UML notation. This analysis involves identifying architecturally significant requirements (ASR) established by stakeholders, and selecting architectural drivers based on the prioritization of said RAS.

In the process of this derivation, both the commitments (tradeoffs) and responsibilities that a software architect assumes with stakeholders and with society in general must be recognized, when prioritizing a subset of the RAS considered; also recognize the scope and impact that these have on the overall design of the system and its quality attributes.

OT3. Identify and apply architectural patterns and styles according to a prioritized set of architecturally significant requirements (RAS), mappable to quality attributes, to refine a reference architecture and obtain a specific software architecture, for systems of low or medium complexity.

Recognize that the quality of the software largely depends on the good treatment of these RAS and the relationships (interactions) between the components (re)used in the software, since desirable properties emerge from all of this (e.g., good quality attributes).

OT4. Define the global structure of the code based on the architectural design for its implementation, and carry out its deployment.

### **Project Description: Distributed Sorting System**

This project aims to design and implement a software system to address sorting challenges in large datasets through distributed sorting. The system includes a client that requests the data file name and invokes the distributed component (`dist_sorter`). The latter implements a distributed sorting strategy, combining various design patterns, with precise measurements of processing time and data transfer between nodes. Experiments are conducted by varying the number of machines and sorters, assessing performance in distributed configurations. The goal is to determine when distributing the process becomes advantageous and to evaluate the system's capacity. The results are presented in a ZIP file containing a deployment diagram, source code, compilation instructions, and a report featuring a table and comparative analysis of execution times versus data sizes, highlighting the improvements achieved in distributed performance compared to the monolithic execution.

### **Configuration** ⚙️

For this demonstration, we will use a project with two subprojects to build the Master and Worker applications.

Make sure to have the `distributed_sorting.zip` file on your computer.

To connect remotely to the computer, execute the following SSH command:

```bash
ssh swarch@xhgrid# (password: swarch)
```

***Where '#' is the number of the computer you want to connect to.***

Run the `ifconfig` command on each xhgrid# and take note (or remember) of the IPV4 address of each computer. This information will be useful later.

For this example, we will connect to two computers to deploy a Master and a Worker:

**Master:**
```bash
ssh swarch@xhgrid7 (password: swarch)
```

**Worker:**
```bash
ssh swarch@xhgrid8 (password: swarch)
```

**1. Master**

Open the console on xhgrid7 and create a new folder (with a name of your choice) in that path.

```bash
mkdir new_folder
```

On computer xhgrid7, go to `/path/to/created/folder/`

Run the `pwd` command on xhgrid7 and copy the path.

Transfer the `distributed_sorting` file in .zip format located on your computer. To do this, execute the following command:

```bash
scp master.zip swarch@xhgrid7:/path/to/created/folder/
```

Unzip `distributed_sorting.zip` by running the following command:

```bash
unzip distributed_sorting.zip
```

On xhgrid7, go to the path `/path/distributed_sorting/`. Then, execute the following command:

```bash
gradle build
```

Go to `/path/distributed_sorting/master/build/libs/`

Now, you need to create a folder called `doc` to store the file that will be sorted. You can do this using the following command:

```bash
mkdir doc
```

Next, enter the `doc` folder:

```bash
cd doc
```

Execute the `pwd` command and copy the path generated by that command:

```bash
pwd
```

Afterwards, you need to navigate back through the folders of `distributed_sorting` until you reach `path/distributed_sorting/doc/`. You can do this with the following command:

```bash
cd ..
```

***Note:*** Execute this command until you reach the `doc` folder.

Subsequently, run the following command to list the available files in the `doc` folder:

```bash
ls
```

With the name of your file (including the .txt extension), execute the following command, pasting the path you had previously copied from `/path/distributed_sorting/master/build/libs/doc/`:

```bash
mv file.txt /path/distributed_sorting/master/build/libs/doc/
```

Execute the following command:

```bash
mc
```

Use the keyboard arrows to select the file named `master.jar` and press the `Enter` key.

Use the keyboard arrows to select the file named `master.cfg` and press the `F4` key.

Press the `i` key.

Change the following line of code:

```bash
Master.Endpoints = default -h localhost -p 10000
```

to

```bash
Master.Endpoints = default -h IPV4_xhgrid7 -p 10000
```

Where `IPV4_xhgrid7` indicates the previously obtained IPV4 from computer xhgrid7.

Press the `Esc` key and then type `:x` and press `Enter`.

Go to `/..` with the keyboard arrows. Press `Enter`.

Then, run the following command:

```bash
java -jar master.jar
```

**2. Worker**

Open the console on xhgrid8 and create a new folder (with a name of your choice) in that path.

```bash
mkdir new_folder
```

On computer xhgrid8, go to /path/to/created/folder/

Run the `pwd` command on xhgrid8 and copy the path.

Transfer the `distributed_sorting` file in .zip format located on your computer. To do this, execute the following command:

```bash
scp distributed_sorting.zip swarch@xhgrid8:/path/to/created/folder/
```

Unzip `distributed_sorting.zip` by running the following command:

```bash
unzip distributed_sorting.zip
```

On xhgrid8, go to the path `/path/distributed_sorting/`. Then, execute the following command:

```bash
gradle build
```

Go to `/path/worker/build/libs/`

Execute the following command:

```bash
mc
```

Use the keyboard arrows to select the file named `worker.jar` and press the `Enter` key.

Use the keyboard arrows to select the file named `worker.cfg` and press the `F4` key.

Press the `i` key.

Change the following lines of code:

```bash
Worker.Endpoints = default -h localhost -p 10001
MasterInterface.Proxy = Master:default -h localhost -p 10000
```

to

```bash
Worker.Endpoints = default -h IPV4_xhgrid8 -p 10001
MasterInterface.Proxy = Master:default -h IPV4_xhgrid7 -p 10000
```

Where `IPV4_xhgrid8` indicates the previously obtained IPV4 from computer xhgrid8. And `IPV4_xhgrid7` indicates the previously obtained IPV4 from computer xhgrid7.

Press the `Esc` key and then type `:x` and press `Enter`.

Go to `/..` with the keyboard arrows. Press `Enter`.

Then, run the following command:

```bash
java -jar worker.jar
```

***Note:*** The process of deploying **workers** is executed as many times as necessary.

### **Build With** 🛠️

<div style="text-align: left">
    <p>
        <a href="https://www.jetbrains.com/idea/" target="_blank"> <img alt="IntelliJ IDEA" src="https://cdn.svgporn.com/logos/intellij-idea.svg" height="60" width = "60"></a>
        <a href="https://www.java.com/" target="_blank"> <img alt="Java" src="https://cdn.svgporn.com/logos/java.svg" height="60" width = "60"></a>
    </p>
</div>

This project requires the following versions:

- **JDK:** 11
- **ZeroC ICE:** 3.7.6
- **Gradle:** 1.4.5
