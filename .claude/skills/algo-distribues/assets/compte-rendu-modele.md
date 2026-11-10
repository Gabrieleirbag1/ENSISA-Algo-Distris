# TPn – <Nom de l'algorithme>

<Plan par défaut. Si l'énoncé numérote ses questions, remplacer les sections suivantes par une section par question :
- « Question 1 » : principe, pseudo-code, justification, sortie ;
- « Question 2 » : mesures ou observations, si la question est sans code ;
- « Question 3 » : l'extension ;
- puis « Tests » et « Limites ».>

## Fichiers

- `ensisa/<Algo>.java` : le livrable, un seul fichier qui traite toutes les questions
- `run.sh` : compile et lance une simulation, ex. `./run.sh <Algo> 8 -random`
- `test.sh` : lance toutes les topologies et vérifie les sorties, ex. `./test.sh <Algo>`

Les scripts ne sont pas nécessaires. Depuis ce dossier, les commandes de l'énoncé suffisent :

```
javac -cp processlibrary.jar ensisa/<Algo>.java
java -jar simulator.jar -n 4 -random -classpath ".:processlibrary.jar" ensisa.<Algo>
```

## Principe

<Deux ou trois paragraphes : ce que fait l'initiateur, ce que font les autres, les types de messages.>

```
<pseudo-code court>
```

## Pourquoi ça marche

<Un court paragraphe par propriété :
- ce qui est garanti (affichage unique, élu unique…) ;
- le nombre de messages ;
- pourquoi chaque processus s'arrête sans qu'on lui écrive ensuite.>

## Exemples de sortie

`./run.sh <Algo> 5 -<topologie>`
```
<sortie réellement obtenue>
```

<Une ou deux sorties courtes (n ≤ 6). Préciser que l'ordre des lignes varie d'une exécution à l'autre.>

## Question <Y>

<Ce que la même classe fait en plus pour cette question : format du message, ce que chaque processus garde, ce qui est affiché, une sortie réelle.>

## Tests

<Ce que vérifie test.sh, la matrice testée, le résultat obtenu.>

## Limites

<S'il y en a : topologies où l'algorithme ne s'applique pas, avec le message d'erreur réel.>
