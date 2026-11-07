import { CheckLg, EyeFill } from "react-bootstrap-icons";

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
            <span key={i} className="slot--filled badge rounded-pill bg-success fs-6 fw-normal">
              {found[i]} <CheckLg aria-hidden="true" />
            </span>
          );
        }
        const revealedAnswer = revealed[i - found.length];
        if (revealedAnswer) {
          return (
            <span key={i} className="slot--filled badge rounded-pill bg-secondary fs-6 fw-normal" title="Revealed">
              {revealedAnswer} <EyeFill aria-hidden="true" />
              <span className="visually-hidden"> (revealed)</span>
            </span>
          );
        }
        return (
          <span key={i} className="answer-slot-empty rounded-pill px-3 text-muted fs-6" aria-label="Not found yet">?</span>
        );
      })}
    </div>
  );
}
