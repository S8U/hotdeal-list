import type { HotdealResponse } from "@/api/generated/model";
import type { PlatformType } from "@/lib/communities";
import type { CategoryNode } from "@/lib/types";

import { DealCard } from "./deal-card";

type DealGridProps = {
    deals: HotdealResponse[];
    categoryTree: CategoryNode[];
    platformCommunityMap: Record<string, string>;
    onCategoryClick?: (categoryCode: string) => void;
    onCommunityClick?: (platformType: PlatformType) => void;
};

export function DealGrid({ deals, categoryTree, platformCommunityMap, onCategoryClick, onCommunityClick }: DealGridProps) {
    if (deals.length === 0) {
        return (
            <div className="flex min-h-60 items-center justify-center text-sm text-muted-foreground">
                조건에 맞는 핫딜이 없습니다.
            </div>
        );
    }

    return (
        <div className="grid grid-cols-1 gap-x-3 gap-y-1.5 sm:gap-y-2 lg:grid-cols-2 2xl:grid-cols-3">
            {deals.map((deal) => (
                <DealCard
                    key={deal.id}
                    deal={deal}
                    categoryTree={categoryTree}
                    platformCommunityMap={platformCommunityMap}
                    onCategoryClick={onCategoryClick}
                    onCommunityClick={onCommunityClick}
                />
            ))}
        </div>
    );
}
