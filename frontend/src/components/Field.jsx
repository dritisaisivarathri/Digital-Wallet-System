export function Field({
  label,
  as = "input",
  type = "text",
  value,
  onChange,
  placeholder,
  options = [],
  rows = 4,
  required = false,
}) {
  const className =
    "mt-2 w-full rounded-xl border border-slate-900/10 bg-white px-4 py-3 text-sm text-ink outline-none transition focus:border-ink focus:ring-2 focus:ring-ember/20";

  return (
    <label className="block">
      <span className="label-text">
        {label}
        {required ? " *" : ""}
      </span>
      {as === "textarea" ? (
        <textarea
          rows={rows}
          className={className}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          required={required}
        />
      ) : as === "select" ? (
        <select className={className} value={value} onChange={onChange} required={required}>
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      ) : (
        <input
          className={className}
          type={type}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          required={required}
        />
      )}
    </label>
  );
}
