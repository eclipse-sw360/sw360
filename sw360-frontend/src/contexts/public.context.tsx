import React, { createContext, useState } from 'react';

export const PublicContext = createContext(null);

const PublicContextProvider = ({ children }: any) => {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
    const publicContextData = {
        isAuthenticated,
        setIsAuthenticated
    }

    return (
        <PublicContext.Provider value={publicContextData}>{children}</PublicContext.Provider>
    );
}

export default PublicContextProvider;
