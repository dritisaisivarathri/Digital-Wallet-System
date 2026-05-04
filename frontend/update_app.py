import re

with open('src/App.jsx', 'r') as f:
    app_jsx = f.read()

# Replace the authentication section
new_auth_section = """  if (workspaceRole === "auth") {
    return (
      <div className="auth-screen">
        <section className="auth-hero-panel">
          <span className="brand-chip">DIGITAL WALLET SYSTEM</span>
          <h1>Wallet operations, loyalty control, and KYC flow in one cinematic workspace.</h1>
          <p>
            This frontend is tailored to your existing Spring Boot microservices and API gateway.
            Users get wallet controls, rewards, transactions, and profile workflows, while admins
            get approvals, campaigns, and oversight in one place.
          </p>

          <div className="auth-feature-list" style={{ display: "flex", gap: "12px", marginTop: "24px" }}>
            <div className="auth-feature" style={{ flex: 1 }}>
              <strong>Wallet Ledger</strong>
              <span>Balance, top-up, transfer</span>
            </div>
            <div className="auth-feature" style={{ flex: 1 }}>
              <strong>Loyalty Engine</strong>
              <span>Points, catalog, redemption</span>
            </div>
            <div className="auth-feature" style={{ flex: 1 }}>
              <strong>Ops Console</strong>
              <span>KYC queue and campaigns</span>
            </div>
          </div>
        </section>

        <section className="auth-card" style={{ justifyContent: 'flex-start' }}>
          {authView !== "forgot" && (
            <div className="auth-card-tabs" style={{ display: 'flex', background: 'rgba(17,24,39,0.5)', borderRadius: '999px', padding: '4px', gap: '4px', marginBottom: '32px' }}>
              <button
                type="button"
                style={{ flex: 1, padding: '12px', borderRadius: '999px', background: authView === 'login' ? 'var(--accent)' : 'transparent', color: authView === 'login' ? '#000' : 'var(--muted)', fontWeight: 600, border: 'none' }}
                onClick={() => setAuthView("login")}
              >
                Sign In
              </button>
              <button
                type="button"
                style={{ flex: 1, padding: '12px', borderRadius: '999px', background: authView === 'signup' ? 'var(--accent)' : 'transparent', color: authView === 'signup' ? '#000' : 'var(--muted)', fontWeight: 600, border: 'none' }}
                onClick={() => setAuthView("signup")}
              >
                Create Account
              </button>
            </div>
          )}

          {authView === "login" && (
            <>
              <div className="auth-card-head" style={{ marginBottom: '24px' }}>
                <h2>Access your workspace</h2>
                <p>Sign in with the credentials provisioned by your auth service.</p>
              </div>
              <form className="field-grid" onSubmit={handleLogin}>
                <Field
                  label="USERNAME"
                  value={authForm.identifier}
                  onChange={(event) => updateAuth("identifier", event.target.value)}
                />
                <Field
                  label="PASSWORD"
                  type="password"
                  value={authForm.password}
                  onChange={(event) => updateAuth("password", event.target.value)}
                />
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '4px', marginBottom: '16px' }}>
                  <button type="button" style={{ background: 'none', border: 'none', color: 'var(--muted)', fontSize: '0.85rem' }} onClick={() => setAuthView("forgot")}>
                    Forgot password?
                  </button>
                </div>
                <button type="submit" className="app-button primary" style={{ width: '100%', borderRadius: '999px', padding: '16px' }}>
                  Sign In
                </button>
              </form>
            </>
          )}

          {authView === "signup" && (
            <>
              <div className="auth-card-head" style={{ marginBottom: '24px' }}>
                <h2>Open a new wallet account</h2>
                <p>This registration uses the current backend signup endpoint and defaults new accounts to the USER role.</p>
              </div>
              <form className="field-grid" onSubmit={handleSignup}>
                <div className="field-row two-up">
                  <Field
                    label="FULL NAME"
                    value={signupForm.fullName}
                    onChange={(event) => setSignupForm((current) => ({ ...current, fullName: event.target.value }))}
                  />
                  <Field
                    label="USERNAME"
                    value={signupForm.email}
                    onChange={(event) => setSignupForm((current) => ({ ...current, email: event.target.value }))}
                  />
                </div>
                <div className="field-row two-up">
                  <Field
                    label="EMAIL"
                    value={signupForm.email}
                    onChange={(event) => setSignupForm((current) => ({ ...current, email: event.target.value }))}
                  />
                  <Field
                    label="PHONE NUMBER"
                    value={signupForm.phone}
                    onChange={(event) => setSignupForm((current) => ({ ...current, phone: event.target.value }))}
                  />
                </div>
                <Field
                  label="PASSWORD"
                  type="password"
                  value={signupForm.password}
                  onChange={(event) => setSignupForm((current) => ({ ...current, password: event.target.value }))}
                />
                <div style={{ marginTop: '16px' }}>
                  <button type="submit" className="app-button primary" style={{ width: '100%', borderRadius: '999px', padding: '16px' }}>
                    Create Account
                  </button>
                </div>
              </form>
            </>
          )}

          {authView === "forgot" && (
            <>
              <div className="auth-card-head" style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <h2>Reset your password</h2>
                  <p>Enter your email, verify the OTP sent to your inbox, and then choose a new password.</p>
                </div>
                <button type="button" className="app-button secondary" style={{ borderRadius: '999px' }} onClick={() => setAuthView("login")}>
                  Back
                </button>
              </div>
              <form className="field-grid" onSubmit={handleForgot}>
                <Field
                  label="EMAIL"
                  value={forgotForm.identifier}
                  onChange={(event) => setForgotForm({ identifier: event.target.value })}
                />
                <div style={{ marginTop: '16px' }}>
                  <button type="submit" className="app-button primary" style={{ width: '100%', borderRadius: '999px', padding: '16px' }}>
                    Send OTP
                  </button>
                </div>
              </form>
            </>
          )}
        </section>
      </div>
    );
  }"""

