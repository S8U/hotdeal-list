const STORAGE_KEY = "recent-searches";
const MAX_ITEMS = 10;

export function getRecentSearches(): string[] {
    if (typeof window === "undefined") return [];
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        return raw ? (JSON.parse(raw) as string[]) : [];
    } catch {
        return [];
    }
}

export function addRecentSearch(keyword: string): string[] {
    const trimmed = keyword.trim();
    if (!trimmed) return getRecentSearches();

    const prev = getRecentSearches().filter((k) => k !== trimmed);
    const next = [trimmed, ...prev].slice(0, MAX_ITEMS);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    return next;
}

export function removeRecentSearch(keyword: string): string[] {
    const next = getRecentSearches().filter((k) => k !== keyword);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    return next;
}

export function clearRecentSearches(): string[] {
    localStorage.removeItem(STORAGE_KEY);
    return [];
}
