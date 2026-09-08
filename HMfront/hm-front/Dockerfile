# syntax=docker/dockerfile:1
# ---------- 构建阶段 ----------
FROM node:22-alpine AS build
WORKDIR /app

# .yarnrc.yml 引用的 yarn release 文件未纳入版本控制，构建时按官方源补齐
RUN corepack enable \
    && mkdir -p .yarn/releases \
    && wget -qO .yarn/releases/yarn-4.12.0.cjs \
       https://repo.yarnpkg.com/4.12.0/packages/yarnpkg-cli/bin/yarn.js

COPY .yarnrc.yml package.json yarn.lock ./
RUN yarn install --immutable

COPY . .
# docker 模式使用相对 API 地址（/hm），由 nginx 同源反代到后端
RUN yarn build --mode docker

# ---------- 运行阶段 ----------
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
