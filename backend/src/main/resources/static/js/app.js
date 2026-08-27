// Main Application logic for AI Smart Expense Tracker

document.addEventListener('DOMContentLoaded', () => {
    // Check authentication unless on login page
    if (!window.location.pathname.endsWith('login.html')) {
        API.requireAuth();
        setupUserProfile();
    }

    // Page-specific initializations
    const path = window.location.pathname;
    if (path.endsWith('index.html') || path.endsWith('/') || path === '') {
        initDashboard();
    } else if (path.endsWith('expenses.html')) {
        initExpensesPage();
    } else if (path.endsWith('income.html')) {
        initIncomePage();
    } else if (path.endsWith('analytics.html')) {
        initAnalyticsPage();
    }
});

// Setup User Profile badge in sidebar
function setupUserProfile() {
    const user = API.getUser();
    if (user) {
        const nameEl = document.getElementById('user-full-name');
        const emailEl = document.getElementById('user-email');
        const avatarEl = document.getElementById('user-avatar');

        if (nameEl) nameEl.textContent = user.fullName || user.username;
        if (emailEl) emailEl.textContent = user.email;
        if (avatarEl) avatarEl.textContent = (user.fullName || user.username).charAt(0).toUpperCase();
    }

    const logoutBtn = document.getElementById('btn-logout');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => API.logout());
    }
}

// -------------------------------------------------------------
// DASHBOARD PAGE
// -------------------------------------------------------------
async function initDashboard() {
    try {
        const summary = await API.getDashboardSummary();
        renderDashboardKpis(summary);
        renderAiInsights(summary.aiInsights);
        renderRecentTransactions(summary.recentTransactions);
        
        // Render Charts
        Charts.renderMonthlyTrends('trendChart', summary.monthlyTrends);
        Charts.renderCategoryBreakdown('categoryChart', summary.categoryBreakdown);
    } catch (error) {
        showToast('Failed to load dashboard data', 'error');
    }
}

function renderDashboardKpis(data) {
    const incomeEl = document.getElementById('kpi-total-income');
    const expenseEl = document.getElementById('kpi-total-expense');
    const savingsEl = document.getElementById('kpi-current-savings');
    const savingsRateEl = document.getElementById('kpi-savings-rate');
    const anomalyBadgeEl = document.getElementById('kpi-anomaly-badge');

    if (incomeEl) incomeEl.textContent = formatINR(data.totalIncome);
    if (expenseEl) expenseEl.textContent = formatINR(data.totalExpense);
    if (savingsEl) savingsEl.textContent = formatINR(data.currentSavings);
    if (savingsRateEl) savingsRateEl.textContent = `${data.savingsRate}%`;

    if (anomalyBadgeEl) {
        anomalyBadgeEl.textContent = `${data.anomalyCount} Alert${data.anomalyCount !== 1 ? 's' : ''}`;
        anomalyBadgeEl.className = data.anomalyCount > 0 ? 'badge badge-anomaly' : 'badge badge-ai';
    }

    // Budget progress bar if element exists
    const budgetBarEl = document.getElementById('budget-progress-bar');
    const budgetPctEl = document.getElementById('budget-pct-text');
    if (budgetBarEl && data.monthlyBudget > 0) {
        const pct = Math.min(100, data.budgetUsedPercentage);
        budgetBarEl.style.width = `${pct}%`;
        budgetBarEl.style.backgroundColor = pct >= 90 ? 'var(--danger)' : pct >= 70 ? 'var(--warning)' : 'var(--accent-primary)';
        if (budgetPctEl) budgetPctEl.textContent = `${data.budgetUsedPercentage}% used of ${formatINR(data.monthlyBudget)}`;
    }
}

function renderAiInsights(insights) {
    const container = document.getElementById('ai-insights-list');
    if (!container) return;

    if (!insights || insights.length === 0) {
        container.innerHTML = `<p class="text-muted" style="font-size: 0.9rem; padding: 12px;">No alerts. Add transactions to generate intelligent insights.</p>`;
        return;
    }

    container.innerHTML = insights.map(item => {
        let typeClass = 'info';
        let icon = 'fa-wand-magic-sparkles';
        if (item.type === 'WARNING') { typeClass = 'warning'; icon = 'fa-triangle-exclamation'; }
        if (item.type === 'SUCCESS') { typeClass = 'success'; icon = 'fa-circle-check'; }
        if (item.type === 'ANOMALY') { typeClass = 'anomaly'; icon = 'fa-bell'; }

        return `
            <div class="insight-card">
                <div class="insight-icon ${typeClass}">
                    <i class="fas ${icon}"></i>
                </div>
                <div class="insight-content">
                    <h4>${item.title}</h4>
                    <p>${item.message}</p>
                </div>
            </div>
        `;
    }).join('');
}

