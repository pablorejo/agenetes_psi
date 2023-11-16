# Como ejecutar
This is a set of basic commands to compile and execute the code:

- To compile: 
    ```bash
    javac -cp lib/jade.jar -d bin -sourcepath src src/*.java
    ```
- To execute: 
    ```bash
    java -cp lib/jade.jar:bin jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent1:RandomAgent;RandomAgent2:RandomAgent"
    ```

# Ejecución rápida
## Pasos generales 
    Poner el fichero jade.jar a la carpeta lib.
## Linux & Mac:
Para hacerlo de manera sencilla está el programa ejecutar.sh  
Damos permisos para la ejecución:

```bash
chmod +x ejecutar.sh
```
Y ejecutamos el programa que compilara y lanzará la aplicación.
```bash
./ejecutar.sh
```
Podemos cambiar el fichero para que se ejecuten los agentes que deseemos.

## Windows (do not use the PowerShell):
Para hacerlo de manera sencilla está el programa ejecutar.bat  
Ejecutamos el programa que compilara y lanzará la aplicación.
```bash
./ejecutar.sh
```
Podemos cambiar el fichero para que se ejecuten los agentes que deseemos.

**Note** that you need to have the **"jade.jar"** library in the present folder