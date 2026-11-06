# Fiches algorithmes

Chaque fiche donne l'idée, la topologie qui convient, le coût et la façon de l'écrire dans ce simulateur. Seul l'écho a été réalisé et testé en TP (TP1, `assets/Echo.java`). Pour les autres, l'énoncé fait foi, car le prof peut imposer une variante.

## Sommaire
1. Structure commune
2. Écho de Chang et Segall (diffusion + remontée)
3. Parcours de Tarry / parcours en profondeur
4. Élection sur anneau unidirectionnel : LeLann, Chang-Roberts
5. Élection sur anneau bidirectionnel : Hirschberg-Sinclair
6. Élection sur graphe quelconque : extinction d'échos
7. Arbre couvrant
8. Détection de terminaison : Misra, Dijkstra-Scholten
9. Exclusion mutuelle : Ricart-Agrawala, Lamport, jeton sur anneau
10. Snapshot de Chandy-Lamport

---

## 1. Structure commune

Deux formes suffisent presque toujours.

**Compter les messages attendus.** C'est la bonne forme quand on sait combien de messages vont arriver, par exemple « un par voisin ».

```java
while (count < inNeighbors.size()) {
    InputMessage message = receive();
    count++;
}
```

**Boucle d'événements.** C'est la bonne forme dès qu'un processus doit réagir à plusieurs types de messages, ou à des messages imprévus. L'état partagé passe en champs, avec une méthode par type si `run()` devient long.

```java
while (!done) {
    InputMessage message = receive();
    if (message.type == REQUEST) {
        onRequest(message);
    } else if (message.type == REPLY) {
        onReply(message);
    }
}
```

N'utilise pas `receiveFrom` ou `receiveOfType` pour attendre une réponse précise pendant que d'autres messages doivent être traités : les messages mis de côté ne sont pas traités. Deux processus qui s'attendent mutuellement finissent sur `Process X is waiting for a message but all processes are terminated`.

Penser à **l'arrêt** dès la conception :
- chaque processus doit savoir quand il a reçu son dernier message ;
- sinon, utilise une annonce de fin qui fait le tour (`ELECTED`, `DONE`), ou `receiveOrTerminate`.

Après `run()`, un processus absorbe les messages sans y répondre (`simulateur.md`, « Ce qui se passe après run() »).

---

## 2. Écho de Chang et Segall

- **Topologie** : graphe connexe bidirectionnel (`-bidirectional`, `-mesh`, `-random`).
- **Coût** : exactement 2|E| messages, un dans chaque sens sur chaque arête.
- **Principe** :
  - l'initiateur envoie à tous ses voisins, puis attend un message de chacun ;
  - un suiveur prend comme père l'émetteur de son premier message, transmet à tous ses voisins sauf son père, attend un message de chaque voisin (père compris), puis renvoie l'écho à son père et s'arrête.
- Un seul type de message suffit : un message venant d'un non-père vaut écho.
- **Terminaison sûre** : quand un processus s'arrête, il a reçu un message de chaque voisin, donc plus personne ne lui écrira.
- **Remontée d'information** : chaque message transporte des données, par exemple un ensemble d'identifiants. Chaque processus fusionne ce qu'il reçoit et le renvoie à son père (`assets/EchoCollect.java`).
- Sur `-unidirectional` avec n ≥ 3, c'est impossible, car il faudrait répondre au père.

## 3. Parcours de Tarry / parcours en profondeur

- **Topologie** : graphe connexe bidirectionnel.
- **Principe** : un **jeton unique** visite tout le graphe, de façon séquentielle. Coût : 2|E| messages.
- **Règles de Tarry** :
  1. On n'envoie jamais le jeton deux fois sur le même canal.
  2. On ne renvoie le jeton au père que lorsqu'il ne reste aucun autre canal.
- **Variante en profondeur** : un processus déjà visité qui reçoit le jeton d'un non-père le renvoie immédiatement à l'émetteur.
- **Implémentation** :
  - liste des canaux encore libres : `new ArrayList<>(outNeighbors)`, car la liste fournie est immuable ;
  - une boucle `receive()` ;
  - l'initiateur s'arrête quand il reçoit le jeton et n'a plus de canal libre ;
  - les autres s'arrêtent après avoir rendu le jeton à leur père.
