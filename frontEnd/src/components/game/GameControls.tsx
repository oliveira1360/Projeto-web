import React from "react";

interface GameControlsProps {
    onRoll: () => void;
    onFinish: () => void;
    disabled: boolean;
}

export const GameControls: React.FC<GameControlsProps> = ({onRoll, onFinish, disabled,}) => (
    <div className="controls-area">
        <button onClick={onRoll} disabled={disabled} className="btn btn-roll">
            ROLL
        </button>
        <button onClick={onFinish} disabled={disabled} className="btn btn-finish">
            FINISH ROUND
        </button>
    </div>
);
