export function formatPrice(value: number): string {
    return `${value.toLocaleString("ko-KR")}원`;
}

export function formatCompact(value: number): string {
    if (value >= 10000) return `${(value / 10000).toFixed(value >= 100000 ? 0 : 1)}만`;
    if (value >= 1000) return `${(value / 1000).toFixed(1)}k`;
    return value.toString();
}

export function formatRelativeTime(iso: string): string {
    const date = new Date(iso);
    const diffMs = Date.now() - date.getTime();
    const sec = Math.floor(diffMs / 1000);
    if (sec < 60) return "방금";
    const min = Math.floor(sec / 60);
    if (min < 60) return `${min}분 전`;
    const hour = Math.floor(min / 60);
    if (hour < 24) return `${hour}시간 전`;
    const day = Math.floor(hour / 24);
    if (day < 30) return `${day}일 전`;
    const month = Math.floor(day / 30);
    if (month < 12) return `${month}개월 전`;
    const year = Math.max(1, Math.floor(day / 365));
    return `${year}년 전`;
}
