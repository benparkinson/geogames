import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createLocalStore } from "./localStore";

const store = createLocalStore("gg-test", "gg-test-change");

beforeEach(() => {
  localStorage.clear();
  store.resetForTests();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("createLocalStore", () => {
  it("reads nothing at first, then what was written, under its key", () => {
    expect(store.read()).toBeNull();

    store.write("a");

    expect(store.read()).toBe("a");
    expect(localStorage.getItem("gg-test")).toBe("a");
  });

  it("removes the key when null is written", () => {
    store.write("a");

    store.write(null);

    expect(store.read()).toBeNull();
    expect(localStorage.getItem("gg-test")).toBeNull();
  });

  it("tells subscribers about a write in this tab and about another tab's, until they unsubscribe", () => {
    const onChange = vi.fn();
    const unsubscribe = store.subscribe(onChange);

    store.write("a");
    window.dispatchEvent(new StorageEvent("storage", { key: "gg-test" }));
    expect(onChange).toHaveBeenCalledTimes(2);

    unsubscribe();
    store.write("b");
    expect(onChange).toHaveBeenCalledTimes(2);
  });

  it("keeps the value in memory when saving fails, even though reading still works", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("quota");
    });

    store.write("a");

    expect(store.read()).toBe("a");
  });

  it("keeps the value in memory when reading fails", () => {
    store.write("a");
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });

    expect(store.read()).toBe("a");
  });

  it("forgets the in-memory value and the fallback when reset", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("quota");
    });
    store.write("a");
    vi.restoreAllMocks();

    store.resetForTests();

    expect(store.read()).toBeNull();
  });
});
