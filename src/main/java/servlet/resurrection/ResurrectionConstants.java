package servlet.resurrection;

/**
 * Created by Owntage on 11/26/2016.
 */
public interface ResurrectionConstants {
    interface Json {
        String PLAYER_ID = "id";
        String TYPE = "type";
        String VELOCITY_X = "velX";
        String VELOCITY_Y = "velY";
        String POSITION_X = "x";
        String POSITION_Y = "y";
        String UPDATE_ARRAY = "updateArray";

    }
    interface Requests {
        String CONNECT = "Connection";
        String UPDATE_REQUEST = "Update";
        String DIRECTION_CHANGE = "SelfUpdate";
    }
    interface Updates {
        String MOVE_UPDATE = "update";
        String DESTROY_UPDATE = "destroy";
    }
}
