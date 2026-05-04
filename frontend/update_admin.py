import re

with open('src/App.jsx', 'r') as f:
    app_jsx = f.read()

# Update adminNav
new_admin_nav = """const adminNav = [
  { key: "dashboard", label: "Overview", note: "" },
  { key: "approvals", label: "KYC Queue", note: "" },
  { key: "campaigns", label: "Campaigns", note: "" },
  { key: "catalog", label: "Reward Catalog", note: "" },
  { key: "notifications", label: "Notifications", note: "" },
];"""

app_jsx = re.sub(
    r'const adminNav = \[[\s\S]*?\];',
    new_admin_nav,
    app_jsx
)

# Update top-banner in main return to be conditional
new_top_banner_content = """        <div className="top-banner-content">
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
        </div>"""

app_jsx = re.sub(
    r'<div className="top-banner-content">[\s\S]*?(?=<div className="top-banner-actions">)',
    new_top_banner_content + '\n        ',
    app_jsx
)

# Update Sign out block
new_actions = """        <div className="top-banner-actions">
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
        </div>"""

app_jsx = re.sub(
    r'<div className="top-banner-actions">[\s\S]*?(?=</header>)',
    new_actions + '\n      ',
    app_jsx
)


# Update renderAdminPage
new_render_admin = """  function renderAdminPage() {
    switch (activePage) {
      case "dashboard":
      default:
        return (
          <>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px', marginBottom: '16px' }}>
              <div className="section-card">
                <p className="mini-label">PLATFORM</p>
                <p className="section-label" style={{ background: 'none', padding: 0, marginTop: '8px' }}>System Status</p>
                <h2 style={{ fontSize: '2rem', marginTop: '12px' }}>Operational</h2>
              </div>
              <div className="section-card">
                <p className="mini-label">CAMPAIGNS</p>
                <p className="section-label" style={{ background: 'none', padding: 0, marginTop: '8px' }}>Active Campaigns</p>
                <h2 style={{ fontSize: '2rem', marginTop: '12px' }}>{campaigns.length}</h2>
              </div>
              <div className="section-card">
                <p className="mini-label">KYC</p>
                <p className="section-label" style={{ background: 'none', padding: 0, marginTop: '8px' }}>Pending Reviews</p>
                <h2 style={{ fontSize: '2rem', marginTop: '12px' }}>{approvals.length}</h2>
              </div>
              <div className="section-card">
                <p className="mini-label">CATALOG</p>
                <p className="section-label" style={{ background: 'none', padding: 0, marginTop: '8px' }}>Reward Items</p>
                <h2 style={{ fontSize: '2rem', marginTop: '12px' }}>{rewardsCatalog.length}</h2>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="section-card">
                <h3>Command view</h3>
                <p className="section-copy" style={{ marginBottom: '24px' }}>Immediate operating signals from the backend.</p>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div style={{ padding: '16px', background: 'rgba(19, 34, 56, 0.2)', borderRadius: '16px' }}>
                    <p className="mini-label" style={{ background: 'none', padding: 0 }}>Pending KYC queue</p>
                    <h3 style={{ color: 'var(--accent)', margin: '8px 0' }}>{approvals.length}</h3>
                    <p className="section-copy" style={{ fontSize: '0.85rem' }}>Review approvals and rejections from the KYC queue tab.</p>
                  </div>
                  <div style={{ padding: '16px', background: 'rgba(19, 34, 56, 0.2)', borderRadius: '16px' }}>
                    <p className="mini-label" style={{ background: 'none', padding: 0 }}>Notification records</p>
                    <h3 style={{ color: '#fb923c', margin: '8px 0' }}>22</h3>
                    <p className="section-copy" style={{ fontSize: '0.85rem' }}>Recent delivery history emitted across KYC and loyalty flows.</p>
                  </div>
                </div>
              </div>

              <div className="section-card">
                <h3>Roadmap alignment</h3>
                <p className="section-copy" style={{ marginBottom: '24px' }}>Blueprint features ready to extend later.</p>
                <div className="list-stack">
                  <div style={{ padding: '16px', borderBottom: '1px solid var(--line)' }}>
                    <span className="section-copy" style={{ fontSize: '0.85rem' }}>Risk flags and fraud monitoring can attach to this admin shell without redesign.</span>
                  </div>
                  <div style={{ padding: '16px', borderBottom: '1px solid var(--line)' }}>
                    <span className="section-copy" style={{ fontSize: '0.85rem' }}>Reporting widgets can be added once analytics endpoints are available.</span>
                  </div>
                  <div style={{ padding: '16px' }}>
                    <span className="section-copy" style={{ fontSize: '0.85rem' }}>User management actions are prepared to grow around your current service contracts.</span>
                  </div>
                </div>
              </div>
            </div>
          </>
        );

      case "approvals":
        return (
          <div className="section-card">
            <h3>Pending KYC approvals</h3>
            <p className="section-copy" style={{ marginBottom: '24px' }}>Review and action requests flowing in from the user service.</p>
            {approvals.length === 0 ? (
              <div style={{ padding: '48px', textAlign: 'center', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px' }}>
                <h4 style={{ marginBottom: '8px' }}>No KYC items waiting</h4>
                <p className="section-copy" style={{ fontSize: '0.85rem' }}>The pending queue is clear right now.</p>
              </div>
            ) : (
              <div className="list-stack">
                {approvals.map((item) => (
                  <div key={item.id} className="review-card" style={{ padding: '24px', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <strong>{item.name}</strong>
                      <div className="section-copy" style={{ fontSize: '0.85rem', marginTop: '8px' }}>{item.document} · Risk: {item.risk}</div>
                    </div>
                    <div style={{ display: 'flex', gap: '12px' }}>
                      <button className="app-button primary" style={{ borderRadius: '999px', padding: '8px 24px' }} onClick={() => handleApproval(item.id, "Approved")}>Approve</button>
                      <button className="app-button secondary" style={{ borderRadius: '999px', padding: '8px 24px' }} onClick={() => handleApproval(item.id, "Rejected")}>Reject</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        );

      case "campaigns":
        return (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '16px' }}>
            <div className="section-card">
              <h3>Create campaign</h3>
              <p className="section-copy" style={{ marginBottom: '24px' }}>Publish new admin-side campaigns through the current endpoint.</p>
              <form className="field-grid" onSubmit={handleCampaign}>
                <Field
                  label="CAMPAIGN NAME"
                  value={campaignForm.name}
                  onChange={(event) => setCampaignForm((current) => ({ ...current, name: event.target.value }))}
                />
                <Field
                  label="TARGET TIER"
                  as="select"
                  value={campaignForm.audience}
                  options={["All", "Silver", "Gold", "Platinum"]}
                  onChange={(event) => setCampaignForm((current) => ({ ...current, audience: event.target.value }))}
                />
                <Field
                  label="STATUS"
                  as="select"
                  value="Active"
                  options={["Active", "Draft"]}
                  onChange={() => {}}
                />
                <div style={{ marginTop: '16px' }}>
                  <button type="submit" className="app-button primary" style={{ borderRadius: '999px', padding: '12px 24px' }}>
                    Create Campaign
                  </button>
                </div>
              </form>
            </div>

            <div className="section-card">
              <h3>Campaign register</h3>
              <p className="section-copy" style={{ marginBottom: '24px' }}>Current campaign records from the admin service.</p>
              {campaigns.length === 0 ? (
                <div style={{ padding: '48px', textAlign: 'center', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px' }}>
                  <h4 style={{ marginBottom: '8px' }}>No campaigns yet</h4>
                  <p className="section-copy" style={{ fontSize: '0.85rem' }}>Create your first reward campaign from the form on the left.</p>
                </div>
              ) : (
                <div className="list-stack">
                  {campaigns.map((item) => (
                    <div key={item.id} style={{ padding: '24px', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <strong>{item.name}</strong>
                        <div className="section-copy" style={{ fontSize: '0.85rem', marginTop: '8px' }}>Target: {item.audience}</div>
                      </div>
                      <span className="pill success" style={{ background: 'rgba(79, 209, 197, 0.1)', color: '#4fd1c5', border: '1px solid rgba(79, 209, 197, 0.3)' }}>Active</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        );

      case "catalog":
        return (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '16px' }}>
            <div className="section-card">
              <h3>Create reward item</h3>
              <p className="section-copy" style={{ marginBottom: '24px' }}>Publish a catalog item for user redemption.</p>
              <form className="field-grid" onSubmit={(e) => e.preventDefault()}>
                <Field label="NAME" />
                <Field label="DESCRIPTION" as="textarea" />
                <div className="field-row two-up">
                  <Field label="COST IN POINTS" />
                  <Field label="STOCK QUANTITY" />
                </div>
                <Field label="REQUIRED TIER" as="select" options={["All", "Silver", "Gold", "Platinum"]} />
                <div style={{ marginTop: '16px' }}>
                  <button type="submit" className="app-button primary" style={{ borderRadius: '999px', padding: '12px 24px' }}>
                    Create Catalog Item
                  </button>
                </div>
              </form>
            </div>

            <div className="section-card">
              <h3>Reward catalog</h3>
              <p className="section-copy" style={{ marginBottom: '24px' }}>Inventory users can browse and redeem from their dashboard.</p>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                {rewardsCatalog.map((item) => (
                  <div key={item.id} style={{ padding: '24px', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                      <strong style={{ fontSize: '1.05rem' }}>{item.name}</strong>
                    </div>
                    <p className="section-copy" style={{ fontSize: '0.85rem', marginBottom: '16px' }}>Example description placeholder.</p>
                    <p className="section-copy" style={{ fontSize: '0.85rem' }}>{item.cost} pts <span style={{ marginLeft: '12px' }}>Stock {item.stock}</span> <span style={{ marginLeft: '12px' }}>{item.tier}</span></p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        );

      case "notifications":
        return (
          <div className="section-card">
            <h3>Notification history</h3>
            <p className="section-copy" style={{ marginBottom: '24px' }}>Simple admin-wide view of sent wallet, rewards, and KYC notifications.</p>
            <div className="list-stack">
              <div style={{ padding: '24px', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px', display: 'flex', justifyContent: 'space-between' }}>
                <div>
                  <strong style={{ fontSize: '1.05rem' }}>kyc.status.updated</strong>
                  <p className="section-copy" style={{ fontSize: '0.85rem', marginTop: '8px', marginBottom: '8px' }}>Your KYC status has been updated to: APPROVED. You can now perform full transactions.</p>
                  <p className="section-copy" style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>User: 1a6f7231-067a-4314-ab57-3e4c3407fb16</p>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <p className="section-copy" style={{ fontSize: '0.85rem', marginBottom: '4px' }}>Email</p>
                  <p className="section-copy" style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>28 Mar 2026, 9:55 am</p>
                </div>
              </div>
              <div style={{ padding: '24px', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px', display: 'flex', justifyContent: 'space-between' }}>
                <div>
                  <strong style={{ fontSize: '1.05rem' }}>wallet.topup.success</strong>
                  <p className="section-copy" style={{ fontSize: '0.85rem', marginTop: '8px', marginBottom: '8px' }}>Your wallet has been topped up successfully with amount 1000</p>
                  <p className="section-copy" style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>User: 1a6f7231-067a-4314-ab57-3e4c3407fb16</p>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <p className="section-copy" style={{ fontSize: '0.85rem', marginBottom: '4px' }}>Email</p>
                  <p className="section-copy" style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>28 Mar 2026, 9:56 am</p>
                </div>
              </div>
            </div>
          </div>
        );

      default:
        return (
          <div className="section-card">
            <h3>{activePage}</h3>
            <p className="section-copy">This page layout is not fully configured for the admin design yet.</p>
          </div>
        );
    }
  }"""

app_jsx = re.sub(
    r'function renderAdminPage\(\)\s*\{[\s\S]*?(?=\n\s*const reportRevenue)',
    new_render_admin + '\n\n',
    app_jsx
)

with open('src/App.jsx', 'w') as f:
    f.write(app_jsx)
