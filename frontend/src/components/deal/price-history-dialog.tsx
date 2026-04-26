"use client";

import { useRef, useState } from "react";
import { TrendingUp, Loader2, ExternalLink } from "lucide-react";
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
import type { HotdealSummary } from "@/api/generated/model";
import { formatPrice } from "@/lib/format";
import { gtmEvent } from "@/lib/gtm";
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
        <Dialog
            open={open}
            onOpenChange={(next) => {
                setOpen(next);
                if (next) {
                    gtmEvent("price_history_open", { hotdealId, productName });
                }
            }}
        >
            <DialogTrigger
                render={
                    <button
                        type="button"
                        className="relative z-10 inline-flex cursor-pointer items-center gap-0.5 text-xs text-muted-foreground hover:text-primary"
                        aria-label="가격 추이"
                    >
                        <TrendingUp className="size-3" />
                    </button>
                }
            />
            <DialogPortal>
                <DialogOverlay />
                <DialogContent className="sm:max-w-4xl">
                    <DialogHeader className="overflow-hidden">
                        <DialogTitle className="text-base">가격 추이</DialogTitle>
                        <DialogDescription className="min-w-0 truncate text-sm">
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

type ChartDataItem = {
    date: string;
    label: string;
    min: number;
    max: number;
    avg: number;
    count: number;
    hotdeals: HotdealSummary[];
};

function PriceHistoryChart({ hotdealId }: { hotdealId: number }) {
    const { data, isLoading, isError } = useGetPriceHistory(hotdealId);
    const [selectedDate, setSelectedDate] = useState<string | null>(null);

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

    if ((data.totalSimilarCount ?? 0) <= 1) {
        return (
            <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">
                가격 데이터가 부족합니다
            </div>
        );
    }

    const chartData: ChartDataItem[] = [...data.priceHistory]
        .reverse()
        .map((d) => ({
            date: d.date ?? "",
            label: formatDateShort(d.date ?? ""),
            min: d.minPrice ?? 0,
            max: d.maxPrice ?? 0,
            avg: d.avgPrice ?? 0,
            count: d.count ?? 0,
            hotdeals: d.hotdeals ?? [],
        }));

    const selectedItem = chartData.find((d) => d.date === selectedDate) ?? null;

    const allPrices = chartData.flatMap((d) => [d.min, d.max]).filter((p) => p > 0);
    const globalMin = Math.min(...allPrices);
    const globalMax = Math.max(...allPrices);
    const globalAvg = chartData.reduce((sum, d) => sum + d.avg, 0) / chartData.length;

    return (
        <div className="flex flex-col gap-4 lg:flex-row">
            <div className="min-w-0 lg:flex-1">
                <PriceChart
                    chartData={chartData}
                    selectedDate={selectedDate}
                    onSelectDate={setSelectedDate}
                />
                <div className="mt-3 grid grid-cols-3 gap-2 text-center text-sm">
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
                <p className="mt-3 text-center text-xs text-muted-foreground">
                    총 {data.totalSimilarCount}건의 유사 상품 기준
                </p>
            </div>

            {selectedItem ? (
                <div className="w-full border-t pt-4 lg:w-72 lg:border-t-0 lg:border-l lg:pt-0 lg:pl-4">
                    <DealListPanel selectedDate={selectedDate} selectedItem={selectedItem} sourceHotdealId={hotdealId} />
                </div>
            ) : (
                <p className="text-center text-xs text-muted-foreground lg:flex lg:w-72 lg:items-center lg:justify-center lg:border-l lg:pl-4">
                    차트의 포인트를 클릭하면
                    <br />
                    해당 날짜의 핫딜을 볼 수 있습니다
                </p>
            )}
        </div>
    );
}

