package server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import database.PlayerDAO;
import model.Player;
import protocol.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameManager gameManager;
    private final PlayerDAO playerDAO;

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    private volatile boolean connected;
    private volatile String username;
    private volatile int playerId;
    private volatile GameSession currentSession;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.gameManager = GameManager.getInstance();
        this.playerDAO = new PlayerDAO();
        this.connected = true;
        this.username = "anonymous-" + socket.getPort();
        this.playerId = -1;
    }

    @Override
    public void run() {
        try (socket;
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            this.outputStream = out;
            this.inputStream = in;
            out.flush();

            gameManager.registerClient(this);
            sendRoomList();

            while (connected) {
                Object object = in.readObject();
                if (!(object instanceof Message message)) {
                    sendError("Format de message invalide.");
                    continue;
                }
                dispatch(message);
            }
        } catch (EOFException | SocketException e) {
            ServerLogger.info("Client deconnecte: " + socket.getRemoteSocketAddress());
        } catch (IOException | ClassNotFoundException e) {
            ServerLogger.error("Erreur de communication avec le client " + socket.getRemoteSocketAddress(), e);
        } finally {
            disconnect();
        }
    }

    /**
     * Route un message recu vers la methode metier associee.
     */
    private void dispatch(Message message) {
        if (message.getType() == null) {
            sendError("Type de message manquant.");
            return;
        }

        switch (message.getType()) {
            case "LOGIN" -> handleLogin(message);
            case "REGISTER" -> handleRegister(message);
            case "CREATE_ROOM" -> handleCreateRoom(message);
            case "JOIN_ROOM" -> handleJoinRoom(message);
            case "ANSWER" -> handleAnswer(message);
            case "READY" -> handleReady();
            case "QUIT" -> disconnect();
            default -> sendError("Type de message inconnu: " + message.getType());
        }
    }

    /**
     * Envoie un message au client de facon thread-safe.
     */
    public void sendMessage(Message message) {
        ObjectOutputStream out = this.outputStream;
        if (out == null || !connected) {
            return;
        }

        synchronized (out) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                ServerLogger.warn("Impossible d'envoyer un message a " + getUsernameSafe());
                disconnect();
            }
        }
    }

    /**
     * Traite l'authentification utilisateur via PlayerDAO.
     */
    private void handleLogin(Message message) {
        JsonObject payload = message.getDataAsJsonObject();
        String providedUsername = getAsString(payload, "username");
        String password = getAsString(payload, "password");

        if (providedUsername.isBlank() || password.isBlank()) {
            sendMessage(Message.of("LOGIN_FAIL", reasonPayload("Champs login invalides"), "SERVER"));
            return;
        }

        Player player = playerDAO.login(providedUsername, password);
        if (player == null) {
            sendMessage(Message.of("LOGIN_FAIL", reasonPayload("Mot de passe incorrect"), "SERVER"));
            return;
        }

        // Prevent the same account from logging in multiple times.
        if (!gameManager.tryRegisterUsername(player.getUsername(), this)) {
            sendMessage(Message.of("LOGIN_FAIL", reasonPayload("Utilisateur deja connecte"), "SERVER"));
            return;
        }

        this.username = player.getUsername();
        this.playerId = player.getId();

        JsonObject response = new JsonObject();
        response.addProperty("playerId", this.playerId);
        response.addProperty("username", this.username);
        sendMessage(Message.of("LOGIN_OK", response, "SERVER"));
        sendRoomList();

        ServerLogger.info("LOGIN_OK pour " + this.username);
    }

    /**
     * Traite l'inscription utilisateur via PlayerDAO.
     */
    private void handleRegister(Message message) {
        JsonObject payload = message.getDataAsJsonObject();
        String providedUsername = getAsString(payload, "username");
        String password = getAsString(payload, "password");

        if (providedUsername.isBlank() || password.isBlank()) {
            sendError("Champs inscription invalides.");
            return;
        }

        boolean registered = playerDAO.register(providedUsername, password);
        if (!registered) {
            sendError("Inscription refusee (username peut-etre deja pris).");
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("message", "Inscription reussie");
        sendMessage(Message.of("REGISTER_OK", response, "SERVER"));
    }

    /**
     * Cree une room et notifie le client createur.
     */
    private void handleCreateRoom(Message message) {
        if (!ensureAuthenticated()) {
            return;
        }

        if (isInActiveSession()) {
            sendError("Vous etes deja dans une room active.");
            return;
        }

        JsonObject payload = message.getDataAsJsonObject();
        String roomName = getAsString(payload, "roomName");
        int maxPlayers = payload.has("maxPlayers") ? payload.get("maxPlayers").getAsInt() : 4;

        if (roomName.isBlank()) {
            sendError("Nom de room invalide.");
            return;
        }

        if (maxPlayers < 2 || maxPlayers > 8) {
            sendError("maxPlayers doit etre entre 2 et 8.");
            return;
        }

        GameSession session = gameManager.createRoom(roomName, maxPlayers, this);
        if (session == null) {
            sendError("Impossible de creer la room.");
            return;
        }

        this.currentSession = session;
        sendMessage(Message.of("ROOM_JOINED", roomJoinedPayload(session), "SERVER"));
        broadcastRoomListToAllPlayers();

        ServerLogger.info(getUsernameSafe() + " a cree la room " + roomName);
    }

    /**
     * Ajoute le client dans une room existante.
     */
    private void handleJoinRoom(Message message) {
        if (!ensureAuthenticated()) {
            return;
        }

        if (isInActiveSession()) {
            sendError("Vous etes deja dans une room active.");
            return;
        }

        JsonObject payload = message.getDataAsJsonObject();
        String roomName = getAsString(payload, "roomName");

        if (roomName.isBlank()) {
            sendError("Nom de room invalide.");
            return;
        }

        GameSession session = gameManager.joinRoom(roomName, this);
        if (session == null) {
            sendError("Room introuvable ou pleine.");
            return;
        }

        this.currentSession = session;
        sendMessage(Message.of("ROOM_JOINED", roomJoinedPayload(session), "SERVER"));
        broadcastRoomListToAllPlayers();

        ServerLogger.info(getUsernameSafe() + " a rejoint la room " + roomName);
    }

    /**
     * Traite la reponse d'un joueur pendant la partie.
     */
    private void handleAnswer(Message message) {
        if (!isInActiveSession()) {
            sendError("Vous n'etes dans aucune room.");
            return;
        }

        JsonObject payload = message.getDataAsJsonObject();
        String answer = getAsString(payload, "answer");
        if (answer.isBlank()) {
            sendError("Reponse vide.");
            return;
        }

        currentSession.receiveAnswer(this, answer);
    }

    /**
     * Marque le joueur pret pour commencer la partie.
     */
    private void handleReady() {
        if (!isInActiveSession()) {
            sendError("Vous n'etes dans aucune room.");
            return;
        }
        currentSession.markReady(this);
    }

    /**
     * Ferme proprement la connexion et retire le joueur des structures serveur.
     */
    public void disconnect() {
        if (!connected) {
            return;
        }

        connected = false;
        gameManager.unregisterUsername(username, this);
        gameManager.removePlayerFromSession(this);
        gameManager.unregisterClient(this);

        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            ServerLogger.warn("Erreur de fermeture socket pour " + getUsernameSafe());
        }

        ServerLogger.info("Deconnexion du client " + getUsernameSafe());
    }

    /**
     * Nettoie la reference de session cote client handler quand une partie se termine.
     */
    void clearSession(GameSession session) {
        if (this.currentSession == session) {
            this.currentSession = null;
        }
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getUsernameSafe() {
        return username == null || username.isBlank() ? "inconnu" : username;
    }

    private void sendRoomList() {
        sendMessage(Message.of("ROOM_LIST", gameManager.buildRoomListPayload(), "SERVER"));
    }

    private void broadcastRoomListToAllPlayers() {
        gameManager.broadcastRoomList();
    }

    private JsonObject roomJoinedPayload(GameSession session) {
        JsonObject payload = new JsonObject();
        payload.addProperty("roomName", session.getRoomName());

        JsonArray playersArray = new JsonArray();
        for (String playerName : session.getPlayersUsernames()) {
            playersArray.add(playerName);
        }
        payload.add("players", playersArray);
        return payload;
    }

    private JsonObject reasonPayload(String reason) {
        JsonObject payload = new JsonObject();
        payload.addProperty("reason", reason);
        return payload;
    }

    private void sendError(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        sendMessage(Message.of("ERROR", payload, "SERVER"));
    }

    private String getAsString(JsonObject payload, String key) {
        return payload != null && payload.has(key) ? payload.get(key).getAsString().trim() : "";
    }

    private boolean ensureAuthenticated() {
        if (playerId <= 0) {
            sendError("Authentification requise.");
            return false;
        }
        return true;
    }

    private boolean isInActiveSession() {
        return currentSession != null && currentSession.getState() != GameSession.GameState.FINISHED;
    }
}
