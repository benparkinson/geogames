import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { renderWithClient } from "../test/render";
import InitialsForm from "./InitialsForm";

const PUT = "http://localhost:8080/api/games/g1/challenge-initials";

const input = () => screen.getByRole("textbox", { name: "Your initials" });
const save = () => screen.getByRole("button", { name: "Save" });

describe("InitialsForm", () => {
  it("upper-cases what is typed, drops anything but letters and stops at three", async () => {
    renderWithClient(<InitialsForm gameId="g1" />);
    expect(save()).toBeDisabled();

    await userEvent.type(input(), "a1- bcd");

    expect(input()).toHaveValue("ABC");
  });

  it("saves the initials and takes the box away", async () => {
    const fetchMock = mockFetch([{ method: "PUT", url: PUT, status: 204 }]);
    renderWithClient(<InitialsForm gameId="g1" />);

    await userEvent.type(input(), "ab");
    await userEvent.click(save());

    await waitFor(() => expect(screen.queryByRole("textbox")).not.toBeInTheDocument());
    expect(screen.queryByRole("button", { name: "Save" })).not.toBeInTheDocument();
    expect(callsTo(fetchMock)).toHaveLength(1);
    expect(callsTo(fetchMock)[0].body).toEqual({ initials: "AB" });
  });

  it("lets other initials be tried after a refusal", async () => {
    mockFetch([{ method: "PUT", url: PUT, status: 400, body: {} }]);
    renderWithClient(<InitialsForm gameId="g1" />);
    await userEvent.type(input(), "xx{Enter}");
    expect(await screen.findByRole("alert")).toHaveTextContent("Those initials can't be used. Try others.");

    mockFetch([{ method: "PUT", url: PUT, status: 204 }]);
    await userEvent.clear(input());
    await userEvent.type(input(), "ab{Enter}");

    await waitFor(() => expect(screen.queryByRole("textbox")).not.toBeInTheDocument());
  });

  it.each([
    [400, "Those initials can't be used. Try others."],
    [409, "Couldn't save your initials. Try again."],
  ])("shows the server's refusal (%i) until the text changes", async (status, message) => {
    mockFetch([{ method: "PUT", url: PUT, status, body: {} }]);
    renderWithClient(<InitialsForm gameId="g1" />);

    await userEvent.type(input(), "xx{Enter}");
    expect(await screen.findByRole("alert")).toHaveTextContent(message);

    await userEvent.type(input(), "y");
    await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument());
  });
});
