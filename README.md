# CoworkHub

Aplicación web y API REST para administrar sedes, salas, miembros, planes y reservas de una red de coworking. Está implementada con Java 21, Spring Boot, Spring Data JPA y MySQL.

La aplicación no tiene cuentas de acceso ni requiere iniciar sesión. La interfaz y las rutas de la API están disponibles directamente. No la expongas a internet sin agregar autenticación y controles de acceso.

## Requisitos

- JDK 21 o superior (el repositorio incluye Maven Wrapper).
- MySQL 8.0+.

## Ejecutar

Configura la conexión a MySQL mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` si los valores locales predeterminados no coinciden con tu instalación. Después, desde PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Abre [http://localhost:8080](http://localhost:8080). La interfaz está hecha con HTML, CSS y JavaScript en `src/main/resources/static`; no requiere un proceso de compilación adicional.

Para ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

Las pruebas usan una base H2 aislada. `src/main/resources/import.sql` carga sedes, salas, equipamiento, planes, miembros, servicios y reservas de demostración al crear el esquema.

## API

- Sedes, salas, equipamiento, servicios y planes: `/api/sedes`, `/api/salas`, `/api/equipamientos`, `/api/servicios-adicionales`, `/api/planes`
- Miembros y consumo: `/api/miembros`, `/api/miembros/{id}/consumo?mes=yyyy-MM`
- Disponibilidad: `/api/disponibilidad?sedeId=1&fecha=2030-06-03&horaInicio=10:00&horaFin=11:00`
- Reservas: `/api/reservas`, `/api/reservas/{id}` y acciones `/confirmar`, `/cancelar`, `/completar`
- Documentación: [Swagger UI](http://localhost:8080/doc/swagger-ui.html) y [OpenAPI](http://localhost:8080/v3/api-docs)
- Estado: [Health](http://localhost:8080/actuator/health)

Las rutas no requieren token. La búsqueda de reservas necesita al menos un filtro, como `miembroId`, `salaId`, `fecha` o `estado`.

Ejemplo de creación de miembro:

```json
{
  "documentNumber": "DOC-2001",
  "fullName": "Nombre Apellido",
  "email": "persona@example.com",
  "planId": 1
}
```

## Base de datos existente

Al actualizar una base usada por versiones anteriores, Hibernate con `DDL_AUTO=update` no elimina automáticamente tablas ni columnas antiguas. Haz una copia de seguridad y revisa la base seleccionada antes de ejecutar el script manual [migrate-remove-login-schema.sql](docs/migrate-remove-login-schema.sql), que elimina la tabla y la relación de las antiguas cuentas de acceso. Conserva `DB_USERNAME` y `DB_PASSWORD`: son únicamente para la conexión de Spring a MySQL, no son cuentas de la aplicación.
