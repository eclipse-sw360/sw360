import React, { useState, useEffect } from 'react';
import { Navigate } from "react-router-dom";
import AuthService from '../../services/auth.service';
import { AuthToken } from '../../object-types/AuthToken';

const LogoutScreen = () => {
    const authenticatedUser : AuthToken = AuthService.getUser();
    if (AuthService.isAuthenticated(authenticatedUser)) {
        AuthService.removeUser();
    }
    return <Navigate to = "/"/>;
}

export default LogoutScreen;