# Informe de validación de ejecución

**Generado:** 2026-11-24  
**Proyecto:** CoworkHub, `C:\Users\usuario\OneDrive\Documentos\spring`

## Resumen

| Etapa | Estado | Código | Evidencia |
|---|---|---:|---|
| Conexión Spring/JPA a MySQL | PASS | 0 | `ApplicationTests`: 1 aprobada con MySQL 8.0.46. |
| Pruebas API con MySQL | PASS | 0 | 12 pruebas de `CoworkingApiTests` ejecutadas con MockMvc y transacciones revertidas. |
| Suite completa H2 | PASS | 0 | `.\mvnw.cmd test`: 20 aprobadas, 0 fallidas, 0 omitidas. |
| Paquete | PASS | 0 | `.\mvnw.cmd package`: BUILD SUCCESS; generó `target/spring-0.0.1-SNAPSHOT.jar`. |
| API HTTP en puerto 8080 | No verificada | — | El intento de arranque por depurador no dejó sesión activa y el puerto 8080 siguió cerrado. |
| Colección Postman completa | No verificada | — | Los 14 requests no se ejecutaron contra un servidor HTTP activo. |
| Concurrencia real en MySQL | No verificada | — | No se enviaron dos requests simultáneos contra el servidor API. |

**Resultado general:** NEEDS_SIGNOFF. La integración Spring/JPA y las pruebas API sobre MySQL pasan, pero faltan la ejecución HTTP de los 14 escenarios y la prueba concurrente real.

## Entorno

- Docker: disponible, versión `29.9.3`.
- Contenedor: `mysql-academia` activo y publicado como `localhost:3308 -> 3306`.
- MySQL: versión `8.0.46`, base `academia`.
- Node.js: disponible, versión `v24.21.0`.
- Tipo de aplicación: API REST sin interfaz HTML; Playwright de navegador no aplica.
- El puerto 8080 no estaba escuchando al terminar las comprobaciones.
- Las pruebas contra MySQL se ejecutaron con `ddl-auto=update`; no se usó `create` ni `create-drop`.

## Evidencia automatizada

### MySQL real

- `ApplicationTests`: 1 prueba aprobada; el log confirma JDBC a `127.0.0.1:3308/academia`, MySQL Connector/J y MySQL 8.0.46.
- `CoworkingApiTests`: 12 pruebas aprobadas, 0 fallidas, 0 omitidas. Se excluyeron las comprobaciones de conteos exactos y de cantidad total de reservas, porque la base ya contiene una reserva adicional respecto del seed.
- Las pruebas API usaron MockMvc y la clase está marcada con `@Transactional`; los requests no se ejecutaron mediante Postman ni por HTTP externo.
- Conteos SQL antes y después: 4 cuentas, 16 reservas y 4 detalles de reserva. No hubo cambios en esas filas.

### Suite y paquete

`.\mvnw.cmd test` finalizó con código 0:

- `ApplicationTests`: 1 aprobada.
- `BookingCancellationRulesTests`: 5 aprobadas.
- `CoworkingApiTests`: 14 aprobadas.
- Total: 20 aprobadas, 0 fallidas, 0 omitidas; H2 en memoria.

`.\mvnw.cmd package` finalizó con código 0 y generó el JAR ejecutable.

## Escenarios pendientes de evidencia HTTP

La colección [CoworkHub.postman_collection.json](./CoworkHub.postman_collection.json) conserva los 14 escenarios. Las pruebas MockMvc validan varios flujos sobre MySQL, pero no equivalen a ejecutar esa colección contra un servidor escuchando en 8080.

Pendiente:

1. Dejar la API activa y comprobar health/OpenAPI por HTTP.
2. Ejecutar y registrar los 14 requests de Postman con fechas vigentes y sin cancelar las reservas semilla existentes.
3. Enviar dos reservas idénticas simultáneamente y comprobar exactamente una respuesta `201` y otra `409 RN-01`.

La implementación contiene bloqueo pesimista de sala (`RoomRepository.findByIdForUpdate`), pero su comportamiento bajo concurrencia real en MySQL todavía no está verificado.
