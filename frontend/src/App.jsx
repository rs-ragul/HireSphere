import React, { useState, useEffect } from 'react';
import {
  LayoutDashboard,
  Briefcase,
  Users,
  Award,
  Kanban,
  FileCheck2,
  Plus,
  RefreshCw,
  Search,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Database,
  ArrowRight,
  Filter,
  X,
  Star,
  MessageSquare,
  Clock,
  Sparkles,
  UploadCloud,
  ChevronRight,
  ExternalLink,
  ShieldCheck,
  Check
} from 'lucide-react';

const PIPELINE_STAGES = [
  { id: 'APPLIED', label: '1. New Applied', color: '#38bdf8' },
  { id: 'SCREENING', label: '2. Screening', color: '#f59e0b' },
  { id: 'INTERVIEW', label: '3. Technical Interview', color: '#818cf8' },
  { id: 'OFFERED', label: '4. Offer Extended', color: '#a855f7' },
  { id: 'HIRED', label: '5. Hired', color: '#10b981' },
  { id: 'REJECTED', label: 'Rejected / Archived', color: '#ef4444' }
];

const DEMO_RESUMES = {
  tier1: {
    label: 'Arjun Nair (Lead Java Engineer)',
    matchPreview: 'Expected: ~95-100% (TIER 1)',
    text: `ARJUN NAIR
Email: arjun.nair@enterprise-talent.com
Phone: +1 (415) 890-2415
Location: San Francisco, CA | Remote
Education: B.Tech in Computer Science & Engineering (2019)

PROFESSIONAL SUMMARY
Lead Backend Software Engineer with 5.5 years of experience architecting distributed microservices and low-latency REST APIs. Specialized in Java, Spring Boot, MySQL, and Docker containerization. Proven track record leading cloud migrations on AWS.

TECHNICAL SKILLS
Languages & Frameworks: Java, Spring Boot, REST APIs, Microservices, Python
Databases: MySQL, PostgreSQL, Redis
Cloud & DevOps: Docker, Kubernetes, AWS, Git

EXPERIENCE
Lead Backend Engineer | FinTech Systems (2022 - Present)
- Architected high-throughput transaction processing engine in Java and Spring Boot serving 15M daily requests.
- Optimized MySQL schema and indexing, cutting 99th percentile query latency by 42%.
- Deployed microservices using Docker on AWS ECS with auto-scaling policies.`
  },
  tier2: {
    label: 'Priya Patel (Junior Java Dev)',
    matchPreview: 'Expected: ~60-65% (TIER 2 - Experience Gap)',
    text: `PRIYA PATEL
Email: priya.patel@dev-connect.io
Phone: +1 (512) 555-0149
Location: Austin, TX | Hybrid
Education: B.E in Information Technology (2022)

PROFESSIONAL SUMMARY
Junior Software Engineer with 2.0 years of experience in Core Java backend programming, relational database schema modeling in MySQL, and REST API development.

TECHNICAL SKILLS
Backend: Java, REST APIs
Databases: MySQL, Git
Tools: IntelliJ, Maven

EXPERIENCE
Associate Software Engineer | DataCorp (2022 - Present)
- Built core CRUD services using Java and MySQL.
- Documented and tested RESTful endpoints using Postman.`
  },
  tier3: {
    label: 'Sneha Kulkarni (Frontend/Python Dev)',
    matchPreview: 'Expected: ~20-25% (TIER 3 - Missing Java & Spring)',
    text: `SNEHA KULKARNI
Email: sneha.kulkarni@web-studio.org
Phone: +1 (408) 555-8392
Location: San Jose, CA
Education: B.Sc in Computer Science (2023)

PROFESSIONAL SUMMARY
Frontend developer with 1.0 year experience building user interfaces using React, JavaScript, HTML/CSS, and Python scripts.

TECHNICAL SKILLS
Frontend: React, JavaScript, HTML/CSS
Backend & Scripts: Python, MySQL, Git`
  }
};

