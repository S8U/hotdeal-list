"use client";

import { useState } from "react";
import { TrendingUp, Loader2 } from "lucide-react";
import {
    AreaChart,
    Area,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip as RechartsTooltip,
    ResponsiveContainer,
} from "recharts";

import { useGetPriceHistory } from "@/api/generated/hotdeal/hotdeal";
import { formatPrice } from "@/lib/format";
import {
    Dialog,
    DialogTrigger,
    DialogPortal,
    DialogOverlay,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogClose,
} from "@/components/ui/dialog";

type PriceHistoryDialogProps = {
    hotdealId: number;
    productName?: string | null;
};

export function PriceHistoryButton({ hotdealId, productName }: PriceHistoryDialogProps) {
    const [open, setOpen] = useState(false);

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger
                render={
                    <button
                        type="button"
                        className="relative z-10 inline-flex cursor-pointer items-center gap-0.5 rounded-md px-1.5 py-0.5 text-xs text-muted-foreground hover:bg-muted hover:text-primary"
                        aria-label="가격 추이"
                    >
                        <TrendingUp className="size-3.5" />
                    </button>
                }
            />
            <DialogPortal>
                <DialogOverlay />
                <DialogContent className="max-w-lg">
                    <DialogHeader>
                        <DialogTitle className="text-base">가격 추이</DialogTitle>
                        <DialogDescription className="truncate text-sm">
                            {productName ?? ""}
                        </DialogDescription>
                    </DialogHeader>
                    {open ? (
                        <PriceHistoryChart hotdealId={hotdealId} />
                    ) : null}
                </DialogContent>
            </DialogPortal>
        </Dialog>
    );
}

function PriceHistoryChart({ hotdealId }: { hotdealId: number }) {
    const { data, isLoading, isError } = useGetPriceHistory(hotdealId);

    if (isLoading) {
        return (
            <div className="flex h-48 items-center justify-center">
                <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
        );
    }

    if (isError || !data?.priceHistory?.length) {
        return (
            <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">
                가격 데이터가 없습니다
            </div>
        );
    }

    const chartData = [...data.priceHistory]
        .reverse()
        .map((d) => ({
            date: d.date ?? "",
            label: formatDateShort(d.date ?? ""),
            min: d.minPrice ?? 0,
            max: d.maxPrice ?? 0,
            avg: d.avgPrice ?? 0,
            count: d.count ?? 0,
        }));

    const allPrices = chartData.flatMap((d) => [d.min, d.max]).filter((p) => p > 0);
    const globalMin = Math.min(...allPrices);
    const globalMax = Math.max(...allPrices);
    const globalAvg = chartData.reduce((sum, d) => sum + d.avg, 0) / chartData.length;

    return (
        <div className="space-y-3">
            <div className="h-52">
                <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={chartData} margin={{ top: 5, right: 5, left: 0, bottom: 0 }}>
                        <defs>
                            <linearGradient id="priceGradient" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="hsl(var(--primary))" stopOpacity={0.2} />
                                <stop offset="95%" stopColor="hsl(var(--primary))" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                        <XAxis
                            dataKey="label"
                            tick={{ fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
                            tickLine={false}
                            axisLine={false}
                        />
                        <YAxis
                            tick={{ fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
                            tickLine={false}
                            axisLine={false}
                            tickFormatter={(v: number) => `${Math.round(v / 10000)}만`}
                            width={45}
                        />
                        <RechartsTooltip content={<CustomTooltip />} />
                        <Area
                            type="monotone"
                            dataKey="avg"
                            stroke="hsl(var(--primary))"
                            strokeWidth={2}
                            fill="url(#priceGradient)"
                            dot={{ r: 3, fill: "hsl(var(--primary))" }}
                            activeDot={{ r: 5 }}
                        />
                    </AreaChart>
                </ResponsiveContainer>
            </div>
            <div className="grid grid-cols-3 gap-2 text-center text-sm">
                <div className="rounded-md bg-muted p-2">
                    <p className="text-xs text-muted-foreground">최저</p>
                    <p className="font-semibold text-blue-600">{formatPrice(globalMin)}</p>
                </div>
                <div className="rounded-md bg-muted p-2">
                    <p className="text-xs text-muted-foreground">평균</p>
                    <p className="font-semibold">{formatPrice(Math.round(globalAvg))}</p>
                </div>
                <div className="rounded-md bg-muted p-2">
                    <p className="text-xs text-muted-foreground">최고</p>
                    <p className="font-semibold text-red-500">{formatPrice(globalMax)}</p>
                </div>
            </div>
            <p className="text-center text-xs text-muted-foreground">
                총 {data.totalSimilarCount}건의 유사 상품 기준
            </p>
        </div>
    );
}

function CustomTooltip({ active, payload, label }: any) {
    if (!active || !payload?.length) return null;
    const d = payload[0].payload;
    return (
        <div className="rounded-md border bg-background px-3 py-2 text-sm shadow-md">
            <p className="font-medium">{d.date}</p>
            <p className="text-muted-foreground">평균 {formatPrice(d.avg)}</p>
            <p className="text-xs text-muted-foreground">
                {formatPrice(d.min)} ~ {formatPrice(d.max)} ({d.count}건)
            </p>
        </div>
    );
}

function formatDateShort(dateStr: string) {
    if (!dateStr) return "";
    const parts = dateStr.split("-");
    return `${parts[1]}/${parts[2]}`;
}
