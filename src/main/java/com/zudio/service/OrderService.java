package com.zudio.service;

import com.zudio.dao.CartDAO;
import com.zudio.dao.CouponDAO;
import com.zudio.dao.OrderDAO;
import com.zudio.dao.ProductDAO;
import com.zudio.model.CartItem;
import com.zudio.model.Coupon;
import com.zudio.model.Order;
import com.zudio.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

public class OrderService {
    private final OrderDAO orderDAO = new OrderDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final CouponDAO couponDAO = new CouponDAO();
    private final ProductDAO productDAO = new ProductDAO();

    private static final Set<String> ALLOWED_SIZES = Set.of(
            "XS", "S", "M", "L", "XL", "XXL", "3XL", "6", "7", "8", "9", "10", "11", "FREE", "ONESIZE"
    );

    /**
     * Places an order with strict server-side price integrity and stock deduction inside an ACID transaction.
     * All prices, subtotals, discounts, and totals are computed strictly from database records.
     * Any client-sent prices/totals are completely disregarded.
     */
    public Order checkout(int userId, String paymentMethod, String address, String couponCode) throws Exception {
        // 1. Validate Address
        if (address == null || address.trim().length() < 10 || address.trim().length() > 500) {
            throw new IllegalArgumentException("Please enter a valid complete delivery address (between 10 and 500 characters).");
        }

        // 2. Validate Payment Method
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        String cleanMethod = paymentMethod.trim().toUpperCase();
        if (!cleanMethod.equals("COD") && !cleanMethod.equals("UPI") && !cleanMethod.equals("CARD")) {
            throw new IllegalArgumentException("Invalid payment method. Allowed: COD, UPI, CARD.");
        }

        // 3. Load active cart items from DB
        List<CartItem> items = cartDAO.getCartByUser(userId);
        if (items.isEmpty()) {
            throw new IllegalStateException("Your shopping bag is empty. Please add items before checking out.");
        }

        // 4. Server-Side Price & Quantity Calculation
        BigDecimal computedSubtotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            // Re-fetch product directly from DB to verify price integrity & stock
            Product dbProduct = productDAO.findById(item.getProductId());
            if (dbProduct == null) {
                throw new IllegalStateException("Product #" + item.getProductId() + " is no longer available.");
            }
            if (item.getQuantity() <= 0 || item.getQuantity() > 20) {
                throw new IllegalArgumentException("Invalid item quantity: " + item.getQuantity());
            }
            if (item.getSize() != null && !ALLOWED_SIZES.contains(item.getSize().toUpperCase())) {
                throw new IllegalArgumentException("Invalid item size: " + item.getSize());
            }
            if (dbProduct.getStock() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for '" + dbProduct.getName() + "'. Available: " + dbProduct.getStock());
            }

            // Always enforce DB price
            item.setProductPrice(dbProduct.getPrice());
            computedSubtotal = computedSubtotal.add(dbProduct.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 5. Server-side Coupon Calculation
        BigDecimal computedDiscount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponDAO.findByCode(couponCode.trim().toUpperCase());
            if (coupon != null && coupon.isActive()) {
                if (coupon.getMinOrderAmount() == null || computedSubtotal.compareTo(coupon.getMinOrderAmount()) >= 0) {
                    BigDecimal percent = BigDecimal.valueOf(coupon.getDiscountPercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    computedDiscount = computedSubtotal.multiply(percent).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        // 6. Server-side Shipping Calculation
        BigDecimal shipping = computedSubtotal.compareTo(new BigDecimal("999.00")) >= 0 ? BigDecimal.ZERO : new BigDecimal("49.00");

        // 7. Server-side Final Total Calculation
        BigDecimal finalTotal = computedSubtotal.subtract(computedDiscount).add(shipping);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        return orderDAO.placeOrder(userId, finalTotal, computedDiscount, cleanMethod, address.trim(), items);
    }

    public List<Order> getOrdersByUser(int userId) {
        return orderDAO.listByUser(userId);
    }

    public Order getOrderDetails(int orderId, int userId, boolean isAdmin) {
        Order order = orderDAO.findById(orderId);
        if (order == null) return null;

        // Strict Anti-IDOR Authorization check
        if (!isAdmin && order.getUserId() != userId) {
            throw new SecurityException("Access Denied: You are not authorized to view order #" + orderId);
        }
        return order;
    }

    public boolean updateStatus(int orderId, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Order status cannot be empty.");
        }
        Set<String> validStatuses = Set.of("Placed", "Packed", "Shipped", "Delivered", "Cancelled");
        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid order status: " + newStatus);
        }
        return orderDAO.updateStatus(orderId, newStatus);
    }
}
