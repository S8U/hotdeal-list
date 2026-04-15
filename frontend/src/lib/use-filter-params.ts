"use client";

import { useCallback, useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { INITIAL_FILTER, type FilterState } from "@/components/filter/filter-sidebar";
import type { PlatformType } from "@/lib/communities";

const PARAM_KEYS = {
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
function buildQueryString(filter: FilterState, keyword: string): string {
    const params = new URLSearchParams();

    if (keyword) params.set(PARAM_KEYS.keyword, keyword);
    if (filter.categoryCode) params.set(PARAM_KEYS.category, filter.categoryCode);
    if (filter.platforms.length) params.set(PARAM_KEYS.platforms, filter.platforms.join(","));
    if (filter.priceMin) params.set(PARAM_KEYS.priceMin, filter.priceMin);
    if (filter.priceMax) params.set(PARAM_KEYS.priceMax, filter.priceMax);

    const qs = params.toString();
    return qs ? `?${qs}` : "/";
}

/** 필터/검색 상태 변경 시 URL을 동기화하는 훅 */
export function useFilterParamsSync(filter: FilterState, keyword: string) {
    const router = useRouter();
    const searchParams = useSearchParams();
    const isInitialMount = useRef(true);

    useEffect(() => {
        // 최초 마운트 시에는 URL → state 방향이므로 push하지 않음
        if (isInitialMount.current) {
            isInitialMount.current = false;
            return;
        }

        const newQs = buildQueryString(filter, keyword);
        const currentQs = searchParams.toString();
        const targetQs = newQs === "/" ? "" : newQs.slice(1); // '?' 제거

        if (currentQs !== targetQs) {
            router.replace(newQs, { scroll: false });
        }
    }, [filter, keyword, router, searchParams]);
}

/** URL 파라미터로부터 초기 상태를 가져오는 훅 */
export function useInitialFilterFromParams(): {
    initialFilter: FilterState;
    initialKeyword: string;
} {
    const searchParams = useSearchParams();
    const ref = useRef<{ initialFilter: FilterState; initialKeyword: string } | null>(null);

    if (!ref.current) {
        const { filter, keyword } = parseFilterParams(searchParams);
        ref.current = { initialFilter: filter, initialKeyword: keyword };
    }

    return ref.current;
}
