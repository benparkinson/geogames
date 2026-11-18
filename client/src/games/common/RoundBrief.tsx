import type { Icon } from "react-bootstrap-icons";

interface RoundBriefProps {
  /** What kind of round this is: the game's name. */
  title: string;
  Icon: Icon;
  /** What the player has to name, and how many. */
  question: string;
  /** What the marks on the map are. Left out once the map shows the answer. */
  mapKey?: string;
}

/** The top of the panel: the kind of round, the question, then what the map shows. */
export default function RoundBrief({ title, Icon, question, mapKey }: RoundBriefProps): React.JSX.Element {
  return (
    <div className="round-brief">
      <div className="round-brief-title">
        <Icon aria-hidden="true" /> {title}
      </div>
      <h1 className="round-brief-question">{question}</h1>
      {mapKey && <p className="round-brief-key">{mapKey}</p>}
    </div>
  );
}
