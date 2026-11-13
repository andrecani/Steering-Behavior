@echo off
rem Compila e executa (requer JDK 21+)
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
java -cp out cars.engine.Window
