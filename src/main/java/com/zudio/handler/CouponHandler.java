package com.zudio.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.zudio.dao.CouponDAO;
import com.zudio.model.Coupon;
import com.zudio.service.CartService;
import com.zudio.util.JsonUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CouponHandler extends BaseHttpHandler {
    private final CartService cartService = new CartService();
    private final CouponDAO couponDAO = new CouponDAO();

    @Override
    protected void process(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if (path.endsWith("/validate") && "POST".equals(method)) {
            String body = readBody(exchange);
            JsonObject json = JsonUtil.parseObject(body);
            String code = json.has("code") ? json.get("code").getAsString() : "";
            BigDecimal amount = json.has("orderAmount") 
                    ? json.get("orderAmount").getAsBigDecimal() 
                    : BigDecimal.ZERO;

            Map<String, Object> validation = cartService.validateCoupon(code, amount);
            sendJson(exchange, 200, validation);
        } else if ("GET".equals(method)) {
            List<Coupon> coupons = couponDAO.listAll();
            sendSuccess(exchange, null, coupons);
        } else {
            sendError(exchange, 404, "Endpoint not found");
        }
    }
}
