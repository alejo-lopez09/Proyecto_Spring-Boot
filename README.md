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

Creá la base `academia` y un usuario con permisos sobre ella. Para el entorno Docker configurado en este proyecto, la aplicación se puede conectar a `jdbc:mysql://localhost:3308/academia`.

Se pueden cambiar los valores de conexión mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Esta configuración usa por defecto MySQL en `localhost:3308/academia`. `DDL_AUTO` permite sobreescribir el esquema (por defecto `update`). `create` vuelve a crear las tablas e importa los datos iniciales en cada arranque; usá `update` o `validate` al operar con datos que deban conservarse. Nunca uses credenciales de desarrollo ni el secreto JWT incluido por defecto en producción.

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

## Cómo probar la API con Postman

### 1. Importar la colección

1. Abre Postman.
2. Selecciona **Import**.
3. Selecciona el archivo `docs/CoworkHub.postman_collection.json`.
4. Importa también el archivo como colección nueva.
5. Selecciona la colección **CoworkHub - 14 acceptance scenarios**.                                       

### 2. Configurar la base y ejecutar la API

La colección usa `http://localhost:8080`. Inicia MySQL y luego arranca la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

Configura las variables de conexión si tu MySQL no usa los valores predeterminados:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3308/academia?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="<usuario_mysql>"
$env:DB_PASSWORD="<contraseña_mysql>"
$env:DDL_AUTO="update"
.\mvnw.cmd spring-boot:run
```

Comprueba la aplicación antes de las pruebas:

- Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Swagger: [http://localhost:8080/doc/swagger-ui.html](http://localhost:8080/doc/swagger-ui.html)

Si la base está vacía, detén la aplicación y ejecuta una vez con `DDL_AUTO=create` para cargar `src/main/resources/import.sql`:

```powershell
$env:DDL_AUTO="create"
.\mvnw.cmd spring-boot:run
```

Después vuelve a usar `update` para conservar los datos:

```powershell
$env:DDL_AUTO="update"
.\mvnw.cmd spring-boot:run
```

No uses `create` con datos que deban conservarse, porque recrea las tablas al iniciar.

### 3. Obtener los tokens

La colección contiene variables para `token`, `adminToken` y `receptionToken`.

Ejecuta manualmente estos requests o crea una petición `POST /auth/login`:

| Usuario | Contraseña | Variable |
|---|---|---|
| `ana` | `password` | `token` |
| `admin` | `password` | `adminToken` |
| `reception` | `password` | `receptionToken` |

Ejemplo de cuerpo para iniciar sesión:

```json
{
  "username": "ana",
  "password": "password"
}
```

Respuesta:

```json
{
  "token": "ey...",
  "tipo": "Bearer",
  "expiresInSeconds": 3600
}
```

Copia el valor de `token` sin escribir la palabra `Bearer` en la variable `token` de Postman.

Para obtener los demás tokens:

1. Cambia `username` y `password` en `POST /auth/login`.
2. Copia el valor recibido.
3. Asigna `adminToken` o `receptionToken` en **Colección > Variables**.

También puedes ejecutar con la consola de Postman:

```javascript
pm.collectionVariables.set("token", pm.response.json().token);
```

### 4. Ejecutar los escenarios

La colección incluye los 14 escenarios de aceptación.

1. En Postman, abre la colección.
2. Selecciona **Run collection**.
3. Inicia la aplicación y confirma que `/actuator/health` responde `UP`.
4. Comprueba que `token`, `adminToken` y `receptionToken` tienen valores.
5. Ejecuta la carpeta completa.

Los escenarios principales incluyen:

- Reservar una sala libre con servicios.
- Detectar horarios solapados.
- Reservar en un horario contiguo.
- Rechazar una duración inválida.
- Cancelar con 30 h, 5 h y 1 h de anticipación.
- Confirmar una reserva cancelada.
- Consultar recursos ajenos como `MEMBER`.
- Probar tokens inválidos o ausentes.
- Eliminar una sala con reservas.
- Crear una sede sin nombre.
- Ejecutar dos reservas idénticas simultáneamente.
- Filtrar miembros suspendidos.

La ejecución del escenario 13 puede requerir **Postman Runner** para enviar las dos solicitudes simultáneamente. Una debe responder `201` y la otra `409` con código `RN-01`.

### 5. Valores esperados

| Situación | Resultado esperado |
|---|---|
| Operación exitosa | `200` o `201` |
| Datos inválidos | `400 VALIDATION_ERROR` |
| Sin token | `401 UNAUTHORIZED` |
| Rol o recurso ajeno | `403 FORBIDDEN` |
| Recurso inexistente | `404 NOT_FOUND` |
| Recurso duplicado o regla incumplida | `409` con código `RN-*` |

Todos los errores deben conservar este formato:

```json
{
  "error": "La sala ya está reservada en ese horario",
  "codigo": "RN-01",
  "timestamp": "2030-06-03T13:00:00Z"
}
