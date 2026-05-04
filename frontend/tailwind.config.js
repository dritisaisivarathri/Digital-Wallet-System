/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#132238",
        mist: "#eef5f7",
        ember: "#f26a4b",
        tide: "#0f766e",
        sand: "#f4efe7",
        brass: "#c58b39",
      },
      boxShadow: {
        panel: "0 24px 80px -36px rgba(16, 37, 52, 0.35)",
      },
      backgroundImage: {
        grid:
          "linear-gradient(to right, rgba(19,34,56,0.07) 1px, transparent 1px), linear-gradient(to bottom, rgba(19,34,56,0.07) 1px, transparent 1px)",
      },
      animation: {
        drift: "drift 18s ease-in-out infinite",
        rise: "rise 0.7s ease-out both",
      },
      keyframes: {
        drift: {
          "0%, 100%": { transform: "translate3d(0, 0, 0)" },
          "50%": { transform: "translate3d(0, -12px, 0)" },
        },
        rise: {
          "0%": { opacity: "0", transform: "translate3d(0, 16px, 0)" },
          "100%": { opacity: "1", transform: "translate3d(0, 0, 0)" },
        },
      },
    },
  },
  plugins: [],
};
