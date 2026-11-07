import { CheckLg, EyeFill } from "react-bootstrap-icons";

export default function AnswerSlots({ answerCount, found, gaveUp, allAnswers }: {
  answerCount: number;
  found: string[];
  gaveUp: boolean;
  allAnswers: string[];
}): React.JSX.Element {
  const label = `Answers: ${found.length} of ${answerCount} found`;

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
        // TODO: unreachable in practice. After "Reveal answer" the reducer marks every answer as found,
        // so `found` already fills all the slots and the grey "revealed" style never shows.
        if (gaveUp && allAnswers[i]) {
          return (
            <span key={i} className="slot--filled badge rounded-pill bg-secondary fs-6 fw-normal" title="Revealed">
              {allAnswers[i]} <EyeFill aria-hidden="true" />
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
