package database;

import model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO {
    private final DatabaseManager databaseManager;

    public QuestionDAO() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    /**
     * Retourne un nombre aléatoire de questions depuis la base.
     */
    public List<Question> getRandomQuestions(int count) {
        String sql = "SELECT * FROM questions ORDER BY RANDOM() LIMIT ?";
        List<Question> questions = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, count);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapQuestion(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de la recuperation aleatoire des questions", e);
        }

        return questions;
    }

    /**
     * Retourne les questions filtrées par catégorie.
     */
    public List<Question> getByCategory(String category) {
        String sql = "SELECT * FROM questions WHERE category = ?";
        List<Question> questions = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapQuestion(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de la recuperation des questions par categorie", e);
        }

        return questions;
    }

    /**
     * Ajoute une nouvelle question en base.
     */
    public void addQuestion(Question q) {
        String sql = """
            INSERT INTO questions(question_text, option_a, option_b, option_c, option_d, correct_option, category, difficulty)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, q.getQuestionText());
            statement.setString(2, q.getOptionA());
            statement.setString(3, q.getOptionB());
            statement.setString(4, q.getOptionC());
            statement.setString(5, q.getOptionD());
            statement.setString(6, q.getCorrectOption());
            statement.setString(7, q.getCategory());
            statement.setString(8, q.getDifficulty());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de l'ajout d'une question", e);
        }
    }

    private Question mapQuestion(ResultSet rs) throws SQLException {
        return new Question(
            rs.getInt("id"),
            rs.getString("question_text"),
            rs.getString("option_a"),
            rs.getString("option_b"),
            rs.getString("option_c"),
            rs.getString("option_d"),
            rs.getString("correct_option"),
            rs.getString("category"),
            rs.getString("difficulty")
        );
    }
}
