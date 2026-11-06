# Style de code attendu

Le code est relu par l'enseignant. Chaque tic de code généré ralentit sa lecture et fait douter de l'auteur. L'objectif est un code qu'un étudiant à l'aise écrirait en TP : direct, court, sans décoration.

Prends la forme de l'exemple du prof (`TestMesh1.java`) :
- une classe ;
- un `run()` qui se lit de haut en bas ;
- des `System.out.println` avec concaténation.

N'en reprends pas les commentaires : l'utilisateur n'en veut aucun.

## À faire

- Garder le squelette du prof : constructeur vide, `main` avec `var process = new X(); process.start();`, `run()` public.
- Importer les classes une par une, comme le prof : `import ensisa.process.Process;`. `import ensisa.process.*;` ne compile pas, car `Process` est ambigu avec `java.lang.Process`.
- Nommer les variables comme au tableau, en anglais court : `father`, `count`, `known`, `neighbor`, `message`, `first`, `text`.
- Garder la logique dans `run()` tant qu'il tient sur un écran, même avec une boucle d'événements à deux ou trois types de messages.
- Quand `run()` ne tient plus sur un écran, faire une méthode par type de message (`onRequest`, `onReply`) ; l'état partagé passe alors en champs.
- Hors de ce cas, créer une méthode seulement si elle sert au moins deux fois, comme `createMessage` et `readIds` dans `EchoCollect`.
- Définir les types de message en `static final int` ≥ 0, comme dans `Echo.java`. L'`enum MessageType` du prof est acceptable si l'énoncé le reprend.
- Afficher au format exact demandé par l'énoncé. À défaut : en français, sur une ligne, comme `Echo.java`. `test.sh` doit pouvoir le reconnaître avec `grep`.
- Préférer des `if` simples aux constructions astucieuses.

## À éviter

- Commentaires, javadoc, `// TODO`, bannières `// ===== PHASE 1 =====`.
- `final` sur chaque variable locale, logger, getters et setters, `@Override` systématique.
- Méthodes d'une ligne appelées une seule fois (`isInitiator()`, `sendToAll()`).
- Une constante pour tout (`INITIATOR = 0`, `MESSAGE_TEXT`).
- Classes utilitaires, interfaces, design patterns, `Optional`, streams et lambdas là où une boucle `for` suffit.
- `String.format`, `printf`, text blocks là où une concaténation suffit.
- try/catch qui attrapent puis réaffichent : `run()` déclare déjà `throws Exception`.
- Affichages décoratifs : emojis, `[INFO]`, `>>>`, `✓`, majuscules.
- Noms à rallonge (`numberOfReceivedEchoMessages`) ou à préfixe (`mFather`, `iCount`).
- Vérifications défensives impossibles ici (`if (outNeighbors == null)`).
- `switch` avec une branche `default` qui lance « Unknown message type ».
- Fonctionnalités non demandées : statistiques, chronométrage, couleurs ANSI.

## Checklist avant de rendre

- [ ] `grep -nE '//|/\*' ensisa/<Classe>*.java` ne trouve rien dans les classes écrites (l'exemple du prof garde ses commentaires).
- [ ] `javac -Xlint -cp processlibrary.jar ensisa/<Classe>.java` ne produit aucun avertissement.
- [ ] `run()` se lit d'une traite, chaque variable a un rôle évident.
- [ ] Aucune méthode d'une ligne n'est appelée une seule fois.
- [ ] Les affichages correspondent exactement à ce que vérifie `test.sh`.
- [ ] Les scripts shell sont courts : pas de couleurs, pas de bannières.

## Compte-rendu

Même règle : des phrases simples et des paragraphes courts. Évite :
- les puces à intitulé en gras (`- **X.** …`) ;
- « En résumé », « robuste », « élégant » ;
- les tableaux décoratifs et les emojis.

Un tableau de mesures (n, nombre de messages) est accepté.
