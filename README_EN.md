# General Steps
Place the jade.jar file in the lib folder.

# How to Execute
This is a set of basic commands to compile and execute the code:

- To compile:
    ```bash
    javac -cp lib/jade.jar -d bin -sourcepath src src/*.java
    ```
- To execute:
    ```bash
    java -cp lib/jade.jar:bin jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent1:RandomAgent;RandomAgent2:RandomAgent"
    ```

# Quick Execution

## Linux & Mac:
To do it easily, there is the ejecutar.sh program.
Give permission to execute:

```bash
chmod +x ejecutar.sh
```
And run the program, which will compile and launch the application.

```bash
./ejecutar.sh
```
You can change the file to execute the desired agents.

# Windows (do not use PowerShell):
To do it easily, there is the ejecutar.bat program.
Run the program, which will compile and launch the application.

```bash
./ejecutar.bat
```
You can change the file to execute the desired agents.