package servlet.resurrection;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Owntage on 11/26/2016.
 */
public class HealthUpdate implements Update {
    final int id;
    final int health;

    public HealthUpdate(int id, int health) {
        this.id = id;
        this.health = health;
    }

    @Override
    public JSONObject getJsonObject() throws JSONException {
        JSONObject res = new JSONObject();
        res.put(ResurrectionConstants.Json.POSITION_X, health);
        res.put(ResurrectionConstants.Json.POSITION_Y, 0);
        res.put(ResurrectionConstants.Json.VELOCITY_X, 0);
        res.put(ResurrectionConstants.Json.VELOCITY_Y, 0);
        res.put(ResurrectionConstants.Json.TYPE, ResurrectionConstants.Updates.HEALTH_UPDATE);
        res.put(ResurrectionConstants.Json.PLAYER_ID, id);
        return res;
    }
}
