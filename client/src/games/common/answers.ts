import { GAME_OPTIONS } from "../gameTypes";
import { GameRoundModel } from "./Model";

export function answerNames(round: GameRoundModel): string[] {
  const parsed = JSON.parse(round.jsonBlob);
  if (round.gameType.name === "TRIPOINT") {
    return parsed.countries.map((c: { name: string }) => c.name);
  }
  if (round.gameType.name === "RIVERS_BY_SHAPE") {
    return [parsed.name];
  }
  return [];
}

export function gameTypeLabel(gameTypeName: string): string {
  return GAME_OPTIONS.find(o => o.name === gameTypeName)?.singular ?? gameTypeName;
}
