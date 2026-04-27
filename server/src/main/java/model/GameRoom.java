package model;

import java.util.ArrayList;
import java.util.List;

public class GameRoom {
    private String name;
    private int maxPlayers;
    private final List<String> players;
    private String state;

    public GameRoom() {
        this.players = new ArrayList<>();
    }

    public GameRoom(String name, int maxPlayers, List<String> players, String state) {
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.players = new ArrayList<>(players);
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public List<String> getPlayers() {
        return players;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean addPlayer(String username) {
        if (players.size() >= maxPlayers || players.contains(username)) {
            return false;
        }
        players.add(username);
        return true;
    }

    public boolean removePlayer(String username) {
        return players.remove(username);
    }
}
