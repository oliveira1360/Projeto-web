const GameError = ({ error }: { error: string }) => (
    <div className="error-container">
        <p className="error-message">Erro volte para os lobbies </p>
        <button
            onClick={() => (window.location.href = "/lobbies")}
            className="btn"
        >
            Voltar aos Lobbies
        </button>
    </div>
);
export default GameError;
