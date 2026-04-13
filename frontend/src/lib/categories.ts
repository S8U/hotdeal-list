import type { CategoryNode } from "./types";

export function findCategoryPath(
    tree: CategoryNode[],
    targetCode: string,
    trail: CategoryNode[] = [],
): CategoryNode[] | null {
    for (const node of tree) {
        const next = [...trail, node];
        if (node.code === targetCode) return next;
        if (node.children.length) {
            const found = findCategoryPath(node.children, targetCode, next);
            if (found) return found;
        }
    }
    return null;
}

export function collectDescendantCodes(root: CategoryNode): Set<string> {
    const codes = new Set<string>();
    const walk = (node: CategoryNode) => {
        codes.add(node.code);
        node.children.forEach(walk);
    };
    walk(root);
    return codes;
}

function findNode(tree: CategoryNode[], targetCode: string): CategoryNode | null {
    for (const node of tree) {
        if (node.code === targetCode) return node;
        if (node.children.length) {
            const found = findNode(node.children, targetCode);
            if (found) return found;
        }
    }
    return null;
}

export function pickLeafCode(
    tree: CategoryNode[],
    codes: string[] | undefined,
): string | undefined {
    if (!codes?.length) return undefined;
    let best: { code: string; depth: number } | null = null;
    for (const code of codes) {
        const path = findCategoryPath(tree, code);
        if (!path) continue;
        const depth = path.length;
        if (!best || depth > best.depth) best = { code, depth };
    }
    return best?.code ?? codes.at(-1);
}

export function getCategorySubtreeCodes(
    tree: CategoryNode[],
    rootCode: string,
): Set<string> {
    const node = findNode(tree, rootCode);
    return node ? collectDescendantCodes(node) : new Set();
}
