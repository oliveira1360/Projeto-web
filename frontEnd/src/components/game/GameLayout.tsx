// @ts-ignore
import backgroundImage from "../../img/pokertable.jpg";
import { PlayerSlot } from "./PlayerPanel";
import {Dice, Die} from "./Dice";
import Scoreboard from "./Scoreboard";
import { GameControls } from "./GameControls";
import {Player} from "../../services/game/responsesType";


interface GameLayoutProps {
    players: Player[];
    hand: Die[];
    isMyTurn: boolean;
    currentRound: number;
    totalRounds: number;
    startRound: () => Promise<void>;
    rollDice: () => Promise<void>;
    finishTurn: () => Promise<void>;
    toggleHold: (index: number) => void;
    leaveGame: () => Promise<void>;
    showRoundWinner: boolean;
    roundWinner: any;
    userId: number | null;
    rollNumber: number;
}

// @ts-ignore
const GameLayout: React.FC<GameLayoutProps> = ({
                                                   players,
                                                   hand,
                                                   isMyTurn,
                                                   currentRound,
                                                   totalRounds,
                                                   startRound,
                                                   rollDice,
                                                   finishTurn,
                                                   toggleHold,
                                                   leaveGame,
                                                   showRoundWinner,
                                                   roundWinner,
                                                   userId,
                                                   rollNumber
                                               }) => (
    <div className="game-container" style={{ backgroundImage: `url(${backgroundImage})` }}>
        <div className="game-header">
            <div className="round-info">
                {currentRound > 0 && (
                    <>
                <span className="round-label">
                    Ronda {currentRound}/{totalRounds}
                </span>
                    </>
                )}
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
                            onFinishTurn={finishTurn}
                            dices={hand}
                            rollCount={rollNumber}
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
                        <h2>🏆 Vencedor da Ronda {roundWinner.roundNumber}!</h2>
                        <p className="winner-name">{roundWinner.username}</p>
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