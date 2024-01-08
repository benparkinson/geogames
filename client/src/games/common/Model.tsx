export class GameRoundModel {
    jsonBlob: string;
    totalRoundCount: number;
    gameType: string;
}

export class Round {
    nextRound: () => void;
    prevRound: () => void;
    hasPreviousRound: boolean;
    hasNextRound: boolean;
    currentRound: number;
    totalRoundCount: number;

    constructor(nextRound: () => void, prevRound: () => void, hasPreviousRound: boolean, hasNextRound: boolean, currentRound: number, totalRoundCount: number) {
        this.nextRound = nextRound;
        this.prevRound = prevRound;
        this.hasPreviousRound = hasPreviousRound;
        this.hasNextRound = hasNextRound;
        this.currentRound = currentRound;
        this.totalRoundCount = totalRoundCount;
    }
}