export default function App() {
  const [activeTab, setActiveTab] = useState('pipeline'); // pipeline | leaderboard | parser | jobs | candidates | analytics
  const [jobs, setJobs] = useState([]);
  const [candidates, setCandidates] = useState([]);
  const [applications, setApplications] = useState([]);
  const [funnel, setFunnel] = useState(null);
  const [skillDemand, setSkillDemand] = useState([]);
  const [systemHealth, setSystemHealth] = useState(null);
  const [loading, setLoading] = useState(true);

  // Selected requisition
  const [selectedJobId, setSelectedJobId] = useState(null);

  // Candidate Dossier Slide-Over
  const [activeDossier, setActiveDossier] = useState(null);

  // Modals
  const [showJobModal, setShowJobModal] = useState(false);
  const [showCandModal, setShowCandModal] = useState(false);

  // Toast notification for user confirmation
  const [toast, setToast] = useState(null);

  useEffect(() => {
    loadAllData();
  }, []);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast(null);
    }, 3500);
  };

  const loadAllData = async () => {
    setLoading(true);
    try {
      const [hRes, jRes, cRes, aRes, fRes, sRes] = await Promise.all([
        fetch('/api/health').then(r => r.json()).catch(() => null),
        fetch('/api/jobs').then(r => r.json()).catch(() => []),
        fetch('/api/candidates').then(r => r.json()).catch(() => []),
        fetch('/api/applications').then(r => r.json()).catch(() => []),
        fetch('/api/analytics/summary').then(r => r.json()).catch(() => null),
        fetch('/api/analytics/skill-demand').then(r => r.json()).catch(() => [])
      ]);

      setSystemHealth(hRes);
      setJobs(jRes);
      setCandidates(cRes);
      setApplications(aRes);
      setFunnel(fRes);
      setSkillDemand(sRes);

      if (jRes.length > 0 && !selectedJobId) {
        setSelectedJobId(jRes[0].jobId);
      }
    } catch (e) {
      console.error('Data load error:', e);
    } finally {
      setLoading(false);
    }
  };

  // Quick 1-Click Demo Reset & Seeder
  const handleDemoSync = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/demo/reset', { method: 'POST' });
      const data = await res.json();
      await loadAllData();
      showToast('✔ Complete Demo Pipeline auto-populated & scored in SQLite Database!');
    } catch (e) {
      showToast('Failed to populate demo: ' + e.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  // Run ATS batch-screening on current job
  const handleBatchScreen = async (jobId) => {
    try {
      const res = await fetch(`/api/match/batch-screen/${jobId}`, { method: 'POST' });
      const data = await res.json();
      await loadAllData();
      showToast(`✔ ATS Engine screened ${data.screenedCount || 6} candidates into the pipeline!`);
    } catch (e) {
      showToast('Screening error: ' + e.message, 'error');
    }
  };

  // Update candidate pipeline stage
  const handleUpdateStatus = async (appId, newStatus) => {
    try {
      const res = await fetch(`/api/applications/${appId}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus })
      });
      if (res.ok) {
        setApplications(prev =>
          prev.map(a => a.applicationId === appId ? { ...a, status: newStatus } : a)
        );
        fetch('/api/analytics/summary').then(r => r.json()).then(setFunnel);
        showToast(`✔ Application #${appId} moved to stage [${newStatus}] in Database`);

        if (activeDossier && activeDossier.application && activeDossier.application.applicationId === appId) {
          setActiveDossier(prev => ({
            ...prev,
            application: { ...prev.application, status: newStatus }
          }));
        }
      }
    } catch (e) {
      showToast('Failed to update stage: ' + e.message, 'error');
    }
  };

  // Open Candidate Dossier
  const openDossier = async (app) => {
    const job = jobs.find(j => j.jobId === app.jobId) || jobs[0];
    const candidate = candidates.find(c => c.candidateId === app.candidateId);
    if (!candidate) return;

    try {
      const matchRes = await fetch('/api/match/evaluate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ jobId: job.jobId, candidateId: candidate.candidateId })
      }).then(r => r.json());

      const notesRes = await fetch(`/api/applications/${app.applicationId}/notes`).then(r => r.json()).catch(() => []);

      setActiveDossier({
        application: app,
        job,
        candidate,
        matchResult: matchRes,
        notes: notesRes
      });
    } catch (e) {
      console.error(e);
    }
  };

  // Add interview note
  const handleAddNote = async (appId, author, text, rating) => {
    try {
      const res = await fetch(`/api/applications/${appId}/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ author, noteText: text, rating: String(rating) })
      });
      if (res.ok) {
        const notesRes = await fetch(`/api/applications/${appId}/notes`).then(r => r.json()).catch(() => []);
        if (activeDossier) {
          setActiveDossier(prev => ({ ...prev, notes: notesRes }));
        }
        showToast('✔ Evaluation scorecard note saved to database!');
      }
    } catch (e) {
      showToast('Failed to save note: ' + e.message, 'error');
    }
  };

  const currentJob = jobs.find(j => Number(j.jobId) === Number(selectedJobId)) || jobs[0];

  const currentJobApps = applications.filter(a =>
    currentJob ? Number(a.jobId) === Number(currentJob.jobId) : true
  );

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--bg-app)' }}>
      
      {/* Toast Notification */}
      {toast && (
        <div style={{
          position: 'fixed',
          top: '20px',
          right: '24px',
          zIndex: 9999,
          background: toast.type === 'error' ? '#ef4444' : '#10b981',
          color: '#fff',
          padding: '12px 20px',
          borderRadius: '8px',
          fontWeight: 700,
          fontSize: '0.875rem',
          boxShadow: '0 8px 24px rgba(0, 0, 0, 0.4)',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          animation: 'fadeIn 0.2s ease'
        }}>
          <ShieldCheck size={18} /> {toast.message}
        </div>
      )}

      {/* --- SIDEBAR --- */}
      <aside style={{
        width: '260px',
        background: 'var(--bg-sidebar)',
        borderRight: '1px solid var(--border-main)',
        display: 'flex',
        flexDirection: 'column',
        position: 'sticky',
        top: 0,
        height: '100vh',
        zIndex: 20
      }}>
        {/* Brand */}
        <div style={{ padding: '20px 18px', borderBottom: '1px solid var(--border-main)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{
              width: '36px',
              height: '36px',
              borderRadius: '8px',
              background: '#2563eb',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '1.1rem',
              color: '#fff'
            }}>
              HS
            </div>
            <div>
              <div style={{ fontSize: '1rem', fontWeight: 800, color: '#fff' }}>HireSphere</div>
              <div style={{ fontSize: '0.7rem', color: '#94a3b8', fontWeight: 600 }}>ATS & Skill-Match Engine</div>
            </div>
          </div>
        </div>

        {/* Nav Links */}
        <nav style={{ padding: '16px 10px', flex: 1, display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <NavBtn
            active={activeTab === 'pipeline'}
            onClick={() => setActiveTab('pipeline')}
            icon={<Kanban size={18} />}
            label="1. Hiring Pipeline"
            badge={currentJobApps.length}
          />
          <NavBtn
            active={activeTab === 'leaderboard'}
            onClick={() => setActiveTab('leaderboard')}
            icon={<Award size={18} />}
            label="2. Ranked Leaderboard"
          />
          <NavBtn
            active={activeTab === 'parser'}
            onClick={() => setActiveTab('parser')}
            icon={<FileCheck2 size={18} />}
            label="3. Instant Resume Matcher"
            highlight={true}
          />
          <NavBtn
            active={activeTab === 'jobs'}
            onClick={() => setActiveTab('jobs')}
            icon={<Briefcase size={18} />}
            label="Job Requisitions"
            badge={jobs.length}
          />
          <NavBtn
            active={activeTab === 'candidates'}
            onClick={() => setActiveTab('candidates')}
            icon={<Users size={18} />}
            label="Talent Directory"
            badge={candidates.length}
          />
          <NavBtn
            active={activeTab === 'analytics'}
            onClick={() => setActiveTab('analytics')}
            icon={<LayoutDashboard size={18} />}
            label="Recruiter Analytics"
          />
        </nav>

        {/* Database Status */}
        <div style={{ padding: '16px', borderTop: '1px solid var(--border-main)' }}>
          <div style={{
            background: 'rgba(255, 255, 255, 0.03)',
            border: '1px solid var(--border-main)',
            borderRadius: '8px',
            padding: '12px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10b981' }} />
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#f8fafc' }}>
                  {systemHealth ? systemHealth.databaseType : 'SQLITE'} CONNECTED
                </span>
              </div>
              <Database size={14} color="#64748b" />
            </div>
            <div style={{ fontSize: '0.675rem', color: '#94a3b8' }}>
              Database: <strong>hiresphere.db</strong> (Active)
            </div>
          </div>
        </div>
      </aside>

      {/* --- MAIN CONTENT --- */}
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        
        {/* Top Header & Requisition Selector */}
        <header style={{
          height: '68px',
          background: 'var(--bg-sidebar)',
          borderBottom: '1px solid var(--border-main)',
          padding: '0 28px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          position: 'sticky',
          top: 0,
          zIndex: 10
        }}>
          {/* Active Job Selector */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#94a3b8' }}>Active Job:</span>
            <select
              value={selectedJobId || ''}
              onChange={(e) => setSelectedJobId(Number(e.target.value))}
              className="select"
              style={{ width: '310px', fontWeight: 700, color: '#fff' }}
            >
              {jobs.map(j => (
                <option key={j.jobId} value={j.jobId}>
                  #{j.jobId} - {j.title} ({j.department})
                </option>
              ))}
            </select>
          </div>

          {/* Quick Header Actions */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            {/* Instant Demo Seed Button */}
            <button
              onClick={handleDemoSync}
              className="btn btn-primary btn-sm"
              style={{ background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)' }}
              title="Click to populate demo jobs, resumes, and scores"
            >
              <Sparkles size={14} /> 1-Click Demo Setup
            </button>

            <button onClick={() => setShowJobModal(true)} className="btn btn-secondary btn-sm">
              <Plus size={14} /> New Job
            </button>

            <button onClick={() => setShowCandModal(true)} className="btn btn-secondary btn-sm">
              <Plus size={14} /> Add Candidate
            </button>

            <button onClick={loadAllData} className="btn-icon" title="Reload Database Records">
              <RefreshCw size={15} />
            </button>
          </div>
        </header>

        {/* Global Guided Demo Help Bar */}
        <div style={{
          background: 'rgba(37, 99, 235, 0.1)',
          borderBottom: '1px solid rgba(59, 130, 246, 0.2)',
          padding: '10px 28px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          fontSize: '0.8rem'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#93c5fd' }}>
            <Sparkles size={15} color="#38bdf8" />
            <span>
              <strong>Evaluator Quick Tour:</strong> 1. Select Position &rarr; 2. Click <strong>"Screen Candidates"</strong> &rarr; 3. Click any applicant to inspect their <strong>Skill-Match Scorecard & Gaps</strong>!
            </span>
          </div>

          {currentJob && (
            <button
              onClick={() => handleBatchScreen(currentJob.jobId)}
              className="btn btn-secondary btn-sm"
              style={{ padding: '3px 10px', fontSize: '0.75rem', borderColor: '#3b82f6', color: '#93c5fd' }}
            >
              ⚡ Run ATS Batch-Screen for this Job
            </button>
          )}
        </div>

        {/* View Router */}
        <div style={{ padding: '24px 28px', flex: 1 }}>
          {activeTab === 'pipeline' && (
            <PipelineKanbanView
              job={currentJob}
              applications={currentJobApps}
              onCardClick={openDossier}
              onUpdateStatus={handleUpdateStatus}
              onRunScreen={() => handleBatchScreen(currentJob.jobId)}
            />
          )}

          {activeTab === 'leaderboard' && (
            <RankedLeaderboardView
              job={currentJob}
              applications={currentJobApps}
              onRowClick={openDossier}
              onUpdateStatus={handleUpdateStatus}
            />
          )}

          {activeTab === 'parser' && (
            <ResumeParserView
              jobs={jobs}
              selectedJobId={selectedJobId}
              onImportSuccess={(createdCandId) => {
                loadAllData();
                showToast(`✔ Candidate #${createdCandId} parsed, scored, and stored in Database!`);
                setActiveTab('pipeline');
              }}
            />
          )}

          {activeTab === 'jobs' && (
            <JobRequisitionsView
              jobs={jobs}
              onOpenCreate={() => setShowJobModal(true)}
              onSelectJob={(id) => {
                setSelectedJobId(id);
                setActiveTab('pipeline');
              }}
            />
          )}

          {activeTab === 'candidates' && (
            <CandidateDirectoryView
              candidates={candidates}
              onOpenRegister={() => setShowCandModal(true)}
              onSelectCandidate={(cand) => {
                const app = applications.find(a => a.candidateId === cand.candidateId);
                if (app) {
                  openDossier(app);
                } else if (jobs.length > 0) {
                  openDossier({
                    applicationId: 0,
                    jobId: jobs[0].jobId,
                    candidateId: cand.candidateId,
                    status: 'APPLIED',
                    matchScore: 0
                  });
                }
              }}
            />
          )}

          {activeTab === 'analytics' && (
            <AnalyticsDashboardView
              funnel={funnel}
              skillDemand={skillDemand}
              applications={applications}
            />
          )}
        </div>
      </main>

      {/* --- SLIDE-OVER CANDIDATE DOSSIER --- */}
      {activeDossier && (
        <CandidateDossierDrawer
          dossier={activeDossier}
          onClose={() => setActiveDossier(null)}
          onUpdateStatus={handleUpdateStatus}
          onAddNote={handleAddNote}
        />
      )}

      {/* --- CREATE JOB MODAL --- */}
      {showJobModal && (
        <CreateJobModal
          onClose={() => setShowJobModal(false)}
          onSuccess={(jobTitle) => {
            setShowJobModal(false);
            loadAllData();
            showToast(`✔ Job Requisition "${jobTitle}" stored in Database!`);
          }}
        />
      )}

      {/* --- ADD CANDIDATE MODAL --- */}
      {showCandModal && (
        <AddCandidateModal
          onClose={() => setShowCandModal(false)}
          onSuccess={(candName) => {
            setShowCandModal(false);
            loadAllData();
            showToast(`✔ Candidate "${candName}" profile stored in Database!`);
          }}
        />
      )}

    </div>
  );
}

