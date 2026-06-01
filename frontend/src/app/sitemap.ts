import type { MetadataRoute } from "next";

import { listCategories } from "@/api/generated/category/category";
import type { CategoryResponse } from "@/api/generated/model";
import { PARAM_KEYS } from "@/lib/filter-params";

type CategoryWithChildren = CategoryResponse & { children?: CategoryWithChildren[] };

// 백엔드 응답에 의존하지 않는 고정 커뮤니티 그룹. home의 platforms 파라미터는 콤마 결합 문자열.
const COMMUNITY_PLATFORM_GROUPS: string[][] = [
    ["COOLENJOY_JIRUM"],
    ["QUASARZONE_JIRUM", "QUASARZONE_TASEYO"],
    ["RULIWEB_HOTDEAL"],
    ["PPOMPPU_PPOMPPU", "PPOMPPU_HOTDEAL", "PPOMPPU_OVERSEAS_HOTDEAL"],
    ["CLIEN_ALTTEUL"],
];

function collectCategoryCodes(nodes: CategoryWithChildren[] | undefined, acc: string[] = []): string[] {
    if (!nodes) return acc;
    for (const node of nodes) {
        if (node.code) acc.push(node.code);
        if (node.children?.length) collectCategoryCodes(node.children, acc);
    }
    return acc;
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
    const base = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
    const lastModified = new Date();

    const entries: MetadataRoute.Sitemap = [
        {
            url: `${base}/`,
            lastModified,
            changeFrequency: "hourly",
            priority: 1,
        },
    ];

    // 커뮤니티별 필터 URL (백엔드 응답에 의존하지 않는 고정 목록)
    for (const platforms of COMMUNITY_PLATFORM_GROUPS) {
        const qs = new URLSearchParams();
        qs.set(PARAM_KEYS.platforms, platforms.join(","));
        entries.push({
            url: `${base}/?${qs.toString()}`,
            lastModified,
            changeFrequency: "hourly",
            priority: 0.7,
        });
    }

    // 카테고리별 필터 URL. 백엔드 실패 시 카테고리 URL 없이 진행.
    try {
        const raw = (await listCategories()) as CategoryWithChildren[] | undefined;
        for (const code of collectCategoryCodes(raw)) {
            const qs = new URLSearchParams();
            qs.set(PARAM_KEYS.category, code);
            entries.push({
                url: `${base}/?${qs.toString()}`,
                lastModified,
                changeFrequency: "hourly",
                priority: 0.6,
            });
        }
    } catch {
        // 카테고리 호출 실패는 무시 — 기본 URL들은 그대로 노출
    }

    return entries;
}
