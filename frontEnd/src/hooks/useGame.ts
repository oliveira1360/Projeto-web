import { useEffect, useState } from "react";
import { gameService } from "../services/game/gameService";

export interface Player {
    playerId: number;
    username?: string;
    points?: number;
    hand?: string[];
}

export interface Die {
    value: string;
    held: boolean;
}

export function useGame(gameId?: number) {
    const [players, setPlayers] = useState<Player[]>([]);
    const [hand, setHand] = useState<Die[]>([]);
    const [isMyTurn, setIsMyTurn] = useState(true);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!gameId) return;

        const loadGame = async () => {
            try {
                setLoading(true);
                const playersData = await gameService.listPlayers(gameId);

                const mappedPlayers: Player[] = playersData.map((p) => ({
                    playerId: p.playerId,
                    username: p.name,
                    points: p.balance,
                }));

                setPlayers(mappedPlayers);

                const myPlayer = mappedPlayers[0]; // example: first player
                const myHandValues = (myPlayer as any).hand?.value || [];
                setHand(myHandValues.map((v: string) => ({ value: v, held: false })));
            } catch (e: any) {
                setError(e.message);
            } finally {
                setLoading(false);
            }
        };

        loadGame();
    }, [gameId]);

    const toggleHold = (index: number) => {
        if (!isMyTurn) return;
        setHand((prev) =>
            prev.map((d, i) => (i === index ? { ...d, held: !d.held } : d))
        );
    };

    const rollDice = async () => {
        if (!isMyTurn || !gameId) return;
        const locked = hand
            .map((d, i) => (d.held ? i : -1))
            .filter((i) => i !== -1);
        const newHand = await gameService.shuffle(gameId, locked);
        setHand(newHand.map((v) => ({ value: v, held: false })));
    };

    const finishTurn = async () => {
        if (!isMyTurn || !gameId) return;
        await gameService.finishTurn(gameId);
    };

    return {
        players,
        hand,
        isMyTurn,
        loading,
        error,
        toggleHold,
        rollDice,
        finishTurn,
    };
}