// -------------------------------------------------------------
// VIEW 1: KANBAN HIRING PIPELINE BOARD
// -------------------------------------------------------------

function PipelineKanbanView({ job, applications, onCardClick, onUpdateStatus, onRunScreen }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
      
      {/* Active Position Summary Card */}
      <div className="card" style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '14px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h2 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#fff' }}>
              {job ? job.title : 'Select a Job Position'}
            </h2>
            <span className="badge badge-neutral">{job ? job.department : 'General'}</span>
            <span className="badge badge-tier1">{applications.length} Candidates Screened</span>
          </div>
          <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginTop: '4px' }}>
            Benchmark: {job ? `Min ${job.minExpYears} yrs experience • ${job.minEducation}` : ''}
          </div>
        </div>

        {/* Skills Required Chips */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
            {job && job.requiredSkills && job.requiredSkills.map((sk, i) => (
              <span key={i} style={{
                fontSize: '0.725rem',
                padding: '3px 8px',
                borderRadius: '5px',
                background: sk.weightType === 'MANDATORY' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(59, 130, 246, 0.15)',
                color: sk.weightType === 'MANDATORY' ? '#f87171' : '#60a5fa',
                border: `1px solid ${sk.weightType === 'MANDATORY' ? 'rgba(239, 68, 68, 0.3)' : 'rgba(59, 130, 246, 0.3)'}`,
                fontWeight: 600
              }}>
                {sk.skillName} ({sk.weightType === 'MANDATORY' ? 'Mandatory 3x' : 'Preferred 2x'})
              </span>
            ))}
          </div>

          <button onClick={onRunScreen} className="btn btn-secondary btn-sm" style={{ marginLeft: '10px' }}>
            <RefreshCw size={14} /> Screen All
          </button>
        </div>
      </div>

      {/* Kanban Stages Columns */}
      <div style={{ display: 'flex', gap: '16px', overflowX: 'auto', paddingBottom: '16px' }}>
        {PIPELINE_STAGES.map(stage => {
          const stageApps = applications.filter(a => {
            if (stage.id === 'APPLIED') return a.status === 'APPLIED' || !a.status;
            return a.status === stage.id;
          });

          return (
            <div key={stage.id} className="kanban-col">
              <div className="kanban-col-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: stage.color }} />
                  <span style={{ fontSize: '0.825rem', fontWeight: 800, color: '#f8fafc' }}>
                    {stage.label}
                  </span>
                </div>
                <span className="badge badge-neutral" style={{ fontSize: '0.7rem' }}>
                  {stageApps.length}
                </span>
              </div>

              <div className="kanban-cards-container">
                {stageApps.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '36px 12px', color: '#64748b', fontSize: '0.775rem' }}>
                    No applicants in this stage
                  </div>
                ) : (
                  stageApps.map(app => {
                    const score = app.matchScore;
                    const tierClass = score >= 80 ? 'badge-tier1' : score >= 60 ? 'badge-tier2' : 'badge-tier3';

                    return (
                      <div
                        key={app.applicationId}
                        className="kanban-card"
                        onClick={() => onCardClick(app)}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                          <div style={{ fontWeight: 800, fontSize: '0.925rem', color: '#fff' }}>
                            {app.candidateName}
                          </div>
                          <span className={`badge ${tierClass}`}>
                            {score.toFixed(0)}% Match
                          </span>
                        </div>

                        <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '10px' }}>
                          {app.candidateEmail}
                        </div>

                        {/* Factor breakdown mini bars */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginBottom: '12px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.675rem', color: '#64748b' }}>
                            <span>Skill Coverage:</span>
                            <span style={{ color: '#cbd5e1' }}>{app.skillCoverageScore}%</span>
                          </div>
                          <div style={{ height: '4px', background: '#1e293b', borderRadius: '2px', overflow: 'hidden' }}>
                            <div style={{ width: `${app.skillCoverageScore}%`, height: '100%', background: '#3b82f6' }} />
                          </div>
                        </div>

                        {/* Advance Stage Control */}
                        <div
                          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-main)', paddingTop: '8px' }}
                          onClick={(e) => e.stopPropagation()}
                        >
                          <span style={{ fontSize: '0.675rem', color: '#64748b' }}>Move stage:</span>
                          <select
                            value={app.status || 'APPLIED'}
                            onChange={(e) => onUpdateStatus(app.applicationId, e.target.value)}
                            className="select"
                            style={{ padding: '3px 6px', fontSize: '0.725rem', width: 'auto' }}
                          >
                            {PIPELINE_STAGES.map(s => (
                              <option key={s.id} value={s.id}>{s.label}</option>
                            ))}
                          </select>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          );
        })}
      </div>

    </div>
  );
}

