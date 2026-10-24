package com.zudio.service;

import com.zudio.dao.OrderDAO;
import com.zudio.dao.ProductDAO;
import com.zudio.dao.UserDAO;
import com.zudio.model.Order;
import com.zudio.model.Product;
import com.zudio.model.User;

import java.util.List;
import java.util.Map;

public class AdminService {
    private final OrderDAO orderDAO = new OrderDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final UserDAO userDAO = new UserDAO();
    private final OrderService orderService = new OrderService();

    public Map<String, Object> getDashboardData() {
        Map<String, Object> kpis = orderDAO.getAdminKPIs();
        List<Product> lowStock = productDAO.getLowStockAlerts(15);
        kpis.put("lowStockProducts", lowStock);
        kpis.put("totalProducts", productDAO.countTotalProducts());
        return kpis;
    }

    public List<Order> getAllOrders() {
        return orderDAO.listAll();
    }

    public boolean updateOrderStatus(int orderId, String newStatus) throws Exception {
        return orderService.updateStatus(orderId, newStatus);
    }

    public List<User> getAllCustomers() {
        List<User> customers = userDAO.listAllCustomers();
        for (User u : customers) {
            u.setPasswordHash(null);
        }
        return customers;
    }
}
