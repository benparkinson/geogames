import { afterEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ShareButton from "./ShareButton";

afterEach(() => {
  vi.unstubAllGlobals();
  Object.defineProperty(navigator, "clipboard", { value: undefined, configurable: true });
  Object.defineProperty(navigator, "share", { value: undefined, configurable: true });
});

function setClipboard(writeText: unknown) {
  Object.defineProperty(navigator, "clipboard", { value: writeText === undefined ? undefined : { writeText }, configurable: true });
}

describe("ShareButton", () => {
  it("copies the text and says so", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    setClipboard(writeText);
    render(<ShareButton text="the text" />);

    await userEvent.click(screen.getByRole("button", { name: "Copy result" }));

    expect(writeText).toHaveBeenCalledWith("the text");
    await waitFor(() => expect(screen.getByText("Copied!")).toBeInTheDocument());
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it("shows the text to copy by hand when the clipboard refuses", async () => {
    setClipboard(vi.fn().mockRejectedValue(new Error("denied")));
    render(<ShareButton text="the text" />);

    await userEvent.click(screen.getByRole("button", { name: "Copy result" }));

    const box = await screen.findByRole("textbox", { name: "Copy this text" });
    expect(box).toHaveValue("the text");
    expect(box).toHaveAttribute("readonly");
    expect(screen.queryByText("Copied!")).toBeNull();
  });

  it("shows the text to copy by hand when there is no clipboard", async () => {
    setClipboard(undefined);
    render(<ShareButton text="the text" />);

    await userEvent.click(screen.getByRole("button", { name: "Copy result" }));

    expect(await screen.findByRole("textbox", { name: "Copy this text" })).toHaveValue("the text");
  });

  it("offers the system share sheet as well when there is one", async () => {
    const share = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "share", { value: share, configurable: true });
    render(<ShareButton text="the text" />);

    await userEvent.click(screen.getByRole("button", { name: "Share" }));

    expect(share).toHaveBeenCalledWith({ text: "the text" });
    expect(screen.getByRole("button", { name: "Copy result" })).toBeInTheDocument();
  });

  it("ignores a cancelled share sheet", async () => {
    const share = vi.fn().mockRejectedValue(new DOMException("cancelled", "AbortError"));
    Object.defineProperty(navigator, "share", { value: share, configurable: true });
    render(<ShareButton text="the text" />);

    await userEvent.click(screen.getByRole("button", { name: "Share" }));

    expect(share).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it("selects the text when the box is focused", async () => {
    setClipboard(undefined);
    render(<ShareButton text="the text" />);
    await userEvent.click(screen.getByRole("button", { name: "Copy result" }));
    const box = (await screen.findByRole("textbox")) as HTMLTextAreaElement;
    box.setSelectionRange(0, 0);

    fireEvent.focus(box);

    expect(box.selectionEnd - box.selectionStart).toBe("the text".length);
  });

  it("stops saying Copied! after a few seconds", async () => {
    vi.useFakeTimers();
    try {
      setClipboard(vi.fn().mockResolvedValue(undefined));
      render(<ShareButton text="the text" />);

      fireEvent.click(screen.getByRole("button", { name: "Copy result" }));
      await act(async () => {
        await Promise.resolve();
      });
      expect(screen.getByText("Copied!")).toBeInTheDocument();
      act(() => {
        vi.advanceTimersByTime(5000);
      });

      expect(screen.queryByText("Copied!")).toBeNull();
    } finally {
      vi.useRealTimers();
    }
  });

  it("has no Share button without navigator.share", () => {
    render(<ShareButton text="the text" />);

    expect(screen.queryByRole("button", { name: "Share" })).toBeNull();
  });
});
