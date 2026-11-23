import { useSyncExternalStore } from "react";
import { createLocalStore } from "./localStore";

export type ThemeChoice = "system" | "light" | "dark";

const store = createLocalStore("gg-theme", "gg-theme-change");

function read(): ThemeChoice {
  const raw = store.read();
  return raw === "light" || raw === "dark" ? raw : "system";
}

function osTheme(): "light" | "dark" {
  try {
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  } catch {
    return "light";
  }
}

function write(choice: ThemeChoice): void {
  store.write(choice === "system" ? null : choice);
  document.documentElement.setAttribute("data-theme", choice === "system" ? osTheme() : choice);
}

/**
 * The Appearance setting: the stored choice (System default when nothing is stored), not the theme on the page.
 * The inline script in `_document` applies the theme before React runs and follows the OS when nothing is stored.
 */
export function useThemeChoice(): [ThemeChoice, (choice: ThemeChoice) => void] {
  const choice = useSyncExternalStore(store.subscribe, read, () => "system" as const);
  return [choice, write];
}

/** Test helper: forget the in-memory state so each test starts clean. */
export function resetThemeForTests(): void {
  store.resetForTests();
}