# regex replace from `if (workspaceRole === "auth") {` to just before `return (` of the main shell
app_jsx = re.sub(
    r'if\s*\(workspaceRole === "auth"\)\s*\{[\s\S]*?(?=\s*return\s*\(\s*<div className="page-shell">)',
    new_auth_section + '\n\n',
    app_jsx
)

# Replace the main return block
main_return = """  return (
    <div className="page-shell">
      <header className="top-banner">
        <div className="top-banner-content">
          <span className="brand-chip">CUSTOMER WORKSPACE</span>
          <h1>Welcome back, {profile.name.toLowerCase()}</h1>
          <p>Manage your wallet balance, loyalty earnings, transaction trail, and KYC journey from one connected console.</p>
          <div style={{ display: "flex", gap: "12px", marginTop: "12px" }}>
            <span className="pill" style={{ background: "rgba(79, 209, 197, 0.1)", color: "#4fd1c5", border: "1px solid rgba(79, 209, 197, 0.3)" }}>WALLET + LOYALTY UNIFIED</span>
            <span className="pill" style={{ background: "rgba(79, 209, 197, 0.1)", color: "#4fd1c5", border: "1px solid rgba(79, 209, 197, 0.3)" }}>GATEWAY CONNECTED</span>
          </div>
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
            <button className="theme-btn" onClick={() => window.location.reload()}>Refresh Data</button>
          </div>
          <button className="theme-btn" onClick={exitWorkspace} style={{ alignSelf: 'flex-start' }}>Sign Out</button>
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
}"""

app_jsx = re.sub(
    r'return\s*\(\s*<div className="page-shell">[\s\S]*?(?=\}\s*$)',
    main_return + '\n',
    app_jsx
)

# Update the applicantNav array
applicant_nav = """const applicantNav = [
  { key: "dashboard", label: "Overview", note: "" },
  { key: "add-money", label: "Wallet", note: "" },
  { key: "transactions", label: "Transactions", note: "" },
  { key: "rewards", label: "Rewards", note: "" },
  { key: "profile-kyc", label: "KYC", note: "" },
  { key: "profile", label: "Profile", note: "" },
  { key: "support", label: "Notifications", note: "" },
];"""

app_jsx = re.sub(
    r'const applicantNav = \[[\s\S]*?\];',
    applicant_nav,
    app_jsx
)

with open('src/App.jsx', 'w') as f:
    f.write(app_jsx)
