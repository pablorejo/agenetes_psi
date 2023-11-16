#!/bin/bash

# Eliminar archivos en el directorio bin
rm -f bin/*
rm -f *.txt

# Compilar los archivos fuente en el directorio src y colocar los archivos de clase en el directorio bin
javac -cp lib/jade.jar -d bin -sourcepath src src/*.java

# Ejecutar el programa Jade
# Esta es la linea a editar para cambiar los agentes que se ejecutaran.
java -cp lib/jade.jar:bin jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent1:RandomAgent;RandomAgent2:RandomAgent;D_Agent:Deterministic_D_Agent;H_Agent:Deterministic_H_Agent"

# Eliminar archivos de texto en el directorio bin
rm -f bin/*.txt
rm -f *.txt
