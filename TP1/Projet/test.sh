#!/bin/bash
cd "$(dirname "$0")"

class=${1:-Echo}
javac -cp processlibrary.jar ensisa/$class.java || exit 1

ok=0
ko=0

for topo in -bidirectional -mesh -random; do
    runs=1
    if [ $topo = -random ]; then
        runs=5
    fi

    for n in 2 3 4 8 15 20; do
        for run in $(seq $runs); do
            out=$(timeout 20 java -jar simulator.jar -n $n $topo -classpath "processlibrary.jar:." ensisa.$class 2>&1)
            code=$?
            errors=""

            if [ $code -eq 124 ]; then
                errors="$errors timeout"
            fi
            if ! grep -q "Processus 0 : diffusion terminée" <<< "$out"; then
                errors="$errors pas_de_fin"
            fi
            for i in $(seq 1 $((n - 1))); do
                c=$(grep -c "^Processus $i a reçu \"" <<< "$out")
                if [ $c -ne 1 ]; then
                    errors="$errors p$i:${c}x"
                fi
            done
            if [ $class = EchoCollect ]; then
                expected="[$(seq -s ', ' 1 $((n - 1)))]"
                if ! grep -qF "Processus 0 connaît : $expected" <<< "$out"; then
                    errors="$errors union"
                fi
            fi

            if [ -z "$errors" ]; then
                echo "OK     $topo n=$n"
                ok=$((ok + 1))
            else
                echo "ECHEC  $topo n=$n :$errors"
                ko=$((ko + 1))
            fi
        done
    done
done

echo "$ok OK, $ko ECHEC"
[ $ko -eq 0 ]
