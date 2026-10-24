package com.zudio.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.zudio.model.Order;
import com.zudio.model.Product;
import com.zudio.model.User;
import com.zudio.service.AdminService;
import com.zudio.service.ProductService;
import com.zudio.util.JsonUtil;
import com.zudio.util.TokenUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminHandler extends BaseHttpHandler {
    private final AdminService adminService = new AdminService();
    private final ProductService productService = new ProductService();

    @Override
    protected void process(HttpExchange exchange) throws Exception {
        requireAdmin(exchange); // Enforce role-based access control (ADMIN only)

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if (path.startsWith("/api/admin/dashboard") && "GET".equals(method)) {
            Map<String, Object> dashboard = adminService.getDashboardData();
            sendSuccess(exchange, null, dashboard);
            return;
        }

        if (path.startsWith("/api/admin/customers") && "GET".equals(method)) {
            List<User> customers = adminService.getAllCustomers();
            sendSuccess(exchange, null, customers);
            return;
        }

        if (path.startsWith("/api/admin/orders")) {
            Integer orderId = extractPathId(exchange, "/api/admin/orders");
            if ("GET".equals(method)) {
                List<Order> orders = adminService.getAllOrders();
                sendSuccess(exchange, null, orders);
            } else if ("PUT".equals(method) && orderId != null) {
                String body = readBody(exchange);
                JsonObject json = JsonUtil.parseObject(body);
                String newStatus = json.get("status").getAsString();
                boolean updated = adminService.updateOrderStatus(orderId, newStatus);
                if (updated) {
                    sendSuccess(exchange, "Order #" + orderId + " status updated to " + newStatus, null);
                } else {
                    sendError(exchange, 400, "Failed to update order status.");
                }
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
            return;
        }

        if (path.startsWith("/api/admin/products")) {
            Integer productId = extractPathId(exchange, "/api/admin/products");
            if ("GET".equals(method)) {
                List<Product> products = productService.getProducts(null, null, null, null, "newest", null);
                sendSuccess(exchange, null, products);
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Product product = JsonUtil.fromJson(body, Product.class);
                Product created = productService.createProduct(product);
                sendSuccess(exchange, "Product created successfully!", created);
            } else if ("PUT".equals(method) && productId != null) {
                String body = readBody(exchange);
                Product product = JsonUtil.fromJson(body, Product.class);
                product.setId(productId);
                boolean updated = productService.updateProduct(product);
                if (updated) {
                    sendSuccess(exchange, "Product updated successfully!", product);
                } else {
                    sendError(exchange, 400, "Failed to update product.");
                }
            } else if ("DELETE".equals(method) && productId != null) {
                boolean deleted = productService.deleteProduct(productId);
                if (deleted) {
                    sendSuccess(exchange, "Product deleted successfully.", null);
                } else {
                    sendError(exchange, 400, "Failed to delete product.");
                }
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
            return;
        }

        sendError(exchange, 404, "Endpoint not found: " + path);
    }
}
