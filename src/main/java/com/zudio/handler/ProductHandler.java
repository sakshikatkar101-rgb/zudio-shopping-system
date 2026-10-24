package com.zudio.handler;

import com.sun.net.httpserver.HttpExchange;
import com.zudio.model.Category;
import com.zudio.model.Product;
import com.zudio.service.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProductHandler extends BaseHttpHandler {
    private final ProductService productService = new ProductService();

    @Override
    protected void process(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if (path.startsWith("/api/categories")) {
            handleCategories(exchange);
            return;
        }

        if ("GET".equals(method)) {
            Integer id = extractPathId(exchange, "/api/products");
            if (id != null) {
                handleGetProductById(exchange, id);
            } else {
                handleListProducts(exchange);
            }
        } else {
            sendError(exchange, 405, "Method Not Allowed");
        }
    }

    private void handleCategories(HttpExchange exchange) throws Exception {
        List<Category> list = productService.getAllCategories();
        sendSuccess(exchange, null, list);
    }

    private void handleGetProductById(HttpExchange exchange, int id) throws Exception {
        Product p = productService.getProductById(id);
        if (p == null) {
            sendError(exchange, 404, "Product not found with ID: " + id);
            return;
        }
        sendSuccess(exchange, null, p);
    }

    private void handleListProducts(HttpExchange exchange) throws Exception {
        Map<String, String> q = getQueryParams(exchange);
        String category = q.get("category");
        String search = q.get("search");
        String sortBy = q.get("sortBy");
        Boolean trending = q.containsKey("trending") ? Boolean.parseBoolean(q.get("trending")) : null;

        BigDecimal minPrice = null;
        if (q.containsKey("minPrice")) {
            try { minPrice = new BigDecimal(q.get("minPrice")); } catch (Exception ignored) {}
        }

        BigDecimal maxPrice = null;
        if (q.containsKey("maxPrice")) {
            try { maxPrice = new BigDecimal(q.get("maxPrice")); } catch (Exception ignored) {}
        }

        List<Product> products = productService.getProducts(category, search, minPrice, maxPrice, sortBy, trending);
        sendSuccess(exchange, null, products);
    }
}
