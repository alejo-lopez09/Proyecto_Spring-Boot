# CoworkHub API

REST API para administrar sedes, salas, miembros, planes y reservas de una red de coworking. Implementada con Java 21, Spring Boot 4.1, Spring Data JPA, MySQL 8 y JWT. Swagger UI es la interfaz interactiva; también se incluye una colección Postman en `docs/CoworkHub.postman_collection.json`.

## Requisitos

- JDK 21 o superior y Maven (el repositorio incluye Maven Wrapper).
- MySQL 8.0+ o Docker.

## Preparar MySQL

### Docker

```powershell
docker run -d --name coworkhub-mysql -e MYSQL_DATABASE=coworkhub -e MYSQL_USER=coworkhub -e MYSQL_PASSWORD=coworkhub -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 -v coworkhub-datos:/var/lib/mysql mysql:8.4
docker logs -f coworkhub-mysql
```

Esperá a que MySQL indique `ready for connections`. Para detener/reiniciar el contenedor: `docker stop coworkhub-mysql` / `docker start coworkhub-mysql`.

### MySQL instalado localmente

Creá la base `coworkhub` y un usuario `coworkhub` con permisos sobre ella. Por defecto, la aplicación se conecta a `jdbc:mysql://localhost:3306/coworkhub` con usuario y contraseña `coworkhub`.

Se pueden cambiar los valores de conexión mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. `DDL_AUTO` permite sobreescribir el esquema (por defecto `create`). `create` vuelve a crear las tablas e importa los datos iniciales en cada arranque; usá una estrategia como `validate` o `none` al operar con datos que deban conservarse. Nunca uses las credenciales de desarrollo ni el secreto JWT incluido por defecto en producción.

## Ejecutar

Desde PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Para validar y ejecutar las pruebas sin una instancia MySQL:

```powershell
.\mvnw.cmd test
```

Las pruebas usan una base H2 aislada. El archivo `src/main/resources/import.sql` crea los datos de demostración automáticamente cuando Hibernate genera el esquema.

## Documentación y autenticación

- Swagger UI: [http://localhost:8080/doc/swagger-ui.html](http://localhost:8080/doc/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Exportación versionada: [`docs/openapi.json`](docs/openapi.json)
- Estado: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

Iniciá sesión con `POST /auth/login` y enviá el token recibido en `Authorization: Bearer <token>`. Los tokens duran 60 minutos. Swagger incluye el botón **Authorize**. El usuario y contraseña de demostración para todas las cuentas seed son `password`:

| Usuario | Rol | Miembro |
|---|---|---|
| `admin` | ADMIN | — |
| `reception` | RECEPTIONIST | — |
| `ana` | MEMBER | 1 |
| `mateo` | MEMBER | 2 |

`GET /auth/perfil` informa usuario, rol y `miembroId`. `MEMBER` solo puede consultar y gestionar sus propios datos y reservas; `RECEPTIONIST` gestiona miembros y reservas; `ADMIN` administra catálogos. Los empleados no se asocian a un miembro.

## Recursos principales

- Catálogos: `/api/sedes`, `/api/salas`, `/api/equipamientos`, `/api/servicios-adicionales`, `/api/planes`
- Miembros y consumo: `/api/miembros`, `/api/miembros/{id}/consumo?mes=yyyy-MM`
- Disponibilidad: `/api/disponibilidad?sedeId=1&fecha=2030-06-03&horaInicio=10:00&horaFin=11:00`
- Reservas: `/api/reservas`, `/api/reservas/{id}` y acciones `/confirmar`, `/cancelar`, `/completar`

Los errores usan siempre `{ "error": "...", "codigo": "...", "timestamp": "..." }`. Códigos frecuentes: `RN-01` (cruce de sala), `RN-02` (duración), `RN-03` (horario/anticipación), `RN-05` (miembro suspendido), `RN-06` (límite de reservas), `RN-08` (cancelación), `RN-09` (transición de estado), `RN-10` (servicios) y `RN-12` (recurso en uso). La política de cancelación es sin cargo con 24 h o más, 50 % del costo de sala entre 2 y 24 h, y no permite cancelar con menos de 2 h.

La creación de reservas serializa los intentos para la misma sala mediante bloqueo pesimista de la fila de sala, y vuelve a comprobar los cruces dentro de la transacción. Los límites de fecha/hora usan el bean `Clock`, que también puede reemplazarse en pruebas.

## Pruebas y entregables

Ejecutá toda la suite con:

```powershell
.\mvnw.cmd test
```

Generá el artefacto ejecutable con:

```powershell
.\mvnw.cmd package
```

- Los escenarios HTTP de reserva, membresía, seguridad y catálogos están en `src/test/java/com/proyecto/spring/CoworkingApiTests.java`.
- Las políticas de cancelación están en `src/test/java/com/proyecto/spring/BookingCancellationRulesTests.java`.
- La colección de los 14 escenarios está en `docs/CoworkHub.postman_collection.json`.
- El contrato OpenAPI exportado está en `docs/openapi.json`.
- El plan y la trazabilidad están en `docs/plan.md` y `docs/checkpoints/`.
