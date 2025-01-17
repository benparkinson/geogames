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
}

export class GameTypeModel {
    name: string;
    answerCount: number;
}

export class Round {
    nextRound: () => void;
    prevRound: () => void;
    hasPreviousRound: boolean;
    hasNextRound: boolean;
    currentRound: number;
    totalRoundCount: number;
    answerState: string;
    score: number;
    gameCompleted: boolean;

    constructor(nextRound: () => void, prevRound: () => void, hasPreviousRound: boolean, hasNextRound: boolean, currentRound: number, totalRoundCount: number, answerState: string, score: number, gameCompleted: boolean) {
        this.nextRound = nextRound;
        this.prevRound = prevRound;
        this.hasPreviousRound = hasPreviousRound;
        this.hasNextRound = hasNextRound;
        this.currentRound = currentRound;
        this.totalRoundCount = totalRoundCount;
        this.answerState = answerState;
        this.score = score;
        this.gameCompleted = gameCompleted;
    }
}