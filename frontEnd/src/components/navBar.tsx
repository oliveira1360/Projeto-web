import * as React from "react";
import {Link, useNavigate} from "react-router-dom";
import { logout } from "../services/user/userApi";
import {useEffect, useState} from "react";
import {checkAuth} from "../services/request";

export function NavBar() {
    const navigate = useNavigate();
    const [isAuthenticated, setIsAuthenticated] = useState(false);


    useEffect(() => {
        const verifyAuth = async () => {
            const auth = await checkAuth();
            setIsAuthenticated(auth);
        };

        verifyAuth();
    }, []);

    const handleLogout = async () => {
        try {
            await logout();
            // After logout, redirect to the authentication/sign-in page
            navigate('/');
        } catch (error) {
            console.error('Logout failed', error);
        }
    }


    return (
        <nav>
            <button onClick={() => navigate('/home')}>Home</button>
            <button onClick={() => navigate('/playerProfile')}>Profile</button>
            {isAuthenticated ? (
                <button onClick={handleLogout}>
                    Logout
                </button>
            ) : (
                <Link to="/">Sign In</Link>
            )}
        </nav>
    );
}