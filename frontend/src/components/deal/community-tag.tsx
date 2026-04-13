import { COMMUNITY_MAP, type PlatformType } from "@/lib/communities";

type CommunityTagProps = {
    platformType: PlatformType;
};

export function CommunityTag({ platformType }: CommunityTagProps) {
    const community = COMMUNITY_MAP[platformType];
    if (!community) return null;
    return (
        <span className="inline-flex w-fit items-center rounded-sm bg-muted px-1.5 py-0.5 text-[11px] font-medium text-zinc-500">
            {community.label}
        </span>
    );
}
