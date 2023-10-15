@echo off
javac -cp lib/jade.jar -d bin -sourcepath src src/*.java
java -cp lib/jade.jar;bin jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent:RandomAgent;"
del *.txt