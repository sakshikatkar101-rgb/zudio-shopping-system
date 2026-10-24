package com.zudio.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class WishlistItem {
    private int id;
    private int userId;
    private int productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal productPrice;
    private BigDecimal productMrp;
    private int productStock;
    private Timestamp createdAt;

    public WishlistItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public BigDecimal getProductPrice() { return productPrice; }
    public void setProductPrice(BigDecimal productPrice) { this.productPrice = productPrice; }

    public BigDecimal getProductMrp() { return productMrp; }
    public void setProductMrp(BigDecimal productMrp) { this.productMrp = productMrp; }

    public int getProductStock() { return productStock; }
    public void setProductStock(int productStock) { this.productStock = productStock; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
