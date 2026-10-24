/**
 * ZUDIO – My Orders History & Interactive Order Tracking Stepper
 */
const Orders = {
  ordersList: [],

  async init() {
    if (!Auth.isLoggedIn()) {
      UI.toast('Please log in to view your orders.', 'info');
      Auth.openAuthModal('login');
      window.Router.navigate('/shop');
      return;
    }
    await this.fetchOrders();
  },

  async fetchOrders() {
    const container = document.getElementById('orders-view');
    if (!container) return;

    container.innerHTML = `
      <div class="container" style="padding-top: var(--space-2xl); padding-bottom: var(--space-3xl);">
        <div style="margin-bottom: var(--space-xl);">
          <span class="section-tag">ACCOUNT DASHBOARD</span>
          <h1>My Orders & Tracking</h1>
        </div>
        <div style="display: flex; flex-direction: column; gap: var(--space-lg);" id="orders-cards-list">
          <div class="skeleton" style="height: 160px; width: 100%;"></div>
          <div class="skeleton" style="height: 160px; width: 100%;"></div>
        </div>
      </div>
    `;

    try {
      const res = await API.get('/orders');
      this.ordersList = res.data || [];
      this.render();
    } catch (e) {
      container.innerHTML = `<div class="container" style="padding: 48px; text-align: center;">Error loading orders: ${e.message}</div>`;
    }
  },

  render() {
    const listEl = document.getElementById('orders-cards-list');
    if (!listEl) return;

    if (this.ordersList.length === 0) {
      listEl.innerHTML = `
        <div style="background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: 64px 16px; text-align: center;">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="color: var(--text-muted); margin-bottom: 16px;"><path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/><path d="m3.3 7 8.7 5 8.7-5"/><path d="M12 22V12"/></svg>
          <h3>No Orders Yet</h3>
          <p style="margin-top: 8px;">You haven't placed any orders yet. Upgrade your wardrobe today!</p>
          <button class="btn btn-primary" style="margin-top: 20px;" onclick="window.Router.navigate('/shop')">Start Shopping</button>
        </div>
      `;
      return;
    }

    listEl.innerHTML = this.ordersList.map(order => {
      const steps = ['Placed', 'Packed', 'Shipped', 'Delivered'];
      const currentIdx = steps.indexOf(order.status);
      const isCancelled = order.status === 'Cancelled';

      let timelineHtml = '';
      if (isCancelled) {
        timelineHtml = `
          <div style="padding: 12px; background: rgba(239, 68, 68, 0.1); border-radius: var(--radius-sm); border: 1px solid var(--accent-danger); color: var(--accent-danger); font-weight: 700; font-size: 0.9rem; text-align: center;">
            ⚠️ This order was cancelled.
          </div>
        `;
      } else {
        timelineHtml = `
          <div style="display: flex; justify-content: space-between; position: relative; margin: 24px 0 12px;">
            <div style="position: absolute; top: 14px; left: 24px; right: 24px; height: 3px; background: var(--border-medium); z-index: 1;">
              <div style="width: ${Math.max(0, (currentIdx / (steps.length - 1)) * 100)}%; height: 100%; background: var(--accent-primary); transition: width 0.4s ease;"></div>
            </div>
            ${steps.map((s, idx) => {
              const done = idx <= currentIdx;
              return `
                <div style="position: relative; z-index: 2; display: flex; flex-direction: column; align-items: center; gap: 6px;">
                  <div style="width: 30px; height: 30px; border-radius: 99px; background: ${done ? 'var(--accent-primary)' : 'var(--bg-surface)'}; border: 2px solid ${done ? 'var(--accent-primary)' : 'var(--border-medium)'}; display: flex; align-items: center; justify-content: center; font-size: 0.75rem; font-weight: 700; color: #FFFFFF;">
                    ${done ? '✓' : idx + 1}
                  </div>
                  <span style="font-size: 0.75rem; font-weight: ${idx === currentIdx ? '700' : '500'}; color: ${done ? 'var(--text-main)' : 'var(--text-muted)'};">${s}</span>
                </div>
              `;
            }).join('')}
          </div>
        `;
      }

      return `
        <div style="background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: var(--space-xl); box-shadow: var(--shadow-sm);">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px; border-bottom: 1px solid var(--border-subtle); padding-bottom: 16px;">
            <div>
              <div style="font-family: var(--font-brand); font-size: 1.15rem; font-weight: 800;">Order #${order.id}</div>
              <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 2px;">
                Placed on: ${new Date(order.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
              </div>
            </div>
            <div style="display: flex; align-items: center; gap: 12px;">
              <span class="status-pill status-${order.status}">${order.status}</span>
              <span style="font-family: var(--font-brand); font-weight: 800; font-size: 1.2rem; color: var(--accent-primary);">₹${Number(order.totalAmount).toFixed(2)}</span>
            </div>
          </div>

          <!-- Timeline Stepper -->
          ${timelineHtml}

          <!-- Order Items -->
          <div style="margin-top: 20px; display: flex; flex-direction: column; gap: 12px; border-top: 1px solid var(--border-subtle); padding-top: 16px;">
            ${(order.items || []).map(item => `
              <div style="display: flex; justify-content: space-between; align-items: center; font-size: 0.85rem;">
                <div style="display: flex; gap: 12px; align-items: center;">
                  <img src="${item.productImageUrl}" style="width: 44px; height: 52px; object-fit: cover; border-radius: 4px;" onerror="this.src=UI.getPlaceholderSvg();">
                  <div>
                    <div style="font-weight: 600; color: var(--text-main);">${UI.escapeHtml(item.productName)}</div>
                    <div style="color: var(--text-muted); font-size: 0.8rem;">Size: ${UI.escapeHtml(item.size)} • Qty: ${item.quantity}</div>
                  </div>
                </div>
                <div style="font-weight: 700; font-family: var(--font-brand);">₹${(item.price * item.quantity).toFixed(2)}</div>
              </div>
            `).join('')}
          </div>

          <!-- Delivery Address & Payment Mode -->
          <div style="margin-top: 16px; padding: 12px; background: var(--bg-surface); border-radius: var(--radius-sm); font-size: 0.8rem; color: var(--text-secondary); display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px;">
            <div>📍 <strong>Delivery Address:</strong> ${UI.escapeHtml(order.address)}</div>
            <div>💳 <strong>Payment Method:</strong> ${UI.escapeHtml(order.paymentMethod)}</div>
          </div>
        </div>
      `;
    }).join('');
  }
};

window.Orders = Orders;
