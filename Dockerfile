# 우행통신 — 웹+API 통합 배포 (한 주소)
# Railway Root Directory: 저장소 루트 (.)
# Public URL 예: https://rlawldn7787-rgb-production.up.railway.app

FROM node:22-bookworm-slim AS deps
WORKDIR /app
COPY package.json package-lock.json ./
COPY apps/api/package.json apps/api/
COPY apps/web/package.json apps/web/
COPY packages/shared/package.json packages/shared/
RUN npm install

FROM node:22-bookworm-slim AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY --from=deps /app/package.json /app/package-lock.json ./
COPY apps/api apps/api
COPY apps/web apps/web
COPY packages packages
COPY package.json package-lock.json ./

RUN npm run build -w @woohaeng/api

# 같은 도메인에서 API 호출 (브라우저 relative / SSR은 서버에서 same host)
ARG NEXT_PUBLIC_API_URL=
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
RUN npm run build -w @woohaeng/web

FROM node:22-bookworm-slim AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV UPLOADS_DIR=/app/uploads

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/uploads

COPY --from=build /app/package.json /app/package-lock.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/apps/api/package.json ./apps/api/package.json
COPY --from=build /app/apps/api/dist ./apps/api/dist
COPY --from=build /app/apps/web/package.json ./apps/web/package.json
COPY --from=build /app/apps/web/next.config.js ./apps/web/next.config.js
COPY --from=build /app/apps/web/server.js ./apps/web/server.js
COPY --from=build /app/apps/web/public ./apps/web/public
COPY --from=build /app/apps/web/.next ./apps/web/.next
COPY --from=build /app/packages ./packages

WORKDIR /app/apps/web
EXPOSE 3000
CMD ["node", "server.js"]