// -------------------------------------------------------------
// VIEW 2: RANKED LEADERBOARD TABLE
// -------------------------------------------------------------

function RankedLeaderboardView({ job, applications, onRowClick, onUpdateStatus }) {
  const sorted = [...applications].sort((a, b) => b.matchScore - a.matchScore);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div className="card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff' }}>Applicant Ranking Leaderboard</h2>
          <p style={{ fontSize: '0.8rem', color: '#94a3b8', marginTop: '2px' }}>
            Computed via <code>SkillMatchEngine</code> and sorted by <code>match_score DESC</code>
          </p>
        </div>
        <span className="badge badge-neutral" style={{ fontSize: '0.8rem' }}>
          {sorted.length} Ranked Candidates
        </span>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: '70px' }}>Rank</th>
              <th>Applicant Profile</th>
              <th>Composite Match</th>
              <th>Skill Coverage (55%)</th>
              <th>Exp Alignment (25%)</th>
              <th>Proficiency (20%)</th>
              <th>Current Stage</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {sorted.length === 0 ? (
              <tr>
                <td colSpan="8" style={{ textAlign: 'center', padding: '40px', color: '#64748b' }}>
                  No candidates screened for this position yet.
                </td>
              </tr>
            ) : (
              sorted.map((app, index) => {
                const score = app.matchScore;
                const tierClass = score >= 80 ? 'badge-tier1' : score >= 60 ? 'badge-tier2' : 'badge-tier3';

                return (
                  <tr key={app.applicationId} style={{ cursor: 'pointer' }} onClick={() => onRowClick(app)}>
                    <td>
                      <span style={{
                        fontWeight: 800,
                        color: index === 0 ? '#fbbf24' : index === 1 ? '#cbd5e1' : index === 2 ? '#d97706' : '#64748b'
                      }}>
                        #{index + 1}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontWeight: 800, color: '#fff' }}>{app.candidateName}</div>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{app.candidateEmail}</div>
                    </td>
                    <td>
                      <span className={`badge ${tierClass}`}>
                        {score.toFixed(1)}% {score >= 80 ? 'Tier 1' : score >= 60 ? 'Tier 2' : 'Tier 3'}
                      </span>
                    </td>
                    <td style={{ color: '#cbd5e1' }}>{app.skillCoverageScore}%</td>
                    <td style={{ color: '#cbd5e1' }}>{app.experienceFactor}%</td>
                    <td style={{ color: '#cbd5e1' }}>{app.proficiencyFactor}%</td>
                    <td>
                      <span className="badge badge-neutral">
                        {app.status || 'APPLIED'}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right' }} onClick={(e) => e.stopPropagation()}>
                      <div style={{ display: 'inline-flex', gap: '6px' }}>
                        <button
                          onClick={() => onUpdateStatus(app.applicationId, 'INTERVIEW')}
                          className="btn btn-secondary btn-sm"
                          style={{ padding: '4px 8px', fontSize: '0.725rem' }}
                        >
                          Interview
                        </button>
                        <button
                          onClick={() => onUpdateStatus(app.applicationId, 'HIRED')}
                          className="btn btn-primary btn-sm"
                          style={{ padding: '4px 8px', fontSize: '0.725rem' }}
                        >
                          Hire
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// -------------------------------------------------------------
// VIEW 3: INSTANT RESUME MATCHER & HEURISTIC PARSER
// -------------------------------------------------------------

function ResumeParserView({ jobs, selectedJobId, onImportSuccess }) {
  const [resumeText, setResumeText] = useState(DEMO_RESUMES.tier1.text);
  const [targetJobId, setTargetJobId] = useState(selectedJobId || (jobs[0] ? jobs[0].jobId : 1));
  const [parsing, setParsing] = useState(false);
  const [parsedCandidate, setParsedCandidate] = useState(null);
  const [importing, setImporting] = useState(false);

  const handleSelectSample = (key) => {
    setResumeText(DEMO_RESUMES[key].text);
    setParsedCandidate(null);
  };

  const handlePreviewParse = async () => {
    setParsing(true);
    try {
      const res = await fetch('/api/resumes/parse', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ resumeText })
      });
      const data = await res.json();
      setParsedCandidate(data);
    } catch (e) {
      console.error(e);
    } finally {
      setParsing(false);
    }
  };

  const handleSaveAndIngest = async () => {
    setImporting(true);
    try {
      const res = await fetch('/api/resumes/import', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ resumeText, jobId: String(targetJobId) })
      });
      const data = await res.json();
      onImportSuccess(data.candidateId);
    } catch (e) {
      console.error(e);
    } finally {
      setImporting(false);
    }
  };

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '24px' }}>
      
      {/* Input Resume Box */}
      <div className="card" style={{ padding: '24px', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <div>
            <h2 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#fff' }}>Heuristic Resume Matcher</h2>
            <p style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
              Paste any raw resume text or select a realistic demo profile below:
            </p>
          </div>
        </div>

        {/* 1-Click Sample Resumes */}
        <div style={{ display: 'flex', gap: '8px', marginBottom: '14px', flexWrap: 'wrap' }}>
          <button onClick={() => handleSelectSample('tier1')} className="btn btn-secondary btn-sm">
            🟢 {DEMO_RESUMES.tier1.label}
          </button>
          <button onClick={() => handleSelectSample('tier2')} className="btn btn-secondary btn-sm">
            🟡 {DEMO_RESUMES.tier2.label}
          </button>
          <button onClick={() => handleSelectSample('tier3')} className="btn btn-secondary btn-sm">
            🔴 {DEMO_RESUMES.tier3.label}
          </button>
        </div>

        <textarea
          rows="16"
          value={resumeText}
          onChange={(e) => setResumeText(e.target.value)}
          placeholder="Paste candidate resume text here..."
          className="textarea"
          style={{ fontFamily: 'monospace', fontSize: '0.8rem', lineHeight: 1.5, flex: 1, resize: 'vertical' }}
        />

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Target Position:</span>
            <select
              value={targetJobId}
              onChange={(e) => setTargetJobId(Number(e.target.value))}
              className="select"
              style={{ width: '220px', padding: '6px 8px', fontSize: '0.8rem' }}
            >
              {jobs.map(j => (
                <option key={j.jobId} value={j.jobId}>#{j.jobId} - {j.title}</option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <button onClick={handlePreviewParse} disabled={parsing} className="btn btn-secondary">
              <FileCheck2 size={16} /> {parsing ? 'Extracting...' : 'Preview Extracted Fields'}
            </button>
            <button onClick={handleSaveAndIngest} disabled={importing} className="btn btn-primary">
              <UploadCloud size={16} /> {importing ? 'Storing in DB...' : 'Save & Ingest Candidate'}
            </button>
          </div>
        </div>
      </div>

      {/* Extracted Fields Preview */}
      <div className="card" style={{ padding: '24px', display: 'flex', flexDirection: 'column' }}>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 800, color: '#fff', marginBottom: '14px' }}>
          Extracted Entities & Matching
        </h3>

        {parsedCandidate ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ padding: '14px', background: '#0b1120', borderRadius: '8px', border: '1px solid var(--border-main)' }}>
              <div style={{ fontSize: '1.15rem', fontWeight: 800, color: '#fff' }}>{parsedCandidate.fullName}</div>
              <div style={{ fontSize: '0.8rem', color: '#38bdf8', marginTop: '2px' }}>{parsedCandidate.email}</div>
              <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginTop: '2px' }}>{parsedCandidate.phone}</div>
              <div style={{ fontSize: '0.8rem', color: '#cbd5e1', marginTop: '8px' }}>
                Total Experience: <strong>{parsedCandidate.totalExpYears} years</strong> • Education: <strong>{parsedCandidate.highestEducation}</strong>
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', marginBottom: '8px' }}>
                Detected Skills in Master Database ({parsedCandidate.skills ? parsedCandidate.skills.length : 0}):
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                {parsedCandidate.skills && parsedCandidate.skills.map((sk, i) => (
                  <span key={i} style={{
                    padding: '4px 8px',
                    borderRadius: '6px',
                    background: 'rgba(59, 130, 246, 0.15)',
                    border: '1px solid rgba(59, 130, 246, 0.3)',
                    color: '#60a5fa',
                    fontSize: '0.8rem',
                    fontWeight: 600
                  }}>
                    {sk.skillName} (Lvl {sk.proficiencyLevel}/5, {sk.yearsExperience} yrs)
                  </span>
                ))}
              </div>
            </div>

            <div style={{ marginTop: 'auto', padding: '14px', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.25)', borderRadius: '8px' }}>
              <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#34d399' }}>Ready for Database Ingestion</div>
              <p style={{ fontSize: '0.75rem', color: '#94a3b8', marginTop: '2px' }}>
                Click "Save & Ingest Candidate" to commit this candidate and their skills via JDBC transaction!
              </p>
            </div>
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '60px 20px', color: '#64748b', fontSize: '0.85rem' }}>
            Select a sample resume on the left or click <strong>"Preview Extracted Fields"</strong> to see how raw text is broken down into relational records.
          </div>
        )}
      </div>

    </div>
  );
}

