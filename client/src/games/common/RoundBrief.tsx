import type { IconComponent } from "../../ui/icons";

interface RoundBriefProps {
  /** What kind of round this is: the game's name. */
  title: string;
  Icon: IconComponent;
  /** What the player has to name, and how many. */
  question: string;
}

/** The top of the panel: the kind of round and the question. */
export default function RoundBrief({ title, Icon, question }: RoundBriefProps): React.JSX.Element {
  return (
    <div className="round-brief">
      <div className="round-brief-title">
        <Icon aria-hidden="true" /> {title}
      </div>
      <h1 className="round-brief-question">{question}</h1>
    </div>
  );
}
