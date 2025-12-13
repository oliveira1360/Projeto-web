import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { gameService } from "../services/game/gameService";

interface GameEventHandlers {
    onPlayerFinishedTurn?: (data: any) => void;
    onRoundStarted?: (data: any) => void;
    onRoundEnded?: (data: any) => void;
    onPlayerLeave?: (data: any) => void;
    onGameEnded?: (data: any) => void;
    onConnected?: () => void;
    onError?: (error: Error) => void;
}

export function useGameEvents(
    gameId: number | undefined,
    userId: number | undefined,
    handlers: GameEventHandlers
) {
    const navigate = useNavigate();

    useEffect(() => {
        if (!gameId || !userId) return;

        const stream = gameService.subscribeToGameEvents(
            gameId,
            async (eventType, data) => {

                switch (eventType) {
                    case "PLAYER_FINISHED_TURN":
                        handlers.onPlayerFinishedTurn?.(data);
                        break;

                    case "ROUND_STARTED":
                        handlers.onRoundStarted?.(data);
                        break;

                    case "ROUND_ENDED":
                        handlers.onRoundEnded?.(data);
                        break;

                    case "PLAYER_LEAVE":
                        const removedPlayerId = data.data?.removedPlayerId;
                        if (removedPlayerId === userId) {
                            navigate("/lobbies", { replace: true });
                            return;
                        }
                        handlers.onPlayerLeave?.(data);
                        break;

                    case "GAME_ENDED":
                        handlers.onGameEnded?.(data);
                        break;

                    case "connected":
                        handlers.onConnected?.();
                        break;
                }
            },
            (error) => {
                handlers.onError?.(error);
            }
        );

        return () => stream.close();
    }, [gameId, userId]);
}