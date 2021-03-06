package servlet.resurrection;

import org.json.JSONException;
import org.json.JSONObject;
import servlet.mafia.MafiaException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
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
    final int PLAYER_HP = 1;
    final int RENOVATIO_HP = 10;
    final int RENOVATIO_ID = -1337;
    final long TIMEOUT_IN_SECONDS = 10; //todo should be replaced with some small value
    final long DISCONNECT_TIMEOUT_IN_SECONDS = 30;
    final String EMPTY_UPDATE_ARRAY = "{\"" + ResurrectionConstants.Json.UPDATE_ARRAY + "\":[]}";

    @Override
    public void init() throws ServletException {
        timer.schedule(new TimerTask(){

           @Override
           public void run() {
               time.getAndIncrement();
           }
        }, MoveUpdate.TICK_DELTA, MoveUpdate.TICK_DELTA);
        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                ArrayList<Player> disconnectedPlayers = new ArrayList<>();
                for(Player player : players.values()) {
                    if(player.id == RENOVATIO_ID) {
                        continue;
                    }
                    player.lock.lock();
                    if(!player.notUpdating) {
                        player.notUpdating = true;
                        player.lock.unlock();
                    } else {
                        sendDisconnectMessage(player.id);
                        disconnectedPlayers.add(player);
                    }


                }
                for(Player disconnectedPlayer : disconnectedPlayers) {
                    players.remove(disconnectedPlayer.id);
                    disconnectedPlayer.lock.unlock();
                }
            }
        }, DISCONNECT_TIMEOUT_IN_SECONDS * SECOND, DISCONNECT_TIMEOUT_IN_SECONDS * SECOND);

        players.put(RENOVATIO_ID, new Player(RENOVATIO_ID, RENOVATIO_HP));
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
                case ResurrectionConstants.Requests.HIT_REQUEST: {
                    onHitRequest(req, resp, jsonObject);
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
        Player player = new Player(playerCounter.getAndIncrement(), PLAYER_HP);
        for(Player otherPlayer : players.values()) {
            if(otherPlayer.lastMoveUpdate != null) {
                player.pendingUpdates.add(new MoveUpdate(otherPlayer.lastMoveUpdate, time.get()));
            }
            player.pendingUpdates.add(new HealthUpdate(otherPlayer.id, otherPlayer.hp));
        }
        player.pendingUpdates.add(new HealthUpdate(player.id, player.hp));
        players.put(player.id, player);
        respJson.put(ResurrectionConstants.Json.PLAYER_ID, player.id);
        System.out.println("player was put in map. id: " + player.id);
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
        thisPlayer.notUpdating = false;

        thisPlayer.lock.unlock();
        for(Player player : players.values()) {
            if(player.id != moveUpdate.id) {
                player.lock.lock();
                player.sendUpdate(moveUpdate);
                player.lock.unlock();
            }
        }
    }

    private void onHitRequest(final HttpServletRequest req, final HttpServletResponse resp, JSONObject jsonObj)
            throws IOException, JSONException {
        int id = Integer.parseInt(jsonObj.getString(ResurrectionConstants.Json.PLAYER_ID));
        for(Player player : players.values()) {
            if(id != player.id) {
                player.lock.lock();
                player.sendUpdate(new AnimationUpdate(id));
                player.lock.unlock();
            }
        }

        int damage = Integer.parseInt(jsonObj.getString(ResurrectionConstants.Json.DAMAGE));
        if(damage != 0) {
            System.out.println("damage is not zero");
            int targetId = Integer.parseInt(jsonObj.getString(ResurrectionConstants.Json.TARGET_ID));
            System.out.println("target: " + targetId);
            System.out.println("attacker id: " + id);
            Player targetPlayer = getLockedPlayer(targetId);
            if(targetPlayer == null) return;
            System.out.println("target player is not null");
            targetPlayer.hp -= damage; //todo do some special checks for renovatio
            targetPlayer.lock.unlock();
            for(Player player : players.values()) {
                //if(player.id == targetId) continue; //it means, that if you damage yourself, you won't get update
                player.lock.lock();

                player.sendUpdate(new HealthUpdate(targetId, targetPlayer.hp));

                player.lock.unlock();
            }
        }

    }

    private void onGetUpdateRequest(final HttpServletRequest req, final HttpServletResponse resp, JSONObject jsonObj)
            throws IOException, JSONException {
        System.out.println("getUpdateRequest");
        String id = jsonObj.getString(ResurrectionConstants.Json.PLAYER_ID);
        final Player player = getLockedPlayer(Integer.parseInt(id));
        if(player == null) {
            System.out.println("getUpdate for player not in map. id: " + id);
            return;
        }
        player.notUpdating = false;
        if(player.pendingUpdates.size() > 0) {
            System.out.println("pendingUpdates is not empty. sending them");
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

    private void sendDisconnectMessage(int disconnectedId) {
        for(Player player : players.values()) {
            if(player.id != disconnectedId) {
                try {
                    player.sendUpdate(new DisconnectUpdate(disconnectedId));
                } catch (JSONException | IOException e) {
                    //we can do nothing
                }
            }
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