import { useState } from "react";

interface GameControlsProps {
    isPlayerTurn: boolean;
    onRoll: () => Promise<void>;
}

export const GameControls = ({ isPlayerTurn, onRoll }: GameControlsProps) => {
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

    return (
        <div className="game-controls">
            <button
                onClick={handleRoll}
                disabled={!isPlayerTurn || isLoading}
                className={!isPlayerTurn ? "button-disabled" : ""}
            >
                {isLoading ? "Rolando..." : isPlayerTurn ? "Rolar" : "À espera da sua vez..."}
            </button>

            {error && <p className="error">{error}</p>}
        </div>
    );
};