/**
 * ZUDIO – Authentication & Session State Management
 */
const Auth = {
  user: null,

  async init() {
    const savedUser = localStorage.getItem('zudio_user');
    if (savedUser) {
      try {
        this.user = JSON.parse(savedUser);
      } catch (e) {
        this.user = null;
      }
    }
    this.updateUI();

    // Quick evaluation autologin parameter: ?autologin=admin or ?autologin=customer
    const urlParams = new URLSearchParams(window.location.search);
    const hashParams = new URLSearchParams(window.location.hash.includes('?') ? window.location.hash.split('?')[1] : '');
    const autoRole = urlParams.get('autologin') || hashParams.get('autologin');
    if (autoRole === 'admin' && !this.isAdmin()) {
      await this.login('admin@zudio.in', 'admin123');
    } else if (autoRole === 'customer' && !this.isLoggedIn()) {
      await this.login('sakshi@example.com', 'pass123');
    }

    // Verify token validity with server
    const token = localStorage.getItem('zudio_token');
    if (token) {
      API.get('/auth/me')
        .then(res => {
          if (res.data) {
            this.user = res.data;
            localStorage.setItem('zudio_user', JSON.stringify(res.data));
            this.updateUI();
          }
        })
        .catch(() => {
          this.logout(false);
        });
    }

    window.addEventListener('auth:expired', () => {
      UI.toast('Session expired. Please log in again.', 'info');
      this.logout(false);
    });
  },

  isLoggedIn() {
    return !!this.user && !!localStorage.getItem('zudio_token');
  },

  isAdmin() {
    return this.isLoggedIn() && this.user.role === 'ADMIN';
  },

  async login(email, password) {
    try {
      const res = await API.post('/auth/login', { email, password });
      localStorage.setItem('zudio_token', res.data.token);
      localStorage.setItem('zudio_user', JSON.stringify(res.data.user));
      this.user = res.data.user;
      this.updateUI();
      UI.closeModal('auth-modal');
      UI.toast(`Welcome back, ${this.user.name}!`, 'success');

      if (this.isAdmin() && (window.location.hash.startsWith('#/admin') || window.location.search.includes('admin'))) {
        window.Router.navigate('/admin');
        window.Router.handleRoute();
      } else {
        window.Cart.loadCart();
      }
      return true;
    } catch (err) {
      UI.toast(err.message, 'error');
      return false;
    }
  },

  async register(name, email, password, phone) {
    try {
      const res = await API.post('/auth/register', { name, email, password, phone });
      localStorage.setItem('zudio_token', res.data.token);
      localStorage.setItem('zudio_user', JSON.stringify(res.data.user));
      this.user = res.data.user;
      this.updateUI();
      UI.closeModal('auth-modal');
      UI.toast(`Account created! Welcome to Zudio, ${this.user.name}.`, 'success');
      window.Cart.loadCart();
      return true;
    } catch (err) {
      UI.toast(err.message, 'error');
      return false;
    }
  },

  async logout(notify = true) {
    try {
      if (localStorage.getItem('zudio_token')) {
        await API.post('/auth/logout').catch(() => {});
      }
    } finally {
      localStorage.removeItem('zudio_token');
      localStorage.removeItem('zudio_user');
      this.user = null;
      this.updateUI();
      if (notify) UI.toast('You have been logged out.', 'info');
      if (window.location.hash.startsWith('#/admin') || window.location.hash.startsWith('#/orders') || window.location.hash.startsWith('#/checkout')) {
        window.Router.navigate('/');
      }
    }
  },

  updateUI() {
    const authActionBtn = document.getElementById('nav-auth-btn');
    const profileDropdown = document.getElementById('nav-profile-dropdown');
    const adminNavLinks = document.querySelectorAll('.admin-only-link');

    if (this.isLoggedIn()) {
      if (authActionBtn) authActionBtn.style.display = 'none';
      if (profileDropdown) {
        profileDropdown.style.display = 'block';
        const nameEl = profileDropdown.querySelector('.dropdown-user-name');
        const emailEl = profileDropdown.querySelector('.dropdown-user-email');
        if (nameEl) nameEl.textContent = this.user.name;
        if (emailEl) emailEl.textContent = this.user.email;
      }
      adminNavLinks.forEach(el => el.style.display = this.isAdmin() ? 'flex' : 'none');
    } else {
      if (authActionBtn) authActionBtn.style.display = 'flex';
      if (profileDropdown) profileDropdown.style.display = 'none';
      adminNavLinks.forEach(el => el.style.display = 'none');
    }
  },

  openAuthModal(tab = 'login') {
    UI.openModal('auth-modal');
    this.switchTab(tab);
  },

  switchTab(tab) {
    const loginTabBtn = document.getElementById('tab-login-btn');
    const regTabBtn = document.getElementById('tab-register-btn');
    const loginForm = document.getElementById('login-form');
    const regForm = document.getElementById('register-form');

    if (tab === 'login') {
      loginTabBtn?.classList.add('active');
      regTabBtn?.classList.remove('active');
      if (loginForm) loginForm.style.display = 'block';
      if (regForm) regForm.style.display = 'none';
    } else {
      regTabBtn?.classList.add('active');
      loginTabBtn?.classList.remove('active');
      if (regForm) regForm.style.display = 'block';
      if (loginForm) loginForm.style.display = 'none';
    }
  }
};

window.Auth = Auth;
