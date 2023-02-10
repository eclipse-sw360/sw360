import React from 'react';
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import AuthRoute from './routes/route.auth';
import NotFound from '../views/exceptions/not.found';
import RouteHome from './routes/route.home';
import LogoutRoute from './routes/route.logout';

const router = createBrowserRouter([
  AuthRoute,
  LogoutRoute,
  RouteHome,
  {
    path: "*",
    element: <NotFound/>
  }
]);

const RouterConfig = () => {
  return (
    <RouterProvider router={router} />
  );
}

export default RouterConfig;