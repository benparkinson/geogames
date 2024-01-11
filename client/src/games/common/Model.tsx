export class GameRoundModel {
    jsonBlob: string;
    totalRoundCount: number;
    gameType: string;
    answerState: string;
}

export class Round {
    nextRound: () => void;
    prevRound: () => void;
    hasPreviousRound: boolean;
    hasNextRound: boolean;
    currentRound: number;
    totalRoundCount: number;
    answerState: string;

    constructor(nextRound: () => void, prevRound: () => void, hasPreviousRound: boolean, hasNextRound: boolean, currentRound: number, totalRoundCount: number, answerState: string) {
        this.nextRound = nextRound;
        this.prevRound = prevRound;
        this.hasPreviousRound = hasPreviousRound;
        this.hasNextRound = hasNextRound;
        this.currentRound = currentRound;
        this.totalRoundCount = totalRoundCount;
        this.answerState = answerState;
    }
}