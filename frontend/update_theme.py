import re

with open('src/styles/index.css', 'r') as f:
    c = f.read()

c = c.replace('79, 209, 197', 'var(--theme-r), var(--theme-g), var(--theme-b)')
c = c.replace('49, 151, 149', 'var(--theme-deep-r), var(--theme-deep-g), var(--theme-deep-b)')

# Add new variables to :root
root_replacement = """\
:root {
  --theme-r: 244;
  --theme-g: 114;
  --theme-b: 182;
  --theme-deep-r: 219;
  --theme-deep-g: 39;
  --theme-deep-b: 119;
  --accent: rgb(var(--theme-r), var(--theme-g), var(--theme-b));
  --accent-deep: rgb(var(--theme-deep-r), var(--theme-deep-g), var(--theme-deep-b));\
"""
c = re.sub(r':root\s*\{', root_replacement, c)

# Remove the old hardcoded accent variables
c = re.sub(r'--accent: #4fd1c5;\n', '', c)
c = re.sub(r'--accent-deep: #319795;\n', '', c)

themes = """

[data-theme='aqua'] {
  --theme-r: 79;
  --theme-g: 209;
  --theme-b: 197;
  --theme-deep-r: 49;
  --theme-deep-g: 151;
  --theme-deep-b: 149;
}

[data-theme='sunset'] {
  --theme-r: 251;
  --theme-g: 146;
  --theme-b: 60;
  --theme-deep-r: 234;
  --theme-deep-g: 88;
  --theme-deep-b: 12;
}

[data-theme='green'] {
  --theme-r: 74;
  --theme-g: 222;
  --theme-b: 128;
  --theme-deep-r: 22;
  --theme-deep-g: 163;
  --theme-deep-b: 74;
}
"""
c += themes

with open('src/styles/index.css', 'w') as f:
    f.write(c)
