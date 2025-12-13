import { useState, useEffect } from "react";
import { gameService } from "../services/game/gameService";
import { RoundInfoResponse } from "../services/game/responsesType";

export function useGameRound(gameId?: number, userId?: number) {
    const [currentRound, setCurrentRound] = useState(0);
    const [totalRounds, setTotalRounds] = useState(7);
    const [isMyTurn, setIsMyTurn] = useState(false);
    const [roundWinner, setRoundWinner] = useState<any>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const loadRoundInfo = async () => {
        if (!gameId || !userId) return;
        try {
            setLoading(true);
            setError(null);

            const roundInfo: RoundInfoResponse = await gameService.getRoundInfo(gameId);
            setCurrentRound(roundInfo.round);
            setTotalRounds(roundInfo.maxRoundNumber);

            const nextPlayerId = roundInfo.turn;
            setIsMyTurn(nextPlayerId === userId);
        } catch (e: any) {
            console.error("Error loading round info:", e);
            setError(e.message);
        } finally {
            setLoading(false);
        }
    };

    const startRound = async () => {
        if (!gameId) return;
        try {
            await gameService.startRound(gameId);
        } catch (e: any) {
            setError(e.message);
            throw e;
        }
    };

    const finishTurn = async () => {
        if (!gameId || !isMyTurn) return;
        try {
            await gameService.finishTurn(gameId);
            setIsMyTurn(false);
        } catch (e: any) {
            setError(e.message);
            throw e;
        }
    };

    useEffect(() => {
        if (gameId && userId) {
            loadRoundInfo();
        }
    }, [gameId, userId]);

    return {
        currentRound,
        totalRounds,
        isMyTurn,
        roundWinner,
        loading,
        error,
        loadRoundInfo,
        startRound,
        finishTurn,
        setCurrentRound,
        setIsMyTurn,
        setRoundWinner
    };
}