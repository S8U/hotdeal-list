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

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
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
const ICON_CLASS = "size-6 text-zinc-700";

type SiteHeaderProps = {
    mobileSlot?: React.ReactNode;
    keyword?: string;
    onSearch?: (keyword: string) => void;
};

export function SiteHeader({ mobileSlot, keyword = "", onSearch }: SiteHeaderProps) {
    const [searchOpen, setSearchOpen] = useState(false);
    const [draft, setDraft] = useState(keyword);
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
        }
        onSearch?.(trimmed);
        setSearchOpen(false);
        setDesktopFocused(false);
    }, [onSearch]);

    const handleRecentClick = useCallback((value: string) => {
        setDraft(value);
        handleSubmit(value);
    }, [handleSubmit]);

    const handleRemoveRecent = useCallback((value: string) => {
        setRecentSearches(removeRecentSearch(value));
    }, []);

    const handleClearRecent = useCallback(() => {
        setRecentSearches(clearRecentSearches());
    }, []);
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
                    <a href="/" className="flex shrink-0 items-center gap-2 text-xl font-bold text-zinc-700 sm:text-2xl">
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
                            placeholder="상품/키워드 검색"
                            value={draft}
                            onChange={(e) => setDraft(e.target.value)}
                            onFocus={() => setDesktopFocused(true)}
                            onKeyDown={(e) => {
                                if (e.key === "Enter") handleSubmit(draft);
                            }}
                            className="h-10 rounded-full border-0 bg-muted pl-9 shadow-none focus-visible:ring-0"
                        />
                        {desktopFocused && recentSearches.length > 0 && (
                            <div className="absolute top-full left-0 z-50 mt-1 w-full rounded-xl border bg-background py-2 shadow-lg">
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
                                {recentSearches.map((term) => (
                                    <div
                                        key={term}
                                        className="group flex items-center gap-2 px-4 py-1.5 hover:bg-muted"
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
                                            <XIcon className="size-3.5 text-muted-foreground hover:text-foreground" />
                                        </button>
                                    </div>
                                ))}
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
                        <Button variant="ghost" size="icon" className="size-10" aria-label="알림">
                            <BellIcon className={ICON_CLASS} weight={ICON_WEIGHT} />
                        </Button>
                        <Button
                            variant="outline"
                            className="hidden h-10 rounded-full border-zinc-200 px-4 text-sm font-medium text-zinc-700 sm:inline-flex"
                        >
                            로그인
                        </Button>
                        <Button
                            variant="ghost"
                            size="icon"
                            className="size-10 sm:hidden"
                            aria-label="내 계정"
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

            <Sheet open={searchOpen} onOpenChange={setSearchOpen}>
                <SheetContent side="top" className="h-full p-0">
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
                                placeholder="상품/키워드 검색"
                                defaultValue={keyword}
                                onKeyDown={(e) => {
                                    if (e.key === "Enter") handleSubmit(e.currentTarget.value);
                                }}
                                className="h-10 rounded-full bg-muted pl-9"
                            />
                        </div>
                    </div>
                    <div className="px-4 py-4">
                        {recentSearches.length > 0 ? (
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
                                    {recentSearches.map((term) => (
                                        <div
                                            key={term}
                                            className="group flex items-center gap-3 rounded-lg px-1 py-2 active:bg-muted"
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
                                최근 검색어가 없습니다.
                            </p>
                        )}
                    </div>
                </SheetContent>
            </Sheet>
        </>
    );
}
