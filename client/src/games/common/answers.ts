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
  switch (gameTypeName) {
    case "TRIPOINT":
      return "Tripoint";
    case "RIVERS_BY_SHAPE":
      return "River";
    default:
      return gameTypeName;
  }
}
