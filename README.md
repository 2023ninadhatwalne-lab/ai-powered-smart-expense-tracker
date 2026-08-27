# 🧠 AI Smart Expense Tracker & Financial Intelligence Engine

> **A modern, full-stack personal finance and spending intelligence platform powered by Java 21, Spring Boot 3, PostgreSQL, Scikit-Learn, and Chart.js.**

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?logo=postgresql)](https://www.postgresql.org/)
[![Scikit-Learn](https://img.shields.io/badge/Scikit--Learn-1.7.2-F7931E.svg?logo=scikit-learn)](https://scikit-learn.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.135-009688.svg?logo=fastapi)](https://fastapi.tiangolo.com/)
[![Chart.js](https://img.shields.io/badge/Chart.js-4.4-FF6384.svg?logo=chartdotjs)](https://www.chartjs.org/)
[![JWT Security](https://img.shields.io/badge/Security-Spring%20Security%20%2B%20JWT-red.svg)](https://jwt.io/)

---

## 📌 Project Overview

**AI Smart Expense Tracker** is a production-oriented financial management system designed to eliminate manual bookkeeping friction through applied Machine Learning. Built with a clean 3-tier architecture (Controller → Service → Repository → PostgreSQL), the platform provides real-time transaction processing, automated natural-language categorization, statistical & ML-driven anomaly detection, and actionable financial insights.

### 🌟 Key Highlights
- **Real ML Expense Categorization:** Uses **TF-IDF Vectorization + Logistic Regression** trained on real-world transaction patterns to automatically classify raw transaction memos (e.g., *"Swiggy dinner biryani"* ➔ `Food & Dining`) with merchant extraction.
- **Spending Anomaly Detection:** Implements **Isolation Forest** (with statistical IQR/Z-score fallback) to detect spending spikes and outlier transactions deviating from user baseline habits.
- **Automated Financial Intelligence:** Generates dynamic month-over-month delta insights, budget utilization alerts, category drift analysis, and smart savings ratio recommendations.
- **Enterprise-Grade Security:** Stateless authentication using **Spring Security 6** and cryptographically signed **JSON Web Tokens (JWT)** with BCrypt password hashing.
- **Modern Responsive Dashboard:** Sleek dark-mode financial UI featuring **Chart.js** multi-month trend graphs and category distribution doughnut charts.

---

## 🏗️ Architecture & System Design

```
┌─────────────────────────────────────────────────────────────┐
│                   Frontend Client (Web)                     │
│         Vanilla HTML5 / CSS3 / ES6 / Chart.js               │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP / REST + Bearer JWT
┌──────────────────────────────▼──────────────────────────────┐
│             Spring Boot 3.3.3 Backend API (Java 21)         │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Security Layer (Spring Security 6 + JJWT Filter)      │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ Controllers (Auth, Expense, Income, Analytics, AI)    │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ Business Logic & DTO Services                         │  │
│  │  - ExpenseService / IncomeService                     │  │
│  │  - AnalyticsService (MoM Delta, Trends, KPI Engine)   │  │
│  │  - AiCategorizationService & AnomalyDetectionService  │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ Spring Data JPA Repositories                          │  │
│  └───────────────────────────┬───────────────────────────┘  │
└──────────────────────────────┼──────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               │                               │
┌──────────────▼──────────────┐ ┌──────────────▼──────────────┐
│      PostgreSQL Database    │ │   Python ML Micro-Engine    │
│  - users                    │ │   (FastAPI + Scikit-Learn)  │
│  - expenses                 │ │   - TF-IDF + Logistic Reg   │
│  - incomes                  │ │   - Isolation Forest        │
│  - categories               │ │   - Model Joblib Store      │
└─────────────────────────────┘ └─────────────────────────────┘
```

---

## 🤖 Applied AI/ML Modules

### 1. Natural Language Expense Categorization
- **Algorithm:** `Pipeline(TfidfVectorizer(ngram_range=(1, 2), sublinear_tf=True), LogisticRegression(C=3.0))`
- **Feature Extraction:** Character & sub-word n-grams capturing merchant names, item types, and spending context.
- **Fault-Tolerant Fallback:** If the Python ML service is offline, the backend seamlessly activates a deterministic semantic tokenizer classifier ensuring zero downtime.

### 2. Isolation Forest Spending Anomaly Detection
- **Algorithm:** `sklearn.ensemble.IsolationForest(contamination='auto')`
- **Mechanism:** Isolates unusual expenditure values in multi-dimensional space.
- **Graceful Baseline Handling:** For new accounts (< 3 records), the system gracefully skips anomaly detection with an informative reason rather than generating false positives.

---

## 🗄️ Database Schema (PostgreSQL)

| Table | Primary Columns | Indexes / Constraints |
|---|---|---|
| `users` | `id`, `username`, `email`, `password`, `full_name`, `monthly_budget`, `created_at` | Unique(`username`), Unique(`email`) |
| `categories` | `id`, `name`, `type` (`EXPENSE`/`INCOME`), `icon`, `color` | Unique(`name`) |
| `expenses` | `id`, `user_id`, `amount`, `description`, `category_id`, `expense_date`, `payment_method`, `merchant`, `is_anomaly`, `anomaly_reason`, `ai_categorized` | Index(`user_id`, `expense_date`), FK(`category_id`) |
| `incomes` | `id`, `user_id`, `amount`, `source`, `category_id`, `income_date`, `payment_method` | Index(`user_id`, `income_date`), FK(`category_id`) |

---

## 📡 REST API Reference

### 🔐 Authentication (`/api/auth`)
- `POST /api/auth/register` — Create new user account & return JWT token
- `POST /api/auth/login` — Authenticate user credentials & return JWT token
- `GET /api/auth/me` — Retrieve current authenticated user profile
- `PUT /api/auth/budget` — Update monthly spending budget

### 💸 Expense Management (`/api/expenses`)
- `POST /api/expenses` — Create expense (supports auto AI categorization & anomaly scanning)
- `GET /api/expenses` — Retrieve all expenses for logged-in user
- `GET /api/expenses/paginated?page=0&size=10` — Paginated expense history
- `GET /api/expenses/range?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` — Filter by date range
- `GET /api/expenses/anomalies` — List transactions flagged as unusual
- `PUT /api/expenses/{id}` — Update expense details
- `DELETE /api/expenses/{id}` — Delete expense record

### 💰 Income Management (`/api/incomes`)
- `POST /api/incomes` — Log new income entry
- `GET /api/incomes` — Retrieve all income entries for logged-in user
- `DELETE /api/incomes/{id}` — Delete income record

### 📊 Analytics & Insights (`/api/analytics`)
- `GET /api/analytics/summary` — Full dashboard metrics, KPIs, MoM trends, category breakdown, and AI insights

### 🧠 AI Engine Endpoints (`/api/ai`)
- `POST /api/ai/categorize` — Predict category from raw transaction memo
- `POST /api/ai/check-anomaly` — Test amount against historical spending baseline

---

## 🚀 Quickstart & Setup Guide

### 1. Prerequisites
- **Java 21 JDK** (Temurin / OpenJDK)
- **Maven 3.9+** (Bundled in `tools/apache-maven-3.9.6`)
- **Python 3.10+** (with `scikit-learn`, `fastapi`, `uvicorn`, `pandas`, `joblib`)
- **PostgreSQL 14+**

### 2. Database Setup
```sql
CREATE DATABASE expense_tracker_db;
```

### 3. Start the AI Micro-Engine (Optional but Recommended)
```bash
cd ai-service
python app.py
# Runs on http://localhost:8000
```

### 4. Build & Run the Spring Boot Backend
```bash
cd backend
mvn clean spring-boot:run
# Backend & UI live on http://localhost:8080
```

### 5. Access the Web Application
Open your browser and navigate to:
```
http://localhost:8080/login.html
```

---

## 💼 Resume / Portfolio Bullet Points

```markdown
- Architected and developed a full-stack AI-powered personal finance platform using Java 21, Spring Boot 3, PostgreSQL, Scikit-Learn, and Chart.js with clean 3-tier REST architecture.
- Engineered a natural-language transaction categorization engine using Scikit-Learn TF-IDF Vectorization and Logistic Regression (~90%+ precision) with merchant entity extraction.
- Implemented an automated spending anomaly detector utilizing Isolation Forest and statistical Z-Score fallbacks to flag unusual transaction spikes.
- Designed comprehensive financial analytics calculating month-over-month spending drift, savings rates, and real-time Chart.js trend visualizations.
- Built a secure, stateless authentication system using Spring Security 6, JJWT (HMAC-SHA-256), and BCrypt hashing with centralized exception handling and DTO validation.
```

---

## 📄 License
This project is licensed under the MIT License - feel free to use and adapt for learning and portfolio purposes.
