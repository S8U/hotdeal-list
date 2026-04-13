export function formatPrice(value: number): string {
    return `${value.toLocaleString("ko-KR")}원`;
}

export function formatCompact(value: number): string {
    if (value >= 10000) return `${(value / 10000).toFixed(value >= 100000 ? 0 : 1)}만`;
    if (value >= 1000) return `${(value / 1000).toFixed(1)}k`;
    return value.toString();
}
