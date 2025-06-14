FROM nginx:alpine

RUN apk add --no-cache nginx-mod-http-substitutions-filter

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]