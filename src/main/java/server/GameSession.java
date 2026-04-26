package server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import database.DatabaseManager;
import database.PlayerDAO;
import database.QuestionDAO;
import model.Question;
import protocol.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GameSession {
    public enum GameState {
        WAITING,
        STARTING,
        IN_PROGRESS,
        FINISHED
    }

    private static final int QUESTION_TIME_LIMIT_SECONDS = 15;

    private final String roomName;
    private final int maxPlayers;
    private final List<ClientHandler> players;
    private final List<Question> questions;
    private final Map<String, Integer> scores;
    private final Map<String, Integer> correctAnswers;
    private final Set<String> readyPlayers;
    private final Set<String> answeredPlayersThisQuestion;
    private final ScheduledExecutorService scheduler;

    private volatile int currentQuestionIndex;
    private volatile long currentQuestionStartMillis;
    private volatile GameState state;
    private volatile ScheduledFuture<?> questionTimeoutTask;

    public GameSession(String roomName, int maxPlayers) {
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        this.players = new CopyOnWriteArrayList<>();
        this.questions = new ArrayList<>();
        this.scores = new ConcurrentHashMap<>();
        this.correctAnswers = new ConcurrentHashMap<>();
        this.readyPlayers = ConcurrentHashMap.newKeySet();
        this.answeredPlayersThisQuestion = ConcurrentHashMap.newKeySet();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.currentQuestionIndex = -1;
        this.state = GameState.WAITING;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public GameState getState() {
        return state;
    }

    /**
     * Ajoute un joueur si la room est ouverte et non pleine.
     */
    public synchronized boolean addPlayer(ClientHandler player) {
        if (player == null || state != GameState.WAITING || players.size() >= maxPlayers) {
            return false;
        }

        players.add(player);
        scores.putIfAbsent(player.getUsernameSafe(), 0);
        correctAnswers.putIfAbsent(player.getUsernameSafe(), 0);
        broadcast(Message.of("PLAYER_JOINED", playerJoinedPayload(player.getUsernameSafe()), "SERVER"));
        return true;
    }

    /**
     * Retire un joueur de la session; renvoie true si le joueur etait present.
     */
    public synchronized boolean removePlayer(ClientHandler player) {
        boolean removed = players.remove(player);
        if (removed) {
            String username = player.getUsernameSafe();
            readyPlayers.remove(username);
            answeredPlayersThisQuestion.remove(username);

            JsonObject leftPayload = new JsonObject();
            leftPayload.addProperty("username", username);
            broadcast(Message.of("PLAYER_LEFT", leftPayload, "SERVER"));

            // Si trop peu de joueurs restent, on termine proprement la partie en cours.
            if (state == GameState.IN_PROGRESS && players.size() < 2) {
                broadcast(Message.of("ERROR", errorPayload("Partie arretee: joueurs insuffisants."), "SERVER"));
                endGameInternal(true);
                return true;
            }

            // Si tous les joueurs restants ont deja repondu, on enchaine sans attendre le timeout.
            if (state == GameState.IN_PROGRESS && !players.isEmpty() && answeredPlayersThisQuestion.size() >= players.size()) {
                cancelTimeoutTask();
                broadcastScores();
                sendNextQuestion();
            }

            if (players.isEmpty()) {
                endGameInternal(false);
            }
        }
        return removed;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    /**
     * Marque un joueur comme pret; demarre automatiquement quand tout le monde est pret.
     */
    public synchronized void markReady(ClientHandler player) {
        if (state != GameState.WAITING || player == null) {
            return;
        }
        readyPlayers.add(player.getUsernameSafe());

        if (players.size() >= 2 && readyPlayers.containsAll(getPlayerUsernames())) {
            startGame();
        }
    }

    /**
     * Demarre la partie: charge les questions, envoie GAME_START puis la premiere question.
     */
    public synchronized void startGame() {
        if (state != GameState.WAITING && state != GameState.STARTING) {
            return;
        }

        state = GameState.STARTING;
        List<Question> selectedQuestions = new QuestionDAO().getRandomQuestions(10);
        questions.clear();
        questions.addAll(selectedQuestions);

        if (questions.isEmpty()) {
            broadcast(Message.of("ERROR", errorPayload("Aucune question disponible."), "SERVER"));
            endGameInternal(true);
            return;
        }

        state = GameState.IN_PROGRESS;
        JsonObject payload = new JsonObject();
        payload.addProperty("totalQuestions", questions.size());
        broadcast(Message.of("GAME_START", payload, "SERVER"));
        sendNextQuestion();
    }

    /**
     * Envoie la question suivante et programme un timeout a 15 secondes.
     */
    public synchronized void sendNextQuestion() {
        if (state != GameState.IN_PROGRESS) {
            return;
        }

        cancelTimeoutTask();
        currentQuestionIndex++;

        if (currentQuestionIndex >= questions.size()) {
            endGame();
            return;
        }

        answeredPlayersThisQuestion.clear();
        currentQuestionStartMillis = System.currentTimeMillis();

        Question q = questions.get(currentQuestionIndex);
        JsonObject payload = new JsonObject();
        payload.addProperty("id", q.getId());
        payload.addProperty("text", q.getQuestionText());
        payload.addProperty("optionA", q.getOptionA());
        payload.addProperty("optionB", q.getOptionB());
        payload.addProperty("optionC", q.getOptionC());
        payload.addProperty("optionD", q.getOptionD());
        payload.addProperty("timeLimit", QUESTION_TIME_LIMIT_SECONDS);

        broadcast(Message.of("QUESTION", payload, "SERVER"));

        questionTimeoutTask = scheduler.schedule(this::onQuestionTimeout, QUESTION_TIME_LIMIT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Recoit la reponse d'un joueur, calcule le score puis envoie ANSWER_RESULT.
     */
    public synchronized void receiveAnswer(ClientHandler player, String answer) {
        if (state != GameState.IN_PROGRESS || player == null || currentQuestionIndex < 0 || currentQuestionIndex >= questions.size()) {
            return;
        }

        String username = player.getUsernameSafe();
        if (answeredPlayersThisQuestion.contains(username)) {
            return;
        }

        answeredPlayersThisQuestion.add(username);

        Question currentQuestion = questions.get(currentQuestionIndex);
        boolean correct = currentQuestion.getCorrectOption().equalsIgnoreCase(answer);

        long elapsedMillis = Math.max(0, System.currentTimeMillis() - currentQuestionStartMillis);
        int pointsEarned = ScoreCalculator.calculatePoints(correct, elapsedMillis);

        if (correct) {
            scores.merge(username, pointsEarned, Integer::sum);
            correctAnswers.merge(username, 1, Integer::sum);
        }

        JsonObject resultPayload = new JsonObject();
        resultPayload.addProperty("correct", correct);
        resultPayload.addProperty("correctAnswer", currentQuestion.getCorrectOption());
        resultPayload.addProperty("pointsEarned", pointsEarned);
        player.sendMessage(Message.of("ANSWER_RESULT", resultPayload, "SERVER"));

        if (answeredPlayersThisQuestion.size() >= players.size()) {
            cancelTimeoutTask();
            broadcastScores();
            sendNextQuestion();
        }
    }

    /**
     * Diffuse le classement intermediaire en temps reel.
     */
    public synchronized void broadcastScores() {
        JsonArray scoreArray = new JsonArray();
        scores.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                JsonObject scoreObject = new JsonObject();
                scoreObject.addProperty("username", entry.getKey());
                scoreObject.addProperty("score", entry.getValue());
                scoreArray.add(scoreObject);
            });

        JsonObject payload = new JsonObject();
        payload.add("scores", scoreArray);
        broadcast(Message.of("SCORES_UPDATE", payload, "SERVER"));
    }

    /**
     * Termine la partie, diffuse GAME_OVER puis sauvegarde les resultats en base.
     */
    public synchronized void endGame() {
        endGameInternal(true);
    }

    private synchronized void endGameInternal(boolean notifyPlayers) {
        if (state == GameState.FINISHED) {
            return;
        }

        state = GameState.FINISHED;
        cancelTimeoutTask();
        scheduler.shutdownNow();

        List<Map.Entry<String, Integer>> ranking = scores.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .toList();

        if (notifyPlayers) {
            JsonArray rankingArray = new JsonArray();
            for (int i = 0; i < ranking.size(); i++) {
                Map.Entry<String, Integer> entry = ranking.get(i);
                JsonObject row = new JsonObject();
                row.addProperty("rank", i + 1);
                row.addProperty("username", entry.getKey());
                row.addProperty("score", entry.getValue());
                rankingArray.add(row);
            }

            JsonObject payload = new JsonObject();
            payload.add("ranking", rankingArray);
            broadcast(Message.of("GAME_OVER", payload, "SERVER"));
        }

        persistScores(ranking);
        for (ClientHandler player : players) {
            player.clearSession(this);
        }
        GameManager.getInstance().removeSession(roomName);
    }

    private void onQuestionTimeout() {
        synchronized (this) {
            if (state != GameState.IN_PROGRESS) {
                return;
            }
            broadcastScores();
            sendNextQuestion();
        }
    }

    private void persistScores(List<Map.Entry<String, Integer>> ranking) {
        if (ranking.isEmpty()) {
            return;
        }

        int bestScore = ranking.stream().map(Map.Entry::getValue).max(Comparator.naturalOrder()).orElse(0);
        PlayerDAO playerDAO = new PlayerDAO();

        String insertScoreSql = """
            INSERT INTO scores(player_id, game_room, score, correct_answers, total_questions)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection connection = DatabaseManager.getInstance().getConnection();
             PreparedStatement insertStatement = connection.prepareStatement(insertScoreSql)) {

            for (ClientHandler player : players) {
                int playerId = player.getPlayerId();
                if (playerId <= 0) {
                    continue;
                }

                String username = player.getUsernameSafe();
                int score = scores.getOrDefault(username, 0);
                int goodAnswers = correctAnswers.getOrDefault(username, 0);

                insertStatement.setInt(1, playerId);
                insertStatement.setString(2, roomName);
                insertStatement.setInt(3, score);
                insertStatement.setInt(4, goodAnswers);
                insertStatement.setInt(5, questions.size());
                insertStatement.addBatch();

                boolean won = score == bestScore;
                playerDAO.updateStats(playerId, score, won);
            }
            insertStatement.executeBatch();
        } catch (SQLException e) {
            ServerLogger.error("Erreur lors de la sauvegarde des scores pour la room " + roomName, e);
        }
    }

    private void broadcast(Message message) {
        for (ClientHandler player : players) {
            player.sendMessage(message);
        }
    }

    private Set<String> getPlayerUsernames() {
        Set<String> usernames = ConcurrentHashMap.newKeySet();
        for (ClientHandler player : players) {
            usernames.add(player.getUsernameSafe());
        }
        return usernames;
    }

    private JsonObject playerJoinedPayload(String username) {
        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        return payload;
    }

    private JsonObject errorPayload(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        return payload;
    }

    private void cancelTimeoutTask() {
        if (questionTimeoutTask != null && !questionTimeoutTask.isDone()) {
            questionTimeoutTask.cancel(true);
        }
    }

    public synchronized List<String> getPlayersUsernames() {
        List<String> usernames = new ArrayList<>();
        for (ClientHandler player : players) {
            usernames.add(player.getUsernameSafe());
        }
        return usernames;
    }
}
