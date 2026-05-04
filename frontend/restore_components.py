import re

with open('src/App.jsx', 'r', encoding='utf-8') as f:
    content = f.read()

components = """function HeroBand({ title, subtitle, description }) {
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

function SectionCard({ label, title, children, style }) {
  return (
    <div className="section-card" style={style}>
      {label && <span className="section-label">{label}</span>}
      {title && <h3>{title}</h3>}
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

function Field({ label, value, onChange, type = "text", as = "input", options = [], placeholder, ...props }) {
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
          <input className="field-input" type={type} value={value} onChange={onChange} placeholder={placeholder} {...props} />
        )}
      </div>
    </div>
  );
}

"""

if 'function HeroBand' not in content:
    content = content.replace('function App() {', components + 'function App() {')

with open('src/App.jsx', 'w', encoding='utf-8') as f:
    f.write(content)
