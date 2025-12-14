import {LobbyDetailsResponse} from "../../services/lobby/lobbyResponseTypes";
import React from "react";

interface LobbyDetailsProps {
    details: LobbyDetailsResponse;
    onLeave: (lobbyId: number) => void;
}

export const LobbyDetails: React.FC<LobbyDetailsProps> = ({
                                                              details,
                                                              onLeave,
                                                          }) => (
    <div className="details-panel">
        <h3>Detalhes do Lobby (ID: {details.lobbyId})</h3>
        <p>Nome: **{details.name}**</p>
        <p>Rondas: **{details.rounds}**</p>
        <p>Jogadores Atuais: **{details.currentPlayers.length}/{details.maxPlayers}**</p>
    </div>
);