-- Initial default categories
INSERT INTO categories (id, name, type, icon, color) VALUES 
(1, 'Food & Dining', 'EXPENSE', 'fa-utensils', '#FF6384') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(2, 'Transportation', 'EXPENSE', 'fa-car', '#36A2EB') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(3, 'Housing & Rent', 'EXPENSE', 'fa-house', '#FFCE56') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(4, 'Utilities & Bills', 'EXPENSE', 'fa-bolt', '#4BC0C0') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(5, 'Entertainment', 'EXPENSE', 'fa-film', '#9966FF') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(6, 'Healthcare', 'EXPENSE', 'fa-heart-pulse', '#FF9F40') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(7, 'Shopping', 'EXPENSE', 'fa-bag-shopping', '#E7E9ED') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(8, 'Education', 'EXPENSE', 'fa-graduation-cap', '#2ECC71') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(9, 'Travel', 'EXPENSE', 'fa-plane', '#3498DB') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(10, 'Investments', 'EXPENSE', 'fa-chart-line', '#9B59B6') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(11, 'Other Expense', 'EXPENSE', 'fa-ellipsis', '#95A5A6') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (id, name, type, icon, color) VALUES 
(12, 'Salary', 'INCOME', 'fa-money-bill-wave', '#27AE60') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(13, 'Freelance', 'INCOME', 'fa-laptop-code', '#2980B9') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(14, 'Business', 'INCOME', 'fa-briefcase', '#8E44AD') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(15, 'Investments & Dividends', 'INCOME', 'fa-coins', '#F39C12') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name, type, icon, color) VALUES 
(16, 'Other Income', 'INCOME', 'fa-hand-holding-dollar', '#16A085') ON CONFLICT (name) DO NOTHING;

-- Reset sequence to avoid id collisions with future inserts
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
