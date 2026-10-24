/**
 * ZUDIO – Checkout & Simulated Payment Processing
 */
const Checkout = {
  selectedPayment: 'COD',

  async init() {
    if (!Auth.isLoggedIn()) {
      UI.toast('Please log in to proceed with checkout.', 'info');
      Auth.openAuthModal('login');
      window.Router.navigate('/shop');
      return;
    }

    await Cart.loadCart();

    if (!Cart.data.items || Cart.data.items.length === 0) {
      UI.toast('Your shopping bag is empty. Please add items before checkout.', 'info');
      window.Router.navigate('/shop');
      return;
    }

    this.render();
  },

  render() {
    const container = document.getElementById('checkout-view');
    if (!container) return;

    const user = Auth.user || {};
    const safeName = UI.escapeHtml(user.name || '');
    const safePhone = UI.escapeHtml(user.phone || '+91 98765 43210');

    container.innerHTML = `
      <div class="container" style="padding-top: var(--space-2xl); padding-bottom: var(--space-3xl);">
        <div style="margin-bottom: var(--space-xl);">
          <span class="section-tag">SECURE CHECKOUT</span>
          <h1>Complete Your Order</h1>
        </div>

        <div style="display: grid; grid-template-columns: 1.25fr 0.75fr; gap: var(--space-2xl); align-items: start;">
          <!-- Form column -->
          <div style="display: flex; flex-direction: column; gap: var(--space-xl);">
            <!-- 1. Delivery Address Card -->
            <div style="background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: var(--space-xl);">
              <h3 style="margin-bottom: var(--space-md); display: flex; align-items: center; gap: 8px;">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--accent-primary)" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                1. Delivery Details
              </h3>
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-md);">
                <div>
                  <label style="display: block; font-size: 0.8rem; font-weight: 600; margin-bottom: 6px;">Receiver Name</label>
                  <input type="text" id="chk-name" value="${safeName}" placeholder="Full Name" style="width: 100%;">
                </div>
                <div>
                  <label style="display: block; font-size: 0.8rem; font-weight: 600; margin-bottom: 6px;">Contact Phone</label>
                  <input type="tel" id="chk-phone" value="${safePhone}" placeholder="+91 98765 43210" style="width: 100%;">
                </div>
                <div style="grid-column: 1/-1;">
                  <label style="display: block; font-size: 0.8rem; font-weight: 600; margin-bottom: 6px;">Street Address / Flat No.</label>
                  <textarea id="chk-address" rows="3" placeholder="Flat No., Wing, Apartment, Street name" style="width: 100%;">Flat 402, Sunshine Heights, MG Road</textarea>
                </div>
                <div>
                  <label style="display: block; font-size: 0.8rem; font-weight: 600; margin-bottom: 6px;">City</label>
                  <input type="text" id="chk-city" value="Bengaluru" style="width: 100%;">
                </div>
                <div>
                  <label style="display: block; font-size: 0.8rem; font-weight: 600; margin-bottom: 6px;">PIN Code</label>
                  <input type="text" id="chk-pin" value="560001" style="width: 100%;">
                </div>
              </div>
            </div>

            <!-- 2. Payment Method Card -->
            <div style="background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: var(--space-xl);">
              <h3 style="margin-bottom: var(--space-md); display: flex; align-items: center; gap: 8px;">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--accent-primary)" stroke-width="2"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>
                2. Select Payment Mode (Simulated)
              </h3>
              
              <div style="display: flex; flex-direction: column; gap: var(--space-sm);">
                <!-- COD -->
                <label style="display: flex; align-items: center; gap: 12px; padding: 14px; background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); cursor: pointer;">
                  <input type="radio" name="chk-pay-method" value="COD" checked onchange="Checkout.selectPayment('COD')">
                  <div>
                    <div style="font-weight: 700; font-size: 0.95rem;">Cash on Delivery (COD)</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Pay securely in cash or UPI upon delivery at your doorstep.</div>
                  </div>
                </label>

                <!-- UPI -->
                <label style="display: flex; align-items: center; gap: 12px; padding: 14px; background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); cursor: pointer;">
                  <input type="radio" name="chk-pay-method" value="UPI" onchange="Checkout.selectPayment('UPI')">
                  <div>
                    <div style="font-weight: 700; font-size: 0.95rem;">UPI / Instant QR</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Scan via Google Pay, PhonePe, Paytm, or BHIM.</div>
                  </div>
                </label>

                <!-- Card -->
                <label style="display: flex; align-items: center; gap: 12px; padding: 14px; background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); cursor: pointer;">
                  <input type="radio" name="chk-pay-method" value="CARD" onchange="Checkout.selectPayment('CARD')">
                  <div>
                    <div style="font-weight: 700; font-size: 0.95rem;">Credit / Debit Card</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Simulated Visa, Mastercard, RuPay processing.</div>
                  </div>
                </label>
              </div>

              <!-- Payment details preview box -->
              <div id="payment-details-box" style="margin-top: 16px; padding: 14px; background: var(--bg-surface); border-radius: var(--radius-sm); border: 1px dashed var(--border-medium); font-size: 0.85rem;">
                💵 Cash on Delivery selected. Zero extra charges.
              </div>
            </div>
          </div>

          <!-- Order Summary sidebar -->
          <div style="background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: var(--space-xl); position: sticky; top: 96px;">
            <h3 style="margin-bottom: var(--space-md);">Order Summary</h3>
            
            <div style="max-height: 240px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; margin-bottom: var(--space-md);">
              ${Cart.data.items.map(i => {
                const safeItemName = UI.escapeHtml(i.productName);
                const safeItemSize = UI.escapeHtml(i.size);
                return `
                <div style="display: flex; justify-content: space-between; align-items: center; font-size: 0.85rem;">
                  <div style="display: flex; gap: 8px; align-items: center;">
                    <img src="${i.productImageUrl}" style="width: 40px; height: 48px; object-fit: cover; border-radius: 4px;" onerror="this.src=UI.getPlaceholderSvg();">
                    <div>
                      <div style="font-weight: 600; max-width: 160px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${safeItemName}</div>
                      <div style="color: var(--text-muted);">Size: ${safeItemSize} × ${i.quantity}</div>
                    </div>
                  </div>
                  <div style="font-weight: 700;">₹${(i.productPrice * i.quantity).toFixed(2)}</div>
                </div>
              `;
              }).join('')}
            </div>

            <div style="border-top: 1px solid var(--border-subtle); padding-top: 12px; display: flex; flex-direction: column; gap: 8px; font-size: 0.9rem;">
              <div style="display: flex; justify-content: space-between; color: var(--text-secondary);">
                <span>Subtotal</span>
                <span>₹${Number(Cart.data.subtotal || 0).toFixed(2)}</span>
              </div>
              ${Number(Cart.data.discount || 0) > 0 ? `
                <div style="display: flex; justify-content: space-between; color: var(--accent-success);">
                  <span>Discount (${Cart.data.coupon?.code || 'Coupon'})</span>
                  <span>-₹${Number(Cart.data.discount).toFixed(2)}</span>
                </div>` : ''}
              <div style="display: flex; justify-content: space-between; color: var(--text-secondary);">
                <span>Express Delivery</span>
                <span>${Number(Cart.data.shipping) === 0 ? '<strong style="color: var(--accent-success);">FREE</strong>' : '₹' + Number(Cart.data.shipping).toFixed(2)}</span>
              </div>
              <div style="display: flex; justify-content: space-between; font-weight: 800; font-size: 1.25rem; margin-top: 8px; padding-top: 12px; border-top: 1px solid var(--border-medium);">
                <span>Total Amount</span>
                <span style="color: var(--accent-primary); font-family: var(--font-brand);">₹${Number(Cart.data.total || 0).toFixed(2)}</span>
              </div>
            </div>

            <button class="btn btn-primary btn-block" style="margin-top: 24px; padding: 16px;" onclick="Checkout.placeOrder()">
              Place Order Now (₹${Number(Cart.data.total || 0).toFixed(2)})
            </button>
            <div style="text-align: center; margin-top: 12px; font-size: 0.75rem; color: var(--text-muted);">
              🔒 256-Bit SSL Encrypted & ACID DB Transaction Protected
            </div>
          </div>
        </div>
      </div>
    `;
  },

  selectPayment(method) {
    this.selectedPayment = method;
    const box = document.getElementById('payment-details-box');
    if (!box) return;

    if (method === 'UPI') {
      box.innerHTML = `
        <div style="display: flex; align-items: center; gap: 14px;">
          <div style="width: 70px; height: 70px; background: white; padding: 6px; border-radius: 6px; display: flex; align-items: center; justify-content: center;">
            <svg width="60" height="60" viewBox="0 0 24 24"><path fill="#000" d="M3 3h6v6H3V3zm2 2v2h2V5H5zm8-2h6v6h-6V3zm2 2v2h2V5h-2zM3 13h6v6H3v-6zm2 2v2h2v-2H5zm10 0h2v2h-2v-2zm-2 2h2v2h-2v-2zm4 0h2v2h-2v-2zm-2-4h2v2h-2v-2zm4 4h2v2h-2v-2zM7 7h1v1H7V7zm10 0h1v1h-1V7zM7 17h1v1H7v-1z"/></svg>
          </div>
          <div>
            <div style="font-weight: 700; color: var(--accent-primary);">Simulated UPI Instant Gateway</div>
            <div style="color: var(--text-muted);">Auto-verifies instantly upon clicking "Place Order Now".</div>
          </div>
        </div>
      `;
    } else if (method === 'CARD') {
      box.innerHTML = `
        <div style="display: flex; flex-direction: column; gap: 8px;">
          <input type="text" placeholder="Card Number (e.g. 4532 •••• •••• 8890)" value="4532 8901 2345 6789" style="width: 100%;">
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
            <input type="text" placeholder="MM/YY" value="09/28">
            <input type="password" placeholder="CVV" value="382">
          </div>
        </div>
      `;
    } else {
      box.innerHTML = `💵 Cash on Delivery selected. Zero extra charges.`;
    }
  },

  async placeOrder() {
    const name = document.getElementById('chk-name')?.value.trim();
    const phone = document.getElementById('chk-phone')?.value.trim();
    const street = document.getElementById('chk-address')?.value.trim();
    const city = document.getElementById('chk-city')?.value.trim();
    const pin = document.getElementById('chk-pin')?.value.trim();

    if (!name || !street || !city || !pin) {
      UI.toast('Please complete all delivery address fields.', 'error');
      return;
    }

    const fullAddress = `${name}, Ph: ${phone}, ${street}, ${city} - ${pin}`;

    try {
      UI.toast('Verifying stock and placing order in database...', 'info');

      const res = await API.post('/orders', {
        paymentMethod: this.selectedPayment,
        address: fullAddress,
        couponCode: Cart.appliedCouponCode || null,
      });

      UI.toast('🎉 Order Placed Successfully!', 'success');
      Cart.appliedCouponCode = '';
      await Cart.loadCart();
      window.Router.navigate('/orders');
    } catch (err) {
      UI.toast(err.message, 'error');
    }
  }
};

window.Checkout = Checkout;
