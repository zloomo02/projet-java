package ui;

import client.ServerConnection;
import com.google.gson.JsonObject;
import protocol.Message;

import java.io.IOException;
import java.util.function.Consumer;

public class AppState {
    private static final AppState INSTANCE = new AppState();

    private final ServerConnection serverConnection;
    private volatile String username;
    private volatile int playerId;

    private AppState() {
        this.serverConnection = new ServerConnection("127.0.0.1", 5000);
        this.username = "";
        this.playerId = -1;
    }

    public static AppState getInstance() {
        return INSTANCE;
    }

    /**
     * Ouvre une connexion unique au serveur et associe un callback de messages.
     */
    public synchronized void connect(Consumer<Message> onMessage) throws IOException {
        serverConnection.connect(onMessage);
    }

    /**
     * Permet de changer dynamiquement le callback actif selon l'ecran.
     */
    public void setMessageHandler(Consumer<Message> onMessage) {
        serverConnection.setMessageHandler(onMessage);
    }

    /**
     * Envoie un message protocole au serveur.
     */
    public void send(Message message) throws IOException {
        serverConnection.sendMessage(message);
    }

    public void disconnect() {
        try {
            JsonObject quitPayload = new JsonObject();
            send(Message.of("QUIT", quitPayload, username));
        } catch (IOException ignored) {
            // Ignore pour une fermeture best-effort.
        }
        serverConnection.disconnect();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }
}
