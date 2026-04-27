package client;

import protocol.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class ServerConnection {
    private final String host;
    private final int port;

    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private ClientListener listener;
    private Thread listenerThread;

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Ouvre la connexion au serveur et demarre le thread d'ecoute.
     */
    public void connect(Consumer<Message> onMessage) throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            setMessageHandler(onMessage);
            return;
        }

        socket = new Socket(host, port);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        inputStream = new ObjectInputStream(socket.getInputStream());

        listener = new ClientListener(inputStream, onMessage);
        listenerThread = new Thread(listener, "client-listener");
        listenerThread.start();
    }

    /**
     * Met a jour le callback de messages sans recreer la connexion reseau.
     */
    public void setMessageHandler(Consumer<Message> onMessage) {
        if (listener != null) {
            listener.setMessageHandler(onMessage);
        }
    }

    /**
     * Envoie un message au serveur via ObjectOutputStream.
     */
    public void sendMessage(Message message) throws IOException {
        if (outputStream == null) {
            throw new IOException("Connexion non initialisee.");
        }

        synchronized (outputStream) {
            outputStream.writeObject(message);
            outputStream.flush();
        }
    }

    /**
     * Ferme proprement la connexion et les ressources associees.
     */
    public void disconnect() {
        if (listener != null) {
            listener.stop();
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[ServerConnection] Erreur fermeture socket: " + e.getMessage());
        }
    }
}
