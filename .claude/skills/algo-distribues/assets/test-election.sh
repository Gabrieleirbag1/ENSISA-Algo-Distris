#!/bin/bash
cd "$(dirname "$0")"

class=${1:-ChangRoberts}
topologies="-unidirectional"
sizes="2 3 4 8 15 20"
runs=5

expected() {
    awk -v n=$2 '/^Processus [0-9]+ propose / { v[$2] = $4 + 0 }
    END {
        best = 0
        total = 0
        for (i = 0; i < n; i++) {
            if (v[i] > v[best]) best = i
            j = (i + 1) % n
            total++
            while (v[j] < v[i]) {
                j = (j + 1) % n
                total++
            }
        }
        print best, total
    }' <<< "$1"
}

check() {
    local out=$1
    local n=$2

    if [ $(grep -c "^Processus [0-9]* propose " <<< "$out") -ne $n ]; then
        echo -n " propositions"
        return
    fi
    read leader total <<< "$(expected "$out" $n)"

    case $class in
    ChangRoberts | ChangRobertsCount)
        for i in $(seq 0 $((n - 1))); do
            c=$(grep -c "^Processus $i : l'élu est $leader$" <<< "$out")
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
    ChangRobertsCount)
        if [ $(grep -c "messages de candidature" <<< "$out") -ne 1 ] ||
            ! grep -qx "Processus $leader : $total messages de candidature" <<< "$out"; then
            echo -n " total(attendu $total)"
        fi
        ;;
    esac
}

javac -Xlint -Werror -cp processlibrary.jar ensisa/$class.java || exit 1

ok=0
ko=0

for topo in $topologies; do
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
