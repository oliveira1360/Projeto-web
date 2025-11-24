// @ts-ignore
import React, {useEffect, useState} from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import {checkAuth} from "../services/request";

export function ProtectedRoute() {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);

    useEffect(() => {
        const verifyAuth = async () => {
            const auth = await checkAuth();
            setIsAuthenticated(auth);
        };

        verifyAuth();
        const intervalId = setInterval(verifyAuth, 1000);
        return () => clearInterval(intervalId);
    }, []);

    if (isAuthenticated === null) {
        return <div>Loading...</div>;
    }

    if (!isAuthenticated) {
        return <Navigate to="/" replace />;
    }

    return (
        <div>
            <Outlet />
        </div>
    );
}