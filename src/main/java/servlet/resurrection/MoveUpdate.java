package servlet.resurrection;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Owntage on 11/26/2016.
 */
public class MoveUpdate implements Update {
    public static final long TICK_DELTA = 500; //in milliseconds
    volatile float positionX;
    volatile float positionY;
    volatile float velocityX;
    volatile float velocityY;
    volatile int id;
    final int creationTime;

    public MoveUpdate(float positionX, float positionY, float velocityX, float velocityY, int id, int creationTime) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.id = id;
        this.creationTime = creationTime;
    }

    public MoveUpdate(JSONObject obj, int creationTime) throws JSONException {
        positionX = Float.parseFloat(obj.getString(ResurrectionConstants.Json.POSITION_X));
        positionY = Float.parseFloat(obj.getString(ResurrectionConstants.Json.POSITION_Y));
        velocityX = Float.parseFloat(obj.getString(ResurrectionConstants.Json.VELOCITY_X));
        velocityY = Float.parseFloat(obj.getString(ResurrectionConstants.Json.VELOCITY_Y));
        id = Integer.parseInt(obj.getString(ResurrectionConstants.Json.PLAYER_ID));
        this.creationTime = creationTime;
    }

    public MoveUpdate(MoveUpdate other, int currentTime) {
        float tickDelta = (float) TICK_DELTA / 1000.0f;
        float dt = (currentTime - other.creationTime) * tickDelta;
        positionX = other.positionX + other.velocityX * dt;
        positionY = other.positionY + other.velocityY * dt;
        velocityX = other.velocityX;
        velocityY = other.velocityY;
        creationTime = currentTime;
        id = other.id;
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
