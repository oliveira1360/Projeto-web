import React, { useEffect, useState } from "react";
import {useNavigate, useParams} from "react-router-dom";
import useGame, {useCloseWindow} from "../hooks/game/useGame";
import GameLoading from "../components/game/GameLoading";
import GameError from "../components/game/GameError";
import GameFinished from "../components/game/GameFinished";
import GameLayout from "../components/game/GameLayout";
import {playerService} from "../services/player/playerService";
import {PlayerInfoResponse} from "../services/player/playerResponseTypes";


const GamePage: React.FC = () => {
    const { gameId } = useParams<{ gameId: string }>();
    const [userInfo, setUserInfo] = useState<PlayerInfoResponse | null>(null);
    const [setupError, setSetupError] = useState<string | null>(null);
    const [displayWinner, setDisplayWinner] = useState<any>(null);
    const timerRef = React.useRef<NodeJS.Timeout | null>(null);
    const navigate = useNavigate();


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
        error: gameError,
        gameStatus,
        winner,
        roundWinner,
        toggleHold,
        rollDice,
        finishTurn,
        startRound,
        leaveGame,
        rollNumber
    } = game;

    //useCloseWindow(gameId ? Number(gameId) : undefined);

    const [showRoundWinner, setShowRoundWinner] = useState(false);

    useEffect(() => {
        if (roundWinner) {
            setDisplayWinner(roundWinner);
            setShowRoundWinner(true);

            if (timerRef.current) clearTimeout(timerRef.current);

            timerRef.current = setTimeout(() => {
                setShowRoundWinner(false);
            }, 5000);
        }

    }, [roundWinner]);

    useEffect(() => {
        return () => {
            if (timerRef.current) clearTimeout(timerRef.current);
        };
    }, []);

    useEffect(() => {
        if (gameStatus === "FINISHED") {
            const timer = setTimeout(() => {
                navigate("/lobbies", { replace: true });
            }, 5000);

            return () => clearTimeout(timer);
        }
    }, [gameStatus, navigate]);

    if (setupError) return <GameError error={setupError} />;
    if (gameError) return <GameError error={gameError} />;
    if (!userInfo || loading) return <GameLoading />;

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
            startRound={startRound}
            rollDice={rollDice}
            finishTurn={finishTurn}
            toggleHold={toggleHold}
            leaveGame={leaveGame}
            showRoundWinner={showRoundWinner}
            roundWinner={displayWinner}
            userId={userId}
            rollNumber={rollNumber}
        />
    );
};


export default GamePage;