function renderRecentTransactions(transactions) {
    const tbody = document.getElementById('recent-transactions-tbody');
    if (!tbody) return;

    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 24px;">No transactions recorded yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = transactions.map(t => `
        <tr>
            <td>
                <div style="font-weight: 600;">${t.description}</div>
                <div style="font-size: 0.75rem; color: var(--text-muted);">${t.merchant ? t.merchant + ' • ' : ''}${t.paymentMethod || 'UPI'}</div>
            </td>
            <td>
                <span class="badge badge-category" style="border-left: 3px solid ${t.categoryColor || '#6366f1'};">
                    <i class="fas ${t.categoryIcon || 'fa-tag'}" style="color: ${t.categoryColor || '#6366f1'};"></i>
                    ${t.categoryName}
                </span>
                ${t.aiCategorized ? '<span class="badge badge-ai" title="Categorized by Scikit-Learn Model"><i class="fas fa-brain"></i> AI</span>' : ''}
            </td>
            <td style="font-weight: 700; color: var(--danger);">${formatINR(t.amount)}</td>
            <td style="color: var(--text-secondary); font-size: 0.85rem;">${t.date}</td>
            <td>
                ${t.isAnomaly 
                    ? `<span class="badge badge-anomaly" title="${t.anomalyReason || 'Outlier transaction'}"><i class="fas fa-exclamation-circle"></i> Outlier</span>` 
                    : '<span style="color: var(--success); font-size: 0.8rem;"><i class="fas fa-check"></i> Normal</span>'}
            </td>
        </tr>
    `).join('');
}

// -------------------------------------------------------------
// EXPENSES PAGE
// -------------------------------------------------------------
let cachedCategories = [];

async function initExpensesPage() {
    loadCategoriesForDropdown();
    loadExpensesTable();
    setupExpenseModalHandlers();
}

async function loadCategoriesForDropdown() {
    try {
        cachedCategories = await API.getCategories('EXPENSE');
        const select = document.getElementById('expense-category-select');
        if (select) {
            select.innerHTML = `<option value="">-- Let AI Auto-Classify or Choose --</option>` +
                cachedCategories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }
    } catch (e) {
        console.error('Failed to load categories', e);
    }
}

async function loadExpensesTable() {
    const tbody = document.getElementById('expenses-table-tbody');
    if (!tbody) return;

    try {
        const expenses = await API.getExpenses();
        if (expenses.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 32px;">No expenses found. Click "+ Add Expense" above to create one.</td></tr>`;
            return;
        }

        tbody.innerHTML = expenses.map(e => `
            <tr>
                <td><strong>${e.description}</strong></td>
                <td>${e.merchant || '—'}</td>
                <td>
                    <span class="badge badge-category" style="border-left: 3px solid ${e.categoryColor || '#6366f1'};">
                        <i class="fas ${e.categoryIcon || 'fa-tag'}" style="color: ${e.categoryColor || '#6366f1'};"></i>
                        ${e.categoryName}
                    </span>
                    ${e.aiCategorized ? '<span class="badge badge-ai" style="margin-left: 4px;"><i class="fas fa-brain"></i> AI</span>' : ''}
                </td>
                <td style="font-weight: 700; color: var(--danger);">${formatINR(e.amount)}</td>
                <td>${e.paymentMethod || 'UPI'}</td>
                <td style="color: var(--text-secondary); font-size: 0.85rem;">${e.date}</td>
                <td>
                    <div style="display: flex; gap: 8px; align-items: center;">
                        ${e.isAnomaly ? `<span class="badge badge-anomaly" title="${e.anomalyReason || 'Anomaly'}"><i class="fas fa-triangle-exclamation"></i> Outlier</span>` : ''}
                        <button class="btn btn-outline btn-sm" onclick="handleDeleteExpense(${e.id})" style="color: var(--danger); border-color: rgba(239, 68, 68, 0.4);">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        showToast('Error loading expenses', 'error');
    }
}

function setupExpenseModalHandlers() {
    const modal = document.getElementById('expense-modal');
    const openBtn = document.getElementById('btn-open-expense-modal');
    const closeBtn = document.getElementById('btn-close-expense-modal');
    const form = document.getElementById('expense-form');
    const magicAiBtn = document.getElementById('btn-magic-ai');
    const descInput = document.getElementById('expense-desc-input');
    const categorySelect = document.getElementById('expense-category-select');
    const merchantInput = document.getElementById('expense-merchant-input');
    const aiPredictionStatus = document.getElementById('ai-prediction-status');

    if (openBtn && modal) {
        openBtn.addEventListener('click', () => {
            modal.classList.add('show');
            document.getElementById('expense-date-input').value = new Date().toISOString().split('T')[0];
        });
    }

    if (closeBtn && modal) {
        closeBtn.addEventListener('click', () => modal.classList.remove('show'));
    }

    // Live AI Magic button test
    if (magicAiBtn && descInput) {
        magicAiBtn.addEventListener('click', async () => {
            const text = descInput.value.trim();
            if (!text) {
                showToast('Please enter an expense description first (e.g., "Swiggy dinner 450")', 'warning');
                return;
            }

            magicAiBtn.innerHTML = `<i class="fas fa-spinner fa-spin"></i> Categorizing...`;
            try {
                const res = await API.aiCategorize(text, null, merchantInput ? merchantInput.value : null);
                if (categorySelect && res.categoryId) {
                    categorySelect.value = res.categoryId;
                }
                if (merchantInput && res.extractedMerchant && !merchantInput.value) {
                    merchantInput.value = res.extractedMerchant;
                }
                if (aiPredictionStatus) {
                    aiPredictionStatus.innerHTML = `
                        <div style="background: var(--ai-purple-bg); border: 1px solid var(--ai-purple); padding: 8px 12px; border-radius: 6px; font-size: 0.8rem; color: #f8fafc; margin-top: 6px;">
                            <i class="fas fa-brain" style="color: var(--ai-purple);"></i> AI classified as <strong>${res.category}</strong> (${(res.confidence * 100).toFixed(0)}% confidence) via <em>${res.modelName}</em>
                        </div>
                    `;
                }
                showToast(`AI classified as "${res.category}"`, 'success');
            } catch (err) {
                showToast('AI Classification failed', 'error');
            } finally {
                magicAiBtn.innerHTML = `<i class="fas fa-wand-magic-sparkles"></i> AI Suggest`;
            }
        });
    }

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const amount = parseFloat(document.getElementById('expense-amount-input').value);
            const description = descInput.value.trim();
            const categoryId = categorySelect.value ? parseInt(categorySelect.value) : null;
            const merchant = merchantInput.value.trim();
            const paymentMethod = document.getElementById('expense-payment-method').value;
            const date = document.getElementById('expense-date-input').value;

            try {
                await API.createExpense({
                    amount,
                    description,
                    categoryId,
                    merchant,
                    paymentMethod,
                    date,
                    autoCategorize: !categoryId
                });

                showToast('Expense added successfully!', 'success');
                modal.classList.remove('show');
                form.reset();
                if (aiPredictionStatus) aiPredictionStatus.innerHTML = '';
                loadExpensesTable();
            } catch (err) {
                showToast(err.message, 'error');
            }
        });
    }
}

async function handleDeleteExpense(id) {
    if (!confirm('Are you sure you want to delete this expense?')) return;
    try {
        await API.deleteExpense(id);
        showToast('Expense deleted', 'success');
        loadExpensesTable();
    } catch (e) {
        showToast('Failed to delete expense', 'error');
    }
}

// -------------------------------------------------------------
// INCOME PAGE
// -------------------------------------------------------------
async function initIncomePage() {
    loadIncomeCategoriesDropdown();
    loadIncomesTable();
    setupIncomeModalHandlers();
}

async function loadIncomeCategoriesDropdown() {
    try {
        const categories = await API.getCategories('INCOME');
        const select = document.getElementById('income-category-select');
        if (select) {
            select.innerHTML = categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }
    } catch (e) {
        console.error('Failed to load income categories', e);
    }
}

async function loadIncomesTable() {
    const tbody = document.getElementById('incomes-table-tbody');
    if (!tbody) return;

    try {
        const incomes = await API.getIncomes();
        if (incomes.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 32px;">No income records found. Click "+ Add Income" above.</td></tr>`;
            return;
        }

        tbody.innerHTML = incomes.map(i => `
            <tr>
                <td><strong>${i.source}</strong></td>
                <td>
                    <span class="badge badge-category" style="border-left: 3px solid ${i.categoryColor || '#2ECC71'};">
                        <i class="fas ${i.categoryIcon || 'fa-money-bill'}" style="color: ${i.categoryColor || '#2ECC71'};"></i>
                        ${i.categoryName}
                    </span>
                </td>
                <td style="font-weight: 700; color: var(--success);">${formatINR(i.amount)}</td>
                <td>${i.paymentMethod || 'UPI'}</td>
                <td style="color: var(--text-secondary); font-size: 0.85rem;">${i.date}</td>
                <td>
                    <button class="btn btn-outline btn-sm" onclick="handleDeleteIncome(${i.id})" style="color: var(--danger); border-color: rgba(239, 68, 68, 0.4);">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        showToast('Error loading incomes', 'error');
    }
}

function setupIncomeModalHandlers() {
    const modal = document.getElementById('income-modal');
    const openBtn = document.getElementById('btn-open-income-modal');
    const closeBtn = document.getElementById('btn-close-income-modal');
    const form = document.getElementById('income-form');

    if (openBtn && modal) {
        openBtn.addEventListener('click', () => {
            modal.classList.add('show');
            document.getElementById('income-date-input').value = new Date().toISOString().split('T')[0];
        });
    }

    if (closeBtn && modal) {
        closeBtn.addEventListener('click', () => modal.classList.remove('show'));
    }

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const amount = parseFloat(document.getElementById('income-amount-input').value);
            const source = document.getElementById('income-source-input').value.trim();
            const categoryId = parseInt(document.getElementById('income-category-select').value);
            const paymentMethod = document.getElementById('income-payment-method').value;
            const date = document.getElementById('income-date-input').value;

            try {
                await API.createIncome({
                    amount,
                    source,
                    categoryId,
                    paymentMethod,
                    date
                });

                showToast('Income logged successfully!', 'success');
                modal.classList.remove('show');
                form.reset();
                loadIncomesTable();
            } catch (err) {
                showToast(err.message, 'error');
            }
        });
    }
}

async function handleDeleteIncome(id) {
    if (!confirm('Are you sure you want to delete this income entry?')) return;
    try {
        await API.deleteIncome(id);
        showToast('Income deleted', 'success');
        loadIncomesTable();
    } catch (e) {
        showToast('Failed to delete income', 'error');
    }
}

// -------------------------------------------------------------
// ANALYTICS PAGE
// -------------------------------------------------------------
async function initAnalyticsPage() {
    try {
        const summary = await API.getDashboardSummary();
        renderAnalyticsKpis(summary);
        renderAiInsights(summary.aiInsights);
        Charts.renderMonthlyTrends('analyticsTrendChart', summary.monthlyTrends);
        Charts.renderCategoryBreakdown('analyticsCategoryChart', summary.categoryBreakdown);
        renderAnomaliesList(summary.unusualTransactions);
    } catch (e) {
        showToast('Failed to load analytics', 'error');
    }
}

function renderAnalyticsKpis(data) {
    const budgetEl = document.getElementById('analytics-budget');
    const savingsRateEl = document.getElementById('analytics-savings-rate');
    const anomalyCountEl = document.getElementById('analytics-anomaly-count');

    if (budgetEl) budgetEl.textContent = formatINR(data.monthlyBudget);
    if (savingsRateEl) savingsRateEl.textContent = `${data.savingsRate}%`;
    if (anomalyCountEl) anomalyCountEl.textContent = data.anomalyCount;
}

function renderAnomaliesList(anomalies) {
    const container = document.getElementById('anomalies-list-container');
    if (!container) return;

    if (!anomalies || anomalies.length === 0) {
        container.innerHTML = `<p class="text-muted" style="padding: 16px;">No unusual transactions detected. All expenses match baseline patterns.</p>`;
        return;
    }

    container.innerHTML = anomalies.map(a => `
        <div style="background: var(--bg-secondary); border: 1px solid rgba(239, 68, 68, 0.4); border-radius: var(--radius-md); padding: 16px; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
            <div>
                <div style="font-weight: 700; color: var(--text-primary); font-size: 1rem;">${a.description} • ${formatINR(a.amount)}</div>
                <div style="font-size: 0.8rem; color: var(--danger); margin-top: 4px;">
                    <i class="fas fa-circle-exclamation"></i> ${a.anomalyReason || 'Unusually high amount flagged by Isolation Forest'}
                </div>
            </div>
            <div style="font-size: 0.85rem; color: var(--text-muted);">${a.date}</div>
        </div>
    `).join('');
}
