import React from "react";
import { useParams } from "react-router-dom";
import { useGame } from "../hooks/useGame";
import { Dice } from "../components/game/Dice";
import { PlayerSlot } from "../components/game/PlayerPanel";
import { GameControls } from "../components/game/GameControls";
// @ts-ignore
import backgroundImage from "../img/pokertable.jpg";

const GamePage: React.FC = () => {
    const { gameId } = useParams<{ gameId: string }>();
    const {
        players,
        hand,
        isMyTurn,
        loading,
        error,
        toggleHold,
        rollDice,
        finishTurn,
    } = useGame(Number(gameId));

    const handleLeave = () => {
        console.log("Leaving game...");
        // todo: implement leave game
    };

    if (loading) return <p>Loading game...</p>;
    if (error) return <p className="error-message">Error: {error}</p>;

    return (
        // load img
        <div
            className="game-container"
            style={{ backgroundImage: `url(${backgroundImage})` }}
        >
            <div className="poker-table">
                {players.map((player, i) => (
                    <PlayerSlot
                        key={player.playerId || i}
                        player={player}
                        isActiveTurn={!isMyTurn}
                        positionClass={`pos-${i + 1}`}
                    />
                ))}

                <div className="center-area">
                    <Dice hand={hand} isMyTurn={isMyTurn} onToggleHold={toggleHold} />
                    <GameControls
                        onRoll={rollDice}
                        onFinish={finishTurn}
                        disabled={!isMyTurn}
                    />
                </div>

                <button onClick={handleLeave} className="btn btn-leave">
                    LEAVE
                </button>
            </div>
        </div>
    );
};

export default GamePage;