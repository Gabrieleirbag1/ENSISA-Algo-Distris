---
name: algo-distribues
description: Réaliser de bout en bout un TP d'algorithmique distribuée de l'ENSISA (cours de P. Studer) en Java sur le simulateur du prof (simulator.jar, processlibrary.jar, classe ensisa.process.Process avec send/receive/receiveFrom/receiveOrTerminate) - code sans commentaires, scripts de test, compte-rendu. À utiliser dès que l'utilisateur veut faire, finir, tester ou rédiger un TP d'« algo distribués » / « algo distrib » (énoncé « TP n .pdf », dossier Projet/), même sans nommer le simulateur - écho, Tarry, vague, inondation, élection (LeLann, Chang-Roberts, Hirschberg-Sinclair), terminaison (Misra, Dijkstra-Scholten), exclusion mutuelle (Lamport, Ricart-Agrawala), snapshot Chandy-Lamport, arbre couvrant, sur anneau, mesh ou random. Aussi pour une simulation bloquée ou en timeout, une erreur VS Code sur ce projet, ou un prompt à écrire pour ces TP. Pas pour un autre langage ou outil (Go, MPI, Python, Akka, RMI), Raft/Paxos hors de ces TP, ni une question de cours sans code.
---

# TP d'algorithmes distribués (simulateur ENSISA)

Chaque processus du système distribué est une sous-classe Java de `ensisa.process.Process`. Le simulateur lance n processus et relie leurs messages selon une topologie. Le travail type consiste à :
- coder un algorithme ;
- le tester sur plusieurs topologies et tailles ;
- l'étendre selon une question suivante ;
- rendre un compte-rendu.

L'utilisateur veut un code sans commentaires, qui se lit comme celui d'un étudiant qui maîtrise le sujet. Le code sera relu par l'enseignant.

## Démarche

1. **Lire tout le matériel.**
   - À la racine du dépôt : l'énoncé (`TP n .pdf`) et l'API (`API.pdf`).
   - Dans `Projet/` : les jar, `readme.txt` et l'exemple du prof (`ensisa/TestMesh1.java` ou équivalent).
   - Le jar contient les sources de la librairie, qui font foi : `unzip -p processlibrary.jar ensisa/process/Process.java` (idem pour `InputMessage.java` et `OutputMessage.java`).
   - `API.pdf` répète ces sources.
   - Si un comportement n'est ni dans les sources ni dans `references/simulateur.md`, consulte le bytecode : `javap -c -p -cp processlibrary.jar ensisa.process.PrivateNetwork`.
   - Les faits de `references/simulateur.md` ont été vérifiés sur le jar du 2023-11-27 (`unzip -l` affiche la date). Si le jar a changé, revérifie ceux dont tu dépends.

2. **Relever ce que l'énoncé impose**, question par question :
   - le ou les initiateurs ;
   - le contenu des messages et le texte exact des affichages ;
   - les noms de classes et les topologies à tester ;
   - les livrables.

   Ce qui est imposé l'emporte sur les assets. Ceux-ci viennent du TP1 (écho) : leur texte, leur initiateur 0 et leurs affichages ne valent que pour ce TP. Les questions théoriques ou sans code se traitent dans le compte-rendu.

3. **Vérifier Java** (JDK 21 minimum).
   - Si `java` est introuvable, regarde d'abord `ls /usr/lib/jvm` avant de proposer `sudo apt install openjdk-21-jdk`. Le bac à sable de Bash peut masquer `/usr/bin/java` alors qu'il est installé.
   - Dans ce cas, préfixe tes commandes par `export PATH=/usr/lib/jvm/<jdk>/bin:$PATH`. Ne touche pas au PATH de l'utilisateur : chez lui, il est correct.
   - Valide ensuite le simulateur : compile et lance l'exemple du prof avec la commande de `readme.txt`.

4. **Coder une classe par question**, dans le paquetage `ensisa`, sans toucher à l'exemple du prof, aux jar ni aux classes des TP précédents.
   - Commence par lire `references/style.md` et `references/simulateur.md`.
   - Dans `references/algorithmes.md`, lis la section 1 (structure commune), puis seulement la fiche de ton algorithme.
   - `assets/Echo.java` montre la forme attendue : squelette, noms, longueur. Reprends sa structure, pas son contenu.
   - Pour une boucle sur plusieurs types de messages, `assets/ChangRoberts.java` (élection sur anneau) sert d'exemple.
   - Une question qui étend la précédente (« modifiez le programme pour… ») devient une nouvelle classe copiée de la précédente, pour que les deux versions restent démontrables.

