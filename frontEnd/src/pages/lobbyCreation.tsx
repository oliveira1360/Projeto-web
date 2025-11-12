// pages/LobbyCreationPage.tsx

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { lobbyService } from '../services/lobby/lobbyService'; // Importe o service

const LobbyCreationPage: React.FC = () => {
    const [name, setName] = useState('');
    const [maxPlayers, setMaxPlayers] = useState(4);
    const [rounds, setRounds] = useState(5);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Hook para navegação programática
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        try {
            // 🚨 Chamada ao serviço de criação
            const result = await lobbyService.createLobby(name, maxPlayers, rounds);

            // Se for bem-sucedido, redireciona o utilizador para a página do lobby criado
            // Ou para a página principal de listagem de lobbies
            alert(`Lobby '${result.name}' criado com sucesso! ID: ${result.lobbyId}`);

            // Exemplo de redirecionamento para a página principal de lobbies
            navigate('/lobbies');

        } catch (e: any) {
            console.error("Creation Error:", e);
            setError(`Failed to create lobby: ${e.message}`);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="lobby-creation-container">
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
                        onChange={(e) => setMaxPlayers(Number(e.target.value))}
                        min="2"
                        max="8" // Exemplo de limite
                        required
                    />
                </label>

                <label>
                    Número de Rondas:
                    <input
                        type="number"
                        value={rounds}
                        onChange={(e) => setRounds(Number(e.target.value))}
                        min="1"
                        required
                    />
                </label>

                <button type="submit" className="btn btn-primary" disabled={loading}>
                    {loading ? 'A Criar...' : 'Criar Lobby'}
                </button>
            </form>
        </div>
    );
};

export default LobbyCreationPage;