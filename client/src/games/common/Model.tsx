export class GameRoundModel {
    jsonBlob: string;
    totalRoundCount: number;
    gameType: GameTypeModel;
    answerState: string;
    gameResult: GameResultModel;
}

export class GameResultModel {
    gameState: string;
    roundsUnanswered: number;
    score: number;
    roundAnswerStates: string[];
}

export class GameTypeModel {
    name: string;
    answerCount: number;
}

export class Round {
    nextRound: () => void;
    goToRound: (round: number) => void;
    showSummary: () => void;
    hasNextRound: boolean;
    currentRound: number;
    totalRoundCount: number;
    answerState: string;
    score: number;
    gameCompleted: boolean;
    roundAnswerStates: string[];

    constructor(init: Round) {
        Object.assign(this, init);
    }
}

export const CORRECT_ANSWER: string = "CORRECT";
export const GAVE_UP: string = "GAVE_UP";
export const UNANSWERED: string = "UNANSWERED";
