import React from 'react';
import { RouteObject } from "react-router-dom"
import AuthScreen from '../../views/auth/auth'

const AuthRoute: RouteObject =
    {
        path: "/auth",
        element: <AuthScreen />
    }

export default AuthRoute