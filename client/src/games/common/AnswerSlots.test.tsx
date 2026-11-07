import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import AnswerSlots from "./AnswerSlots";

describe("AnswerSlots", () => {
  it("describes how many answers have been found", () => {
    render(<AnswerSlots answerCount={3} found={["France"]} gaveUp={false} allAnswers={[]} />);

    expect(screen.getByRole("group", { name: "Answers: 1 of 3 found" })).toBeInTheDocument();
  });

  it("shows found answers first and a placeholder for each missing one", () => {
    render(<AnswerSlots answerCount={3} found={["France"]} gaveUp={false} allAnswers={[]} />);

    expect(screen.getByText("France")).toBeInTheDocument();
    expect(screen.getAllByLabelText("Not found yet")).toHaveLength(2);
    expect(screen.queryByText("(revealed)")).not.toBeInTheDocument();
  });

  it("shows the remaining answers as revealed after giving up", () => {
    render(
      <AnswerSlots
        answerCount={3}
        found={["France"]}
        gaveUp
        allAnswers={["France", "Spain", "Italy"]}
      />,
    );

    expect(screen.getByText("Spain")).toBeInTheDocument();
    expect(screen.getByText("Italy")).toBeInTheDocument();
    expect(screen.getAllByText("(revealed)")).toHaveLength(2);
    expect(screen.queryByLabelText("Not found yet")).not.toBeInTheDocument();
  });

  it("keeps a placeholder for slots with no known answer even after giving up", () => {
    render(<AnswerSlots answerCount={2} found={[]} gaveUp allAnswers={["Spain"]} />);

    expect(screen.getByText("Spain")).toBeInTheDocument();
    expect(screen.getAllByLabelText("Not found yet")).toHaveLength(1);
  });

  it("reveals only the answers that weren't found, whatever order they were found in", () => {
    render(
      <AnswerSlots
        answerCount={3}
        found={["Italy"]}
        gaveUp
        allAnswers={["France", "Spain", "Italy"]}
      />,
    );

    expect(screen.getAllByText("Italy")).toHaveLength(1);
    expect(screen.getAllByText("(revealed)")).toHaveLength(2);
    expect(screen.getByText("France")).toBeInTheDocument();
    expect(screen.getByText("Spain")).toBeInTheDocument();
  });

  it("shows nothing as revealed before giving up", () => {
    render(<AnswerSlots answerCount={2} found={[]} gaveUp={false} allAnswers={["France", "Spain"]} />);

    expect(screen.queryByText("France")).not.toBeInTheDocument();
    expect(screen.getAllByLabelText("Not found yet")).toHaveLength(2);
  });
});
