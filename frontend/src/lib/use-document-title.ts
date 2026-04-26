"use client";

import { useEffect } from "react";

/**
 * Next.js 메타데이터 시스템이 클라이언트 라우팅 중 `<title>`을 새 엘리먼트로 교체하는 경우가 있어,
 * `<head>` 전체를 관찰해 title이 변경/교체될 때마다 원하는 값으로 복원한다.
 */
export function useDocumentTitle(title: string) {
    useEffect(() => {
        if (typeof document === "undefined") return;

        const apply = () => {
            if (document.title !== title) document.title = title;
        };
        apply();

        const observer = new MutationObserver(apply);
        observer.observe(document.head, { childList: true, subtree: true, characterData: true });
        return () => observer.disconnect();
    }, [title]);
}
