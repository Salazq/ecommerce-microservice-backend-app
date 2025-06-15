# Usar la imagen base oficial de nginx
FROM nginx:latest


# Eliminar la configuración por defecto
RUN rm /etc/nginx/conf.d/default.conf

# Copiar la configuración principal de nginx
COPY nginx-main.conf /etc/nginx/nginx.conf

# Copiar nuestra configuración de servidor
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Exponer el puerto 80
EXPOSE 80

# Comando por defecto (ya incluido en la imagen base de nginx)
CMD ["nginx", "-g", "daemon off;"]
