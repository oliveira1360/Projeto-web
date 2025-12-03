// pages/LobbyCreationPage.tsx

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { lobbyService } from '../services/lobby/lobbyService';

const MAX_TOTAL_LIMIT = 60;

const LobbyCreationPage: React.FC = () => {
    const [name, setName] = useState('');
    const [maxPlayers, setMaxPlayers] = useState(4);
    const [rounds, setRounds] = useState(5);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Hook para navegação programática
    const navigate = useNavigate();

    // Sempre que 'maxPlayers' muda, recalcula e ajusta 'rounds' se o limite for excedido.
    useEffect(() => {
        // Calcula o máximo de rondas permitido para o maxPlayers atual
        const maxRoundsAllowed = Math.floor(MAX_TOTAL_LIMIT / maxPlayers);

        // Se o valor atual de 'rounds' exceder o novo máximo permitido, ajusta
        if (rounds > maxRoundsAllowed) {
            setRounds(maxRoundsAllowed);
        }
    }, [maxPlayers, rounds]); // Depende de maxPlayers e rounds

    // Calcula o limite máximo de rondas que o input pode aceitar
    const maxRounds = Math.floor(MAX_TOTAL_LIMIT / maxPlayers);

    const handleGoBack = () => {
        navigate('/lobbies');
    };

    const handleMaxPlayersChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newMaxPlayers = Number(e.target.value);
        if (newMaxPlayers < 2 || newMaxPlayers > 8) return; // Garante que está nos limites do input HTML

        setMaxPlayers(newMaxPlayers);

    };

    const handleRoundsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newRounds = Number(e.target.value);

        // Garante que o input não é negativo (embora 'min="1"' já ajude)
        if (newRounds >= 1) {
            setRounds(newRounds);
        }
    };


    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (maxPlayers * rounds > MAX_TOTAL_LIMIT) {
            setError(`O produto de Jogadores (${maxPlayers}) e Rondas (${rounds}) não pode exceder ${MAX_TOTAL_LIMIT}.`);
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const result = await lobbyService.createLobby(name, maxPlayers, rounds);
            alert(`Lobby '${result.name}' criado com sucesso! ID: ${result.lobbyId}`);
            navigate('/lobby/' + result.lobbyId);

        } catch (e: any) {
            console.error("Creation Error:", e);
            setError(`Failed to create lobby: ${e.message}`);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="lobby-creation-container lobby-page">
            <h2>Criar Novo Lobby</h2>

            {error && <p className="error-message">{error}</p>}

            <form onSubmit={handleSubmit} className="creation-form">
                <label>
                    Nome do Lobby:
                    <input
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                    />
                </label>

                <label>
                    Máximo de Jogadores:
                    <input
                        type="number"
                        value={maxPlayers}
                        onChange={handleMaxPlayersChange}
                        min="2"
                        max="8"
                        required
                        onKeyDown={(e) => {
                            // Permite apenas as teclas de navegação (setas, tab, etc.)
                            // Bloqueia a digitação de caracteres
                            if (e.key.length === 1 && !e.ctrlKey && !e.metaKey) {
                                e.preventDefault();
                            }
                        }}
                    />
                </label>

                <label>
                    Número de Rondas (Máx: {maxRounds}):
                    <input
                        type="number"
                        value={rounds}
                        onChange={handleRoundsChange}
                        min="1"
                        max={maxRounds}
                        required
                        onKeyDown={(e) => {
                            // Permite apenas as teclas de navegação (setas, tab, etc.)
                            // Bloqueia a digitação de caracteres
                            if (e.key.length === 1 && !e.ctrlKey && !e.metaKey) {
                                e.preventDefault();
                            }
                        }}
                    />
                </label>
                <button type="submit" className="btn btn-primary" disabled={loading}>

                    {loading ? 'A Criar...' : 'Criar Lobby'}

                </button>
            </form>
            <h2></h2>
            <button
                onClick={handleGoBack}
                className="btn btn-secondary btn-back-to-lobbies"
            >
                VOLTAR AOS LOBBIES
            </button>
        </div>
    );
};

export default LobbyCreationPage;