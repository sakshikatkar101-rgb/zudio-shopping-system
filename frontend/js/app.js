/**
 * ZUDIO – Main Application Bootstrap, Home View, & Global Interactivity
 */

// Home View Controller
const Home = {
  async init() {
    await Promise.all([
      this.loadCategoryTiles(),
      this.loadTrendingProducts(),
    ]);
  },

  async loadCategoryTiles() {
    const container = document.getElementById('home-category-grid');
    if (!container) return;

    try {
      const res = await API.get('/categories');
      const categories = res.data || [];

      container.innerHTML = categories.map(c => `
        <div class="category-card" onclick="Shop.setCategory('${UI.escapeHtml(c.name)}'); window.Router.navigate('/shop');">
          <img src="${c.imageUrl}" alt="${UI.escapeHtml(c.name)}" onerror="this.src=UI.getPlaceholderSvg('${encodeURIComponent(c.name)}');">
          <div class="category-card-overlay">
            <div class="category-card-title">${UI.escapeHtml(c.name)}</div>
            <div class="category-card-subtitle">${UI.escapeHtml(c.description || 'Explore trendy styles')}</div>
          </div>
        </div>
      `).join('');
    } catch (e) {
      console.warn('Home categories failed:', e.message);
    }
  },

  async loadTrendingProducts() {
    const grid = document.getElementById('home-trending-grid');
    if (!grid) return;

    try {
      const res = await API.get('/products', { trending: true, sortBy: 'popularity' });
      const trending = (res.data || []).slice(0, 8);

      grid.innerHTML = trending.map(p => {
        const isFav = Shop.wishlistIds.has(p.id);
        const discountPct = p.mrp > p.price ? Math.round(((p.mrp - p.price) / p.mrp) * 100) : 0;
        const safeName = UI.escapeHtml(p.name);
        const safeCat = UI.escapeHtml(p.categoryName || 'Fashion');

        return `
          <div class="product-card">
            <div class="product-image-wrap" onclick="Shop.openProductDetail(${p.id})">
              <img src="${p.imageUrl}" alt="${safeName}" loading="lazy" onerror="this.src=UI.getPlaceholderSvg('${encodeURIComponent(p.categoryName || 'ZUDIO')}');">
              <span class="product-tag-pill">HOT DROP</span>
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
                  <span style="color: var(--accent-gold); font-weight: 700;">★ ${p.rating || '4.5'}</span>
                  <span>${p.stock <= 5 ? `<strong style="color: var(--accent-danger);">Only ${p.stock} left!</strong>` : 'In Stock'}</span>
                </div>
              </div>
            </div>
          </div>
        `;
      }).join('');
    } catch (e) {
      grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center;">Could not load trending drops.</div>`;
    }
  }
};

// Wishlist View Controller
const Wishlist = {
  items: [],

  async init() {
    if (!Auth.isLoggedIn()) {
      UI.toast('Please log in to view your saved wishlist.', 'info');
      Auth.openAuthModal('login');
      window.Router.navigate('/shop');
      return;
    }
    await this.load();
  },

  async load() {
    const grid = document.getElementById('wishlist-product-grid');
    if (!grid) return;

    try {
      const res = await API.get('/wishlist');
      this.items = res.data || [];
      this.render();
    } catch (e) {
      grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center;">Error loading wishlist: ${e.message}</div>`;
    }
  },

  render() {
    const grid = document.getElementById('wishlist-product-grid');
    if (!grid) return;

    if (this.items.length === 0) {
      grid.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 64px 16px;">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="color: var(--text-muted); margin-bottom: 16px;"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          <h3>Your Wishlist is Empty</h3>
          <p style="margin-top: 8px;">Save the styles you love to check back anytime.</p>
          <button class="btn btn-primary" style="margin-top: 20px;" onclick="window.Router.navigate('/shop')">Browse Drops</button>
        </div>
      `;
      return;
    }

    grid.innerHTML = this.items.map(item => `
      <div class="product-card">
        <div class="product-image-wrap" onclick="Shop.openProductDetail(${item.productId})">
          <img src="${item.productImageUrl}" alt="${item.productName}" onerror="this.src=UI.getPlaceholderSvg();">
          <button class="wishlist-heart-btn active" onclick="event.stopPropagation(); Wishlist.remove(${item.productId});" title="Remove from wishlist">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          </button>
        </div>
        <div class="product-info">
          <h4 class="product-title" onclick="Shop.openProductDetail(${item.productId})">${item.productName}</h4>
          <div class="product-price-row">
            <span class="price-current">₹${item.productPrice.toFixed(2)}</span>
            ${item.productMrp ? `<span class="price-mrp">₹${item.productMrp.toFixed(2)}</span>` : ''}
          </div>
          <button class="btn btn-primary btn-sm btn-block" style="margin-top: 12px;" onclick="Cart.addItem(${item.productId}, 'M', 1); Wishlist.remove(${item.productId});">
            Move to Bag
          </button>
        </div>
      </div>
    `).join('');
  },

  async remove(productId) {
    try {
      await API.delete(`/wishlist/${productId}`);
      Shop.wishlistIds.delete(productId);
      UI.toast('Item removed from wishlist.', 'info');
      this.load();
    } catch (e) {
      UI.toast(e.message, 'error');
    }
  }
};

window.Home = Home;
window.Wishlist = Wishlist;

// Global App Initialization
document.addEventListener('DOMContentLoaded', async () => {
  UI.initTheme();
  await Auth.init();
  Cart.init();
  Router.init();

  // Glassmorphism Navbar shrink/blur on scroll
  const header = document.querySelector('.site-header');
  window.addEventListener('scroll', () => {
    if (window.scrollY > 30) {
      header?.classList.add('scrolled');
    } else {
      header?.classList.remove('scrolled');
    }
  });

  // Search Debounce in Header & Shop
  const shopSearchInput = document.getElementById('shop-search-input');
  let searchTimeout = null;
  shopSearchInput?.addEventListener('input', (e) => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      Shop.handleSearch(e.target.value);
    }, 300);
  });

  // Close modals on Escape key
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-overlay.open').forEach(m => m.classList.remove('open'));
      Cart.closeDrawer();
      document.body.style.overflow = '';
    }
  });

  // Close modals when clicking backdrop
  document.querySelectorAll('.modal-overlay').forEach(modal => {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        modal.classList.remove('open');
        document.body.style.overflow = '';
      }
    });
  });

  console.log('[ZUDIO] Frontend SPA Initialized successfully.');
});
