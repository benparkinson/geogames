import { CheckLg, EyeFill } from "react-bootstrap-icons";
import { Chip } from "../../ui";

export default function AnswerSlots({ answerCount, found, gaveUp, allAnswers }: {
  answerCount: number;
  found: string[];
  gaveUp: boolean;
  allAnswers: string[];
}): React.JSX.Element {
  const label = `Answers: ${found.length} of ${answerCount} found`;
  // After giving up, the slots after the found answers show the answers that weren't found.
  const revealed = gaveUp ? allAnswers.filter(a => !found.includes(a)) : [];

  return (
    <div className="d-flex gap-2 flex-wrap mb-2" role="group" aria-label={label}>
      {Array.from({ length: answerCount }, (_v, i) => {
        if (i < found.length) {
          return (
            <Chip key={i} tone="correct" className="slot--filled">
              {found[i]} <CheckLg aria-hidden="true" />
            </Chip>
          );
        }
        const revealedAnswer = revealed[i - found.length];
        if (revealedAnswer) {
          return (
            <Chip key={i} tone="revealed" className="slot--filled" title="Revealed">
              {revealedAnswer} <EyeFill aria-hidden="true" />
              <span className="visually-hidden"> (revealed)</span>
            </Chip>
          );
        }
        return (
          <span key={i} className="answer-slot-empty px-3 gg-text-muted fs-6" aria-label="Not found yet">?</span>
        );
      })}
    </div>
  );
}
