import React from "react";

export interface Die {
    value: string;
    held: boolean;
}

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
                    onClick={() => isMyTurn && onToggleHold(i)}
                    className={`die ${isMyTurn ? "my-turn" : ""} ${
                        die.held ? "held" : ""
                    }`}
                    title={isMyTurn ? "Clique para manter" : "Aguardando sua vez"}
                >
                    <span className="fallback-text">{die.value}</span>
                </div>
            ))
        ) : (
            <p className="no-dice-message">Sem dados — Role para começar!</p>
        )}
    </div>
);