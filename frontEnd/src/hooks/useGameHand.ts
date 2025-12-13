import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { gameService } from "../services/game/gameService";

export interface Die {
    value: string;
    held: boolean;
}

export function useGameHand(gameId?: number) {
    const [hand, setHand] = useState<Die[]>([]);
    const [rollNumber, setRollNumber] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    const loadHand = async () => {
        if (!gameId) return;
        try {
            setLoading(true);
            setError(null);

            const handData = await gameService.getPlayerHand(gameId);
            setHand(handData.hand.map((cardValue) => ({
                value: cardValue,
                held: false
            })));
        } catch (e: any) {
            if (e.code === "USER_NOT_IN_GAME") {
                navigate("/home", { replace: true });
                return;
            }
            setHand([]);
        } finally {
            setLoading(false);
        }
    };

    const toggleHold = (index: number) => {
        setHand((prev) =>
            prev.map((d, i) => (i === index ? { ...d, held: !d.held } : d))
        );
    };

    const rollDice = async () => {
        if (!gameId) return;
        try {
            const locked = hand
                .map((d, i) => (d.held ? i : -1))
                .filter((i) => i !== -1);

            const newHandData = await gameService.shuffle(gameId, locked);
            setHand(newHandData.hand.map((v) => ({ value: v, held: false })));
            setRollNumber(newHandData.rollNumber);
        } catch (e: any) {
            setError(e.message);
            throw e;
        }
    };

    const resetHand = () => {
        setHand([]);
        setRollNumber(0);
    };

    useEffect(() => {
        if (gameId) {
            loadHand();
        }
    }, [gameId]);

    return {
        hand,
        rollNumber,
        loading,
        error,
        toggleHold,
        rollDice,
        loadHand,
        resetHand,
        setHand
    };
}