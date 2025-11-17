import backgroundImage from "../../img/pokertable.jpg";
import { PlayerSlot } from "./PlayerPanel";
import { Dice } from "./Dice";
import Scoreboard from "./Scoreboard";
import { GameControls } from "./GameControls";

// @ts-ignore
const GameLayout = ({
                        players,
                        hand,
                        isMyTurn,
                        currentRound,
                        totalRounds,
                        remainingTime,
                        startRound,
                        rollDice,
                        finishTurn,
                        toggleHold,
                        leaveGame,
                        showRoundWinner,
                        roundWinner,
                        userId,
                    }) => (
    <div className="game-container" style={{ backgroundImage: `url(${backgroundImage})` }}>
        <div className="game-header">
            <div className="round-info">
                <span className="round-label">
                    Rodada {currentRound}/{totalRounds}
                </span>
                <span className="time-label"> {Math.floor(remainingTime / 1000)}s</span>
            </div>
        </div>

        <div className="poker-table">
            {players.map((player, i) => {
                const isThisPlayerMe = player.playerId === userId;

                return (
                    <PlayerSlot
                        key={player.playerId || i}
                        player={player}
                        isCurrentPlayer={isThisPlayerMe}
                        isActiveTurn={isMyTurn && isThisPlayerMe}
                        positionClass={`pos-${i + 1}`}
                    />
                );
            })}

            <div className="center-area">
                <div className="dice-container">
                    <h3>Seus Dados</h3>
                    <Dice hand={hand} isMyTurn={isMyTurn} onToggleHold={toggleHold} />
                </div>

                <div className="controls-container">
                    {currentRound === 0 ? (
                        <button
                            onClick={startRound}
                            className="btn btn-large btn-start"
                        >
                            INICIAR JOGO
                        </button>
                    ) : (
                        <GameControls
                            isPlayerTurn={isMyTurn}
                            onRoll={rollDice}
                        />
                    )}
                </div>

                <div className="scoreboard-small">
                    <Scoreboard players={players} />
                </div>
            </div>

            {showRoundWinner && roundWinner && (
                <div className="round-winner-modal">
                    <div className="modal-content">
                        <h2>Vencedor da ronda!</h2>
                        {}
                        <p className="winner-name">Jogador ID: {roundWinner.playerId}</p>
                        <p className="winner-hand">{roundWinner.handValue}</p>
                        <p className="winner-points">{roundWinner.points} pontos</p>
                    </div>
                </div>
            )}

            <button onClick={leaveGame} className="btn btn-leave">
                SAIR DO JOGO
            </button>
        </div>
    </div>
);

export default GameLayout;