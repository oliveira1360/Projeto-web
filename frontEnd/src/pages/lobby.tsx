import * as React from "react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { useLobbyRoom } from "../hooks/useLobbyRoom";
import { playerService } from "../services/player/playerService";
import { PlayerInfoResponse } from "../services/player/playerResponseTypes";
import {LobbyLoading} from "../components/Lobby/LobbyLoading";
import {LobbyError} from "../components/Lobby/LobbyError";
import {LobbyRoom} from "../components/Lobby/LobbyRoom";

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
        currentPlayers,
        maxPlayers,
        rounds,
        players,
        loading,
        error,
        leaveLobby,
    } = useLobbyRoom(lobbyId ? Number(lobbyId) : undefined, userId ?? undefined);

    if (!userInfo) return <LobbyLoading />;
    if (loading) return <LobbyLoading />;
    if (error) return <LobbyError error={error} />;

    return (
        <LobbyRoom
            lobbyId={currentLobbyId}
            name={name}
            currentPlayers={currentPlayers}
            maxPlayers={maxPlayers}
            rounds={rounds}
            players={players}
            userId={userId}
            leaveLobby={leaveLobby}
        />
    );
}

export default LobbyPage;