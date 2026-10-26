/**
 * ZUDIO – My Orders History, Cancellation & Interactive Tracking Stepper
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
        <div style="margin-bottom: var(--space-xl); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          <div>
            <span class="section-tag">ACCOUNT DASHBOARD</span>
            <h1>My Orders & Tracking</h1>
          </div>
          <button class="btn btn-secondary btn-sm" onclick="Orders.fetchOrders()">↻ Refresh Orders</button>
        </div>
        <div style="display: flex; flex-direction: column; gap: var(--space-lg);" id="orders-cards-list">
          <div class="skeleton" style="height: 180px; width: 100%;"></div>
          <div class="skeleton" style="height: 180px; width: 100%;"></div>
        </div>
      </div>
    `;

    try {
      const res = await API.get('/orders');
      this.ordersList = res.data || [];
      this.render();
    } catch (e) {
      container.innerHTML = `<div class="container" style="padding: 48px; text-align: center; color: var(--accent-danger);">Error loading orders: ${UI.escapeHtml(e.message)}</div>`;
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
          <p style="margin-top: 8px; color: var(--text-muted);">You haven't placed any orders yet. Upgrade your wardrobe today!</p>
          <button class="btn btn-primary" style="margin-top: 20px;" onclick="window.Router.navigate('/shop')">Start Shopping</button>
        </div>
      `;
      return;
    }

    listEl.innerHTML = this.ordersList.map(order => {
      const steps = ['Placed', 'Packed', 'Shipped', 'Delivered'];
      const currentIdx = steps.indexOf(order.status);
      const isCancelled = order.status === 'Cancelled';
      const canCancel = order.status === 'Placed' || order.status === 'Packed';

      // Payment Status Badge Helper
      let paymentBadgeHtml = '';
      const pStatus = (order.paymentStatus || 'PENDING').toUpperCase();
      if (pStatus === 'PAID') {
        paymentBadgeHtml = `<span class="badge-payment badge-paid">● Paid</span>`;
      } else if (pStatus === 'REFUND_PENDING') {
        paymentBadgeHtml = `<span class="badge-payment badge-refund">● Refund Pending</span>`;
      } else if (pStatus === 'REFUNDED') {
        paymentBadgeHtml = `<span class="badge-payment badge-refunded">● Refunded</span>`;
      } else {
        paymentBadgeHtml = `<span class="badge-payment badge-pending">● ${order.paymentMethod === 'COD' ? 'COD Pending' : 'Payment Pending'}</span>`;
      }

      // Payment Method Display
      let paymentMethodDesc = UI.escapeHtml(order.paymentMethod || 'COD');
      if (order.paymentMethod === 'CARD') {
        paymentMethodDesc = `💳 ${UI.escapeHtml(order.cardBrand || 'Card')} •••• ${UI.escapeHtml(order.cardLast4 || '••••')}`;
      } else if (order.paymentMethod === 'UPI') {
        paymentMethodDesc = `⚡ UPI Instant Pay`;
      } else {
        paymentMethodDesc = `💵 Cash on Delivery`;
      }

      // Timeline / Stepper HTML
      let timelineHtml = '';
      if (isCancelled) {
        const cancelledDateStr = order.cancelledAt 
          ? new Date(order.cancelledAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
          : 'Recently';
        timelineHtml = `
          <div class="cancel-reason-callout" style="margin: 18px 0 12px;">
            <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 6px;">
              <strong style="color: var(--accent-danger); font-size: 0.9rem;">🚫 Order Cancelled on ${UI.escapeHtml(cancelledDateStr)}</strong>
              ${order.paymentMethod !== 'COD' ? '<span class="badge-payment badge-refund">Refund in 3–5 Business Days</span>' : ''}
            </div>
            <div style="color: var(--text-secondary); margin-top: 4px;">
              <strong>Reason:</strong> "${UI.escapeHtml(order.cancelReason || 'Customer requested cancellation')}"
            </div>
            ${order.paymentMethod !== 'COD' ? `
              <div style="font-size: 0.82rem; color: #A78BFA; margin-top: 6px; padding: 6px 12px; background: rgba(139, 92, 246, 0.12); border-radius: 4px;">
                💰 <strong>Refund Timeline:</strong> A full refund of <strong>₹${Number(order.totalAmount).toFixed(2)}</strong> has been initiated to your original ${UI.escapeHtml(order.paymentMethod)} account (simulated). Funds will reflect in your account within <strong>3 to 5 business days</strong>.
              </div>` : `
              <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 4px;">
                💵 <strong>Cash on Delivery:</strong> No advance payment was collected, so no refund is needed.
              </div>`}
          </div>
        `;
      } else {
        timelineHtml = `
          <div style="display: flex; justify-content: space-between; position: relative; margin: 24px 0 16px;">
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

      // Calculate estimated delivery: 4 days from order date
      const orderDate = new Date(order.createdAt);
      const estDeliveryDate = new Date(orderDate.getTime() + 4 * 24 * 60 * 60 * 1000);
      const estDeliveryStr = estDeliveryDate.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });

      let deliveryStatusSubtitle = '';
      if (order.status === 'Delivered') {
        deliveryStatusSubtitle = `<span style="color: var(--accent-success); font-weight: 600;">✅ Delivered successfully</span>`;
      } else if (isCancelled) {
        deliveryStatusSubtitle = `<span style="color: var(--accent-danger);">🚫 Order Cancelled</span>`;
      } else {
        deliveryStatusSubtitle = `<span style="color: var(--accent-success); font-weight: 600;">🚚 Estimated Delivery: <strong>${estDeliveryStr}</strong> (3–5 business days)</span>`;
      }

      return `
        <div style="background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: var(--space-xl); box-shadow: var(--shadow-sm);">
          <!-- Order Card Header -->
          <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px; border-bottom: 1px solid var(--border-subtle); padding-bottom: 16px;">
            <div>
              <div style="display: flex; align-items: center; gap: 10px;">
                <span style="font-family: var(--font-brand); font-size: 1.2rem; font-weight: 800;">Order #${order.id}</span>
                <span class="status-pill status-${order.status}">${order.status}</span>
                ${paymentBadgeHtml}
              </div>
              <div style="font-size: 0.82rem; color: var(--text-muted); margin-top: 4px; display: flex; flex-wrap: wrap; gap: 12px; align-items: center;">
                <span>Placed on: ${new Date(order.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })}</span>
                <span>•</span>
                ${deliveryStatusSubtitle}
              </div>
            </div>

            <div style="display: flex; align-items: center; gap: 14px;">
              <span style="font-family: var(--font-brand); font-weight: 800; font-size: 1.3rem; color: var(--accent-primary);">₹${Number(order.totalAmount).toFixed(2)}</span>
              ${canCancel ? `
                <button class="btn btn-outline btn-sm" style="color: var(--accent-danger); border-color: var(--accent-danger); font-size: 0.8rem; padding: 6px 14px;" onclick="Orders.openCancelModal(${order.id})">
                  Cancel Order
                </button>
              ` : (isCancelled ? '' : '<span style="font-size: 0.75rem; color: var(--text-muted);">Dispatched (Non-cancellable)</span>')}
            </div>
          </div>

          <!-- Timeline Stepper / Cancellation Notice -->
          ${timelineHtml}

          <!-- Order Items -->
          <div style="margin-top: 18px; display: flex; flex-direction: column; gap: 12px; border-top: 1px solid var(--border-subtle); padding-top: 16px;">
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

          <!-- Delivery Address & Payment Summary Bar -->
          <div style="margin-top: 16px; padding: 12px 16px; background: var(--bg-surface); border-radius: var(--radius-sm); font-size: 0.82rem; color: var(--text-secondary); display: flex; justify-content: space-between; flex-wrap: wrap; gap: 10px;">
            <div>📍 <strong>Address:</strong> ${UI.escapeHtml(order.address)}</div>
            <div>💳 <strong>Payment:</strong> ${paymentMethodDesc} (${paymentBadgeHtml})</div>
          </div>
        </div>
      `;
    }).join('');
  },

  openCancelModal(orderId) {
    const order = this.ordersList.find(o => o.id === orderId);
    if (!order) {
      UI.toast('Order not found', 'error');
      return;
    }

    const modalBody = document.getElementById('cancel-order-modal-body');
    if (!modalBody) return;

    const isOnlinePaid = order.paymentMethod !== 'COD';

    modalBody.innerHTML = `
      <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 12px;">
        <span style="font-size: 1.5rem;">⚠️</span>
        <h2 style="font-size: 1.3rem;">Cancel Order #${order.id}</h2>
      </div>

      <p style="color: var(--text-secondary); font-size: 0.9rem; margin-bottom: 14px;">
        Are you sure you want to cancel this order? This action is immediate and cannot be reversed.
      </p>

      <div style="background: var(--bg-surface); border: 1px solid var(--border-medium); border-radius: var(--radius-sm); padding: 14px; margin-bottom: 16px; font-size: 0.85rem;">
        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
          <span style="color: var(--text-muted);">Items:</span>
          <strong>${(order.items || []).length} items (Stock will be restored)</strong>
        </div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
          <span style="color: var(--text-muted);">Total Value:</span>
          <strong>₹${Number(order.totalAmount).toFixed(2)}</strong>
        </div>
        <div style="display: flex; justify-content: space-between;">
          <span style="color: var(--text-muted);">Payment Method:</span>
          <strong>${UI.escapeHtml(order.paymentMethod)}</strong>
        </div>
        ${isOnlinePaid ? `
          <div style="margin-top: 10px; padding: 10px 12px; background: rgba(139, 92, 246, 0.12); border-radius: var(--radius-xs); border-left: 3px solid #8B5CF6; color: var(--text-main); font-size: 0.82rem;">
            💰 <strong>Refund in 3–5 Business Days:</strong> A full refund of ₹${Number(order.totalAmount).toFixed(2)} will be initiated to your ${UI.escapeHtml(order.paymentMethod)} account. It typically reflects in your bank account within <strong>3 to 5 business days</strong>.
          </div>
        ` : `
          <div style="margin-top: 10px; padding: 8px 12px; background: rgba(245, 158, 11, 0.08); border-radius: var(--radius-xs); border-left: 3px solid #F59E0B; color: var(--text-muted); font-size: 0.8rem;">
            💵 <strong>Cash on Delivery:</strong> No payment has been deducted.
          </div>
        `}
      </div>

      <div style="margin-bottom: 20px;">
        <label style="display: block; font-size: 0.8rem; font-weight: 700; margin-bottom: 6px;">Reason for Cancellation</label>
        <select id="modal-cancel-reason" style="width: 100%; margin-bottom: 8px;">
          <option value="Changed my mind">Changed my mind</option>
          <option value="Ordered wrong size or color">Ordered wrong size or color</option>
          <option value="Found better price elsewhere">Found better price elsewhere</option>
          <option value="Delivery time is too long">Delivery time is too long</option>
          <option value="Need to change shipping address">Need to change shipping address</option>
          <option value="Other">Other (specify below)</option>
        </select>
        <input type="text" id="modal-cancel-custom-reason" placeholder="Additional details (optional)..." style="width: 100%; font-size: 0.85rem;">
      </div>

      <div style="display: flex; justify-content: flex-end; gap: 10px;">
        <button class="btn btn-secondary" onclick="UI.closeModal('cancel-order-modal')">Keep My Order</button>
        <button id="btn-confirm-cancel" class="btn btn-primary" style="background: var(--accent-danger); border-color: var(--accent-danger);" onclick="Orders.confirmCancel(${order.id})">
          Confirm Cancellation
        </button>
      </div>
    `;

    UI.openModal('cancel-order-modal');
  },

  async confirmCancel(orderId) {
    const select = document.getElementById('modal-cancel-reason');
    const custom = document.getElementById('modal-cancel-custom-reason');
    const btn = document.getElementById('btn-confirm-cancel');

    let reason = select ? select.value : 'Cancelled by customer';
    if (custom && custom.value.trim()) {
      reason += ' – ' + custom.value.trim();
    }

    if (btn) btn.disabled = true;

    try {
      UI.toast('Processing order cancellation and inventory restoration...', 'info');
      const res = await API.post(`/orders/${orderId}/cancel`, { reason });

      UI.closeModal('cancel-order-modal');
      UI.toast(`Order #${orderId} cancelled successfully. Inventory restored.`, 'success');
      await this.fetchOrders();
    } catch (e) {
      if (btn) btn.disabled = false;
      UI.toast(e.message, 'error');
    }
  }
};

window.Orders = Orders;
