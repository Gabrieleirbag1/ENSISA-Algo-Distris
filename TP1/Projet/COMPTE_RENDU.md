# TP1 – Algorithme de l'écho de Chang et Segall

## Fichiers

- `ensisa/Echo.java` : diffusion d'un texte (question 8)
- `ensisa/EchoCollect.java` : diffusion + remontée des identifiants vers l'initiateur (question 10)
- `run.sh` : compile et lance une simulation, ex. `./run.sh Echo 8 -random`
- `test.sh` : lance toutes les topologies pour n = 2, 3, 4, 8, 15, 20 et vérifie les sorties, ex. `./test.sh EchoCollect`

## Principe

Le processus 0 est l'initiateur. Il envoie le message à tous ses voisins puis attend un message de chacun d'eux.

Un autre processus attend son premier message. L'émetteur de ce message devient son père, et il affiche le texte à ce moment-là. Il transmet ensuite le message à tous ses voisins sauf son père, puis attend d'avoir reçu un message de chacun de ses voisins. Il renvoie alors l'écho à son père et s'arrête.

On n'utilise qu'un seul type de message : un message reçu d'un voisin qui n'est pas le père sert d'écho, puisque ce voisin a déjà été atteint par un autre chemin.

```
initiateur :
    envoyer M à tous les voisins
    recevoir deg messages
    fin

suiveur :
    recevoir M de p ; pere = p ; afficher texte
    envoyer M à tous les voisins sauf pere
    recevoir deg - 1 messages
    envoyer M à pere
    fin
```

## Pourquoi ça marche

- **Affichage unique.** Seul le premier `receive()` d'un suiveur affiche le texte. Les messages suivants sont seulement comptés.
- **Un message par voisin.** Sur chaque arête, chaque extrémité envoie exactement un message à l'autre : la propagation, ou l'écho si le voisin est le père. Un processus de degré d reçoit donc exactement d messages, et l'attente se termine toujours. Au total, l'algorithme envoie **2|E| messages**.
- **Terminaison.** Les pères forment un arbre couvrant enraciné en 0. Une feuille de cet arbre renvoie son écho dès qu'elle a reçu les messages de ses voisins. Les échos remontent ainsi jusqu'à la racine. Quand 0 a reçu un message de tous ses voisins, tous les processus ont reçu le texte.
- **Arrêt sans perte.** Un processus s'arrête juste après avoir envoyé son écho. Il a déjà reçu un message de chaque voisin, donc plus personne ne peut lui écrire.

## Exemples de sortie

`./run.sh Echo 5 -bidirectional`
```
Processus 1 a reçu "Bonjour à tous" de 0
Processus 4 a reçu "Bonjour à tous" de 0
Processus 2 a reçu "Bonjour à tous" de 1
Processus 3 a reçu "Bonjour à tous" de 4
Processus 0 : diffusion terminée
```

`./run.sh Echo 5 -mesh`
```
Processus 2 a reçu "Bonjour à tous" de 0
Processus 4 a reçu "Bonjour à tous" de 0
Processus 1 a reçu "Bonjour à tous" de 0
Processus 3 a reçu "Bonjour à tous" de 0
Processus 0 : diffusion terminée
```

`./run.sh Echo 6 -random`
```
Processus 4 a reçu "Bonjour à tous" de 0
Processus 5 a reçu "Bonjour à tous" de 0
Processus 2 a reçu "Bonjour à tous" de 4
Processus 1 a reçu "Bonjour à tous" de 4
Processus 3 a reçu "Bonjour à tous" de 2
Processus 0 : diffusion terminée
```

L'ordre des lignes change d'une exécution à l'autre, et l'arbre obtenu aussi en `-mesh` et `-random`.

## Question 10 : remontée des identifiants

Le message contient le texte, puis le nombre d'identifiants, puis les identifiants eux-mêmes.

Chaque suiveur garde un ensemble `known`. Il y ajoute son propre numéro et tous les identifiants trouvés dans les messages qu'il reçoit, y compris ceux qui ne viennent pas de son père. Il met cet ensemble dans chaque message qu'il envoie, et notamment dans l'écho vers son père.

L'initiateur affiche l'ensemble reçu de chaque voisin, puis leur union. Les ensembles reçus se recouvrent, parce qu'un voisin non-père transmet aussi ce qu'il connaît déjà. L'union contient toujours tous les processus.

`./run.sh EchoCollect 8 -random`
```
Processus 4 a reçu "Bonjour à tous" de 0
Processus 2 a reçu "Bonjour à tous" de 0
Processus 6 a reçu "Bonjour à tous" de 0
Processus 1 a reçu "Bonjour à tous" de 0
Processus 5 a reçu "Bonjour à tous" de 0
Processus 3 a reçu "Bonjour à tous" de 2
Processus 7 a reçu "Bonjour à tous" de 4
Processus 0 a reçu de 1 : [1, 2, 5, 6]
Processus 0 a reçu de 4 : [2, 3, 4, 5, 6, 7]
Processus 0 a reçu de 6 : [1, 2, 3, 4, 5, 6, 7]
Processus 0 a reçu de 5 : [1, 4, 5, 6, 7]
Processus 0 a reçu de 2 : [1, 2, 3, 4, 6, 7]
Processus 0 : diffusion terminée
Processus 0 connaît : [1, 2, 3, 4, 5, 6, 7]
```

## Tests

`test.sh` vérifie trois choses pour chaque exécution :
- chaque processus 1..n-1 affiche le texte une seule fois ;
- l'initiateur termine ;
- la simulation ne dépasse pas le timeout de 20 s.

Pour `EchoCollect`, il vérifie aussi que l'union vaut {1, …, n-1}.

Topologies `-bidirectional`, `-mesh` et `-random` (5 exécutions pour `-random`), avec n = 2, 3, 4, 8, 15, 20 : 42/42 OK pour `Echo` et 42/42 OK pour `EchoCollect`.

Le simulateur refuse n = 1 (`Incorrect number of processes`).

## Limite : anneau unidirectionnel

L'algorithme a besoin de canaux dans les deux sens, car l'écho repart vers le père. Sur `-unidirectional`, un processus ne peut écrire qu'à son successeur. L'envoi de l'écho au père provoque donc l'erreur `No communication channel between process 1 and process 0`, et la simulation reste bloquée.
