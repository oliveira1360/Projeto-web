import { useState } from "react";

interface GameControlsProps {
    isPlayerTurn: boolean;
    onRoll: () => Promise<void>;
    onFinishTurn: () => Promise<void>;
}

export const GameControls = ({ isPlayerTurn, onRoll, onFinishTurn }: GameControlsProps) => {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

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

    return (
        <div className="game-controls">
            <button
                onClick={handleRoll}
                disabled={!isPlayerTurn || isLoading}
                className={!isPlayerTurn ? "button-disabled" : ""}
            >
                {isLoading ? "Aguarde..." : "Rolar"}
            </button>

            <button
                onClick={handleFinishTurn}
                disabled={!isPlayerTurn || isLoading}
                className={`btn-finish ${!isPlayerTurn ? "button-disabled" : ""}`}
            >
                {isLoading ? "Aguarde..." : "Finalizar Turno"}
            </button>

            {error && <p className="error">{error}</p>}
        </div>
    );
};