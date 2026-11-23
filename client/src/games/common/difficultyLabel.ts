import type { Difficulty } from "../../api/types";

/** What a game says about its difficulty: nothing for Normal. */
export const DIFFICULTY_LABEL: Record<Difficulty, string[]> = { EASY: ["Easy"], NORMAL: [], HARD: ["Hard mode"] };
