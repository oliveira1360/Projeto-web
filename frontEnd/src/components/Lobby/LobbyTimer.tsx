import React from "react";

interface LobbyTimerProps {
    timeRemaining: number | null;
    timerStatus: 'waiting' | 'starting' | 'closing' | null;
    currentPlayersCount: number;
    minPlayersToStart: number;
    maxPlayers: number;
}

export const LobbyTimer: React.FC<LobbyTimerProps> = ({
                                                          timeRemaining,
                                                          timerStatus,
                                                          currentPlayersCount,
                                                          minPlayersToStart,
                                                          maxPlayers
                                                      }) => {
    if (timeRemaining === null || timerStatus === null) {
        return null;
    }

    const formatTime = (seconds: number): string => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    const getTimerContent = () => {
        switch (timerStatus) {
            case 'waiting': {
                const playersNeeded = minPlayersToStart - currentPlayersCount;
                const isFull = currentPlayersCount >= maxPlayers;

                if (isFull) {
                    return {
                        className: 'lobby-full',
                        icon: '✓',
                        message: 'Lobby cheio! Aguardando início...'
                    };
                } else if (currentPlayersCount >= minPlayersToStart) {
                    return {
                        className: 'ready-to-start',
                        icon: '⏱️',
                        message: `Jogo inicia em ${formatTime(timeRemaining)} ou quando o lobby encher`
                    };
                } else {
                    return {
                        className: 'waiting-players',
                        icon: '⏳',
                        message: `Aguardando ${playersNeeded} ${playersNeeded === 1 ? 'jogador' : 'jogadores'}... Timeout em ${formatTime(timeRemaining)}`
                    };
                }
            }

            case 'starting':
                return {
                    className: 'game-starting',
                    icon: '🚀',
                    message: `🎮 Jogo iniciando em ${timeRemaining}s`
                };

            case 'closing':
                return {
                    className: 'lobby-closing',
                    icon: '⛔',
                    message: `⚠️ Lobby fechando em ${timeRemaining}s (jogadores insuficientes)`
                };

            default:
                return {
                    className: '',
                    icon: '',
                    message: ''
                };
        }
    };

    const { className, icon, message } = getTimerContent();

    return (
        <div className="lobby-timer-container">
            <div className={`timer-card ${className}`}>
                <div className="timer-content">
                    <span className="timer-icon">{icon}</span>
                    <span className="timer-message">{message}</span>
                </div>
            </div>
        </div>
    );
};
