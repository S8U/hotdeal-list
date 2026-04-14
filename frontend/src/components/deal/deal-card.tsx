import { useState } from "react";

import {
    Armchair,
    Cpu,
    Gift,
    type LucideIcon,
    MessageCircle,
    Package,
    Shirt,
    Sparkles,
    ThumbsUp,
    UtensilsCrossed,
    Volleyball,
    Wrench,
} from "lucide-react";

import type { HotdealResponse } from "@/api/generated/model";
import { findCategoryPath, pickLeafCode } from "@/lib/categories";
import type { PlatformType } from "@/lib/communities";
import { formatCompact, formatPrice, formatRelativeTime } from "@/lib/format";
import type { CategoryNode } from "@/lib/types";

import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";

import { CommunityTag } from "./community-tag";

const CATEGORY_ICON_MAP: Record<string, LucideIcon> = {
    electronics: Cpu,
    auto_tools: Wrench,
    fashion: Shirt,
    beauty: Sparkles,
    food: UtensilsCrossed,
    living: Armchair,
    hobby: Volleyball,
    etc: Gift,
};

type DealCardProps = {
    deal: HotdealResponse;
    categoryTree: CategoryNode[];
    platformCommunityMap: Record<string, string>;
    onCategoryClick?: (categoryCode: string) => void;
    onCommunityClick?: (platformType: PlatformType) => void;
};

