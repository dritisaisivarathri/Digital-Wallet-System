import re

with open('src/styles/index.css', 'r') as f:
    css = f.read()

# Replace .page-shell
css = re.sub(
    r'\.page-shell\s*\{[^}]*\}',
    '.page-shell {\n  display: flex;\n  flex-direction: column;\n  gap: 24px;\n  min-height: 100vh;\n  padding: 24px;\n  max-width: 1400px;\n  margin: 0 auto;\n}',
    css
)

# Add Violet theme
violet_theme = """
[data-theme='violet'] {
  --theme-r: 167;
  --theme-g: 139;
  --theme-b: 250;
  --theme-deep-r: 124;
  --theme-deep-g: 58;
  --theme-deep-b: 237;
}
"""
if "[data-theme='violet']" not in css:
    css += violet_theme

# Add new styles for top banner, nav pills, and theme switcher
new_styles = """
.top-banner {
  border-radius: 28px;
  padding: 32px;
  border: 1px solid var(--line);
  background: rgba(17, 24, 39, 0.82);
  box-shadow: 0 28px 70px -20px rgba(0, 0, 0, 0.8), 0 0 20px rgba(79, 209, 197, 0.1);
  backdrop-filter: blur(14px);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.top-banner-content h1 {
  font-size: clamp(2.2rem, 3vw, 3rem);
  margin: 12px 0;
  font-family: Bahnschrift, "Trebuchet MS", sans-serif;
  letter-spacing: 0;
}

.top-banner-content p {
  color: var(--muted);
  max-width: 600px;
  line-height: 1.6;
  margin-bottom: 24px;
}

.top-banner-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-end;
}

.theme-switcher-group {
  display: flex;
  gap: 12px;
}

.theme-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(17, 24, 39, 0.6);
  color: var(--muted);
  font-weight: 600;
  font-size: 0.9rem;
  transition: all 160ms ease;
}

.theme-btn.active {
  border-color: var(--line-strong);
  color: var(--ink);
  background: rgba(var(--theme-r), var(--theme-g), var(--theme-b), 0.1);
}

.theme-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.theme-dot.aqua { background: #4fd1c5; }
.theme-dot.sunset { background: #fb923c; }
.theme-dot.violet { background: #a78bfa; }

.nav-pill-list {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  overflow-x: auto;
}

.nav-pill {
  padding: 10px 24px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(17, 24, 39, 0.6);
  color: var(--muted);
  font-weight: 600;
  white-space: nowrap;
  transition: all 160ms ease;
}

.nav-pill:hover {
  border-color: var(--line-strong);
}

.nav-pill.active {
  background: rgba(var(--theme-r), var(--theme-g), var(--theme-b), 0.15);
  border-color: rgba(var(--theme-r), var(--theme-g), var(--theme-b), 0.3);
  color: var(--ink);
}

.transaction-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.transaction-table th {
  padding: 16px;
  border-bottom: 1px solid var(--line);
  color: var(--muted);
  font-size: 0.85rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.transaction-table td {
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.transaction-table tr:last-child td {
  border-bottom: none;
}

.auth-card-tabs {
  display: flex;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.6);
  border: 1px solid var(--line);
  overflow: hidden;
  margin-bottom: 24px;
}

.auth-card-tab {
  flex: 1;
  padding: 16px;
  text-align: center;
  font-weight: 600;
  color: var(--muted);
  background: transparent;
  border: none;
  border-radius: 999px;
  transition: all 160ms ease;
}

.auth-card-tab.active {
  background: var(--accent);
  color: #000;
}
"""
if ".top-banner {" not in css:
    css += new_styles

with open('src/styles/index.css', 'w') as f:
    f.write(css)
