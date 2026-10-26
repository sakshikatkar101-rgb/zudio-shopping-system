/**
 * ZUDIO – Checkout & Realistic Simulated Payment Processing Flow
 */
const Checkout = {
  selectedPayment: 'COD',
  cardState: {
    number: '4532 8901 2345 6789',
    name: 'Aarav Mehta',
    expiry: '10/28',
    cvv: '382',
    brand: 'Visa'
  },

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

    // Default card name from logged in user if available
    if (Auth.user && Auth.user.name) {
      this.cardState.name = Auth.user.name;
    }

    this.render();
  },

  detectBrand(num) {
    const clean = (num || '').replace(/\D/g, '');
    if (/^4/.test(clean)) return 'Visa';
    if (/^(5[1-5]|2[2-7])/.test(clean)) return 'Mastercard';
    if (/^(60|65|81|82)/.test(clean)) return 'RuPay';
    if (/^3[47]/.test(clean)) return 'Amex';
    return 'Card';
  },

  formatCardNumber(val) {
    const digits = val.replace(/\D/g, '').substring(0, 16);
    const groups = digits.match(/.{1,4}/g);
    return groups ? groups.join(' ') : digits;
  },

  formatExpiry(val) {
    const clean = val.replace(/\D/g, '').substring(0, 4);
    if (clean.length >= 3) {
      return clean.substring(0, 2) + '/' + clean.substring(2);
    }
    return clean;
  },

  onCardNumberInput(input) {
    const formatted = this.formatCardNumber(input.value);
    input.value = formatted;
    this.cardState.number = formatted;
    this.cardState.brand = this.detectBrand(formatted);

    // Update Card Preview
    const previewNum = document.getElementById('card-preview-number');
    const previewBrand = document.getElementById('card-preview-brand');
    if (previewNum) {
      const clean = formatted.replace(/\D/g, '');
      if (clean.length > 4) {
        const masked = '•••• •••• •••• ' + clean.slice(-4);
        previewNum.textContent = masked;
      } else {
        previewNum.textContent = formatted || '•••• •••• •••• ••••';
      }
    }
    if (previewBrand) previewBrand.textContent = this.cardState.brand.toUpperCase();
  },

  onCardNameInput(input) {
    const val = input.value.trim() || 'CARDHOLDER NAME';
    this.cardState.name = input.value;
    const previewName = document.getElementById('card-preview-name');
    if (previewName) previewName.textContent = UI.escapeHtml(val.toUpperCase());
  },

  onExpiryInput(input) {
    const formatted = this.formatExpiry(input.value);
    input.value = formatted;
    this.cardState.expiry = formatted;
    const previewExp = document.getElementById('card-preview-expiry');
    if (previewExp) previewExp.textContent = formatted || 'MM/YY';
  },

  setUpiHandle(handle) {
    const input = document.getElementById('chk-upi-id');
    if (!input) return;
    const current = input.value.trim();
    const prefix = current.includes('@') ? current.split('@')[0] : (current || 'username');
    input.value = prefix + handle;
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
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-md);">
                <h3 style="display: flex; align-items: center; gap: 8px;">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--accent-primary)" stroke-width="2"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>
                  2. Select Payment Mode
                </h3>
                <span class="badge-payment badge-paid">⚡ Simulation Mode</span>
              </div>
              
              <div style="display: flex; flex-direction: column; gap: var(--space-sm);">
                <!-- COD -->
                <label style="display: flex; align-items: center; gap: 12px; padding: 14px; background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); cursor: pointer;">
                  <input type="radio" name="chk-pay-method" value="COD" ${this.selectedPayment === 'COD' ? 'checked' : ''} onchange="Checkout.selectPayment('COD')">
                  <div>
                    <div style="font-weight: 700; font-size: 0.95rem;">💵 Cash on Delivery (COD)</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Pay in cash or scan QR when your package arrives at your door.</div>
                  </div>
                </label>

                <!-- UPI -->
                <label style="display: flex; align-items: center; gap: 12px; padding: 14px; background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); cursor: pointer;">
                  <input type="radio" name="chk-pay-method" value="UPI" ${this.selectedPayment === 'UPI' ? 'checked' : ''} onchange="Checkout.selectPayment('UPI')">
                  <div>
                    <div style="font-weight: 700; font-size: 0.95rem;">⚡ UPI Instant Pay (Google Pay / PhonePe / Paytm / BHIM)</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Simulated instant authorization via your Virtual Payment Address (VPA).</div>
                  </div>
                </label>

                <!-- Card -->
                <label style="display: flex; align-items: center; gap: 12px; padding: 14px; background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); cursor: pointer;">
                  <input type="radio" name="chk-pay-method" value="CARD" ${this.selectedPayment === 'CARD' ? 'checked' : ''} onchange="Checkout.selectPayment('CARD')">
                  <div>
                    <div style="font-weight: 700; font-size: 0.95rem;">💳 Credit / Debit Card (Visa, Mastercard, RuPay, Amex)</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Simulated gateway authorization with 256-bit encryption.</div>
                  </div>
                </label>
              </div>

              <!-- Payment details interactive box -->
              <div id="payment-details-box" style="margin-top: 18px;">
                ${this.renderPaymentMethodDetails(this.selectedPayment)}
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
              <div style="font-size: 0.78rem; color: var(--accent-success); margin-top: -4px;">
                🚚 Estimated Delivery: <strong>3–5 Business Days</strong>
              </div>
              <div style="display: flex; justify-content: space-between; font-weight: 800; font-size: 1.25rem; margin-top: 8px; padding-top: 12px; border-top: 1px solid var(--border-medium);">
                <span>Total Amount</span>
                <span style="color: var(--accent-primary); font-family: var(--font-brand);">₹${Number(Cart.data.total || 0).toFixed(2)}</span>
              </div>
            </div>

            <button id="btn-place-order" class="btn btn-primary btn-block" style="margin-top: 24px; padding: 16px;" onclick="Checkout.placeOrder()">
              Place Order Now (₹${Number(Cart.data.total || 0).toFixed(2)})
            </button>

            <!-- Delivery & Refund Guarantee Callout -->
            <div style="margin-top: 16px; padding: 12px; background: var(--bg-surface); border: 1px dashed var(--border-subtle); border-radius: var(--radius-sm); font-size: 0.78rem; color: var(--text-secondary); display: flex; flex-direction: column; gap: 6px;">
              <div>🚚 <strong>Delivery:</strong> Guaranteed doorstep delivery within <strong>3–5 business days</strong>.</div>
              <div>↩️ <strong>Refunds:</strong> Cancel anytime before dispatch; refunds credited in <strong>3–5 business days</strong>.</div>
            </div>

            <div style="text-align: center; margin-top: 12px; font-size: 0.75rem; color: var(--text-muted);">
              🔒 256-Bit SSL Encrypted & ACID DB Transaction Protected
            </div>
          </div>
        </div>
      </div>
    `;
  },

  renderPaymentMethodDetails(method) {
    if (method === 'UPI') {
      return `
        <div style="background: var(--bg-surface); padding: 18px; border-radius: var(--radius-sm); border: 1px solid var(--border-medium);">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
            <label style="font-size: 0.85rem; font-weight: 700; display: block;">Enter Virtual Payment Address (VPA)</label>
            <span style="font-size: 0.75rem; color: var(--accent-primary); font-weight: 600;">Instant Verification</span>
          </div>
          
          <input type="text" id="chk-upi-id" value="aarav.mehta@oksbi" placeholder="username@bank" style="width: 100%; font-family: monospace; font-size: 0.95rem;">
          
          <div style="margin-top: 8px;">
            <span style="font-size: 0.75rem; color: var(--text-muted);">Quick Handles:</span>
            <div class="upi-chips-row">
              <button type="button" class="upi-chip-btn" onclick="Checkout.setUpiHandle('@okhdfcbank')">@okhdfcbank</button>
              <button type="button" class="upi-chip-btn" onclick="Checkout.setUpiHandle('@oksbi')">@oksbi</button>
              <button type="button" class="upi-chip-btn" onclick="Checkout.setUpiHandle('@paytm')">@paytm</button>
              <button type="button" class="upi-chip-btn" onclick="Checkout.setUpiHandle('@ybl')">@ybl</button>
              <button type="button" class="upi-chip-btn" onclick="Checkout.setUpiHandle('@icici')">@icici</button>
            </div>
          </div>

          <div style="margin-top: 14px; padding: 10px 14px; background: rgba(59, 130, 246, 0.08); border-radius: var(--radius-xs); border-left: 3px solid #3B82F6; font-size: 0.78rem; color: var(--text-secondary);">
            💡 <strong>Demo Simulation:</strong> An instant authorization request will be sent to the entered VPA. No actual money will be debited.
          </div>
        </div>
      `;
    }

    if (method === 'CARD') {
      const brand = this.cardState.brand || 'Visa';
      const cleanNum = (this.cardState.number || '').replace(/\D/g, '');
      const masked = cleanNum.length > 4 ? '•••• •••• •••• ' + cleanNum.slice(-4) : '•••• •••• •••• 6789';

      return `
        <div style="background: var(--bg-surface); padding: 18px; border-radius: var(--radius-sm); border: 1px solid var(--border-medium);">
          <!-- Dynamic Card Visual Preview -->
          <div class="card-visual-wrapper">
            <div class="card-visual-top">
              <div class="card-chip-icon"></div>
              <div class="card-brand-tag" id="card-preview-brand">${brand.toUpperCase()}</div>
            </div>
            <div class="card-number-display" id="card-preview-number">${masked}</div>
            <div class="card-visual-bottom">
              <div>
                <div>Card Holder</div>
                <div class="card-holder-name" id="card-preview-name">${UI.escapeHtml((this.cardState.name || 'Aarav Mehta').toUpperCase())}</div>
              </div>
              <div style="text-align: right;">
                <div>Expires</div>
                <div class="card-expiry-val" id="card-preview-expiry">${this.cardState.expiry || '10/28'}</div>
              </div>
            </div>
          </div>

          <!-- Card Input Form -->
          <div style="display: flex; flex-direction: column; gap: 12px;">
            <div>
              <label style="display: block; font-size: 0.8rem; font-weight: 600; margin-bottom: 6px;">Card Number</label>
              <input type="text" id="chk-card-num" value="${this.cardState.number}" placeholder="1234 5678 9012 3456" maxlength="19" oninput="Checkout.onCardNumberInput(this)" style="width: 100%; font-family: monospace;">
            </div>

            <div>
              <label style="display: block; font-size: 0.8rem; font-weight: 600; margin-bottom: 6px;">Cardholder Name</label>
              <input type="text" id="chk-card-name" value="${UI.escapeHtml(this.cardState.name)}" placeholder="Full Name on Card" oninput="Checkout.onCardNameInput(this)" style="width: 100%;">
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
              <div>
                <label style="display: block; font-size: 0.8rem; font-weight: 600; margin-bottom: 6px;">Expiry (MM/YY)</label>
                <input type="text" id="chk-card-exp" value="${this.cardState.expiry}" placeholder="MM/YY" maxlength="5" oninput="Checkout.onExpiryInput(this)" style="width: 100%; font-family: monospace;">
              </div>
              <div>
                <label style="display: block; font-size: 0.8rem; font-weight: 600; margin-bottom: 6px;">CVV / CVC</label>
                <input type="password" id="chk-card-cvv" value="${this.cardState.cvv}" placeholder="•••" maxlength="4" style="width: 100%; font-family: monospace;">
              </div>
            </div>
          </div>

          <div style="margin-top: 14px; padding: 10px 14px; background: rgba(245, 158, 11, 0.08); border-radius: var(--radius-xs); border-left: 3px solid #F59E0B; font-size: 0.78rem; color: var(--text-secondary);">
            🔒 <strong>Demo Payment Simulation:</strong> Do NOT enter real card credentials. Sensitive details are never sent or saved; only last 4 digits & brand are stored.
          </div>
        </div>
      `;
    }

    // COD Default
    return `
      <div style="padding: 16px; background: var(--bg-surface); border-radius: var(--radius-sm); border: 1px dashed var(--border-medium); font-size: 0.85rem; display: flex; align-items: center; gap: 12px;">
        <span style="font-size: 1.5rem;">💵</span>
        <div>
          <div style="font-weight: 700; color: var(--text-main);">Cash on Delivery Selected</div>
          <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 2px;">
            Pay with cash or UPI QR upon doorstep delivery. No advance payment required. Zero handling fees.
          </div>
        </div>
      </div>
    `;
  },

  selectPayment(method) {
    this.selectedPayment = method;
    const box = document.getElementById('payment-details-box');
    if (box) {
      box.innerHTML = this.renderPaymentMethodDetails(method);
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

    let paymentStatus = 'PENDING';
    let cardLast4 = null;
    let cardBrand = null;

    if (this.selectedPayment === 'CARD') {
      const cardNum = document.getElementById('chk-card-num')?.value.trim() || '';
      const cleanDigits = cardNum.replace(/\D/g, '');
      const expiry = document.getElementById('chk-card-exp')?.value.trim() || '';
      const cvv = document.getElementById('chk-card-cvv')?.value.trim() || '';

      if (cleanDigits.length < 15) {
        UI.toast('Please enter a valid simulated card number (15-16 digits).', 'error');
        return;
      }
      if (!/^\d{2}\/\d{2}$/.test(expiry)) {
        UI.toast('Please enter a valid expiry in MM/YY format.', 'error');
        return;
      }
      if (cvv.length < 3) {
        UI.toast('Please enter a 3 or 4 digit CVV code.', 'error');
        return;
      }

      cardLast4 = cleanDigits.slice(-4);
      cardBrand = this.detectBrand(cleanDigits);
      paymentStatus = 'PAID';

    } else if (this.selectedPayment === 'UPI') {
      const upiId = document.getElementById('chk-upi-id')?.value.trim() || '';
      if (!upiId.includes('@') || upiId.length < 5) {
        UI.toast('Please enter a valid Virtual Payment Address (e.g. name@bank).', 'error');
        return;
      }
      paymentStatus = 'PAID';
    } else {
      // COD
      paymentStatus = 'PENDING';
    }

    // Disable button to prevent double-submit
    const btn = document.getElementById('btn-place-order');
    if (btn) btn.disabled = true;

    try {
      // If online simulated payment (CARD or UPI), show realistic processing modal
      if (this.selectedPayment === 'CARD' || this.selectedPayment === 'UPI') {
        const procModal = document.getElementById('payment-processing-modal');
        const procTitle = document.getElementById('pay-proc-title');
        const procDesc = document.getElementById('pay-proc-desc');

        if (procModal) {
          if (procTitle) {
            procTitle.textContent = this.selectedPayment === 'CARD' 
              ? `Authorizing ${cardBrand || 'Card'} Payment...` 
              : 'Verifying UPI Authorization...';
          }
          if (procDesc) {
            procDesc.textContent = this.selectedPayment === 'CARD'
              ? 'Contacting simulated banking authorization network...'
              : 'Waiting for instant confirmation from UPI gateway...';
          }
          UI.openModal('payment-processing-modal');
        }

        // Realistic gateway latency simulation
        await new Promise(r => setTimeout(r, 1200));

        if (procTitle) procTitle.textContent = 'Payment Authorized! 🎉';
        if (procDesc) procDesc.textContent = 'Recording transaction and reserving stock in database...';
        await new Promise(r => setTimeout(r, 350));
      } else {
        UI.toast('Reserving items and locking inventory in database...', 'info');
      }

      // NOTE: Strictly send ONLY cardLast4 and cardBrand! Never send full card number or CVV.
      const payload = {
        paymentMethod: this.selectedPayment,
        address: fullAddress,
        couponCode: Cart.appliedCouponCode || null,
        paymentStatus: paymentStatus,
        cardLast4: cardLast4,
        cardBrand: cardBrand
      };

      const res = await API.post('/orders', payload);

      // Close processing modal if open
      UI.closeModal('payment-processing-modal');

      UI.toast('🎉 Order Placed Successfully!', 'success');
      Cart.appliedCouponCode = '';
      await Cart.loadCart();
      window.Router.navigate('/orders');
    } catch (err) {
      UI.closeModal('payment-processing-modal');
      UI.toast(err.message, 'error');
      if (btn) btn.disabled = false;
    }
  }
};

window.Checkout = Checkout;
