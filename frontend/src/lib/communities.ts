import type { ListHotdealsPlatformsItem } from "@/api/generated/model";
import type { PlatformResponse } from "@/api/generated/model";

export type PlatformType =
    (typeof ListHotdealsPlatformsItem)[keyof typeof ListHotdealsPlatformsItem];

/** 사이트(커뮤니티) 단위로 그룹핑된 플랫폼 */
export type CommunityGroup = {
    communityName: string;
    platforms: PlatformType[];
};

export function toCommunityGroups(raw: PlatformResponse[] | undefined): CommunityGroup[] {
    if (!raw) return [];
    const map = new Map<string, PlatformType[]>();
    for (const p of raw) {
        if (!p.code || !p.communityName) continue;
        const list = map.get(p.communityName) ?? [];
        list.push(p.code as PlatformType);
        map.set(p.communityName, list);
    }
    return Array.from(map, ([communityName, platforms]) => ({ communityName, platforms }));
}

/** platformType → communityName 역방향 맵 */
export function toPlatformCommunityMap(
    groups: CommunityGroup[],
): Record<string, string> {
    const map: Record<string, string> = {};
    for (const g of groups) {
        for (const p of g.platforms) {
            map[p] = g.communityName;
        }
    }
    return map;
}