export function DealCard({ deal, categoryTree, platformCommunityMap, onCategoryClick, onCommunityClick }: DealCardProps) {
    const leafCode = pickLeafCode(categoryTree, deal.categoryCodes);
    const path = leafCode ? findCategoryPath(categoryTree, leafCode) : null;
    const categoryLabelShort = path ? path.at(-1)!.name : leafCode ?? "";

    const rootCode = path?.[0]?.code;
    const FallbackIcon = (rootCode && CATEGORY_ICON_MAP[rootCode]) || Package;

    // TODO: 원래는 deal.thumbnailUrl 사용 — 임시로 플랫폼/포스트ID 기반 URL 사용
    // const thumbnailUrl = deal.thumbnailUrl;
    const postId = deal.url?.match(/(\d+)(?!.*\d)/)?.[1];
    const thumbnailUrl =
        deal.platformType && postId
            ? `https://swas.s8u.kr/HotDeal/api/info/thumbnail/${deal.platformType}/${postId}`
            : null;

    const ended = !!deal.ended;

    return (
        <div className={`group flex gap-3 rounded-xl bg-background p-2 sm:gap-4 sm:p-3${ended ? " opacity-60" : ""}`}>
            <a
                href={deal.url}
                className="relative block size-24 shrink-0 overflow-hidden rounded-md bg-black/3 sm:size-28"
                aria-label={deal.title}
                target="_blank"
                rel="noreferrer"
            >
                {thumbnailUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                        src={thumbnailUrl}
                        alt={deal.productName ?? deal.title ?? ""}
                        className="size-full object-cover mix-blend-multiply transition-transform duration-200 group-hover:scale-[1.02]"
                        loading="lazy"
                        onError={(e) => {
                            e.currentTarget.style.display = "none";
                            e.currentTarget.parentElement
                                ?.querySelector<HTMLElement>("[data-fallback]")
                                ?.style.setProperty("display", "flex");
                        }}
                    />
                ) : null}
                <div
                    data-fallback
                    className="absolute inset-0 items-center justify-center text-zinc-300"
                    style={{ display: thumbnailUrl ? "none" : "flex" }}
                    aria-hidden
                >
                    <FallbackIcon className="size-7" strokeWidth={1.25} />
                </div>
                {ended ? (
                    <div className="absolute inset-0 flex items-center justify-center rounded-md bg-black/50">
                        <span className="text-xs font-semibold text-white">종료</span>
                    </div>
                ) : null}
            </a>

            <div className="flex min-w-0 flex-1 flex-col">
                <div className="flex flex-wrap items-center gap-1">
                    {leafCode && onCategoryClick ? (
                        <button
                            type="button"
                            onClick={() => onCategoryClick(leafCode)}
                            className="inline-flex w-fit max-w-full cursor-pointer items-center rounded-sm bg-muted px-1.5 py-0.5 text-[11px] font-medium text-zinc-500 hover:text-primary"
                        >
                            <span className="truncate">{categoryLabelShort}</span>
                        </button>
                    ) : categoryLabelShort ? (
                        <span className="inline-flex w-fit max-w-full items-center rounded-sm bg-muted px-1.5 py-0.5 text-[11px] font-medium text-zinc-500">
                            <span className="truncate">{categoryLabelShort}</span>
                        </span>
                    ) : null}
                    {deal.platformType ? (
                        <CommunityTag
                            platformType={deal.platformType as PlatformType}
                            platformCommunityMap={platformCommunityMap}
                            onClick={onCommunityClick ? () => onCommunityClick(deal.platformType as PlatformType) : undefined}
                        />
                    ) : null}
                </div>

                <a
                    href={deal.url}
                    className="mt-1.5 focus:outline-none"
                    aria-label={deal.title}
                    target="_blank"
                    rel="noreferrer"
                >
                    {deal.highlightedTitle ? (
                        <h3
                            className="line-clamp-2 text-sm font-medium leading-snug text-zinc-700 group-hover:text-primary [&_em]:not-italic [&_em]:font-bold [&_em]:text-primary"
                            dangerouslySetInnerHTML={{ __html: deal.highlightedTitle }}
                        />
                    ) : (
                        <h3 className="line-clamp-2 text-sm font-medium leading-snug text-zinc-700 group-hover:text-primary">
                            {deal.title}
                        </h3>
                    )}
                </a>

                <div className="mt-auto flex items-center justify-between gap-2 pt-2">
                    <div className="flex min-w-0 items-center gap-2">
                        {typeof deal.price === "number" && deal.price > 0 ? (
                            <p className="truncate text-base font-bold text-foreground sm:text-lg">
                                {formatPrice(deal.price)}
                            </p>
                        ) : null}
                        <div className="flex shrink-0 items-center gap-2 text-xs text-muted-foreground">
                            {typeof deal.likeCount === "number" && deal.likeCount > 0 ? (
                                <span className="inline-flex items-center gap-0.5">
                                    <ThumbsUp className="size-3" />
                                    {formatCompact(deal.likeCount)}
                                </span>
                            ) : null}
                            {typeof deal.commentCount === "number" && deal.commentCount > 0 ? (
                                <span className="inline-flex items-center gap-0.5">
                                    <MessageCircle className="size-3" />
                                    {formatCompact(deal.commentCount)}
                                </span>
                            ) : null}
                        </div>
                    </div>
                    {deal.wroteAt ? (
                        <TimeLabel iso={deal.wroteAt} />
                    ) : null}
                </div>
            </div>
        </div>
    );
}

const formatAbsolute = (iso: string) =>
    new Date(iso).toLocaleString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    });

function TimeLabel({ iso }: { iso: string }) {
    const [showAbsolute, setShowAbsolute] = useState(false);

    return (
        <>
            {/* 모바일: 클릭 토글 */}
            <time
                dateTime={iso}
                className="shrink-0 cursor-pointer text-xs text-muted-foreground lg:hidden"
                onClick={() => setShowAbsolute((v) => !v)}
            >
                {showAbsolute ? formatAbsolute(iso) : formatRelativeTime(iso)}
            </time>
            {/* 데스크톱: Tooltip */}
            <TooltipProvider>
                <Tooltip>
                    <TooltipTrigger
                        render={
                            <time
                                dateTime={iso}
                                className="hidden shrink-0 cursor-default text-xs text-muted-foreground lg:inline"
                            >
                                {formatRelativeTime(iso)}
                            </time>
                        }
                    />
                    <TooltipContent>{formatAbsolute(iso)}</TooltipContent>
                </Tooltip>
            </TooltipProvider>
        </>
    );
}
