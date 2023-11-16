@echo off
del bin/*
del *.txt
javac -cp lib/jade.jar -d bin -sourcepath src src/*.java
@REM Esta es la linea a editar para cambiar los agentes que se ejecutaran.
java -cp lib/jade.jar;bin jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent1:RandomAgent;RandomAgent2:RandomAgent;D_Agent:Deterministic_D_Agent;H_Agent:Deterministic_H_Agent"
del bin/*.txt
del *.txt