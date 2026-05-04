import { startTransition, useEffect, useRef, useState } from "react";
import { apiRequest, buildUrl } from "./lib/api";
import { clearSession, readSession, writeSession } from "./lib/session";
import { applicantPageLinks, renderApplicantPage } from "./pages/applicant/ApplicantPages";
import { adminPageLinks, renderAdminWorkspace } from "./pages/admin/AdminPages";

const WalletIcon = ({ size = 20 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20 12V8H6a2 2 0 0 1-2-2c0-1.1.9-2 2-2h12v4" />
    <path d="M4 6v12c0 1.1.9 2 2 2h14v-4" />
    <path d="M18 12a2 2 0 0 0-2 2c0 1.1.9 2 2 2h4v-4h-4z" />
  </svg>
);

const LogoIcon = ({ size = 120 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" style={{ opacity: 0.9 }}>
    <path d="M20 12V8H6a2 2 0 0 1-2-2c0-1.1.9-2 2-2h12v4" />
    <path d="M4 6v12c0 1.1.9 2 2 2h14v-4" />
    <path d="M18 12a2 2 0 0 0-2 2c0 1.1.9 2 2 2h4v-4h-4z" />
    <circle cx="18" cy="14" r="0.5" fill="currentColor" />
    <path d="M12 12h.01" strokeWidth="3" />
  </svg>
);

const RewardsIcon = ({ size = 20 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="8" r="6" />
    <path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11" />
  </svg>
);

const KYCIcon = ({ size = 20 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    <path d="m9 12 2 2 4-4" />
  </svg>
);

const HistoryIcon = ({ size = 20 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
    <path d="M3 3v5h5" />
    <path d="M12 7v5l4 2" />
  </svg>
);

const applicantNav = Object.entries(applicantPageLinks).map(([key, label]) => ({ key, label, note: "" }));
const adminNav = Object.entries(adminPageLinks).map(([key, label]) => ({ key, label, note: "" }));

const quickAmounts = [500, 1500, 2500, 5000];
const PASSWORD_MIN_LENGTH = 8;
const PASSWORD_HINT = "At least 8 characters";
const PASSWORD_HELPER_TEXT = "Use at least 8 characters and 1 special character.";
const PASSWORD_PATTERN = "(?=.*[^A-Za-z0-9]).{8,}";
const PROFILE_IMAGE_LOAD_ATTEMPTS = 10;
const PROFILE_IMAGE_LOAD_DELAY_MS = 400;
const PROFILE_IMAGE_MAX_SIZE_BYTES = 5 * 1024 * 1024;
const PROFILE_PREVIEW_KEY_PREFIX = "digital-wallet-profile-preview";
const REWARD_BASE_NEXT_THRESHOLD = 100;
const WALLET_STATE_KEY_PREFIX = "digital-wallet-state";
const REALTIME_REFRESH_INTERVAL_MS = 8000;
const REDEEM_REFRESH_DELAY_MS = 1500;

function getInitials(name = "") {
  const initials = name
    .split(" ")
    .filter(Boolean)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  return initials || "NA";
}

function getDisplayName(profile = {}) {
  return String(
    profile?.name ||
    profile?.username ||
    profile?.email ||
    ""
  ).trim();
}

function getProfileImageSrc(profileImageUrl = "") {
  if (!profileImageUrl) {
    return "";
  }

  const normalized = String(profileImageUrl).trim().replace(/\\/g, "/");

  if (/^(blob:|data:|https?:)/i.test(normalized)) {
    return normalized;
  }

  if (normalized.startsWith("/api/users/uploads/")) {
    return buildUrl(normalized);
  }

  if (normalized.startsWith("/users/uploads/")) {
    return buildUrl(normalized);
  }

  if (normalized.startsWith("/uploads/")) {
    return buildUrl(`/users${normalized}`);
  }

  if (normalized.startsWith("/profiles/") || normalized.startsWith("/kyc/")) {
    return buildUrl(`/users/uploads${normalized}`);
  }

  if (normalized.startsWith("profiles/") || normalized.startsWith("kyc/")) {
    return buildUrl(`/users/uploads/${normalized}`);
  }

  return buildUrl(`/users/uploads/${normalized.replace(/^\/+/, "")}`);
}

function resolveProfilePreviewStorageKey(session, role) {
  const roleKey = role === "admin" ? "admin" : "applicant";
  const identity = String(session?.userId || session?.username || session?.email || "").trim().toLowerCase();
  if (!identity) {
    return "";
  }
  return `${PROFILE_PREVIEW_KEY_PREFIX}:${roleKey}:${identity}`;
}

function readCachedProfilePreview(session, role) {
  const key = resolveProfilePreviewStorageKey(session, role);
  if (!key) {
    return "";
  }
  try {
    return window.localStorage.getItem(key) || "";
  } catch (error) {
    return "";
  }
}

function writeCachedProfilePreview(session, role, value) {
  const key = resolveProfilePreviewStorageKey(session, role);
  if (!key) {
    return;
  }
  try {
    if (value) {
      window.localStorage.setItem(key, value);
    } else {
      window.localStorage.removeItem(key);
    }
  } catch (error) {
    // Ignore localStorage write errors.
  }
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(new Error("Unable to read selected image."));
    reader.readAsDataURL(file);
  });
}

async function persistInitialProfileDetails(session, fullName, phoneNumber) {
  if (!session?.token) {
    return null;
  }

  const normalizedFullName = String(fullName || "").trim();
  const normalizedPhone = String(phoneNumber || "").trim();
  if (!normalizedFullName && !normalizedPhone) {
    return null;
  }

  const formData = new FormData();
  if (normalizedFullName) {
    formData.append("fullName", normalizedFullName);
  }
  if (normalizedPhone) {
    formData.append("phoneNumber", normalizedPhone);
  }

  const updatedUser = await apiRequest("/users/profile", {
    method: "POST",
    token: session.token,
    body: formData,
  });

  return updatedUser && typeof updatedUser === "object" ? updatedUser : null;
}

function waitForDelay(delayMs) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, delayMs);
  });
}

function attemptImageLoad(src) {
  return new Promise((resolve) => {
    if (!src) {
      resolve(false);
      return;
    }

    const image = new Image();
    image.onload = () => resolve(true);
    image.onerror = () => resolve(false);
    image.src = src;
  });
}

async function waitForProfileImage(profileImageUrl) {
  const src = getProfileImageSrc(profileImageUrl);

  if (!src) {
    return false;
  }

  for (let attempt = 0; attempt < PROFILE_IMAGE_LOAD_ATTEMPTS; attempt += 1) {
    const cacheBustedSrc = `${src}${src.includes("?") ? "&" : "?"}previewCheck=${Date.now()}-${attempt}`;
    if (await attemptImageLoad(cacheBustedSrc)) {
      return true;
    }

    if (attempt < PROFILE_IMAGE_LOAD_ATTEMPTS - 1) {
      await waitForDelay(PROFILE_IMAGE_LOAD_DELAY_MS);
    }
  }

  return false;
}

const initialTransactions = [
  {
    id: "LED-4811",
    title: "Topup via UPI",
    counterparty: "HDFC UPI",
    type: "CREDIT",
    channel: "UPI",
    amount: 9500,
    status: "SUCCESS",
    time: "2026-04-27T15:08:00",
    ref: "RCPT-73218",
  },
  {
    id: "LED-4804",
    title: "Lunch",
    counterparty: "Recipient Wallet",
    type: "DEBIT",
    channel: "Wallet Transfer",
    amount: 10000,
    status: "SUCCESS",
    time: "2026-04-27T14:57:00",
    ref: "TXN-21941",
  },
  {
    id: "LED-4798",
    title: "Rent share",
    counterparty: "Recipient Wallet",
    type: "DEBIT",
    channel: "Wallet Transfer",
    amount: 20000,
    status: "SUCCESS",
    time: "2026-04-25T20:09:00",
    ref: "TXN-21792",
  },
  {
    id: "LED-4786",
    title: "string",
    counterparty: "Recipient Wallet",
    type: "DEBIT",
    channel: "Wallet Transfer",
    amount: 10000,
    status: "SUCCESS",
    time: "2026-03-30T09:43:00",
    ref: "TXN-21708",
  },
  {
    id: "LED-4772",
    title: "samosa",
    counterparty: "Recipient Wallet",
    type: "DEBIT",
    channel: "Wallet Transfer",
    amount: 500,
    status: "SUCCESS",
    time: "2026-03-28T15:27:00",
    ref: "TXN-21410",
  },
];

const initialRewardsCatalog = [
  { id: "reward-1", name: "Cashback Rs 100", cost: 20, stock: 998, tier: "Basic", description: "Direct wallet credit", rewardType: "CASHBACK", cashbackAmount: 100 },
  { id: "reward-2", name: "Amazon Voucher", cost: 50, stock: 96, tier: "Basic", description: "$10 Amazon Gift Card", rewardType: "VOUCHER", cashbackAmount: 0 },
  { id: "reward-3", name: "Flipkart Voucher", cost: 30, stock: 97, tier: "Basic", description: "$5 Flipkart Gift Card", rewardType: "VOUCHER", cashbackAmount: 0 },
];

const initialTickets = [
  {
    id: "SUP-101",
    topic: "Statement export",
    status: "Open",
    owner: "Operations Desk",
    updatedAt: "2026-04-27T07:40:00",
  },
  {
    id: "SUP-093",
    topic: "KYC clarification",
    status: "Resolved",
    owner: "Identity Team",
    updatedAt: "2026-04-26T16:20:00",
  },
];

const initialNotificationsFeed = [];

const initialApprovals = [];

const initialCampaigns = [];

const initialAdminNotificationsFeed = [];

const initialAlerts = [
  { id: "RSK-18", title: "Burst transfer pattern", severity: "High", note: "Four outbound transfers in twelve minutes from one wallet." },
  { id: "RSK-14", title: "Repeat failed top-up", severity: "Medium", note: "Two card retries from separate issuers on the same user account." },
  { id: "RSK-09", title: "Dormant wallet wake-up", severity: "Low", note: "Inactive account resumed with a small test transaction." },
];

