/** The top of the panel: the question. The round's name is in the top bar. */
export default function RoundBrief({ question }: { question: string }): React.JSX.Element {
  return (
    <div className="round-brief">
      <h1 className="round-brief-question">{question}</h1>
    </div>
  );
}
