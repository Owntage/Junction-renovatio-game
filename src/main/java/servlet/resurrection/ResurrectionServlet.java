package servlet.resurrection;

import org.json.JSONException;
import org.json.JSONObject;
import servlet.mafia.MafiaException;

import java.io.IOException;
import java.io.Reader;
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
        respJson.put(ResurrectionConstants.Json.PLAYER_ID, Integer.toString(playerCounter.getAndIncrement()));
        resp.getWriter().write(respJson.toString());
        resp.getWriter().flush();
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
}