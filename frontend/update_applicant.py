import re

with open('src/App.jsx', 'r') as f:
    app_jsx = f.read()

new_render_applicant = """  function renderApplicantPage() {
    switch (activePage) {
      case "dashboard":
      default:
        return (
          <>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px', marginBottom: '16px' }}>
              <div className="section-card">
                <p className="mini-label">WALLET</p>
                <p className="section-label" style={{ background: 'none', padding: 0, marginTop: '8px' }}>Available Balance</p>
                <h2 style={{ fontSize: '2rem', marginTop: '12px' }}>{formatMoney(walletBalance)}</h2>
              </div>
              <div className="section-card">
                <p className="mini-label">LEDGER</p>
                <p className="section-label" style={{ background: 'none', padding: 0, marginTop: '8px' }}>Ledger Balance</p>
                <h2 style={{ fontSize: '2rem', marginTop: '12px' }}>{formatMoney(ledgerBalance)}</h2>
              </div>
              <div className="section-card">
                <p className="mini-label">LOYALTY</p>
                <p className="section-label" style={{ background: 'none', padding: 0, marginTop: '8px' }}>Reward Points</p>
                <h2 style={{ fontSize: '2rem', marginTop: '12px' }}>{rewardState.points.toLocaleString("en-IN")}</h2>
              </div>
              <div className="section-card">
                <p className="mini-label">KYC</p>
                <p className="section-label" style={{ background: 'none', padding: 0, marginTop: '8px' }}>Verification Status</p>
                <h2 style={{ fontSize: '2rem', marginTop: '12px' }}>Approved</h2>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '16px' }}>
              <div className="section-card">
                <h3>Financial pulse</h3>
                <p className="section-copy" style={{ marginBottom: '24px' }}>A quick look at wallet movement, loyalty posture, and what to do next.</p>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div style={{ padding: '16px', background: 'rgba(19, 34, 56, 0.2)', borderRadius: '16px' }}>
                    <p className="mini-label" style={{ background: 'none', padding: 0 }}>Tier & rewards readiness</p>
                    <h3 style={{ color: 'var(--accent)', margin: '8px 0' }}>{rewardState.tier}</h3>
                    <p className="section-copy" style={{ fontSize: '0.85rem' }}>Keep transacting and redeeming from the active catalog to move up loyalty tiers.</p>
                  </div>
                  <div style={{ padding: '16px', background: 'rgba(19, 34, 56, 0.2)', borderRadius: '16px' }}>
                    <p className="mini-label" style={{ background: 'none', padding: 0 }}>Recent transaction count</p>
                    <h3 style={{ color: '#fb923c', margin: '8px 0' }}>{transactions.length}</h3>
                    <p className="section-copy" style={{ fontSize: '0.85rem' }}>Your latest wallet history is already synced from the transaction service.</p>
                  </div>
                </div>
              </div>

              <div className="section-card">
                <h3>Roadmap notes</h3>
                <p className="section-copy" style={{ marginBottom: '24px' }}>Frontend placeholders for blueprint features still waiting on backend APIs.</p>
                <div className="list-stack">
                  <div style={{ padding: '16px', borderBottom: '1px solid var(--line)' }}>
                    <span className="section-copy" style={{ fontSize: '0.85rem' }}>Statements and receipt downloads can be connected once export endpoints are added.</span>
                  </div>
                  <div style={{ padding: '16px', borderBottom: '1px solid var(--line)' }}>
                    <span className="section-copy" style={{ fontSize: '0.85rem' }}>Dispute tracking can be added later without restructuring the UI shell.</span>
                  </div>
                  <div style={{ padding: '16px' }}>
                    <span className="section-copy" style={{ fontSize: '0.85rem' }}>The app is already prepared for future React pages when you add more APIs.</span>
                  </div>
                </div>
              </div>
            </div>
          </>
        );

      case "add-money":
        return (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div className="section-card">
              <h3>Add money</h3>
              <p className="section-copy" style={{ marginBottom: '24px' }}>Push funds into the wallet through the existing top-up endpoint.</p>
              <form className="field-grid" onSubmit={handleTopup}>
                <Field
                  label="AMOUNT"
                  value={topupForm.amount}
                  onChange={(event) => setTopupForm((current) => ({ ...current, amount: event.target.value }))}
                />
                <Field
                  label="PAYMENT METHOD"
                  as="select"
                  value={topupForm.method}
                  options={["UPI", "Card", "Net Banking"]}
                  onChange={(event) => setTopupForm((current) => ({ ...current, method: event.target.value }))}
                />
                <div style={{ marginTop: '16px' }}>
                  <button type="submit" className="app-button primary" style={{ borderRadius: '999px', padding: '12px 24px' }}>
                    Top Up Wallet
                  </button>
                </div>
              </form>
            </div>

            <div className="section-card">
              <h3>Transfer funds</h3>
              <p className="section-copy" style={{ marginBottom: '24px' }}>Move balance to another user through the wallet transfer API.</p>
              <form className="field-grid" onSubmit={handleTransfer}>
                <Field
                  label="RECIPIENT USER ID"
                  value={transferForm.recipient}
                  onChange={(event) => setTransferForm((current) => ({ ...current, recipient: event.target.value }))}
                />
                <p className="section-copy" style={{ fontSize: '0.8rem', marginTop: '-10px', marginBottom: '8px' }}>
                  The current backend transfer API accepts a recipient UUID, not username or email. For now, the other user has to share their user ID from the Profile section.
                </p>
                <Field
                  label="AMOUNT"
                  value={transferForm.amount}
                  onChange={(event) => setTransferForm((current) => ({ ...current, amount: event.target.value }))}
                />
                <Field
                  label="NOTES"
                  as="textarea"
                  value={transferForm.note}
                  onChange={(event) => setTransferForm((current) => ({ ...current, note: event.target.value }))}
                />
                <div style={{ marginTop: '16px' }}>
                  <button type="submit" className="app-button primary" style={{ borderRadius: '999px', padding: '12px 24px' }}>
                    Send Funds
                  </button>
                </div>
              </form>
            </div>
          </div>
        );

      case "transactions":
        return (
          <div className="section-card">
            <h3>Transaction history</h3>
            <p className="section-copy" style={{ marginBottom: '24px' }}>Synced directly from the transaction service through the gateway.</p>
            <table className="transaction-table">
              <thead>
                <tr>
                  <th>TYPE</th>
                  <th>AMOUNT</th>
                  <th>STATUS</th>
                  <th>TIMESTAMP</th>
                  <th>NOTES</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((item, idx) => (
                  <tr key={idx}>
                    <td style={{ fontWeight: 600 }}>{item.type === 'CREDIT' ? 'Top-up' : 'Transfer'}</td>
                    <td>{formatMoney(item.amount)}</td>
                    <td>Completed</td>
                    <td>{formatTime(item.time)}</td>
                    <td style={{ color: 'var(--muted)' }}>{item.title}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );

      case "rewards":
        return (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '16px' }}>
            <div className="section-card">
              <h3>Rewards profile</h3>
              <p className="section-copy" style={{ marginBottom: '24px' }}>Live loyalty summary from the rewards service.</p>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div style={{ padding: '16px', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px' }}>
                  <span className="section-copy" style={{ fontSize: '0.85rem' }}>Available points</span>
                  <h2 style={{ color: 'var(--accent)', marginTop: '12px' }}>{rewardState.points.toLocaleString("en-IN")}</h2>
                </div>
                <div style={{ padding: '16px', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px' }}>
                  <span className="section-copy" style={{ fontSize: '0.85rem' }}>Current tier</span>
                  <h2 style={{ color: '#fb923c', marginTop: '12px' }}>{rewardState.tier}</h2>
                </div>
              </div>
            </div>

            <div className="section-card">
              <h3>Rewards catalog</h3>
              <p className="section-copy" style={{ marginBottom: '24px' }}>Redeem eligible items directly from the active loyalty catalog.</p>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                {rewardsCatalog.map((item) => (
                  <div key={item.id} style={{ padding: '24px', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <strong style={{ fontSize: '1.1rem' }}>{item.name}</strong>
                      <span style={{ fontSize: '0.8rem', color: '#fb923c', border: '1px solid rgba(251, 146, 60, 0.3)', padding: '2px 8px', borderRadius: '999px' }}>{item.cost} pts</span>
                    </div>
                    <p className="section-copy" style={{ fontSize: '0.85rem', marginTop: '8px' }}>Example description for {item.name}</p>
                    <p className="section-copy" style={{ fontSize: '0.85rem', marginTop: '16px', marginBottom: '16px' }}>Tier: {item.tier} <span style={{ marginLeft: '12px' }}>Stock: {item.stock}</span></p>
                    <button className="app-button primary" style={{ borderRadius: '999px', padding: '8px 24px' }} onClick={() => { setRewardSelection(item.id); handleRedeem(); }}>
                      Redeem
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        );

      case "profile-kyc":
        return (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div className="section-card">
              <h3>Current KYC state</h3>
              <p className="section-copy" style={{ marginBottom: '24px' }}>This reflects the current status from the user service.</p>
              <div style={{ padding: '24px', background: 'rgba(19, 34, 56, 0.2)', border: '1px solid var(--line)', borderRadius: '16px' }}>
                <span className="section-copy" style={{ fontSize: '0.85rem' }}>Verification status</span>
                <h2 style={{ marginTop: '12px', marginBottom: '16px' }}>Approved</h2>
                <p className="section-copy" style={{ fontSize: '0.85rem' }}>If your KYC is pending, the admin team can review and approve it from their portal.</p>
              </div>
            </div>

            <div className="section-card">
              <h3>Submit or resubmit KYC</h3>
              <p className="section-copy" style={{ marginBottom: '24px' }}>Upload metadata for your verification request through the existing endpoint.</p>
              <form className="field-grid" onSubmit={handleKycSubmit}>
                <div className="field-row two-up">
                  <Field
                    label="DOCUMENT TYPE"
                    as="select"
                    value={kycForm.documentType}
                    options={["Aadhar", "Passport", "Driver License"]}
                    onChange={(event) => setKycForm((current) => ({ ...current, documentType: event.target.value }))}
                  />
                  <Field
                    label="DOCUMENT NUMBER"
                    value={kycForm.documentId}
                    onChange={(event) => setKycForm((current) => ({ ...current, documentId: event.target.value }))}
                  />
                </div>
                <Field
                  label="DOCUMENT URL"
                  value="https://example.com/document"
                />
                <div style={{ marginTop: '16px' }}>
                  <button type="submit" className="app-button primary" style={{ width: '100%', borderRadius: '999px', padding: '16px' }}>
                    Submit KYC
                  </button>
                </div>
              </form>
            </div>
          </div>
        );

      default:
        return (
          <div className="section-card">
            <h3>{activePage}</h3>
            <p className="section-copy">This page layout is not fully configured for the new design yet.</p>
          </div>
        );
    }
  }"""

app_jsx = re.sub(
    r'function renderApplicantPage\(\)\s*\{[\s\S]*?(?=\n\s*function renderAdminPage\(\)\s*\{)',
    new_render_applicant + '\n\n',
    app_jsx
)

with open('src/App.jsx', 'w') as f:
    f.write(app_jsx)
