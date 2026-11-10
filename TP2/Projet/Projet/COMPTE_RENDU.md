# Examen – Écho de Chang et Segall : température moyenne

## Fichiers

- `ensisa/EchoChangSegall.java` : le livrable, un seul fichier qui enchaîne les deux parties (calcul de la moyenne, puis diffusion)
- `run.sh` : compile et lance une simulation, ex. `./run.sh EchoChangSegall 8 -random`
- `test.sh` : lance toutes les topologies et vérifie les sorties, ex. `./test.sh`

Les scripts ne sont pas nécessaires. Depuis ce dossier, les commandes de l'énoncé suffisent :

```
javac -cp processlibrary.jar ensisa/EchoChangSegall.java
java -jar simulator.jar -n 4 -random -classpath ".:processlibrary.jar" ensisa.EchoChangSegall
```

## Première partie

Le processus 0 joue le rôle de la station de base. C'est lui qui lance la vague. Chaque processus, station comprise, tire au démarrage une température entière entre 10 et 30 et l'affiche. La station est donc comptée comme un capteur : la moyenne porte sur les n processus.

L'algorithme utilise deux types de messages. `WAVE` est la vague : il ne porte rien. `ECHO` est le message qu'un capteur envoie à son père à la fin ; il porte le couple (somme des températures, nombre de capteurs) de son sous-arbre.

L'initiateur envoie `WAVE` à tous ses voisins. Un autre processus prend pour père l'émetteur du premier message reçu et envoie `WAVE` à tous ses voisins sauf son père. Chacun attend ensuite un message de chaque voisin. Seuls les `ECHO` sont ajoutés à la somme et au compteur, qui partent de la température du processus et de 1. Les `WAVE` reçus sur une arête transversale comptent seulement pour atteindre le nombre de voisins. Quand tous ses voisins lui ont écrit, un capteur envoie `ECHO(somme, nombre)` à son père. L'initiateur, lui, divise la somme par le nombre : c'est l'événement de décision.

```
temperature = hasard(10..30); somme = temperature; nombre = 1
si id != 0 : pere = émetteur du premier message ; recus = 1
envoyer WAVE à tous les voisins sauf pere
tant que recus < nombre de voisins :
    m = recevoir()
    si m est un ECHO : somme += m.somme ; nombre += m.nombre
    recus++
si id == 0 : afficher somme / nombre
sinon      : envoyer ECHO(somme, nombre) à pere
```

### Pourquoi ça marche

Sur chaque arête, chaque extrémité envoie exactement un message. Sur une arête de l'arbre (père, fils), le père envoie `WAVE` et le fils renvoie `ECHO`. Sur une arête transversale, les deux extrémités s'envoient un `WAVE`. Un processus reçoit donc un message par voisin, et il en reçoit un `ECHO` exactement de chacun de ses fils. Il y a 2|E| messages en tout, par exemple n(n-1) sur le graphe complet.

Par récurrence depuis les feuilles, l'`ECHO` d'un capteur contient la somme et le nombre de capteurs de son sous-arbre. Les sous-arbres des fils de l'initiateur forment une partition des autres processus. L'initiateur obtient donc la somme de toutes les températures et le nombre n.

Quand un processus s'arrête, il a reçu un message de chacun de ses voisins. Personne ne lui écrira plus.

## Seconde partie

Une fois la moyenne calculée, l'initiateur relance une seconde vague de Chang et Segall avec un troisième type de message, `AVERAGE`, qui porte la moyenne (un `double`, écrit avec `stream().writeDouble`). Un capteur qui a envoyé son `ECHO` attend cette seconde vague. Il prend pour nouveau père l'émetteur du premier `AVERAGE` reçu, affiche sa température et la moyenne reçue, puis fait comme dans la première vague : il envoie `AVERAGE` à ses autres voisins, attend un message de chacun, et renvoie `AVERAGE` à son père. L'initiateur affiche la fin de la diffusion quand tous ses voisins lui ont répondu.

Les deux vagues ne se mélangent pas. L'initiateur ne décide que lorsque tous les processus ont envoyé leur `ECHO`, donc lorsque chacun a reçu tous les messages de la première vague. Tout message reçu après l'`ECHO` est donc un `AVERAGE`. La seconde vague coûte à nouveau 2|E| messages, et l'arbre peut être différent du premier.

## Exemple de sortie

`java -jar simulator.jar -n 4 -random -classpath ".:processlibrary.jar" ensisa.EchoChangSegall`
```
Processus 0 : température 16
Processus 1 : température 14
Processus 2 : température 12
Processus 3 : température 22
Processus 0 : température moyenne 16.0 (64 / 4)
Processus 1 : température 14, moyenne reçue 16.0
Processus 3 : température 22, moyenne reçue 16.0
Processus 2 : température 12, moyenne reçue 16.0
Processus 0 : diffusion de la moyenne terminée
```

Les cinq premières lignes correspondent à la première partie : 16 + 14 + 12 + 22 = 64, et 64 / 4 = 16. Les quatre suivantes correspondent à la seconde. L'ordre des lignes varie d'une exécution à l'autre.

## Tests

`test.sh` relit les températures affichées et recalcule la moyenne attendue. Pour chaque exécution, il vérifie :
- que chaque processus affiche une température et une seule ;
- que l'initiateur affiche une seule moyenne, avec la bonne somme, le bon nombre n et le bon quotient ;
- que chaque capteur 1 à n-1 affiche une seule fois sa propre température et la moyenne calculée par l'initiateur, et que l'initiateur affiche la fin de la diffusion ;
- qu'il n'y a ni exception, ni erreur du simulateur, ni timeout.

Les températures étant tirées au hasard, chaque cas est répété : 3 fois pour `-bidirectional` et `-mesh`, 5 fois pour `-random`, dont le graphe change à chaque exécution. Les tailles sont n = 2, 3, 4, 8, 15 et 20.

Résultat : `./test.sh` affiche `66 OK, 0 ECHEC`.

## Limites

L'écho a besoin de canaux dans les deux sens, puisqu'un capteur répond à son père. Il ne s'applique pas à `-unidirectional` dès que n ≥ 3. Avec n = 3, le processus 0 n'a que 1 comme successeur et 2 comme prédécesseur :

`java -jar simulator.jar -n 3 -unidirectional -classpath ".:processlibrary.jar" ensisa.EchoChangSegall`
```
Processus 0 : température 23
Processus 1 : température 10
Processus 2 : température 10
Exception in thread "Thread-1" java.lang.RuntimeException: No communication channel between process 1 and process 0
Exception in thread "Thread-2" java.lang.RuntimeException: No communication channel between process 2 and process 1
Processus 0 : température moyenne 23.0 (23 / 1)
Processus 1 : température 10, moyenne reçue 23.0
```

Le processus 0 attend un seul message, celui de 2. Il reçoit la vague que 2 lui transmet, la prend pour un jeton de non-fils et conclut avec sa seule température : la moyenne est fausse. Les `ECHO` de 1 et de 2 sont refusés par le simulateur, qui ne relaie plus rien de ces processus. La seconde vague ne peut donc pas se terminer, et la simulation s'arrête sur le timeout de 60 s.
