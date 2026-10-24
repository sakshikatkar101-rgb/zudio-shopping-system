/**
 * ZUDIO – Vanilla Single-Page-App (SPA) Hash Router
 */
const Router = {
  routes: {},
  currentRoute: '',

  init() {
    window.addEventListener('hashchange', () => this.handleRoute());
    this.handleRoute();
  },

  navigate(hash) {
    if (!hash.startsWith('#')) {
      hash = '#' + hash;
    }
    window.location.hash = hash;
  },

  handleRoute() {
    const rawHash = window.location.hash || '#/';
    const cleanHash = rawHash.split('?')[0];

    // Scroll to top smoothly
    window.scrollTo({ top: 0, behavior: 'smooth' });

    // Hide all view containers
    const views = document.querySelectorAll('.app-view');
    views.forEach(v => {
      v.style.display = 'none';
      v.classList.remove('view-transition');
    });

    // Update active nav links
    document.querySelectorAll('.nav-link').forEach(link => {
      link.classList.remove('active');
      const href = link.getAttribute('href');
      if (href === cleanHash || (cleanHash === '#/' && href === '#/home')) {
        link.classList.add('active');
      }
    });

    if (cleanHash === '#/' || cleanHash === '#/home') {
      this.showView('home-view');
      window.Home?.init();
    } else if (cleanHash === '#/shop') {
      this.showView('shop-view');
      window.Shop?.init();
    } else if (cleanHash.startsWith('#/product/')) {
      const id = parseInt(cleanHash.replace('#/product/', ''));
      this.showView('shop-view');
      window.Shop?.init().then(() => {
        if (!isNaN(id)) window.Shop?.openProductDetail(id);
      });
    } else if (cleanHash === '#/cart') {
      window.Cart?.openDrawer();
      if (!this.currentRoute || this.currentRoute === '#/cart') {
        this.showView('home-view');
      }
    } else if (cleanHash === '#/checkout') {
      this.showView('checkout-view');
      window.Checkout?.init();
    } else if (cleanHash === '#/orders') {
      this.showView('orders-view');
      window.Orders?.init();
    } else if (cleanHash === '#/wishlist') {
      this.showView('wishlist-view');
      window.Wishlist?.init();
    } else if (cleanHash.startsWith('#/admin')) {
      this.showView('admin-view');
      window.Admin?.init();
    } else {
      this.showView('home-view');
      window.Home?.init();
    }

    this.currentRoute = cleanHash;
  },

  showView(viewId) {
    const el = document.getElementById(viewId);
    if (el) {
      el.style.display = 'block';
      el.classList.add('view-transition');
    }
  }
};

window.Router = Router;
