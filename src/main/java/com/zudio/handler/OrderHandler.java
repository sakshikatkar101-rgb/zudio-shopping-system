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
        String path = exchange.getRequestURI().getPath();

        if ("GET".equals(method)) {
            Integer pathId = extractPathId(exchange, "/api/orders");
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
            if (path.matches("^/api/orders/\\d+/cancel/?$")) {
                // Cancel Order Endpoint
                Integer orderId = extractPathId(exchange, "/api/orders");
                if (orderId == null) {
                    sendError(exchange, 400, "Invalid order identifier.");
                    return;
                }
                String body = readBody(exchange);
                String reason = "Cancelled by user";
                try {
                    JsonObject json = JsonUtil.parseObject(body);
                    if (json != null && json.has("reason") && !json.get("reason").isJsonNull()) {
                        reason = json.get("reason").getAsString();
                    }
                } catch (Exception ignored) {}

                Order cancelled = orderService.cancelOrder(orderId, user.getUserId(), user.isAdmin(), reason);
                sendSuccess(exchange, "Order #" + orderId + " has been successfully cancelled.", cancelled);
                return;
            }

            // Checkout Endpoint
            String body = readBody(exchange);
            JsonObject json = JsonUtil.parseObject(body);

            String paymentMethod = (json.has("paymentMethod") && !json.get("paymentMethod").isJsonNull()) 
                    ? json.get("paymentMethod").getAsString() : "COD";
            String address = (json.has("address") && !json.get("address").isJsonNull()) 
                    ? json.get("address").getAsString() : "";
            String couponCode = (json.has("couponCode") && !json.get("couponCode").isJsonNull()) 
                    ? json.get("couponCode").getAsString() : null;
            String paymentStatus = (json.has("paymentStatus") && !json.get("paymentStatus").isJsonNull()) 
                    ? json.get("paymentStatus").getAsString() : null;
            String cardLast4 = (json.has("cardLast4") && !json.get("cardLast4").isJsonNull()) 
                    ? json.get("cardLast4").getAsString() : null;
            String cardBrand = (json.has("cardBrand") && !json.get("cardBrand").isJsonNull()) 
                    ? json.get("cardBrand").getAsString() : null;

            Order createdOrder = orderService.checkout(user.getUserId(), paymentMethod, address, couponCode, 
                                                      paymentStatus, cardLast4, cardBrand);
            sendSuccess(exchange, "Order placed successfully! Order ID #" + createdOrder.getId(), createdOrder);
        } else {
            sendError(exchange, 405, "Method Not Allowed");
        }
    }
}
