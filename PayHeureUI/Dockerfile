FROM node:18 AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

RUN npm run build


FROM nginx:alpine

RUN apk add --no-cache gettext

COPY --from=build /app/dist/pay-heure-ui/browser/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Valeur par défaut, surchargeable via `environment:` dans docker-compose.yml.
ENV API_BASE_URL="http://localhost:8080/api"

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
