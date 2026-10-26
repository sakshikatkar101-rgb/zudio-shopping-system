/**
 * ZUDIO – Shop Catalog, Filtering, Search, & Product Detail Modal
 */
const Shop = {
  products: [],
  categories: [],
  wishlistIds: new Set(),
  activeFilters: {
    category: 'all',
    search: '',
    minPrice: '',
    maxPrice: '',
    sortBy: 'popularity',
    trending: false,
  },
  currentDetailProduct: null,
  selectedDetailSize: 'M',
  selectedDetailQty: 1,

  async init() {
    await Promise.all([
      this.loadCategories(),
      this.loadWishlistIds(),
    ]);
    await this.fetchAndRender();
  },

  async loadCategories() {
    try {
      const res = await API.get('/categories');
      if (res.data) {
        this.categories = res.data;
        this.renderCategoryPills();
      }
    } catch (e) {
      console.warn('Categories load error:', e.message);
    }
  },

  async loadWishlistIds() {
    if (!Auth.isLoggedIn()) {
      this.wishlistIds.clear();
      return;
    }
    try {
      const res = await API.get('/wishlist');
      if (res.data) {
        this.wishlistIds = new Set(res.data.map(item => item.productId));
      }
    } catch (e) {
      this.wishlistIds.clear();
    }
  },

  async fetchAndRender() {
    const grid = document.getElementById('shop-product-grid');
    if (!grid) return;

    // Render skeleton placeholders while loading
    grid.innerHTML = Array(8).fill(0).map(() => `
      <div class="product-card">
        <div class="skeleton" style="height: 330px; width: 100%;"></div>
        <div style="padding: 16px; display: flex; flex-direction: column; gap: 8px;">
          <div class="skeleton" style="height: 12px; width: 40%;"></div>
          <div class="skeleton" style="height: 18px; width: 90%;"></div>
          <div class="skeleton" style="height: 24px; width: 50%;"></div>
        </div>
      </div>
    `).join('');

    try {
      const params = {};
      if (this.activeFilters.category && this.activeFilters.category !== 'all') {
        params.category = this.activeFilters.category;
      }
      if (this.activeFilters.search) {
        params.search = this.activeFilters.search;
      }
      if (this.activeFilters.minPrice) {
        params.minPrice = this.activeFilters.minPrice;
      }
      if (this.activeFilters.maxPrice) {
        params.maxPrice = this.activeFilters.maxPrice;
      }
      if (this.activeFilters.sortBy) {
        params.sortBy = this.activeFilters.sortBy;
      }
      if (this.activeFilters.trending) {
        params.trending = true;
      }

      const res = await API.get('/products', params);
      this.products = res.data || [];
      this.renderProductGrid();
    } catch (err) {
      grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 48px;">Failed to load catalog. ${err.message}</div>`;
    }
  },

  renderCategoryPills() {
    const container = document.getElementById('filter-categories-container');
    if (!container) return;

    let html = `<button class="filter-pill ${this.activeFilters.category === 'all' ? 'active' : ''}" onclick="Shop.setCategory('all')">All Drops</button>`;
    this.categories.forEach(c => {
      const isActive = this.activeFilters.category.toLowerCase() === c.name.toLowerCase();
      const safeName = UI.escapeHtml(c.name);
      html += `<button class="filter-pill ${isActive ? 'active' : ''}" onclick="Shop.setCategory('${safeName}')">${safeName}</button>`;
    });
    container.innerHTML = html;
  },

  setCategory(categoryName) {
    this.activeFilters.category = categoryName;
    this.renderCategoryPills();
    this.fetchAndRender();
  },

  handleSearch(query) {
    this.activeFilters.search = query;
    this.fetchAndRender();
  },

  handleSort(sortBy) {
    this.activeFilters.sortBy = sortBy;
    this.fetchAndRender();
  },

  handlePriceFilter(min, max) {
    this.activeFilters.minPrice = min;
    this.activeFilters.maxPrice = max;
    this.fetchAndRender();
  },

  renderProductGrid() {
    const grid = document.getElementById('shop-product-grid');
    if (!grid) return;

    if (this.products.length === 0) {
      grid.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 64px 16px;">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="color: var(--text-muted); margin-bottom: 16px;"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <h3>No styles found</h3>
          <p style="margin-top: 8px;">Try adjusting your filters, price range, or search terms.</p>
          <button class="btn btn-secondary btn-sm" style="margin-top: 16px;" onclick="Shop.resetFilters()">Clear Filters</button>
        </div>
      `;
      return;
    }

    grid.innerHTML = this.products.map(p => {
      const isFav = this.wishlistIds.has(p.id);
      const discountPct = p.mrp > p.price ? Math.round(((p.mrp - p.price) / p.mrp) * 100) : 0;
      const safeName = UI.escapeHtml(p.name);
      const safeCat = UI.escapeHtml(p.categoryName || 'Fashion');

      return `
        <div class="product-card" data-id="${p.id}">
          <div class="product-image-wrap" onclick="Shop.openProductDetail(${p.id})">
            <img src="${p.imageUrl}" alt="${safeName}" loading="lazy" onerror="this.src=UI.getPlaceholderSvg('${encodeURIComponent(p.categoryName || 'ZUDIO')}');">
            ${p.isTrending ? `<span class="product-tag-pill">TRENDING</span>` : ''}
            <button class="wishlist-heart-btn ${isFav ? 'active' : ''}" onclick="event.stopPropagation(); Shop.toggleWishlist(${p.id}, this);" title="Wishlist">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="${isFav ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
            </button>
            <div class="quick-add-overlay" onclick="event.stopPropagation();">
              <button class="btn btn-primary btn-sm btn-block" onclick="Shop.quickAdd(${p.id})">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                Quick Add (M)
              </button>
            </div>
          </div>
          <div class="product-info">
            <div>
              <span class="product-category-name">${safeCat}</span>
              <h4 class="product-title" onclick="Shop.openProductDetail(${p.id})">${safeName}</h4>
            </div>
            <div>
              <div class="product-price-row">
                <span class="price-current">₹${p.price.toFixed(2)}</span>
                ${p.mrp ? `<span class="price-mrp">₹${p.mrp.toFixed(2)}</span>` : ''}
                ${discountPct > 0 ? `<span class="price-discount-tag">${discountPct}% OFF</span>` : ''}
              </div>
              <div style="display: flex; align-items: center; justify-content: space-between; margin-top: 6px; font-size: 0.8rem; color: var(--text-muted);">
                <span style="display: flex; align-items: center; gap: 4px; color: var(--accent-gold); font-weight: 700;">
                  ★ ${p.rating || '4.5'}
                </span>
                <span>${p.stock <= 5 ? `<strong style="color: var(--accent-danger);">Only ${p.stock} left!</strong>` : 'In Stock'}</span>
              </div>
            </div>
          </div>
        </div>
      `;
    }).join('');
  },

  async toggleWishlist(productId, btnEl) {
    if (!Auth.isLoggedIn()) {
      UI.toast('Please log in to save items to your wishlist.', 'info');
      Auth.openAuthModal('login');
      return;
    }
    try {
      const res = await API.post('/wishlist', { productId });
      if (res.data && res.data.wishlisted) {
        this.wishlistIds.add(productId);
        if (btnEl) {
          btnEl.classList.add('active');
          btnEl.querySelector('svg').setAttribute('fill', 'currentColor');
        }
        UI.toast('Added to your wishlist!', 'success');
      } else {
        this.wishlistIds.delete(productId);
        if (btnEl) {
          btnEl.classList.remove('active');
          btnEl.querySelector('svg').setAttribute('fill', 'none');
        }
        UI.toast('Removed from wishlist.', 'info');
      }
    } catch (e) {
      UI.toast(e.message, 'error');
    }
  },

  quickAdd(productId) {
    Cart.addItem(productId, 'M', 1);
  },

  async openProductDetail(productId) {
    try {
      const res = await API.get(`/products/${productId}`);
      const p = res.data;
      if (!p) return;

      this.currentDetailProduct = p;
      this.selectedDetailSize = 'M';
      this.selectedDetailQty = 1;

      const modalContent = document.getElementById('product-detail-modal-body');
      if (!modalContent) return;

      const isFav = this.wishlistIds.has(p.id);
      const discountPct = p.mrp > p.price ? Math.round(((p.mrp - p.price) / p.mrp) * 100) : 0;
      const safeName = UI.escapeHtml(p.name);
      const safeCat = UI.escapeHtml(p.categoryName || 'Apparel');
      const safeDesc = UI.escapeHtml(p.description || 'Premium everyday staple crafted with superior comfort, modern tailored fit, and signature Zudio aesthetics.');

      modalContent.innerHTML = `
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-xl); align-items: start;">
          <div style="position: relative; border-radius: var(--radius-md); overflow: hidden; background: var(--bg-card); max-height: 480px;">
            <img src="${p.imageUrl}" alt="${safeName}" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.src=UI.getPlaceholderSvg();">
            ${p.isTrending ? `<span class="product-tag-pill">TRENDING</span>` : ''}
          </div>
          <div style="display: flex; flex-direction: column; gap: var(--space-md);">
            <div>
              <span class="product-category-name">${safeCat}</span>
              <h2 style="font-size: 1.6rem; margin: 4px 0 8px;">${safeName}</h2>
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="color: var(--accent-gold); font-weight: 700;">★ ${p.rating || '4.5'} Rating</span>
                <span>•</span>
                <span style="color: var(--text-muted); font-size: 0.85rem;">Verified Retail Quality</span>
              </div>
            </div>

            <div class="product-price-row" style="margin: 0;">
              <span class="price-current" style="font-size: 1.85rem;">₹${p.price.toFixed(2)}</span>
              ${p.mrp ? `<span class="price-mrp" style="font-size: 1.1rem;">₹${p.mrp.toFixed(2)}</span>` : ''}
              ${discountPct > 0 ? `<span class="price-discount-tag" style="font-size: 0.9rem;">${discountPct}% OFF</span>` : ''}
            </div>

            <p style="font-size: 0.9rem; color: var(--text-secondary); line-height: 1.6;">${safeDesc}</p>

            <!-- Size Selector -->
            <div>
              <label style="display: block; font-size: 0.85rem; font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Select Size</label>
              <div style="display: flex; gap: 8px;" id="detail-size-pills">
                ${['S', 'M', 'L', 'XL', 'XXL'].map(s => `
                  <button class="filter-pill ${s === 'M' ? 'active' : ''}" style="width: 44px; text-align: center; padding: 8px 0;" onclick="Shop.setDetailSize('${s}', this)">${s}</button>
                `).join('')}
              </div>
            </div>

            <!-- Quantity & Actions -->
            <div style="display: flex; gap: var(--space-md); align-items: center; margin-top: 8px;">
              <div class="quantity-stepper" style="height: 48px;">
                <button style="width: 38px; height: 100%;" onclick="Shop.adjustDetailQty(-1)">−</button>
                <span id="detail-qty-val" style="width: 42px; font-size: 1rem;">1</span>
                <button style="width: 38px; height: 100%;" onclick="Shop.adjustDetailQty(1)">+</button>
              </div>

              <button class="btn btn-primary" style="flex: 1; height: 48px;" onclick="Shop.addDetailToCart()">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
                Add to Bag
              </button>

              <button class="nav-icon-btn ${isFav ? 'active' : ''}" style="width: 48px; height: 48px;" onclick="Shop.toggleWishlist(${p.id}, this)" title="Add to Wishlist">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="${isFav ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
              </button>
            </div>

            <!-- Features / Shipping badges -->
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 8px; font-size: 0.75rem; color: var(--text-muted); border-top: 1px solid var(--border-subtle); padding-top: 12px;">
              <div>⚡ Free Delivery on ₹999+</div>
              <div>🚚 Delivery in 3–5 Business Days</div>
              <div>↩️ Easy Cancellation & Refund (3–5 Days)</div>
              <div>💳 Cash on Delivery Available</div>
            </div>
          </div>
        </div>
      `;

      UI.openModal('product-detail-modal');
    } catch (e) {
      UI.toast(e.message, 'error');
    }
  },

  setDetailSize(size, el) {
    this.selectedDetailSize = size;
    document.querySelectorAll('#detail-size-pills button').forEach(b => b.classList.remove('active'));
    el.classList.add('active');
  },

  adjustDetailQty(delta) {
    this.selectedDetailQty = Math.max(1, this.selectedDetailQty + delta);
    const valEl = document.getElementById('detail-qty-val');
    if (valEl) valEl.textContent = this.selectedDetailQty;
  },

  addDetailToCart() {
    if (!this.currentDetailProduct) return;
    Cart.addItem(this.currentDetailProduct.id, this.selectedDetailSize, this.selectedDetailQty);
    UI.closeModal('product-detail-modal');
  },

  resetFilters() {
    this.activeFilters = {
      category: 'all',
      search: '',
      minPrice: '',
      maxPrice: '',
      sortBy: 'popularity',
      trending: false,
    };
    const searchInput = document.getElementById('shop-search-input');
    if (searchInput) searchInput.value = '';
    this.renderCategoryPills();
    this.fetchAndRender();
  }
};

window.Shop = Shop;
