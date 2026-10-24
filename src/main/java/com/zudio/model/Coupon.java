package com.zudio.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Coupon {
    private String code;
    private int discountPercent;
    private boolean active;
    private BigDecimal minOrderAmount;
    private Timestamp createdAt;

    public Coupon() {}

    public Coupon(String code, int discountPercent, boolean active, BigDecimal minOrderAmount, Timestamp createdAt) {
        this.code = code;
        this.discountPercent = discountPercent;
        this.active = active;
        this.minOrderAmount = minOrderAmount;
        this.createdAt = createdAt;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
