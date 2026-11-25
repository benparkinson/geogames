import { findGameModule } from "../games/registry";

/**
 * One icon per round of today's daily. Shows nothing if a type is unknown to this version of the site (a newer
 * server), rather than a row with a gap.
 */
export default function DailyPreview({ gameTypes }: { gameTypes: string[] }): React.JSX.Element | null {
  const modules = gameTypes.map(findGameModule);
  if (modules.length === 0 || modules.some((module) => module === undefined)) return null;
  return (
    <ul className="gg-ticket__rounds" aria-label="Today's games">
      {modules.map((module, index) => {
        const { Icon, title } = module!;
        return (
          <li key={index}>
            <Icon aria-hidden="true" />
            <span className="gg-visually-hidden">{title}</span>
          </li>
        );
      })}
    </ul>
  );
}
