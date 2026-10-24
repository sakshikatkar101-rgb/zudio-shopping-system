package com.zudio.service;

import com.zudio.dao.CategoryDAO;
import com.zudio.dao.ProductDAO;
import com.zudio.model.Category;
import com.zudio.model.Product;

import java.math.BigDecimal;
import java.util.List;

public class ProductService {
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    public List<Product> getProducts(String category, String search, BigDecimal minPrice, 
                                    BigDecimal maxPrice, String sortBy, Boolean trendingOnly) {
        String cleanSearch = null;
        if (search != null && !search.isBlank()) {
            cleanSearch = search.trim();
            if (cleanSearch.length() > 100) cleanSearch = cleanSearch.substring(0, 100);
        }
        return productDAO.listProducts(category, cleanSearch, minPrice, maxPrice, sortBy, trendingOnly);
    }

    public Product getProductById(int id) {
        if (id <= 0) return null;
        return productDAO.findById(id);
    }

    public List<Category> getAllCategories() {
        return categoryDAO.listAll();
    }

    public Product createProduct(Product product) {
        validateProduct(product);
        return productDAO.create(product);
    }

    public boolean updateProduct(Product product) {
        if (product.getId() <= 0) throw new IllegalArgumentException("Invalid product ID");
        validateProduct(product);
        return productDAO.update(product);
    }

    public boolean deleteProduct(int id) {
        if (id <= 0) throw new IllegalArgumentException("Invalid product ID");
        return productDAO.delete(id);
    }

    public boolean updateStock(int id, int stock) {
        if (id <= 0) throw new IllegalArgumentException("Invalid product ID");
        if (stock < 0 || stock > 50000) throw new IllegalArgumentException("Stock must be between 0 and 50,000 units.");
        return productDAO.updateStock(id, stock);
    }

    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().trim().length() < 2 || product.getName().trim().length() > 200) {
            throw new IllegalArgumentException("Product name must be between 2 and 200 characters.");
        }
        product.setName(product.getName().trim());

        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0 || product.getPrice().compareTo(new BigDecimal("1000000")) > 0) {
            throw new IllegalArgumentException("Price must be greater than 0 and less than ₹10,00,000.");
        }

        if (product.getMrp() == null || product.getMrp().compareTo(product.getPrice()) < 0) {
            product.setMrp(product.getPrice());
        }

        if (product.getStock() < 0 || product.getStock() > 50000) {
            throw new IllegalArgumentException("Stock must be between 0 and 50,000.");
        }

        if (product.getImageUrl() != null && product.getImageUrl().length() > 500) {
            throw new IllegalArgumentException("Image URL cannot exceed 500 characters.");
        }
    }
}
