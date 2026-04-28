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
import java.util.Locale;

public class ProtocolTestClient {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("[ProtocolTestClient] Connexion a " + HOST + ":" + PORT);

        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            System.out.println("Commands: login | register | quit | help | exit");
            String currentSender = "client";

            while (true) {
                System.out.print("> ");
                String line = console.readLine();
                if (line == null) {
                    break;
                }

                String cmd = line.trim().toLowerCase(Locale.ROOT);
                if (cmd.isBlank()) {
                    continue;
                }

                switch (cmd) {
                    case "help" -> System.out.println("Commands: login | register | quit | help | exit");
                    case "exit" -> {
                        return;
                    }
                    case "login" -> {
                        String username = prompt(console, "Username");
                        String password = prompt(console, "Password");
                        currentSender = username;
                        Message login = Message.of("LOGIN", credentials(username, password), username);
                        sendAndPrint(writer, reader, login, "LOGIN");
                    }
                    case "register" -> {
                        String username = prompt(console, "Username");
                        String password = prompt(console, "Password");
                        Message register = Message.of("REGISTER", credentials(username, password), username);
                        sendAndPrint(writer, reader, register, "REGISTER");
                    }
                    case "quit" -> {
                        Message quit = Message.of("QUIT", new JsonObject(), currentSender);
                        sendAndPrint(writer, reader, quit, "QUIT");
                    }
                    default -> System.out.println("Unknown command. Type 'help'.");
                }
            }
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

    private static String prompt(BufferedReader console, String label) throws IOException {
        System.out.print(label + ": ");
        String value = console.readLine();
        return value == null ? "" : value.trim();
    }
}
