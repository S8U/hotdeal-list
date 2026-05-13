"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { type FilterState } from "@/components/filter/filter-sidebar";

import { buildQueryString, parseFilterParams } from "./filter-params";

export { PARAM_KEYS, parseFilterParams, buildQueryString } from "./filter-params";

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
    const [initialParams] = useState(() => {
        const { filter, keyword } = parseFilterParams(searchParams);
        return { initialFilter: filter, initialKeyword: keyword };
    });

    return initialParams;
}
