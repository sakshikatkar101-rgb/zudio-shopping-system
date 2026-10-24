/**
 * ZUDIO – Shopping Bag / Cart Manager & Slide-in Drawer
 */
const Cart = {
  data: {
    items: [],
    subtotal: 0,
    mrpTotal: 0,
    discount: 0,
    shipping: 0,
    total: 0,
    coupon: null,
  },
  appliedCouponCode: '',

  init() {
    this.loadCart();
  },

  async loadCart() {
    if (!Auth.isLoggedIn()) {
      this.updateBadge(0);
      return;
    }
    try {
      const res = await API.get('/cart', { coupon: this.appliedCouponCode });
      if (res.data) {
        this.data = res.data;
        this.updateBadge(this.data.totalItems || 0);
        this.renderDrawer();
      }
    } catch (err) {
      console.warn('Could not load cart:', err.message);
    }
  },

  async addItem(productId, size = 'M', quantity = 1) {
    if (!Auth.isLoggedIn()) {
      UI.toast('Please log in to add items to your shopping bag.', 'info');
      Auth.openAuthModal('login');
      return;
    }

    try {
      const res = await API.post('/cart', { productId, size, quantity });
      this.data = res.data;
      this.updateBadge(this.data.totalItems || 0);
      this.renderDrawer();
      this.openDrawer();
      UI.toast('Added to shopping bag!', 'success');
    } catch (err) {
      UI.toast(err.message, 'error');
    }
  },

  async updateQuantity(cartItemId, newQuantity) {
    try {
      const res = await API.put(`/cart/${cartItemId}`, { quantity: newQuantity });
      this.data = res.data;
      this.updateBadge(this.data.totalItems || 0);
      this.renderDrawer();
    } catch (err) {
      UI.toast(err.message, 'error');
    }
  },

  async removeItem(cartItemId) {
    try {
      const res = await API.delete(`/cart/${cartItemId}`);
      this.data = res.data;
      this.updateBadge(this.data.totalItems || 0);
      this.renderDrawer();
      UI.toast('Item removed from bag.', 'info');
    } catch (err) {
      UI.toast(err.message, 'error');
    }
  },

  async applyCoupon(code) {
    if (!code || !code.trim()) {
      UI.toast('Please enter a coupon code.', 'error');
      return;
    }
    const cleanCode = code.trim().toUpperCase();
    try {
      const res = await API.post('/coupons/validate', {
        code: cleanCode,
        orderAmount: this.data.subtotal
      });

      if (res.valid) {
        this.appliedCouponCode = cleanCode;
        UI.toast(res.message, 'success');
        this.loadCart();
      } else {
        UI.toast(res.message, 'error');
      }
    } catch (err) {
      UI.toast(err.message, 'error');
    }
  },

  removeCoupon() {
    this.appliedCouponCode = '';
    UI.toast('Coupon removed.', 'info');
    this.loadCart();
  },

  openDrawer() {
    const overlay = document.getElementById('cart-drawer-overlay');
    if (overlay) {
      overlay.classList.add('open');
      document.body.style.overflow = 'hidden';
      this.renderDrawer();
    }
  },

  closeDrawer() {
    const overlay = document.getElementById('cart-drawer-overlay');
    if (overlay) {
      overlay.classList.remove('open');
      document.body.style.overflow = '';
    }
  },

  updateBadge(count) {
    const badges = document.querySelectorAll('.cart-badge-count');
    badges.forEach(b => {
      b.textContent = count;
      b.style.display = count > 0 ? 'flex' : 'none';
      b.style.transform = 'scale(1.25)';
      setTimeout(() => b.style.transform = 'scale(1)', 200);
    });
  },

  renderDrawer() {
    const body = document.getElementById('cart-drawer-body');
    const footer = document.getElementById('cart-drawer-footer');
    if (!body || !footer) return;

    if (!Auth.isLoggedIn()) {
      body.innerHTML = `
        <div style="text-align: center; padding: 48px 16px;">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="color: var(--text-muted); margin-bottom: 16px;"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
          <h3 style="margin-bottom: 8px;">Your Bag is Empty</h3>
          <p style="margin-bottom: 24px;">Sign in to view your bag and saved items.</p>
          <button class="btn btn-primary" onclick="Cart.closeDrawer(); Auth.openAuthModal('login');">Sign In</button>
        </div>
      `;
      footer.style.display = 'none';
      return;
    }

    if (!this.data.items || this.data.items.length === 0) {
      body.innerHTML = `
        <div style="text-align: center; padding: 48px 16px;">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="color: var(--text-muted); margin-bottom: 16px;"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
          <h3 style="margin-bottom: 8px;">Your Bag is Empty</h3>
          <p style="margin-bottom: 24px;">Discover the freshest drops and add styles to your bag.</p>
          <button class="btn btn-primary" onclick="Cart.closeDrawer(); window.Router.navigate('/shop');">Explore Drops</button>
        </div>
      `;
      footer.style.display = 'none';
      return;
    }

    footer.style.display = 'flex';

    // Free delivery progress
    const freeDeliveryThreshold = 999;
    const progress = Math.min((this.data.subtotal / freeDeliveryThreshold) * 100, 100);
    const amountLeft = Math.max(freeDeliveryThreshold - this.data.subtotal, 0);

    let progressHtml = `
      <div style="background: var(--bg-surface); padding: 12px; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); margin-bottom: 8px;">
        <div style="display: flex; justify-content: space-between; font-size: 0.8rem; margin-bottom: 6px;">
          <span>${amountLeft > 0 ? `Add <strong style="color: var(--accent-primary);">₹${amountLeft.toFixed(0)}</strong> for FREE Express Delivery` : '🎉 You unlocked <strong style="color: var(--accent-success);">FREE Express Delivery!</strong>'}</span>
          <span>${progress.toFixed(0)}%</span>
        </div>
        <div style="height: 6px; background: var(--border-medium); border-radius: 99px; overflow: hidden;">
          <div style="width: ${progress}%; height: 100%; background: var(--accent-primary); transition: width 0.3s ease;"></div>
        </div>
      </div>
    `;

    body.innerHTML = progressHtml + this.data.items.map(item => {
      const safeName = UI.escapeHtml(item.productName);
      const safeSize = UI.escapeHtml(item.size);

      return `
      <div class="cart-item-row">
        <img class="cart-item-img" src="${item.productImageUrl}" alt="${safeName}" onerror="this.src=UI.getPlaceholderSvg();">
        <div style="display: flex; flex-direction: column; justify-content: space-between;">
          <div>
            <div style="font-size: 0.9rem; font-weight: 600; line-height: 1.3; margin-bottom: 4px;">${safeName}</div>
            <div style="font-size: 0.75rem; color: var(--text-muted); display: flex; gap: 8px;">
              <span>Size: <strong>${safeSize}</strong></span>
              <span>•</span>
              <span style="color: var(--accent-success); font-weight: 600;">In Stock</span>
            </div>
          </div>
          <div class="quantity-stepper">
            <button onclick="Cart.updateQuantity(${item.id}, ${item.quantity - 1})">−</button>
            <span>${item.quantity}</span>
            <button onclick="Cart.updateQuantity(${item.id}, ${item.quantity + 1})">+</button>
          </div>
        </div>
        <div style="display: flex; flex-direction: column; align-items: flex-end; justify-content: space-between;">
          <button style="color: var(--text-muted); padding: 4px;" onclick="Cart.removeItem(${item.id})" title="Remove item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          </button>
          <div style="text-align: right;">
            <div style="font-family: var(--font-brand); font-weight: 700; font-size: 1rem;">₹${(item.productPrice * item.quantity).toFixed(2)}</div>
            ${item.productMrp ? `<div style="font-size: 0.75rem; color: var(--text-muted); text-decoration: line-through;">₹${(item.productMrp * item.quantity).toFixed(2)}</div>` : ''}
          </div>
        </div>
      </div>
    `;
    }).join('');

    // Footer summary
    const safeCouponCode = UI.escapeHtml(this.appliedCouponCode);
    const safeCouponDisplay = UI.escapeHtml(this.data.coupon?.code || 'Promo');

    footer.innerHTML = `
      <div style="display: flex; gap: 8px;">
        <input type="text" id="cart-coupon-input" placeholder="Coupon (e.g. ZUDIO10)" value="${safeCouponCode}" style="flex: 1; text-transform: uppercase;">
        ${this.appliedCouponCode 
          ? `<button class="btn btn-secondary btn-sm" onclick="Cart.removeCoupon()">Remove</button>` 
          : `<button class="btn btn-secondary btn-sm" onclick="Cart.applyCoupon(document.getElementById('cart-coupon-input').value)">Apply</button>`}
      </div>

      <div style="display: flex; flex-direction: column; gap: 6px; font-size: 0.9rem; padding: 4px 0;">
        <div style="display: flex; justify-content: space-between;">
          <span style="color: var(--text-secondary);">Subtotal</span>
          <span>₹${Number(this.data.subtotal || 0).toFixed(2)}</span>
        </div>
        ${Number(this.data.discount || 0) > 0 ? `
          <div style="display: flex; justify-content: space-between; color: var(--accent-success);">
            <span>Discount (${safeCouponDisplay})</span>
            <span>-₹${Number(this.data.discount).toFixed(2)}</span>
          </div>` : ''}
        <div style="display: flex; justify-content: space-between;">
          <span style="color: var(--text-secondary);">Shipping</span>
          <span>${Number(this.data.shipping) === 0 ? '<strong style="color: var(--accent-success);">FREE</strong>' : '₹' + Number(this.data.shipping).toFixed(2)}</span>
        </div>
        <div style="display: flex; justify-content: space-between; font-weight: 700; font-size: 1.15rem; margin-top: 6px; padding-top: 8px; border-top: 1px solid var(--border-subtle);">
          <span>Estimated Total</span>
          <span style="color: var(--accent-primary); font-family: var(--font-brand);">₹${Number(this.data.total || 0).toFixed(2)}</span>
        </div>
      </div>

      <button class="btn btn-primary btn-block" onclick="Cart.closeDrawer(); window.Router.navigate('/checkout');">
        Proceed to Checkout
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
      </button>
    `;
  }
};

window.Cart = Cart;
