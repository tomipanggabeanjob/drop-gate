FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html
COPY --from=build /app/dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /var/log/nginx \
    && chmod -R 755 /usr/share/nginx/html \
    && sed -i -E 's#^(\s*pid\s+).*;#\1/tmp/nginx.pid;#' /etc/nginx/nginx.conf
USER nginx
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
