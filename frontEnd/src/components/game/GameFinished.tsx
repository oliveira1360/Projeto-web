import Scoreboard from "./Scoreboard";
// @ts-ignore
import backgroundImage from "../../img/pokertable.jpg";

// @ts-ignore
const GameFinished = ({ winner, players, leaveGame }) => (
    <div
        className="game-container finished"
        style={{ backgroundImage: `url(${backgroundImage})` }}
    >
        <div className="winner-modal">
            <h1 className="winner-title"> JOGO FINALIZADO </h1>

            <div className="winner-info">
                <p className="winner-name">
                    Vencedor: <strong>{winner.username}</strong>
                </p>
                <p className="winner-points">
                    Pontos Totais: <strong>{winner.points}</strong>
                </p>
            </div>

            <div className="final-scoreboard">
                <h2>Placar Final</h2>
                <Scoreboard players={players} />
            </div>

            <button onClick={leaveGame} className="btn btn-large">
                Voltar aos Lobbies
            </button>
        </div>
    </div>
);

export default GameFinished;
