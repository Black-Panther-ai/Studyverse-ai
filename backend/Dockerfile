# Multi-stage Dockerfile using Node 22 Alpine
FROM node:22-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

COPY tsconfig.json ./
COPY src ./src

RUN npm run build

FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=8080

COPY package.json package-lock.json* ./
RUN npm ci --only=production

COPY --from=builder /app/dist ./dist

EXPOSE 8080

CMD ["node", "dist/server.js"]