- **À afficher** : l'ordre de visite (le jeton peut transporter la liste des processus visités) et l'arbre (`father`).

## 4. Élection sur anneau unidirectionnel

- **Topologie** : `-unidirectional`. On n'envoie qu'au successeur, `outNeighbors.get(0)`.

**LeLann**
- Chaque processus envoie son identifiant, et chaque identifiant fait un tour complet.
- Chaque processus voit passer tous les identifiants et connaît donc le maximum.
- Un processus s'arrête quand son propre identifiant lui revient ; les canaux FIFO garantissent qu'il a déjà vu passer tous les autres.
- Coût : n² messages.

**Chang-Roberts**
- Chaque processus envoie sa valeur candidate. À la réception de `j` :
  - si `j > candidat`, il transmet ;
  - si `j < candidat`, il détruit le message ;
  - si `j == candidat`, il est élu.
- L'élu envoie `ELECTED` pour un tour complet. Chaque processus s'arrête après l'avoir transmis, et l'élu quand `ELECTED` lui revient.
- Coût en messages de candidature :
  - pire cas : n(n+1)/2 ;
  - moyenne : n·H(n), soit O(n log n) ;
  - meilleur cas : 2n-1.

  Il faut y ajouter n messages `ELECTED`.
- Pourquoi l'arrêt est sûr : les canaux sont FIFO, et `ELECTED` part après la dernière candidature qui circule encore. C'est donc le dernier message sur chaque lien, et tout compteur porté par `ELECTED` est définitif à sa réception.
- Test : les candidats sont aléatoires. `check` doit donc relire les valeurs affichées (« Processus i propose v ») et refaire l'élection pour connaître l'élu et le nombre de messages attendus, par exemple avec `awk`. Répète aussi chaque taille plusieurs fois.

**Pièges**
- Avec les identifiants comme candidats, l'anneau 0→1→…→n-1 donne toujours le **meilleur cas** pour l'élection du maximum : chaque identifiant est détruit dès le premier saut, soit 2n-1 messages, et le résultat est connu d'avance. Pour une démonstration intéressante, tire une valeur aléatoire distincte, par exemple `new Random().nextInt(1000) * 100 + id`. Fais-le si l'énoncé le permet ou le demande.
- Après avoir détruit un message, un processus doit continuer à recevoir. La condition d'arrêt est la réception de `ELECTED`.

## 5. Élection sur anneau bidirectionnel : Hirschberg-Sinclair

- **Topologie** : `-bidirectional`. Calcule les deux voisins avec `(id + 1) % n` et `(id + n - 1) % n`.
- **Messages** : `PROBE(candidat, phase, distance)` et `REPLY(candidat, phase)`.
- **Phase k** : chaque candidat encore actif envoie `PROBE` dans les deux sens.
- **Un processus qui reçoit un `PROBE`** :
  - si le candidat est plus petit que lui, il l'arrête ;
  - s'il est plus grand et que la distance est inférieure à 2^k, il le relaie plus loin ;
  - s'il est plus grand et que la distance vaut 2^k, il renvoie `REPLY` vers l'émetteur.
- **Les `REPLY`** sont relayés par les intermédiaires jusqu'au candidat. Celui-ci passe à la phase k+1 quand il a reçu ses deux `REPLY`.
- **Élection** : un candidat qui reçoit son propre `PROBE` est élu. Il le reçoit des deux côtés : il ne doit se déclarer élu qu'une fois.
- **Fin** : l'élu fait faire le tour à `ELECTED`. Sans cela, les autres restent dans leur boucle et finissent sur l'exception « waiting for a message ».
- **Coût** : O(n log n) messages.
- n=2 : un seul voisin, les deux sens sont le même canal. Teste ce cas à part.
- Il faut une boucle d'événements.

## 6. Élection sur graphe quelconque : extinction d'échos

- **Topologie** : graphe connexe bidirectionnel.
- **Principe** :
  - chaque initiateur lance un écho (section 2) marqué de son identifiant ;
  - un processus qui reçoit une vague plus grande que celle qu'il suit l'adopte : nouveau père, compteur remis à 0 ;
  - il ignore les vagues plus petites.
- Seule la vague du maximum se termine, et son initiateur est élu. Il diffuse ensuite le résultat pour que tout le monde s'arrête.
- **Coût** : O(n·|E|) dans le pire cas.

