import { useSyncExternalStore } from "react";

type Theme = "light" | "dark";

function subscribe(onChange: () => void): () => void {
  const observer = new MutationObserver(onChange);
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ["data-bs-theme"] });
  return () => observer.disconnect();
}

function currentTheme(): Theme {
  return document.documentElement.getAttribute("data-bs-theme") === "dark" ? "dark" : "light";
}

/** Small button that switches between light and dark. The choice is remembered in localStorage. */
export default function ThemeToggle(): React.JSX.Element {
  // The inline script in _document sets the real theme before React runs; the server snapshot is only the static export's.
  const theme = useSyncExternalStore(subscribe, currentTheme, () => "light" as Theme);

  function toggle(): void {
    const next: Theme = currentTheme() === "dark" ? "light" : "dark";
    document.documentElement.setAttribute("data-bs-theme", next);
    try {
      localStorage.setItem("gg-theme", next);
    } catch {
      // Storage blocked: the choice lasts until the page is reloaded.
    }
  }

  const label = theme === "dark" ? "Switch to light mode" : "Switch to dark mode";
  return (
    <button
      type="button"
      className="btn btn-sm btn-outline-secondary theme-toggle"
      onClick={toggle}
      aria-label={label}
      title={label}
    >
      <span aria-hidden="true">{theme === "dark" ? "☀️" : "🌙"}</span>
    </button>
  );
}
