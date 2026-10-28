import React, { useState, useEffect } from 'react';
import {
  LayoutDashboard,
  Briefcase,
  Users,
  Award,
  Kanban,
  FileText,
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
  Building,
  MapPin,
  DollarSign,
  ChevronRight,
  SlidersHorizontal,
  UploadCloud,
  FileCheck2
} from 'lucide-react';

const PIPELINE_STAGES = [
  { id: 'APPLIED', label: 'New Applied', color: '#38bdf8' },
  { id: 'SCREENING', label: 'Screening', color: '#f59e0b' },
  { id: 'INTERVIEW', label: 'Interview', color: '#818cf8' },
  { id: 'OFFERED', label: 'Offer Sent', color: '#a855f7' },
  { id: 'HIRED', label: 'Hired', color: '#10b981' },
  { id: 'REJECTED', label: 'Archived / Rejected', color: '#ef4444' }
];

const SAMPLE_RESUMES = {
  javaBackend: `ARJUN NAIR
Email: arjun.nair@enterprise-talent.com
Phone: +1 (415) 890-2415
Location: San Francisco, CA | Remote
Education: B.Tech in Computer Science & Engineering (2019)

PROFESSIONAL SUMMARY
Senior Backend Software Engineer with 5.5 years of experience architecting distributed microservices and low-latency REST APIs. Specialized in Java, Spring Boot, MySQL, and Docker containerization. Proven track record leading cloud migrations on AWS.

TECHNICAL SKILLS
Languages & Frameworks: Java, Spring Boot, REST APIs, Microservices, Python
Databases: MySQL, PostgreSQL, Redis
Cloud & DevOps: Docker, Kubernetes, AWS, Git

EXPERIENCE
Lead Backend Engineer | FinTech Systems (2022 - Present)
- Architected high-throughput transaction processing engine in Java and Spring Boot serving 15M daily requests.
- Optimized MySQL schema and indexing, cutting 99th percentile query latency by 42%.
- Deployed microservices using Docker on AWS ECS with auto-scaling policies.`,

  cloudDevops: `VIKRAM SINGHANIA
Email: vikram.singh@cloudnative-dev.io
Phone: +1 (206) 555-8910
Location: Seattle, WA | Hybrid
Education: M.S. in Software Systems (2018)

PROFESSIONAL SUMMARY
Cloud Infrastructure and Platform Architect with 6.5 years of experience designing Kubernetes microservices platforms, AWS cloud networking, and CI/CD pipelines.

TECHNICAL SKILLS
Cloud & Orchestration: AWS, Kubernetes, Docker, Terraform
Backend: Java, Microservices, REST APIs, Python
Databases: PostgreSQL, Redis, Kafka

EXPERIENCE
Principal DevOps Architect | CloudSphere (2021 - Present)
- Managed 40+ node Kubernetes clusters running Java microservices on AWS.
- Built event-driven telemetry pipeline using Kafka and Redis.`,

  frontendReact: `SNEHA REDDY
Email: sneha.reddy@web-craft.dev
Phone: +1 (408) 555-7281
Location: San Jose, CA
Education: B.S. in Information Systems (2023)

PROFESSIONAL SUMMARY
Frontend Developer with 1.5 years of experience building responsive dashboards with React, JavaScript, HTML/CSS, and REST APIs.

TECHNICAL SKILLS
Frontend: React, JavaScript, HTML/CSS
Backend & Data: Python, MySQL, Git`
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

  // Selected filters
  const [selectedJobId, setSelectedJobId] = useState(null);

  // Active Dossier Drawer for inspecting candidate
  const [activeDossier, setActiveDossier] = useState(null); // { candidate, application, job, matchResult, notes }

  // Modals
  const [showJobModal, setShowJobModal] = useState(false);
  const [showCandModal, setShowCandModal] = useState(false);

  useEffect(() => {
    loadPortalData();
  }, []);

  const loadPortalData = async () => {
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
      console.error('Failed to load portal data:', e);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (appId, newStatus) => {
    try {
      await fetch(`/api/applications/${appId}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus })
      });
      // Refresh apps & funnel
      const aRes = await fetch('/api/applications').then(r => r.json());
      setApplications(aRes);
      fetch('/api/analytics/summary').then(r => r.json()).then(setFunnel);

      // If active dossier is currently open, update its status
      if (activeDossier && activeDossier.application && activeDossier.application.applicationId === appId) {
        setActiveDossier({
          ...activeDossier,
          application: { ...activeDossier.application, status: newStatus }
        });
      }
    } catch (e) {
      console.error(e);
    }
  };

  const openDossier = async (app) => {
    const job = jobs.find(j => j.jobId === app.jobId);
    const candidate = candidates.find(c => c.candidateId === app.candidateId);
    if (!job || !candidate) return;

    try {
      // Evaluate live match result
      const matchRes = await fetch('/api/match/evaluate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ jobId: job.jobId, candidateId: candidate.candidateId })
      }).then(r => r.json());

      // Fetch recruiter notes
      const notesRes = await fetch(`/api/applications/${app.applicationId}/notes`).then(r => r.json()).catch(() => []);

      setActiveDossier({
        application: app,
        job,
        candidate,
        matchResult: matchRes,
        notes: notesRes
      });
    } catch (e) {
      console.error('Error opening dossier:', e);
    }
  };

  const handleAddNote = async (appId, author, text, rating) => {
    try {
      await fetch(`/api/applications/${appId}/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ author, noteText: text, rating: String(rating) })
      });
      // Refresh notes in active dossier
      const notesRes = await fetch(`/api/applications/${appId}/notes`).then(r => r.json()).catch(() => []);
      if (activeDossier) {
        setActiveDossier({ ...activeDossier, notes: notesRes });
      }
    } catch (e) {
      console.error(e);
    }
  };

  const filteredApps = selectedJobId
    ? applications.filter(a => a.jobId === selectedJobId)
    : applications;

  const currentJob = jobs.find(j => j.jobId === selectedJobId) || jobs[0];

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--bg-app)' }}>
      
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
        {/* Workspace Brand */}
        <div style={{ padding: '20px 18px', borderBottom: '1px solid var(--border-main)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{
              width: '34px',
              height: '34px',
              borderRadius: '8px',
              background: '#2563eb',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '1rem',
              color: '#fff'
            }}>
              HS
            </div>
            <div>
              <div style={{ fontSize: '0.95rem', fontWeight: 800, color: '#fff', letterSpacing: '-0.01em' }}>
                HireSphere
              </div>
              <div style={{ fontSize: '0.7rem', color: '#94a3b8', fontWeight: 600 }}>
                Enterprise ATS Portal
              </div>
            </div>
          </div>
        </div>

        {/* Navigation */}
        <nav style={{ padding: '16px 10px', flex: 1, display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <NavBtn
            active={activeTab === 'pipeline'}
            onClick={() => setActiveTab('pipeline')}
            icon={<Kanban size={18} />}
            label="Hiring Pipeline"
            badge={filteredApps.length}
          />
          <NavBtn
            active={activeTab === 'leaderboard'}
            onClick={() => setActiveTab('leaderboard')}
            icon={<Award size={18} />}
            label="Ranked Leaderboard"
          />
          <NavBtn
            active={activeTab === 'parser'}
            onClick={() => setActiveTab('parser')}
            icon={<FileCheck2 size={18} />}
            label="Resume Parser Hub"
            highlight={true}
          />
          <NavBtn
            active={activeTab === 'jobs'}
            onClick={() => setActiveTab('jobs')}
            icon={<Briefcase size={18} />}
            label="Requisitions (Jobs)"
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
            label="Talent Insights"
          />
        </nav>

        {/* Database Info Widget */}
        <div style={{ padding: '16px', borderTop: '1px solid var(--border-main)' }}>
          <div style={{
            background: 'rgba(255, 255, 255, 0.03)',
            border: '1px solid var(--border-main)',
            borderRadius: '8px',
            padding: '10px 12px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '4px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: '#10b981' }} />
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#f8fafc' }}>
                  {systemHealth ? systemHealth.databaseType : 'SQLITE'} CONNECTED
                </span>
              </div>
              <Database size={13} color="#64748b" />
            </div>
            <div style={{ fontSize: '0.675rem', color: '#64748b', wordBreak: 'break-all' }}>
              {systemHealth ? systemHealth.database : 'hiresphere.db'}
            </div>
          </div>
        </div>
      </aside>

      {/* --- MAIN WORKSPACE --- */}
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        
        {/* Top Header */}
        <header style={{
          height: '64px',
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
          {/* Job Filter Selector */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: '0.8rem', color: '#94a3b8', fontWeight: 600 }}>Active Requisition:</span>
            <select
              value={selectedJobId || ''}
              onChange={(e) => setSelectedJobId(Number(e.target.value))}
              className="select"
              style={{ width: '280px', padding: '6px 10px', fontSize: '0.825rem' }}
            >
              {jobs.map(j => (
                <option key={j.jobId} value={j.jobId}>
                  #{j.jobId} - {j.title} ({j.department})
                </option>
              ))}
            </select>
          </div>

          {/* Quick Actions */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <button onClick={() => setShowJobModal(true)} className="btn btn-secondary btn-sm">
              <Plus size={14} /> New Requisition
            </button>
            <button onClick={() => setShowCandModal(true)} className="btn btn-secondary btn-sm">
              <Plus size={14} /> Add Candidate
            </button>
            <button onClick={() => setActiveTab('parser')} className="btn btn-primary btn-sm">
              <UploadCloud size={14} /> Ingest Resume
            </button>
            <button onClick={loadPortalData} className="btn-icon" title="Refresh Portal Data">
              <RefreshCw size={15} />
            </button>
          </div>
        </header>

        {/* View Switcher */}
        <div style={{ padding: '28px', flex: 1 }}>
          {activeTab === 'pipeline' && (
            <PipelineKanbanView
              job={currentJob}
              applications={filteredApps}
              onCardClick={openDossier}
              onUpdateStatus={handleUpdateStatus}
            />
          )}

          {activeTab === 'leaderboard' && (
            <RankedLeaderboardView
              job={currentJob}
              applications={filteredApps}
              onRowClick={openDossier}
              onUpdateStatus={handleUpdateStatus}
            />
          )}

          {activeTab === 'parser' && (
            <ResumeParserView
              jobs={jobs}
              selectedJobId={selectedJobId}
              onImportSuccess={() => {
                loadPortalData();
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
                  // Synthetic dossier view
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

      {/* --- CANDIDATE DOSSIER DRAWER (INSPECTOR) --- */}
      {activeDossier && (
        <CandidateDossierDrawer
          dossier={activeDossier}
          onClose={() => setActiveDossier(null)}
          onUpdateStatus={handleUpdateStatus}
          onAddNote={handleAddNote}
        />
      )}

      {/* --- MODALS --- */}
      {showJobModal && (
        <CreateJobModal
          onClose={() => setShowJobModal(false)}
          onSuccess={() => {
            setShowJobModal(false);
            loadPortalData();
          }}
        />
      )}

      {showCandModal && (
        <AddCandidateModal
          onClose={() => setShowCandModal(false)}
          onSuccess={() => {
            setShowCandModal(false);
            loadPortalData();
          }}
        />
      )}

    </div>
  );
}

// -------------------------------------------------------------
// VIEW 1: KANBAN HIRING PIPELINE BOARD (ASHBY/LEVER STYLE)
// -------------------------------------------------------------

function PipelineKanbanView({ job, applications, onCardClick, onUpdateStatus }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Job Context Strip */}
      <div className="card" style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#fff' }}>{job ? job.title : 'All Requisitions'}</h2>
              <span className="badge badge-neutral">{job ? job.department : 'General'}</span>
            </div>
            <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginTop: '4px' }}>
              Requirements: {job ? `${job.minExpYears} yrs min exp • ${job.minEducation}` : ''} • {applications.length} Active Applicants
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {job && job.requiredSkills && job.requiredSkills.slice(0, 4).map((sk, i) => (
            <span key={i} style={{
              fontSize: '0.725rem',
              padding: '3px 8px',
              borderRadius: '4px',
              background: sk.weightType === 'MANDATORY' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(59, 130, 246, 0.15)',
              color: sk.weightType === 'MANDATORY' ? '#f87171' : '#60a5fa',
              border: `1px solid ${sk.weightType === 'MANDATORY' ? 'rgba(239, 68, 68, 0.3)' : 'rgba(59, 130, 246, 0.3)'}`,
              fontWeight: 600
            }}>
              {sk.skillName} ({sk.weightType === 'MANDATORY' ? 'Mandatory' : 'Preferred'})
            </span>
          ))}
        </div>
      </div>

      {/* Kanban Board Horizontal Columns */}
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
                  <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#f8fafc' }}>
                    {stage.label}
                  </span>
                </div>
                <span className="badge badge-neutral" style={{ fontSize: '0.7rem' }}>
                  {stageApps.length}
                </span>
              </div>

              <div className="kanban-cards-container">
                {stageApps.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '28px 12px', color: '#64748b', fontSize: '0.775rem' }}>
                    No candidates in this stage
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
                          <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#fff' }}>
                            {app.candidateName}
                          </div>
                          <span className={`badge ${tierClass}`}>
                            {score.toFixed(0)}%
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

                        {/* Quick stage selector */}
                        <div
                          style={{ display: 'flex', justifyContent: 'flex-end', gap: '4px' }}
                          onClick={(e) => e.stopPropagation()}
                        >
                          <select
                            value={app.status || 'APPLIED'}
                            onChange={(e) => onUpdateStatus(app.applicationId, e.target.value)}
                            className="select"
                            style={{ padding: '3px 6px', fontSize: '0.725rem', width: 'auto' }}
                          >
                            {PIPELINE_STAGES.map(s => (
                              <option key={s.id} value={s.id}>Move: {s.label}</option>
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
  // Sort dynamically by match score DESC
  const sorted = [...applications].sort((a, b) => b.matchScore - a.matchScore);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      <div className="card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff' }}>Applicant Ranking Leaderboard</h2>
          <p style={{ fontSize: '0.8rem', color: '#94a3b8', marginTop: '2px' }}>
            Sorted dynamically by <code>match_score DESC</code> with weighted multi-factor scoring
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
              <th>Composite Score</th>
              <th>Coverage (55%)</th>
              <th>Exp Delta (25%)</th>
              <th>Proficiency (20%)</th>
              <th>Pipeline Stage</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {sorted.length === 0 ? (
              <tr>
                <td colSpan="8" style={{ textAlign: 'center', padding: '40px', color: '#64748b' }}>
                  No applications recorded for this requisition.
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
                      <div style={{ fontWeight: 700, color: '#fff' }}>{app.candidateName}</div>
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
// VIEW 3: RESUME PARSER & INGESTION HUB (REAL WORKING PARSER)
// -------------------------------------------------------------

function ResumeParserView({ jobs, selectedJobId, onImportSuccess }) {
  const [resumeText, setResumeText] = useState(SAMPLE_RESUMES.javaBackend);
  const [targetJobId, setTargetJobId] = useState(selectedJobId || (jobs[0] ? jobs[0].jobId : 1));
  const [parsing, setParsing] = useState(false);
  const [parsedResult, setParsedResult] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importStatus, setImportStatus] = useState(null);

  const handleParseOnly = async () => {
    setParsing(true);
    setImportStatus(null);
    try {
      const res = await fetch('/api/resumes/parse', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ resumeText })
      });
      const data = await res.json();
      setParsedResult(data);
    } catch (e) {
      console.error(e);
    } finally {
      setParsing(false);
    }
  };

  const handleImportToPipeline = async () => {
    setImporting(true);
    try {
      const res = await fetch('/api/resumes/import', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ resumeText, jobId: String(targetJobId) })
      });
      const data = await res.json();
      setImportStatus(data);
      setTimeout(() => {
        onImportSuccess();
      }, 1500);
    } catch (e) {
      console.error(e);
    } finally {
      setImporting(false);
    }
  };

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '24px' }}>
      
      {/* Left: Input Textarea */}
      <div className="card" style={{ padding: '24px', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <div>
            <h2 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#fff' }}>Resume Ingestion & Heuristic Parser</h2>
            <p style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
              Paste raw candidate resume text to extract skills and profile fields in real time.
            </p>
          </div>
          
          {/* Quick Sample Selector */}
          <div style={{ display: 'flex', gap: '6px' }}>
            <button
              onClick={() => { setResumeText(SAMPLE_RESUMES.javaBackend); setParsedResult(null); }}
              className="btn btn-secondary btn-sm"
            >
              Sample 1 (Java)
            </button>
            <button
              onClick={() => { setResumeText(SAMPLE_RESUMES.cloudDevops); setParsedResult(null); }}
              className="btn btn-secondary btn-sm"
            >
              Sample 2 (DevOps)
            </button>
            <button
              onClick={() => { setResumeText(SAMPLE_RESUMES.frontendReact); setParsedResult(null); }}
              className="btn btn-secondary btn-sm"
            >
              Sample 3 (Frontend)
            </button>
          </div>
        </div>

        <textarea
          rows="18"
          value={resumeText}
          onChange={(e) => setResumeText(e.target.value)}
          placeholder="Paste candidate resume text here..."
          className="textarea"
          style={{ fontFamily: 'monospace', fontSize: '0.8rem', lineHeight: 1.5, flex: 1, resize: 'vertical' }}
        />

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
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

          <div style={{ display: 'flex', gap: '10px' }}>
            <button onClick={handleParseOnly} disabled={parsing} className="btn btn-secondary">
              <FileCheck2 size={16} /> {parsing ? 'Parsing...' : 'Preview Extracted Fields'}
            </button>
            <button onClick={handleImportToPipeline} disabled={importing} className="btn btn-primary">
              <UploadCloud size={16} /> {importing ? 'Saving to Database...' : 'Ingest & Rank Candidate'}
            </button>
          </div>
        </div>

        {importStatus && (
          <div style={{ marginTop: '14px', padding: '12px', borderRadius: '8px', background: 'rgba(16, 185, 129, 0.15)', border: '1px solid #10b981', color: '#34d399', fontSize: '0.85rem' }}>
            ✔ Successfully ingested #{importStatus.candidateId} into pipeline! Match score calculated: <strong>{importStatus.matchScore}% ({importStatus.matchTier})</strong>. Redirecting...
          </div>
        )}
      </div>

      {/* Right: Real Extracted Entities Preview */}
      <div className="card" style={{ padding: '24px', display: 'flex', flexDirection: 'column' }}>
        <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#fff', marginBottom: '14px' }}>
          Extracted Profile & Skill Tokens
        </h3>

        {parsedResult ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ padding: '14px', background: '#0b1120', borderRadius: '8px', border: '1px solid var(--border-main)' }}>
              <div style={{ fontSize: '1.1rem', fontWeight: 800, color: '#fff' }}>{parsedResult.fullName}</div>
              <div style={{ fontSize: '0.8rem', color: '#38bdf8', marginTop: '2px' }}>{parsedResult.email}</div>
              <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginTop: '2px' }}>{parsedResult.phone}</div>
              <div style={{ fontSize: '0.8rem', color: '#cbd5e1', marginTop: '8px' }}>
                Experience: <strong>{parsedResult.totalExpYears} years</strong> • Degree: <strong>{parsedResult.highestEducation}</strong>
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', marginBottom: '8px' }}>
                Detected Skills Catalog Overlap ({parsedResult.skills ? parsedResult.skills.length : 0}):
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                {parsedResult.skills && parsedResult.skills.map((sk, i) => (
                  <span key={i} style={{
                    padding: '4px 10px',
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

            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', marginBottom: '6px' }}>
                Bio Summary:
              </div>
              <div style={{ fontSize: '0.8rem', color: '#94a3b8', lineHeight: 1.5, background: '#0b1120', padding: '10px', borderRadius: '6px' }}>
                {parsedResult.resumeSummary}
              </div>
            </div>
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '60px 20px', color: '#64748b', fontSize: '0.85rem' }}>
            Click <strong>"Preview Extracted Fields"</strong> or <strong>"Ingest & Rank Candidate"</strong> to inspect how the parser breaks down raw text into structured JDBC records.
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
          <p style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Open corporate positions with weighted skill requisites</p>
        </div>
        <button onClick={onOpenCreate} className="btn btn-primary">
          <Plus size={16} /> Create Requisition
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
                  <div style={{ fontWeight: 700, color: '#fff' }}>{cand.fullName}</div>
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
                  <button className="btn btn-ghost btn-sm">Inspect Dossier →</button>
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
// VIEW 6: TALENT ANALYTICS DASHBOARD
// -------------------------------------------------------------

function AnalyticsDashboardView({ funnel, skillDemand, applications }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      
      {/* KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: '16px' }}>
        <MetricCard title="Total Requisitions" value={funnel ? `${funnel.openJobs} Active` : '0'} sub={`${funnel ? funnel.totalJobs : 0} Total created`} />
        <MetricCard title="Talent Pool" value={funnel ? funnel.totalCandidates : 0} sub="Candidates indexed" />
        <MetricCard title="Applications Screened" value={funnel ? funnel.totalApplications : 0} sub="Evaluated by Engine" />
        <MetricCard title="Shortlisted Pipeline" value={funnel ? funnel.shortlistedCount : 0} sub="Passed threshold" color="#34d399" />
        <MetricCard title="Avg Match Score" value={funnel ? `${funnel.avgMatchScore}%` : '0%'} sub="Global ATS average" color="#fbbf24" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '20px' }}>
        {/* Skill Demand Matrix */}
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

        {/* Funnel Conversion */}
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
// COMPONENT: SLIDE-OVER CANDIDATE DOSSIER DRAWER (INSPECTOR)
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
              {/* Score Radar Bars */}
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

              {/* Matched Skills */}
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

              {/* Skill Gaps */}
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
              {/* Add Note Form */}
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

              {/* Note History */}
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

        {/* Drawer Stage Advance Footer */}
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
    onSuccess();
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
    onSuccess();
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
        background: active ? 'rgba(59, 130, 246, 0.12)' : 'transparent',
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
