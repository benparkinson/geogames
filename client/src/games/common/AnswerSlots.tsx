import { Chip } from "../../ui";
import { FoundIcon, RevealIcon } from "../../ui/icons";

export default function AnswerSlots({
  answerCount,
  found,
  gaveUp,
  allAnswers,
}: {
  answerCount: number;
  found: string[];
  gaveUp: boolean;
  allAnswers: string[];
}): React.JSX.Element {
  const label = `Answers: ${found.length} of ${answerCount} found`;
  // After giving up, the slots after the found answers show the answers that weren't found.
  const revealed = gaveUp ? allAnswers.filter((a) => !found.includes(a)) : [];

  // With one answer, the empty "?" slot says nothing the question doesn't: don't spend a row on it.
  if (answerCount === 1 && found.length === 0 && revealed.length === 0) {
    return <></>;
  }

  return (
    <div className="gg-slots" role="group" aria-label={label}>
      {Array.from({ length: answerCount }, (_v, i) => {
        if (i < found.length) {
          return (
            <Chip key={i} tone="correct" className="slot--filled">
              {found[i]} <FoundIcon aria-hidden="true" />
            </Chip>
          );
        }
        const revealedAnswer = revealed[i - found.length];
        if (revealedAnswer) {
          return (
            <Chip key={i} tone="revealed" className="slot--filled" title="Revealed">
              {revealedAnswer} <RevealIcon aria-hidden="true" />
              <span className="gg-visually-hidden"> (revealed)</span>
            </Chip>
          );
        }
        return (
          <span key={i} className="answer-slot-empty gg-slots__empty gg-text-muted" aria-label="Not found yet">
            ?
          </span>
        );
      })}
    </div>
  );
}
