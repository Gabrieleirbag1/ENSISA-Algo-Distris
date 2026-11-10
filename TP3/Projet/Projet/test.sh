#!/bin/bash
cd "$(dirname "$0")"

class=${1:-EchoChangSegallGabrielGarrone}
topologies="-bidirectional -mesh -random"
sizes="2 3 4 8 15 20"
runs=5

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

    read min max <<< "$(awk '$4 == "température" && NF == 5 {
        t = $5 + 0
        if (seen == 0 || t < min) min = t
        if (seen == 0 || t > max) max = t
        seen = 1
    } END { print min, max }' <<< "$out")"

    if [ $(grep -c "température minimale" <<< "$out") -ne 1 ] ||
        ! grep -qxF "Processus 0 : température minimale $min, maximale $max ($n capteurs)" <<< "$out"; then
        echo -n " decision(attendu $min $max $n)"
        return
    fi

    if [ $(grep -c "minimale reçue" <<< "$out") -ne $((n - 1)) ]; then
        echo -n " receptions"
    fi
    for i in $(seq 1 $((n - 1))); do
        t=$(temperature "$out" $i)
        c=$(grep -cxF "Processus $i : température $t, minimale reçue $min, maximale reçue $max" <<< "$out")
        if [ $c -ne 1 ]; then
            echo -n " p$i:${c}x"
        fi
    done
    if ! grep -qx "Processus 0 : diffusion terminée" <<< "$out"; then
        echo -n " pas_de_fin"
    fi
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
