package protocol;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MessageTest {
    @Test
    void shouldSerializeAndDeserializeMessage() {
        JsonObject payload = new JsonObject();
        payload.addProperty("username", "alice");
        payload.addProperty("password", "1234");

        Message message = Message.of("LOGIN", payload, "alice");
        String json = message.toJson();

        Message parsed = Message.fromJson(json);

        assertEquals("LOGIN", parsed.getType());
        assertEquals("alice", parsed.getSender());
        assertNotNull(parsed.getDataAsJsonObject());
        assertEquals("alice", parsed.getDataAsJsonObject().get("username").getAsString());
    }
}
