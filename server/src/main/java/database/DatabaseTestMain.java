package database;

import model.Player;
import model.Question;

import java.util.List;

public class DatabaseTestMain {
    public static void main(String[] args) {
        DatabaseManager.getInstance();

        PlayerDAO playerDAO = new PlayerDAO();
        QuestionDAO questionDAO = new QuestionDAO();

        String username = "test_user";
        String password = "test123";

        // Test inscription (peut retourner false si l'utilisateur existe deja).
        boolean registered = playerDAO.register(username, password);
        System.out.println("Inscription reussie: " + registered);

        // Test login.
        Player player = playerDAO.login(username, password);
        if (player == null) {
            System.out.println("Echec login");
            return;
        }
        System.out.println("Login OK pour: " + player.getUsername() + " (id=" + player.getId() + ")");

        // Test lecture aleatoire des questions.
        List<Question> randomQuestions = questionDAO.getRandomQuestions(3);
        System.out.println("Questions aleatoires recuperees: " + randomQuestions.size());
        for (Question q : randomQuestions) {
            System.out.println("- [" + q.getId() + "] " + q.getQuestionText() + " (" + q.getCategory() + ")");
        }

        // Test mise a jour stats.
        playerDAO.updateStats(player.getId(), 250, true);
        System.out.println("Stats mises a jour");

        // Test leaderboard.
        List<Player> topPlayers = playerDAO.getLeaderboard();
        System.out.println("Top joueurs (max 10): " + topPlayers.size());
    }
}
