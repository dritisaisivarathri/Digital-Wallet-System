export function StatCard({ label, value, detail, tone = "default" }) {
  const toneClass =
    tone === "warm"
      ? "border-ember/30 bg-ember/10"
      : tone === "cool"
        ? "border-tide/30 bg-tide/10"
        : "border-slate-900/10 bg-white/70";

  return (
    <article className={`rounded-xl border p-4 ${toneClass}`}>
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
        {label}
      </p>
      <p className="mt-3 text-2xl font-semibold text-ink">{value}</p>
      {detail ? <p className="mt-2 text-sm text-slate-600">{detail}</p> : null}
    </article>
  );
}
