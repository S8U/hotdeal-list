import { ListHotdealsPlatformsItem } from "@/api/generated/model";

export type PlatformType =
    (typeof ListHotdealsPlatformsItem)[keyof typeof ListHotdealsPlatformsItem];

export type Community = {
    platformType: PlatformType;
    label: string;
    color: string;
};

export const COMMUNITIES: Community[] = [
    { platformType: "COOLENJOY_JIRUM", label: "쿨엔조이", color: "#2563eb" },
    { platformType: "QUASARZONE_JIRUM", label: "퀘이사존", color: "#9333ea" },
    { platformType: "QUASARZONE_TASEYO", label: "퀘이사존", color: "#a855f7" },
    { platformType: "CLIEN_ALTTEUL", label: "클리앙", color: "#16a34a" },
    { platformType: "RULIWEB_HOTDEAL", label: "루리웹", color: "#ea580c" },
    { platformType: "PPOMPPU_PPOMPPU", label: "뽐뿌", color: "#0891b2" },
    { platformType: "PPOMPPU_HOTDEAL", label: "뽐뿌", color: "#0891b2" },
    { platformType: "PPOMPPU_OVERSEAS_HOTDEAL", label: "뽐뿌 해외", color: "#0e7490" },
];

export const COMMUNITY_MAP: Record<PlatformType, Community> = COMMUNITIES.reduce(
    (acc, c) => {
        acc[c.platformType] = c;
        return acc;
    },
    {} as Record<PlatformType, Community>,
);
