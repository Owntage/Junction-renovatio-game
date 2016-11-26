package servlet.mafia;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.*;

@WebServlet(
        name = "MafiaServlet",
        asyncSupported = true,
        urlPatterns = {"/mafia-game"}
)
public class MafiaGameServlet extends HttpServlet {
    private final ConcurrentHashMap<Integer, MafiaRoom> rooms = new ConcurrentHashMap<>();
    private final HashSet<MafiaRoom> waitingRooms = new HashSet<>();
    private volatile MafiaRoom waitingRoom;
    private volatile int roomCounter = 0;
    private volatile int playerCounter = 0;
    private final long MINUTE = 60_000;

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        String body = readBody(req);
        try {

            JSONObject jsonObject = new JSONObject(body);
            String type = jsonObject.getString("type");
            switch(type) {
                case "Connection": {
                    onConnectRequest(req, resp);
                    break;
                }
                case "Wait": {
                    onWaitRequest(req, resp, jsonObject);
                    break;
                }
                default : {
                    sendErrorResponse(resp, "no such type of request. your json: " + jsonObject.toString());
                    break;
                }

            }
        } catch (JSONException | MafiaException e) {
            sendErrorResponse(resp,"request text: " + body + " error message: " + e.getMessage());
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

    private void sendErrorResponse(final HttpServletResponse resp, String message) throws IOException {
        resp.getWriter().write("error: " + message);
        resp.getWriter().flush();
    }

    private void onWaitRequest(final HttpServletRequest req, final HttpServletResponse resp, JSONObject requestJson)
            throws JSONException, MafiaException {
        MafiaRoom room = rooms.get(requestJson.getInt("room_id"));
        int userId = requestJson.getInt("user_id");
        if(room == null || room.getFirstPlayer() != userId) {
            throw new MafiaException();
        }

        final AsyncContext asyncContext = req.startAsync(req, resp);
        final AsyncWrapper asyncWrapper = new AsyncWrapper(asyncContext);
        asyncContext.setTimeout(MINUTE / 2);
        new Timer().schedule(new TimerTask(){
            @Override
            public void run() {
                asyncWrapper.lock.lock();
                if(asyncWrapper.isFinished) {
                    asyncWrapper.lock.unlock();
                    return;
                }
                asyncWrapper.isFinished = true;
                JSONObject respJson = new JSONObject();
                try {
                    respJson.put("status", 0);
                    asyncContext.getResponse().getWriter().write(respJson.toString());
                    asyncContext.getResponse().getWriter().flush();
                    asyncContext.complete();
                } catch (JSONException | IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    asyncWrapper.lock.unlock();
                }
            }
        }, MINUTE / 3);
        room.setFirstAsyncContext(asyncWrapper);
    }

    private MafiaRoom createFirstPlayer(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException, JSONException  {
        MafiaRoom room = new MafiaRoom(playerCounter++, roomCounter++);
        rooms.put(room.getRoomId(), room);
        waitingRooms.add(room);
        return room;
    }

    private void createSecondPlayer(final HttpServletRequest req, final HttpServletResponse resp, MafiaRoom room)
            throws IOException, JSONException {
        room.getFirstPlayerContext().isFinished = true;
        room.setSecondPlayer(playerCounter++);
        JSONObject respJson = new JSONObject();
        respJson.put("status", 1);
        room.getFirstPlayerContext().context.getResponse().getWriter().write(respJson.toString());
        room.getFirstPlayerContext().context.getResponse().getWriter().flush();
        room.getFirstPlayerContext().context.complete();
        room.getFirstPlayerContext().lock.unlock();
    }

    private MafiaRoom getAvailableRoom() {
        MafiaRoom res = null;
        for(MafiaRoom room : waitingRooms) {
            if(room.getFirstPlayerContext() == null) {
                continue;
            }
            room.getFirstPlayerContext().lock.lock();
            if(!room.getFirstPlayerContext().isFinished) {
                res = room; //do not unlock the room, if it is suitable

                break;
            }
            room.getFirstPlayerContext().lock.unlock();
        }
        return res;
    }

    synchronized private void onConnectRequest(final HttpServletRequest req, final HttpServletResponse resp)
            throws JSONException, IOException {
        MafiaRoom room = getAvailableRoom();
        boolean isFirst = false;
        if(room == null) {
            isFirst = true;
            room = createFirstPlayer(req, resp);
        } else {
            createSecondPlayer(req, resp, room);
        }


        JSONObject response = new JSONObject();
        response.put("room_id", room.getRoomId());
        response.put("user_id", playerCounter - 1);
        response.put("map", room.getField());
        response.put("player_order", isFirst ? 1 : 2);
        response.put("character_id",
                isFirst ? room.getFirstCharacter() : room.getSecondCharacter());
        resp.getWriter().write(response.toString());
        resp.getWriter().flush();

    }
}