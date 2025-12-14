import React from "react";
import {PlayerInfo} from "../../services/lobby/lobbyResponseTypes";

interface LobbyRoomProps {
    lobbyId: number;
    name: string;
    maxPlayers: number;
    rounds: number;
    players: PlayerInfo[];
    userId: number | null;
    leaveLobby: () => void;
}

export const LobbyRoom: React.FC<LobbyRoomProps> = ({
                                                        lobbyId,
                                                        name,
                                                        maxPlayers,
                                                        rounds,
                                                        players,
                                                        userId,
                                                        leaveLobby,
                                                    }) => {
    const currentPlayersCount = players.length;

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
                        <p className="info-value">{currentPlayersCount} / {maxPlayers}</p>
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
                                key={player.id}
                                className={`player-card ${player.id === userId ? 'current-user' : ''}`}
                            >
                                <div className="player-avatar">
                                    {player.imageUrl ? (
                                        <img
                                            src={player.imageUrl}
                                            alt={player.username}
                                            style={{
                                                width: '100%',
                                                height: '100%',
                                                objectFit: 'cover',
                                                borderRadius: '50%'
                                            }}
                                            onError={(e) => {
                                                // Se a imagem falhar, esconder e mostrar a inicial
                                                const img = e.currentTarget;
                                                img.style.display = 'none';
                                                // Criar div com inicial
                                                const parent = img.parentElement;
                                                if (parent && !parent.querySelector('.player-initial')) {
                                                    const initial = document.createElement('div');
                                                    initial.className = 'player-initial';
                                                    initial.textContent = player.username.charAt(0).toUpperCase();
                                                    initial.style.cssText = `
                                                        position: absolute;
                                                        top: 0;
                                                        left: 0;
                                                        width: 100%;
                                                        height: 100%;
                                                        display: flex;
                                                        align-items: center;
                                                        justify-content: center;
                                                        font-size: 2em;
                                                        color: white;
                                                        font-weight: bold;
                                                    `;
                                                    parent.appendChild(initial);
                                                }
                                            }}
                                        />
                                    ) : (
                                        // Fallback: mostrar inicial do nome
                                        player.username.charAt(0).toUpperCase()
                                    )}
                                </div>
                                <p className="player-name">{player.username}</p>
                                {player.id === userId && (
                                    <span className="you-badge">Você</span>
                                )}
                            </div>
                        ))}

                        {/* Slots vazios */}
                        {Array.from({ length: maxPlayers - currentPlayersCount }).map((_, idx) => (
                            <div key={`empty-${idx}`} className="player-card empty">
                                <div className="player-avatar empty">?</div>
                                <p className="player-name">Aguardando...</p>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="lobby-status">
                    {currentPlayersCount < 2 ? (
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