package com.zudio.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.zudio.model.Order;
import com.zudio.service.OrderService;
import com.zudio.util.JsonUtil;
import com.zudio.util.TokenUtil;

import java.util.List;

public class OrderHandler extends BaseHttpHandler {
    private final OrderService orderService = new OrderService();

    @Override
    protected void process(HttpExchange exchange) throws Exception {
        TokenUtil.SessionUser user = requireAuth(exchange);
        String method = exchange.getRequestMethod().toUpperCase();
        Integer pathId = extractPathId(exchange, "/api/orders");

        if ("GET".equals(method)) {
            if (pathId != null) {
                Order order = orderService.getOrderDetails(pathId, user.getUserId(), user.isAdmin());
                if (order == null) {
                    sendError(exchange, 404, "Order not found.");
                    return;
                }
                sendSuccess(exchange, null, order);
            } else {
                List<Order> orders = orderService.getOrdersByUser(user.getUserId());
                sendSuccess(exchange, null, orders);
            }
        } else if ("POST".equals(method)) {
            String body = readBody(exchange);
            JsonObject json = JsonUtil.parseObject(body);

            String paymentMethod = (json.has("paymentMethod") && !json.get("paymentMethod").isJsonNull()) 
                    ? json.get("paymentMethod").getAsString() : "COD";
            String address = (json.has("address") && !json.get("address").isJsonNull()) 
                    ? json.get("address").getAsString() : "";
            String couponCode = (json.has("couponCode") && !json.get("couponCode").isJsonNull()) 
                    ? json.get("couponCode").getAsString() : null;

            Order createdOrder = orderService.checkout(user.getUserId(), paymentMethod, address, couponCode);
            sendSuccess(exchange, "Order placed successfully! Order ID #" + createdOrder.getId(), createdOrder);
        } else {
            sendError(exchange, 405, "Method Not Allowed");
        }
    }
}