// -------------------------------------------------------------
// VIEW 4: JOB REQUISITIONS
// -------------------------------------------------------------

function JobRequisitionsView({ jobs, onOpenCreate, onSelectJob }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff' }}>Job Requisitions Catalog</h2>
          <p style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Corporate positions with weighted skill requirements</p>
        </div>
        <button onClick={onOpenCreate} className="btn btn-primary">
          <Plus size={16} /> New Job Requisition
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: '20px' }}>
        {jobs.map(job => (
          <div key={job.jobId} className="card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <span className="badge badge-neutral">{job.department}</span>
                <span className="badge badge-tier1">{job.status}</span>
              </div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#fff', marginBottom: '8px' }}>{job.title}</h3>
              <p style={{ fontSize: '0.8rem', color: '#94a3b8', marginBottom: '16px' }}>
                Min Experience: <strong>{job.minExpYears} yrs</strong> • Degree: <strong>{job.minEducation}</strong>
              </p>

              <div style={{ marginBottom: '16px' }}>
                <div style={{ fontSize: '0.725rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', marginBottom: '6px' }}>
                  Weighted Skills Required:
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px' }}>
                  {job.requiredSkills && job.requiredSkills.map((sk, i) => (
                    <span key={i} style={{
                      padding: '3px 8px',
                      borderRadius: '5px',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      background: sk.weightType === 'MANDATORY' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(59, 130, 246, 0.15)',
                      color: sk.weightType === 'MANDATORY' ? '#f87171' : '#60a5fa',
                      border: `1px solid ${sk.weightType === 'MANDATORY' ? 'rgba(239, 68, 68, 0.25)' : 'rgba(59, 130, 246, 0.25)'}`
                    }}>
                      {sk.skillName} ({sk.weightType === 'MANDATORY' ? '3x' : '2x'}, Min: {sk.minProficiency}/5)
                    </span>
                  ))}
                </div>
              </div>
            </div>

            <button onClick={() => onSelectJob(job.jobId)} className="btn btn-secondary" style={{ width: '100%', justifyContent: 'center' }}>
              View Pipeline Candidates <ChevronRight size={15} />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

// -------------------------------------------------------------
// VIEW 5: CANDIDATE DIRECTORY
// -------------------------------------------------------------

function CandidateDirectoryView({ candidates, onOpenRegister, onSelectCandidate }) {
  const [search, setSearch] = useState('');
  const filtered = candidates.filter(c =>
    c.fullName.toLowerCase().includes(search.toLowerCase()) ||
    c.email.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ position: 'relative', width: '320px' }}>
          <Search size={15} color="#64748b" style={{ position: 'absolute', left: '12px', top: '12px' }} />
          <input
            type="text"
            placeholder="Search candidate directory..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="input"
            style={{ paddingLeft: '36px' }}
          />
        </div>
        <button onClick={onOpenRegister} className="btn btn-primary">
          <Plus size={16} /> Register Candidate
        </button>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Applicant Name</th>
              <th>Email & Contact</th>
              <th>Experience</th>
              <th>Education</th>
              <th>Verified Skills</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(cand => (
              <tr key={cand.candidateId} style={{ cursor: 'pointer' }} onClick={() => onSelectCandidate(cand)}>
                <td style={{ fontWeight: 700, color: '#64748b' }}>#{cand.candidateId}</td>
                <td>
                  <div style={{ fontWeight: 800, color: '#fff' }}>{cand.fullName}</div>
                  <div style={{ fontSize: '0.725rem', color: '#64748b', maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {cand.resumeSummary}
                  </div>
                </td>
                <td>
                  <div style={{ color: '#cbd5e1' }}>{cand.email}</div>
                  <div style={{ fontSize: '0.725rem', color: '#64748b' }}>{cand.phone}</div>
                </td>
                <td style={{ fontWeight: 700, color: '#f8fafc' }}>{cand.totalExpYears} yrs</td>
                <td style={{ color: '#94a3b8' }}>{cand.highestEducation}</td>
                <td>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', maxWidth: '320px' }}>
                    {cand.skills && cand.skills.map((s, i) => (
                      <span key={i} style={{ padding: '2px 6px', borderRadius: '4px', background: '#0b1120', fontSize: '0.725rem', color: '#94a3b8' }}>
                        {s.skillName} ({s.proficiencyLevel}/5)
                      </span>
                    ))}
                  </div>
                </td>
                <td style={{ textAlign: 'right' }}>
                  <button className="btn btn-ghost btn-sm">Inspect Dossier &rarr;</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// -------------------------------------------------------------
// VIEW 6: RECRUITER TALENT ANALYTICS
// -------------------------------------------------------------

function AnalyticsDashboardView({ funnel, skillDemand, applications }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: '16px' }}>
        <MetricCard title="Active Requisitions" value={funnel ? `${funnel.openJobs}` : '0'} sub={`${funnel ? funnel.totalJobs : 0} Total in Database`} />
        <MetricCard title="Talent Pool Size" value={funnel ? funnel.totalCandidates : 0} sub="Candidates indexed" />
        <MetricCard title="Screened Pipeline" value={funnel ? funnel.totalApplications : 0} sub="Evaluated by Engine" />
        <MetricCard title="Shortlisted Talent" value={funnel ? funnel.shortlistedCount : 0} sub="Ready for interview" color="#34d399" />
        <MetricCard title="ATS Average Score" value={funnel ? `${funnel.avgMatchScore}%` : '0%'} sub="Across all applicants" color="#fbbf24" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '20px' }}>
        <div className="card" style={{ padding: '24px' }}>
          <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#fff', marginBottom: '4px' }}>Technical Skill Demand Matrix</h3>
          <p style={{ fontSize: '0.8rem', color: '#94a3b8', marginBottom: '20px' }}>Aggregated across all active open requisitions in 3NF schema</p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {skillDemand.map((sd, i) => {
              const max = Math.max(...skillDemand.map(x => x.demand), 1);
              const pct = Math.round((sd.demand / max) * 100);
              return (
                <div key={i}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '4px' }}>
                    <span style={{ fontWeight: 600, color: '#f8fafc' }}>{sd.skill}</span>
                    <span style={{ color: '#3b82f6', fontWeight: 700 }}>{sd.demand} Requisitions</span>
                  </div>
                  <div style={{ height: '6px', background: '#0b1120', borderRadius: '3px', overflow: 'hidden' }}>
                    <div style={{ width: `${pct}%`, height: '100%', background: '#3b82f6', borderRadius: '3px' }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="card" style={{ padding: '24px' }}>
          <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#fff', marginBottom: '4px' }}>Hiring Conversion Funnel</h3>
          <p style={{ fontSize: '0.8rem', color: '#94a3b8', marginBottom: '20px' }}>Candidates progressing through stages</p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {PIPELINE_STAGES.map(s => {
              const count = applications.filter(a => (a.status || 'APPLIED') === s.id).length;
              const total = Math.max(applications.length, 1);
              const pct = Math.round((count / total) * 100);
              return (
                <div key={s.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', background: '#0b1120', borderRadius: '8px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: s.color }} />
                    <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#fff' }}>{s.label}</span>
                  </div>
                  <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#cbd5e1' }}>
                    {count} ({pct}%)
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      </div>

    </div>
  );
}

function MetricCard({ title, value, sub, color }) {
  return (
    <div className="card" style={{ padding: '18px' }}>
      <div style={{ fontSize: '0.725rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase' }}>{title}</div>
      <div style={{ fontSize: '1.45rem', fontWeight: 800, color: color || '#fff', marginTop: '4px' }}>{value}</div>
      <div style={{ fontSize: '0.7rem', color: '#94a3b8', marginTop: '2px' }}>{sub}</div>
    </div>
  );
}

// -------------------------------------------------------------
// COMPONENT: SLIDE-OVER CANDIDATE DOSSIER DRAWER
// -------------------------------------------------------------

function CandidateDossierDrawer({ dossier, onClose, onUpdateStatus, onAddNote }) {
  const { candidate, job, application, matchResult, notes } = dossier;
  const [tab, setTab] = useState('match'); // match | resume | notes
  const [newNote, setNewNote] = useState('');
  const [rating, setRating] = useState(4);
  const [author, setAuthor] = useState('Lead Interviewer');

  const score = matchResult ? matchResult.compositeScore : application.matchScore;
  const tierClass = score >= 80 ? 'badge-tier1' : score >= 60 ? 'badge-tier2' : 'badge-tier3';

  const submitNote = (e) => {
    e.preventDefault();
    if (!newNote.trim()) return;
    onAddNote(application.applicationId, author, newNote, rating);
    setNewNote('');
  };

  return (
    <div className="drawer-overlay" onClick={onClose}>
      <div className="drawer-content" onClick={(e) => e.stopPropagation()}>
        
        {/* Drawer Header */}
        <div style={{ padding: '24px', borderBottom: '1px solid var(--border-main)', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{
                width: '44px',
                height: '44px',
                borderRadius: '10px',
                background: '#2563eb',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 800,
                fontSize: '1.1rem'
              }}>
                {candidate.fullName.charAt(0)}
              </div>
              <div>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff' }}>{candidate.fullName}</h3>
                <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                  {candidate.email} • {candidate.phone}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
              <span className={`badge ${tierClass}`}>
                {score.toFixed(1)}% Match ({score >= 80 ? 'Tier 1 Strong' : score >= 60 ? 'Tier 2 Moderate' : 'Tier 3 Deficit'})
              </span>
              <span className="badge badge-neutral">Stage: {application.status || 'APPLIED'}</span>
              <span className="badge badge-neutral">Exp: {candidate.totalExpYears} yrs</span>
            </div>
          </div>

          <button onClick={onClose} className="btn-icon"><X size={18} /></button>
        </div>

        {/* Tab Switcher */}
        <div style={{ display: 'flex', borderBottom: '1px solid var(--border-main)', padding: '0 24px' }}>
          <button
            onClick={() => setTab('match')}
            className="btn btn-ghost"
            style={{
              borderRadius: 0,
              borderBottom: tab === 'match' ? '2px solid #3b82f6' : '2px solid transparent',
              color: tab === 'match' ? '#fff' : '#94a3b8',
              fontWeight: tab === 'match' ? 700 : 500
            }}
          >
            ATS Scorecard & Gaps
          </button>
          <button
            onClick={() => setTab('resume')}
            className="btn btn-ghost"
            style={{
              borderRadius: 0,
              borderBottom: tab === 'resume' ? '2px solid #3b82f6' : '2px solid transparent',
              color: tab === 'resume' ? '#fff' : '#94a3b8',
              fontWeight: tab === 'resume' ? 700 : 500
            }}
          >
            Resume Text
          </button>
          <button
            onClick={() => setTab('notes')}
            className="btn btn-ghost"
            style={{
              borderRadius: 0,
              borderBottom: tab === 'notes' ? '2px solid #3b82f6' : '2px solid transparent',
              color: tab === 'notes' ? '#fff' : '#94a3b8',
              fontWeight: tab === 'notes' ? 700 : 500
            }}
          >
            Evaluation Notes ({notes ? notes.length : 0})
          </button>
        </div>

        {/* Drawer Body */}
        <div style={{ padding: '24px', flex: 1, overflowY: 'auto' }}>
          {tab === 'match' && matchResult && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              
              <div style={{ background: '#0b1120', padding: '16px', borderRadius: '8px', border: '1px solid var(--border-main)' }}>
                <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#f8fafc', marginBottom: '12px' }}>
                  Multi-Factor Weight Breakdown:
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: '#94a3b8', marginBottom: '4px' }}>
                      <span>Skill Coverage (55% weight)</span>
                      <strong style={{ color: '#fff' }}>{matchResult.skillCoverageScore}%</strong>
                    </div>
                    <div style={{ height: '6px', background: '#1e293b', borderRadius: '3px', overflow: 'hidden' }}>
                      <div style={{ width: `${matchResult.skillCoverageScore}%`, height: '100%', background: '#3b82f6' }} />
                    </div>
                  </div>

                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: '#94a3b8', marginBottom: '4px' }}>
                      <span>Experience Alignment (25% weight)</span>
                      <strong style={{ color: '#fff' }}>{matchResult.experienceFactor}%</strong>
                    </div>
                    <div style={{ height: '6px', background: '#1e293b', borderRadius: '3px', overflow: 'hidden' }}>
                      <div style={{ width: `${matchResult.experienceFactor}%`, height: '100%', background: '#a855f7' }} />
                    </div>
                  </div>

                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: '#94a3b8', marginBottom: '4px' }}>
                      <span>Proficiency Benchmark (20% weight)</span>
                      <strong style={{ color: '#fff' }}>{matchResult.proficiencyFactor}%</strong>
                    </div>
                    <div style={{ height: '6px', background: '#1e293b', borderRadius: '3px', overflow: 'hidden' }}>
                      <div style={{ width: `${matchResult.proficiencyFactor}%`, height: '100%', background: '#10b981' }} />
                    </div>
                  </div>
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#10b981', textTransform: 'uppercase', marginBottom: '8px' }}>
                  ✔ Verified Matching Skills ({matchResult.matchedSkills.length}):
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                  {matchResult.matchedSkills.map((ms, i) => (
                    <span key={i} style={{ padding: '4px 8px', borderRadius: '5px', background: 'rgba(16, 185, 129, 0.15)', color: '#34d399', fontSize: '0.75rem', fontWeight: 600 }}>
                      ✔ {ms}
                    </span>
                  ))}
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#ef4444', textTransform: 'uppercase', marginBottom: '8px' }}>
                  ▲ Critical Qualification Gaps ({matchResult.skillGaps.length}):
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {matchResult.skillGaps.length === 0 ? (
                    <div style={{ fontSize: '0.8rem', color: '#34d399', background: 'rgba(16, 185, 129, 0.1)', padding: '10px', borderRadius: '6px' }}>
                      Zero critical gaps detected! Applicant exceeds all baseline constraints.
                    </div>
                  ) : (
                    matchResult.skillGaps.map((gap, i) => (
                      <div key={i} style={{
                        padding: '10px 12px',
                        borderRadius: '6px',
                        background: gap.gapType === 'MISSING_MANDATORY_SKILL' ? 'rgba(239, 68, 68, 0.12)' : 'rgba(245, 158, 11, 0.1)',
                        border: `1px solid ${gap.gapType === 'MISSING_MANDATORY_SKILL' ? 'rgba(239, 68, 68, 0.3)' : 'rgba(245, 158, 11, 0.3)'}`,
                        fontSize: '0.8rem'
                      }}>
                        <div style={{ fontWeight: 700, color: gap.gapType === 'MISSING_MANDATORY_SKILL' ? '#f87171' : '#fbbf24' }}>
                          {gap.gapType.replace(/_/g, ' ')}
                        </div>
                        <div style={{ color: '#cbd5e1', marginTop: '2px' }}>{gap.description}</div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}

          {tab === 'resume' && (
            <div style={{ background: '#0b1120', padding: '16px', borderRadius: '8px', border: '1px solid var(--border-main)', fontSize: '0.85rem', lineHeight: 1.6, whiteSpace: 'pre-wrap', color: '#cbd5e1' }}>
              {candidate.resumeSummary || 'No resume text on file.'}
            </div>
          )}

          {tab === 'notes' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <form onSubmit={submitNote} style={{ display: 'flex', flexDirection: 'column', gap: '10px', background: '#0b1120', padding: '14px', borderRadius: '8px' }}>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <input
                    type="text"
                    value={author}
                    onChange={(e) => setAuthor(e.target.value)}
                    placeholder="Interviewer Name"
                    className="input"
                    style={{ flex: 1, padding: '6px 10px', fontSize: '0.8rem' }}
                  />
                  <select
                    value={rating}
                    onChange={(e) => setRating(Number(e.target.value))}
                    className="select"
                    style={{ width: '130px', padding: '6px 10px', fontSize: '0.8rem' }}
                  >
                    <option value="5">⭐⭐⭐⭐⭐ (5/5)</option>
                    <option value="4">⭐⭐⭐⭐ (4/5)</option>
                    <option value="3">⭐⭐⭐ (3/5)</option>
                    <option value="2">⭐⭐ (2/5)</option>
                    <option value="1">⭐ (1/5)</option>
                  </select>
                </div>
                <textarea
                  rows="3"
                  value={newNote}
                  onChange={(e) => setNewNote(e.target.value)}
                  placeholder="Enter interview evaluation notes and technical feedback..."
                  className="textarea"
                  style={{ fontSize: '0.8rem' }}
                />
                <button type="submit" className="btn btn-primary btn-sm" style={{ alignSelf: 'flex-end' }}>
                  Submit Evaluation Note
                </button>
              </form>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {notes && notes.map((n, i) => (
                  <div key={i} style={{ padding: '12px', borderRadius: '6px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-main)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                      <span style={{ fontWeight: 700, fontSize: '0.8rem', color: '#fff' }}>{n.author}</span>
                      <span style={{ color: '#fbbf24', fontSize: '0.75rem' }}>{'★'.repeat(n.rating)}</span>
                    </div>
                    <div style={{ fontSize: '0.8rem', color: '#cbd5e1' }}>{n.noteText}</div>
                    <div style={{ fontSize: '0.675rem', color: '#64748b', marginTop: '4px' }}>{n.createdAt}</div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Advance Stage Footer */}
        <div style={{ padding: '16px 24px', borderTop: '1px solid var(--border-main)', background: '#0b1120', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Advance Stage:</span>
          <div style={{ display: 'flex', gap: '6px' }}>
            <button onClick={() => onUpdateStatus(application.applicationId, 'SCREENING')} className="btn btn-secondary btn-sm">
              Screening
            </button>
            <button onClick={() => onUpdateStatus(application.applicationId, 'INTERVIEW')} className="btn btn-secondary btn-sm">
              Interview
            </button>
            <button onClick={() => onUpdateStatus(application.applicationId, 'OFFERED')} className="btn btn-secondary btn-sm">
              Offer
            </button>
            <button onClick={() => onUpdateStatus(application.applicationId, 'HIRED')} className="btn btn-primary btn-sm">
              Hire
            </button>
            <button onClick={() => onUpdateStatus(application.applicationId, 'REJECTED')} className="btn btn-ghost btn-sm" style={{ color: '#ef4444' }}>
              Reject
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}

// -------------------------------------------------------------
// MODALS
// -------------------------------------------------------------

function CreateJobModal({ onClose, onSuccess }) {
  const [title, setTitle] = useState('');
  const [department, setDepartment] = useState('Engineering');
  const [minExpYears, setMinExpYears] = useState('3.5');
  const [minEducation, setMinEducation] = useState('Bachelor in Computer Science');
  const [skills, setSkills] = useState('Java:MANDATORY:4;Spring Boot:MANDATORY:3;MySQL:PREFERRED:3;Docker:OPTIONAL:2');

  const handleSubmit = async (e) => {
    e.preventDefault();
    await fetch('/api/jobs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title, department, minExpYears, minEducation, skills })
    });
    onSuccess(title);
  };

  return (
    <div className="drawer-overlay" style={{ alignItems: 'center', justifyContent: 'center' }}>
      <div className="card" style={{ width: '520px', padding: '28px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#fff' }}>Post New Job Requisition</h3>
          <button onClick={onClose} className="btn-icon"><X size={16} /></button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div>
            <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>Job Title</label>
            <input required type="text" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Senior Backend Engineer" className="input" style={{ marginTop: '4px' }} />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>Department</label>
              <input type="text" value={department} onChange={(e) => setDepartment(e.target.value)} className="input" style={{ marginTop: '4px' }} />
            </div>
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>Min Exp (Years)</label>
              <input type="number" step="0.5" value={minExpYears} onChange={(e) => setMinExpYears(e.target.value)} className="input" style={{ marginTop: '4px' }} />
            </div>
          </div>

          <div>
            <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>Minimum Education</label>
            <input type="text" value={minEducation} onChange={(e) => setMinEducation(e.target.value)} className="input" style={{ marginTop: '4px' }} />
          </div>

          <div>
            <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>
              Skill Constraints (Format: SkillName:MANDATORY:MinProficiency separated by semicolons)
            </label>
            <textarea rows="3" value={skills} onChange={(e) => setSkills(e.target.value)} className="textarea" style={{ marginTop: '4px' }} />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
            <button type="button" onClick={onClose} className="btn btn-secondary">Cancel</button>
            <button type="submit" className="btn btn-primary">Create Requisition</button>
          </div>
        </form>
      </div>
    </div>
  );
}

function AddCandidateModal({ onClose, onSuccess }) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('+1 (555) 019-4820');
  const [totalExpYears, setTotalExpYears] = useState('4.0');
  const [highestEducation, setHighestEducation] = useState('B.Tech in Computer Science');
  const [skills, setSkills] = useState('Java:4:4.0;Spring Boot:4:3.5;MySQL:3:3.0');

  const handleSubmit = async (e) => {
    e.preventDefault();
    await fetch('/api/candidates', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName, email, phone, totalExpYears, highestEducation, skills })
    });
    onSuccess(fullName);
  };

  return (
    <div className="drawer-overlay" style={{ alignItems: 'center', justifyContent: 'center' }}>
      <div className="card" style={{ width: '520px', padding: '28px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#fff' }}>Register Candidate Profile</h3>
          <button onClick={onClose} className="btn-icon"><X size={16} /></button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div>
            <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>Full Name</label>
            <input required type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} className="input" style={{ marginTop: '4px' }} />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>Email</label>
              <input required type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="input" style={{ marginTop: '4px' }} />
            </div>
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>Phone</label>
              <input type="text" value={phone} onChange={(e) => setPhone(e.target.value)} className="input" style={{ marginTop: '4px' }} />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>Total Exp (Years)</label>
              <input type="number" step="0.5" value={totalExpYears} onChange={(e) => setTotalExpYears(e.target.value)} className="input" style={{ marginTop: '4px' }} />
            </div>
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>Degree</label>
              <input type="text" value={highestEducation} onChange={(e) => setHighestEducation(e.target.value)} className="input" style={{ marginTop: '4px' }} />
            </div>
          </div>

          <div>
            <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8' }}>Skills (Format: Skill:Proficiency[1-5]:Years)</label>
            <input type="text" value={skills} onChange={(e) => setSkills(e.target.value)} className="input" style={{ marginTop: '4px' }} />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
            <button type="button" onClick={onClose} className="btn btn-secondary">Cancel</button>
            <button type="submit" className="btn btn-primary">Save Profile</button>
          </div>
        </form>
      </div>
    </div>
  );
}

function NavBtn({ active, onClick, icon, label, badge, highlight }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '9px 12px',
        borderRadius: '7px',
        border: '1px solid transparent',
        background: active ? 'rgba(59, 130, 246, 0.14)' : 'transparent',
        color: active ? '#ffffff' : '#94a3b8',
        fontWeight: active ? 700 : 500,
        fontSize: '0.85rem',
        cursor: 'pointer',
        textAlign: 'left'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <span style={{ color: active ? '#3b82f6' : highlight ? '#10b981' : '#64748b' }}>
          {icon}
        </span>
        <span>{label}</span>
      </div>
      {badge !== null && badge !== undefined && (
        <span style={{
          fontSize: '0.675rem',
          padding: '2px 6px',
          borderRadius: '999px',
          background: active ? 'rgba(59, 130, 246, 0.25)' : 'rgba(255, 255, 255, 0.05)',
          color: active ? '#93c5fd' : '#94a3b8',
          fontWeight: 700
        }}>
          {badge}
        </span>
      )}
    </button>
  );
}
