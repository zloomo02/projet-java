package protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private String type;
    private String data;
    private String sender;

    public Message() {
    }

    public Message(String type, String data, String sender) {
        this.type = type;
        this.data = data;
        this.sender = sender;
    }

    /**
     * Fabrique un message à partir d'un objet Java qui sera sérialisé en JSON.
     */
    public static Message of(String type, Object payload, String sender) {
        String json = payload == null ? "{}" : GSON.toJson(payload);
        return new Message(type, json, sender);
    }

    /**
     * Convertit le champ data JSON vers une classe cible.
     */
    public <T> T getDataAs(Class<T> clazz) {
        if (data == null || data.isBlank()) {
            return null;
        }
        return GSON.fromJson(data, clazz);
    }

    /**
     * Convertit le champ data JSON vers JsonObject pour un accès dynamique.
     */
    public JsonObject getDataAsJsonObject() {
        if (data == null || data.isBlank()) {
            return new JsonObject();
        }
        return GSON.fromJson(data, JsonObject.class);
    }

    /**
     * Sérialise le message complet en JSON pour transport texte.
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Désérialise un JSON texte en instance Message.
     */
    public static Message fromJson(String json) {
        return GSON.fromJson(json, Message.class);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