5. **Tester.**
   - Si `Projet/run.sh` et `Projet/test.sh` existent déjà (TP précédent), garde-les et ajoute une branche pour la nouvelle classe dans le `case` de `check`. Sinon, copie `assets/run.sh` et `assets/test.sh`.
   - Lance d'abord un `./run.sh Classe 5 <topo>` et lis la sortie toi-même. Lance ensuite `./test.sh Classe`, qui teste n = 2, 3, 4, 8, 15, 20.
   - Adapte `topologies` : ajoute `-unidirectional` si l'algorithme s'y applique, retire ce qui ne s'applique pas.
   - Adapte `check` à l'algorithme. Par défaut, seul `-random` est répété ; si l'algorithme tire des valeurs au hasard, répète toutes les topologies (`runs=5` partout).
   - Si la sortie dépend de valeurs aléatoires, `check` relit les valeurs affichées et recalcule le résultat attendu, au lieu de chercher une ligne fixe. `assets/test-election.sh` le fait avec un petit `awk`.
   - La matrice prend 3 à 4 minutes avec mesh et random (une simulation à n=20 en mesh dure environ 18 s), et moins d'une minute en unidirectionnel seul. Donne à Bash un timeout de 10 min, ou lance la matrice en arrière-plan.
   - **Ne lance aucune autre simulation pendant ce temps** : tous les processus passent par le port 4444.
   - `Process X is waiting for a message but all processes are terminated` signale un interblocage ou un mauvais compte de messages.
   - Un timeout signale plutôt des messages qui tournent sans fin ou un envoi hors topologie. Relance avec `run.sh`, et ajoute si besoin `-log debug`.

6. **Compte-rendu** dans `Projet/`, à côté des scripts.
   - Nom : `COMPTE_RENDU.md`, ou `COMPTE_RENDU_TPn.md` si celui d'un TP précédent existe déjà.
   - `assets/compte-rendu-modele.md` donne le plan par défaut et le cas d'un énoncé à questions numérotées.
   - Les sorties citées sont des sorties réellement obtenues.

7. **Relire** le code avec la checklist de `references/style.md`, résumer les résultats, puis proposer un commit.

8. **VS Code**, seulement si l'utilisateur signale des erreurs ou si la racine du dépôt n'a pas de `.vscode/settings.json` alors que `Projet/` n'est pas la racine. Copie `assets/vscode-settings.json` dans `<racine>/.vscode/settings.json` en adaptant le chemin. Sans ce fichier, `Process` est compris comme `java.lang.Process`, ce qui donne « id cannot be resolved » et « must implement getErrorStream ».

## Pièges qui coûtent du temps

- `-unidirectional` : on ne peut envoyer qu'à `id+1`. Tout algorithme qui répond à l'émetteur, comme l'écho, y est impossible dès que n ≥ 3. Avec n=2, l'anneau contient 0→1 et 1→0, donc l'écho y fonctionne par hasard.
  - `send()` revient normalement : c'est le simulateur qui affiche `No communication channel`.
  - Ensuite, plus rien de cet émetteur n'est relayé, et la simulation se bloque jusqu'au timeout.
  - Un try/catch ne peut pas intercepter cette erreur.
- `inNeighbors` et `outNeighbors` contiennent les mêmes voisins, mais pas dans le même ordre. Ne suppose aucun ordre.
- L'ordre des affichages varie d'une exécution à l'autre. Teste la présence et le nombre des lignes, pas leur ordre.
- Une seule simulation à la fois (port 4444), toujours sous `timeout 60`. Avec 20 s, on obtient de faux timeouts à n=20.
- `receiveFrom` et `receiveOfType` mettent de côté les autres messages. Attendre une réponse précise pendant que d'autres messages doivent être traités mène à un interblocage. Utilise alors une boucle `receive()`.
- Après `run()`, un processus continue d'absorber les messages **sans y répondre**, jusqu'à la fin globale. Un processus qui doit encore répondre aux autres ne doit pas sortir trop tôt.
- `inNeighbors`, `outNeighbors` et `circuit` sont immuables : copie-les dans une `new ArrayList<>(…)` avant de les modifier.
- Le simulateur rend le code 0 même si un processus lève une exception : cherche `Exception` dans la sortie.
- `receiveOrTerminate` détecte la terminaison globale (Misra) sans compter les messages, sauf si l'énoncé demande de la coder soi-même.

Le reste est dans `references/simulateur.md` : API, format des messages, types réservés, bornes de n, erreurs connues.

## Rendu attendu

- `Projet/ensisa/<Algo>.java`, plus une classe par extension : compile sans avertissement, sans commentaires.
- `Projet/run.sh` et `Projet/test.sh` : `test.sh` affiche `N OK, 0 ECHEC`.
- Le compte-rendu.

Si l'utilisateur demande d'abord « un prompt pour faire le TP », rédige-le à partir de ces étapes, adaptées à l'énoncé lu.
