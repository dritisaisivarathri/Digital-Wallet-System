export const adminPageLinks = {
  dashboard: "Overview",
  approvals: "KYC Queue",
  campaigns: "Campaigns",
  catalog: "Reward Catalog",
  notifications: "Notifications",
  profile: "Profile",
};

function resolveKycDocumentHref(buildUrl, documentUrl) {
  const normalized = String(documentUrl || "").trim();
  if (!normalized) return "";
  if (/^(blob:|data:|https?:)/i.test(normalized)) return normalized;
  if (normalized.startsWith("/api/users/uploads/")) return buildUrl(normalized);
  if (normalized.startsWith("/users/uploads/")) return buildUrl(normalized);
  if (normalized.startsWith("/uploads/")) return buildUrl(`/users${normalized}`);
  if (normalized.startsWith("/kyc/") || normalized.startsWith("/profiles/")) {
    return buildUrl(`/users/uploads${normalized}`);
  }
  return buildUrl(`/users/uploads/${normalized.replace(/^\/+/, "")}`);
}

export function renderAdminWorkspace({
  activePage,
  campaigns,
  approvals,
  approvalReviewState,
  setApprovalReviewState,
  rewardsCatalog,
  adminNotificationsFeed,
  buildUrl,
  handleApproval,
  Field,
  campaignForm,
  setCampaignForm,
  handleCampaign,
  catalogForm,
  setCatalogForm,
  handleCatalogCreate,
  formatTime,
  adminProfile,
  handleAdminProfileSave,
  handleProfileImageError,
  getProfileImageSrc,
  getInitials,
  startProfilePhotoUpload,
  setAdminProfile,
  handleAdminPasswordUpdate,
  adminPasswordForm,
  setAdminPasswordForm,
  PASSWORD_HINT,
  PASSWORD_HELPER_TEXT,
  PASSWORD_MIN_LENGTH,
  PASSWORD_PATTERN,
  adminSystemStatus,
  adminActionItems,
}) {
  switch (activePage) {
    case "dashboard":
    default:
      return (
        <>
          <div className="overview-metric-grid">
            <div className="overview-metric-card">
              <span className="overview-card-kicker">STATUS</span>
              <p className="overview-card-label">Current Status</p>
              <h2>{adminSystemStatus || "Operational"}</h2>
            </div>
            <div className="overview-metric-card">
              <span className="overview-card-kicker">CAMPAIGNS</span>
              <p className="overview-card-label">Active Campaigns</p>
              <h2>{campaigns.length}</h2>
            </div>
            <div className="overview-metric-card">
              <span className="overview-card-kicker">KYC</span>
              <p className="overview-card-label">Pending Reviews</p>
              <h2>{approvals.length}</h2>
            </div>
            <div className="overview-metric-card">
              <span className="overview-card-kicker">CATALOG</span>
              <p className="overview-card-label">Reward Items</p>
              <h2>{rewardsCatalog.length}</h2>
            </div>
          </div>

          <div className="overview-detail-grid">
            <div className="section-card overview-panel">
              <h3>Quick summary</h3>
              <p className="section-copy">See the most important items that need attention.</p>
              <div className="overview-note-grid">
                <div className="overview-note-card">
                  <p className="overview-note-kicker">Pending KYC queue</p>
                  <h4>{approvals.length}</h4>
                  <p>Review approvals and rejections from the KYC queue tab.</p>
                </div>
                <div className="overview-note-card">
                  <p className="overview-note-kicker">Notification records</p>
                  <h4>{adminNotificationsFeed.length}</h4>
                  <p>Keep track of recent updates sent to users.</p>
                </div>
              </div>
            </div>

            <div className="section-card overview-panel">
              <h3>Admin actions</h3>
              <p className="section-copy">Live actions and status updates from admin workflows.</p>
              <div className="overview-highlight-stack">
                {(adminActionItems || []).map((item) => (
                  <div key={item} className="overview-highlight-card">
                    {item}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </>
      );

    case "approvals":
      return (
        <div className="section-card notifications-panel">
          <h3>Pending KYC approvals</h3>
          <p className="section-copy">Review submitted documents and approve or reject them.</p>
          {approvals.length === 0 ? (
            <div className="admin-empty-state">
              <h4 style={{ marginBottom: "8px" }}>No KYC items waiting</h4>
              <p className="section-copy" style={{ fontSize: "0.85rem" }}>The pending queue is clear right now.</p>
            </div>
          ) : (
            <div className="notifications-list">
              {approvals.map((item) => (
                <div key={item.id || item.userId} className="notification-row-card kyc-approval-card">
                  {(() => {
                    const reviewKey = String(item?.id || item?.userId || item?.email || "").trim();
                    const reviewed = reviewKey ? Boolean(approvalReviewState?.[reviewKey]) : false;
                    const documentUrl = String(item.documentUrl || "").trim().toLowerCase();
                    const hasDocument = Boolean(documentUrl);
                    const documentHref = hasDocument ? resolveKycDocumentHref(buildUrl, item.documentUrl) : "";
                    const isPdf = documentUrl.endsWith(".pdf");
                    const isImage = /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(documentUrl);
                    const canTakeAction = reviewed && hasDocument;

                    return (
                      <>
                        <div className="notification-copy">
                          <strong>{item.email || item.userId}</strong>
                          <p style={{ fontSize: "0.85rem", opacity: 0.8 }}>
                            {item.documentType} — {item.documentNumber}
                          </p>

                          {hasDocument && (
                            <div className="kyc-document-preview-wrap">
                              {isPdf ? (
                                <iframe
                                  src={`${documentHref}#toolbar=0&navpanes=0&scrollbar=0`}
                                  className="kyc-document-preview-frame"
                                  title="KYC PDF Preview"
                                />
                              ) : isImage ? (
                                <img
                                  src={documentHref}
                                  alt="KYC Document Preview"
                                  className="kyc-document-image-preview"
                                />
                              ) : (
                                <div className="kyc-document-preview-placeholder">
                                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ marginBottom: "12px", opacity: 0.5 }}>
                                    <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
                                    <polyline points="14 2 14 8 20 8" />
                                  </svg>
                                  <strong>Document Preview Unavailable</strong>
                                  <p>Inline preview is not supported for this file type ({documentUrl.split('.').pop()}).</p>
                                </div>
                              )}
                              <div className="kyc-document-review-row">
                                <a
                                  href={documentHref}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="kyc-document-link"
                                >
                                  <span>Open in new tab</span>
                                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                                    <polyline points="15 3 21 3 21 9" />
                                    <line x1="10" y1="14" x2="21" y2="3" />
                                  </svg>
                                </a>
                                <label className="kyc-review-consent">
                                  <input
                                    type="checkbox"
                                    checked={reviewed}
                                    onChange={(event) => {
                                      if (!reviewKey) return;
                                      setApprovalReviewState((current) => ({
                                        ...current,
                                        [reviewKey]: event.target.checked,
                                      }));
                                    }}
                                  />
                                  <span>Confirm document reviewed</span>
                                </label>
                              </div>
                            </div>
                          )}

                          {!hasDocument && (
                            <div className="kyc-document-preview-placeholder" style={{ marginTop: "12px", borderRadius: "20px" }}>
                              <p>No document was found for this KYC request.</p>
                            </div>
                          )}
                        </div>

                        <div className="kyc-action-footer">
                          <button
                            className="app-button secondary"
                            style={{ borderRadius: "999px", padding: "0 24px" }}
                            onClick={() => handleApproval(item.userId, "REJECTED", reviewed, reviewKey)}
                            disabled={!canTakeAction}
                            title={!canTakeAction ? "Review the document and tick the checkbox first." : "Reject KYC"}
                          >
                            Reject
                          </button>
                          <button
                            className="app-button primary"
                            style={{ borderRadius: "999px", padding: "0 24px", minWidth: "140px" }}
                            onClick={() => handleApproval(item.userId, "APPROVED", reviewed, reviewKey)}
                            disabled={!canTakeAction}
                            title={!canTakeAction ? "Review the document and tick the checkbox first." : "Approve KYC"}
                          >
                            Approve
                          </button>
                        </div>
                      </>
                    );
                  })()}
                </div>
              ))}
            </div>
          )}
        </div>
      );

    case "campaigns":
      return (
        <div className="wallet-page-grid">
          <div className="section-card wallet-form-panel">
            <h3>Create campaign</h3>
            <p className="section-copy">Add a campaign for the users you want to target.</p>
            <form className="field-grid wallet-form-grid" onSubmit={handleCampaign}>
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
              <button type="submit" className="app-button primary wallet-inline-submit">
                Create Campaign
              </button>
            </form>
          </div>

          <div className="section-card wallet-form-panel">
            <h3>Campaign list</h3>
            <p className="section-copy">Review all campaigns created so far.</p>
            {campaigns.length === 0 ? (
              <div className="admin-empty-state">
                <h4 style={{ marginBottom: "8px" }}>No campaigns yet</h4>
                <p className="section-copy" style={{ fontSize: "0.85rem" }}>Create your first reward campaign from the form on the left.</p>
              </div>
            ) : (
              <div className="notifications-list">
                {campaigns.map((item) => (
                  <div key={item.id} className="notification-row-card">
                    <div className="notification-copy">
                      <strong>{item.email || item.userId}</strong>
                      <p>Target: {item.audience}</p>
                    </div>
                    <div className="notification-meta">
                      <span>Active</span>
                      <time>{item.liveAt}</time>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      );

    case "catalog":
      return (
        <div className="wallet-page-grid rewards-layout-grid">
          <div className="section-card wallet-form-panel">
            <h3>Create reward item</h3>
            <p className="section-copy">Add a reward users can redeem with points.</p>
            <form className="field-grid wallet-form-grid" onSubmit={handleCatalogCreate}>
              <Field label="NAME" value={catalogForm.name} onChange={(event) => setCatalogForm((current) => ({ ...current, name: event.target.value }))} />
              <Field label="DESCRIPTION" as="textarea" value={catalogForm.description} onChange={(event) => setCatalogForm((current) => ({ ...current, description: event.target.value }))} />
              <div className="field-row two-up">
                <Field label="COST IN POINTS" value={catalogForm.cost} onChange={(event) => setCatalogForm((current) => ({ ...current, cost: event.target.value }))} />
                <Field label="STOCK QUANTITY" value={catalogForm.stock} onChange={(event) => setCatalogForm((current) => ({ ...current, stock: event.target.value }))} />
              </div>
              <Field label="REQUIRED TIER" as="select" value={catalogForm.tier} options={["All", "Silver", "Gold", "Platinum"]} onChange={(event) => setCatalogForm((current) => ({ ...current, tier: event.target.value }))} />
              <button type="submit" className="app-button primary wallet-full-submit">
                Create Catalog Item
              </button>
            </form>
          </div>

          <div className="section-card rewards-catalog-panel">
            <h3>Reward catalog</h3>
            <p className="section-copy">Review the rewards currently available to users.</p>
            <div className="rewards-catalog-grid">
              {rewardsCatalog.map((item) => (
                <div key={item.id} className="reward-product-card">
                  <div className="reward-product-head">
                    <strong>{item.email || item.userId}</strong>
                  </div>
                  <p>{item.description}</p>
                  <div className="reward-product-meta">{item.cost} pts <span>Stock {item.stock}</span> <span>{item.tier}</span></div>
                </div>
              ))}
            </div>
          </div>
        </div>
      );

    case "notifications":
      return (
        <div className="section-card notifications-panel">
          <h3>Notification history</h3>
          <p className="section-copy">Review pending KYC reviews, KYC status updates, newly created campaigns, and reward catalog additions.</p>
          {adminNotificationsFeed.length === 0 ? (
            <div className="admin-empty-state">
              <h4 style={{ marginBottom: "8px" }}>No admin notifications yet</h4>
              <p className="section-copy" style={{ fontSize: "0.85rem" }}>Pending KYC, campaign-created, reward-added, and KYC status events will appear here.</p>
            </div>
          ) : (
            <div className="notifications-list">
              {adminNotificationsFeed.map((item) => (
                <div key={item.userId || item.id} className="notification-row-card">
                  <div className="notification-copy">
                    <strong>{item.title}</strong>
                    <p>{item.body}</p>
                    <p className="section-copy" style={{ fontSize: "0.8rem", marginTop: "8px" }}>User: {item.user}</p>
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

    case "profile":
      return (
        <div className="wallet-page-grid profile-page-grid">
          <div className="section-card profile-details-panel">
            <h3>Admin profile</h3>
            <p className="section-copy">Update your account details here.</p>
            <form className="field-grid wallet-form-grid" onSubmit={handleAdminProfileSave}>
              <div className="profile-photo-card">
                <div className="profile-avatar">
                  {adminProfile.previewUrl ? (
                    <img src={adminProfile.previewUrl} alt="Preview" onError={() => handleProfileImageError("admin")} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                  ) : adminProfile.profileImageUrl ? (
                    <img src={getProfileImageSrc(adminProfile.profileImageUrl)} alt="Avatar" onError={() => handleProfileImageError("admin")} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                  ) : (
                    getInitials(adminProfile.name)
                  )}
                </div>
                <div className="profile-photo-copy">
                  <strong>Profile photo</strong>
                  <p>Upload a photo to personalize your admin account.</p>
                  <input
                    type="file"
                    id="admin-photo-input"
                    style={{ display: 'none' }}
                    accept="image/*"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) startProfilePhotoUpload(file, "admin");
                      e.target.value = "";
                    }}
                  />
                  <button type="button" className="theme-btn" onClick={() => document.getElementById('admin-photo-input').click()}>
                    Upload Photo
                  </button>
                </div>
              </div>

              <div className="field-row two-up">
                <Field label="FULL NAME" value={adminProfile.name} onChange={(event) => setAdminProfile((current) => ({ ...current, name: event.target.value }))} />
                <Field label="USERNAME" value={adminProfile.username} onChange={(event) => setAdminProfile((current) => ({ ...current, username: event.target.value }))} />
              </div>

              <div className="field-row two-up">
                <Field label="EMAIL" value={adminProfile.email} onChange={(event) => setAdminProfile((current) => ({ ...current, email: event.target.value }))} />
                <Field label="PHONE NUMBER" value={adminProfile.phone} onChange={(event) => setAdminProfile((current) => ({ ...current, phone: event.target.value }))} />
              </div>

              <div className="profile-meta-grid admin-profile-meta-grid">
                <div className="profile-meta-card">
                  <span>ROLE</span>
                  <strong>{adminProfile.role}</strong>
                </div>
                <div className="profile-meta-card">
                  <span>USER ID</span>
                  <strong>{adminProfile.userId}</strong>
                </div>
              </div>

              <button type="submit" className="app-button primary wallet-full-submit">
                Save Profile
              </button>
            </form>
          </div>

          <div className="section-card profile-password-panel">
            <h3>Change password</h3>
            <p className="section-copy">Keep your admin account secure by updating your password.</p>
            <form className="field-grid wallet-form-grid" onSubmit={handleAdminPasswordUpdate}>
              <Field label="CURRENT PASSWORD" type="password" value={adminPasswordForm.currentPassword} onChange={(event) => setAdminPasswordForm((current) => ({ ...current, currentPassword: event.target.value }))} placeholder="Enter current password" />
              <Field label="NEW PASSWORD" type="password" value={adminPasswordForm.newPassword} onChange={(event) => setAdminPasswordForm((current) => ({ ...current, newPassword: event.target.value }))} placeholder={PASSWORD_HINT} helperText={PASSWORD_HELPER_TEXT} minLength={PASSWORD_MIN_LENGTH} pattern={PASSWORD_PATTERN} title={PASSWORD_HELPER_TEXT} />
              <Field label="CONFIRM NEW PASSWORD" type="password" value={adminPasswordForm.confirmPassword} onChange={(event) => setAdminPasswordForm((current) => ({ ...current, confirmPassword: event.target.value }))} placeholder={PASSWORD_HINT} minLength={PASSWORD_MIN_LENGTH} pattern={PASSWORD_PATTERN} title={PASSWORD_HELPER_TEXT} />
              <button type="submit" className="app-button primary wallet-inline-submit">
                Change Password
              </button>
            </form>
          </div>
        </div>
      );
  }
}
