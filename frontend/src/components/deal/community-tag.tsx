import type { PlatformType } from "@/lib/communities";

type CommunityTagProps = {
    platformType: PlatformType;
    platformCommunityMap: Record<string, string>;
};

export function CommunityTag({ platformType, platformCommunityMap }: CommunityTagProps) {
    const communityName = platformCommunityMap[platformType];
    if (!communityName) return null;
    return (
        <span className="inline-flex w-fit items-center rounded-sm bg-muted px-1.5 py-0.5 text-[11px] font-medium text-zinc-500">
            {communityName}
        </span>
    );
}
