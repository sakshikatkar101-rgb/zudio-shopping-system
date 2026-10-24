/**
 * ZUDIO – Admin Portal, KPIs, Pure SVG Chart, Product CRUD & Order Manager
 */
const Admin = {
  activeTab: 'dashboard',
  dashboardData: null,
  productsList: [],
  ordersList: [],
  customersList: [],

  async init() {
    if (!Auth.isLoggedIn()) {
      UI.toast('Please log in with admin credentials.', 'info');
      Auth.openAuthModal('login');
      window.Router.navigate('/');
      return;
    }

    if (!Auth.isAdmin()) {
      UI.toast('Access denied. Administrator privileges required.', 'error');
      window.Router.navigate('/');
      return;
    }

    this.renderLayout();
    this.switchTab('dashboard');
  },

  renderLayout() {
    const container = document.getElementById('admin-view');
    if (!container) return;

    container.innerHTML = `
      <div class="admin-layout">
        <!-- Sidebar -->
        <aside class="admin-sidebar">
          <div>
            <div style="font-family: var(--font-brand); font-size: 1.15rem; font-weight: 800; color: var(--accent-primary); margin-bottom: 4px;">ZUDIO ADMIN</div>
            <div style="font-size: 0.8rem; color: var(--text-muted);">Management Console</div>
          </div>

          <ul class="admin-sidebar-menu">
            <li class="admin-nav-item active" id="admin-tab-btn-dashboard" onclick="Admin.switchTab('dashboard')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
              Dashboard
            </li>
            <li class="admin-nav-item" id="admin-tab-btn-products" onclick="Admin.switchTab('products')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
              Products
            </li>
            <li class="admin-nav-item" id="admin-tab-btn-orders" onclick="Admin.switchTab('orders')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
              Orders
            </li>
            <li class="admin-nav-item" id="admin-tab-btn-customers" onclick="Admin.switchTab('customers')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
              Customers
            </li>
          </ul>

          <div style="margin-top: auto; border-top: 1px solid var(--border-subtle); padding-top: 16px;">
            <button class="btn btn-secondary btn-sm btn-block" onclick="window.Router.navigate('/')">
              ← Storefront
            </button>
          </div>
        </aside>

        <!-- Main Tab Area -->
        <main class="admin-main" id="admin-main-content">
          <!-- Dynamically populated -->
        </main>
      </div>
    `;
  },

  switchTab(tab) {
    this.activeTab = tab;
    document.querySelectorAll('.admin-nav-item').forEach(el => el.classList.remove('active'));
    const btn = document.getElementById(`admin-tab-btn-${tab}`);
    if (btn) btn.classList.add('active');

    if (tab === 'dashboard') this.loadDashboard();
    else if (tab === 'products') this.loadProducts();
    else if (tab === 'orders') this.loadOrders();
    else if (tab === 'customers') this.loadCustomers();
  },

  // 1. Dashboard Tab
  async loadDashboard() {
    const main = document.getElementById('admin-main-content');
    if (!main) return;

    main.innerHTML = `<div class="skeleton" style="height: 300px; width: 100%;"></div>`;

    try {
      const res = await API.get('/admin/dashboard');
      this.dashboardData = res.data;

      const kpis = this.dashboardData;
      main.innerHTML = `
        <div style="margin-bottom: var(--space-xl); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          <div>
            <h1>Executive Overview</h1>
            <p>Real-time retail performance metrics & inventory velocity</p>
          </div>
          <button class="btn btn-secondary btn-sm" onclick="Admin.loadDashboard()">↻ Refresh</button>
        </div>

        <!-- KPI Cards Grid -->
        <div class="kpi-grid">
          <div class="kpi-card">
            <div>
              <div class="kpi-label">Total Revenue</div>
              <div class="kpi-value" id="kpi-val-rev">₹0</div>
            </div>
            <div class="kpi-icon-box">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
            </div>
          </div>

          <div class="kpi-card">
            <div>
              <div class="kpi-label">Total Orders</div>
              <div class="kpi-value" id="kpi-val-orders">0</div>
            </div>
            <div class="kpi-icon-box" style="background: rgba(0, 245, 212, 0.1); color: var(--accent-secondary);">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
            </div>
          </div>

          <div class="kpi-card">
            <div>
              <div class="kpi-label">Active Customers</div>
              <div class="kpi-value" id="kpi-val-cust">0</div>
            </div>
            <div class="kpi-icon-box" style="background: rgba(255, 183, 3, 0.1); color: var(--accent-gold);">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
            </div>
          </div>

          <div class="kpi-card">
            <div>
              <div class="kpi-label">Low Stock Alerts</div>
              <div class="kpi-value" id="kpi-val-stock" style="color: var(--accent-danger);">0</div>
            </div>
            <div class="kpi-icon-box" style="background: rgba(239, 68, 68, 0.1); color: var(--accent-danger);">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            </div>
          </div>
        </div>

        <!-- Sales Chart Card (Pure SVG/Canvas, No Library) -->
        <div class="chart-container-card">
          <div class="chart-header">
            <div>
              <div class="kpi-label">Revenue Velocity</div>
              <h3>Sales Trend (Past 7 Days)</h3>
            </div>
            <div style="font-size: 0.8rem; color: var(--text-muted);">Rendered dynamically with Pure SVG</div>
          </div>
          <div class="chart-svg-wrap" id="sales-svg-chart-container">
            <!-- Pure SVG generated below -->
          </div>
        </div>

        <!-- Low Stock Items List -->
        <div class="table-card">
          <div class="table-header-bar">
            <div>
              <h3>Critical Inventory Alerts (Stock ≤ 15)</h3>
              <p style="font-size: 0.8rem; color: var(--text-muted); margin-top: 2px;">Restock recommended to prevent catalog dropouts.</p>
            </div>
          </div>
          <div class="data-table-wrapper">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Category</th>
                  <th>Price</th>
                  <th>Units In Stock</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                ${(kpis.lowStockProducts || []).map(p => `
                  <tr>
                    <td>
                      <div style="display: flex; align-items: center; gap: 10px;">
                        <img src="${p.imageUrl}" style="width: 36px; height: 44px; object-fit: cover; border-radius: 4px;" onerror="this.src=UI.getPlaceholderSvg();">
                        <span style="font-weight: 600; color: var(--text-main);">${UI.escapeHtml(p.name)}</span>
                      </div>
                    </td>
                    <td>${UI.escapeHtml(p.categoryName || 'Fashion')}</td>
                    <td style="font-family: var(--font-brand); font-weight: 700;">₹${p.price.toFixed(2)}</td>
                    <td><strong style="color: var(--accent-danger); font-size: 1rem;">${p.stock} units</strong></td>
                    <td>
                      <button class="btn btn-secondary btn-sm" onclick="Admin.quickRestock(${p.id}, ${p.stock})">+ Restock</button>
                    </td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      `;

      // Trigger counter animations
      UI.animateNumber(document.getElementById('kpi-val-rev'), Number(kpis.totalRevenue || 0), '₹');
      UI.animateNumber(document.getElementById('kpi-val-orders'), Number(kpis.totalOrders || 0));
      UI.animateNumber(document.getElementById('kpi-val-cust'), Number(kpis.totalCustomers || 0));
      UI.animateNumber(document.getElementById('kpi-val-stock'), Number(kpis.lowStockCount || 0));

      // Draw Pure SVG Chart
      this.drawPureSvgChart(kpis.salesTrend || []);

    } catch (e) {
      main.innerHTML = `<div style="padding: 32px; color: var(--accent-danger);">Error loading dashboard: ${e.message}</div>`;
    }
  },

  // Pure SVG Line & Gradient Area Chart Drawer
  drawPureSvgChart(trendData) {
    const container = document.getElementById('sales-svg-chart-container');
    if (!container) return;

    // Fallback if data is sparse
    if (!trendData || trendData.length < 2) {
      trendData = [
        { date: 'Oct 18', revenue: 2490 },
        { date: 'Oct 19', revenue: 3890 },
        { date: 'Oct 20', revenue: 5120 },
        { date: 'Oct 21', revenue: 4200 },
        { date: 'Oct 22', revenue: 6890 },
        { date: 'Oct 23', revenue: 7450 },
        { date: 'Oct 24', revenue: 8990 },
      ];
    }

    const width = 800;
    const height = 240;
    const padding = { top: 20, right: 30, bottom: 40, left: 60 };

    const maxRev = Math.max(...trendData.map(d => Number(d.revenue || 0)), 5000) * 1.15;
    const innerWidth = width - padding.left - padding.right;
    const innerHeight = height - padding.top - padding.bottom;

    const points = trendData.map((d, i) => {
      const x = padding.left + (i / (trendData.length - 1)) * innerWidth;
      const y = padding.top + innerHeight - (Number(d.revenue || 0) / maxRev) * innerHeight;
      return { x, y, ...d };
    });

    // Generate smooth bezier curve path
    let pathD = `M ${points[0].x} ${points[0].y}`;
    for (let i = 0; i < points.length - 1; i++) {
      const p0 = points[i];
      const p1 = points[i + 1];
      const mx = (p0.x + p1.x) / 2;
      pathD += ` C ${mx} ${p0.y}, ${mx} ${p1.y}, ${p1.x} ${p1.y}`;
    }

    const areaD = `${pathD} L ${points[points.length - 1].x} ${padding.top + innerHeight} L ${points[0].x} ${padding.top + innerHeight} Z`;

    // Grid lines
    const gridLines = [0, 0.25, 0.5, 0.75, 1.0].map(ratio => {
      const y = padding.top + innerHeight * (1 - ratio);
      const val = Math.round(maxRev * ratio);
      return `
        <line x1="${padding.left}" y1="${y}" x2="${width - padding.right}" y2="${y}" stroke="var(--border-subtle)" stroke-dasharray="4,4"/>
        <text x="${padding.left - 12}" y="${y + 4}" fill="var(--text-muted)" font-size="11" text-anchor="end">₹${val >= 1000 ? (val/1000).toFixed(1) + 'k' : val}</text>
      `;
    }).join('');

    // X Axis Labels & Data points
    const pointsAndLabels = points.map(p => `
      <text x="${p.x}" y="${height - 12}" fill="var(--text-secondary)" font-size="11" text-anchor="middle">${p.date}</text>
      <circle cx="${p.x}" cy="${p.y}" r="5" fill="#FF2E63" stroke="#FFFFFF" stroke-width="2">
        <title>${p.date}: ₹${Number(p.revenue).toFixed(2)}</title>
      </circle>
    `).join('');

    container.innerHTML = `
      <svg class="chart-svg" viewBox="0 0 ${width} ${height}" preserveAspectRatio="none">
        <defs>
          <linearGradient id="chartGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#FF2E63" stop-opacity="0.45"/>
            <stop offset="100%" stop-color="#FF2E63" stop-opacity="0.0"/>
          </linearGradient>
        </defs>
        ${gridLines}
        <path d="${areaD}" fill="url(#chartGradient)"/>
        <path d="${pathD}" fill="none" stroke="#FF2E63" stroke-width="3" stroke-linecap="round"/>
        ${pointsAndLabels}
      </svg>
    `;
  },

  // 2. Products Management Tab
  async loadProducts() {
    const main = document.getElementById('admin-main-content');
    if (!main) return;

    main.innerHTML = `<div class="skeleton" style="height: 300px; width: 100%;"></div>`;

    try {
      const res = await API.get('/admin/products');
      this.productsList = res.data || [];

      main.innerHTML = `
        <div style="margin-bottom: var(--space-xl); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          <div>
            <h1>Product Inventory (${this.productsList.length})</h1>
            <p>Manage style catalog, pricing, inventory stock & imagery</p>
          </div>
          <button class="btn btn-primary" onclick="Admin.openProductModal()">+ Add New Product</button>
        </div>

        <div class="table-card">
          <div class="data-table-wrapper">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Category</th>
                  <th>Price</th>
                  <th>MRP</th>
                  <th>Stock</th>
                  <th>Trending</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                ${this.productsList.map(p => `
                  <tr>
                    <td>
                      <div style="display: flex; align-items: center; gap: 12px;">
                        <img src="${p.imageUrl}" style="width: 44px; height: 54px; object-fit: cover; border-radius: 4px;" onerror="this.src=UI.getPlaceholderSvg();">
                        <div>
                          <div style="font-weight: 600; color: var(--text-main);">${UI.escapeHtml(p.name)}</div>
                          <div style="font-size: 0.75rem; color: var(--text-muted);">SKU: #ZUD-${p.id}</div>
                        </div>
                      </div>
                    </td>
                    <td>${UI.escapeHtml(p.categoryName || 'Fashion')}</td>
                    <td style="font-family: var(--font-brand); font-weight: 700; color: var(--text-main);">₹${p.price.toFixed(2)}</td>
                    <td style="color: var(--text-muted); text-decoration: line-through;">₹${(p.mrp || p.price).toFixed(2)}</td>
                    <td>
                      <span style="font-weight: 700; color: ${p.stock <= 10 ? 'var(--accent-danger)' : 'var(--accent-success)'};">${p.stock}</span>
                    </td>
                    <td>
                      ${p.isTrending ? '<span class="status-pill status-Delivered">Yes</span>' : '<span style="color: var(--text-muted); font-size: 0.8rem;">No</span>'}
                    </td>
                    <td>
                      <div style="display: flex; gap: 6px;">
                        <button class="table-action-btn" onclick="Admin.openProductModal(${p.id})" title="Edit Product">
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                        </button>
                        <button class="table-action-btn delete" onclick="Admin.deleteProduct(${p.id})" title="Delete Product">
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      `;
    } catch (e) {
      main.innerHTML = `<div style="padding: 32px; color: var(--accent-danger);">Error loading products: ${e.message}</div>`;
    }
  },

  // 3. Orders Management Tab
  orderFilter: 'ALL',

  async loadOrders() {
    const main = document.getElementById('admin-main-content');
    if (!main) return;

    main.innerHTML = `<div class="skeleton" style="height: 300px; width: 100%;"></div>`;

    try {
      const res = await API.get('/admin/orders');
      this.ordersList = res.data || [];
      this.renderOrdersTable();
    } catch (e) {
      main.innerHTML = `<div style="padding: 32px; color: var(--accent-danger);">Error loading orders: ${UI.escapeHtml(e.message)}</div>`;
    }
  },

  setOrderFilter(status) {
    this.orderFilter = status;
    this.renderOrdersTable();
  },

  renderOrdersTable() {
    const main = document.getElementById('admin-main-content');
    if (!main) return;

    const filtered = (this.orderFilter === 'ALL')
      ? this.ordersList
      : this.ordersList.filter(o => o.status === this.orderFilter);

    main.innerHTML = `
      <div style="margin-bottom: var(--space-xl); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
        <div>
          <h1>Orders Management (${this.ordersList.length})</h1>
          <p>Track customer fulfillment, update status, and manage simulated refunds</p>
        </div>
        <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
          <!-- Status Filter -->
          <select id="admin-order-filter" class="status-select" style="padding: 8px 12px; font-weight: 600;" onchange="Admin.setOrderFilter(this.value)">
            <option value="ALL" ${this.orderFilter === 'ALL' ? 'selected' : ''}>All Orders (${this.ordersList.length})</option>
            <option value="Placed" ${this.orderFilter === 'Placed' ? 'selected' : ''}>Placed</option>
            <option value="Packed" ${this.orderFilter === 'Packed' ? 'selected' : ''}>Packed</option>
            <option value="Shipped" ${this.orderFilter === 'Shipped' ? 'selected' : ''}>Shipped</option>
            <option value="Delivered" ${this.orderFilter === 'Delivered' ? 'selected' : ''}>Delivered</option>
            <option value="Cancelled" ${this.orderFilter === 'Cancelled' ? 'selected' : ''}>Cancelled</option>
          </select>
          <button class="btn btn-secondary btn-sm" onclick="Admin.loadOrders()">↻ Refresh</button>
        </div>
      </div>

      <div class="table-card">
        <div class="data-table-wrapper">
          <table class="data-table">
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Customer</th>
                <th>Total Amount</th>
                <th>Payment Details</th>
                <th>Fulfillment Status</th>
                <th>Date</th>
                <th>Update Status</th>
              </tr>
            </thead>
            <tbody>
              ${filtered.length === 0 ? `
                <tr><td colspan="7" style="text-align: center; padding: 36px; color: var(--text-muted);">No orders match the selected filter.</td></tr>
              ` : filtered.map(o => {
                // Payment Status Badge
                let pBadge = '';
                const pStatus = (o.paymentStatus || 'PENDING').toUpperCase();
                if (pStatus === 'PAID') {
                  pBadge = '<span class="badge-payment badge-paid">● Paid</span>';
                } else if (pStatus === 'REFUND_PENDING') {
                  pBadge = '<span class="badge-payment badge-refund">● Refund Pending</span>';
                } else if (pStatus === 'REFUNDED') {
                  pBadge = '<span class="badge-payment badge-refunded">● Refunded</span>';
                } else {
                  pBadge = '<span class="badge-payment badge-pending">● Pending</span>';
                }

                // Payment Method Line
                let pMethodLine = UI.escapeHtml(o.paymentMethod || 'COD');
                if (o.paymentMethod === 'CARD') {
                  pMethodLine = `💳 ${UI.escapeHtml(o.cardBrand || 'Card')} •••• ${UI.escapeHtml(o.cardLast4 || '••••')}`;
                } else if (o.paymentMethod === 'UPI') {
                  pMethodLine = `⚡ UPI`;
                } else {
                  pMethodLine = `💵 COD`;
                }

                return `
                  <tr>
                    <td><strong>#${o.id}</strong></td>
                    <td>
                      <div>
                        <div style="font-weight: 600; color: var(--text-main);">${UI.escapeHtml(o.customerName || 'Customer')}</div>
                        <div style="font-size: 0.75rem; color: var(--text-muted);">${UI.escapeHtml(o.customerEmail || '')}</div>
                      </div>
                    </td>
                    <td style="font-family: var(--font-brand); font-weight: 700; color: var(--accent-primary);">₹${Number(o.totalAmount).toFixed(2)}</td>
                    <td>
                      <div style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;">
                        <span style="font-size: 0.8rem; font-weight: 600;">${pMethodLine}</span>
                        ${pBadge}
                      </div>
                    </td>
                    <td>
                      <div style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;">
                        <span class="status-pill status-${o.status}">${UI.escapeHtml(o.status)}</span>
                        ${o.status === 'Cancelled' ? `
                          <div style="font-size: 0.72rem; color: var(--accent-danger); max-width: 180px; line-height: 1.2;">
                            ${UI.escapeHtml(o.cancelReason || 'Cancelled')}
                          </div>
                        ` : ''}
                      </div>
                    </td>
                    <td style="font-size: 0.8rem; color: var(--text-muted);">${new Date(o.createdAt).toLocaleDateString()}</td>
                    <td>
                      <select class="status-select" onchange="Admin.updateOrderStatus(${o.id}, this.value, '${o.status}')">
                        ${['Placed', 'Packed', 'Shipped', 'Delivered', 'Cancelled'].map(s => `
                          <option value="${s}" ${s === o.status ? 'selected' : ''}>${s}</option>
                        `).join('')}
                      </select>
                    </td>
                  </tr>
                `;
              }).join('')}
            </tbody>
          </table>
        </div>
      </div>
    `;
  },

  async updateOrderStatus(orderId, newStatus, prevStatus) {
    if (newStatus === prevStatus) return;

    if (newStatus === 'Cancelled') {
      const confirmCancel = confirm(`Are you sure you want to cancel Order #${orderId}?\nThis will immediately restore stock for all items in the order back to inventory.`);
      if (!confirmCancel) {
        this.renderOrdersTable();
        return;
      }
    }

    try {
      UI.toast(`Updating Order #${orderId} to ${newStatus}...`, 'info');
      await API.put(`/admin/orders/${orderId}`, { status: newStatus });
      UI.toast(`Order #${orderId} status updated to ${newStatus}`, 'success');
      await this.loadOrders();
    } catch (e) {
      UI.toast(e.message, 'error');
      this.renderOrdersTable();
    }
  },

  // 4. Customers Directory Tab
  async loadCustomers() {
    const main = document.getElementById('admin-main-content');
    if (!main) return;

    try {
      const res = await API.get('/admin/customers');
      this.customersList = res.data || [];

      main.innerHTML = `
        <div style="margin-bottom: var(--space-xl);">
          <h1>Customer Directory (${this.customersList.length})</h1>
          <p>Registered customer profiles and account details</p>
        </div>

        <div class="table-card">
          <div class="data-table-wrapper">
            <table class="data-table">
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>Customer Name</th>
                  <th>Email Address</th>
                  <th>Phone</th>
                  <th>Registered On</th>
                </tr>
              </thead>
              <tbody>
                ${this.customersList.map(c => `
                  <tr>
                    <td>#${c.id}</td>
                    <td style="font-weight: 600; color: var(--text-main);">${UI.escapeHtml(c.name)}</td>
                    <td>${UI.escapeHtml(c.email)}</td>
                    <td>${UI.escapeHtml(c.phone || '—')}</td>
                    <td>${new Date(c.createdAt).toLocaleDateString()}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      `;
    } catch (e) {
      main.innerHTML = `<div style="padding: 32px; color: var(--accent-danger);">Error: ${e.message}</div>`;
    }
  },

  // Product Add / Edit Modal
  openProductModal(productId = null) {
    const isEdit = !!productId;
    const p = isEdit ? this.productsList.find(item => item.id === productId) : null;

    const modalBody = document.getElementById('admin-product-modal-body');
    if (!modalBody) return;

    modalBody.innerHTML = `
      <h2 style="margin-bottom: var(--space-lg);">${isEdit ? 'Edit Fashion Product' : 'Add New Fashion Drop'}</h2>
      <form id="admin-product-form" onsubmit="event.preventDefault(); Admin.saveProduct(${productId || 'null'});">
        <div style="display: flex; flex-direction: column; gap: var(--space-md);">
          <div>
            <label style="display: block; font-size: 0.8rem; font-weight: 700; margin-bottom: 6px;">Product Name</label>
            <input type="text" id="adm-p-name" required value="${p ? p.name : ''}" placeholder="e.g. Acid Wash Oversized Graphic Tee" style="width: 100%;">
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div>
              <label style="display: block; font-size: 0.8rem; font-weight: 700; margin-bottom: 6px;">Price (₹)</label>
              <input type="number" step="0.01" id="adm-p-price" required value="${p ? p.price : ''}" placeholder="499.00" style="width: 100%;">
            </div>
            <div>
              <label style="display: block; font-size: 0.8rem; font-weight: 700; margin-bottom: 6px;">MRP (₹)</label>
              <input type="number" step="0.01" id="adm-p-mrp" value="${p ? p.mrp : ''}" placeholder="799.00" style="width: 100%;">
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div>
              <label style="display: block; font-size: 0.8rem; font-weight: 700; margin-bottom: 6px;">Category</label>
              <select id="adm-p-cat" style="width: 100%;">
                <option value="1" ${p && p.categoryId === 1 ? 'selected' : ''}>Men</option>
                <option value="2" ${p && p.categoryId === 2 ? 'selected' : ''}>Women</option>
                <option value="3" ${p && p.categoryId === 3 ? 'selected' : ''}>Kids</option>
                <option value="4" ${p && p.categoryId === 4 ? 'selected' : ''}>Footwear</option>
                <option value="5" ${p && p.categoryId === 5 ? 'selected' : ''}>Accessories</option>
              </select>
            </div>
            <div>
              <label style="display: block; font-size: 0.8rem; font-weight: 700; margin-bottom: 6px;">Stock Quantity</label>
              <input type="number" id="adm-p-stock" required value="${p ? p.stock : 25}" placeholder="25" style="width: 100%;">
            </div>
          </div>

          <div>
            <label style="display: block; font-size: 0.8rem; font-weight: 700; margin-bottom: 6px;">High-Resolution Image URL</label>
            <input type="url" id="adm-p-image" required value="${p ? p.imageUrl : 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=800&q=80'}" placeholder="https://..." style="width: 100%;">
          </div>

          <div>
            <label style="display: block; font-size: 0.8rem; font-weight: 700; margin-bottom: 6px;">Description</label>
            <textarea id="adm-p-desc" rows="3" placeholder="Enter fabric details, fit, and style guidance..." style="width: 100%;">${p ? p.description : ''}</textarea>
          </div>

          <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
            <input type="checkbox" id="adm-p-trending" ${p && p.isTrending ? 'checked' : ''}>
            <span style="font-weight: 600; font-size: 0.85rem;">Feature in Trending / Drops Carousel</span>
          </label>

          <button type="submit" class="btn btn-primary btn-block" style="margin-top: 12px;">
            ${isEdit ? 'Update Product' : 'Add to Catalog'}
          </button>
        </div>
      </form>
    `;

    UI.openModal('admin-product-modal');
  },

  async saveProduct(productId) {
    const isEdit = !!productId;
    const name = document.getElementById('adm-p-name').value.trim();
    const price = parseFloat(document.getElementById('adm-p-price').value);
    const mrp = parseFloat(document.getElementById('adm-p-mrp').value) || price;
    const categoryId = parseInt(document.getElementById('adm-p-cat').value);
    const stock = parseInt(document.getElementById('adm-p-stock').value) || 0;
    const imageUrl = document.getElementById('adm-p-image').value.trim();
    const description = document.getElementById('adm-p-desc').value.trim();
    const isTrending = document.getElementById('adm-p-trending').checked;

    const payload = { name, price, mrp, categoryId, stock, imageUrl, description, isTrending };

    try {
      if (isEdit) {
        await API.put(`/admin/products/${productId}`, payload);
        UI.toast('Product updated successfully!', 'success');
      } else {
        await API.post('/admin/products', payload);
        UI.toast('New product added to catalog!', 'success');
      }
      UI.closeModal('admin-product-modal');
      this.loadProducts();
    } catch (e) {
      UI.toast(e.message, 'error');
    }
  },

  async deleteProduct(productId) {
    if (!confirm('Are you sure you want to delete this product? This action cannot be undone.')) return;
    try {
      await API.delete(`/admin/products/${productId}`);
      UI.toast('Product deleted from catalog.', 'info');
      this.loadProducts();
    } catch (e) {
      UI.toast(e.message, 'error');
    }
  },

  async quickRestock(productId, currentStock) {
    const addStr = prompt(`Current stock is ${currentStock}. How many units would you like to add?`, "20");
    if (!addStr) return;
    const add = parseInt(addStr);
    if (isNaN(add) || add <= 0) return;

    try {
      const p = this.dashboardData.lowStockProducts.find(item => item.id === productId);
      if (p) {
        p.stock = currentStock + add;
        await API.put(`/admin/products/${productId}`, p);
        UI.toast(`Restocked! New quantity: ${p.stock}`, 'success');
        this.loadDashboard();
      }
    } catch (e) {
      UI.toast(e.message, 'error');
    }
  }
};

window.Admin = Admin;
