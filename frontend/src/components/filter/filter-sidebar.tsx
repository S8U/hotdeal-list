"use client";

import { useMemo } from "react";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { findCategoryPath } from "@/lib/categories";
import type { CommunityGroup, PlatformType } from "@/lib/communities";
import type { CategoryNode } from "@/lib/types";

import { CategoryTree } from "./category-tree";

export type FilterState = {
    categoryCode: string | null;
    priceMin: string;
    priceMax: string;
    platforms: PlatformType[];
};

export const INITIAL_FILTER: FilterState = {
    categoryCode: null,
    priceMin: "",
    priceMax: "",
    platforms: [],
};

export type FilterSection = "category" | "price" | "community";

type FilterSidebarProps = {
    categoryTree: CategoryNode[];
    communityGroups: CommunityGroup[];
    value: FilterState;
    onChange: (next: FilterState) => void;
    onApply?: () => void;
    className?: string;
    only?: FilterSection;
    hideTitle?: boolean;
};

export function FilterSidebar({
    categoryTree,
    communityGroups,
    value,
    onChange,
    onApply,
    className,
    only,
    hideTitle,
}: FilterSidebarProps) {
    const show = (section: FilterSection) => !only || only === section;
    const forceExpandedCodes = useMemo(() => {
        if (!value.categoryCode) return undefined;
        const path = findCategoryPath(categoryTree, value.categoryCode);
        if (!path) return undefined;
        return new Set(path.slice(0, -1).map((n) => n.code));
    }, [categoryTree, value.categoryCode]);

    const toggleCommunity = (group: CommunityGroup) => {
        const allSelected = group.platforms.every((p) => value.platforms.includes(p));
        const next = allSelected
            ? value.platforms.filter((p) => !group.platforms.includes(p))
            : [...value.platforms.filter((p) => !group.platforms.includes(p)), ...group.platforms];
        onChange({ ...value, platforms: next as PlatformType[] });
    };

    return (
        <aside className={className}>
            {hideTitle ? null : <h2 className="mb-6 text-base font-semibold">필터</h2>}

            {show("category") ? (
                <section
                    className={
                        only === "category"
                            ? "flex min-h-0 flex-1 flex-col"
                            : only ? "space-y-1" : "mb-8 space-y-2"
                    }
                >
                    {(!only || value.categoryCode) ? (
                        <header className="mb-2 flex items-center justify-between">
                            {only ? null : (
                                <h3 className="text-sm font-medium text-muted-foreground">카테고리</h3>
                            )}
                            {value.categoryCode ? (
                                <button
                                    type="button"
                                    onClick={() => onChange({ ...value, categoryCode: null })}
                                    className="ml-auto text-xs text-muted-foreground hover:text-foreground"
                                >
                                    전체
                                </button>
                            ) : null}
                        </header>
                    ) : null}

                    <div
                        className={
                            only === "category"
                                ? "scrollbar-subtle min-h-0 flex-1 overflow-auto"
                                : "scrollbar-subtle max-h-[320px] overflow-auto"
                        }
                    >
                        <CategoryTree
                            nodes={categoryTree}
                            value={value.categoryCode}
                            onChange={(code) => onChange({ ...value, categoryCode: code })}
                            forceExpandedCodes={forceExpandedCodes}
                            size={only ? "lg" : "sm"}
                        />
                    </div>
                </section>
            ) : null}

            {show("price") ? (
                <section className={only ? "space-y-1" : "mb-8 space-y-2"}>
                    {only ? null : (
                        <h3 className="text-sm font-medium text-muted-foreground">가격 범위</h3>
                    )}
                    <div className="flex items-center gap-2">
                        <Input
                            aria-label="최소 가격"
                            inputMode="numeric"
                            placeholder="최소"
                            value={value.priceMin}
                            onChange={(e) =>
                                onChange({
                                    ...value,
                                    priceMin: e.target.value.replace(/[^\d]/g, ""),
                                })
                            }
                            className={only ? "h-12 text-base" : ""}
                        />
                        <span className="text-muted-foreground">~</span>
                        <Input
                            aria-label="최대 가격"
                            inputMode="numeric"
                            placeholder="최대"
                            value={value.priceMax}
                            onChange={(e) =>
                                onChange({
                                    ...value,
                                    priceMax: e.target.value.replace(/[^\d]/g, ""),
                                })
                            }
                            className={only ? "h-12 text-base" : ""}
                        />
                    </div>
                </section>
            ) : null}

            {show("community") ? (
                <section className={only ? "space-y-1" : "mb-8 space-y-2"}>
                    {only ? null : (
                        <h3 className="text-sm font-medium text-muted-foreground">커뮤니티</h3>
                    )}
                    <ul className={only ? "space-y-1" : "space-y-1"}>
                        {communityGroups.map((g) => {
                            const checked = g.platforms.every((p) => value.platforms.includes(p));
                            return (
                                <li key={g.communityName}>
                                    <label
                                        className={
                                            only
                                                ? "flex cursor-pointer items-center gap-3 py-1 text-base"
                                                : "flex cursor-pointer items-center gap-2.5 py-1 text-sm"
                                        }
                                    >
                                        <Checkbox
                                            checked={checked}
                                            onCheckedChange={() => toggleCommunity(g)}
                                            className={only ? "size-5" : ""}
                                        />
                                        <span>{g.communityName}</span>
                                    </label>
                                </li>
                            );
                        })}
                    </ul>
                </section>
            ) : null}

            {onApply ? (
                <div className="mt-3 flex gap-2">
                    {only ? (
                        <Button
                            variant="outline"
                            className="h-12 shrink-0 rounded-lg border-border px-7 text-base font-medium text-foreground"
                            onClick={() => {
                                if (only === "category")
                                    onChange({ ...value, categoryCode: null });
                                else if (only === "price")
                                    onChange({ ...value, priceMin: "", priceMax: "" });
                                else if (only === "community")
                                    onChange({ ...value, platforms: [] });
                            }}
                        >
                            초기화
                        </Button>
                    ) : null}
                    <Button
                        className="h-12 flex-1 rounded-lg text-base font-semibold"
                        onClick={onApply}
                    >
                        적용
                    </Button>
                </div>
            ) : null}
        </aside>
    );
}
