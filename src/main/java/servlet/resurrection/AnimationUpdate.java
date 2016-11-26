package servlet.resurrection;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Owntage on 11/26/2016.
 */
public class AnimationUpdate implements Update {
    final int id;

    public AnimationUpdate(int id) {
        this.id = id;
    }

    @Override
    public JSONObject getJsonObject() throws JSONException {
        JSONObject res = new JSONObject();
        res.put(ResurrectionConstants.Json.POSITION_X, 0);
        res.put(ResurrectionConstants.Json.POSITION_Y, 0);
        res.put(ResurrectionConstants.Json.VELOCITY_X, 0);
        res.put(ResurrectionConstants.Json.VELOCITY_Y, 0);
        res.put(ResurrectionConstants.Json.TYPE, ResurrectionConstants.Updates.ANIMATION_UPDATE);
        res.put(ResurrectionConstants.Json.PLAYER_ID, id);
        return res;
    }
}
