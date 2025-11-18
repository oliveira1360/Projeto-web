import { useEffect, useState } from "react";
import { gameService } from "../services/game/gameService";
import { Player, RoundInfoResponse } from "../services/game/responsesType";

export interface Die {
    value: string;
    held: boolean;
}


export function useGame(gameId?: number, userId?: number) {
    const [players, setPlayers] = useState<Player[]>([]);
    const [hand, setHand] = useState<Die[]>([]);
    const [currentRound, setCurrentRound] = useState(0);
    const [totalRounds, setTotalRounds] = useState(0);
    const [gameStatus, setGameStatus] = useState<"ACTIVE" | "FINISHED">("ACTIVE");
    const [winner, setWinner] = useState<Player | null>(null);
    const [roundWinner, setRoundWinner] = useState<any>(null);
    const [remainingTime, setRemainingTime] = useState<number>(0);
    const [waitingForRound, setWaitingForRound] = useState(false);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [isMyTurn, setIsMyTurn] = useState(false);

    const loadGameWinner = async () => {
        if (!gameId) return;
        try {
            const gameWinner = await gameService.getGameWinner(gameId);
            setWinner({
                playerId: gameWinner.winner.playerId,
                username: players.find(p => p.playerId === gameWinner.winner.playerId)?.username || "Unknown",
                points: gameWinner.winner.totalPoints,
            });
        } catch (e: any) {
            console.error("Error loading game winner:", e);
        }
    };

    const loadGameState = async () => {
        if (!gameId || !userId) return;
        try {
            setLoading(true);
            setError(null);

            try {
                const playersData = await gameService.listPlayers(gameId);
                setPlayers(playersData.players);
            } catch (e: any) {
                console.error("!!! ERRO AO CARREGAR Jogadores:", e.message, e);
                setPlayers([]);
            }

            try {
                const handData = await gameService.getPlayerHand(gameId);
                setHand(handData.hand.map((cardValue) => ({
                    value: cardValue,
                    held: false
                })));
            } catch (e: any) {
                console.warn("Não foi possível carregar a mão (normal se a ronda não começou):", e.message);
                setHand([]);
            }

            try {
                const roundInfo: RoundInfoResponse = await gameService.getRoundInfo(gameId);

                setCurrentRound(roundInfo.round);
                setTotalRounds(roundInfo.players || 0);

                const nextPlayerId = roundInfo.turn;
                setIsMyTurn(nextPlayerId === userId);

            } catch (e: any) {
                console.error("!!! ERRO AO CARREGAR RoundInfo:", e.message, e);
            }
        } catch (e: any) {
            console.error("!!! ERRO GERAL no loadGameState:", e.message, e);
            setError(e.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        console.log("loadGameState called");
        if (!gameId || !userId) return;

        const stream = gameService.subscribeToGameEvents(
            gameId,
            async (eventType, data) => {
                console.log("Received event:", eventType, data);
                switch (eventType) {
                    case "PLAYER_FINISHED_TURN":
                        console.log("Player finished turn:", data);
                        const roundInfo = await gameService.getRoundInfo(gameId);
                        setCurrentRound(roundInfo.round);
                        setTotalRounds(roundInfo.players || 0);
                        setIsMyTurn(roundInfo.turn === userId);
                        break;
                    case "ROUND_STARTED":
                    case "ROUND_ENDED":
                        loadGameState();
                        break;
                    case "GAME_ENDED":
                        setGameStatus("FINISHED");
                        loadGameWinner();
                        break;
                    case "connected":
                        loadGameState();
                        break;
                }
            },
            (error) => setError(error.message)
        );

        return () => stream.close();
    }, [gameId, userId]);

    useEffect(() => {
        if (gameId && userId) {
            loadGameState();
        }
    }, [gameId, userId]);


    const toggleHold = (index: number) => {
        setHand((prev) =>
            prev.map((d, i) => (i === index ? { ...d, held: !d.held } : d))
        );
    };

    const rollDice = async () => {
        if (!gameId || !isMyTurn) return;
        try {
            const locked = hand
                .map((d, i) => (d.held ? i : -1))
                .filter((i) => i !== -1);

            const newHandData = await gameService.shuffle(gameId, locked);
            setHand(newHandData.hand.map((v) => ({ value: v, held: false })));
        } catch (e: any) {
            setError(e.message);
        }
    };

    const finishTurn = async () => {
        if (!gameId || !isMyTurn) return;
        try {
            await gameService.finishTurn(gameId);
            setIsMyTurn(false);
        } catch (e: any) {
            setError(e.message);
        }
    };

    const startRound = async () => {
        if (!gameId) return;
        try {
            await gameService.startRound(gameId);
        } catch (e: any) {
            setError(e.message);
        }
    };

    const leaveGame = async () => {
        if (!gameId) return;
        try {
            await gameService.closeGame(gameId);
            window.location.href = "/lobbies";
        } catch (e: any) {
            setError(e.message);
        }
    };

    return {
        players,
        hand,
        currentRound,
        totalRounds,
        loading,
        error,
        gameStatus,
        winner,
        roundWinner,
        remainingTime,
        waitingForRound,
        isMyTurn,
        toggleHold,
        rollDice,
        finishTurn,
        startRound,
        leaveGame,
    };
}

export default useGame;