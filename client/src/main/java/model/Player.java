package model;

import java.time.LocalDateTime;

public class Player {
    private int id;
    private String username;
    private String password;
    private int totalGames;
    private int totalWins;
    private int bestScore;
    private LocalDateTime createdAt;

    public Player() {
    }

    public Player(int id, String username, String password, int totalGames, int totalWins, int bestScore, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.totalGames = totalGames;
        this.totalWins = totalWins;
        this.bestScore = bestScore;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public int getBestScore() {
        return bestScore;
    }

    public void setBestScore(int bestScore) {
        this.bestScore = bestScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
