package servlet.resurrection;

import org.json.JSONException;
import org.json.JSONObject;
import servlet.mafia.MafiaException;

import java.io.IOException;
import java.io.Reader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(
        name = "ResurrectionServlet",
        asyncSupported = true,
        urlPatterns = {"/resurrection-game"}
)
public class ResurrectionServlet extends HttpServlet {

    final AtomicInteger playerCounter = new AtomicInteger(1);
    final ConcurrentHashMap<Integer, Player>  players = new ConcurrentHashMap<>();
    final AtomicInteger time = new AtomicInteger();
    final Timer timer = new Timer();

    final long SECOND = 1000;
    final long TIMEOUT_IN_SECONDS = 10; //todo should be replaced with some small value
    final String EMPTY_UPDATE_ARRAY = "{\"" + ResurrectionConstants.Json.UPDATE_ARRAY + "\":[]}";

    @Override
    public void init() throws ServletException {
       timer.schedule(new TimerTask(){

           @Override
           public void run() {
               time.getAndIncrement();
           }
       }, MoveUpdate.TICK_DELTA, MoveUpdate.TICK_DELTA);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        String body = readBody(req);
        try {

            JSONObject jsonObject = new JSONObject(body);
            String type = jsonObject.getString(ResurrectionConstants.Json.TYPE);
            switch(type) {
                case ResurrectionConstants.Requests.CONNECT: {
                    onConnectionRequest(req, resp, jsonObject);
                    break;
                }
                case ResurrectionConstants.Requests.UPDATE_REQUEST: {
                    onGetUpdateRequest(req, resp, jsonObject);
                    break;
                }
                case ResurrectionConstants.Requests.DIRECTION_CHANGE: {
                    onDirectionChangeRequest(req, resp, jsonObject);
                    break;
                }

                default : {
                    sendErrorResponse(resp, "no such type of request. your json: " + jsonObject.toString());
                    break;
                }

            }
        } catch (JSONException e) {
            sendErrorResponse(resp,"request text: " + body + " error message: " + e.getMessage());
        }
    }

    private void onConnectionRequest(final HttpServletRequest req, final HttpServletResponse resp, JSONObject jsonObj)
            throws IOException, JSONException {
        System.out.println("connection request");
        JSONObject respJson = new JSONObject();
        Player player = new Player(playerCounter.getAndIncrement());
        for(Player otherPlayer : players.values()) {
            if(otherPlayer.lastMoveUpdate != null) {
                player.pendingUpdates.add(new MoveUpdate(otherPlayer.lastMoveUpdate, time.get()));
            }
        }
        players.put(player.id, player);
        respJson.put(ResurrectionConstants.Json.PLAYER_ID, player.id);
        resp.getWriter().write(respJson.toString());
        resp.getWriter().flush();
    }

    private void onDirectionChangeRequest(final HttpServletRequest req, final HttpServletResponse resp, JSONObject jsonObj)
            throws IOException, JSONException {
        System.out.println("directionChangeRequest: " + jsonObj.toString());
        //todo send this update to all players.
        MoveUpdate moveUpdate = new MoveUpdate(jsonObj, time.get());
        Player thisPlayer = getLockedPlayer(moveUpdate.id);
        if(thisPlayer == null) return;
        thisPlayer.lastMoveUpdate = moveUpdate;

        thisPlayer.lock.unlock();
        for(Player player : players.values()) {
            if(player.id != moveUpdate.id) {
                player.lock.lock();
                player.sendUpdate(moveUpdate);
                player.lock.unlock();
            }
        }
    }

    private void onGetUpdateRequest(final HttpServletRequest req, final HttpServletResponse resp, JSONObject jsonObj)
            throws IOException, JSONException {
        String id = jsonObj.getString(ResurrectionConstants.Json.PLAYER_ID);
        final Player player = getLockedPlayer(Integer.parseInt(id));
        if(player == null) return;
        if(player.pendingUpdates.size() > 0) {
            JSONObject respJson = new JSONObject();
            respJson.put(ResurrectionConstants.Json.UPDATE_ARRAY, player.getPendingUpdatesJsonArray());
            player.pendingUpdates.clear();
            resp.getWriter().write(respJson.toString());
        } else {
            player.asyncContext = req.startAsync(req, resp);
            player.asyncContext.setTimeout(SECOND * TIMEOUT_IN_SECONDS * 2);
            timer.schedule(new TimerTask(){
                @Override
                public void run() {
                    player.lock.lock();
                    if(player.asyncContext != null) {
                        try {
                            player.asyncContext.getResponse().getWriter().write(EMPTY_UPDATE_ARRAY);
                            player.asyncContext.getResponse().getWriter().flush();
                            player.asyncContext.complete();
                        } catch (IOException e) {
                            player.asyncContext.dispatch();
                        } finally {
                            player.asyncContext = null;
                        }
                    }
                    player.lock.unlock();
                }
            }, SECOND * TIMEOUT_IN_SECONDS);
        }
        player.lock.unlock();
    }

    private String readBody(HttpServletRequest req) throws IOException {
        Reader reader = req.getReader();
        StringBuilder stringBuilder = new StringBuilder();
        char[] buffer = new char[1024];
        while(reader.read(buffer) > 0) {
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }

    private Player getLockedPlayer(int id) {
        if(!players.containsKey(id)) return null;
        Player player = players.get(id);
        player.lock.lock();
        if(!players.containsKey(id)) {
            player.lock.unlock(); //pretty rare situation
            return null;
        }
        return player;
    }

    private void sendErrorResponse(final HttpServletResponse resp, String message) throws IOException {
        resp.getWriter().write("error: " + message);
        resp.getWriter().flush();
    }
}