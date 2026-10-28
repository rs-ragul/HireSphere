# HireSphere: Applicant Tracking System (ATS) with Skill-Match Analytics

**A Modern Full-Stack Enterprise Mini Project: Java 17 + JDBC + React Web Portal**

HireSphere is an Applicant Tracking System (ATS) designed to eliminate resume screening bias and manual review bottlenecks by dynamically matching, scoring, and ranking candidate profiles against job requisitions using a multi-factor **Skill-Match Analytics Engine**.

Featuring both an **Executive React Web Portal** (glassmorphic dark UI, real-time KPI metrics, interactive ranking leaderboard, gap diagnostic cards) and an **Interactive Terminal Console**, powered by a high-performance **Java 17 JDBC Data Access Layer**.

---

## 🌟 Key Architecture & Stack

```
 ┌─────────────────────────────────────────────────────────────────────────┐
 │               PREMIUM REACT WEB PORTAL (Vite + React 18)                │
 │  • Executive KPI Dashboard  • Dynamic ATS Ranking Leaderboard          │
 │  • Skill-Match Simulator    • Job Requisition & Talent Directory        │
 └────────────────────────────────────┬────────────────────────────────────┘
                                      │ REST API (JSON / HTTP)
 ┌────────────────────────────────────▼────────────────────────────────────┐
 │                  JAVA 17 LIGHTWEIGHT API SERVER (JDK)                  │
 │  • com.sun.net.httpserver.HttpServer (Zero external web framework)      │
 │  • Static React SPA Asset Server & REST API Endpoints                   │
 └────────────────────────────────────┬────────────────────────────────────┘
                                      │
 ┌────────────────────────────────────▼────────────────────────────────────┐
 │               SKILL-MATCH ANALYTICS ENGINE & SERVICE LAYER              │
 │  • Weighted Coverage (55%)  • Experience Delta (25%)                    │
 │  • Competency Benchmark (20%) • Missing Mandatory Skill Penalty         │
 └────────────────────────────────────┬────────────────────────────────────┘
                                      │
 ┌────────────────────────────────────▼────────────────────────────────────┐
 │                   JDBC DATA ACCESS LAYER (DAO Pattern)                  │
 │  • PreparedStatement  • Transaction Rollbacks (setAutoCommit false)    │
 │  • Batch Updates (executeBatch)  • Aggregates (GROUP BY, JOINs)         │
 └────────────────────────────────────┬────────────────────────────────────┘
                                      │
 ┌────────────────────────────────────▼────────────────────────────────────┐
 │                     RELATIONAL DATABASE (3NF Schema)                    │
 │  • Embedded SQLite (Zero setup default: hiresphere.db)                  │
 │  • MySQL / XAMPP Compatible (Single toggle in db.properties)           │
 └─────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 How to Launch the Web Portal

### 1-Click Launch (Easiest)
Simply double-click **`run-web.bat`** in the project folder!
- Starts the Java + JDBC backend and REST API server on port `8080`.
- Automatically opens **`http://localhost:8080`** in your default web browser!

### Running from Terminal (PowerShell / CMD)
```powershell
cd c:\Users\user\Desktop\HireSphere
.\run-web.bat
```

> **Note**: Both the **React Web Portal** (`http://localhost:8080`) and the **Interactive Terminal Menu** are active simultaneously!

---

## 🎨 Web Portal Modules & Features

1. **Executive Overview Dashboard**:
   - High-level KPIs: Active Open Jobs, Indexed Candidates, Total Applications Screened, Shortlisted Count, Hires Made, and Global Average Skill-Match Score.
   - **Skill Demand Matrix**: Live bar chart showing the most in-demand technical skills across all active positions (computed via SQL `GROUP BY` and `COUNT`).
