import re

with open('src/App.jsx', 'r') as f:
    app_jsx = f.read()

new_main_return = """  return (
    <div className="page-shell">
      <header className="top-banner">
        <div className="top-banner-content">
          {workspaceRole === "admin" ? (
            <>
              <span className="brand-chip">ADMIN OPERATIONS</span>
              <h1>Control center for KYC, campaigns, and loyalty operations</h1>
              <p>This admin surface is tuned to the services you already have today, while leaving room for future analytics, disputes, and governance workflows.</p>
              <div style={{ display: "flex", gap: "12px", marginTop: "12px" }}>
                <span className="pill" style={{ background: "rgba(79, 209, 197, 0.1)", color: "#4fd1c5", border: "1px solid rgba(79, 209, 197, 0.3)" }}>APPROVAL AND OPS RAIL</span>
                <span className="pill" style={{ background: "rgba(79, 209, 197, 0.1)", color: "#4fd1c5", border: "1px solid rgba(79, 209, 197, 0.3)" }}>ADMIN WORKFLOW ACTIVE</span>
              </div>
            </>
          ) : (
            <>
              <span className="brand-chip">CUSTOMER WORKSPACE</span>
              <h1>Welcome back, {profile.name.toLowerCase()}</h1>
              <p>Manage your wallet balance, loyalty earnings, transaction trail, and KYC journey from one connected console.</p>
              <div style={{ display: "flex", gap: "12px", marginTop: "12px" }}>
                <span className="pill" style={{ background: "rgba(79, 209, 197, 0.1)", color: "#4fd1c5", border: "1px solid rgba(79, 209, 197, 0.3)" }}>WALLET + LOYALTY UNIFIED</span>
                <span className="pill" style={{ background: "rgba(79, 209, 197, 0.1)", color: "#4fd1c5", border: "1px solid rgba(79, 209, 197, 0.3)" }}>GATEWAY CONNECTED</span>
              </div>
            </>
          )}
        </div>
        <div className="top-banner-actions">
          <div className="theme-switcher-group">
            <button className={`theme-btn ${theme === 'aqua' ? 'active' : ''}`} onClick={() => setTheme('aqua')}>
              <div className="theme-dot aqua"></div> Aqua
            </button>
            <button className={`theme-btn ${theme === 'sunset' ? 'active' : ''}`} onClick={() => setTheme('sunset')}>
              <div className="theme-dot sunset"></div> Sunset
            </button>
            <button className={`theme-btn ${theme === 'violet' ? 'active' : ''}`} onClick={() => setTheme('violet')}>
              <div className="theme-dot violet"></div> Violet
            </button>
          </div>
          <div style={{ display: "flex", gap: "12px", alignSelf: "flex-end", alignItems: "center" }}>
            {workspaceRole === "admin" && (
              <span className="section-copy" style={{ fontSize: "0.85rem", border: "1px solid rgba(255,255,255,0.1)", padding: "8px 16px", borderRadius: "999px" }}>
                Signed in as {profile.name.toLowerCase()} (Admin)
              </span>
            )}
            <button className="theme-btn" onClick={() => window.location.reload()}>Refresh Data</button>
            <button className="theme-btn" onClick={exitWorkspace}>Sign Out</button>
          </div>
        </div>
      </header>

      <nav className="nav-pill-list">
        {navItems.map((item) => {
          const active = activePage === item.key;
          return (
            <button
              key={item.key}
              type="button"
              className={`nav-pill ${active ? 'active' : ''}`}
              onClick={() => startTransition(() => setActivePage(item.key))}
            >
              {item.label}
            </button>
          );
        })}
      </nav>

      <main className="content-shell" style={{ marginTop: '8px' }}>
        {workspaceRole === "admin" ? renderAdminPage() : renderApplicantPage()}
      </main>
    </div>
  );
}

export default App;"""

app_jsx = re.sub(
    r'return\s*\(\s*<div className="page-shell">[\s\S]*',
    new_main_return,
    app_jsx
)

with open('src/App.jsx', 'w') as f:
    f.write(app_jsx)
