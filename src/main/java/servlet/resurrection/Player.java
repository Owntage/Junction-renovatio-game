package servlet.resurrection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.AsyncContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Owntage on 11/26/2016.
 */
public class Player {
    public final Lock lock = new ReentrantLock();
    public final ArrayList<Update> pendingUpdates = new ArrayList<>();
    public volatile AsyncContext asyncContext = null;
    public final int id;

    public Player(int id) {
        this.id = id;
    }

    public void sendUpdate(Update update)
            throws JSONException, IOException {
        if(asyncContext == null) {
            pendingUpdates.add(update);
        } else {
            //if we have an async context, it means, that we don't have any pending updates.
            //because async context disappears, when first update arrives.
            pendingUpdates.add(update);
            JSONObject resp = new JSONObject();
            resp.put(ResurrectionConstants.Json.UPDATE_ARRAY, getPendingUpdatesJsonArray());
            pendingUpdates.clear();
            asyncContext.getResponse().getWriter().write(resp.toString());
            asyncContext.getResponse().getWriter().flush();
            asyncContext.complete();
            asyncContext = null;
        }
    }
    public JSONArray getPendingUpdatesJsonArray() throws JSONException {
        JSONArray res = new JSONArray();
        for(Update update : pendingUpdates) {
            res.put(update.getJsonObject());
        }
        return res;
    }
}