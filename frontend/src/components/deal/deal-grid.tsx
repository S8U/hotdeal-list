import type { HotdealResponse } from "@/api/generated/model";
import type { CategoryNode } from "@/lib/types";

import { DealCard } from "./deal-card";

type DealGridProps = {
    deals: HotdealResponse[];
    categoryTree: CategoryNode[];
    onCategoryClick?: (categoryCode: string) => void;
};

export function DealGrid({ deals, categoryTree, onCategoryClick }: DealGridProps) {
    if (deals.length === 0) {
        return (
            <div className="flex min-h-60 items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground">
                조건에 맞는 핫딜이 없습니다.
            </div>
        );
    }

    return (
        <div className="grid grid-cols-2 gap-x-3 gap-y-8 sm:gap-x-4 sm:gap-y-10 lg:grid-cols-3 xl:grid-cols-4">
            {deals.map((deal) => (
                <DealCard
                    key={deal.id}
                    deal={deal}
                    categoryTree={categoryTree}
                    onCategoryClick={onCategoryClick}
                />
            ))}
        </div>
    );
}
