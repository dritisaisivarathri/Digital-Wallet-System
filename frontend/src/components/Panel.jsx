export function Panel({ title, subtitle, actions, children, className = "" }) {
  return (
    <section className={`panel animate-rise ${className}`}>
      <div className="mb-5 flex flex-col gap-3 border-b border-slate-900/10 pb-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p className="eyebrow">{title}</p>
          {subtitle ? <h2 className="panel-title">{subtitle}</h2> : null}
        </div>
        {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
      </div>
      {children}
    </section>
  );
}
