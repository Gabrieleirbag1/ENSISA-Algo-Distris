#!/bin/bash
cd "$(dirname "$0")"

class=${1:-EchoChangSegall}
topologies="-bidirectional -mesh -random"
sizes="2 3 4 8 15 20"

temperature() {
    awk -v i=$2 '$2 == i && NF == 5 && $4 == "température" { print $5 }' <<< "$1"
}

check() {
    local out=$1
    local n=$2

    if [ $(grep -cE "^Processus [0-9]+ : température [0-9]+$" <<< "$out") -ne $n ]; then
        echo -n " temperatures"
        return
    fi
    for i in $(seq 0 $((n - 1))); do
        if [ -z "$(temperature "$out" $i)" ]; then
            echo -n " p$i:sans_temperature"
            return
        fi
    done

    sum=$(awk '$4 == "température" && NF == 5 { s += $5 } END { print s }' <<< "$out")
    average=$(awk -v s=$sum -v n=$n '/^Processus 0 : température moyenne / {
        d = $6 - s / n
        if (d < 1e-9 && d > -1e-9 && $7 == "(" s && $9 == n ")") print $6
    }' <<< "$out")
    if [ $(grep -c "température moyenne" <<< "$out") -ne 1 ] || [ -z "$average" ]; then
        echo -n " moyenne(attendu $sum / $n)"
        return
    fi

    if [ $(grep -c "moyenne reçue" <<< "$out") -ne $((n - 1)) ]; then
        echo -n " receptions"
    fi
    for i in $(seq 1 $((n - 1))); do
        t=$(temperature "$out" $i)
        c=$(grep -cxF "Processus $i : température $t, moyenne reçue $average" <<< "$out")
        if [ $c -ne 1 ]; then
            echo -n " p$i:${c}x"
        fi
    done
    if ! grep -qx "Processus 0 : diffusion de la moyenne terminée" <<< "$out"; then
        echo -n " pas_de_fin"
    fi
}

javac -Xlint -Werror -cp processlibrary.jar ensisa/$class.java || exit 1

ok=0
ko=0

for topo in $topologies; do
    runs=3
    if [ $topo = -random ]; then
        runs=5
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
