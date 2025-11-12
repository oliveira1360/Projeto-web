import React from "react";
import { Die } from "../../hooks/useGame";

interface DiceProps {
    hand: Die[];
    isMyTurn: boolean;
    onToggleHold: (index: number) => void;
}

export const Dice: React.FC<DiceProps> = ({ hand, isMyTurn, onToggleHold }) => (
    <div className="dice-area">
        {hand.length > 0 ? (
            hand.map((die, i) => (
                <div
                    key={i}
                    onClick={() => onToggleHold(i)}
                    className={`die ${isMyTurn ? "my-turn" : ""} ${
                        die.held ? "held" : ""
                    }`}
                >
                    <span className="fallback-text">{die.value}</span>
                </div>
            ))
        ) : (
            <p className="no-dice-message">No dice yet — Roll to start!</p>
        )}
    </div>
);
