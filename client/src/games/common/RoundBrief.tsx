/** The top of the panel: the question, and with several answers the count found, beside it when there is room. The round's name is in the top bar. */
export default function RoundBrief({
  question,
  found,
  answerCount = 1,
}: {
  question: string;
  found?: number;
  answerCount?: number;
}): React.JSX.Element {
  return (
    <div className="round-brief">
      <h1 className="round-brief-question">{question}</h1>
      {answerCount > 1 && found !== undefined && (
        <span className="round-brief__count">{`${found}/${answerCount} found`}</span>
      )}
    </div>
  );
}
