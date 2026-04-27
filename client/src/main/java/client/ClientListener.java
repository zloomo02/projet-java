package client;

import protocol.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ClientListener implements Runnable {
    private final ObjectInputStream inputStream;
    private final AtomicReference<Consumer<Message>> onMessageRef;
    private volatile boolean running;

    public ClientListener(ObjectInputStream inputStream, Consumer<Message> onMessage) {
        this.inputStream = inputStream;
        this.onMessageRef = new AtomicReference<>(onMessage);
        this.running = true;
    }

    /**
     * Ecoute continue des messages serveur sur le flux objet.
     */
    @Override
    public void run() {
        try {
            while (running) {
                Object object = inputStream.readObject();
                if (object instanceof Message message) {
                    Consumer<Message> callback = onMessageRef.get();
                    if (callback != null) {
                        callback.accept(message);
                    }
                }
            }
        } catch (EOFException | SocketException ignored) {
            // Deconnexion normale.
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[ClientListener] Erreur de lecture: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }

    public void setMessageHandler(Consumer<Message> onMessage) {
        onMessageRef.set(onMessage);
    }
}
