# Examen – Écho de Chang et Segall : températures minimale et maximale

## Fichiers

- `ensisa/EchoChangSegall.java` : le livrable, un seul fichier qui enchaîne les deux parties (calcul du minimum et du maximum, puis diffusion)
- `run.sh` : compile et lance une simulation, ex. `./run.sh EchoChangSegall 8 -random`
- `test.sh` : lance toutes les topologies et vérifie les sorties, ex. `./test.sh`

Les scripts ne sont pas nécessaires. Depuis ce dossier, les commandes de l'énoncé suffisent :

```
javac -cp processlibrary.jar ensisa/EchoChangSegall.java
java -jar simulator.jar -n 4 -random -classpath ".:processlibrary.jar" ensisa.EchoChangSegall
```

## Première partie

Le processus 0 joue le rôle de la station de base. C'est lui qui lance la vague. Chaque processus, station comprise, tire au démarrage une température entière entre 0 et 40 et l'affiche. La station est donc comptée comme un capteur.

L'algorithme utilise deux types de messages. `WAVE` est la vague : il ne porte rien. `ECHO` est le message qu'un capteur envoie à son père à la fin ; il porte le triplet (température minimale, température maximale, nombre de capteurs) de son sous-arbre.

L'initiateur envoie `WAVE` à tous ses voisins. Un autre processus prend pour père l'émetteur du premier message reçu et envoie `WAVE` à tous ses voisins sauf son père. Chacun attend ensuite un message de chaque voisin. Le minimum et le maximum partent de la température du processus, le nombre de capteurs part de 1. Seuls les `ECHO` les mettent à jour. Les `WAVE` reçus sur une arête transversale comptent seulement pour atteindre le nombre de voisins. Quand tous ses voisins lui ont écrit, un capteur envoie `ECHO(min, max, nombre)` à son père. L'initiateur, lui, affiche le minimum et le maximum : c'est l'événement de décision.

```
temperature = hasard(0..40); min = max = temperature; nombre = 1
si id != 0 : pere = émetteur du premier message ; recus = 1
envoyer WAVE à tous les voisins sauf pere
tant que recus < nombre de voisins :
    m = recevoir()
    si m est un ECHO :
        min = min(min, m.min) ; max = max(max, m.max) ; nombre += m.nombre
    recus++
si id == 0 : afficher min, max et nombre
sinon      : envoyer ECHO(min, max, nombre) à pere
```

### Pourquoi ça marche

Sur chaque arête, chaque extrémité envoie exactement un message. Sur une arête de l'arbre (père, fils), le père envoie `WAVE` et le fils renvoie `ECHO`. Sur une arête transversale, les deux extrémités s'envoient un `WAVE`. Un processus reçoit donc un message par voisin, et il en reçoit un `ECHO` exactement de chacun de ses fils. Il y a 2|E| messages en tout, par exemple n(n-1) sur le graphe complet.

Par récurrence depuis les feuilles, l'`ECHO` d'un capteur contient le minimum, le maximum et le nombre de capteurs de son sous-arbre. Les sous-arbres des fils de l'initiateur forment une partition des autres processus. L'initiateur obtient donc le minimum et le maximum de toutes les températures. Il obtient aussi le nombre n, qu'il affiche pour montrer que tous les capteurs ont été pris en compte.

Quand un processus a fini la vague, il a reçu un message de chacun de ses voisins. Plus personne ne lui écrira dans cette vague.

## Seconde partie

Une fois le minimum et le maximum connus, l'initiateur relance une seconde vague de Chang et Segall avec un troisième type de message, `RESULT`, qui porte les deux valeurs. Un capteur qui a envoyé son `ECHO` attend cette seconde vague. Il prend pour nouveau père l'émetteur du premier `RESULT` reçu et affiche sa température, le minimum et le maximum reçus. Il fait ensuite comme dans la première vague : il envoie `RESULT` à ses autres voisins, attend un message de chacun, et renvoie `RESULT` à son père. L'initiateur affiche la fin de la diffusion quand tous ses voisins lui ont répondu.

Les deux vagues ne se mélangent pas. L'initiateur ne décide que lorsque tous les processus ont envoyé leur `ECHO`, donc lorsque chacun a reçu tous les messages de la première vague. Tout message reçu après l'`ECHO` est donc un `RESULT`. La seconde vague coûte à nouveau 2|E| messages, et l'arbre peut être différent du premier.

## Exemple de sortie

`java -jar simulator.jar -n 4 -random -classpath ".:processlibrary.jar" ensisa.EchoChangSegall`
```
Processus 0 : température 35
Processus 1 : température 30
Processus 2 : température 25
Processus 3 : température 31
Processus 0 : température minimale 25, maximale 35 (4 capteurs)
Processus 2 : température 25, minimale reçue 25, maximale reçue 35
Processus 1 : température 30, minimale reçue 25, maximale reçue 35
Processus 3 : température 31, minimale reçue 25, maximale reçue 35
Processus 0 : diffusion terminée
```

Les cinq premières lignes correspondent à la première partie : parmi 35, 30, 25 et 31, le minimum est 25 et le maximum 35. Les quatre suivantes correspondent à la seconde. L'ordre des lignes varie d'une exécution à l'autre.

## Tests

`test.sh` relit les températures affichées et recalcule le minimum et le maximum attendus. Pour chaque exécution, il vérifie :
- que chaque processus affiche une température et une seule ;
- que l'initiateur affiche une seule décision, avec le bon minimum, le bon maximum et n capteurs ;
- que chaque capteur 1 à n-1 affiche une seule fois sa propre température avec le minimum et le maximum de l'initiateur, et que l'initiateur affiche la fin de la diffusion ;
- qu'il n'y a ni exception, ni erreur du simulateur, ni timeout.

Les températures étant tirées au hasard, chaque cas est répété 5 fois, sur les trois topologies. En `-random`, le graphe change en plus à chaque exécution. Les tailles sont n = 2, 3, 4, 8, 15 et 20.

Résultat : `./test.sh` affiche `90 OK, 0 ECHEC`.

## Limites

L'écho a besoin de canaux dans les deux sens, puisqu'un capteur répond à son père. Il ne s'applique pas à `-unidirectional` dès que n ≥ 3. Avec n = 3, le processus 0 n'a que 1 comme successeur et 2 comme prédécesseur :

`timeout 60 java -jar simulator.jar -n 3 -unidirectional -classpath ".:processlibrary.jar" ensisa.EchoChangSegall`
```
Processus 0 : température 3
Processus 1 : température 19
Processus 2 : température 32
Exception in thread "Thread-1" java.lang.RuntimeException: No communication channel between process 1 and process 0
Exception in thread "Thread-2" java.lang.RuntimeException: No communication channel between process 2 and process 1
Processus 0 : température minimale 3, maximale 3 (1 capteurs)
Processus 1 : température 19, minimale reçue 3, maximale reçue 3
```

Le processus 0 attend un seul message, celui de 2. Il reçoit la vague que 2 lui transmet, la prend pour un jeton de non-fils et conclut avec sa seule température : le résultat est faux, et le compte d'un seul capteur le montre. Les `ECHO` de 1 et de 2 sont refusés par le simulateur, qui ne relaie plus rien de ces processus. La seconde vague ne peut donc pas se terminer, et la simulation s'arrête sur le timeout de 60 s.
