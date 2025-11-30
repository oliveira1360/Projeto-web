import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import useGame from "../hooks/useGame";
import GameLoading from "../components/game/GameLoading";
import GameError from "../components/game/GameError";
import GameFinished from "../components/game/GameFinished";
import GameLayout from "../components/game/GameLayout";
import {playerService} from "../services/player/playerService";
import {PlayerInfoResponse} from "../services/player/playerResponseTypes";


const GamePage: React.FC = () => {
    const { gameId } = useParams<{ gameId: string }>();
    const [userInfo, setUserInfo] = useState<PlayerInfoResponse | null>(null);

    useEffect(() => {
        const load = async () => {
            const info = await playerService.playerInfo();
            setUserInfo(info);
        };
        load();
    }, []);


    const userId = userInfo?.userId ?? null;

    const game = useGame(gameId ? Number(gameId) : undefined, userId ?? undefined);
    const {
        players,
        hand,
        isMyTurn,
        currentRound,
        totalRounds,
        loading,
        error,
        gameStatus,
        winner,
        roundWinner,
        remainingTime,
        toggleHold,
        rollDice,
        finishTurn,
        startRound,
        leaveGame,
    } = game;

    const [showRoundWinner, setShowRoundWinner] = useState(false);

    useEffect(() => {
        if (roundWinner) {
            setShowRoundWinner(true);
            const timer = setTimeout(() => setShowRoundWinner(false), 3000);
            return () => clearTimeout(timer);
        }
    }, [roundWinner]);

    if (!userInfo) return <GameLoading />;
    if (loading) return <GameLoading />;
    if (error) return <GameError error={error} />;

    if (gameStatus === "FINISHED" && winner)
        return (
            <GameFinished
                winner={winner}
                players={players}
                leaveGame={leaveGame}
            />
        );

    return (
        <GameLayout
            players={players}
            hand={hand}
            isMyTurn={isMyTurn}
            currentRound={currentRound}
            totalRounds={totalRounds}
            remainingTime={remainingTime}
            startRound={startRound}
            rollDice={rollDice}
            finishTurn={finishTurn}
            toggleHold={toggleHold}
            leaveGame={leaveGame}
            showRoundWinner={showRoundWinner}
            roundWinner={roundWinner}
            userId={userId}
        />
    );
};


export default GamePage;