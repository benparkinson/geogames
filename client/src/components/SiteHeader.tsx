import { localDateString } from "../api/daily";
import { currentStreak, useDailyStreak } from "../daily/dailyStreak";
import { useNowSeconds } from "../daily/useNowSeconds";
import { StreakIcon } from "../ui/icons";

/**
 * The top of the home page and the static pages: the site name (`title`) at the start, then at the far end
 * `extra`, the daily streak (from one day, never while pre-rendering). An ink rule closes it.
 */
export default function SiteHeader({
  title,
  extra,
}: {
  title: React.ReactNode;
  extra?: React.ReactNode;
}): React.JSX.Element {
  const nowSeconds = useNowSeconds();
  const today = nowSeconds === null ? null : localDateString(new Date(nowSeconds * 1000));
  const streak = currentStreak(useDailyStreak(), today);

  return (
    <header className="gg-site-header">
      <div className="gg-site-header__name">{title}</div>
      <div className="gg-site-header__end">
        {extra}
        {streak >= 1 && (
          <span className="gg-site-header__streak gg-mono" data-streak={streak}>
            <StreakIcon aria-hidden="true" /> {streak}-day streak
          </span>
        )}
      </div>
    </header>
  );
}
