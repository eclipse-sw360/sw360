import React, { useState, useEffect } from 'react';
import { useNavigate } from "react-router-dom";
import { AsyncStorageUtils, StorageKey } from '../../services/async.storage.service';
import AuthService from '../../services/auth.service';
import { AuthToken } from '../../object-types/AuthToken';

const Home = () => {
    const navigate = useNavigate();

    useEffect(() => {
        const authenticatedUser : AuthToken = AuthService.getUser();
        if (!AuthService.isAuthenticated(authenticatedUser)) {
            navigate('/auth');
        }
    }, []);

    return <h1>home page</h1>
}

export default Home