import { cache } from "react";
import type { Metadata } from "next";
import { HydrationBoundary, QueryClient, dehydrate } from "@tanstack/react-query";

import {
    getListCategoriesQueryKey,
    listCategories,
} from "@/api/generated/category/category";
import {
    getListHotdealsInfiniteQueryOptions,
} from "@/api/generated/hotdeal/hotdeal";
import {
    getListPlatformsQueryKey,
    listPlatforms,
} from "@/api/generated/platform/platform";
import type { CategoryResponse } from "@/api/generated/model";
import { findCategoryPath, getCategorySubtreeCodes } from "@/lib/categories";
import { parseFilterParams } from "@/lib/filter-params";
import type { CategoryNode } from "@/lib/types";

import HomeClient from "./home-client";

type CategoryWithChildren = CategoryResponse & { children?: CategoryWithChildren[] };
const HOME_TITLE = "핫딜리스트 - 실시간 핫딜 모음";

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

// 동일 요청 내에서 generateMetadata와 Home 양쪽이 카테고리 응답을 공유한다.
// 백엔드 호출 실패는 빈 배열로 폴백하여 메타/페이지 양쪽 모두 안전하게 진행.
const getCategoriesRaw = cache(async (): Promise<CategoryWithChildren[]> => {
    try {
        return ((await listCategories()) as CategoryWithChildren[] | undefined) ?? [];
    } catch {
        return [];
    }
});

const getCategoryTree = cache(async (): Promise<CategoryNode[]> => {
    return toCategoryNodes(await getCategoriesRaw());
});

function resolveCategoryName(tree: CategoryNode[], code: string): string {
    const path = findCategoryPath(tree, code);
    return path?.at(-1)?.name ?? code;
}

type SearchParamsRecord = Record<string, string | string[] | undefined>;

function toURLSearchParams(input: SearchParamsRecord): URLSearchParams {
    const sp = new URLSearchParams();
    for (const [key, value] of Object.entries(input)) {
        if (value === undefined) continue;
        if (Array.isArray(value)) {
            for (const v of value) sp.append(key, v);
        } else {
            sp.set(key, value);
        }
    }
    return sp;
}

function describeFilters(
    keyword: string,
    categoryLabel: string | null,
    platforms: string[],
    priceMin: string,
    priceMax: string,
): string {
    const parts: string[] = [];
    if (keyword) parts.push(`'${keyword}' 검색`);
    if (categoryLabel) parts.push(`카테고리 ${categoryLabel}`);
    if (platforms.length) parts.push(`커뮤니티 ${platforms.join(", ")}`);
    if (priceMin || priceMax) {
        parts.push(`가격 ${priceMin || "0"}~${priceMax || "∞"}`);
    }
    return parts.length
        ? `${parts.join(" · ")} 핫딜 모음 - 여러 커뮤니티의 실시간 핫딜을 한 곳에서`
        : "여러 커뮤니티의 실시간 핫딜을 한 곳에서 모아 보세요.";
}

export async function generateMetadata({
    searchParams,
}: {
    searchParams: Promise<SearchParamsRecord>;
}): Promise<Metadata> {
    const sp = toURLSearchParams(await searchParams);
    const { filter, keyword } = parseFilterParams(sp);
    const trimmed = keyword.trim();

    const categoryTree = filter.categoryCode ? await getCategoryTree() : [];
    const categoryLabel = filter.categoryCode
        ? resolveCategoryName(categoryTree, filter.categoryCode)
        : null;

    const title = trimmed ? `${trimmed} - 핫딜리스트 검색` : HOME_TITLE;
    const description = describeFilters(
        trimmed,
        categoryLabel,
        filter.platforms,
        filter.priceMin,
        filter.priceMax,
    );

    const qs = sp.toString();
    const canonicalPath = qs ? `/?${qs}` : "/";

    return {
        title,
        description,
        alternates: { canonical: canonicalPath },
        openGraph: {
            title,
            description,
            url: canonicalPath,
            type: "website",
            siteName: "핫딜리스트",
            locale: "ko_KR",
        },
        twitter: {
            card: "summary",
            title,
            description,
        },
    };
}

export default async function Home({
    searchParams,
}: {
    searchParams: Promise<SearchParamsRecord>;
}) {
    const sp = toURLSearchParams(await searchParams);
    const { filter, keyword } = parseFilterParams(sp);

    const queryClient = new QueryClient({
        defaultOptions: { queries: { staleTime: 30_000 } },
    });

    // generateMetadata와 공유되는 cached 호출. 동일 요청 내에서는 backend로 한 번만 나감.
    const categoriesRaw = await getCategoriesRaw();
    if (categoriesRaw.length) {
        // 클라이언트가 useListCategories()로 즉시 캐시 hit 하도록 raw 응답을 dehydrate에 포함.
        queryClient.setQueryData(getListCategoriesQueryKey(), categoriesRaw);
    }
    const categoryTree = toCategoryNodes(categoriesRaw);

    // 플랫폼은 메타에 영향 없으므로 page에서만 prefetch
    await queryClient
        .prefetchQuery({
            queryKey: getListPlatformsQueryKey(),
            queryFn: ({ signal }) => listPlatforms(signal),
        })
        .catch(() => undefined);

    const categoryCodes = filter.categoryCode
        ? Array.from(getCategorySubtreeCodes(categoryTree, filter.categoryCode))
        : undefined;

    const params = {
        size: 40,
        keyword: keyword || undefined,
        categories: categoryCodes && categoryCodes.length ? categoryCodes : undefined,
        platforms: filter.platforms.length ? filter.platforms : undefined,
        minPrice: filter.priceMin ? Number(filter.priceMin) : undefined,
        maxPrice: filter.priceMax ? Number(filter.priceMax) : undefined,
    };

    await queryClient
        .prefetchInfiniteQuery({
            // orval이 만들어주는 옵션 객체를 그대로 사용 (queryKey/queryFn 일치 보장)
            ...getListHotdealsInfiniteQueryOptions(params),
            initialPageParam: undefined,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any)
        .catch(() => undefined);

    const dehydrated = dehydrate(queryClient);

    return (
        <HydrationBoundary state={dehydrated}>
            <HomeClient initialFilter={filter} initialKeyword={keyword} />
        </HydrationBoundary>
    );
}
