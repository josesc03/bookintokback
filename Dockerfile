# Usa imagen oficial de OpenJDK 21
FROM eclipse-temurin:21-jre-alpine

# Directorio dentro del contenedor
WORKDIR /app

# Copiar el archivo jar generado por shadowJar
COPY build/libs/bookintok-backend.jar app.jar

# Puerto que expone Ktor por defecto
EXPOSE 8080

# Comando para ejecutar la app
CMD ["java", "-jar", "app.jar"]