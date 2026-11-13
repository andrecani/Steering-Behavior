#!/usr/bin/env bash
# Compila e executa (requer JDK 21+)
set -e
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out cars.engine.Window
