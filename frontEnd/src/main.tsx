import * as React from "react";
import * as ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';


import HomePage from './pages/HomePage';
import AuthenticationPage from './pages/authentication';
import AboutPage from "./pages/about";
import GamePage from "./pages/game";
import LobbiesPage from "./pages/lobbies";
import LobbyPage from "./pages/lobby";
import LobbyCreationPage from "./pages/lobbyCreation";
import PlayerProfilePage from "./pages/playerProfile";
import {JSX} from "react";
import { ProtectedRoute } from './components/ProtectedRoute';

type Page = {
    path: string;
    element: JSX.Element;
}

const authRoutes: Page[] = [
    {path: '/home', element: <HomePage />},
    {path: '/about', element: <AboutPage /> },
    {path: '/game', element: <GamePage /> },
    {path: '/lobbies', element: <LobbiesPage /> },
    {path: '/lobby', element: <LobbyPage /> },
    {path: '/lobbyCreation', element: <LobbyCreationPage /> },
    {path: '/playerProfile', element: <PlayerProfilePage /> },
]

const publicRoutes: Page[] = [
    {path: '/', element: <AuthenticationPage />},
]

const router = createBrowserRouter([
    ...publicRoutes,
    {
        element: <ProtectedRoute />,
        children: authRoutes,
    },
]);

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        {/* ajuda a identificar erros */}
        <RouterProvider router={router} />
    </React.StrictMode>
);