import * as React from "react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { useLobbyRoom } from "../hooks/lobby/useLobbyRoom";
import { playerService } from "../services/player/playerService";
import { PlayerInfoResponse } from "../services/player/playerResponseTypes";
import { LobbyLoading } from "../components/Lobby/LobbyLoading";
import { LobbyError } from "../components/Lobby/LobbyError";
import { LobbyRoom } from "../components/Lobby/LobbyRoom";
import { LobbyTimer } from "../components/Lobby/LobbyTimer";

function LobbyPage() {
    const { lobbyId } = useParams<{ lobbyId: string }>();
    const [userInfo, setUserInfo] = useState<PlayerInfoResponse | null>(null);

    useEffect(() => {
        const load = async () => {
            const info = await playerService.playerInfo();
            setUserInfo(info);
        };
        load();
    }, []);

    const userId = userInfo?.userId ?? null;

    const {
        lobbyId: currentLobbyId,
        name,
        maxPlayers,
        rounds,
        players,
        loading,
        error,
        timeRemaining,
        timerStatus,
        minPlayersToStart,
        leaveLobby,
    } = useLobbyRoom(lobbyId ? Number(lobbyId) : undefined);

    if (!userInfo) return <LobbyLoading />;
    if (loading) return <LobbyLoading />;
    if (error) return <LobbyError error={error} />;

    return (
        <div>


            <LobbyRoom
                lobbyId={currentLobbyId}
                name={name}
                maxPlayers={maxPlayers}
                rounds={rounds}
                players={players}
                userId={userId}
                leaveLobby={leaveLobby}
            />
            <LobbyTimer
                timeRemaining={timeRemaining}
                timerStatus={timerStatus}
                currentPlayersCount={players.length}
                minPlayersToStart={minPlayersToStart}
                maxPlayers={maxPlayers}
            />
        </div>
    );
}

export default LobbyPage;