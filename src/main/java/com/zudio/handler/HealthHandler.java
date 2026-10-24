package com.zudio.handler;

import com.sun.net.httpserver.HttpExchange;
import com.zudio.util.DatabaseUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint for Docker HEALTHCHECK and cloud orchestrators.
 * Returns: {"status":"UP","db":"UP"} (HTTP 200) or {"status":"DOWN","db":"DOWN"} (HTTP 503).
 */
public class HealthHandler extends BaseHttpHandler {

    @Override
    protected void process(HttpExchange exchange) throws Exception {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        boolean dbUp = DatabaseUtil.isHealthy();
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", dbUp ? "UP" : "DOWN");
        response.put("db", dbUp ? "UP" : "DOWN");

        sendJson(exchange, dbUp ? 200 : 503, response);
    }
}
