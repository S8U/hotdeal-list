import { defineConfig } from "orval";

export default defineConfig({
    hotdeal: {
        input: "./src/api/openapi.json",
        output: {
            mode: "tags-split",
            target: "src/api/generated",
            schemas: "src/api/generated/model",
            client: "react-query",
            httpClient: "axios",
            clean: true,
            override: {
                mutator: {
                    path: "src/api/axios.ts",
                    name: "customInstance",
                },
                operations: {
                    listHotdeals: {
                        query: {
                            useQuery: true,
                            useInfinite: true,
                            useInfiniteQueryParam: "cursor",
                        },
                    },
                },
            },
        },
    },
});
