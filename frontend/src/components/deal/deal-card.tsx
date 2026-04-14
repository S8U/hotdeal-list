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
    onCategoryClick?: (categoryCode: string) => void;
};

export function DealCard({ deal, categoryTree, onCategoryClick }: DealCardProps) {
    const leafCode = pickLeafCode(categoryTree, deal.categoryCodes);
    const path = leafCode ? findCategoryPath(categoryTree, leafCode) : null;
    const categoryLabelShort = path ? path.at(-1)!.name : leafCode ?? "";
    const categoryLabelLong = path
        ? path.slice(-2).map((n) => n.name).join(" > ")
        : categoryLabelShort;

    const rootCode = path?.[0]?.code;
    const FallbackIcon = (rootCode && CATEGORY_ICON_MAP[rootCode]) || Package;

    // TODO: 원래는 deal.thumbnailUrl 사용 — 임시로 플랫폼/포스트ID 기반 URL 사용
    // const thumbnailUrl = deal.thumbnailUrl;
    const postId = deal.url?.match(/(\d+)(?!.*\d)/)?.[1];
    const thumbnailUrl =
        deal.platformType && postId
            ? `https://swas.s8u.kr/HotDeal/api/info/thumbnail/${deal.platformType}/${postId}`
            : null;

    return (
        <div className="group flex gap-3 rounded-xl bg-background p-2 sm:gap-4 sm:p-3">
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
            </a>

            <div className="flex min-w-0 flex-1 flex-col">
                <div className="flex flex-wrap items-center gap-1">
                    {leafCode && onCategoryClick ? (
                        <button
                            type="button"
                            onClick={() => onCategoryClick(leafCode)}
                            className="inline-flex w-fit max-w-full items-center rounded-sm bg-muted px-1.5 py-0.5 text-[11px] font-medium text-zinc-500 hover:text-primary"
                        >
                            <span className="truncate lg:hidden">{categoryLabelShort}</span>
                            <span className="hidden truncate lg:inline">{categoryLabelLong}</span>
                        </button>
                    ) : categoryLabelShort ? (
                        <span className="inline-flex w-fit max-w-full items-center rounded-sm bg-muted px-1.5 py-0.5 text-[11px] font-medium text-zinc-500">
                            <span className="truncate lg:hidden">{categoryLabelShort}</span>
                            <span className="hidden truncate lg:inline">{categoryLabelLong}</span>
                        </span>
                    ) : null}
                    {deal.platformType ? (
                        <CommunityTag platformType={deal.platformType as PlatformType} />
                    ) : null}
                </div>

                <a
                    href={deal.url}
                    className="mt-1.5 focus:outline-none"
                    aria-label={deal.title}
                    target="_blank"
                    rel="noreferrer"
                >
                    <h3 className="line-clamp-2 text-sm font-medium leading-snug text-zinc-700 group-hover:text-primary">
                        {deal.title}
                    </h3>
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
                        <time
                            dateTime={deal.wroteAt}
                            className="shrink-0 text-xs text-muted-foreground"
                        >
                            {formatRelativeTime(deal.wroteAt)}
                        </time>
                    ) : null}
                </div>
            </div>
        </div>
    );
}
