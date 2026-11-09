#!/bin/bash
cd "$(dirname "$0")"

if [ $# -ne 3 ]; then
    echo "usage: ./run.sh Classe n -bidirectional|-mesh|-random"
    exit 1
fi

javac -cp processlibrary.jar ensisa/$1.java || exit 1
timeout 20 java -jar simulator.jar -n $2 $3 -classpath "processlibrary.jar:." ensisa.$1
