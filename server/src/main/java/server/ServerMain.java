package server;

import database.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        DatabaseManager.getInstance();
        ServerLogger.info("Serveur Quiz demarre sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ServerLogger.info("Connexion entrante depuis " + clientSocket.getRemoteSocketAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(clientHandler, "client-handler-" + clientSocket.getPort());
                clientThread.start();
            }
        } catch (IOException e) {
            ServerLogger.error("Arret du serveur suite a une erreur reseau", e);
        }
    }
}
