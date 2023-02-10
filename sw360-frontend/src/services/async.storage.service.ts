/**
 * Key of AsyncStorage
 */
export enum StorageKey {
    AUTH_TOKEN = 'AUTH_TOKEN',
}

export class AsyncStorageUtils {
    /**
     * Save value to AsyncStorage
     * @param key
     * @param value
     */
    static save(key: StorageKey, value: string): void {
        localStorage.setItem(key, value);
    }

    /**
     * Get value from AsyncStorage
     * @param key
     */
    static get(key: StorageKey): string | null {
        return localStorage.getItem(key);
    }

    /**
     * Remove value stored in AsyncStorage
     * @param key
     */
    static remove(key: StorageKey): void {
        return localStorage.removeItem(key);
    }

    /**
     * Get Object from AsyncStorage
     * @param key
     */
    static getObject<T>(key: StorageKey): T | null {
        const value = localStorage.getItem(key);
        if (!value) return null;

        return JSON.parse(value);
    }

    /**
     * Save Object to AsyncStorage
     * @param key
     * @param value
     */
    static saveObject<T>(key: StorageKey, value: T): void {
        localStorage.setItem(key, JSON.stringify(value));
    }

    static clear(): void {
        localStorage.clear();
    }
}
