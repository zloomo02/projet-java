package client;

import com.google.gson.JsonObject;
import protocol.Message;

import java.io.IOException;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        ServerConnection connection = new ServerConnection("127.0.0.1", 5000);

        try (Scanner scanner = new Scanner(System.in)) {
            connection.connect(message -> {
                System.out.println("[SERVER] type=" + message.getType() + " sender=" + message.getSender() + " data=" + message.getData());
            });

            System.out.println("Client connecte. Commandes:");
            System.out.println("  login <username> <password>");
            System.out.println("  register <username> <password>");
            System.out.println("  create <roomName> [maxPlayers]");
            System.out.println("  join <roomName>");
            System.out.println("  ready");
            System.out.println("  answer <A|B|C|D>");
            System.out.println("  quit");

            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase();

                switch (cmd) {
                    case "login" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: login <username> <password>");
                            continue;
                        }
                        JsonObject payload = new JsonObject();
                        payload.addProperty("username", parts[1]);
                        payload.addProperty("password", parts[2]);
                        connection.sendMessage(Message.of("LOGIN", payload, parts[1]));
                    }
                    case "register" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: register <username> <password>");
                            continue;
                        }
                        JsonObject payload = new JsonObject();
                        payload.addProperty("username", parts[1]);
                        payload.addProperty("password", parts[2]);
                        connection.sendMessage(Message.of("REGISTER", payload, parts[1]));
                    }
                    case "create" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: create <roomName> [maxPlayers]");
                            continue;
                        }
                        int maxPlayers = parts.length >= 3 ? Integer.parseInt(parts[2]) : 4;
                        JsonObject payload = new JsonObject();
                        payload.addProperty("roomName", parts[1]);
                        payload.addProperty("maxPlayers", maxPlayers);
                        connection.sendMessage(Message.of("CREATE_ROOM", payload, "client"));
                    }
                    case "join" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: join <roomName>");
                            continue;
                        }
                        JsonObject payload = new JsonObject();
                        payload.addProperty("roomName", parts[1]);
                        connection.sendMessage(Message.of("JOIN_ROOM", payload, "client"));
                    }
                    case "ready" -> connection.sendMessage(Message.of("READY", new JsonObject(), "client"));
                    case "answer" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: answer <A|B|C|D>");
                            continue;
                        }
                        JsonObject payload = new JsonObject();
                        payload.addProperty("answer", parts[1].toUpperCase());
                        connection.sendMessage(Message.of("ANSWER", payload, "client"));
                    }
                    case "quit" -> {
                        connection.sendMessage(Message.of("QUIT", new JsonObject(), "client"));
                        connection.disconnect();
                        return;
                    }
                    default -> System.out.println("Commande inconnue.");
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur client: " + e.getMessage());
        } finally {
            connection.disconnect();
        }
    }
}
