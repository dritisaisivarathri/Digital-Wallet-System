export const applicantPageLinks = {
  dashboard: "Overview",
  "add-money": "Wallet",
  transactions: "Transactions",
  rewards: "Rewards",
  "profile-kyc": "KYC",
  profile: "Profile",
  support: "Notifications",
};

export function renderApplicantPage({
  activePage,
  formatMoney,
  walletBalance,
  ledgerBalance,
  rewardState,
  formatKycStatus,
  kycStatus,
  transactions,
  Field,
  topupForm,
  setTopupForm,
  handleTopup,
  transferForm,
  setTransferForm,
  handleTransfer,
  formatTime,
  rewardsCatalog,
  setRewardSelection,
  handleRedeem,
  describeKycStatus,
  kycRejectionReason,
  normalizeKycStatus,
  kycForm,
  setKycForm,
  handleKycSubmit,
  handleProfileSave,
  profile,
  handleProfileImageError,
  getProfileImageSrc,
  getInitials,
  startProfilePhotoUpload,
  setProfile,
  handlePasswordUpdate,
  passwordForm,
  setPasswordForm,
  PASSWORD_HINT,
  PASSWORD_HELPER_TEXT,
  PASSWORD_MIN_LENGTH,
  PASSWORD_PATTERN,
  notificationsFeed,
  accountStatusItems,
}) {
  switch (activePage) {
    case "dashboard":
      return (
        <>
          <div className="overview-metric-grid">
            <div className="overview-metric-card">
              <span className="overview-card-kicker">WALLET</span>
              <p className="overview-card-label">Available Balance</p>
              <h2>{formatMoney(walletBalance)}</h2>
            </div>
            <div className="overview-metric-card">
              <span className="overview-card-kicker">LEDGER</span>
              <p className="overview-card-label">Ledger Balance</p>
              <h2>{formatMoney(ledgerBalance)}</h2>
            </div>
            <div className="overview-metric-card">
              <span className="overview-card-kicker">LOYALTY</span>
              <p className="overview-card-label">Reward Points</p>
              <h2>{rewardState.points}</h2>
            </div>
            <div className="overview-metric-card">
              <span className="overview-card-kicker">KYC</span>
              <p className="overview-card-label">Verification Status</p>
              <h2>{formatKycStatus(kycStatus)}</h2>
            </div>
          </div>

          <div className="overview-detail-grid">
            <div className="section-card overview-panel">
              <h3>Financial pulse</h3>
              <p className="section-copy">A quick summary of your account activity.</p>
              <div className="overview-note-grid">
                <div className="overview-note-card">
                  <p className="overview-note-kicker">Tier & rewards readiness</p>
                  <h4>{rewardState.tier}</h4>
                  <p>Keep transacting and redeeming from the active catalog to move up loyalty tiers.</p>
                </div>
                <div className="overview-note-card">
                  <p className="overview-note-kicker">Recent transaction count</p>
                  <h4>{transactions.length}</h4>
                  <p>Review your latest wallet activity and recent payment history here.</p>
                </div>
              </div>
            </div>

            <div className="section-card overview-panel">
              <h3>Account highlights</h3>
              <p className="section-copy">Live account status synced from wallet, rewards, KYC, and notifications.</p>
              <div className="overview-highlight-stack">
                {(accountStatusItems || []).map((item) => (
                  <div key={item.label} className="overview-highlight-card">
                    <strong>{item.label}: </strong>{item.value}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </>
      );

    case "add-money":
      return (
        <div className="wallet-page-grid">
          <div className="section-card wallet-form-panel">
            <h3>Add money</h3>
            <p className="section-copy">Add funds to your wallet using your preferred payment method.</p>
            <form className="field-grid wallet-form-grid" onSubmit={handleTopup}>
              <Field
                label="AMOUNT"
                value={topupForm.amount}
                onChange={(event) => setTopupForm((current) => ({ ...current, amount: event.target.value }))}
                placeholder="Enter amount"
              />
              <Field
                label="PAYMENT METHOD"
                as="select"
                value={topupForm.method}
                options={["UPI", "Card", "Net Banking"]}
                onChange={(event) => setTopupForm((current) => ({ ...current, method: event.target.value }))}
              />
              <button type="submit" className="app-button primary wallet-inline-submit">
                Top Up Wallet
              </button>
            </form>
          </div>

          <div className="section-card wallet-form-panel">
            <h3>Transfer funds</h3>
            <p className="section-copy">Send money to another wallet user using their user ID.</p>
            <form className="field-grid wallet-form-grid" onSubmit={handleTransfer}>
              <Field
                label="RECIPIENT USER ID"
                value={transferForm.recipient}
                onChange={(event) => setTransferForm((current) => ({ ...current, recipient: event.target.value }))}
                placeholder="Enter recipient user ID"
              />
              <p className="wallet-helper-copy">
                Ask the recipient to share the user ID shown in their profile before sending money.
              </p>
              <Field
                label="AMOUNT"
                value={transferForm.amount}
                onChange={(event) => setTransferForm((current) => ({ ...current, amount: event.target.value }))}
                placeholder="Enter amount"
              />
              <Field
                label="NOTES"
                as="textarea"
                value={transferForm.note}
                onChange={(event) => setTransferForm((current) => ({ ...current, note: event.target.value }))}
                placeholder="Lunch split, rent share, wallet transfer..."
              />
              <button type="submit" className="app-button primary wallet-inline-submit">
                Send Funds
              </button>
            </form>
          </div>
        </div>
      );

    case "transactions":
      return (
        <div className="section-card transaction-history-panel">
          <h3>Transaction history</h3>
          <p className="section-copy">Review your recent wallet activity.</p>
          <div className="transaction-table-shell">
            <table className="transaction-table compact-wallet-table">
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
                {transactions.map((item) => (
                  <tr key={item.id}>
                    <td>{item.title || (item.type === "CREDIT" ? "Topup" : "Transfer")}</td>
                    <td>{formatMoney(item.amount)}</td>
                    <td>Completed</td>
                    <td>{formatTime(item.time)}</td>
                    <td>{item.title}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      );

    case "rewards":
      return (
        <div className="wallet-page-grid rewards-layout-grid">
          <div className="section-card rewards-profile-panel">
            <h3>Rewards profile</h3>
            <p className="section-copy">See your points and current rewards tier.</p>
            <div className="rewards-summary-grid">
              <div className="rewards-summary-card">
                <span>Available points</span>
                <h2>{rewardState.points}</h2>
              </div>
              <div className="rewards-summary-card">
                <span>Current tier</span>
                <h2 className="tier">{rewardState.tier}</h2>
              </div>
            </div>
          </div>

          <div className="section-card rewards-catalog-panel">
            <h3>Rewards catalog</h3>
            <p className="section-copy">Redeem available rewards using your points.</p>
            <div className="rewards-catalog-grid">
              {rewardsCatalog.map((item) => (
                <div key={item.id} className="reward-product-card">
                  <div className="reward-product-head">
                    <strong>{item.name || item.id}</strong>
                    <span>{item.cost} pts</span>
                  </div>
                  <p>{item.description}</p>
                  <div className="reward-product-meta">Tier: {item.tier} <span>Stock: {item.stock}</span></div>
                  <button className="app-button primary wallet-inline-submit" onClick={() => { setRewardSelection(item.id); handleRedeem(item.id); }}>
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
        <div className="wallet-page-grid">
          <div className="section-card kyc-state-panel">
            <h3>KYC status</h3>
            <p className="section-copy">Your verification progress.</p>
            <div className="kyc-status-card">
              <span>Verification status</span>
              <h2>{formatKycStatus(kycStatus)}</h2>
              <p>{describeKycStatus(kycStatus, kycRejectionReason)}</p>
            </div>
          </div>

          {normalizeKycStatus(kycStatus) !== "APPROVED" && (
            <div className="section-card wallet-form-panel">
              <h3>Submit or resubmit KYC</h3>
              <p className="section-copy">Enter your document details to continue with verification.</p>
              <form className="field-grid wallet-form-grid" onSubmit={handleKycSubmit}>
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
                <div className="field-row">
                  <Field
                    label="DOCUMENT FILE (.PDF, .DOCX)"
                    type="file"
                    accept=".pdf,.docx"
                    onChange={(event) => setKycForm((current) => ({ ...current, documentFile: event.target.files[0] }))}
                  />
                </div>
                <button type="submit" className="app-button primary wallet-full-submit">
                  Submit KYC
                </button>
              </form>
            </div>
          )}
        </div>
      );

    case "profile":
      return (
        <div className="wallet-page-grid profile-page-grid">
          <div className="section-card profile-details-panel">
            <h3>Profile details</h3>
            <p className="section-copy">Update your account information here.</p>
            <form className="field-grid wallet-form-grid" onSubmit={handleProfileSave}>
              <div className="profile-photo-card">
                <div className="profile-avatar">
                  {profile.previewUrl ? (
                    <img src={profile.previewUrl} alt="Preview" onError={() => handleProfileImageError("applicant")} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                  ) : profile.profileImageUrl ? (
                    <img src={getProfileImageSrc(profile.profileImageUrl)} alt="Avatar" onError={() => handleProfileImageError("applicant")} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                  ) : (
                    getInitials(profile.name)
                  )}
                </div>
                <div className="profile-photo-copy">
                  <strong>Profile photo</strong>
                  <p>Upload a clear square photo so your account is easier to recognize.</p>
                  <input
                    type="file"
                    id="profile-photo-input"
                    style={{ display: 'none' }}
                    accept="image/*"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) startProfilePhotoUpload(file, "applicant");
                      e.target.value = "";
                    }}
                  />
                  <button type="button" className="theme-btn" onClick={() => document.getElementById('profile-photo-input').click()}>
                    Upload Photo
                  </button>
                </div>
              </div>

              <div className="field-row two-up">
                <Field
                  label="FULL NAME"
                  value={profile.name}
                  onChange={(event) => setProfile((current) => ({ ...current, name: event.target.value }))}
                />
                <Field
                  label="USERNAME"
                  value={profile.username}
                  onChange={(event) => setProfile((current) => ({ ...current, username: event.target.value }))}
                />
              </div>

              <div className="field-row two-up">
                <Field
                  label="EMAIL"
                  value={profile.email}
                  onChange={(event) => setProfile((current) => ({ ...current, email: event.target.value }))}
                />
                <Field
                  label="PHONE NUMBER"
                  value={profile.phone}
                  onChange={(event) => setProfile((current) => ({ ...current, phone: event.target.value }))}
                />
              </div>

              <div className="profile-meta-grid">
                <div className="profile-meta-card">
                  <span>ROLE</span>
                  <strong>{profile.role}</strong>
                </div>
                <div className="profile-meta-card">
                  <span>STATUS</span>
                  <strong>{profile.status}</strong>
                </div>
                <div className="profile-meta-card">
                  <span>USER ID</span>
                  <strong>{profile.userId}</strong>
                </div>
              </div>

              <button type="submit" className="app-button primary wallet-full-submit">
                Save Profile
              </button>
            </form>
          </div>

          <div className="section-card profile-password-panel">
            <h3>Change password</h3>
            <p className="section-copy">Keep your account secure by updating your password.</p>
            <form className="field-grid wallet-form-grid" onSubmit={handlePasswordUpdate}>
              <Field
                label="CURRENT PASSWORD"
                type="password"
                value={passwordForm.currentPassword}
                onChange={(event) => setPasswordForm((current) => ({ ...current, currentPassword: event.target.value }))}
                placeholder="Enter current password"
              />
              <Field
                label="NEW PASSWORD"
                type="password"
                value={passwordForm.newPassword}
                onChange={(event) => setPasswordForm((current) => ({ ...current, newPassword: event.target.value }))}
                placeholder={PASSWORD_HINT}
                helperText={PASSWORD_HELPER_TEXT}
                minLength={PASSWORD_MIN_LENGTH}
                pattern={PASSWORD_PATTERN}
                title={PASSWORD_HELPER_TEXT}
              />
              <Field
                label="CONFIRM NEW PASSWORD"
                type="password"
                value={passwordForm.confirmPassword}
                onChange={(event) => setPasswordForm((current) => ({ ...current, confirmPassword: event.target.value }))}
                placeholder={PASSWORD_HINT}
                minLength={PASSWORD_MIN_LENGTH}
                pattern={PASSWORD_PATTERN}
                title={PASSWORD_HELPER_TEXT}
              />
              <button type="submit" className="app-button primary wallet-inline-submit">
                Change Password
              </button>
            </form>
          </div>
        </div>
      );

    case "support":
      return (
        <div className="section-card notifications-panel">
          <h3>Notifications</h3>
          <p className="section-copy">See recent account updates and important alerts.</p>
          {notificationsFeed.length === 0 ? (
            <div className="admin-empty-state">
              <h4 style={{ marginBottom: "8px" }}>No notifications yet</h4>
              <p className="section-copy" style={{ fontSize: "0.85rem" }}>Approval and rejection emails will appear here after the admin reviews your KYC.</p>
            </div>
          ) : (
            <div className="notifications-list">
              {notificationsFeed.map((item) => (
                <div key={item.userId || item.id} className="notification-row-card">
                  <div className="notification-copy">
                    <strong>{item.title}</strong>
                    <p>{item.body}</p>
                  </div>
                  <div className="notification-meta">
                    <span>{item.channel}</span>
                    <time>{formatTime(item.time)}</time>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      );

    default:
      return (
        <div className="section-card">
          <h3>{activePage}</h3>
          <p className="section-copy">This page is not available right now.</p>
        </div>
      );
  }
}
