package servlet.resurrection;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Owntage on 11/26/2016.
 */
public class MoveUpdate implements Update {
    volatile float positionX;
    volatile float positionY;
    volatile float velocityX;
    volatile float velocityY;
    volatile int id;

    public MoveUpdate(float positionX, float positionY, float velocityX, float velocityY, int id) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.id = id;
    }

    @Override
    public JSONObject getJsonObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put(ResurrectionConstants.Json.POSITION_X, positionX);
        result.put(ResurrectionConstants.Json.POSITION_Y, positionY);
        result.put(ResurrectionConstants.Json.VELOCITY_X, velocityX);
        result.put(ResurrectionConstants.Json.VELOCITY_Y, velocityY);
        result.put(ResurrectionConstants.Json.PLAYER_ID, id);
        result.put(ResurrectionConstants.Json.TYPE, ResurrectionConstants.Updates.MOVE_UPDATE);
        return result;
    }
}
