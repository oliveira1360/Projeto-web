// components/lobby/LobbyElements.tsx

import React from 'react';
import { Lobby, LobbyDetailsResponse } from '../../services/lobby/lobbyService';
import { Link, useNavigate } from "react-router-dom";
import { NavBar } from '../navBar';
import '../../../LobbyRoom.css';

// Propriedades para o Card de um Lobby
interface LobbyCardProps {
    lobby: Lobby;
    isSelected: boolean;
    onSelect: (lobbyId: number) => void;
    onJoin: (lobbyId: number) => void;
}

export const LobbyCard: React.FC<LobbyCardProps> = ({
                                                        lobby,
                                                        isSelected,
                                                        onSelect,
                                                        onJoin
                                                    }) => {
    const { lobbyId, name, currentPlayers, maxPlayers } = lobby;

    return (
        <div
            key={lobbyId}
            className={`lobby-card ${isSelected ? 'selected' : ''}`}
            onClick={() => onSelect(lobbyId)}
        >
            <h3>{name}</h3>
            <p>Jogadores: **{currentPlayers}/{maxPlayers}**</p>


            <div className="card-actions">
                <button
                    onClick={(e) => {
                        e.stopPropagation();
                        onSelect(lobbyId);
                    }}
                    className="btn btn-secondary btn-small"
                >
                    {isSelected ? 'Detalhes' : 'Ver Detalhes'}
                </button>
                <button
                    onClick={(e) => {
                        e.stopPropagation();
                        onJoin(lobbyId);
                    }}
                    className="btn btn-success btn-small"
                    disabled={currentPlayers === maxPlayers}
                >
                    ENTRAR
                </button>
            </div>
        </div>
    );
};

export const LobbyHeaderControls: React.FC = () => (
    <div className="lobby-controls">
        <Link to="/home" className="btn btn-secondary">
            VOLTAR PARA HOME
        </Link>
        <Link to="/lobbyCreation" className="btn btn-primary">
            CRIAR NOVO LOBBY
        </Link>
    </div>
);

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
        <p>Jogadores Atuais: **{details.currentPlayers}/{details.maxPlayers}**</p>
    </div>
);



export const LobbyLoading: React.FC = () => {
    return (
        <div>

            <div style={{ textAlign: 'center', marginTop: '50px' }}>
                <h2>Carregando lobby...</h2>
                <div className="spinner"></div>
            </div>
        </div>
    );
};

interface LobbyErrorProps {
    error: string;
}

export const LobbyError: React.FC<LobbyErrorProps> = ({ error }) => {
    const navigate = useNavigate();

    return (
        <div>

            <div className="lobby-error-container">
                <h2>Erro ao carregar lobby</h2>
                <p>{error}</p>
                <button
                    onClick={() => navigate('/lobbies')}
                    className="btn btn-primary"
                >
                    Voltar para Lobbies
                </button>
            </div>
        </div>
    );
};

interface Player {
    userId: number;
    username: string;
}

interface LobbyRoomProps {
    lobbyId: number;
    name: string;
    currentPlayers: number;
    maxPlayers: number;
    rounds: number;
    players: Player[];
    userId: number | null;
    leaveLobby: () => void;
}

export const LobbyRoom: React.FC<LobbyRoomProps> = ({
                                                        lobbyId,
                                                        name,
                                                        currentPlayers,
                                                        maxPlayers,
                                                        rounds,
                                                        players,
                                                        userId,
                                                        leaveLobby,
                                                    }) => {
    return (
        <div>

            <div className="lobby-room-container">
                <div className="lobby-room-header">
                    <h1>{name}</h1>
                    <p className="lobby-id">Lobby #{lobbyId}</p>
                </div>

                <div className="lobby-info">
                    <div className="info-card">
                        <h3>Jogadores</h3>
                        <p className="info-value">{currentPlayers} / {maxPlayers}</p>
                    </div>
                    <div className="info-card">
                        <h3>Rondas</h3>
                        <p className="info-value">{rounds}</p>
                    </div>
                </div>

                <div className="players-section">
                    <h2>Jogadores no Lobby</h2>
                    <div className="players-grid">
                        {players.map((player) => (
                            <div
                                key={player.userId}
                                className={`player-card ${player.userId === userId ? 'current-user' : ''}`}
                            >
                                <div className="player-avatar">
                                    {player.username.charAt(0).toUpperCase()}
                                </div>
                                <p className="player-name">{player.username}</p>
                                {player.userId === userId && (
                                    <span className="you-badge">Você</span>
                                )}
                            </div>
                        ))}

                        {/* Slots vazios */}
                        {Array.from({ length: maxPlayers - currentPlayers }).map((_, idx) => (
                            <div key={`empty-${idx}`} className="player-card empty">
                                <div className="player-avatar empty">?</div>
                                <p className="player-name">Aguardando...</p>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="lobby-status">
                    {currentPlayers < 2 ? (
                        <div className="status-message waiting">
                            <p>⏳ Aguardando mais jogadores...</p>
                            <p className="status-detail">
                                Necessário pelo menos 2 jogadores para iniciar
                            </p>
                        </div>
                    ) : (
                        <div className="status-message ready">
                            <p>✓ Aguardando o servidor iniciar o jogo...</p>
                            <p className="status-detail">
                                O jogo iniciará automaticamente quando estiver pronto
                            </p>
                        </div>
                    )}
                </div>

                <div className="lobby-actions">
                    <button
                        onClick={leaveLobby}
                        className="btn btn-danger"
                    >
                        Sair do Lobby
                    </button>
                </div>
            </div>
        </div>
    );
};