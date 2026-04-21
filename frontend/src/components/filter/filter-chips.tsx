"use client";

import { CaretDownIcon } from "@phosphor-icons/react/dist/ssr";

import { Button } from "@/components/ui/button";
import { findCategoryPath } from "@/lib/categories";
import type { CommunityGroup } from "@/lib/communities";
import type { CategoryNode } from "@/lib/types";
import { cn } from "@/lib/utils";

import type { FilterState } from "./filter-sidebar";

export type FilterChipKey = "category" | "price" | "community";

type FilterChipsProps = {
    categoryTree: CategoryNode[];
    platformCommunityMap: Record<string, string>;
    communityGroups: CommunityGroup[];
    value: FilterState;
    onOpen: (key: FilterChipKey) => void;
    onReset: () => void;
    className?: string;
};

const formatPriceShort = (raw: string) => {
    if (!raw) return "";
    const n = Number(raw);
    if (n >= 10000) return `${(n / 10000).toFixed(n % 10000 === 0 ? 0 : 1)}만`;
    return n.toLocaleString("ko-KR");
};

export function FilterChips({
    categoryTree,
    platformCommunityMap,
    communityGroups,
    value,
    onOpen,
    onReset,
    className,
}: FilterChipsProps) {
    const categoryPath = value.categoryCode
        ? findCategoryPath(categoryTree, value.categoryCode)
        : null;
    const categoryLabel = categoryPath ? categoryPath.at(-1)!.name : "카테고리";

    const priceLabel = (() => {
        const min = formatPriceShort(value.priceMin);
        const max = formatPriceShort(value.priceMax);
        if (!min && !max) return "가격";
        if (min && max) return `${min}~${max}원`;
        if (min) return `${min}원~`;
        return `~${max}원`;
    })();

    const communityLabel = (() => {
        if (value.platforms.length === 0) return "커뮤니티";
        const selectedNames = new Set(
            value.platforms.map((p) => platformCommunityMap[p]).filter(Boolean),
        );
        if (selectedNames.size === 1) return [...selectedNames][0];
        if (selectedNames.size > 1) return `커뮤니티 ${selectedNames.size}`;
        return "커뮤니티";
    })();

    const hasAny =
        !!value.categoryCode ||
        !!value.priceMin ||
        !!value.priceMax ||
        value.platforms.length > 0;

    return (
        <div className={cn("flex items-center gap-2 overflow-x-auto", className)}>
            <Chip active={!!value.categoryCode} onClick={() => onOpen("category")}>
                {categoryLabel}
            </Chip>
            <Chip
                active={!!value.priceMin || !!value.priceMax}
                onClick={() => onOpen("price")}
            >
                {priceLabel}
            </Chip>
            <Chip
                active={value.platforms.length > 0}
                onClick={() => onOpen("community")}
            >
                {communityLabel}
            </Chip>

            {hasAny ? (
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={onReset}
                    className="ml-auto shrink-0 text-xs text-muted-foreground"
                >
                    초기화
                </Button>
            ) : null}
        </div>
    );
}

function Chip({
    children,
    active,
    onClick,
}: {
    children: React.ReactNode;
    active: boolean;
    onClick: () => void;
}) {
    return (
        <button
            type="button"
            onClick={onClick}
            className={cn(
                "inline-flex shrink-0 cursor-pointer items-center gap-1 rounded-full px-3.5 py-1.5 text-sm font-medium transition-colors",
                active
                    ? "bg-foreground text-background"
                    : "bg-muted text-foreground hover:bg-muted/70",
            )}
        >
            <span className="truncate">{children}</span>
            <CaretDownIcon className="size-4" weight="bold" />
        </button>
    );
}
