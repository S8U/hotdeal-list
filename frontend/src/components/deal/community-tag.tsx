import type { PlatformType } from "@/lib/communities";

type CommunityTagProps = {
    platformType: PlatformType;
    platformCommunityMap: Record<string, string>;
    onClick?: () => void;
};

export function CommunityTag({ platformType, platformCommunityMap, onClick }: CommunityTagProps) {
    const communityName = platformCommunityMap[platformType];
    if (!communityName) return null;

    if (onClick) {
        return (
            <button
                type="button"
                onClick={onClick}
                className="inline-flex w-fit cursor-pointer items-center rounded-sm bg-muted px-1.5 py-0.5 text-[11px] font-medium text-muted-foreground hover:text-primary"
            >
                {communityName}
            </button>
        );
    }

    return (
        <span className="inline-flex w-fit items-center rounded-sm bg-muted px-1.5 py-0.5 text-[11px] font-medium text-muted-foreground">
            {communityName}
        </span>
    );
}
