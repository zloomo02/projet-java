package server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import protocol.Message;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private static final GameManager INSTANCE = new GameManager();

    private final Map<String, GameSession> sessions;
    private final Set<ClientHandler> connectedClients;
    private final Map<String, ClientHandler> activeUsers;

    private GameManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.connectedClients = ConcurrentHashMap.newKeySet();
        this.activeUsers = new ConcurrentHashMap<>();
    }

    public static GameManager getInstance() {
        return INSTANCE;
    }

    public void registerClient(ClientHandler clientHandler) {
        connectedClients.add(clientHandler);
    }

    public void unregisterClient(ClientHandler clientHandler) {
        connectedClients.remove(clientHandler);
    }

    /**
     * Enregistre un username actif pour empecher les connexions multiples.
     */
    public boolean tryRegisterUsername(String username, ClientHandler handler) {
        if (username == null || username.isBlank() || handler == null) {
            return false;
        }

        ClientHandler existing = activeUsers.putIfAbsent(username, handler);
        return existing == null || existing == handler;
    }

    /**
     * Retire un username actif si le handler correspond.
     */
    public void unregisterUsername(String username, ClientHandler handler) {
        if (username == null || username.isBlank() || handler == null) {
            return;
        }
        activeUsers.remove(username, handler);
    }

    /**
     * Cree une room si son nom est libre, puis y ajoute le createur.
     */
    public synchronized GameSession createRoom(String roomName, int maxPlayers, ClientHandler owner) {
        if (roomName == null || roomName.isBlank() || sessions.containsKey(roomName)) {
            return null;
        }

        GameSession session = new GameSession(roomName, maxPlayers);
        if (!session.addPlayer(owner)) {
            return null;
        }
        sessions.put(roomName, session);
        return session;
    }

    /**
     * Ajoute un joueur dans une room existante.
     */
    public synchronized GameSession joinRoom(String roomName, ClientHandler player) {
        GameSession session = sessions.get(roomName);
        if (session == null) {
            return null;
        }

        if (!session.addPlayer(player)) {
            return null;
        }
        return session;
    }

    /**
     * Supprime une room, par exemple en fin de partie.
     */
    public void removeSession(String roomName) {
        sessions.remove(roomName);
        broadcastRoomList();
    }

    /**
     * Retire un joueur de sa room courante et supprime la room si vide.
     */
    public void removePlayerFromSession(ClientHandler player) {
        for (GameSession session : sessions.values()) {
            if (session.removePlayer(player)) {
                if (session.isEmpty()) {
                    sessions.remove(session.getRoomName());
                    ServerLogger.info("Room supprimee car vide: " + session.getRoomName());
                }
                broadcastRoomList();
                return;
            }
        }
    }

    /**
     * Construit le payload ROOM_LIST selon le protocole attendu.
     */
    public JsonObject buildRoomListPayload() {
        JsonArray roomsArray = new JsonArray();

        for (GameSession session : sessions.values()) {
            JsonObject room = new JsonObject();
            room.addProperty("name", session.getRoomName());
            room.addProperty("players", session.getPlayerCount());
            room.addProperty("max", session.getMaxPlayers());
            roomsArray.add(room);
        }

        JsonObject payload = new JsonObject();
        payload.add("rooms", roomsArray);
        return payload;
    }

    /**
     * Diffuse la liste des rooms a tous les clients connectes.
     */
    public void broadcastRoomList() {
        Message message = Message.of("ROOM_LIST", buildRoomListPayload(), "SERVER");
        for (ClientHandler clientHandler : connectedClients) {
            clientHandler.sendMessage(message);
        }
    }
}
