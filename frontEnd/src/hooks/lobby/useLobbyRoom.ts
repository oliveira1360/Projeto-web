import { useReducer } from "react";
import { lobbyReducer, initialState } from "./lobbyReducer";
import { useLobbyActions } from "./useLobbyActions";
import { useLobbySubscription } from "./useLobbySubscription";

export function useLobbyRoom(lobbyId: number | undefined) {
    const [state, dispatch] = useReducer(lobbyReducer, {
        ...initialState,
        lobbyId: lobbyId ?? 0
    });

    const actions = useLobbyActions(lobbyId, dispatch);

    useLobbySubscription(lobbyId, actions, dispatch);

    return {
        lobbyId: state.lobbyId,
        name: state.name,
        maxPlayers: state.maxPlayers,
        rounds: state.rounds,
        players: state.players,
        loading: state.loading,
        error: state.error,
        timeRemaining: state.timeRemaining,
        timerStatus: state.timerStatus,
        minPlayersToStart: state.minPlayersToStart,
        leaveLobby: actions.leaveLobby
    };
}