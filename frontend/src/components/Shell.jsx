import { startTransition } from "react";

export function Shell({
  session,
  items,
  activeSection,
  onSelectSection,
  onRefresh,
  onLogout,
  status,
  children,
}) {
  return (
    <div className="min-h-screen px-4 py-5 md:px-6">
      <div className="mx-auto grid max-w-7xl gap-5 lg:grid-cols-[260px_minmax(0,1fr)]">
        <aside className="panel relative overflow-hidden">
          <div className="absolute inset-x-6 top-0 h-32 rounded-b-full bg-ember/20 blur-2xl" />
          <div className="relative">
            <p className="eyebrow">Workspace</p>
            <h1 className="font-display mt-2 text-3xl font-semibold text-ink">
              Control Center
            </h1>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Signed in as <span className="font-semibold text-ink">{session.email}</span>
            </p>
            <div className="mt-5 grid gap-2 rounded-xl border border-slate-900/10 bg-slate-900/5 p-3 text-sm">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Role</span>
                <span className="rounded-full bg-white px-3 py-1 font-semibold text-ink">
                  {session.role}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">User ID</span>
                <span className="font-mono text-xs text-slate-700">
                  {session.userId?.slice(0, 8)}
                </span>
              </div>
            </div>

            <nav className="mt-6 space-y-2">
              {items.map((item) => {
                const active = activeSection === item.key;
                return (
                  <button
                    key={item.key}
                    type="button"
                    onClick={() =>
                      startTransition(() => {
                        onSelectSection(item.key);
                      })
                    }
                    className={`w-full rounded-xl px-4 py-3 text-left text-sm font-semibold transition ${
                      active
                        ? "bg-ink text-white shadow-panel"
                        : "bg-white/70 text-slate-700 hover:bg-slate-900/5"
                    }`}
                  >
                    <div className="flex items-center justify-between gap-3">
                      <span>{item.label}</span>
                      <span
                        className={`text-[11px] uppercase tracking-[0.18em] ${
                          active ? "text-white/70" : "text-slate-400"
                        }`}
                      >
                        {item.tag}
                      </span>
                    </div>
                  </button>
                );
              })}
            </nav>

            <div className="mt-6 flex flex-wrap gap-2">
              <button type="button" className="button-secondary" onClick={onRefresh}>
                Refresh
              </button>
              <button type="button" className="button-primary" onClick={onLogout}>
                Logout
              </button>
            </div>
            <p className="mt-4 text-sm text-slate-500">{status}</p>
          </div>
        </aside>

        <main className="space-y-5">{children}</main>
      </div>
    </div>
  );
}
