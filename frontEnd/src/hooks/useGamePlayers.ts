import { useState, useEffect } from "react";
import { gameService } from "../services/game/gameService";
import { Player } from "../services/game/responsesType";

export function useGamePlayers(gameId?: number) {
    const [players, setPlayers] = useState<Player[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const loadPlayers = async (showLoading = true) => {
        if (!gameId) return;
        try {
            if (showLoading) setLoading(true);
            setError(null);

            const playersData = await gameService.listPlayers(gameId);
            setPlayers(playersData.players);
        } catch (e: any) {
            setError(e.message);
            setPlayers([]);
        } finally {
            if (showLoading) setLoading(false);
        }
    };

    useEffect(() => {
        if (gameId) {
            loadPlayers();
        }
    }, [gameId]);

    return {
        players,
        loading,
        error,
        loadPlayers,
        setPlayers
    };
}