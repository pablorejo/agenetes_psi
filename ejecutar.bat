@echo off
del bin/*
del *.txt
javac -cp lib/jade.jar -d bin -sourcepath src src/*.java src/agents/*.java 
@REM Esta es la linea a editar para cambiar los agentes que se ejecutaran.
java -cp "lib/jade.jar;bin;bin\agents" jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent1:agents.RandomAgent;RL_agent:agents.RL_Agent;
del bin/*.txt
del *.txt