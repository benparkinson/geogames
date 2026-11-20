import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { pageActions, UpdateNotice } from "./UpdateNotice";

const router = vi.hoisted(() => ({ pathname: "/" }));
vi.mock("next/router", () => ({ useRouter: () => router }));

const update = vi.hoisted(() => ({ value: { updateAvailable: false, latestBuildId: null as string | null } }));
vi.mock("../api/version", () => ({ useUpdateAvailable: () => update.value }));

const TEXT = "A new version of Geogames is available.";

beforeEach(() => {
  router.pathname = "/";
  update.value = { updateAvailable: true, latestBuildId: "b" };
});

describe("UpdateNotice", () => {
  it("shows the toast when an update is available", () => {
    render(<UpdateNotice />);
    expect(screen.getByRole("status").textContent).toContain(TEXT);
  });

  it("shows nothing when there is no update", () => {
    update.value = { updateAvailable: false, latestBuildId: "a" };
    render(<UpdateNotice />);
    expect(screen.queryByRole("status")).toBeNull();
  });

  it("shows nothing on a game page", () => {
    router.pathname = "/games/[gameId]";
    render(<UpdateNotice />);
    expect(screen.queryByRole("status")).toBeNull();
  });

  it("hides when dismissed and returns for a different newer build", async () => {
    const { rerender } = render(<UpdateNotice />);
    await userEvent.click(screen.getByRole("button", { name: "Dismiss" }));
    expect(screen.queryByRole("status")).toBeNull();

    rerender(<UpdateNotice />);
    expect(screen.queryByRole("status")).toBeNull();

    update.value = { updateAvailable: true, latestBuildId: "c" };
    act(() => rerender(<UpdateNotice />));
    expect(screen.getByRole("status")).toBeTruthy();
  });

  it("reloads on Refresh", async () => {
    const reload = vi.spyOn(pageActions, "reload").mockImplementation(() => {});
    render(<UpdateNotice />);
    await userEvent.click(screen.getByRole("button", { name: "Refresh" }));
    expect(reload).toHaveBeenCalledOnce();
  });
});
