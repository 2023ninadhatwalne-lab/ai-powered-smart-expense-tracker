// Chart.js helper module for AI Smart Expense Tracker

let categoryChartInstance = null;
let trendChartInstance = null;

const Charts = {
    // 1. Category Breakdown Doughnut Chart
    renderCategoryBreakdown(canvasId, categories) {
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;

        if (categoryChartInstance) {
            categoryChartInstance.destroy();
        }

        if (!categories || categories.length === 0) {
            ctx.getContext('2d').clearRect(0, 0, ctx.width, ctx.height);
            return;
        }

        const labels = categories.map(c => c.categoryName);
        const data = categories.map(c => c.amount);
        const backgroundColors = categories.map(c => c.color || '#6366f1');

        categoryChartInstance = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: backgroundColors,
                    borderColor: '#1e293b',
                    borderWidth: 2,
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            color: '#94a3b8',
                            font: { family: 'Inter', size: 12 },
                            padding: 14,
                            usePointStyle: true
                        }
                    },
                    tooltip: {
                        backgroundColor: '#0f172a',
                        titleColor: '#f8fafc',
                        bodyColor: '#94a3b8',
                        borderColor: '#334155',
                        borderWidth: 1,
                        callbacks: {
                            label: function(context) {
                                const val = context.raw || 0;
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const pct = total > 0 ? ((val / total) * 100).toFixed(1) : 0;
                                return ` ${context.label}: ₹${val.toLocaleString('en-IN')} (${pct}%)`;
                            }
                        }
                    }
                },
                cutout: '68%'
            }
        });
    },

    // 2. Monthly Income vs Expense Trend Bar Chart
    renderMonthlyTrends(canvasId, trends) {
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;

        if (trendChartInstance) {
            trendChartInstance.destroy();
        }

        if (!trends || trends.length === 0) {
            ctx.getContext('2d').clearRect(0, 0, ctx.width, ctx.height);
            return;
        }

        const labels = trends.map(t => t.month);
        const incomeData = trends.map(t => t.income);
        const expenseData = trends.map(t => t.expense);

        trendChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Income',
                        data: incomeData,
                        backgroundColor: 'rgba(16, 185, 129, 0.75)',
                        borderColor: '#10b981',
                        borderWidth: 1.5,
                        borderRadius: 6,
                        barPercentage: 0.6
                    },
                    {
                        label: 'Expenses',
                        data: expenseData,
                        backgroundColor: 'rgba(239, 68, 68, 0.75)',
                        borderColor: '#ef4444',
                        borderWidth: 1.5,
                        borderRadius: 6,
                        barPercentage: 0.6
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            color: '#94a3b8',
                            font: { family: 'Inter', size: 12 },
                            usePointStyle: true
                        }
                    },
                    tooltip: {
                        backgroundColor: '#0f172a',
                        titleColor: '#f8fafc',
                        bodyColor: '#94a3b8',
                        borderColor: '#334155',
                        borderWidth: 1,
                        callbacks: {
                            label: function(context) {
                                return ` ${context.dataset.label}: ₹${context.raw.toLocaleString('en-IN')}`;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { color: 'rgba(51, 65, 85, 0.3)' },
                        ticks: { color: '#94a3b8', font: { family: 'Inter' } }
                    },
                    y: {
                        grid: { color: 'rgba(51, 65, 85, 0.3)' },
                        ticks: {
                            color: '#94a3b8',
                            font: { family: 'Inter' },
                            callback: function(value) {
                                return '₹' + (value >= 1000 ? (value / 1000) + 'k' : value);
                            }
                        }
                    }
                }
            }
        });
    }
};
