package server;

import com.google.gson.JsonObject;
import protocol.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ProtocolTestServer {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("[ProtocolTestServer] Demarrage sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[ProtocolTestServer] En attente d'un client...");

            try (Socket socket = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                System.out.println("[ProtocolTestServer] Client connecte: " + socket.getInetAddress());

                String line;
                while ((line = reader.readLine()) != null) {
                    Message request = Message.fromJson(line);
                    Message response = handleMessage(request);
                    writer.println(response.toJson());

                    if ("QUIT".equalsIgnoreCase(request.getType())) {
                        System.out.println("[ProtocolTestServer] Fin de session demandee par le client.");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ProtocolTestServer] Erreur reseau: " + e.getMessage());
        }
    }

    /**
     * Simule un petit routeur de messages pour valider le protocole JSON.
     */
    private static Message handleMessage(Message request) {
        if (request == null || request.getType() == null) {
            return Message.of("ERROR", errorPayload("Message invalide"), "SERVER");
        }

        return switch (request.getType()) {
            case "LOGIN" -> handleLogin(request);
            case "REGISTER" -> Message.of("REGISTER_OK", okPayload("Inscription acceptee (test)"), "SERVER");
            case "QUIT" -> Message.of("BYE", okPayload("Deconnexion"), "SERVER");
            default -> Message.of("ERROR", errorPayload("Type inconnu: " + request.getType()), "SERVER");
        };
    }

    /**
     * Simule une authentification basique pour test de bout en bout.
     */
    private static Message handleLogin(Message request) {
        JsonObject data = request.getDataAsJsonObject();
        String username = data.has("username") ? data.get("username").getAsString() : "";
        String password = data.has("password") ? data.get("password").getAsString() : "";

        if ("alice".equals(username) && "1234".equals(password)) {
            JsonObject payload = new JsonObject();
            payload.addProperty("playerId", 1);
            payload.addProperty("username", username);
            return Message.of("LOGIN_OK", payload, "SERVER");
        }

        return Message.of("LOGIN_FAIL", errorPayload("Mot de passe incorrect"), "SERVER");
    }

    private static JsonObject okPayload(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        return payload;
    }

    private static JsonObject errorPayload(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("reason", message);
        payload.addProperty("message", message);
        return payload;
    }
}
