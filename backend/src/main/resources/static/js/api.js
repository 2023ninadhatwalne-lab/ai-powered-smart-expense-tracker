// Unified API Client for AI Smart Expense Tracker

const API_BASE_URL = window.location.origin.includes('localhost:8080') || window.location.origin.includes('127.0.0.1:8080') 
    ? '/api' 
    : 'http://localhost:8080/api';

const API = {
    getToken() {
        return localStorage.getItem('token');
    },

    setAuth(data) {
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({
            id: data.id,
            username: data.username,
            email: data.email,
            fullName: data.fullName,
            monthlyBudget: data.monthlyBudget
        }));
    },

    getUser() {
        const user = localStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    },

    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = 'login.html';
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    requireAuth() {
        if (!this.isAuthenticated()) {
            window.location.href = 'login.html';
        }
    },

    async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            const response = await fetch(url, {
                ...options,
                headers
            });

            if (response.status === 401 || response.status === 403) {
                // If on login page, let login fail gracefully without loop
                if (!window.location.pathname.endsWith('login.html')) {
                    this.logout();
                }
            }

            const data = await response.json();

            if (!response.ok) {
                const message = data.message || `Error ${response.status}: Request failed`;
                throw new Error(message);
            }

            return data.data !== undefined ? data.data : data;
        } catch (error) {
            console.error(`API Error [${endpoint}]:`, error);
            throw error;
        }
    },

    // Auth endpoints
    login(credentials) {
        return this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify(credentials)
        });
    },

    register(userData) {
        return this.request('/auth/register', {
            method: 'POST',
            body: JSON.stringify(userData)
        });
    },

    getProfile() {
        return this.request('/auth/me');
    },

    updateBudget(monthlyBudget) {
        return this.request('/auth/budget', {
            method: 'PUT',
            body: JSON.stringify({ monthlyBudget })
        });
    },

    // Categories
    getCategories(type) {
        const query = type ? `?type=${type}` : '';
        return this.request(`/categories${query}`);
    },

    // Expenses
    getExpenses() {
        return this.request('/expenses');
    },

    createExpense(expenseData) {
        return this.request('/expenses', {
            method: 'POST',
            body: JSON.stringify(expenseData)
        });
    },

    updateExpense(id, expenseData) {
        return this.request(`/expenses/${id}`, {
            method: 'PUT',
            body: JSON.stringify(expenseData)
        });
    },

    deleteExpense(id) {
        return this.request(`/expenses/${id}`, {
            method: 'DELETE'
        });
    },

    getAnomalies() {
        return this.request('/expenses/anomalies');
    },

    // Incomes
    getIncomes() {
        return this.request('/incomes');
    },

    createIncome(incomeData) {
        return this.request('/incomes', {
            method: 'POST',
            body: JSON.stringify(incomeData)
        });
    },

    deleteIncome(id) {
        return this.request(`/incomes/${id}`, {
            method: 'DELETE'
        });
    },

    // Budgets
    getBudgets(month, year) {
        let query = '';
        if (month && year) {
            query = `?month=${month}&year=${year}`;
        }
        return this.request(`/budgets${query}`);
    },

    getBudgetSummary(month, year) {
        let query = '';
        if (month && year) {
            query = `?month=${month}&year=${year}`;
        }
        return this.request(`/budgets/summary${query}`);
    },

    createBudget(budgetData) {
        return this.request('/budgets', {
            method: 'POST',
            body: JSON.stringify(budgetData)
        });
    },

    updateBudgetEntry(id, budgetData) {
        return this.request(`/budgets/${id}`, {
            method: 'PUT',
            body: JSON.stringify(budgetData)
        });
    },

    deleteBudget(id) {
        return this.request(`/budgets/${id}`, {
            method: 'DELETE'
        });
    },

    // Analytics
    getDashboardSummary() {
        return this.request('/analytics/summary');
    },

    // AI
    aiCategorize(text, amount, merchant) {
        return this.request('/ai/categorize', {
            method: 'POST',
            body: JSON.stringify({ text, amount, merchant })
        });
    },

    aiCheckAnomaly(amount, category) {
        return this.request('/ai/check-anomaly', {
            method: 'POST',
            body: JSON.stringify({ amount, category })
        });
    }
};

// UI Toast Helper
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'fa-info-circle';
    if (type === 'success') icon = 'fa-check-circle';
    if (type === 'error') icon = 'fa-exclamation-circle';
    if (type === 'warning') icon = 'fa-triangle-exclamation';

    toast.innerHTML = `
        <i class="fas ${icon}"></i>
        <span>${message}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Currency Formatter
function formatINR(amount) {
    if (amount === null || amount === undefined) return '₹0.00';
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2
    }).format(amount);
}
