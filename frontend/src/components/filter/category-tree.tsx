"use client";

import { ChevronRight } from "lucide-react";
import { useEffect, useState } from "react";

import { cn } from "@/lib/utils";
import type { CategoryNode } from "@/lib/types";

type Size = "sm" | "lg";

type CategoryTreeProps = {
    nodes: CategoryNode[];
    value: string | null;
    onChange: (code: string | null) => void;
    depth?: number;
    forceExpandedCodes?: ReadonlySet<string>;
    size?: Size;
};

export function CategoryTree({
    nodes,
    value,
    onChange,
    depth = 0,
    forceExpandedCodes,
    size = "sm",
}: CategoryTreeProps) {
    return (
        <ul className="flex flex-col">
            {nodes.map((node) => (
                <CategoryTreeItem
                    key={node.code}
                    node={node}
                    value={value}
                    onChange={onChange}
                    depth={depth}
                    forceExpandedCodes={forceExpandedCodes}
                    size={size}
                />
            ))}
        </ul>
    );
}

function CategoryTreeItem({
    node,
    value,
    onChange,
    depth,
    forceExpandedCodes,
    size,
}: {
    node: CategoryNode;
    value: string | null;
    onChange: (code: string | null) => void;
    depth: number;
    forceExpandedCodes?: ReadonlySet<string>;
    size: Size;
}) {
    const hasChildren = node.children.length > 0;
    const [open, setOpen] = useState(false);
    const shouldForceOpen = !!forceExpandedCodes?.has(node.code);
    useEffect(() => {
        if (shouldForceOpen) setOpen(true);
    }, [shouldForceOpen]);
    const isActive = value === node.code;

    const lg = size === "lg";

    return (
        <li>
            <div
                className={cn(
                    "group flex items-center rounded-md",
                    lg ? "gap-0.5 py-1 text-base" : "gap-1 px-1.5 py-1 text-sm",
                    isActive && "bg-primary/10 text-primary",
                )}
                style={{
                    paddingLeft: `${depth * (lg ? 16 : 12) + (lg ? (depth === 0 ? 0 : 8) : 6)}px`,
                    paddingRight: lg ? "8px" : undefined,
                }}
            >
                {hasChildren ? (
                    <button
                        type="button"
                        onClick={() => setOpen((v) => !v)}
                        className={cn(
                            "flex shrink-0 cursor-pointer items-center justify-center rounded text-muted-foreground hover:bg-muted",
                            lg ? "size-7" : "size-5",
                        )}
                        aria-label={open ? "접기" : "펼치기"}
                    >
                        <ChevronRight
                            className={cn(
                                "transition-transform",
                                lg ? "size-4" : "size-3",
                                open && "rotate-90",
                            )}
                        />
                    </button>
                ) : (
                    <span
                        className={cn("shrink-0", lg ? "size-7" : "size-5")}
                        aria-hidden
                    />
                )}

                <button
                    type="button"
                    onClick={() => onChange(isActive ? null : node.code)}
                    className={cn(
                        "flex-1 cursor-pointer truncate text-left hover:text-foreground",
                        isActive ? "font-medium text-primary" : "text-foreground/80",
                    )}
                >
                    {node.name}
                </button>
            </div>

            {hasChildren && open ? (
                <CategoryTree
                    nodes={node.children}
                    value={value}
                    onChange={onChange}
                    depth={depth + 1}
                    forceExpandedCodes={forceExpandedCodes}
                    size={size}
                />
            ) : null}
        </li>
    );
}
