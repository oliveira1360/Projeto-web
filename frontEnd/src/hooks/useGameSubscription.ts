import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { gameService } from "../services/game/gameService";
import { GameAction } from "./gameReducer";

type Actions = {
    loadPlayers: () => void;
    loadHand: () => void;
    loadRoundInfo: () => void;
};

export function useGameSubscription(
    gameId: number | undefined,
    userId: number | undefined,
    actions: Actions,
    dispatch: React.Dispatch<GameAction>
) {
    const navigate = useNavigate();

    useEffect(() => {
        if (!gameId || !userId) return;

        actions.loadPlayers();
        actions.loadHand();
        actions.loadRoundInfo();

        const stream = gameService.subscribeToGameEvents(
            gameId,
            async (eventType, data) => {
                switch (eventType) {
                    case "PLAYER_FINISHED_TURN":
                        actions.loadRoundInfo();
                        break;

                    case "ROUND_STARTED":
                        const roundData = await gameService.getRoundInfo(gameId);
                        actions.loadPlayers();
                        dispatch({ type: 'ROUND_STARTED', payload: { info: roundData, userId } });
                        break;

                    case "ROUND_ENDED":
                        dispatch({ type: 'ROUND_ENDED', payload: data.data.winner });
                        break;

                    case "PLAYER_LEAVE":
                        if (data.data?.removedPlayerId === userId) {
                            navigate("/lobbies", { replace: true });
                        } else {
                            actions.loadPlayers();
                        }
                        break;

                    case "GAME_ENDED":
                        const playersData = await gameService.listPlayers(gameId);
                        const winnerData = await gameService.getGameWinner(gameId);
                        const winnerName = playersData.players.find(p => p.playerId === winnerData.winner.playerId)?.username || "Unknown";

                        dispatch({
                            type: 'GAME_ENDED',
                            payload: { winnerName, points: winnerData.winner.totalPoints }
                        });
                        break;
                }
            },
            (err) => console.error("SSE Error", err)
        );

        return () => stream.close();
    }, [gameId, userId, actions.loadPlayers, actions.loadHand, actions.loadRoundInfo]);
}