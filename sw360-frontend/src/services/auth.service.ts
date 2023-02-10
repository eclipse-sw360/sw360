import { SW360_API_URL } from '../utils/env';
import RequestContent from '../object-types/RequestContent';
import UserData from '../object-types/UserData';
import OAuthClient from '../object-types/OAuthClient';
import { AuthToken } from '../object-types/AuthToken';
import { AsyncStorageUtils, StorageKey } from './async.storage.service';

const generateToken = async (userData: UserData) => {

    let clientManagementURL: string = SW360_API_URL + '/authorization/client-management';
    let credentials: string = Buffer.from(`${userData.emailAddress}:${userData.password}`).toString('base64');

    const opts: RequestContent = { method: 'GET', headers: {}, body: null };

    opts.headers['Content-Type'] = 'application/json';
    opts.headers['Authorization'] = `Basic ${credentials}`;

    let oAuthClient: OAuthClient | null = null;

    await fetch(clientManagementURL, opts)
        .then((response) => {
            if (response.status == 200) {
                return response.text();
            } else {
                return null;
            }
        })
        .then((json) => {
            try {
                oAuthClient = JSON.parse(json)[0];
            } catch (err) {
                oAuthClient = null;
            }
        })
        .catch(() => {
            oAuthClient = null;
        });

    if (oAuthClient == null) {
        return null;
    }

    credentials = Buffer.from(`${oAuthClient.client_id}:${oAuthClient.client_secret}`, `binary`).toString(
        'base64'
    );

    opts.headers['Authorization'] = `Basic ${credentials}`;
    let authorizationURL: string = SW360_API_URL + '/authorization/oauth/token?grant_type=password&username=' + userData.emailAddress + '&password=' + userData.password;

    let sw360token: AuthToken | null = null
    await fetch(authorizationURL, opts)
        .then((response) => {
            if (response.status == 200) {
                return response.text();
            } else {
                return null;
            }
        })
        .then((json) => {
            try {
                sw360token = JSON.parse(json);
            } catch (err) {
                sw360token = null;
            }
        })
        .catch(() => {
            oAuthClient = null;
        });;

    if (sw360token != null) {
        storeUser(sw360token);
    }

    return sw360token;
}

const getUser = (): AuthToken => AsyncStorageUtils.getObject<AuthToken>(StorageKey.AUTH_TOKEN);

const storeUser = (sw360token: AuthToken) => {
    AsyncStorageUtils.saveObject(StorageKey.AUTH_TOKEN, sw360token);
}

const removeUser = () => {
    AsyncStorageUtils.remove(StorageKey.AUTH_TOKEN);
}

const isAuthenticated = (sw360token: AuthToken | null) => {
    if (sw360token == null) {
        return false;
    }

    return true;
}

const AuthService = {
    generateToken,
    getUser,
    isAuthenticated,
    removeUser
}

export default AuthService
