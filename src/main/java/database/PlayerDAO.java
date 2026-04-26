package database;

import model.Player;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlayerDAO {
    private final DatabaseManager databaseManager;

    public PlayerDAO() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    /**
     * Authentifie un joueur avec son username et mot de passe hashé.
     */
    public Player login(String username, String password) {
        String sql = "SELECT * FROM players WHERE username = ? AND password = ?";
        String hashedPassword = hashPassword(password);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, hashedPassword);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapPlayer(rs);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de la connexion du joueur", e);
        }

        return null;
    }

    /**
     * Inscrit un nouveau joueur en base avec mot de passe hashé.
     */
    public boolean register(String username, String password) {
        String sql = "INSERT INTO players(username, password) VALUES (?, ?)";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, hashPassword(password));
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Met à jour les statistiques globales du joueur après une partie.
     */
    public void updateStats(int playerId, int score, boolean won) {
        String sql = """
            UPDATE players
            SET total_games = total_games + 1,
                total_wins = total_wins + ?,
                best_score = CASE WHEN ? > best_score THEN ? ELSE best_score END
            WHERE id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, won ? 1 : 0);
            statement.setInt(2, score);
            statement.setInt(3, score);
            statement.setInt(4, playerId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de la mise a jour des statistiques", e);
        }
    }

    /**
     * Retourne le top 10 des joueurs ordonné par meilleur score.
     */
    public List<Player> getLeaderboard() {
        String sql = "SELECT * FROM players ORDER BY best_score DESC, total_wins DESC LIMIT 10";
        List<Player> leaderboard = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                leaderboard.add(mapPlayer(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de la recuperation du classement", e);
        }

        return leaderboard;
    }

    private Player mapPlayer(ResultSet rs) throws SQLException {
        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;

        return new Player(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getInt("total_games"),
            rs.getInt("total_wins"),
            rs.getInt("best_score"),
            createdAt
        );
    }

    /**
     * Hash SHA-256 utilisé pour stocker et comparer les mots de passe.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashBytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithme SHA-256 indisponible", e);
        }
    }
}
