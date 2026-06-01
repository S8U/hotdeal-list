import type { HotdealResponse } from "@/api/generated/model";

const SITE_NAME = "핫딜리스트";

/** 사이트 전역 WebSite 스키마 + 검색 액션 (홈에서 1회 출력) */
export function buildWebSiteJsonLd(siteUrl: string) {
    return {
        "@context": "https://schema.org",
        "@type": "WebSite",
        name: SITE_NAME,
        alternateName: ["핫딜 리스트", "핫딜 모음"],
        url: siteUrl,
        description: "여러 커뮤니티의 실시간 핫딜을 한 곳에서 모아 보는 핫딜 애그리게이터",
        potentialAction: {
            "@type": "SearchAction",
            target: {
                "@type": "EntryPoint",
                urlTemplate: `${siteUrl}/?q={search_term_string}`,
            },
            "query-input": "required name=search_term_string",
        },
    };
}

/** 핫딜 목록을 ItemList(각 항목 Product+Offer)로 마크업 */
export function buildItemListJsonLd(siteUrl: string, deals: HotdealResponse[]) {
    const elements = deals
        .filter((d) => (d.productName || d.title) && d.url)
        .slice(0, 40)
        .map((deal, index) => {
            const name = (deal.productName || deal.title || "").trim();
            const product: Record<string, unknown> = {
                "@type": "Product",
                name,
                url: deal.url,
            };
            if (deal.thumbnailUrl) product.image = deal.thumbnailUrl;
            if (typeof deal.price === "number" && deal.price > 0) {
                product.offers = {
                    "@type": "Offer",
                    price: deal.price,
                    priceCurrency: deal.currencyUnit || "KRW",
                    availability: deal.ended
                        ? "https://schema.org/Discontinued"
                        : "https://schema.org/InStock",
                    url: deal.url,
                };
            }
            return {
                "@type": "ListItem",
                position: index + 1,
                item: product,
            };
        });

    return {
        "@context": "https://schema.org",
        "@type": "ItemList",
        name: `${SITE_NAME} 실시간 핫딜`,
        url: siteUrl,
        numberOfItems: elements.length,
        itemListElement: elements,
    };
}
