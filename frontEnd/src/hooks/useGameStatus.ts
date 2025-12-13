import { useState } from "react";
import { gameService } from "../services/game/gameService";
import { Player } from "../services/game/responsesType";

export function useGameStatus(gameId?: number) {
    const [gameStatus, setGameStatus] = useState<"ACTIVE" | "FINISHED">("ACTIVE");
    const [winner, setWinner] = useState<Player | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const loadGameWinner = async () => {
        if (!gameId) return;
        try {
            setLoading(true);
            setError(null);

            const playersData = await gameService.listPlayers(gameId);
            const gameWinner = await gameService.getGameWinner(gameId);
            const player = playersData.players.find(
                p => p.playerId === gameWinner.winner.playerId
            );
            setWinner({
                playerId: gameWinner.winner.playerId,
                username: player?.username || "Unknown",
                points: gameWinner.winner.totalPoints,
            });
        } catch (e: any) {
            console.error("Error loading game winner:", e);
            setError(e.message);
        } finally {
            setLoading(false);
        }
    };

    const leaveGame = async () => {
        if (!gameId) return;
        try {
            await gameService.closeGame(gameId);
            window.location.href = "/lobbies";
        } catch (e: any) {
            setError(e.message);
            throw e;
        }
    };

    return {
        gameStatus,
        winner,
        loading,
        error,
        loadGameWinner,
        leaveGame,
        setGameStatus
    };
}