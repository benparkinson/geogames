import { FoundIcon, IconComponent, OutOfLivesIcon, RevealIcon } from "../../ui/icons";
import type { RoundResultKind } from "./roundView";

/** The icon and words for how a finished round ended, so every view that shows it matches. */
export const RESULT_ICONS: Record<RoundResultKind, IconComponent> = {
  correct: FoundIcon,
  revealed: RevealIcon,
  outOfGuesses: OutOfLivesIcon,
};

export const RESULT_WORDS: Record<RoundResultKind, string> = {
  correct: "correct",
  revealed: "gave up",
  outOfGuesses: "out of lives",
};
