package com.zudio.service;

import com.zudio.dao.CartDAO;
import com.zudio.dao.CouponDAO;
import com.zudio.dao.ProductDAO;
import com.zudio.model.CartItem;
import com.zudio.model.Coupon;
import com.zudio.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CartService {
    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final CouponDAO couponDAO = new CouponDAO();

    private static final Set<String> ALLOWED_SIZES = Set.of(
            "XS", "S", "M", "L", "XL", "XXL", "3XL", "6", "7", "8", "9", "10", "11", "FREE", "ONESIZE"
    );

    public Map<String, Object> getCartSummary(int userId, String couponCode) {
        List<CartItem> items = cartDAO.getCartByUser(userId);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal mrpTotal = BigDecimal.ZERO;
        int totalItemCount = 0;

        for (CartItem item : items) {
            // Always refresh prices from DB
            Product p = productDAO.findById(item.getProductId());
            if (p != null) {
                item.setProductPrice(p.getPrice());
                item.setProductMrp(p.getMrp());
            }

            subtotal = subtotal.add(item.getSubtotal());
            if (item.getProductMrp() != null) {
                mrpTotal = mrpTotal.add(item.getProductMrp().multiply(BigDecimal.valueOf(item.getQuantity())));
            } else {
                mrpTotal = mrpTotal.add(item.getSubtotal());
            }
            totalItemCount += item.getQuantity();
        }

        BigDecimal discount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;

        if (couponCode != null && !couponCode.isBlank()) {
            appliedCoupon = couponDAO.findByCode(couponCode.trim().toUpperCase());
            if (appliedCoupon != null && appliedCoupon.isActive()) {
                if (appliedCoupon.getMinOrderAmount() == null || subtotal.compareTo(appliedCoupon.getMinOrderAmount()) >= 0) {
                    BigDecimal percent = BigDecimal.valueOf(appliedCoupon.getDiscountPercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    discount = subtotal.multiply(percent).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        BigDecimal shipping = (subtotal.compareTo(new BigDecimal("999.00")) >= 0 || items.isEmpty()) 
                ? BigDecimal.ZERO 
                : new BigDecimal("49.00");

        BigDecimal total = subtotal.subtract(discount).add(shipping);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("totalItems", totalItemCount);
        result.put("subtotal", subtotal);
        result.put("mrpTotal", mrpTotal);
        result.put("savings", mrpTotal.subtract(subtotal).add(discount));
        result.put("discount", discount);
        result.put("shipping", shipping);
        result.put("total", total);
        result.put("coupon", appliedCoupon);
        return result;
    }

    public boolean addToCart(int userId, int productId, String size, int quantity) {
        if (productId <= 0) throw new IllegalArgumentException("Invalid product ID");
        if (quantity <= 0 || quantity > 20) {
            throw new IllegalArgumentException("Quantity must be between 1 and 20.");
        }
        String cleanSize = (size == null || size.isBlank()) ? "M" : size.trim().toUpperCase();
        if (!ALLOWED_SIZES.contains(cleanSize)) {
            throw new IllegalArgumentException("Invalid size option: " + cleanSize);
        }

        Product p = productDAO.findById(productId);
        if (p == null) throw new IllegalArgumentException("Product not found");
        if (p.getStock() <= 0) throw new IllegalStateException("Product is out of stock.");
        if (quantity > p.getStock()) {
            throw new IllegalStateException("Cannot add " + quantity + " units; only " + p.getStock() + " available in stock.");
        }

        return cartDAO.addItem(userId, productId, cleanSize, quantity);
    }

    public boolean updateQuantity(int cartItemId, int userId, int quantity) {
        if (quantity < 0 || quantity > 20) {
            throw new IllegalArgumentException("Quantity must be between 0 and 20.");
        }
        return cartDAO.updateQuantity(cartItemId, userId, quantity);
    }

    public boolean removeItem(int cartItemId, int userId) {
        return cartDAO.removeItem(cartItemId, userId);
    }

    public boolean clearCart(int userId) {
        return cartDAO.clearCart(userId);
    }

    public Map<String, Object> validateCoupon(String code, BigDecimal orderAmount) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Coupon code cannot be empty.");
        }
        Coupon c = couponDAO.findByCode(code.trim().toUpperCase());
        Map<String, Object> res = new HashMap<>();
        if (c == null || !c.isActive()) {
            res.put("valid", false);
            res.put("message", "Invalid or inactive promo coupon code.");
            return res;
        }

        if (c.getMinOrderAmount() != null && orderAmount.compareTo(c.getMinOrderAmount()) < 0) {
            res.put("valid", false);
            res.put("message", "Coupon requires a minimum order value of ₹" + c.getMinOrderAmount());
            return res;
        }

        BigDecimal percent = BigDecimal.valueOf(c.getDiscountPercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal discount = orderAmount.multiply(percent).setScale(2, RoundingMode.HALF_UP);

        res.put("valid", true);
        res.put("code", c.getCode());
        res.put("discountPercent", c.getDiscountPercent());
        res.put("discountAmount", discount);
        res.put("message", "Coupon applied: " + c.getDiscountPercent() + "% OFF!");
        return res;
    }
}
