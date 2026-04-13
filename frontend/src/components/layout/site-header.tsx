"use client";

import {
    ArrowLeftIcon,
    BellIcon,
    FireIcon,
    ListIcon,
    MagnifyingGlassIcon,
    UserIcon,
} from "@phosphor-icons/react/dist/ssr";
import { useEffect, useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import {
    Sheet,
    SheetContent,
    SheetTitle,
} from "@/components/ui/sheet";

const ICON_WEIGHT = "bold";
const ICON_CLASS = "size-6 text-zinc-700";

type SiteHeaderProps = {
    mobileSlot?: React.ReactNode;
};

export function SiteHeader({ mobileSlot }: SiteHeaderProps) {
    const [searchOpen, setSearchOpen] = useState(false);
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

                    <div className="relative mx-auto hidden w-full max-w-xl md:block">
                        <MagnifyingGlassIcon
                            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                            weight={ICON_WEIGHT}
                        />
                        <Input
                            type="search"
                            placeholder="상품/키워드 검색"
                            className="h-10 rounded-full border-0 bg-muted pl-9 shadow-none focus-visible:ring-0"
                        />
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
                                className="h-10 rounded-full bg-muted pl-9"
                            />
                        </div>
                    </div>
                    <div className="px-4 py-6 text-sm text-muted-foreground">
                        최근 검색어가 여기에 표시됩니다.
                    </div>
                </SheetContent>
            </Sheet>
        </>
    );
}
