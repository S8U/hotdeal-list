import { writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";

const URL = process.env.OPENAPI_URL ?? "http://localhost:8080/v3/api-docs";
const OUT = resolve("src/api/openapi.json");

const res = await fetch(URL);
if (!res.ok) {
    console.error(`Failed to fetch ${URL}: ${res.status}`);
    process.exit(1);
}
const spec = await res.json();

// orval은 컴포넌트 키에 공백을 허용하지 않음 → 공백을 밑줄로 치환
const renameKeys = (obj) => {
    if (!obj || typeof obj !== "object") return obj;
    const out = {};
    for (const [k, v] of Object.entries(obj)) {
        out[k.replace(/\s+/g, "_")] = v;
    }
    return out;
};

if (spec.components?.securitySchemes) {
    spec.components.securitySchemes = renameKeys(spec.components.securitySchemes);
}
if (Array.isArray(spec.security)) {
    spec.security = spec.security.map(renameKeys);
}

mkdirSync(dirname(OUT), { recursive: true });
writeFileSync(OUT, JSON.stringify(spec, null, 2));
console.log(`OpenAPI spec saved to ${OUT}`);
