package database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:resources/quiz.db";

    private DatabaseManager() {
        initDatabase();
    }

    private static class Holder {
        private static final DatabaseManager INSTANCE = new DatabaseManager();
    }

    public static DatabaseManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Retourne une nouvelle connexion SQLite active.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initialise la base si elle n'existe pas: création des tables et insertion
     * des questions de démarrage.
     */
    public void initDatabase() {
        try {
            Path resourcesPath = Paths.get("resources");
            if (!Files.exists(resourcesPath)) {
                Files.createDirectories(resourcesPath);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de créer le dossier resources", e);
        }

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    total_games INTEGER DEFAULT 0,
                    total_wins INTEGER DEFAULT 0,
                    best_score INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS questions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    question_text TEXT NOT NULL,
                    option_a TEXT NOT NULL,
                    option_b TEXT NOT NULL,
                    option_c TEXT NOT NULL,
                    option_d TEXT NOT NULL,
                    correct_option CHAR(1) NOT NULL,
                    category TEXT DEFAULT 'General',
                    difficulty TEXT DEFAULT 'medium'
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS scores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_id INTEGER NOT NULL,
                    game_room TEXT NOT NULL,
                    score INTEGER DEFAULT 0,
                    correct_answers INTEGER DEFAULT 0,
                    total_questions INTEGER DEFAULT 0,
                    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (player_id) REFERENCES players(id)
                )
                """);

            statement.execute("""
                INSERT OR IGNORE INTO questions
                (id, question_text, option_a, option_b, option_c, option_d, correct_option, category, difficulty)
                VALUES
                (1,'Quelle est la capitale de la France ?','Berlin','Madrid','Paris','Rome','C','Geographie','easy'),
                (2,'Combien font 7 x 8 ?','54','56','48','64','B','Maths','easy'),
                (3,'Qui a peint la Joconde ?','Picasso','Michel-Ange','Da Vinci','Raphael','C','Culture','medium'),
                (4,'Quel langage utilise la JVM ?','Python','Java','C++','Ruby','B','Informatique','easy'),
                (5,'En quelle annee a eu lieu la Revolution francaise ?','1776','1848','1789','1815','C','Histoire','medium')
                """);
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de l'initialisation de la base de donnees", e);
        }
    }

    public static void main(String[] args) {
        DatabaseManager.getInstance();
        System.out.println("Base de donnees initialisee: resources/quiz.db");
    }
}
