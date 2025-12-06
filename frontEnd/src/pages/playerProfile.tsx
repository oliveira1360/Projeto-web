import * as React from "react";
import { playerService } from "../services/player/playerService";
import { PlayerInfoResponse } from "../services/player/playerResponseTypes";
import { Link } from "react-router";

function PlayerProfilePage() {
    const [userInfo, setUserInfo] = React.useState<PlayerInfoResponse>();
    React.useEffect(() => {
        async function fetchUserInfo() {
            try {
                const userInfo = await playerService.playerInfo();
                console.log("User Info:", userInfo);
                setUserInfo(userInfo);
            } catch (error) {
                console.error("Error fetching user info:", error);
            }
        }
        fetchUserInfo();
    }, [userInfo != undefined]);
    return (
        <div className="home-container lobby-page">
            <h1>Player Profile</h1>
            <Link to="/home">Home</Link>
            <div>
                {userInfo ? (
                    <div>
                        {userInfo.imageUrl && <img src={userInfo.imageUrl} alt="User Avatar" />}
                        <p><strong>Name:</strong> {userInfo.name}</p>
                        <p><strong>Nickname:</strong> {userInfo.nickName}</p>
                        <p><strong>Email:</strong> {userInfo.email}</p>
                        <p><strong>Balance:</strong> {userInfo.balance} <strong> €</strong></p>
                    </div>
                ) : (
                    <p>Loading user information...</p>
                )}
                <div>
                    <Link to="/playerProfile/update" state={{ user: userInfo }}>
                        Update Profile
                    </Link>
                </div>
            </div>
        </div>
    );
}

export default PlayerProfilePage