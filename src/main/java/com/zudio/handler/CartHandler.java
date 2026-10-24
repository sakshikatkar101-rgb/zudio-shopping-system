package com.zudio.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.zudio.service.CartService;
import com.zudio.util.JsonUtil;
import com.zudio.util.TokenUtil;

import java.util.Map;

public class CartHandler extends BaseHttpHandler {
    private final CartService cartService = new CartService();

    @Override
    protected void process(HttpExchange exchange) throws Exception {
        TokenUtil.SessionUser user = requireAuth(exchange);
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if (path.endsWith("/clear") && "DELETE".equals(method)) {
            cartService.clearCart(user.getUserId());
            sendSuccess(exchange, "Cart cleared successfully.", null);
            return;
        }

        Integer pathId = extractPathId(exchange, "/api/cart");

        switch (method) {
            case "GET":
                Map<String, String> query = getQueryParams(exchange);
                String coupon = query.get("coupon");
                Map<String, Object> summary = cartService.getCartSummary(user.getUserId(), coupon);
                sendSuccess(exchange, null, summary);
                break;

            case "POST":
                String addBody = readBody(exchange);
                JsonObject addJson = JsonUtil.parseObject(addBody);
                int productId = addJson.get("productId").getAsInt();
                String size = addJson.has("size") ? addJson.get("size").getAsString() : "M";
                int quantity = addJson.has("quantity") ? addJson.get("quantity").getAsInt() : 1;

                boolean added = cartService.addToCart(user.getUserId(), productId, size, quantity);
                if (added) {
                    sendSuccess(exchange, "Added to shopping bag!", cartService.getCartSummary(user.getUserId(), null));
                } else {
                    sendError(exchange, 400, "Could not add item to cart.");
                }
                break;

            case "PUT":
                if (pathId == null) {
                    sendError(exchange, 400, "Missing cart item ID.");
                    return;
                }
                String putBody = readBody(exchange);
                JsonObject putJson = JsonUtil.parseObject(putBody);
                int newQty = putJson.get("quantity").getAsInt();
                cartService.updateQuantity(pathId, user.getUserId(), newQty);
                sendSuccess(exchange, "Quantity updated.", cartService.getCartSummary(user.getUserId(), null));
                break;

            case "DELETE":
                if (pathId == null) {
                    sendError(exchange, 400, "Missing cart item ID.");
                    return;
                }
                cartService.removeItem(pathId, user.getUserId());
                sendSuccess(exchange, "Item removed from cart.", cartService.getCartSummary(user.getUserId(), null));
                break;

            default:
                sendError(exchange, 405, "Method Not Allowed");
        }
    }
}