function PriceChart({
    chartData,
    selectedDate,
    onSelectDate,
}: {
    chartData: ChartDataItem[];
    selectedDate: string | null;
    onSelectDate: (date: string) => void;
}) {
    const activeIndexRef = useRef<number | null>(null);

    const allPrices = chartData.flatMap((d) => [d.min, d.max]).filter((p) => p > 0);
    const globalMin = Math.min(...allPrices);
    const globalMax = Math.max(...allPrices);

    const minIndex = chartData.findIndex((d) => d.avg === globalMin || d.min === globalMin);
    const maxIndex = chartData.findIndex((d) => d.avg === globalMax || d.max === globalMax);
    const selectedIndex = chartData.findIndex((d) => d.date === selectedDate);

    const total = chartData.length;

    const getAnchor = (index: number) => {
        if (index <= 1) return "start";
        if (index >= total - 2) return "end";
        return "middle";
    };

    const renderDot = (props: any) => {
        const { cx, cy, index } = props;

        if (index === selectedIndex && index !== minIndex && index !== maxIndex) {
            return (
                <circle key={`dot-sel-${index}`} cx={cx} cy={cy} r={5} fill="hsl(var(--primary))" stroke="#fff" strokeWidth={2} />
            );
        }
        if (index === minIndex) {
            const anchor = getAnchor(index);
            return (
                <g key={`dot-min-${index}`}>
                    <circle cx={cx} cy={cy} r={4} fill="#2563eb" stroke="#fff" strokeWidth={2} />
                    <text x={cx} y={cy + 16} textAnchor={anchor} fill="#2563eb" fontSize={10} fontWeight={600}>
                        최저 {formatPrice(globalMin)}
                    </text>
                </g>
            );
        }
        if (index === maxIndex) {
            const anchor = getAnchor(index);
            return (
                <g key={`dot-max-${index}`}>
                    <circle cx={cx} cy={cy} r={4} fill="#ef4444" stroke="#fff" strokeWidth={2} />
                    <text x={cx} y={cy - 8} textAnchor={anchor} fill="#ef4444" fontSize={10} fontWeight={600}>
                        최고 {formatPrice(globalMax)}
                    </text>
                </g>
            );
        }
        return <circle key={`dot-${index}`} cx={cx} cy={cy} r={3} fill="hsl(var(--primary))" />;
    };

    const handleMouseMove = (state: any) => {
        if (state?.activeTooltipIndex != null) {
            activeIndexRef.current = state.activeTooltipIndex;
        }
    };

    const handleMouseLeave = () => {
        activeIndexRef.current = null;
    };

    const handleContainerClick = () => {
        const idx = activeIndexRef.current;
        if (idx != null && chartData[idx]) {
            onSelectDate(chartData[idx].date);
        }
    };

    return (
        <div className="h-64 [&_svg]:overflow-visible" onClick={handleContainerClick} style={{ cursor: "pointer" }}>
            <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartData} margin={{ top: 25, right: 15, left: 0, bottom: 20 }} onMouseMove={handleMouseMove} onMouseLeave={handleMouseLeave}>
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
                        dot={renderDot}
                        activeDot={{ r: 5 }}
                    />
                </AreaChart>
            </ResponsiveContainer>
        </div>
    );
}

function DealListPanel({
    selectedDate,
    selectedItem,
    sourceHotdealId,
}: {
    selectedDate: string | null;
    selectedItem: ChartDataItem;
    sourceHotdealId: number;
}) {
    if (!selectedItem.hotdeals.length) {
        return (
            <div className="flex h-full min-h-32 items-center justify-center text-sm text-muted-foreground">
                해당 날짜에 핫딜 데이터가 없습니다
            </div>
        );
    }

    return (
        <div>
            <h4 className="mb-2 text-sm font-semibold">
                {selectedItem.date}{" "}
                <span className="font-normal text-muted-foreground">({selectedItem.hotdeals.length}건)</span>
            </h4>
            <ul className="space-y-2 overflow-y-auto lg:max-h-72">
                {selectedItem.hotdeals.map((deal) => (
                    <li key={deal.id}>
                        <a
                            href={deal.url}
                            target="_blank"
                            rel="noreferrer"
                            onClick={() =>
                                gtmEvent("price_history_deal_click", {
                                    hotdealId: deal.id,
                                    sourceHotdealId,
                                    date: selectedDate,
                                })
                            }
                            className="group/deal flex items-start gap-2 rounded-md border p-2 transition-colors hover:bg-muted"
                        >
                            {deal.thumbnailUrl ? (
                                // eslint-disable-next-line @next/next/no-img-element
                                <img
                                    src={deal.thumbnailUrl}
                                    alt=""
                                    className="size-10 shrink-0 rounded object-cover"
                                    loading="lazy"
                                />
                            ) : null}
                            <div className="min-w-0 flex-1">
                                <p className="line-clamp-2 text-xs font-medium leading-snug group-hover/deal:text-primary">
                                    {deal.productName ?? deal.title}
                                </p>
                                {typeof deal.price === "number" && deal.price > 0 ? (
                                    <p className="mt-0.5 text-xs font-bold text-foreground">
                                        {formatPrice(deal.price)}
                                    </p>
                                ) : null}
                            </div>
                            <ExternalLink className="mt-0.5 size-3 shrink-0 text-muted-foreground" />
                        </a>
                    </li>
                ))}
            </ul>
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
