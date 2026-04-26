# Tests d'integration - Quiz Multijoueur

Ce document couvre les tests minimums pour valider l'integration Socket + DB + JavaFX.

## Prerequis

- JDK 21 installe (`java -version`)
- Maven installe (`mvn -v`)

## 1) Compilation du projet

```bash
mvn -DskipTests compile
```

## 1.b) Tests unitaires rapides

```bash
mvn test
```

Attendu:
- tests `ScoreCalculatorTest` passes
- tests `MessageTest` passes

## 2) Demarrage serveur

Dans un premier terminal:

```bash
mvn -DskipTests exec:java -Dexec.mainClass=server.ServerMain
```

Attendu:
- log de demarrage du serveur sur le port 5000
- base SQLite initialisee dans `resources/quiz.db`

## 3) Test console multi-clients

Dans deux terminaux differents:

```bash
mvn -DskipTests exec:java -Dexec.mainClass=client.ClientMain
```

Scenario conseille:
1. Client A: `register alice 1234`
2. Client A: `login alice 1234`
3. Client A: `create room1 4`
4. Client B: `register bob 1234`
5. Client B: `login bob 1234`
6. Client B: `join room1`
7. Clients A/B: `ready`
8. Repondre a plusieurs questions avec `answer A`/`B`/`C`/`D`

Verifier:
- reception `QUESTION`
- reception `ANSWER_RESULT`
- reception `SCORES_UPDATE`
- reception `GAME_OVER`

## 4) Test JavaFX

Dans un terminal:

```bash
mvn -DskipTests javafx:run
```

Verifier:
- login/inscription fonctionnels
- navigation login -> lobby -> game -> score
- scores live dans la table laterale

## 5) Cas d'erreur a valider

1. Mauvais mot de passe: `LOGIN_FAIL`
2. Room inexistante: `ERROR`
3. Deconnexion d'un joueur en partie:
   - notification `PLAYER_LEFT`
   - si joueurs restants < 2: `ERROR` puis `GAME_OVER`
4. Timeout question sans reponse:
   - passage automatique a la question suivante

## 6) Verifications DB

Apres au moins une partie terminee, verifier en base:
- table `scores`: lignes inserees
- table `players`: `total_games`, `total_wins`, `best_score` mis a jour

Exemple commande SQLite:

```bash
sqlite3 resources/quiz.db "SELECT username,total_games,total_wins,best_score FROM players ORDER BY best_score DESC;"
```
