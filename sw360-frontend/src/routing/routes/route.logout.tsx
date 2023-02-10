import React from 'react';
import { Navigate, RouteObject } from "react-router-dom"
import LogoutScreen from '../../views/auth/logout'

const LogoutRoute: RouteObject =
{
    path: "/logout",
    element: <LogoutScreen />
}

export default LogoutRoute