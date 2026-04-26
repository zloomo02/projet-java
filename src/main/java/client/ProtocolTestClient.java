package client;

import com.google.gson.JsonObject;
import protocol.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ProtocolTestClient {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("[ProtocolTestClient] Connexion a " + HOST + ":" + PORT);

        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            // 1) Test LOGIN en succes.
            Message loginSuccess = Message.of("LOGIN", credentials("alice", "1234"), "alice");
            sendAndPrint(writer, reader, loginSuccess, "LOGIN succes");

            // 2) Test LOGIN en echec.
            Message loginFail = Message.of("LOGIN", credentials("alice", "bad"), "alice");
            sendAndPrint(writer, reader, loginFail, "LOGIN echec");

            // 3) Test REGISTER.
            Message register = Message.of("REGISTER", credentials("bob", "pass"), "bob");
            sendAndPrint(writer, reader, register, "REGISTER");

            // 4) Fin de session.
            Message quit = Message.of("QUIT", new JsonObject(), "alice");
            sendAndPrint(writer, reader, quit, "QUIT");

        } catch (IOException e) {
            System.err.println("[ProtocolTestClient] Erreur reseau: " + e.getMessage());
        }
    }

    /**
     * Envoie un message JSON et affiche la reponse brute + decodee.
     */
    private static void sendAndPrint(PrintWriter writer, BufferedReader reader, Message message, String label) throws IOException {
        writer.println(message.toJson());
        String responseJson = reader.readLine();
        Message response = Message.fromJson(responseJson);

        System.out.println("=== " + label + " ===");
        System.out.println("Reponse JSON: " + responseJson);
        System.out.println("Type: " + response.getType() + " | Sender: " + response.getSender() + " | Data: " + response.getData());
    }

    private static JsonObject credentials(String username, String password) {
        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.addProperty("password", password);
        return payload;
    }
}
