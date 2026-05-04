import { clearSession, readSession, writeSession } from "./session";

const configuredBase = import.meta.env.VITE_API_BASE_URL?.trim();
const API_ROOT = configuredBase ? configuredBase.replace(/\/$/, "") : "";

export function buildUrl(path) {
  const normalizedPath = path.startsWith("/api") ? path : `/api${path}`;
  return API_ROOT ? `${API_ROOT}${normalizedPath}` : normalizedPath;
}

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const fieldErrors = payload && typeof payload === "object"
      ? Object.values(payload).filter(Boolean).join(" ")
      : "";
    const message =
      typeof payload === "string"
        ? payload
        : payload?.message || payload?.error || fieldErrors || "Request failed";
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return payload;
}

let refreshPromise = null;

async function refreshSessionToken() {
  const session = readSession();
  if (!session?.refreshToken) {
    return null;
  }

  if (!refreshPromise) {
    refreshPromise = fetch(buildUrl("/auth/refresh-token"), {
      method: "POST",
      cache: "no-store",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken: session.refreshToken }),
    })
      .then(parseResponse)
      .then((payload) => {
        const refreshedSession = {
          ...session,
          ...payload,
          token: payload?.token || session.token,
          refreshToken: payload?.refreshToken || session.refreshToken,
        };
        writeSession(refreshedSession);
        return refreshedSession;
      })
      .catch((error) => {
        clearSession();
        throw error;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

export async function apiRequest(path, options = {}) {
  const { token, headers = {}, body, __retriedAfterRefresh = false, ...rest } = options;
  const requestHeaders = { ...headers };

  if (body !== undefined && !(body instanceof FormData)) {
    requestHeaders["Content-Type"] = "application/json";
  }

  if (token) {
    requestHeaders.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(buildUrl(path), {
      ...rest,
      cache: rest.cache || "no-store",
      headers: requestHeaders,
      body: body !== undefined ? (body instanceof FormData ? body : JSON.stringify(body)) : undefined,
    });
  } catch (error) {
    throw new Error("Backend services are starting or unreachable. Please retry in a few moments.");
  }

  try {
    return await parseResponse(response);
  } catch (error) {
    if (error?.status !== 401 || __retriedAfterRefresh || path === "/auth/refresh-token") {
      throw error;
    }

    const refreshedSession = await refreshSessionToken();
    if (!refreshedSession?.token) {
      throw new Error("Session expired. Please sign in again.");
    }

    return apiRequest(path, {
      ...options,
      token: refreshedSession.token,
      __retriedAfterRefresh: true,
    });
  }
}
