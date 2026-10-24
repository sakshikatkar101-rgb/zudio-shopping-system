package com.zudio.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.zudio.dao.WishlistDAO;
import com.zudio.model.WishlistItem;
import com.zudio.util.JsonUtil;
import com.zudio.util.TokenUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WishlistHandler extends BaseHttpHandler {
    private final WishlistDAO wishlistDAO = new WishlistDAO();

    @Override
    protected void process(HttpExchange exchange) throws Exception {
        TokenUtil.SessionUser user = requireAuth(exchange);
        String method = exchange.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            List<WishlistItem> list = wishlistDAO.getWishlistByUser(user.getUserId());
            sendSuccess(exchange, null, list);
        } else if ("POST".equals(method)) {
            String body = readBody(exchange);
            JsonObject json = JsonUtil.parseObject(body);
            int productId = json.get("productId").getAsInt();
            boolean isWishlisted = wishlistDAO.toggleItem(user.getUserId(), productId);
            Map<String, Object> res = new HashMap<>();
            res.put("wishlisted", isWishlisted);
            sendSuccess(exchange, isWishlisted ? "Item added to wishlist!" : "Item removed from wishlist.", res);
        } else if ("DELETE".equals(method)) {
            Integer productId = extractPathId(exchange, "/api/wishlist");
            if (productId != null) {
                wishlistDAO.removeItem(user.getUserId(), productId);
                sendSuccess(exchange, "Item removed from wishlist.", null);
            } else {
                sendError(exchange, 400, "Missing product ID.");
            }
        } else {
            sendError(exchange, 405, "Method Not Allowed");
        }
    }
}
