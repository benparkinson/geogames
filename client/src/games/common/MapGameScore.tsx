import React from 'react';
import { Round } from './Model';

const MapGameScore: React.FC<{ round: Round }> = ({ round }) => {
    function renderScoreDescription(): string {
        if (round.gameCompleted) {
            return "Final";
        } else {
            return "Current";
        }
    }

    return (
        <div className="d-flex align-items-center">
            <div className="m-1">
                Round {round.currentRound + 1}/{round.totalRoundCount}
            </div>
            <div className="m-1">
                {renderScoreDescription()} score: {round.score}/{round.totalRoundCount}
            </div>
        </div>
    );
};

export default MapGameScore;
