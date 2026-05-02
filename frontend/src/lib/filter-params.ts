import type { FilterState } from "@/components/filter/filter-sidebar";
import type { PlatformType } from "@/lib/communities";

export const PARAM_KEYS = {
    keyword: "q",
    category: "category",
    platforms: "platforms",
    priceMin: "minPrice",
    priceMax: "maxPrice",
} as const;

/** URL 검색 파라미터에서 FilterState + keyword를 파싱 */
export function parseFilterParams(searchParams: URLSearchParams): {
    filter: FilterState;
    keyword: string;
} {
    const keyword = searchParams.get(PARAM_KEYS.keyword) ?? "";
    const categoryCode = searchParams.get(PARAM_KEYS.category) ?? null;
    const platformsRaw = searchParams.get(PARAM_KEYS.platforms);
    const platforms = platformsRaw
        ? (platformsRaw.split(",").filter(Boolean) as PlatformType[])
        : [];
    const priceMin = searchParams.get(PARAM_KEYS.priceMin) ?? "";
    const priceMax = searchParams.get(PARAM_KEYS.priceMax) ?? "";

    return {
        keyword,
        filter: { categoryCode, platforms, priceMin, priceMax },
    };
}

/** FilterState + keyword를 URLSearchParams 문자열로 변환 */
export function buildQueryString(filter: FilterState, keyword: string): string {
    const params = new URLSearchParams();

    if (keyword) params.set(PARAM_KEYS.keyword, keyword);
    if (filter.categoryCode) params.set(PARAM_KEYS.category, filter.categoryCode);
    if (filter.platforms.length) params.set(PARAM_KEYS.platforms, filter.platforms.join(","));
    if (filter.priceMin) params.set(PARAM_KEYS.priceMin, filter.priceMin);
    if (filter.priceMax) params.set(PARAM_KEYS.priceMax, filter.priceMax);

    const qs = params.toString();
    return qs ? `?${qs}` : "/";
}
