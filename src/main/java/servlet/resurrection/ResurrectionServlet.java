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

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(
        name = "ResurrectionServlet",
        urlPatterns = {"/resurrection-game"}
)
public class ResurrectionServlet extends HttpServlet {

    final AtomicInteger playerCounter = new AtomicInteger(1);
    final ConcurrentHashMap<Integer, Player>  players = new ConcurrentHashMap<>();
    final Timer timer = new Timer();

    final long SECOND = 1000;
    final String EMPTY_UPDATE_ARRAY = "{\"" + ResurrectionConstants.Json.UPDATE_ARRAY + "\":[]}";

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
        JSONObject respJson = new JSONObject();
        respJson.put(ResurrectionConstants.Json.PLAYER_ID, playerCounter.getAndIncrement());
        resp.getWriter().write(respJson.toString());
        resp.getWriter().flush();
    }

    private void onDirectionChangeRequest(final HttpServletRequest req, final HttpServletResponse resp, JSONObject jsonObj)
            throws IOException, JSONException {
        System.out.println("directionChangeRequest: " + jsonObj.toString());
        //todo send this update to all players.
    }

    private void onGetUpdateRequest(final HttpServletRequest req, final HttpServletResponse resp, JSONObject jsonObj)
            throws IOException, JSONException {
        String id = jsonObj.getString(ResurrectionConstants.Json.PLAYER_ID);
        final Player player = getLockedPlayer(Integer.parseInt(id));
        if(player == null) return;
        if(player.pendingUpdates.size() > 0) {
            JSONObject respJson = new JSONObject();
            respJson.put(ResurrectionConstants.Json.UPDATE_ARRAY, player.getPendingUpdatesJsonArray());
            resp.getWriter().write(respJson.toString());
        } else {
            player.asyncContext = req.startAsync(req, resp);
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
            }, SECOND * 10);
        }
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