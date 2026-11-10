#!/bin/bash
cd "$(dirname "$0")"

class=${1:-Echo}
topologies="-bidirectional -mesh -random"
sizes="2 3 4 8 15 20"
random_runs=5

check() {
    local out=$1
    local n=$2

    case $class in
    Echo | EchoCollect)
        if ! grep -q "Processus 0 : diffusion terminée" <<< "$out"; then
            echo -n " pas_de_fin"
        fi
        for i in $(seq 1 $((n - 1))); do
            c=$(grep -c "^Processus $i a reçu \"" <<< "$out")
            if [ $c -ne 1 ]; then
                echo -n " p$i:${c}x"
            fi
        done
        ;;
    *)
        echo -n " pas_de_check"
        ;;
    esac

    case $class in
    EchoCollect)
        expected="[$(seq -s ', ' 1 $((n - 1)))]"
        if ! grep -qF "Processus 0 connaît : $expected" <<< "$out"; then
            echo -n " union"
        fi
        ;;
    esac
}

javac -Xlint -Werror -cp processlibrary.jar ensisa/$class.java || exit 1

ok=0
ko=0

for topo in $topologies; do
    runs=1
    if [ $topo = -random ]; then
        runs=$random_runs
    fi

    for n in $sizes; do
        for run in $(seq $runs); do
            out=$(timeout 60 java -jar simulator.jar -n $n $topo -classpath ".:processlibrary.jar" ensisa.$class 2>&1)
            code=$?
            errors=$(check "$out" $n)

            if [ $code -eq 124 ]; then
                errors="$errors timeout"
            fi
            if grep -qE "Exception|\[Error\]|No communication channel" <<< "$out"; then
                errors="$errors erreur"
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
