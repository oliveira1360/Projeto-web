import * as React from "react";
import * as ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';


import HomePage from './pages/HomePage';
import AuthenticationPage from './pages/authentication';


const router = createBrowserRouter([
    {
        path: '/',
        element: <HomePage />,
    },
    {
        path: '/auth',
        element: <AuthenticationPage />,
    },
]);

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        {/* ajuda a identificar erros */}
        <RouterProvider router={router} />
    </React.StrictMode>
);