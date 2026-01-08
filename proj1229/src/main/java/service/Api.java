package service;

import base.EventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import events.ApiEvent;
import events.RequestBody;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
public class Api extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handle(req, resp);
    }
    public void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        try {
            RequestBody body = objectMapper.readValue(req.getReader(), RequestBody.class);
            ApiEvent ev = new ApiEvent();
            ev.setBody(body);
            EventBus.publish(ev);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(ev.getResp());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Invalid JSON format\"}");
        }
    }
}