2. **Dynamic ATS Ranking & Screening Leaderboard**:
   - Select any job requisition $\rightarrow$ click **"Run ATS Batch-Screening"**.
   - Instantly calculates and displays the candidate leaderboard sorted dynamically by `match_score DESC`.
   - Visual badges: `TIER 1 (Strong Match >= 80%)`, `TIER 2 (Moderate Match 60-79%)`, `TIER 3 (Underqualified < 60%)`.
   - Multi-factor breakdown columns: Skill Coverage %, Experience Factor %, Proficiency Factor %.
   - 1-Click recruiter stage actions: **Shortlist**, **Hire**, **Reject**.
3. **Skill-Match Simulator & Gap Diagnosis**:
   - 1-on-1 comparison of any candidate against any job requisition.
   - Circular score gauge, factor progress bars, verified skill checkmarks.
   - **Diagnostic Alerts**: Explicitly flags `[CRITICAL] Missing mandatory skill: Java`, `[DEFICIT] Deficient proficiency`, and `[EXPERIENCE] Experience shortfall`.
4. **Job Requisitions Management**:
   - Filter and search open positions.
   - "+ Post Job" modal with dynamic skill tags, weights (Mandatory 3x, Preferred 2x, Optional 1x), and minimum required proficiency (1–5 scale).
5. **Talent Pool Directory**:
   - Candidate directory with experience years, education, verified skills, and resume summary.
   - "+ Register Candidate" modal and Bulk CSV resume ingestion.

---

## 🧮 The Skill-Match Analytics Algorithm

$$\text{Composite Match Score} = (0.55 \times S_{\text{cov}}) + (0.25 \times E_{\text{factor}}) + (0.20 \times P_{\text{align}})$$

1. **Weighted Skill Coverage ($S_{\text{cov}}$)**:
   - Evaluates mandatory ($3\times$), preferred ($2\times$), and optional ($1\times$) skills.
   - Direct penalty deduction per missing mandatory skill to prevent underqualified applicants from ranking high.
2. **Experience Factor ($E_{\text{factor}}$)**:
   - Compares applicant experience against job requisites: $\min(100, (\text{Exp}_{\text{candidate}} / \text{Exp}_{\text{req}}) \times 100)$.
3. **Proficiency Benchmark ($P_{\text{align}}$)**:
   - Assesses self/assessed competency level (1–5 scale) against required threshold.
4. **Candidate Tiering**:
   - **TIER 1**: $\ge 80.0\%$ (Immediate Interview / Shortlist recommendation)
   - **TIER 2**: $60.0\% - 79.9\%$ (Secondary consideration)
   - **TIER 3**: $< 60.0\%$ (Critical gaps present)

---

## 🎤 Viva Voce Q&A Cheat Sheet (For Evaluator Questions)

| Evaluator Question | Winning Answer |
| :--- | :--- |
| **Why not just build a simple CRUD app?** | *"Basic CRUD only stores records. HireSphere is a data-heavy system featuring an analytical engine that weights skills, analyzes experience gaps, and sorts applicants algorithmically."* |
| **Where are JDBC Transactions used?** | *"In `JobDAO` and `ApplicationDAO`. Operations like creating a job with multiple required skills or submitting an application with audit logs execute within an atomic transaction (`conn.setAutoCommit(false)` with commit/rollback) to prevent partial data corruption."* |
| **How did you prevent SQL injection?** | *"100% parameterized `PreparedStatement` with strongly typed setters across all DAO methods. No string concatenation in SQL queries."* |
| **How does the system handle bulk candidate resumes?** | *"Through JDBC Batch Updates (`addBatch()` and `executeBatch()`) in `CandidateDAO` and `CsvParser`. Bulk loading 1,000 candidates executes in a single database round-trip."* |
| **How is the React frontend served?** | *"The Java backend includes an embedded HTTP server using standard JDK `com.sun.net.httpserver.HttpServer`. It serves the REST API (`/api/*`) and delivers the pre-bundled React SPA, requiring zero heavy external frameworks."* |
| **Can this run on MySQL?** | *"Yes! HireSphere has dual-driver support. In `src/main/resources/db.properties`, switching `db.type=mysql` instantly routes all operations to MySQL/XAMPP."* |
