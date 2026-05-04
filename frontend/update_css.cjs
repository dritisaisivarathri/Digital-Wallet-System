const fs = require('fs');
let css = fs.readFileSync('src/styles/index.css', 'utf8');

css = css.replace(':root {', `@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');\n\n:root {`);

css = css.replace(/--bg-0: #f3f8fa;/g, '--bg-0: #0a0e17;');
css = css.replace(/--bg-1: #ffffff;/g, '--bg-1: #111827;');
css = css.replace(/--bg-2: #ebf3f5;/g, '--bg-2: #1f2937;');
css = css.replace(/--ink: #132238;/g, '--ink: #f3f4f6;');
css = css.replace(/--muted: #5a6a78;/g, '--muted: #9ca3af;');
css = css.replace(/--line: rgba\(19, 34, 56, 0.12\);/g, '--line: rgba(79, 209, 197, 0.15);');
css = css.replace(/--line-strong: rgba\(19, 34, 56, 0.2\);/g, '--line-strong: rgba(79, 209, 197, 0.3);');
css = css.replace(/--accent: #ef6c4f;/g, '--accent: #4fd1c5;');
css = css.replace(/--accent-deep: #cf5539;/g, '--accent-deep: #319795;');
css = css.replace(/--tide: #0f766e;/g, '--tide: #4fd1c5;');
css = css.replace(/--tide-soft: rgba\(15, 118, 110, 0.12\);/g, '--tide-soft: rgba(79, 209, 197, 0.15);');
css = css.replace(/color-scheme: light;/g, 'color-scheme: dark;');
css = css.replace(/font-family: Aptos, "Segoe UI", sans-serif;/g, 'font-family: \'Outfit\', "Segoe UI", sans-serif;');

// Background
css = css.replace(/radial-gradient\(circle at top left, rgba\(239, 108, 79, 0.14\), transparent 26%\),\s*radial-gradient\(circle at top right, rgba\(15, 118, 110, 0.15\), transparent 28%\),\s*linear-gradient\(180deg, #f7fbfc 0%, #eef4f7 52%, #f7fafc 100%\)/g, 'radial-gradient(circle at top left, rgba(49, 151, 149, 0.15), transparent 35%),\n    radial-gradient(circle at bottom right, rgba(79, 209, 197, 0.1), transparent 35%),\n    linear-gradient(180deg, #0a0e17 0%, #111827 100%)');

// Replace card backgrounds
css = css.replace(/rgba\(255, 255, 255, 0.82\)/g, 'rgba(17, 24, 39, 0.82)');
css = css.replace(/rgba\(255, 255, 255, 0.72\)/g, 'rgba(17, 24, 39, 0.72)');
css = css.replace(/rgba\(255, 255, 255, 0.64\)/g, 'rgba(17, 24, 39, 0.64)');
css = css.replace(/rgba\(255, 255, 255, 0.68\)/g, 'rgba(17, 24, 39, 0.68)');
css = css.replace(/rgba\(255, 255, 255, 0.7\)/g, 'rgba(17, 24, 39, 0.7)');
css = css.replace(/rgba\(255, 255, 255, 0.76\)/g, 'rgba(17, 24, 39, 0.76)');
css = css.replace(/rgba\(255, 255, 255, 0.8\)/g, 'rgba(17, 24, 39, 0.8)');
css = css.replace(/rgba\(255, 255, 255, 0.92\)/g, 'rgba(17, 24, 39, 0.92)');
css = css.replace(/rgba\(255, 255, 255, 0.94\)/g, 'rgba(17, 24, 39, 0.94)');
css = css.replace(/rgba\(255, 255, 255, 0.96\)/g, 'rgba(17, 24, 39, 0.96)');
css = css.replace(/background: white;/g, 'background: rgba(17, 24, 39, 0.8);');
css = css.replace(/color: white;/g, 'color: #000;');

// Inner gradients for cards
css = css.replace(/linear-gradient\(180deg, rgba\(15, 118, 110, 0.08\), rgba\(255, 255, 255, 0.92\)\)/g, 'linear-gradient(180deg, rgba(79, 209, 197, 0.08), rgba(17, 24, 39, 0.92))');
css = css.replace(/linear-gradient\(135deg, rgba\(15, 118, 110, 0.12\), rgba\(255, 255, 255, 0.96\)\)/g, 'linear-gradient(135deg, rgba(79, 209, 197, 0.12), rgba(17, 24, 39, 0.96))');

// Update border colors
css = css.replace(/rgba\(15, 118, 110, 0.3\)/g, 'rgba(79, 209, 197, 0.3)');
css = css.replace(/rgba\(15, 118, 110, 0.12\)/g, 'rgba(79, 209, 197, 0.12)');
css = css.replace(/rgba\(15, 118, 110, 0.24\)/g, 'rgba(79, 209, 197, 0.24)');
css = css.replace(/rgba\(15, 118, 110, 0.1\)/g, 'rgba(79, 209, 197, 0.1)');
css = css.replace(/rgba\(15, 118, 110, 0.42\)/g, 'rgba(79, 209, 197, 0.42)');
css = css.replace(/rgba\(15, 118, 110, 0.28\)/g, 'rgba(79, 209, 197, 0.28)');
css = css.replace(/rgba\(15, 118, 110, 0.34\)/g, 'rgba(79, 209, 197, 0.34)');

// Primary buttons update
css = css.replace(/\.app-button\.primary \{\s*background: var\(--ink\);\s*color: #000;\s*\}/g, '.app-button.primary {\n  background: linear-gradient(90deg, var(--accent), var(--accent-deep));\n  color: #000;\n  box-shadow: 0 4px 14px rgba(79, 209, 197, 0.3);\n}');
css = css.replace(/\.app-button\.primary \{\s*background: var\(--ink\);\s*color: white;\s*\}/g, '.app-button.primary {\n  background: linear-gradient(90deg, var(--accent), var(--accent-deep));\n  color: #000;\n  box-shadow: 0 4px 14px rgba(79, 209, 197, 0.3);\n}');

// Shadows update
css = css.replace(/var\(--shadow\)/g, '0 28px 70px -20px rgba(0, 0, 0, 0.8), 0 0 20px rgba(79, 209, 197, 0.1)');

// Add text gradients for specific headers
css += `\n.auth-hero-panel h1, .brand-block h1 {\n  background: linear-gradient(90deg, #fff, #9ca3af);\n  -webkit-background-clip: text;\n  -webkit-text-fill-color: transparent;\n}\n`;

fs.writeFileSync('src/styles/index.css', css);
console.log('CSS Replaced');
