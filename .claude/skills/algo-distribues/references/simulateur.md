# Simulateur ENSISA – référence

Tout ce qui suit a été vérifié sur le jar du 2023-11-27, soit dans les sources et le bytecode (`javap -c -p`), soit par des programmes de test. `PrivateNetwork` n'a pas de source dans le jar : son comportement ne se lit qu'avec `javap`.

## Ligne de commande

```
javac -cp processlibrary.jar ensisa/MaClasse.java
timeout 60 java -jar simulator.jar -n N TOPO [-log niveau] -classpath "processlibrary.jar:." ensisa.MaClasse
```

- `-n N` : de 2 à 20. Valeur par défaut : 4.
  - Hors de l'intervalle : `[Error] Incorrect number of processes`.
  - Valeur non numérique : `[Error] Number of processes expected after -n`.
- `TOPO` : `-unidirectional`, `-bidirectional`, `-mesh` ou `-random`. Valeur par défaut : `-random`.
- `-log debug|info|warn|error` (défaut `error`).
  - `info` affiche la liste d'adjacence et le circuit.
  - `debug` affiche en plus chaque passage du jeton de Misra.
- La classe doit être le **dernier** argument. Sinon : `Usage: simulator …`.
- Sous Windows, le classpath utilise `;` au lieu de `:`.
- Chaque processus est une JVM séparée. Tous les messages passent par le simulateur, en TCP sur 127.0.0.1:4444.
  - **Une seule simulation à la fois.**
  - Après un `timeout`, aucune JVM ne reste orpheline.
- Le simulateur rend le code 0 même si un processus a levé une exception. Il faut donc chercher `Exception` dans la sortie.
- La sortie d'erreur des processus est renvoyée sur celle du simulateur, d'où `2>&1` dans les scripts.

### Durée

La fin d'une simulation fait circuler le jeton de Misra sur tout `circuit`, à environ 40 ms par passage. Ordres de grandeur pour un programme vide :

| Topologie, n = 20 | Durée |
|---|---|
| `-bidirectional` | environ 3 s |
| `-mesh` | environ 18 s (circuit de 380 arcs) |
| `-random` | jusqu'à environ 18 s |

D'où `timeout 60` dans les scripts. Avec 20 s, on obtient de faux timeouts à n = 20. Une matrice complète prend 3 à 4 minutes.

## Topologies

L'ordre des voisins est **déterministe** :
- `outNeighbors` liste `(id + k) % n` pour k croissant ;
- `inNeighbors` liste `(id - k) mod n`.

Les deux listes contiennent les mêmes voisins, sauf en `-unidirectional`, mais dans un ordre différent.

| Topologie | Voisins |
|---|---|
| `-unidirectional` | out=[id+1], in=[id-1], circuit `[0, 1, …, n-1]` |
| `-bidirectional` | anneau. Pour n=4, le processus 0 a out=[1, 3] et in=[3, 1]. Pour n=2, un seul voisin, sans doublon |
| `-mesh` | graphe complet |
| `-random` | un chemin hamiltonien aléatoire, plus quelques arêtes au hasard. Change à chaque exécution. Pour n ≤ 3, c'est toujours un simple chemin |

- En `-unidirectional` avec n=2, on a 0→1 et 1→0 : un algorithme à réponse y fonctionne par hasard. Il échoue dès que n ≥ 3.
- `circuit` est un parcours **cyclique** qui couvre tous les arcs : le dernier élément se raccorde au premier. La librairie s'en sert pour le jeton de Misra.
- `inNeighbors`, `outNeighbors` et `circuit` sont **immuables** (`List.copyOf`), et `remove` lève `UnsupportedOperationException`. Pour une liste modifiable, copie-la : `new ArrayList<>(outNeighbors)`.
- Pour un anneau bidirectionnel, calcule plutôt `(id + 1) % n` et `(id + n - 1) % n`.

## Classe `Process`

- Importe `ensisa.process.Process` explicitement. `import ensisa.process.*;` ne compile pas, car `Process` devient ambigu avec `java.lang.Process`.

