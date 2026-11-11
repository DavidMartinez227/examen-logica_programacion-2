#!/bin/bash
# Compila y ejecuta (Linux/Mac). En Windows use compilar.bat
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -cp "lib/*" -d out src/ruta/*.java || exit 1
java -cp "out:lib/*" ruta.Main
