import React from 'react';
import { RouteObject } from "react-router-dom"
import Home from '../../views/home/index'

const HomeRoute: RouteObject =
    {
        path: "/",
        element: <Home />
    }

export default HomeRoute