| Membre | Comportement vérifié |
|---|---|
| `int id`, `int n` | identité (à partir de 0) et nombre de processus |
| `inNeighbors`, `outNeighbors`, `circuit` | listes immuables (voir plus haut) |
| `send(to, m)` | revient toujours normalement si `to ≥ 0` et `type ≥ 0`. Un envoi vers un non-voisin est refusé **par le simulateur** : voir « Erreurs » |
| `receive()` | rend d'abord les messages mis de côté (dans leur ordre d'arrivée), puis ceux du réseau. N'appelle jamais de handler |
| `receiveFrom(from)`, `receiveOfType(type)`, `receiveFromType(from, type)` | met de côté les messages qui ne correspondent pas, sauf ceux dont le type a un handler : celui-ci est alors appelé. `receiveFrom` d'un processus absent de `inNeighbors` lève tout de suite `No communication channel between process <from> and process <id>` |
| `receiveOfType(type, ms)` | renvoie `null` si aucun message de ce type n'arrive en `ms`. Le délai repart à chaque message reçu qui ne correspond pas |
| `receiveOrTerminate(boolean[] t)` | voir plus bas |
| `onMessageReception(type, h)` | enregistre un handler pour ce type |
| `waitReception()` | traite **un seul** message qui a un handler (d'abord parmi ceux mis de côté) et met les autres de côté |

Squelette :

```java
public class MaClasse extends Process {
    public MaClasse() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        var process = new MaClasse();
        process.start();
    }

    public void run() throws Exception {
    }
}
```

### Ce qui se passe après `run()`

`start()` appelle `run()`, puis `close()`. `close()` **continue de recevoir** jusqu'à ce que toute la simulation soit terminée :
- il appelle les handlers des messages qui en ont un ;
- il jette les autres sans rien dire ;
- il fait passer le jeton de Misra.

Conséquences :
- Envoyer à un processus qui a fini ne provoque aucune erreur, mais le message est perdu.
- Un processus qui doit encore répondre aux autres (exclusion mutuelle, par exemple) ne doit pas sortir de `run()` trop tôt.

## Terminaison et interblocage

Le jeton de Misra circule à l'intérieur de **tous** les appels de réception. Quand tous les processus attendent un message ou ont fini, la terminaison globale est détectée, et :
- `receiveOrTerminate(t)` met `t[0] = true` et renvoie `null`, y compris lors des appels suivants ;
- tout processus encore bloqué dans un autre `receive*` lève `RuntimeException: Process X is waiting for a message but all processes are terminated`.

Un interblocage ou un mauvais compte de messages donne donc **cette exception**, rapidement, et non un timeout.

Pour arrêter proprement tous les processus sans compter les messages :

```java
boolean[] terminate = new boolean[1];
while (true) {
    InputMessage message = receiveOrTerminate(terminate);
    if (terminate[0]) {
        break;
    }
    ...
}
```

Il suffit que chaque processus soit dans cette boucle **ou** déjà sorti de `run()`. C'est vérifié pour une diffusion par inondation sur l'anneau unidirectionnel, le mesh et le random. Si l'énoncé demande d'**implémenter** la détection de terminaison, il faut la coder soi-même au lieu d'utiliser ce raccourci.

## Messages

- `new OutputMessage(type)` ou `new OutputMessage(type, int)`.
  - Écriture : `writeInt`, `writeString` (longueur sur un int, puis les octets).
  - `stream()` donne un `DataOutputStream` pour les autres types (`writeBoolean`, `writeLong`…).
- `InputMessage` : `senderProcessId`, `type`, `readInt()`, `readString()`, `stream()`.
- On relit dans l'ordre d'écriture. Pour une collection, écris d'abord sa taille, puis les éléments.
- `type` doit être ≥ 0. Un type négatif est refusé dans `send()` par `Incorrect type in message` ; le jeton de Misra utilise `-1` en interne.
- Un même `OutputMessage` peut être envoyé plusieurs fois.
- Les canaux sont **FIFO** pour chaque paire émetteur→récepteur : un thread par émetteur dans le simulateur. Aucune violation sur 15 000 messages.

## Erreurs connues

| Symptôme | Cause |
|---|---|
| `[Error] Incorrect number of processes` | `n < 2` ou `n > 20` |
| `Usage: simulator …` | arguments dans le désordre : la classe doit être le dernier |
| `Exception in thread "Thread-k" … No communication channel between process X and process Y`, puis timeout | `send` de X vers Y qui n'est pas son voisin. `send()` est revenu normalement, mais le simulateur ne relaie plus rien de X. Classique en `-unidirectional`, où l'on répond à l'émetteur |
| `No communication channel …` levée dans `receiveFrom` | on attend un processus qui n'est pas dans `inNeighbors` |
| `Incorrect type in message between process X and process Y` | type de message négatif |
| `Process X is waiting for a message but all processes are terminated` | interblocage ou mauvais compte : X attend un message que personne n'enverra |
| Timeout | messages ou jeton applicatif qui tournent sans fin, processus qui boucle sans jamais recevoir, envoi hors topologie, ou simplement n=20 en mesh ou random avec un timeout trop court |
| Valeur absurde, `EOFException`, `NegativeArraySizeException` à la lecture | lecture dans un autre ordre que l'écriture, ou lecture en trop |
| VS Code : « The import ensisa.process cannot be resolved », « must implement getErrorStream » | le jar n'est pas dans le classpath de l'éditeur (voir `assets/vscode-settings.json`) |
| `reference to Process is ambiguous` | `import ensisa.process.*;` : importer `ensisa.process.Process` explicitement |
