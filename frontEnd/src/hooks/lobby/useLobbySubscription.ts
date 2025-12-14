import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { lobbyService } from "../../services/lobby/lobbyService";
import { LobbyAction } from "./lobbyReducer";
import { PlayerInfo } from "../../services/lobby/lobbyResponseTypes";

type Actions = {
    loadLobbyDetails: () => void;
    startCountdown: (seconds: number, status: 'waiting' | 'starting' | 'closing') => void;
    stopTimer: () => void;
};

export function useLobbySubscription(
    lobbyId: number | undefined,
    actions: Actions,
    dispatch: React.Dispatch<LobbyAction>
) {
    const navigate = useNavigate();

    useEffect(() => {
        if (!lobbyId) return;

        actions.loadLobbyDetails();

        const subscription = lobbyService.subscribeToLobbyEvents(
            lobbyId,
            (eventData) => {
                const { type, data } = eventData;

                switch (type) {
                    case "CONNECTED":
                        break;

                    case "PLAYER_JOINED": {
                        const newPlayer: PlayerInfo = {
                            id: data.data.userId,
                            username: data.data.userName,
                            imageUrl: data.data.imageUrl,
                        };

                        dispatch({ type: 'PLAYER_JOINED', payload: newPlayer });
                        break;
                    }

                    case "PLAYER_LEFT": {
                        const leftUserId = data.data.userId;
                        dispatch({ type: 'PLAYER_LEFT', payload: leftUserId });
                        break;
                    }

                    case "LOBBY_STARTING":
                        actions.stopTimer();
                        actions.startCountdown(3, 'starting');
                        dispatch({ type: 'LOBBY_STARTING' });
                        break;

                    case "GAME_STARTED": {
                        actions.stopTimer();

                        let gameId = data.data.gameId;

                        // Handle nested gameId structure if exists
                        if (gameId && gameId.value) {
                            gameId = gameId.value.gameId;
                        }

                        if (gameId) {
                            navigate(`/game/${Number(gameId)}`);
                        }
                        break;
                    }

                    case "LOBBY_CLOSED":
                        actions.stopTimer();
                        dispatch({
                            type: 'LOBBY_CLOSED',
                            payload: data.message
                        });
                        navigate("/lobbies");
                        break;
                }
            },
            () => {
                dispatch({ type: 'SET_ERROR', payload: "Conexão perdida com o servidor" });
                actions.stopTimer();
            }
        );

        return () => {
            subscription.close();
            actions.stopTimer();
        };
    }, [lobbyId, actions.loadLobbyDetails]);
}