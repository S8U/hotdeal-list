"use client";

import {
    ArrowLeftIcon,
    BellIcon,
    ClockCounterClockwiseIcon,
    FireIcon,
    MagnifyingGlassIcon,
    UserIcon,
    XIcon,
} from "@phosphor-icons/react/dist/ssr";
import { useCallback, useEffect, useRef, useState } from "react";

import { useSuggest } from "@/api/generated/hotdeal/hotdeal";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { gtmEvent } from "@/lib/gtm";
import {
    addRecentSearch,
    clearRecentSearches,
    getRecentSearches,
    removeRecentSearch,
} from "@/lib/recent-searches";
import {
    Sheet,
    SheetContent,
    SheetTitle,
} from "@/components/ui/sheet";

const ICON_WEIGHT = "bold";
const ICON_CLASS = "size-6 text-foreground";

type SiteHeaderProps = {
    mobileSlot?: React.ReactNode;
    keyword?: string;
    onSearch?: (keyword: string) => void;
};

export function SiteHeader({ mobileSlot, keyword = "", onSearch }: SiteHeaderProps) {
    const [searchOpen, setSearchOpen] = useState(false);
    const [draft, setDraft] = useState(keyword);
    const [mobileDraft, setMobileDraft] = useState(keyword);
    const [recentSearches, setRecentSearches] = useState<string[]>([]);
    const [desktopFocused, setDesktopFocused] = useState(false);
    const desktopWrapperRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        setRecentSearches(getRecentSearches());
    }, []);

    useEffect(() => {
        if (!desktopFocused) return;
        const onPointerDown = (e: PointerEvent) => {
            if (desktopWrapperRef.current && !desktopWrapperRef.current.contains(e.target as Node)) {
                setDesktopFocused(false);
            }
        };
        document.addEventListener("pointerdown", onPointerDown);
        return () => document.removeEventListener("pointerdown", onPointerDown);
    }, [desktopFocused]);

    const handleSubmit = useCallback((value: string) => {
        const trimmed = value.trim();
        if (trimmed) {
            setRecentSearches(addRecentSearch(trimmed));
            gtmEvent("search", { keyword: trimmed });
        }
        onSearch?.(trimmed);
        setDesktopFocused(false);
        setSearchOpen(false);
        if (document.activeElement instanceof HTMLElement) {
            document.activeElement.blur();
        }
    }, [onSearch]);

    const handleRecentClick = useCallback((value: string) => {
        gtmEvent("recent_search_click", { keyword: value });
        setDraft(value);
        handleSubmit(value);
    }, [handleSubmit]);

    const handleRemoveRecent = useCallback((value: string) => {
        setRecentSearches(removeRecentSearch(value));
    }, []);

    const handleClearRecent = useCallback(() => {
        setRecentSearches(clearRecentSearches());
    }, []);

    // 자동완성: throttle + debounce 검색어
    const [debouncedDraft, setDebouncedDraft] = useState("");
    const activeDraft = searchOpen ? mobileDraft : draft;
    const lastFiredRef = useRef(0);
    useEffect(() => {
        const trimmed = activeDraft.trim();
        if (!trimmed) {
            setDebouncedDraft("");
            return;
        }
        const now = Date.now();
        const elapsed = now - lastFiredRef.current;
        const delay = elapsed >= 200 ? 0 : 200 - elapsed;
        const timer = setTimeout(() => {
            lastFiredRef.current = Date.now();
            setDebouncedDraft(trimmed);
        }, delay);
        return () => clearTimeout(timer);
    }, [activeDraft]);

    const { data: suggestData } = useSuggest(
        { q: debouncedDraft },
        { query: { enabled: debouncedDraft.length >= 1, placeholderData: (prev) => prev } },
    );
    const prevSuggestionsRef = useRef<string[]>([]);
    const rawSuggestions = debouncedDraft ? (suggestData?.suggestions ?? []) : [];
    if (rawSuggestions.length > 0) {
        prevSuggestionsRef.current = rawSuggestions;
    }
    const suggestions = debouncedDraft
        ? (rawSuggestions.length > 0 ? rawSuggestions : prevSuggestionsRef.current)
        : [];

    const handleSuggestionClick = useCallback((value: string) => {
        const query = (searchOpen ? mobileDraft : draft).trim();
        gtmEvent("search_suggestion_click", { keyword: value, query });
        setDraft(value);
        handleSubmit(value);
    }, [handleSubmit, searchOpen, mobileDraft, draft]);

    // 키보드 네비게이션
    const [activeIndex, setActiveIndex] = useState(-1);

    // 드롭다운에 표시되는 항목 리스트 계산
    const desktopItems = suggestions.length > 0
        ? suggestions
        : (!draft.trim() && recentSearches.length > 0 ? recentSearches : []);

    const mobileItems = suggestions.length > 0
        ? suggestions
        : (!mobileDraft.trim() && recentSearches.length > 0 ? recentSearches : []);

    // 항목이 바뀌면 선택 초기화
    useEffect(() => {
        setActiveIndex(-1);
    }, [debouncedDraft, searchOpen]);

    const handleKeyNav = useCallback((e: React.KeyboardEvent, items: string[], onSelect: (value: string) => void) => {
        if (items.length === 0) return;

        if (e.key === "ArrowDown") {
            e.preventDefault();
            setActiveIndex((prev) => (prev + 1) % items.length);
        } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setActiveIndex((prev) => (prev <= 0 ? items.length - 1 : prev - 1));
        } else if (e.key === "Enter" && activeIndex >= 0 && activeIndex < items.length) {
            e.preventDefault();
            onSelect(items[activeIndex]);
        }
    }, [activeIndex]);

    const [slotHidden, setSlotHidden] = useState(false);
    const lastYRef = useRef(0);

    useEffect(() => {
        if (!mobileSlot) return;
        const onScroll = () => {
            const y = window.scrollY;
            const delta = y - lastYRef.current;
            if (y < 40) {
                setSlotHidden(false);
            } else if (delta > 4) {
                setSlotHidden(true);
            } else if (delta < -4) {
                setSlotHidden(false);
            }
            lastYRef.current = y;
        };
        window.addEventListener("scroll", onScroll, { passive: true });
        return () => window.removeEventListener("scroll", onScroll);
    }, [mobileSlot]);

    return (
        <>
            <header className="sticky top-0 z-40 w-full bg-background">
                <div className="mx-auto flex h-16 w-full max-w-[1440px] items-center gap-2 px-4 sm:gap-4 sm:px-6">
                    <a href="/" className="flex shrink-0 items-center gap-2 text-xl font-bold text-foreground sm:text-2xl">
                        <FireIcon className="size-7 text-orange-500 sm:size-8" weight="fill" />
                        <span>핫딜리스트</span>
                    </a>

                    <div ref={desktopWrapperRef} className="relative mx-auto hidden w-full max-w-xl md:block">
                        <MagnifyingGlassIcon
                            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                            weight={ICON_WEIGHT}
                        />
                        <Input
                            type="search"
                            autoComplete="off"
                            placeholder="상품/키워드 검색"
                            value={draft}
                            onChange={(e) => setDraft(e.target.value)}
                            onFocus={() => setDesktopFocused(true)}
                            onKeyDown={(e) => {
                                if (e.nativeEvent.isComposing) return;
                                if (e.key === "Escape") {
                                    setDesktopFocused(false);
                                } else if (desktopFocused && desktopItems.length > 0 && (e.key === "ArrowDown" || e.key === "ArrowUp" || (e.key === "Enter" && activeIndex >= 0))) {
                                    handleKeyNav(e, desktopItems, handleSuggestionClick);
                                } else if (e.key === "Enter") {
                                    handleSubmit(draft);
                                }
                            }}
                            className="h-10 rounded-full border-0 bg-muted pl-9 shadow-none focus-visible:ring-0"
                        />
                        {desktopFocused && (suggestions.length > 0 || (!draft.trim() && recentSearches.length > 0)) && (
                            <div className="absolute top-full left-0 z-50 mt-1 w-full rounded-xl border bg-background py-2 shadow-lg">
                                {suggestions.length > 0 ? (
                                    <>
                                        <div className="px-4 pb-1">
                                            <span className="text-xs font-medium text-muted-foreground">추천 검색어</span>
                                        </div>
                                        {suggestions.map((term, i) => (
                                            <div
                                                key={term}
                                                className={cn("flex items-center gap-2 px-4 py-1.5 hover:bg-muted", activeIndex === i && "bg-muted")}
                                            >
                                                <MagnifyingGlassIcon className="size-4 shrink-0 text-muted-foreground" weight={ICON_WEIGHT} />
                                                <button
                                                    type="button"
                                                    className="flex-1 cursor-pointer truncate text-left text-sm"
                                                    onClick={() => handleSuggestionClick(term)}
                                                >
                                                    {term}
                                                </button>
                                            </div>
                                        ))}
                                    </>
                                ) : (
                                    <>
                                        <div className="flex items-center justify-between px-4 pb-1">
                                            <span className="text-xs font-medium text-muted-foreground">최근 검색어</span>
                                            <button
                                                type="button"
                                                onClick={handleClearRecent}
                                                className="cursor-pointer text-xs text-muted-foreground hover:text-foreground"
                                            >
                                                전체 삭제
                                            </button>
                                        </div>
                                        {recentSearches.map((term, i) => (
                                            <div
                                                key={term}
                                                className={cn("group flex items-center gap-2 px-4 py-1.5 hover:bg-muted", activeIndex === i && "bg-muted")}
                                            >
                                                <ClockCounterClockwiseIcon className="size-4 shrink-0 text-muted-foreground" />
                                                <button
                                                    type="button"
                                                    className="flex-1 cursor-pointer truncate text-left text-sm"
                                                    onClick={() => handleRecentClick(term)}
                                                >
                                                    {term}
                                                </button>
                                                <button
                                                    type="button"
                                                    onClick={() => handleRemoveRecent(term)}
                                                    className="cursor-pointer opacity-0 group-hover:opacity-100"
                                                    aria-label={`${term} 삭제`}
                                                >
                                                    <XIcon className="size-4 text-muted-foreground hover:text-foreground" />
                                                </button>
                                            </div>
                                        ))}
                                    </>
                                )}
                            </div>
                        )}
                    </div>

                    <nav className="ml-auto flex shrink-0 items-center gap-1 sm:gap-2">
                        <Button
                            variant="ghost"
                            size="icon"
                            className="size-10 md:hidden"
                            onClick={() => setSearchOpen(true)}
                            aria-label="검색"
                        >
                            <MagnifyingGlassIcon className={ICON_CLASS} weight={ICON_WEIGHT} />
                        </Button>
                        <Button variant="ghost" size="icon" className="size-10" aria-label="알림" onClick={() => alert("준비 중인 기능입니다.")}>
                            <BellIcon className={ICON_CLASS} weight={ICON_WEIGHT} />
                        </Button>
                        <Button
                            variant="outline"
                            className="hidden h-10 rounded-full border-border px-4 text-sm font-medium text-foreground sm:inline-flex"
                            onClick={() => alert("준비 중인 기능입니다.")}
                        >
                            로그인
                        </Button>
                        <Button
                            variant="ghost"
                            size="icon"
                            className="size-10 sm:hidden"
                            aria-label="내 계정"
                            onClick={() => alert("준비 중인 기능입니다.")}
                        >
                            <UserIcon className={ICON_CLASS} weight={ICON_WEIGHT} />
                        </Button>
                    </nav>
                </div>

                {mobileSlot ? (
                    <div
                        className={cn(
                            "grid overflow-hidden transition-[grid-template-rows,opacity] duration-200 lg:hidden",
                            slotHidden
                                ? "grid-rows-[0fr] opacity-0"
                                : "grid-rows-[1fr] opacity-100",
                        )}
                    >
                        <div className="min-h-0">
                            <div className="mx-auto w-full max-w-[1440px] px-4 pb-2 sm:px-6">
                                {mobileSlot}
                            </div>
                        </div>
                    </div>
                ) : null}
            </header>

            <Sheet open={searchOpen} onOpenChange={(open) => {
                setSearchOpen(open);
                if (open) setMobileDraft(keyword);
            }}>
                <SheetContent side="top" className="h-full p-0" showCloseButton={false}>
                    <SheetTitle className="sr-only">검색</SheetTitle>
                    <div className="flex h-16 items-center gap-2 px-3">
                        <Button
                            variant="ghost"
                            size="icon"
                            className="size-10"
                            onClick={() => setSearchOpen(false)}
                            aria-label="닫기"
                        >
                            <ArrowLeftIcon className={ICON_CLASS} weight={ICON_WEIGHT} />
                        </Button>
                        <div className="relative flex-1">
                            <MagnifyingGlassIcon
                                className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                                weight={ICON_WEIGHT}
                            />
                            <Input
                                autoFocus
                                type="search"
                                autoComplete="off"
                                placeholder="상품/키워드 검색"
                                value={mobileDraft}
                                onChange={(e) => setMobileDraft(e.target.value)}
                                onKeyDown={(e) => {
                                    if (e.nativeEvent.isComposing) return;
                                    if (mobileItems.length > 0 && (e.key === "ArrowDown" || e.key === "ArrowUp" || (e.key === "Enter" && activeIndex >= 0))) {
                                        handleKeyNav(e, mobileItems, (value) => {
                                            gtmEvent("search_suggestion_click", { keyword: value, query: mobileDraft.trim() });
                                            setMobileDraft(value);
                                            handleSubmit(value);
                                        });
                                    } else if (e.key === "Enter") {
                                        handleSubmit(mobileDraft);
                                    }
                                }}
                                className="h-10 rounded-full bg-muted pl-9"
                            />
                        </div>
                    </div>
                    <div className="px-4 py-4">
                        {suggestions.length > 0 ? (
                            <>
                                <div className="pb-2">
                                    <span className="text-sm font-medium text-muted-foreground">추천 검색어</span>
                                </div>
                                <div className="space-y-0.5">
                                    {suggestions.map((term, i) => (
                                        <div
                                            key={term}
                                            className={cn("flex items-center gap-3 rounded-lg px-1 py-2 active:bg-muted", activeIndex === i && "bg-muted")}
                                        >
                                            <MagnifyingGlassIcon className="size-4 shrink-0 text-muted-foreground" weight={ICON_WEIGHT} />
                                            <button
                                                type="button"
                                                className="flex-1 cursor-pointer truncate text-left text-sm"
                                                onClick={() => {
                                                    gtmEvent("search_suggestion_click", { keyword: term, query: mobileDraft.trim() });
                                                    setMobileDraft(term);
                                                    handleSubmit(term);
                                                }}
                                            >
                                                {term}
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            </>
                        ) : recentSearches.length > 0 && !mobileDraft.trim() ? (
                            <>
                                <div className="flex items-center justify-between pb-2">
                                    <span className="text-sm font-medium text-muted-foreground">최근 검색어</span>
                                    <button
                                        type="button"
                                        onClick={handleClearRecent}
                                        className="cursor-pointer text-xs text-muted-foreground hover:text-foreground"
                                    >
                                        전체 삭제
                                    </button>
                                </div>
                                <div className="space-y-0.5">
                                    {recentSearches.map((term, i) => (
                                        <div
                                            key={term}
                                            className={cn("group flex items-center gap-3 rounded-lg px-1 py-2 active:bg-muted", activeIndex === i && "bg-muted")}
                                        >
                                            <ClockCounterClockwiseIcon className="size-4 shrink-0 text-muted-foreground" />
                                            <button
                                                type="button"
                                                className="flex-1 cursor-pointer truncate text-left text-sm"
                                                onClick={() => {
                                                    setMobileDraft(term);
                                                    handleRecentClick(term);
                                                }}
                                            >
                                                {term}
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => handleRemoveRecent(term)}
                                                className="cursor-pointer p-1"
                                                aria-label={`${term} 삭제`}
                                            >
                                                <XIcon className="size-4 text-muted-foreground" />
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            </>
                        ) : (
                            <p className="py-2 text-sm text-muted-foreground">
                                {mobileDraft.trim() ? "" : "최근 검색어가 없습니다."}
                            </p>
                        )}
                    </div>
                </SheetContent>
            </Sheet>
        </>
    );
}