function formatMoney(value) {
  return `₹${Number(value).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function formatTime(value) {
  const formatted = new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  }).format(new Date(value));

  return formatted.replace("am", "am").replace("pm", "pm");
}

function statusTone(value) {
  const normalized = value.toLowerCase();
  if (normalized.includes("success") || normalized.includes("verified") || normalized.includes("resolved") || normalized.includes("live")) {
    return "success";
  }
  if (normalized.includes("pending") || normalized.includes("open") || normalized.includes("medium")) {
    return "warning";
  }
  if (normalized.includes("high") || normalized.includes("failed") || normalized.includes("blocked")) {
    return "danger";
  }
  return "neutral";
}

function mapRoleToWorkspace(role) {
  return String(role || "").toUpperCase().includes("ADMIN") ? "admin" : "applicant";
}

function mapUsernameToWorkspace(username) {
  return String(username || "").trim().toLowerCase().endsWith("_admin") ? "admin" : "applicant";
}

function normalizeKycStatus(value) {
  const normalized = String(value || "").trim().toUpperCase();
  if (!normalized) {
    return "NOT_SUBMITTED";
  }
  return normalized;
}

function formatKycStatus(status) {
  switch (normalizeKycStatus(status)) {
    case "APPROVED":
      return "Approved";
    case "REJECTED":
      return "Rejected";
    case "PENDING":
      return "Pending review";
    default:
      return "Not submitted";
  }
}

function describeKycStatus(status, rejectionReason) {
  switch (normalizeKycStatus(status)) {
    case "APPROVED":
      return "Your KYC has been approved by the admin team.";
    case "REJECTED":
      return rejectionReason
        ? `Your KYC was rejected. Reason: ${rejectionReason}`
        : "Your KYC was rejected. Please review the admin feedback and resubmit.";
    case "PENDING":
      return "Your KYC is waiting for admin approval or rejection.";
    default:
      return "Submit your KYC details to start the admin review process.";
  }
}

function mapNotificationRecord(item) {
  return {
    id: item?.id || `notification-${Math.random().toString(36).slice(2)}`,
    title: item?.topic || "notification",
    body: item?.message || "",
    channel: item?.type || "Email",
    time: item?.sentAt || new Date().toISOString(),
    user: item?.userId || "",
  };
}

function toNumeric(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function normalizeTierLabel(value) {
  const normalized = String(value || "").trim().toUpperCase();
  if (!normalized) {
    return "Basic";
  }
  return normalized.charAt(0) + normalized.slice(1).toLowerCase();
}

function isUuidLike(value) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(String(value || "").trim());
}

function resolveStableSessionUserId(session, profileData = {}, currentProfile = {}) {
  const sessionUserId = String(session?.userId || "").trim();
  if (isUuidLike(sessionUserId)) {
    return sessionUserId;
  }
  const profileUserId = String(profileData?.id || "").trim();
  if (isUuidLike(profileUserId)) {
    return profileUserId;
  }
  const currentUserId = String(currentProfile?.userId || "").trim();
  return isUuidLike(currentUserId) ? currentUserId : "";
}

function getTransferNotice(error) {
  const message = String(error?.message || "").trim();
  const normalized = message.toLowerCase();

  if (normalized.includes("target user id") || normalized.includes("recipient") || normalized.includes("user id")) {
    return "Enter correct user ID. The recipient user ID was not found. Ask the recipient to copy the ID from their Profile page.";
  }
  if (normalized.includes("amount") || normalized.includes("insufficient")) {
    return "Enter correct amount. The amount must be greater than 0 and within your available wallet balance.";
  }
  if (normalized.includes("backend services") || normalized.includes("verify recipient")) {
    return "Unable to verify the recipient right now. Please make sure user-service and wallet-service are running, then try again.";
  }
  return message || "Unable to complete transfer right now.";
}

function mapRewardCatalogItem(item) {
  const rewardType = String(item?.rewardType || "").trim().toUpperCase();
  const fallbackType = `${item?.name || ""} ${item?.description || ""}`.toLowerCase().includes("cashback")
    ? "CASHBACK"
    : "VOUCHER";
  const resolvedType = rewardType || fallbackType;
  return {
    id: item?.id || `reward-${Math.random().toString(36).slice(2)}`,
    name: item?.name || "Reward",
    cost: toNumeric(item?.costInPoints ?? item?.cost),
    stock: toNumeric(item?.stockQuantity ?? item?.stock),
    tier: normalizeTierLabel(item?.requiredTier ?? item?.tier ?? "All"),
    description: item?.description || "",
    rewardType: resolvedType,
    cashbackAmount: toNumeric(item?.cashbackAmount),
  };
}

function mapTransactionRecord(item, currentUserId) {
  const fromUserId = String(item?.fromUserId || "");
  const toUserId = String(item?.toUserId || "");
  const userId = String(currentUserId || "");
  const transactionType = String(item?.type || "").toUpperCase();
  const isCredit = toUserId && toUserId === userId;
  const isTopUp = transactionType === "TOPUP";
  const isRewardCashback = transactionType === "REWARD_CASHBACK";
  const isWithdrawal = transactionType === "WITHDRAWAL";
  const amount = toNumeric(item?.amount);
  const timestamp = item?.timestamp || new Date().toISOString();
  return {
    id: item?.id || `tx-${Math.random().toString(36).slice(2)}`,
    title: isRewardCashback
      ? "Reward cashback"
      : isTopUp
        ? "Wallet top-up"
        : isWithdrawal
          ? "Wallet withdrawal"
          : isCredit
            ? "Wallet transfer received"
            : "Wallet transfer",
    counterparty: isTopUp
      ? "Wallet"
      : isRewardCashback
        ? "Rewards"
        : isWithdrawal
          ? "Wallet"
          : isCredit
            ? fromUserId || "User"
            : toUserId || "User",
    type: isCredit || isTopUp || isRewardCashback ? "CREDIT" : "DEBIT",
    channel: isRewardCashback ? "Rewards" : isTopUp ? "Top-up" : isWithdrawal ? "Withdrawal" : "Wallet Transfer",
    amount,
    status: String(item?.status || "COMPLETED").toUpperCase(),
    time: timestamp,
    ref: String(item?.id || "").slice(0, 8),
  };
}

function buildOptimisticRewardCashbackTransaction(rewardName, amount, timestamp) {
  return {
    id: `reward-${Date.now()}`,
    title: "Reward cashback",
    counterparty: "Rewards",
    type: "CREDIT",
    channel: "Rewards",
    amount,
    status: "COMPLETED",
    time: timestamp || new Date().toISOString(),
    ref: String(rewardName || "REWARD").slice(0, 8).toUpperCase(),
  };
}

function isValidPassword(password) {
  return new RegExp(`^(?=.*[^A-Za-z0-9]).{${PASSWORD_MIN_LENGTH},}$`).test(String(password || ""));
}

function mapPointsToTier(points) {
  if (points >= 300) {
    return { tier: "Gold", nextTier: "Platinum", pointsToNext: 0 };
  }
  if (points >= 100) {
    return { tier: "Silver", nextTier: "Gold", pointsToNext: 300 - points };
  }
  return { tier: "Basic", nextTier: "Silver", pointsToNext: REWARD_BASE_NEXT_THRESHOLD - points };
}

function resolveCachedProfilePreviewByRole(role) {
  const session = readSession();
  return readCachedProfilePreview(session, role);
}

function resolveWalletStateStorageKey(session, role) {
  const roleKey = role === "admin" ? "admin" : "applicant";
  const identity = String(session?.userId || session?.username || session?.email || "").trim().toLowerCase();
  if (!identity) {
    return "";
  }
  return `${WALLET_STATE_KEY_PREFIX}:${roleKey}:${identity}`;
}

function readWalletState(session, role) {
  const key = resolveWalletStateStorageKey(session, role);
  if (!key) {
    return null;
  }
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    return null;
  }
}

function writeWalletState(session, role, payload) {
  const key = resolveWalletStateStorageKey(session, role);
  if (!key) {
    return;
  }
  try {
    window.localStorage.setItem(key, JSON.stringify(payload));
  } catch (error) {
    // Ignore localStorage write failures.
  }
}

function HeroBand({ title, subtitle, description }) {
  return (
    <div className="hero-band">
      <div className="hero-band-content">
        <span className="brand-chip">{subtitle}</span>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
    </div>
  );
}

function SectionCard({ label, title, icon: Icon, children, style }) {
  return (
    <div className="section-card" style={style}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          {label && <span className="section-label">{label}</span>}
          {title && <h3>{title}</h3>}
        </div>
        {Icon && <div style={{ opacity: 0.6 }}><Icon size={24} /></div>}
      </div>
      {children}
    </div>
  );
}

function MetricCard({ label, value, detail }) {
  return (
    <div className="metric-card">
      <p className="mini-label" style={{ background: 'none', padding: 0 }}>{label}</p>
      <h3 style={{ fontSize: '1.5rem', margin: '8px 0' }}>{value}</h3>
      <p className="section-copy" style={{ fontSize: '0.8rem' }}>{detail}</p>
    </div>
  );
}

function Field({ label, value, onChange, type = "text", as = "input", options = [], placeholder, helperText, ...props }) {
  const [passwordVisible, setPasswordVisible] = useState(false);
  const isPasswordField = as === "input" && type === "password";
  const resolvedType = isPasswordField ? (passwordVisible ? "text" : "password") : type;

  return (
    <div className="field-group">
      {label && <label className="field-label">{label}</label>}
      <div className="field-input-wrapper">
        {as === "select" ? (
          <select className="field-input" value={value} onChange={onChange} {...props}>
            {options.map((opt) => (
              <option key={opt} value={opt}>{opt}</option>
            ))}
          </select>
        ) : as === "textarea" ? (
          <textarea className="field-input" value={value} onChange={onChange} placeholder={placeholder} {...props} />
        ) : (
          <input className={`field-input ${isPasswordField ? "is-password-input" : ""}`} type={resolvedType} {...(type !== "file" ? { value } : {})} onChange={onChange} placeholder={placeholder} {...props} />
        )}
        {isPasswordField && (
          <button
            type="button"
            className="field-password-toggle"
            onClick={() => setPasswordVisible((current) => !current)}
            aria-label={passwordVisible ? "Hide password" : "Show password"}
            title={passwordVisible ? "Hide password" : "Show password"}
          >
            {passwordVisible ? "Hide" : "Show"}
          </button>
        )}
      </div>
      {helperText ? <p className="wallet-helper-copy">{helperText}</p> : null}
    </div>
  );
}

function App() {
  const [workspaceRole, setWorkspaceRole] = useState("auth");
  const [authView, setAuthView] = useState("login");
  const [notice, setNotice] = useState("Sign in to continue or create a new account.");
  const [authPending, setAuthPending] = useState(false);
  const [activePage, setActivePage] = useState("dashboard");
  const [authForm, setAuthForm] = useState({
    identifier: "",
    password: "",
  });
  const [signupForm, setSignupForm] = useState({
    fullName: "",
    username: "",
    email: "",
    phone: "",
    password: "",
  });
  const [forgotForm, setForgotForm] = useState({
    identifier: "",
    otp: "",
    resetToken: "",
    newPassword: "",
    otpSent: false,
    otpVerified: false,
  });
  const [showResetTokenField, setShowResetTokenField] = useState(false);
  const [profile, setProfile] = useState({
    name: "",
    username: "",
    email: "",
    phone: "",
    role: "User",
    status: "Active",
    userId: "",
    city: "",
    kycDocument: "",
    profileImageUrl: "",
    previewUrl: "",
    photoFile: null,
  });
  const [adminProfile, setAdminProfile] = useState({
    name: "",
    username: "",
    email: "",
    phone: "",
    role: "Admin",
    userId: "",
    profileImageUrl: "",
    previewUrl: "",
    photoFile: null,
  });
  const [theme, setTheme] = useState("green");

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  useEffect(() => {
    return () => {
      if (profile.previewUrl?.startsWith("blob:")) {
        URL.revokeObjectURL(profile.previewUrl);
      }

      if (adminProfile.previewUrl?.startsWith("blob:")) {
        URL.revokeObjectURL(adminProfile.previewUrl);
      }
    };
  }, [profile.previewUrl, adminProfile.previewUrl]);

  useEffect(() => {
    const savedSession = readSession();
    if (!savedSession?.token) {
      return;
    }
    const workspace = savedSession.username
      ? mapUsernameToWorkspace(savedSession.username)
      : mapRoleToWorkspace(savedSession.role);
    const restoredProfile = {
      name: savedSession.fullName || savedSession.username || savedSession.email || "",
      username: savedSession.username || "",
      email: savedSession.email || "",
      phone: savedSession.phoneNumber || "",
      role: workspace === "admin" ? "Admin" : "User",
      userId: savedSession.userId || "",
      profileImageUrl: savedSession.profileImageUrl || "",
    };

    if (workspace === "admin") {
      setAdminProfile((current) => ({ ...current, ...restoredProfile }));
    } else {
      setProfile((current) => ({ ...current, ...restoredProfile }));
    }

    hydrateWalletStateFromSession(savedSession, workspace);
    setWorkspaceRole(workspace);
    setAuthView("login");
    setNotice("");
  }, []);
  const [walletBalance, setWalletBalance] = useState(0);
  const [ledgerBalance, setLedgerBalance] = useState(0);
  const [rewardState, setRewardState] = useState({
    points: 0,
    tier: "Basic",
    nextTier: "Silver",
    pointsToNext: REWARD_BASE_NEXT_THRESHOLD,
  });
  const [transactions, setTransactions] = useState([]);
  const [rewardsCatalog, setRewardsCatalog] = useState(initialRewardsCatalog);
  const [tickets, setTickets] = useState(initialTickets);
  const [notificationsFeed, setNotificationsFeed] = useState(initialNotificationsFeed);
  const [adminNotificationsFeed, setAdminNotificationsFeed] = useState(initialAdminNotificationsFeed);
  const [activityNotifications, setActivityNotifications] = useState([]);
  const [topupForm, setTopupForm] = useState({
    amount: "",
    method: "UPI",
    provider: "Razorpay",
  });
  const [topupStatus, setTopupStatus] = useState("Idle");
  const [transferForm, setTransferForm] = useState({
    recipient: "",
    amount: "",
    note: "",
  });
  const [transferStatus, setTransferStatus] = useState("Ready");
  const [rewardSelection, setRewardSelection] = useState("reward-2");
  const [redeemStatus, setRedeemStatus] = useState("Choose a reward to preview the redemption flow.");
  const [redeemPopup, setRedeemPopup] = useState(null);
  const [txFilter, setTxFilter] = useState({
    type: "ALL",
    status: "ALL",
    query: "",
  });
  const [disputeForm, setDisputeForm] = useState({
    reference: "LED-4811",
    message: "I need a cleaner statement line item for reimbursement.",
  });
  const [kycStatus, setKycStatus] = useState("NOT_SUBMITTED");
  const [kycRejectionReason, setKycRejectionReason] = useState("");
  const [kycForm, setKycForm] = useState({
    documentType: "Passport",
    documentId: "N5532107",
    documentUrl: "https://example.com/document",
    consent: true,
  });
  const [supportForm, setSupportForm] = useState({
    topic: "Statement request",
    message: "Please share a clean monthly statement summary for April.",
  });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [adminPasswordForm, setAdminPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [approvals, setApprovals] = useState(initialApprovals);
  const [approvalReviewState, setApprovalReviewState] = useState({});
  const [campaigns, setCampaigns] = useState(initialCampaigns);
  const [campaignForm, setCampaignForm] = useState({
    name: "",
    audience: "All",
    reward: "3x points",
  });
  const [catalogForm, setCatalogForm] = useState({
    name: "",
    description: "",
    cost: "",
    stock: "",
    tier: "All",
  });
  const [alerts, setAlerts] = useState(initialAlerts);
  const [adminSystemStatus, setAdminSystemStatus] = useState("Operational");
  const [isRefreshing, setIsRefreshing] = useState(false);
  const latestRewardNotificationIdRef = useRef("");

  const navItems = workspaceRole === "admin" ? adminNav : applicantNav;
  const selectedReward = rewardsCatalog.find((item) => item.id === rewardSelection) || rewardsCatalog[0];
  const pendingKycNotifications = approvals.map((item) => ({
    id: `pending-${item?.id || item?.userId || item?.email}`,
    title: "kyc.review.pending",
    body: `Pending KYC review for user ${item?.userId || item?.email || "unknown user"}`,
    channel: "IN_APP",
    time: item?.submittedAt || new Date().toISOString(),
    user: item?.userId || "",
  }));
  const applicantNotificationFeed = notificationsFeed;
  const adminNotificationFeed = [...pendingKycNotifications, ...adminNotificationsFeed];
  const filteredTransactions = transactions.filter((item) => {
    const matchesType = txFilter.type === "ALL" || item.type === txFilter.type;
    const matchesStatus = txFilter.status === "ALL" || item.status === txFilter.status;
    const haystack = `${item.title} ${item.counterparty} ${item.channel} ${item.ref}`.toLowerCase();
    const matchesQuery = haystack.includes(txFilter.query.toLowerCase());
    return matchesType && matchesStatus && matchesQuery;
  });
  const latestTransaction = transactions[0];
  const latestApplicantNotification = notificationsFeed[0];
  const applicantDisplayName = getDisplayName(profile);
  const adminDisplayName = getDisplayName(adminProfile);
  const accountStatusItems = [
    { label: "Username", value: profile.username || applicantDisplayName || "Not available" },
    { label: "KYC", value: formatKycStatus(kycStatus) },
    { label: "Wallet access", value: normalizeKycStatus(kycStatus) === "APPROVED" ? "Active" : "Limited" },
    { label: "Rewards", value: `${rewardState.points} points` },
    {
      label: "Latest transaction",
      value: latestTransaction
        ? `${latestTransaction.title} ${formatMoney(latestTransaction.amount)} at ${formatTime(latestTransaction.time)}`
        : "No recent transactions",
    },
    {
      label: "Latest notification",
      value: latestApplicantNotification
        ? `${latestApplicantNotification.title} at ${formatTime(latestApplicantNotification.time)}`
        : "No recent notifications",
    },
  ];
  const adminActionItems = [
    approvals.length > 0
      ? `Review ${approvals.length} pending KYC request(s).`
      : "No pending KYC requests right now.",
    campaigns.length > 0
      ? `${campaigns.length} campaign(s) currently active.`
      : "No active campaigns right now.",
    adminNotificationsFeed[0]
      ? `Latest admin update: ${adminNotificationsFeed[0].body}`
      : "No KYC notification activity yet.",
  ];

  useEffect(() => {
    setApprovalReviewState((current) => {
      const validKeys = new Set(
        approvals
          .map((item) => String(item?.id || item?.userId || item?.email || "").trim())
          .filter(Boolean)
      );
      const currentKeys = Object.keys(current);
      const next = {};

      for (const key of currentKeys) {
        if (validKeys.has(key)) {
          next[key] = Boolean(current[key]);
        }
      }

      if (Object.keys(next).length !== currentKeys.length) {
        return next;
      }

      return current;
    });
  }, [approvals]);

  function hydrateWalletStateFromSession(session, role) {
    const persisted = readWalletState(session, role);
    if (!persisted || typeof persisted !== "object") {
      setWalletBalance(0);
      setLedgerBalance(0);
      setRewardState({
        points: 0,
        tier: "Basic",
        nextTier: "Silver",
        pointsToNext: REWARD_BASE_NEXT_THRESHOLD,
      });
      setTransactions([]);
      setActivityNotifications([]);
      return;
    }
    if (typeof persisted.walletBalance === "number") {
      setWalletBalance(persisted.walletBalance);
    }
    if (typeof persisted.ledgerBalance === "number") {
      setLedgerBalance(persisted.ledgerBalance);
    }
    if (persisted.rewardState && typeof persisted.rewardState === "object") {
      setRewardState((current) => ({
        ...current,
        ...persisted.rewardState,
      }));
    }
    if (Array.isArray(persisted.transactions)) {
      setTransactions(persisted.transactions);
    }
    if (Array.isArray(persisted.activityNotifications)) {
      setActivityNotifications(persisted.activityNotifications);
    }
  }

  useEffect(() => {
    if (workspaceRole === "auth") {
      return;
    }
    const session = readSession();
    if (!session?.token) {
      return;
    }
    writeWalletState(session, workspaceRole, {
      walletBalance,
      ledgerBalance,
      rewardState,
      transactions,
      activityNotifications,
    });
  }, [workspaceRole, walletBalance, ledgerBalance, rewardState, transactions, activityNotifications]);

  async function loadProfile(session) {
    if (!session?.token) return;
    try {
      const data = await apiRequest("/users/profile", { token: session.token });
      const workspace = mapRoleToWorkspace(data.role);
      const cachedPreviewUrl = readCachedProfilePreview(session, workspace);
      if (workspace === "admin") {
        setAdminProfile((current) => ({
          ...current,
          name: data.fullName || current.name || session.fullName || session.username || data.email || "",
          username: current.username || session.username || "",
          email: data.email || current.email || session.email || "",
          phone: data.phoneNumber || current.phone || session.phoneNumber || "",
          role: workspace === "admin" ? "Admin" : "User",
          userId: resolveStableSessionUserId(session, data, current),
          profileImageUrl: data.profileImageUrl || current.profileImageUrl || "",
          previewUrl: data.profileImageUrl ? "" : cachedPreviewUrl,
          status: data.status || current.status || "Active",
        }));
      } else {
        setProfile((current) => ({
          ...current,
          name: data.fullName || current.name || session.fullName || session.username || data.email || "",
          username: current.username || session.username || "",
          email: data.email || current.email || session.email || "",
          phone: data.phoneNumber || current.phone || session.phoneNumber || "",
          role: workspace === "admin" ? "Admin" : "User",
          userId: resolveStableSessionUserId(session, data, current),
          profileImageUrl: data.profileImageUrl || current.profileImageUrl || "",
          previewUrl: data.profileImageUrl ? "" : cachedPreviewUrl,
          status: data.status || current.status || "Active",
        }));
      }

      writeSession({
        ...session,
        userId: resolveStableSessionUserId(session, data),
        fullName: data.fullName || session.fullName || session.username || "",
        phoneNumber: data.phoneNumber || session.phoneNumber || "",
        profileImageUrl: data.profileImageUrl || session.profileImageUrl || "",
      });
    } catch (error) {
      console.error("Failed to load profile:", error);
    }
  }

  async function applySavedProfileState(setter, updatedUser, previewUrlToClear, session, role, cachedPreviewDataUrl = "") {
    const nextProfileImageUrl = updatedUser?.profileImageUrl || "";
    const serverImageReady = previewUrlToClear && nextProfileImageUrl
      ? await waitForProfileImage(nextProfileImageUrl)
      : false;
    const nextPreviewUrl = nextProfileImageUrl ? "" : cachedPreviewDataUrl;

    setter((current) => {
      const nextState = {
        ...current,
        name: updatedUser?.fullName || current.name,
        phone: updatedUser?.phoneNumber || current.phone,
        profileImageUrl: nextProfileImageUrl || current.profileImageUrl,
        previewUrl: nextPreviewUrl || current.previewUrl,
        photoFile: null,
      };

      if (serverImageReady && current.previewUrl === previewUrlToClear) {
        nextState.previewUrl = "";
      }

      return nextState;
    });

    if (serverImageReady && previewUrlToClear?.startsWith("blob:")) {
      URL.revokeObjectURL(previewUrlToClear);
    }

    if (cachedPreviewDataUrl) {
      writeCachedProfilePreview(session, role, cachedPreviewDataUrl);
    }
  }

  async function loadApplicantKycStatus(session) {
    if (!session?.token) {
      setKycStatus("NOT_SUBMITTED");
      setKycRejectionReason("");
      return;
    }

    try {
      const kyc = await apiRequest("/users/kyc/status", {
        token: session.token,
      });
      setKycStatus(normalizeKycStatus(kyc?.status));
      setKycRejectionReason(kyc?.rejectionReason || "");
      setKycForm((current) => ({
        ...current,
        documentType: kyc?.documentType || current.documentType,
        documentId: kyc?.documentNumber || current.documentId,
        documentUrl: kyc?.documentUrl || current.documentUrl,
      }));
    } catch (error) {
      if (String(error?.message || "").includes("KYC details not found")) {
        setKycStatus("NOT_SUBMITTED");
        setKycRejectionReason("");
        return;
      }
      throw error;
    }
  }

  async function loadApplicantNotifications(session) {
    if (!session?.userId) {
      setNotificationsFeed([]);
      return;
    }

    const notifications = await apiRequest(`/notifications/${session.userId}`, {
      token: session.token,
    });
    const mappedNotifications = Array.isArray(notifications) ? notifications.map(mapNotificationRecord) : [];
    setNotificationsFeed(mappedNotifications);
    const latestRewardNotification = mappedNotifications.find(
      (item) => String(item.title || "").toLowerCase() === "reward.redeemed"
    );
    if (latestRewardNotification) {
      latestRewardNotificationIdRef.current = latestRewardNotification.id;
    }
  }

  async function loadApplicantFinancialData(session) {
    if (!session?.token || !session?.userId) {
      return;
    }

    const [walletResult, ledgerResult, rewardsResult, historyResult, catalogResult] = await Promise.allSettled([
      apiRequest("/wallet/balance", { token: session.token }),
      apiRequest(`/transactions/ledger-balance/${session.userId}`, { token: session.token }),
      apiRequest("/rewards/summary", { token: session.token }),
      apiRequest(`/transactions/history/${session.userId}?page=0&size=20`, { token: session.token }),
      apiRequest("/rewards/catalog", { token: session.token }),
    ]);

    const wallet = walletResult.status === "fulfilled" ? walletResult.value : null;
    const ledger = ledgerResult.status === "fulfilled" ? ledgerResult.value : null;
    const rewards = rewardsResult.status === "fulfilled" ? rewardsResult.value : null;
    const history = historyResult.status === "fulfilled" ? historyResult.value : null;
    const catalog = catalogResult.status === "fulfilled" ? catalogResult.value : null;

    if (!wallet && !rewards && !catalog) {
      throw new Error(
        walletResult.status === "rejected"
          ? walletResult.reason?.message || "Unable to load account balances right now."
          : rewardsResult.status === "rejected"
            ? rewardsResult.reason?.message || "Unable to load rewards right now."
            : catalogResult.reason?.message || "Unable to load rewards catalog right now."
      );
    }

    if (wallet) {
      const walletValue = toNumeric(wallet?.balance);
      setWalletBalance(walletValue);
      if (ledger?.ledgerBalance == null) {
        setLedgerBalance((current) => (current > 0 ? current : walletValue));
      }
    }

    if (ledger?.ledgerBalance != null) {
      const ledgerValue = toNumeric(ledger.ledgerBalance);
      setLedgerBalance(ledgerValue);
    }

    if (rewards) {
      const points = toNumeric(rewards?.totalPoints);
      const tierInfo = mapPointsToTier(points);
      setRewardState({
        points,
        tier: normalizeTierLabel(rewards?.tier) || tierInfo.tier,
        nextTier: tierInfo.nextTier,
        pointsToNext: tierInfo.pointsToNext,
      });
    }

    if (history?.content) {
      const mappedTx = Array.isArray(history.content)
        ? history.content.map((item) => mapTransactionRecord(item, session.userId))
        : [];
      setTransactions(mappedTx);
    }

    if (Array.isArray(catalog)) {
      const mappedCatalog = catalog.map(mapRewardCatalogItem);
      if (mappedCatalog.length > 0) {
        setRewardsCatalog(mappedCatalog);
        setRewardSelection((current) =>
          mappedCatalog.some((item) => item.id === current) ? current : mappedCatalog[0]?.id || current
        );
      }
    }
  }

  async function loadAdminApprovals(session) {
    if (!session?.token) {
      setApprovals([]);
      return;
    }

    const queue = await apiRequest("/admin/kyc/pending", {
      token: session.token,
    });
    setApprovals(Array.isArray(queue) ? queue : []);
  }

  async function loadAdminNotifications(session) {
    const notifications = await apiRequest("/notifications", {
      token: session?.token,
    });
    const mapped = Array.isArray(notifications) ? notifications.map(mapNotificationRecord) : [];
    setAdminNotificationsFeed(
      mapped.filter((item) => ["kyc.status.updated", "campaign.created", "reward.catalog.created"].includes(String(item.title || "").toLowerCase()))
    );
  }

  async function loadAdminDashboard(session) {
    if (!session?.token) {
      return;
    }
    const [dashboardMetrics, campaignList, catalog] = await Promise.all([
      apiRequest("/admin/dashboard", { token: session.token }),
      apiRequest("/admin/campaigns", { token: session.token }),
      apiRequest("/rewards/catalog", { token: session.token }),
    ]);
    setAdminSystemStatus(String(dashboardMetrics?.status || "Operational"));
    setCampaigns(Array.isArray(campaignList) ? campaignList : []);
    const mappedCatalog = Array.isArray(catalog) ? catalog.map(mapRewardCatalogItem) : [];
    if (mappedCatalog.length > 0) {
      setRewardsCatalog(mappedCatalog);
    }
  }

  async function refreshWorkspaceData(showErrorNotice = true) {
    const session = readSession();
    if (!session?.token) {
      return;
    }
    try {
      await loadProfile(session);
      if (workspaceRole === "applicant") {
        const [financialResult, kycResult, notificationsResult] = await Promise.allSettled([
          loadApplicantFinancialData(session),
          loadApplicantKycStatus(session),
          loadApplicantNotifications(session),
        ]);

        if (kycResult.status === "rejected") {
          console.warn("Unable to refresh KYC status:", kycResult.reason);
        }

        if (notificationsResult.status === "rejected") {
          console.warn("Unable to refresh applicant notifications:", notificationsResult.reason);
        }

        if (financialResult.status === "rejected") {
          throw financialResult.reason;
        }
      } else if (workspaceRole === "admin") {
        const [dashboardResult, approvalsResult, notificationsResult] = await Promise.allSettled([
          loadAdminDashboard(session),
          loadAdminApprovals(session),
          loadAdminNotifications(session),
        ]);

        if (approvalsResult.status === "rejected") {
          console.warn("Unable to refresh KYC approvals:", approvalsResult.reason);
        }

        if (notificationsResult.status === "rejected") {
          console.warn("Unable to refresh admin notifications:", notificationsResult.reason);
        }

        if (dashboardResult.status === "rejected") {
          throw dashboardResult.reason;
        }
      }
    } catch (error) {
      throw error;
    }
  }

  async function handleRefreshWorkspace() {
    setIsRefreshing(true);
    try {
      await refreshWorkspaceData(true);
      setNotice("Account data refreshed.");
    } catch (error) {
      setNotice(error.message || "Unable to refresh account data right now.");
    } finally {
      setIsRefreshing(false);
    }
  }

  useEffect(() => {
    const session = readSession();
    if (!session?.token) {
      return;
    }

    let cancelled = false;
    const loadWorkspaceData = async (showErrorNotice = true) => {
      try {
        await refreshWorkspaceData(showErrorNotice);
      } catch (error) {
        if (!cancelled && showErrorNotice) {
          setNotice(error.message || "Unable to load the latest workspace data.");
        }
      }
    };

    loadWorkspaceData();
    const intervalId = window.setInterval(() => {
      loadWorkspaceData(false);
    }, REALTIME_REFRESH_INTERVAL_MS);

    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [workspaceRole, activePage]);

  function updateAuth(field, value) {
    setAuthForm((current) => ({ ...current, [field]: value }));
  }

  function validateProfilePhotoFile(file) {
    if (!file) return "No file selected.";
    if (!String(file.type || "").toLowerCase().startsWith("image/")) {
      return "Please upload an image file only.";
    }
    if (file.size > PROFILE_IMAGE_MAX_SIZE_BYTES) {
      return "Image must be 5 MB or smaller.";
    }
    return "";
  }

  async function startProfilePhotoUpload(file, target) {
    const validationMessage = validateProfilePhotoFile(file);
    if (validationMessage) {
      setNotice(validationMessage);
      return;
    }

    let cachedPreviewDataUrl = "";
    try {
      cachedPreviewDataUrl = await fileToDataUrl(file);
    } catch (error) {
      setNotice(error.message || "Unable to prepare image preview.");
    }

    const session = readSession();
    if (cachedPreviewDataUrl) {
      writeCachedProfilePreview(session, target === "admin" ? "admin" : "applicant", cachedPreviewDataUrl);
    }

    if (target === "admin") {
      if (adminProfile.previewUrl?.startsWith("blob:")) {
        URL.revokeObjectURL(adminProfile.previewUrl);
      }
      const previewUrl = URL.createObjectURL(file);
      setAdminProfile((prev) => ({
        ...prev,
        previewUrl: cachedPreviewDataUrl || previewUrl,
        photoFile: file,
      }));
      handleAdminProfileSave(null, file, previewUrl, cachedPreviewDataUrl);
      return;
    }

    if (profile.previewUrl?.startsWith("blob:")) {
      URL.revokeObjectURL(profile.previewUrl);
    }
    const previewUrl = URL.createObjectURL(file);
    setProfile((prev) => ({
      ...prev,
      previewUrl: cachedPreviewDataUrl || previewUrl,
      photoFile: file,
    }));
    handleProfileSave(null, file, previewUrl, cachedPreviewDataUrl);
  }

  function resetForgotFlow(identifier = "") {
    setForgotForm({
      identifier,
      otp: "",
      resetToken: "",
      newPassword: "",
      otpSent: false,
      otpVerified: false,
    });
    setShowResetTokenField(false);
  }

  function handleProfileImageError(role) {
    const cachedPreview = resolveCachedProfilePreviewByRole(role);

    if (role === "admin") {
      setAdminProfile((current) => {
        if (cachedPreview && current.previewUrl !== cachedPreview) {
          return {
            ...current,
            previewUrl: cachedPreview,
            profileImageUrl: "",
          };
        }
        return {
          ...current,
          previewUrl: "",
          profileImageUrl: "",
        };
      });
      return;
    }

    setProfile((current) => {
      if (cachedPreview && current.previewUrl !== cachedPreview) {
        return {
          ...current,
          previewUrl: cachedPreview,
          profileImageUrl: "",
        };
      }
      return {
        ...current,
        previewUrl: "",
        profileImageUrl: "",
      };
    });
  }

  function enterWorkspace(role) {
    startTransition(() => {
      setWorkspaceRole(role);
      setActivePage("dashboard");
    });
  }

  function exitWorkspace() {
    clearSession();
    setWorkspaceRole("auth");
    setAuthView("login");
    setAuthForm({
      identifier: "",
      password: "",
    });
    resetForgotFlow();
    setNotice("You have been signed out.");
  }

  async function handleLogin(event) {
    event.preventDefault();

    const loginIdentifier = String(authForm.identifier || "").trim();
    if (!loginIdentifier || !authForm.password) {
      setNotice("Enter both username/email and password.");
      return;
    }

    setAuthPending(true);
    setNotice("Checking your credentials...");

    try {
      const response = await apiRequest("/auth/login", {
        method: "POST",
        body: {
          username: loginIdentifier,
          password: authForm.password,
        },
      });
      const workspace = response.username
        ? mapUsernameToWorkspace(response.username)
        : mapRoleToWorkspace(response.role);
      const resolvedProfile = {
        name: response.fullName || response.username || response.email || authForm.identifier,
        username: response.username || loginIdentifier,
        email: response.email || authForm.identifier,
        phone: response.phoneNumber || "",
        role: workspace === "admin" ? "Admin" : "User",
        userId: response.userId || "",
      };

      if (workspace === "admin") {
        setAdminProfile((current) => ({
          ...current,
          ...resolvedProfile,
        }));
      } else {
        setProfile((current) => ({
          ...current,
          ...resolvedProfile,
        }));
      }

      const sessionPayload = {
        token: response.token,
        refreshToken: response.refreshToken,
        userId: response.userId,
        username: response.username || loginIdentifier,
        email: response.email || authForm.identifier,
        role: workspace === "admin" ? "ADMIN" : "USER",
        fullName: response.fullName,
        phoneNumber: response.phoneNumber,
      };
      writeSession(sessionPayload);
      hydrateWalletStateFromSession(sessionPayload, workspace);
      setNotice(
        workspace === "admin"
          ? "Signed in successfully."
          : "Signed in successfully."
      );
      enterWorkspace(workspace);
    } catch (error) {
      const normalizedMessage = String(error?.message || "").trim();
      setNotice(
        normalizedMessage === "Invalid Authentication"
          ? "Invalid username/email or password."
          : normalizedMessage || "Unable to sign in right now."
      );
    } finally {
      setAuthPending(false);
    }
  }

  async function handleSignup(event) {
    event.preventDefault();

    if (!isValidPassword(signupForm.password)) {
      setNotice("Password must be at least 8 characters and include 1 special character.");
      return;
    }

    setAuthPending(true);
    setNotice("Creating your wallet account...");

    try {
      await apiRequest("/auth/signup", {
        method: "POST",
        body: {
          username: signupForm.username,
          email: signupForm.email,
          password: signupForm.password,
          role: mapUsernameToWorkspace(signupForm.username) === "admin" ? "ADMIN" : "USER",
        },
      });

      const signupWorkspace = mapUsernameToWorkspace(signupForm.username);

      const loginResponse = await apiRequest("/auth/login", {
        method: "POST",
        body: {
          username: signupForm.username,
          password: signupForm.password,
        },
      });

      const resolvedProfile = {
        name: signupForm.fullName || loginResponse.fullName || loginResponse.username || signupForm.username,
        username: loginResponse.username || signupForm.username,
        email: loginResponse.email || signupForm.email,
        phone: loginResponse.phoneNumber || signupForm.phone,
        role: signupWorkspace === "admin" ? "Admin" : "User",
        userId: loginResponse.userId || "",
      };

      if (signupWorkspace === "admin") {
        setAdminProfile((current) => ({
          ...current,
          ...resolvedProfile,
        }));
      } else {
        setProfile((current) => ({
          ...current,
          ...resolvedProfile,
        }));
      }

      const sessionPayload = {
        token: loginResponse.token,
        refreshToken: loginResponse.refreshToken,
        userId: loginResponse.userId,
        username: loginResponse.username || signupForm.username,
        email: loginResponse.email || signupForm.email,
        role: signupWorkspace === "admin" ? "ADMIN" : "USER",
        fullName: loginResponse.fullName || signupForm.fullName,
        phoneNumber: loginResponse.phoneNumber || signupForm.phone,
      };
      writeSession(sessionPayload);

      try {
        const hydratedProfile = await persistInitialProfileDetails(
          sessionPayload,
          resolvedProfile.name,
          resolvedProfile.phone
        );
        if (hydratedProfile) {
          if (signupWorkspace === "admin") {
            setAdminProfile((current) => ({
              ...current,
              name: hydratedProfile.fullName || current.name,
              phone: hydratedProfile.phoneNumber || current.phone,
              profileImageUrl: hydratedProfile.profileImageUrl || current.profileImageUrl,
            }));
          } else {
            setProfile((current) => ({
              ...current,
              name: hydratedProfile.fullName || current.name,
              phone: hydratedProfile.phoneNumber || current.phone,
              profileImageUrl: hydratedProfile.profileImageUrl || current.profileImageUrl,
            }));
          }
          writeSession({
            ...sessionPayload,
            fullName: hydratedProfile.fullName || sessionPayload.fullName,
            phoneNumber: hydratedProfile.phoneNumber || sessionPayload.phoneNumber,
            profileImageUrl: hydratedProfile.profileImageUrl || sessionPayload.profileImageUrl,
          });
        }
      } catch (profileError) {
        console.error("Unable to hydrate initial profile after signup:", profileError);
      }

      hydrateWalletStateFromSession(sessionPayload, signupWorkspace);
      setAuthForm({
        identifier: signupForm.email,
        password: "",
      });
      resetForgotFlow(signupForm.email);
      setNotice("Account created successfully.");
      enterWorkspace(signupWorkspace);
    } catch (error) {
      setNotice(error.message || "Unable to create the account right now.");
    } finally {
      setAuthPending(false);
    }
  }

  async function handleForgot(event) {
    event.preventDefault();

    const email = String(forgotForm.identifier || "").trim();
    if (!email) {
      setNotice("Enter your email first.");
      return;
    }

    setAuthPending(true);
    setNotice("Sending OTP...");

    try {
      await apiRequest("/auth/forgot-password", {
        method: "POST",
        body: {
          email,
        },
      });

      setForgotForm((current) => ({
        ...current,
        identifier: email,
        otp: "",
        resetToken: "",
        newPassword: "",
        otpSent: true,
        otpVerified: false,
      }));
      setShowResetTokenField(false);
      setNotice("OTP sent to your email.");
    } catch (error) {
      setNotice(error.message || "Unable to send OTP.");
    } finally {
      setAuthPending(false);
    }
  }

  async function handleVerifyOtp(event) {
    event.preventDefault();

    const email = String(forgotForm.identifier || "").trim();
    const otp = String(forgotForm.otp || "").trim();

    if (!email) {
      setNotice("Enter your email first.");
      return;
    }
    if (!otp) {
      setNotice("Enter the 6-digit OTP.");
      return;
    }

    setAuthPending(true);
    setNotice("Verifying OTP...");

    try {
      const message = await apiRequest("/auth/verify-otp", {
        method: "POST",
        body: {
          email,
          code: otp,
        },
      });
      setForgotForm((current) => ({
        ...current,
        otpVerified: true,
      }));
      setNotice(typeof message === "string" ? message : "OTP verified successfully.");
    } catch (error) {
      setNotice(error.message || "Invalid or expired OTP.");
    } finally {
      setAuthPending(false);
    }
  }

  async function handleResetPassword(event) {
    event.preventDefault();

    if (!forgotForm.otpVerified) {
      setNotice("Verify OTP before resetting the password.");
      return;
    }

    if (!isValidPassword(forgotForm.newPassword)) {
      setNotice("Password must be at least 8 characters and include 1 special character.");
      return;
    }

    setAuthPending(true);
    setNotice("Resetting password...");

    try {
      const message = await apiRequest("/auth/reset-password/otp", {
        method: "POST",
        body: {
          email: String(forgotForm.identifier || "").trim(),
          code: String(forgotForm.otp || "").trim(),
          newPassword: forgotForm.newPassword,
        },
      });
      const email = String(forgotForm.identifier || "").trim();
      setAuthForm({
        identifier: email,
        password: "",
      });
      resetForgotFlow(email);
      setAuthView("login");
      setNotice(typeof message === "string" ? message : "Password has been reset successfully.");
    } catch (error) {
      setNotice(error.message || "Unable to reset password.");
    } finally {
      setAuthPending(false);
    }
  }

  async function handleTopup(event) {
    event.preventDefault();
    const session = readSession();
    if (!session?.token || !session?.userId) {
      setNotice("Sign in again before topping up wallet.");
      return;
    }

    const amount = Number(topupForm.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      setNotice("Enter a valid top-up amount before continuing.");
      return;
    }

    try {
      const response = await apiRequest("/wallet/topup", {
        method: "POST",
        token: session.token,
        body: {
          userId: session.userId,
          amount,
          paymentMethod: topupForm.method,
        },
      });

      await Promise.all([
        loadApplicantFinancialData(session),
        loadApplicantNotifications(session),
      ]);
      setTopupStatus("Success");
      setNotice(typeof response === "string" ? response : `${formatMoney(amount)} added using ${topupForm.method}.`);
    } catch (error) {
      setTopupStatus("Blocked");
      setNotice(error.message || "Unable to complete top-up right now.");
    }
  }

  async function handleTransfer(event) {
    event.preventDefault();
    const session = readSession();
    if (!session?.token) {
      setNotice("Sign in again before transferring funds.");
      return;
    }

    const amount = Number(transferForm.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      setTransferStatus("Blocked");
      setNotice("Enter correct amount. The transfer amount must be greater than 0.");
      return;
    }
    const recipient = String(transferForm.recipient || "").trim();
    if (!recipient) {
      setTransferStatus("Blocked");
      setNotice("Enter correct user ID. Paste the recipient user ID from their Profile page.");
      return;
    }
    if (!isUuidLike(recipient)) {
      setTransferStatus("Blocked");
      setNotice("Enter correct user ID. It must look like 8-4-4-4-12 characters, for example 51c6b8d1-d779-442a-8bed-04c1cba550d6.");
      return;
    }

    try {
      const response = await apiRequest("/wallet/transfer", {
        method: "POST",
        token: session.token,
        body: {
          targetUserId: recipient,
          amount,
          notes: transferForm.note,
        },
      });

      await Promise.all([
        loadApplicantFinancialData(session),
        loadApplicantNotifications(session),
      ]);
      setTransferStatus("Success");
      setNotice(typeof response === "string" ? response : `Funds sent successfully. ${formatMoney(amount)} was sent to user ${recipient}.`);
    } catch (error) {
      setTransferStatus("Blocked");
      setNotice(getTransferNotice(error));
    }
  }

  async function handleRedeem(rewardId) {
    const session = readSession();
    if (!session?.token) {
      setNotice("Sign in again before redeeming rewards.");
      return;
    }

    let selected = rewardsCatalog.find((item) => item.id === rewardId) || selectedReward;
    if (!selected?.id) {
      return;
    }

    try {
      if (!isUuidLike(selected.id)) {
        const latestCatalog = await apiRequest("/rewards/catalog", { token: session.token });
        const mappedCatalog = Array.isArray(latestCatalog) ? latestCatalog.map(mapRewardCatalogItem) : [];
        if (mappedCatalog.length > 0) {
          setRewardsCatalog(mappedCatalog);
          const resolvedReward = mappedCatalog.find((item) =>
            isUuidLike(item.id) &&
            String(item.name || "").trim().toLowerCase() === String(selected?.name || "").trim().toLowerCase() &&
            toNumeric(item.cost) === toNumeric(selected?.cost)
          );

          if (resolvedReward) {
            selected = resolvedReward;
            setRewardSelection(resolvedReward.id);
          }
        }
      }

      if (!isUuidLike(selected.id)) {
        throw new Error("Rewards catalog is still syncing. Please refresh once and try redeeming again.");
      }

      const matchesSelectedReward = (item) =>
        item.id === selected.id ||
        (
          String(item.name || "").trim().toLowerCase() === String(selected.name || "").trim().toLowerCase() &&
          toNumeric(item.cost) === toNumeric(selected.cost)
        );

      const response = await apiRequest(`/rewards/redeem/${selected.id}`, {
        method: "POST",
        token: session.token,
      });

      const isCashbackReward = String(response?.rewardType || selected.rewardType || "").toUpperCase() === "CASHBACK";
      const cashbackAmount = toNumeric(response?.cashbackCredited);
      const hasRemainingPoints = Number.isFinite(Number(response?.remainingPoints));
      const hasRemainingStock = Number.isFinite(Number(response?.remainingStock));
      const selectedCost = toNumeric(selected.cost);
      const expectedRemainingPoints = selectedCost > 0
        ? Math.max(toNumeric(rewardState.points) - selectedCost, 0)
        : null;

      if (hasRemainingPoints) {
        const remainingPoints = toNumeric(response.remainingPoints);
        const tierInfo = mapPointsToTier(remainingPoints);
        setRewardState((current) => ({
          ...current,
          points: remainingPoints,
          tier: tierInfo.tier,
          nextTier: tierInfo.nextTier,
          pointsToNext: tierInfo.pointsToNext,
        }));
      } else if (selectedCost > 0) {
        setRewardState((current) => {
          const remainingPoints = Math.max(toNumeric(current.points) - selectedCost, 0);
          const tierInfo = mapPointsToTier(remainingPoints);
          return {
            ...current,
            points: remainingPoints,
            tier: tierInfo.tier,
            nextTier: tierInfo.nextTier,
            pointsToNext: tierInfo.pointsToNext,
          };
        });
      }

      if (hasRemainingStock) {
        const remainingStock = toNumeric(response.remainingStock);
        setRewardsCatalog((current) =>
          current.map((item) =>
            matchesSelectedReward(item)
              ? { ...item, stock: remainingStock }
              : item
          )
        );
      } else {
        setRewardsCatalog((current) =>
          current.map((item) =>
            matchesSelectedReward(item)
              ? { ...item, stock: Math.max(toNumeric(item.stock) - 1, 0) }
              : item
          )
        );
      }

      if (isCashbackReward) {
        if (response?.walletBalance != null) {
          const creditedBalance = toNumeric(response.walletBalance);
          setWalletBalance(creditedBalance);
        } else if (cashbackAmount > 0) {
          setWalletBalance((current) => current + cashbackAmount);
        }

        if (cashbackAmount > 0) {
          setLedgerBalance((current) => current + cashbackAmount);
          setTransactions((current) => [
            buildOptimisticRewardCashbackTransaction(
              response?.rewardName || selected.name,
              cashbackAmount,
              response?.redeemedAt
            ),
            ...current.filter((item) => item.title !== "Reward cashback" || item.amount !== cashbackAmount),
          ]);
        }
      }

      const [latestRewards] = await Promise.all([
        apiRequest("/rewards/summary", { token: session.token }).catch(() => null),
        loadApplicantNotifications(session).catch(() => null),
      ]);
      if (latestRewards) {
        const latestPoints = toNumeric(latestRewards?.totalPoints);
        const points = expectedRemainingPoints != null && latestPoints > expectedRemainingPoints
          ? expectedRemainingPoints
          : latestPoints;
        const tierInfo = mapPointsToTier(points);
        setRewardState({
          points,
          tier: normalizeTierLabel(latestRewards?.tier) || tierInfo.tier,
          nextTier: tierInfo.nextTier,
          pointsToNext: tierInfo.pointsToNext,
        });
      }
      window.setTimeout(() => {
        refreshWorkspaceData(false).catch(() => {});
      }, REDEEM_REFRESH_DELAY_MS);

      const rewardName = response?.rewardName || selected.name;
      const successMessage = isCashbackReward
        ? `Cashback redeemed successfully. ${cashbackAmount > 0 ? `${formatMoney(cashbackAmount)} has been added to your wallet.` : "Your wallet balance has been updated."}`
        : `Voucher redeemed successfully. ${rewardName} is now available in notifications.`;
      const message = response?.message || successMessage;
      setRedeemStatus(successMessage);
      setNotice(successMessage);
      setRedeemPopup({
        title: isCashbackReward
          ? "Cashback redeemed successfully"
          : "Voucher redeemed successfully",
        subtitle: "Reward redeemed",
        message: isCashbackReward
          ? successMessage
          : `${rewardName} voucher redeemed successfully. Check Notifications for the reward entry.`,
      });
    } catch (error) {
      setRedeemStatus(error.message || "Unable to redeem reward right now.");
      setNotice(error.message || "Unable to redeem reward right now.");
    }
  }

  async function handleKycSubmit(event) {
    event.preventDefault();
    const session = readSession();
    if (!session?.token) {
      setNotice("Sign in again before submitting KYC.");
      return;
    }

    if (!kycForm.documentFile) {
      setNotice("Please select a document file (.pdf or .docx) to upload.");
      return;
    }

    const filename = kycForm.documentFile.name.toLowerCase();
    if (!filename.endsWith(".pdf") && !filename.endsWith(".docx")) {
      setNotice("Only .pdf and .docx files are accepted for KYC.");
      return;
    }

    try {
      const formData = new FormData();
      formData.append("documentType", kycForm.documentType);
      formData.append("documentNumber", kycForm.documentId);
      formData.append("file", kycForm.documentFile);

      const response = await apiRequest("/users/kyc", {
        method: "POST",
        token: session.token,
        body: formData,
      });
      setKycStatus("PENDING");
      setKycRejectionReason("");
      setNotice(typeof response === "string" ? response : `${kycForm.documentType} details submitted for review.`);
      await loadApplicantKycStatus(session);
    } catch (error) {
      setNotice(error.message || "Unable to submit KYC right now.");
    }
  }

  async function handleProfileSave(event, directFile, previewUrlOverride, cachedPreviewDataUrl = "") {
    if (event && event.preventDefault) event.preventDefault();
    const session = readSession();
    if (!session?.token) {
      setNotice("Sign in again before saving profile.");
      return;
    }

    try {
      const formData = new FormData();
      formData.append("fullName", profile.name || "");
      formData.append("phoneNumber", profile.phone || "");

      const photoToUpload = directFile || profile.photoFile;
      const previewUrlToClear = previewUrlOverride ?? profile.previewUrl;
      if (photoToUpload) {
        formData.append("photo", photoToUpload);
      }

      const updatedUser = await apiRequest("/users/profile", {
        method: "POST",
        token: session.token,
        body: formData,
      });

      if (updatedUser && typeof updatedUser === "object") {
        await applySavedProfileState(
          setProfile,
          updatedUser,
          previewUrlToClear,
          session,
          "applicant",
          cachedPreviewDataUrl
        );

        // Update session
        const newSession = {
          ...session,
          fullName: updatedUser.fullName || session.fullName,
          phoneNumber: updatedUser.phoneNumber || session.phoneNumber,
          profileImageUrl: updatedUser.profileImageUrl || session.profileImageUrl,
        };
        writeSession(newSession);
        setNotice("Profile details updated successfully.");
      } else {
        setNotice("Profile updated, but server response was unexpected.");
      }
    } catch (error) {
      if (previewUrlOverride?.startsWith("blob:")) {
        URL.revokeObjectURL(previewUrlOverride);
      }
      setProfile((current) => ({
        ...current,
        previewUrl: cachedPreviewDataUrl || current.previewUrl,
        photoFile: null,
      }));
      setNotice(error.message || "Unable to save profile right now.");
    }
  }

  async function handlePasswordUpdate(event) {
    event.preventDefault();
    if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
      setNotice("Fill in all password fields before continuing.");
      return;
    }
    if (!isValidPassword(passwordForm.newPassword)) {
      setNotice("Password must be at least 8 characters and include 1 special character.");
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setNotice("New password and confirm password must match.");
      return;
    }

    const session = readSession();
    if (!session?.token) {
      setNotice("Sign in again before changing password.");
      return;
    }

    try {
      const response = await apiRequest("/auth/change-password", {
        method: "POST",
        token: session.token,
        body: {
          oldPassword: passwordForm.currentPassword,
          newPassword: passwordForm.newPassword,
        },
      });
      setPasswordForm({
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      });
      setNotice(typeof response === "string" ? response : "Password updated successfully.");
    } catch (error) {
      setNotice(error.message || "Unable to update password right now.");
    }
  }

  function handleSupportSubmit(event) {
    event.preventDefault();
    setTickets((current) => [
      {
        id: `SUP-${100 + current.length + 1}`,
        topic: supportForm.topic,
        status: "Open",
        owner: "Support Desk",
        updatedAt: new Date().toISOString(),
      },
      ...current,
    ]);
    setNotice(`Support request raised for "${supportForm.topic}".`);
  }

  function handleDispute(event) {
    event.preventDefault();
    setTickets((current) => [
      {
        id: `SUP-${100 + current.length + 1}`,
        topic: `Dispute ${disputeForm.reference}`,
        status: "Open",
        owner: "Payments Desk",
        updatedAt: new Date().toISOString(),
      },
      ...current,
    ]);
    setNotice(`Dispute created for ${disputeForm.reference}.`);
  }

  async function handleApproval(userId, outcome, isReviewed = false, reviewKey = "") {
    if (!isReviewed) {
      setNotice("Please open and review the document, then tick 'Document reviewed' before approving or rejecting.");
      return;
    }

    const session = readSession();
    if (!session?.token) {
      setNotice("Sign in again before processing the KYC queue.");
      return;
    }

    const isApproval = outcome === "APPROVED";
    let reason = "";

    if (!isApproval) {
      reason = window.prompt("Enter the rejection reason for this KYC request:", "")?.trim() || "";
      if (!reason) {
        setNotice("Rejection cancelled because no reason was entered.");
        return;
      }
    }

    try {
      const path = isApproval
        ? `/admin/kyc/${userId}/approve`
        : `/admin/kyc/${userId}/reject?reason=${encodeURIComponent(reason)}`;
      const response = await apiRequest(path, {
        method: "POST",
        token: session.token,
      });

      await Promise.all([
        loadAdminDashboard(session),
        loadAdminApprovals(session),
        loadAdminNotifications(session),
      ]);
      if (reviewKey) {
        setApprovalReviewState((current) => {
          if (!current[reviewKey]) {
            return current;
          }
          const next = { ...current };
          delete next[reviewKey];
          return next;
        });
      }
      setNotice(typeof response === "string" ? response : `KYC ${outcome.toLowerCase()} successfully.`);
    } catch (error) {
      setNotice(error.message || "Unable to update the KYC request right now.");
    }
  }

  async function handleCampaign(event) {
    event.preventDefault();
    const session = readSession();
    if (!session?.token) {
      setNotice("Sign in again before creating a campaign.");
      return;
    }
    if (!campaignForm.name.trim()) {
      setNotice("Enter a campaign name before creating the campaign.");
      return;
    }

    try {
      await apiRequest("/admin/campaigns", {
        method: "POST",
        token: session.token,
        body: {
          name: campaignForm.name,
          audience: campaignForm.audience,
          reward: campaignForm.reward,
        },
      });
      await loadAdminDashboard(session);
      setCampaignForm({
        name: "",
        audience: "All",
        reward: "3x points",
      });
      setNotice(`Campaign created for ${campaignForm.name}.`);
    } catch (error) {
      setNotice(error.message || "Unable to create campaign right now.");
    }
  }

  async function handleCatalogCreate(event) {
    event.preventDefault();
    const session = readSession();
    if (!session?.token) {
      setNotice("Sign in again before creating reward catalog items.");
      return;
    }
    if (!catalogForm.name.trim()) {
      setNotice("Enter a reward item name before creating the catalog item.");
      return;
    }

    const pointsCost = Number(catalogForm.cost);
    const stockQuantity = Number(catalogForm.stock);
    if (!Number.isFinite(pointsCost) || pointsCost <= 0) {
      setNotice("Cost in points must be a positive number.");
      return;
    }
    if (!Number.isFinite(stockQuantity) || stockQuantity < 0) {
      setNotice("Stock quantity must be zero or greater.");
      return;
    }

    const itemName = catalogForm.name.trim();
    const itemDescription = catalogForm.description.trim();
    const inferredCashback = /cashback/i.test(`${itemName} ${itemDescription}`);
    const cashbackMatch = `${itemName} ${itemDescription}`.match(/(\d+(?:\.\d+)?)/);

    try {
      await apiRequest("/rewards/catalog", {
        method: "POST",
        token: session.token,
        body: {
          name: itemName,
          description: itemDescription || "Reward item",
          costInPoints: pointsCost,
          stockQuantity,
          requiredTier: String(catalogForm.tier || "ALL").toUpperCase(),
          rewardType: inferredCashback ? "CASHBACK" : "VOUCHER",
          cashbackAmount: inferredCashback && cashbackMatch ? Number(cashbackMatch[1]) : null,
        },
      });
      await loadAdminDashboard(session);
      setNotice(`Catalog item created for ${catalogForm.name}.`);
      setCatalogForm({
        name: "",
        description: "",
        cost: "",
        stock: "",
        tier: "All",
      });
    } catch (error) {
      setNotice(error.message || "Unable to create reward catalog item right now.");
    }
  }

  async function handleAdminProfileSave(event, directFile, previewUrlOverride, cachedPreviewDataUrl = "") {
    if (event && event.preventDefault) event.preventDefault();
    const session = readSession();
    if (!session?.token) {
      setNotice("Sign in again before saving profile.");
      return;
    }

    try {
      const formData = new FormData();
      formData.append("fullName", adminProfile.name || "");
      formData.append("phoneNumber", adminProfile.phone || "");

      const photoToUpload = directFile || adminProfile.photoFile;
      const previewUrlToClear = previewUrlOverride ?? adminProfile.previewUrl;
      if (photoToUpload) {
        formData.append("photo", photoToUpload);
      }

      const updatedUser = await apiRequest("/users/profile", {
        method: "POST",
        token: session.token,
        body: formData,
      });

      if (updatedUser && typeof updatedUser === "object") {
        await applySavedProfileState(
          setAdminProfile,
          updatedUser,
          previewUrlToClear,
          session,
          "admin",
          cachedPreviewDataUrl
        );

        const newSession = {
          ...session,
          fullName: updatedUser.fullName || session.fullName,
          phoneNumber: updatedUser.phoneNumber || session.phoneNumber,
          profileImageUrl: updatedUser.profileImageUrl || session.profileImageUrl,
        };
        writeSession(newSession);
        setNotice("Admin profile updated successfully.");
      }
    } catch (error) {
      if (previewUrlOverride?.startsWith("blob:")) {
        URL.revokeObjectURL(previewUrlOverride);
      }
      setAdminProfile((current) => ({
        ...current,
        previewUrl: cachedPreviewDataUrl || current.previewUrl,
        photoFile: null,
      }));
      setNotice(error.message || "Unable to save admin profile right now.");
    }
  }

  async function handleAdminPasswordUpdate(event) {
    event.preventDefault();
    if (!adminPasswordForm.currentPassword || !adminPasswordForm.newPassword || !adminPasswordForm.confirmPassword) {
      setNotice("Fill in all admin password fields before continuing.");
      return;
    }
    if (!isValidPassword(adminPasswordForm.newPassword)) {
      setNotice("Password must be at least 8 characters and include 1 special character.");
      return;
    }
    if (adminPasswordForm.newPassword !== adminPasswordForm.confirmPassword) {
      setNotice("Admin confirm password must match the new password.");
      return;
    }

    const session = readSession();
    if (!session?.token) {
      setNotice("Sign in again before changing password.");
      return;
    }

    try {
      const response = await apiRequest("/auth/change-password", {
        method: "POST",
        token: session.token,
        body: {
          oldPassword: adminPasswordForm.currentPassword,
          newPassword: adminPasswordForm.newPassword,
        },
      });
      setAdminPasswordForm({
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      });
      setNotice(typeof response === "string" ? response : "Password updated successfully.");
    } catch (error) {
      setNotice(error.message || "Unable to update password right now.");
    }
  }

  if (workspaceRole === "auth") {
    return (
      <div className="auth-screen wallet-auth-screen">
        <section className="auth-hero-panel wallet-auth-hero">
          <span className="wallet-auth-chip">DIGITAL WALLET</span>
          <h1>Your money, rewards, and account checks in one secure place.</h1>
          <p>
            Sign in to review balances, send funds, track rewards, and keep your personal details ready for every payment moment.
          </p>
          <div className="wallet-auth-balance-card">
            <div className="wallet-auth-balance-head">
              <span>EVERYDAY BALANCE</span>
              <span className="wallet-auth-protected-pill">Protected</span>
            </div>
            <h3>Ready to spend</h3>
            <div className="wallet-auth-balance-bars" aria-hidden="true">
              <span />
              <span />
              <span />
              <span />
              <span />
              <span />
              <span />
            </div>
            <div className="wallet-auth-mini-grid">
              <article className="wallet-auth-mini-card">
                <strong>Identity</strong>
                <span>Kept current</span>
              </article>
              <article className="wallet-auth-mini-card">
                <strong>Rewards</strong>
                <span>Points visible</span>
              </article>
              <article className="wallet-auth-mini-card">
                <strong>Transfers</strong>
                <span>Clear history</span>
              </article>
            </div>
          </div>
        </section>

        <section className="auth-card wallet-auth-card">
          {authView !== "forgot" && (
            <div className="wallet-auth-tabs">
              <button
                type="button"
                className={`wallet-auth-tab ${authView === "login" ? "active" : ""}`}
                onClick={() => {
                  setAuthView("login");
                  resetForgotFlow(authForm.identifier);
                }}
              >
                Sign In
              </button>
              <button
                type="button"
                className={`wallet-auth-tab ${authView === "signup" ? "active" : ""}`}
                onClick={() => {
                  setAuthView("signup");
                  resetForgotFlow(signupForm.email);
                }}
              >
                Create Account
              </button>
            </div>
          )}
          {authView === "login" && (
            <>
              <div className="auth-card-head wallet-auth-head">
                <h2>Access your workspace</h2>
                <p>Sign in to manage your wallet, rewards, and KYC details.</p>
              </div>
              <form className="field-grid wallet-auth-form" onSubmit={handleLogin}>
                <Field
                  label="USERNAME OR EMAIL"
                  value={authForm.identifier}
                  onChange={(event) => updateAuth("identifier", event.target.value)}
                  placeholder="Enter username or email"
                />
                <Field
                  label="PASSWORD"
                  type="password"
                  value={authForm.password}
                  onChange={(event) => updateAuth("password", event.target.value)}
                  placeholder="Enter your password"
                />
                <div className="wallet-auth-link-row">
                  <button
                    type="button"
                    className="wallet-auth-link"
                    onClick={() => {
                      setAuthView("forgot");
                      resetForgotFlow(authForm.identifier);
                    }}
                  >
                    Forgot password?
                  </button>
                </div>
                <button type="submit" className="app-button primary wallet-auth-submit" disabled={authPending}>
                  {authPending ? "Signing In..." : "Sign In"}
                </button>
              </form>
            </>
          )}

          {authView === "signup" && (
            <>
              <div className="auth-card-head wallet-auth-head">
                <h2>Open a new wallet account</h2>
                <p>Create your account to start using wallet and rewards.</p>
              </div>
              <form className="field-grid wallet-auth-form" onSubmit={handleSignup}>
                <div className="field-row two-up">
                  <Field
                    label="FULL NAME"
                    value={signupForm.fullName}
                    onChange={(event) => setSignupForm((current) => ({ ...current, fullName: event.target.value }))}
                    placeholder="Enter full name"
                  />
                  <Field
                    label="USERNAME"
                    value={signupForm.username}
                    onChange={(event) => setSignupForm((current) => ({ ...current, username: event.target.value }))}
                    placeholder="Choose username"
                  />
                </div>
                <div className="field-row two-up">
                  <Field
                    label="EMAIL"
                    value={signupForm.email}
                    onChange={(event) => setSignupForm((current) => ({ ...current, email: event.target.value }))}
                    placeholder="Enter email"
                  />
                  <Field
                    label="PHONE NUMBER"
                    value={signupForm.phone}
                    onChange={(event) => setSignupForm((current) => ({ ...current, phone: event.target.value }))}
                    placeholder="Enter phone number"
                  />
                </div>
                <Field
                  label="PASSWORD"
                  type="password"
                  value={signupForm.password}
                  onChange={(event) => setSignupForm((current) => ({ ...current, password: event.target.value }))}
                  placeholder={PASSWORD_HINT}
                  helperText={PASSWORD_HELPER_TEXT}
                  minLength={PASSWORD_MIN_LENGTH}
                  pattern={PASSWORD_PATTERN}
                  title={PASSWORD_HELPER_TEXT}
                />
                <button type="submit" className="app-button primary wallet-auth-submit" disabled={authPending}>
                  {authPending ? "Creating Account..." : "Create Account"}
                </button>
              </form>
            </>
          )}

          {authView === "forgot" && (
            <>
              <div className="auth-card-head wallet-auth-head" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <h2>Reset your password</h2>
                  <p>Enter your email, then use the OTP from your inbox to choose a new password.</p>
                </div>
                <button
                  type="button"
                  className="app-button secondary"
                  style={{ borderRadius: '999px' }}
                  onClick={() => {
                    const email = String(forgotForm.identifier || authForm.identifier || "").trim();
                    resetForgotFlow(email);
                    setAuthForm((current) => ({
                      ...current,
                      identifier: email || current.identifier,
                    }));
                    setAuthView("login");
                  }}
                >
                  Back
                </button>
              </div>
              {!forgotForm.otpSent && (
                <form className="field-grid wallet-auth-form" onSubmit={handleForgot}>
                  <Field
                    label="EMAIL"
                    value={forgotForm.identifier}
                    onChange={(event) => setForgotForm((current) => ({ ...current, identifier: event.target.value }))}
                    placeholder="Enter your email"
                  />
                  <button type="submit" className="app-button primary wallet-auth-submit" disabled={authPending}>
                    {authPending ? "Sending OTP..." : "Send OTP"}
                  </button>
                </form>
              )}

              {forgotForm.otpSent && !forgotForm.otpVerified && (
                <form className="field-grid wallet-auth-form" onSubmit={handleVerifyOtp}>
                  <Field
                    label="EMAIL"
                    value={forgotForm.identifier}
                    onChange={(event) => setForgotForm((current) => ({ ...current, identifier: event.target.value }))}
                    placeholder="Enter your email"
                    readOnly
                  />
                  <Field
                    label="OTP"
                    value={forgotForm.otp}
                    onChange={(event) => setForgotForm((current) => ({ ...current, otp: event.target.value }))}
                    placeholder="Enter the 6-digit OTP"
                  />
                  <button type="submit" className="app-button primary wallet-auth-submit" disabled={authPending}>
                    {authPending ? "Verifying OTP..." : "Verify OTP"}
                  </button>
                </form>
              )}

              {forgotForm.otpSent && forgotForm.otpVerified && (
                <form className="field-grid wallet-auth-form" onSubmit={handleResetPassword}>
                  <Field
                    label="EMAIL"
                    value={forgotForm.identifier}
                    onChange={(event) => setForgotForm((current) => ({ ...current, identifier: event.target.value }))}
                    placeholder="Enter your email"
                    readOnly
                  />
                  <Field
                    label="OTP"
                    value={forgotForm.otp}
                    onChange={(event) => setForgotForm((current) => ({ ...current, otp: event.target.value }))}
                    placeholder="Enter the 6-digit OTP"
                    readOnly
                  />
                  <Field
                    label="NEW PASSWORD"
                    type="password"
                    value={forgotForm.newPassword}
                    onChange={(event) => setForgotForm((current) => ({ ...current, newPassword: event.target.value }))}
                    placeholder={PASSWORD_HINT}
                    helperText={PASSWORD_HELPER_TEXT}
                    minLength={PASSWORD_MIN_LENGTH}
                    pattern={PASSWORD_PATTERN}
                    title={PASSWORD_HELPER_TEXT}
                  />
                  <div className="wallet-auth-link-row" style={{ justifyContent: "flex-start" }}>
                    <button
                      type="button"
                      className="wallet-auth-link"
                      onClick={() => setShowResetTokenField((current) => !current)}
                    >
                      {showResetTokenField ? "Hide token field" : "Use reset token (API compatibility)"}
                    </button>
                  </div>
                  {showResetTokenField && (
                    <Field
                      label="RESET TOKEN"
                      value={forgotForm.resetToken}
                      onChange={(event) => setForgotForm((current) => ({ ...current, resetToken: event.target.value }))}
                      placeholder="Paste reset token if your API requires it"
                      helperText="If left empty, OTP will be used as token."
                    />
                  )}
                  <button type="submit" className="app-button primary wallet-auth-submit" disabled={authPending}>
                    {authPending ? "Resetting..." : "Reset Password"}
                  </button>
                </form>
              )}
            </>
          )}

          <div className="wallet-auth-notice">{notice}</div>
        </section>
      </div>
    );
  }

  if (workspaceRole === "applicant") {
    return (
      <div className="applicant-shell">
        <aside className="applicant-sidebar">
          <div className="applicant-brand">
            <div className="applicant-brand-mark">DW</div>
            <div>
              <strong>Digital Wallet</strong>
              <span>Personal account</span>
            </div>
          </div>

          <div className="applicant-profile-tile">
            <div className="applicant-profile-photo">
              {profile.previewUrl ? (
                <img src={profile.previewUrl} alt="Preview" onError={() => handleProfileImageError("applicant")} style={{ width: "100%", height: "100%", borderRadius: "16px", objectFit: "cover" }} />
              ) : profile.profileImageUrl ? (
                <img src={getProfileImageSrc(profile.profileImageUrl)} alt="Avatar" onError={() => handleProfileImageError("applicant")} style={{ width: "100%", height: "100%", borderRadius: "16px", objectFit: "cover" }} />
              ) : (
                <span>{getInitials(applicantDisplayName)}</span>
              )}
            </div>
            <div className="applicant-profile-copy">
              <strong>{applicantDisplayName || "User"}</strong>
              <span>{profile.email || "No email linked"}</span>
            </div>
          </div>

          <nav className="applicant-nav">
            {applicantNav.map((item) => {
              const active = activePage === item.key;
              return (
                <button
                  key={item.key}
                  type="button"
                  className={`applicant-nav-item ${active ? "active" : ""}`}
                  onClick={() => startTransition(() => setActivePage(item.key))}
                >
                  {item.label}
                </button>
              );
            })}
          </nav>

          <div className="applicant-theme-grid">
            <button className={`applicant-theme-btn ${theme === "white" ? "active" : ""}`} onClick={() => setTheme("white")}>White</button>
            <button className={`applicant-theme-btn ${theme === "black" ? "active" : ""}`} onClick={() => setTheme("black")}>Black</button>
            <button className={`applicant-theme-btn ${theme === "green" ? "active" : ""}`} onClick={() => setTheme("green")}>Teal</button>
            <button className={`applicant-theme-btn ${theme === "royal" ? "active" : ""}`} onClick={() => setTheme("royal")}>Royal</button>
          </div>

          <div className="applicant-side-actions">
            <button className="applicant-side-btn" onClick={exitWorkspace}>Sign Out</button>
          </div>
        </aside>

        <main className="applicant-main">
          <header className="applicant-hero">
            <div>
              <span className="applicant-hero-kicker">PERSONAL MONEY CENTER</span>
              <h1>Welcome back, {applicantDisplayName || "User"}</h1>
              <p>Review your balance, send money, follow rewards, and keep your identity documents ready for uninterrupted wallet use.</p>
            </div>
            <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: "10px" }}>
              <button className="theme-btn" onClick={handleRefreshWorkspace} disabled={isRefreshing}>
                {isRefreshing ? "Refreshing..." : "Refresh Account"}
              </button>
              <div className="applicant-hero-badge">
              <strong>DIGITAL WALLET SYSTEM</strong>
              </div>
            </div>
          </header>

          {notice && (
            <div className={`applicant-notice ${/success|refreshed|created|updated|sent|redeemed|submitted|approved/i.test(notice) ? "success" : "warning"}`}>
              {notice}
            </div>
          )}

          <section className="applicant-content">
            {renderApplicantPage({
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
              notificationsFeed: applicantNotificationFeed,
              accountStatusItems,
            })}
          </section>
          {redeemPopup && (
            <div className="redeem-popup-overlay" role="dialog" aria-modal="true">
              <div className="redeem-popup-card">
                <span className="redeem-popup-kicker">{redeemPopup.subtitle}</span>
                <h3>{redeemPopup.title}</h3>
                <p>{redeemPopup.message}</p>
                <button className="app-button primary wallet-inline-submit" onClick={() => setRedeemPopup(null)}>
                  Awesome
                </button>
              </div>
            </div>
          )}
        </main>
      </div>
    );
  }

  if (workspaceRole === "admin") {
    return (
      <div className="adminlite-shell">
        <aside className="adminlite-sidebar">
          <div className="adminlite-brand">
            <div className="adminlite-brand-mark">DW</div>
            <div>
              <strong>Digital Wallet</strong>
              <span>Operations desk</span>
            </div>
          </div>

          <div className="adminlite-profile-tile">
            <div className="adminlite-profile-photo">
              {adminProfile.previewUrl ? (
                <img src={adminProfile.previewUrl} alt="Preview" onError={() => handleProfileImageError("admin")} style={{ width: "100%", height: "100%", borderRadius: "16px", objectFit: "cover" }} />
              ) : adminProfile.profileImageUrl ? (
                <img src={getProfileImageSrc(adminProfile.profileImageUrl)} alt="Avatar" onError={() => handleProfileImageError("admin")} style={{ width: "100%", height: "100%", borderRadius: "16px", objectFit: "cover" }} />
              ) : (
                <span>{getInitials(adminDisplayName)}</span>
              )}
            </div>
            <div className="adminlite-profile-copy">
              <strong>{adminDisplayName || "Admin"}</strong>
              <span>Admin account</span>
            </div>
          </div>

          <nav className="adminlite-nav">
            {adminNav.map((item) => {
              const active = activePage === item.key;
              return (
                <button
                  key={item.key}
                  type="button"
                  className={`adminlite-nav-item ${active ? "active" : ""}`}
                  onClick={() => startTransition(() => setActivePage(item.key))}
                >
                  {item.label}
                </button>
              );
            })}
          </nav>

          <div className="adminlite-theme-grid">
            <button className={`adminlite-theme-btn ${theme === "white" ? "active" : ""}`} onClick={() => setTheme("white")}>White</button>
            <button className={`adminlite-theme-btn ${theme === "black" ? "active" : ""}`} onClick={() => setTheme("black")}>Black</button>
            <button className={`adminlite-theme-btn ${theme === "green" ? "active" : ""}`} onClick={() => setTheme("green")}>Teal</button>
            <button className={`adminlite-theme-btn ${theme === "royal" ? "active" : ""}`} onClick={() => setTheme("royal")}>Royal</button>
          </div>

          <div className="adminlite-side-actions">
            <button className="adminlite-side-btn" onClick={exitWorkspace}>Sign Out</button>
          </div>
        </aside>

        <main className="adminlite-main">
          <header className="adminlite-hero">
            <div>
              <span className="adminlite-hero-kicker">OPERATIONS COMMAND CENTER</span>
              <h1>Review customers, campaigns, and rewards with confidence.</h1>
              <p>Keep identity checks moving, maintain reward offers, and watch the items that need attention today.</p>
            </div>
            <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: "10px" }}>
              <button className="theme-btn" onClick={handleRefreshWorkspace} disabled={isRefreshing}>
                {isRefreshing ? "Refreshing..." : "Refresh Dashboard"}
              </button>
              <div className="adminlite-hero-badge">
              <strong>DIGITAL WALLET SYSTEM</strong>
              </div>
            </div>
          </header>

          <section className="adminlite-content">
            {renderAdminWorkspace({
              activePage,
              campaigns,
              approvals,
              approvalReviewState,
              setApprovalReviewState,
              rewardsCatalog,
              adminNotificationsFeed: adminNotificationFeed,
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
            })}
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <header className="top-banner">
        <div className="top-banner-content">
          {workspaceRole === "admin" ? (
            <>
              <span className="brand-chip">ADMIN OPERATIONS</span>
              <h1>Manage KYC approvals, campaigns, and rewards</h1>
              <p>Review verification requests, create campaigns, and manage rewards from one place.</p>
            </>
          ) : (
            <>
              <span className="brand-chip">CUSTOMER WORKSPACE</span>
              <h1>Welcome back, {applicantDisplayName || "User"}</h1>
              <p>Check your balance, send money, track rewards, and complete KYC in one place.</p>
              <div style={{ display: "flex", gap: "12px", marginTop: "12px" }}>
                <span className="pill" style={{ background: "rgba(79, 209, 197, 0.1)", color: "#4fd1c5", border: "1px solid rgba(79, 209, 197, 0.3)" }}>WALLET + REWARDS</span>
                <span className="pill" style={{ background: "rgba(79, 209, 197, 0.1)", color: "#4fd1c5", border: "1px solid rgba(79, 209, 197, 0.3)" }}>SECURE ACCESS</span>
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
            <div className="profile-avatar" style={{ width: '40px', height: '40px', fontSize: '0.8rem', flexShrink: 0 }}>
              {workspaceRole === "admin" ? (
                adminProfile.previewUrl ? (
                  <img src={adminProfile.previewUrl} alt="Preview" onError={() => handleProfileImageError("admin")} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                ) : adminProfile.profileImageUrl ? (
                  <img src={getProfileImageSrc(adminProfile.profileImageUrl)} alt="Avatar" onError={() => handleProfileImageError("admin")} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                ) : (
                  getInitials(adminDisplayName)
                )
              ) : (
                profile.previewUrl ? (
                  <img src={profile.previewUrl} alt="Preview" onError={() => handleProfileImageError("applicant")} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                ) : profile.profileImageUrl ? (
                  <img src={getProfileImageSrc(profile.profileImageUrl)} alt="Avatar" onError={() => handleProfileImageError("applicant")} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                ) : (
                  getInitials(applicantDisplayName)
                )
              )}
            </div>
            {workspaceRole === "admin" && (
              <span className="section-copy" style={{ fontSize: "0.85rem", border: "1px solid rgba(255,255,255,0.1)", padding: "8px 16px", borderRadius: "999px" }}>
                Signed in as {adminDisplayName || "Admin"} (Admin)
              </span>
            )}
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
        {workspaceRole === "admin"
          ? renderAdminWorkspace({
              activePage,
              campaigns,
              approvals,
              approvalReviewState,
              setApprovalReviewState,
              rewardsCatalog,
              adminNotificationsFeed: adminNotificationFeed,
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
            })
          : renderApplicantPage({
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
              notificationsFeed: applicantNotificationFeed,
              accountStatusItems,
            })}
      </main>
    </div>
  );
}

export default App;
