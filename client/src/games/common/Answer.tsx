import React from 'react';

interface AnswerProps {
    answer: string;
    isCorrect: boolean;
}

const Answer: React.FC<AnswerProps> = ({ answer, isCorrect }) => {
    const className = isCorrect ? 'text-success' : 'text-danger';
    return (
        <div className="m-1">
            {answer} <span className={className}>{isCorrect ? "✓" : "✗"}</span>
        </div>
    );
};

export default Answer;
