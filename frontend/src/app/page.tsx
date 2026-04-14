"use client";

import { useEffect, useMemo, useRef, useState } from "react";

import { useListCategories } from "@/api/generated/category/category";
import { useListHotdealsInfinite } from "@/api/generated/hotdeal/hotdeal";
import type { CategoryResponse, HotdealListResponse } from "@/api/generated/model";
import { DealGrid } from "@/components/deal/deal-grid";
import { FilterChips, type FilterChipKey } from "@/components/filter/filter-chips";
import {
    FilterSidebar,
    INITIAL_FILTER,
    type FilterState,
} from "@/components/filter/filter-sidebar";
import { SiteHeader } from "@/components/layout/site-header";
import {
    Sheet,
    SheetContent,
    SheetHeader,
    SheetTitle,
} from "@/components/ui/sheet";
import { getCategorySubtreeCodes } from "@/lib/categories";
import type { CategoryNode } from "@/lib/types";

type CategoryWithChildren = CategoryResponse & { children?: CategoryWithChildren[] };

const toCategoryNodes = (raw: CategoryWithChildren[] | undefined): CategoryNode[] => {
    if (!raw) return [];
    return raw
        .filter((c) => c.code && c.name)
        .map((c) => ({
            code: c.code!,
            name: c.name!,
            children: toCategoryNodes(c.children),
        }));
};

export default function Home() {
    const [filter, setFilter] = useState<FilterState>(INITIAL_FILTER);
    const [sheetOpen, setSheetOpen] = useState(false);
    const [activeChip, setActiveChip] = useState<FilterChipKey | null>(null);

    const { data: categoriesRaw } = useListCategories();
    const categoryTree = useMemo(
        () => toCategoryNodes(categoriesRaw as CategoryWithChildren[] | undefined),
        [categoriesRaw],
    );

    const categoryCodes = useMemo(() => {
        if (!filter.categoryCode) return undefined;
        const codes = getCategorySubtreeCodes(categoryTree, filter.categoryCode);
        return codes.size ? Array.from(codes) : undefined;
    }, [categoryTree, filter.categoryCode]);

    const params = useMemo(
        () => ({
            size: 40,
            categories: categoryCodes,
            platforms: filter.platforms.length ? filter.platforms : undefined,
            minPrice: filter.priceMin ? Number(filter.priceMin) : undefined,
            maxPrice: filter.priceMax ? Number(filter.priceMax) : undefined,
        }),
        [categoryCodes, filter.platforms, filter.priceMin, filter.priceMax],
    );

    const { data, isLoading, isError, fetchNextPage, hasNextPage, isFetchingNextPage } =
        useListHotdealsInfinite(params, {
            query: {
                initialPageParam: undefined,
                getNextPageParam: (lastPage: HotdealListResponse) =>
                    lastPage.hasMore ? lastPage.nextCursor : undefined,
            },
        });

    const deals = useMemo(
        () => data?.pages.flatMap((p: HotdealListResponse) => p.items ?? []) ?? [],
        [data],
    );

    const sentinelRef = useRef<HTMLDivElement | null>(null);
    useEffect(() => {
        const el = sentinelRef.current;
        if (!el) return;
        const io = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
                    fetchNextPage();
                }
            },
            { rootMargin: "400px" },
        );
        io.observe(el);
        return () => io.disconnect();
    }, [fetchNextPage, hasNextPage, isFetchingNextPage]);

    const openChipSheet = (key: FilterChipKey) => {
        setActiveChip(key);
        setSheetOpen(true);
    };

    return (
        <div className="min-h-screen bg-zinc-50">
            <SiteHeader
                mobileSlot={
                    <FilterChips
                        categoryTree={categoryTree}
                        value={filter}
                        onOpen={openChipSheet}
                        onReset={() => setFilter(INITIAL_FILTER)}
                    />
                }
            />

            <div className="mx-auto flex w-full max-w-[1440px] gap-6 px-4 pt-2 pb-6 sm:px-6 sm:pt-3">
                <FilterSidebar
                    categoryTree={categoryTree}
                    value={filter}
                    onChange={setFilter}
                    className="sticky top-16 hidden h-[calc(100vh-4rem)] w-60 shrink-0 overflow-auto py-1 pr-2 lg:block"
                />

                <main className="min-w-0 flex-1">
                    {isError ? (
                        <div className="flex min-h-60 items-center justify-center text-sm text-muted-foreground">
                            핫딜을 불러오지 못했습니다.
                        </div>
                    ) : isLoading ? (
                        <div className="flex min-h-60 items-center justify-center text-sm text-muted-foreground">
                            불러오는 중...
                        </div>
                    ) : (
                        <>
                            <DealGrid
                                deals={deals}
                                categoryTree={categoryTree}
                                onCategoryClick={(categoryCode) =>
                                    setFilter({ ...INITIAL_FILTER, categoryCode })
                                }
                            />
                            <div ref={sentinelRef} className="h-10" />
                            {isFetchingNextPage ? (
                                <div className="py-4 text-center text-sm text-muted-foreground">
                                    불러오는 중...
                                </div>
                            ) : null}
                        </>
                    )}
                </main>
            </div>

            <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
                <SheetContent
                    side="bottom"
                    showCloseButton={false}
                    className={
                        activeChip === "category"
                            ? "flex !h-[75vh] flex-col rounded-t-2xl px-5 pt-5 pb-[max(0.5rem,env(safe-area-inset-bottom))]"
                            : "flex max-h-[85vh] flex-col rounded-t-2xl px-5 pt-5 pb-[max(0.5rem,env(safe-area-inset-bottom))]"
                    }
                >
                    <SheetHeader className="px-0 py-0">
                        <SheetTitle className="text-xl font-bold leading-none text-foreground">
                            {activeChip === "category"
                                ? "카테고리"
                                : activeChip === "price"
                                  ? "가격 범위"
                                  : activeChip === "community"
                                    ? "커뮤니티"
                                    : "필터"}
                        </SheetTitle>
                    </SheetHeader>
                    <FilterSidebar
                        categoryTree={categoryTree}
                        value={filter}
                        onChange={setFilter}
                        onApply={() => setSheetOpen(false)}
                        className="flex min-h-0 flex-1 flex-col"
                        only={activeChip ?? undefined}
                        hideTitle
                    />
                </SheetContent>
            </Sheet>
        </div>
    );
}
