#!/bin/bash
cd "$(dirname "$0")"

if [ $# -lt 3 ]; then
    echo "usage: ./run.sh Classe n topologie [-log debug]"
    exit 1
fi

javac -cp processlibrary.jar ensisa/$1.java || exit 1
timeout 60 java -jar simulator.jar -n $2 $3 ${@:4} -classpath ".:processlibrary.jar" ensisa.$1
