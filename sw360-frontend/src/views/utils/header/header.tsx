import React, { useEffect, useState, useContext } from "react";
import Sw360Logo from '../../../assets/images/sw360.png';
import './css/header.css'
import { AuthToken } from "../../../object-types/AuthToken";
import AuthService from "../../../services/auth.service";
import { PublicContext } from "../../../contexts/public.context";

const PageHeader = () => {
    const { isAuthenticated, setIsAuthenticated } = useContext(PublicContext);
    useEffect(() => {
        const loggedInUser: AuthToken = AuthService.getUser();
        setIsAuthenticated(AuthService.isAuthenticated(loggedInUser));
    }, [isAuthenticated]);

    return (
        <div className="header">
            <div className="row">
                <div className="col-3">
                    <a className="logo custom-logo" href="/" title="Go to SW360">
                        <img height="56" src={Sw360Logo} alt='...' />
                    </a>
                </div>
                <div className="col">
                    <div className="text-end" hidden={!isAuthenticated}>
                        <a href="/logout">Logout</a>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default PageHeader