## 7. Arbre couvrant

- Les pointeurs `father` de l'écho ou de Tarry forment l'arbre.
- Pour qu'un processus connaisse aussi ses fils : le message qu'un processus envoie à son père porte `1`, tous ses autres envois portent `0`. Chacun compte comme fils les voisins qui lui ont envoyé `1`.
- L'initiateur peut afficher l'arbre entier si chaque écho remonte la liste des arêtes (père, fils) du sous-arbre.

## 8. Détection de terminaison

**Misra**
- Déjà fournie par `receiveOrTerminate` (`simulateur.md`).
- Si l'énoncé demande de la **coder**, les règles sont les suivantes :
  - un processus devient noir quand il reçoit un message de base ;
  - le jeton ne part que d'un processus **passif** ;
  - un processus noir remet `nb` à 0, un blanc fait `nb++` ;
  - le processus redevient blanc dès qu'il passe le jeton ;
  - la terminaison est détectée quand `nb == circuit.size()` et que le processus qui reçoit le jeton est blanc.
- Le jeton suit `circuit`, qui est cyclique.

**Dijkstra-Scholten** (calcul diffus)
- Chaque message de base est acquitté.
- Le premier message reçu quand on n'a pas de père crée le lien père.
- Un message reçu quand on a déjà un père est acquitté immédiatement.
- On acquitte son père quand on est passif et que tous ses propres messages ont été acquittés (déficit nul). On se désengage alors, et on peut retrouver un père plus tard.
- La terminaison est détectée quand la racine est passive avec un déficit nul.

## 9. Exclusion mutuelle

**Ricart-Agrawala**
- **Topologie** : `-mesh` (graphe complet). Horloges de Lamport.
- **Coût** : 2(n-1) messages par section critique.
- **Pour entrer** : envoyer `REQUEST(h, id)` à tous, où `(h, id)` est l'estampille de **sa** requête, puis attendre n-1 `REPLY`.
- **À la réception d'un `REQUEST`** :
  - répondre tout de suite si l'on n'est **ni en SC ni demandeur**, ou si la requête reçue est prioritaire (plus petit `(h, id)`) ;
  - sinon, différer la réponse jusqu'à la sortie de SC.
- Il faut une boucle d'événements.

**Lamport**
- File de requêtes répliquée, avec `REQUEST`, `REPLY` et `RELEASE`.
- Coût : 3(n-1) messages par SC.

**Jeton sur anneau**
- `-unidirectional` : le jeton circule, et son détenteur peut entrer en SC.

**Pièges**
- Un processus qui a fini ses k sections critiques ne doit pas sortir de `run()`. Après `run()`, il absorberait les `REQUEST` sans répondre, et les autres resteraient bloqués. Il continue donc de répondre jusqu'à un `DONE` global : par exemple, chacun envoie `DONE` à tous et s'arrête quand il en a reçu n-1.
- Le jeton sur anneau doit s'arrêter un jour : fixer un nombre de tours, ou faire circuler une annonce de fin.
- **Démonstration** : afficher `Processus i entre en SC` / `sort de SC`. Le test vérifie que les entrées et les sorties alternent strictement.

## 10. Snapshot de Chandy-Lamport

- **Prérequis** : canaux FIFO (c'est le cas dans ce simulateur) et graphe fortement connexe. Toutes les topologies conviennent, y compris `-unidirectional`.
- **Déroulement** :
  1. L'initiateur enregistre son état et envoie `MARKER` sur tous ses canaux sortants.
  2. Au premier `MARKER` reçu, un processus enregistre son état, déclare vide le canal d'où vient le marqueur, et envoie `MARKER` partout.
  3. Ensuite, chaque message de base reçu sur un canal qui n'a pas encore livré son `MARKER` fait partie de l'état de ce canal.
  4. Un processus a fini quand il a reçu un `MARKER` de chacun de ses `inNeighbors`.
- **Piège** : ne pas attendre les `MARKER` avec `receiveOfType`. Le `MARKER` passerait alors devant des messages de base arrivés plus tôt sur le même canal, ce qui fausse l'état des canaux. Il faut une boucle `receive()`.
- **Calcul de base en parallèle** : par exemple, des transferts d'argent aléatoires dont on vérifie que la somme totale est conservée dans l'instantané.
