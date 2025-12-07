import * as React from "react";
import { playerService } from "../services/player/playerService";
import { PlayerInfoResponse } from "../services/player/playerResponseTypes";
import { Link } from "react-router";
import "../../style/playerProfile.css";

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
        <div className="player-profile-container">
            <div className="player-profile-header">
                <h1>Player Profile</h1>
            </div>
            
            <nav className="player-profile-nav">
                <Link to="/home">Home</Link>
            </nav>

            {userInfo ? (
                <div className="player-profile-card">
                    <div className="player-profile-avatar">
                        {userInfo.imageUrl && (
                            <img src={userInfo.imageUrl} alt="User Avatar" />
                        )}
                        <h2>{userInfo.name}</h2>
                        <p>@{userInfo.nickName}</p>
                    </div>
                    
                    <div className="player-profile-info">
                        <p><strong>Email</strong> <span>{userInfo.email}</span></p>
                        <p><strong>Balance</strong> <span>{userInfo.balance} €</span></p>
                    </div>
                    
                    <div className="player-profile-actions">
                        <Link to="/playerProfile/update" state={{ user: userInfo }}>
                            Update Profile
                        </Link>
                        <Link to="/createInvite">Create Invite</Link>
                    </div>
                </div>
            ) : (
                <div className="player-profile-loading">Loading user information...</div>
            )}
        </div>
    );
}

export default PlayerProfilePage