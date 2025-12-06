import { useState } from "react";

interface GameControlsProps {
    isPlayerTurn: boolean;
    onRoll: () => Promise<void>;
    onFinishTurn: () => Promise<void>;
    dices: any[];
    rollCount: number;
}

export const GameControls = ({ isPlayerTurn, onRoll, onFinishTurn, dices, rollCount }: GameControlsProps) => {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const hasRolledDice = dices && dices.length > 0;

    const isFinishDisabled = !isPlayerTurn || isLoading || !hasRolledDice;

    const handleRoll = async () => {
        if (!isPlayerTurn) {
            setError("Não é a sua vez!");
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            await onRoll();
        } catch {
            setError("Erro ao rolar os dados.");
        } finally {
            setIsLoading(false);
        }
    };

    const handleFinishTurn = async () => {
        if (!isPlayerTurn) {
            setError("Não é a sua vez!");
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            await onFinishTurn();
        } catch {
            setError("Erro ao finalizar o turno.");
        } finally {
            setIsLoading(false);
        }
    };

    const rollAvalible = rollCount  < 3
    return (
        <div className="game-controls">
            <button
                onClick={handleRoll}
                disabled={!isPlayerTurn || isLoading || !rollAvalible}
                className={!isPlayerTurn ? "button-disabled" : ""}
            >
                {isLoading ? "Aguarde..." : `Rolar ${rollCount} / 3`}
            </button>

            <button
                onClick={handleFinishTurn}
                disabled={isFinishDisabled}
                className={`btn-finish ${isFinishDisabled ? "button-disabled" : ""}`}
            >
                {isLoading ? "Aguarde..." : "Finalizar Turno"}
            </button>

            {error && <p className="error">{error}</p>}
        </div>
    );
};