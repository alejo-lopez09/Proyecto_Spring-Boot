# Plan de entrega - CoworkHub Spring Boot

## Resumen

Completar y validar el proyecto existente contra el enunciado de CoworkHub. El repositorio ya contiene entidades JPA, servicios, controladores, seguridad JWT, datos semilla, Swagger y una base de pruebas; el trabajo restante debe centrarse en cerrar brechas, verificar las reglas RN-01 a RN-13 y producir la evidencia final de los 14 escenarios.

Estado inicial comprobado: `mvnw test` ejecuta 13 pruebas y todas pasan.

## Contexto técnico

- Java 21 y Spring Boot 4.1.2
- Maven
- MySQL 8 en ejecución; H2 aislada para pruebas
- Spring Data JPA, Hibernate, Bean Validation y OpenAPI
- Spring Security 6/7 con JWT, BCrypt y autorización por rol y dueño
- Arquitectura: presentation -> business -> persistence -> domain
- Hora controlada mediante `Clock`
- Respuestas de error uniformes mediante `ApiExceptionHandler`

## Constitución del trabajo

- Mantener la arquitectura por capas y la separación entre entidades DTO.
- Usar transacciones en operaciones de escritura y bloqueos pesimistas donde se proteja concurrencia.
- Mantener RN-01 a RN-13 como reglas verificables con códigos de error definidos.
- Prohibir reglas de negocio con respuestas 500 o entidades JPA expuestas.
- Preservar los datos semilla y el contrato público documentado.
- Validar cada fase con la menor suite que cubra el cambio y ejecutar `mvnw test` al cerrar.

## Guías aplicadas

- Spring: servicios como interfaz más implementación; controladores REST ligeros; constructor injection.
- JPA: relaciones con lado dueño explícito, colecciones LAZY, transacciones y bloqueo pesimista para reservas.
- REST: 200/201 para éxito, 400 validación, 401 autenticación, 403 autorización, 404 recurso inexistente y 409 regla de negocio.
- Concurrencia: bloquear la sala dentro de la transacción y repetir la comprobación de solapamiento antes de guardar.
- Seguridad: estado stateless, BCrypt, JWT de 3600 segundos y métodos `@PreAuthorize`.
- Pruebas: H2 sin MySQL, `Clock` fijo y pruebas de aceptación HTTP.

## Fases e implementación

### Paso 1: Auditoría y congelación del comportamiento

- [x] T001 [G1] [Plan:1.1] Revisar las entidades y el diagrama JPA contra la sección 2 del enunciado; registrar brechas en `docs/analisis.md`.
- [x] T002 [G1] [Plan:1.2] Revisar repositorios, consultas derivadas y consultas por bloqueo en `src/main/java/com/proyecto/spring/persistence/`.
- [x] T003 [G1] [Plan:1.3] Ejecutar el baseline `mvnw test` y registrar el resultado.
- [x] T004 [Cross-cutting] [Plan:1.4] Congelar la matriz de 20 escenarios REQ-FUNC/RN/endpoint antes de modificar código.

### Paso 2: Modelo JPA y datos semilla

- [x] T005 [G1] [P] [Plan:2.1] Verificar relaciones, fetch, cascade y orphanRemoval en `src/main/java/com/proyecto/spring/domain/`.
- [x] T006 [G1] [Plan:2.2] Completar o corregir los índices y restricciones únicos definidos por RN-11.
- [x] T007 [G1] [Plan:2.3] Verificar que `src/main/resources/import.sql` contiene 2 sedes, 6 salas, 4 equipos, 3 servicios, 3 planes, 5 miembros, 4 cuentas y 15 reservas.
- [x] T008 [G1] [Plan:2.4] Confirmar que las fechas semilla son relativas y que las reservas pasadas/futuras permiten probar estados.
- [x] T009 [G1] [Plan:2.5] Ejecutar el arranque con H2/MySQL y verificar las tablas mediante la consola.

### Paso 3: Catálogos y miembros

- [ ] T010 [G2] [Plan:3.1] Completar CRUD, validaciones y respuestas de catálogos en `CatalogController.java` y `CatalogServiceImpl.java`.
- [ ] T011 [G2] [Plan:3.2] Implementar o verificar filtros por nombre y estado y paginación de miembros.
- [ ] T012 [G2] [Plan:3.3] Completar registro simultáneo Member/UserAccount, suspensión, activación y eliminación RN-12.
- [ ] T013 [G2] [Plan:3.4] Resolver la propiedad mensual de consumo en `MemberServiceImpl.java`.
- [ ] T014 [G2] [Plan:3.5] Añadir pruebas HTTP para estados suspendido, filtros y reglas de acceso.

### Paso 4: Reglas de reservas

- [ ] T015 [G3] [Plan:4.1] Consolidar validaciones RN-02, RN-03 y RN-04 en `BookingServiceImpl.java`.
- [ ] T016 [G3] [Plan:4.2] Probar solapamiento por extremos abiertos y bloqueo pesimista RN-01.
- [ ] T017 [G3] [Plan:4.3] Verificar capacidad, miembro activo, máximo de reservas y costo mensual RN-05 a RN-07.
- [ ] T018 [G3] [Plan:4.4] Verificar cancelación por anticipación y totalización de cargo RN-08.
- [ ] T019 [G3] [Plan:4.5] Verificar transiciones PENDING → CONFIRMED → COMPLETED/CANCELLED RN-09.
- [ ] T020 [G3] [Plan:4.6] Verificar modificación de BookingItem solo en PENDING y preservación del precio aplicado RN-10.
- [ ] T021 [G3] [Plan:4.7] Completar disponibilidad por sede, capacidad y equipamientos RF-03.
- [ ] T022 [G3] [Plan:4.8] Añadir pruebas específicas para reservas simultáneas con dos hilos/transacciones.

### Paso 5: Seguridad, errores y Swagger

- [ ] T023 [G4] [Plan:5.1] Verificar el flujo `/auth/login`, JWT de 3600 segundos y `/auth/perfil`.
- [ ] T024 [G4] [Plan:5.2] Verificar BCrypt, `401 NO_AUTENTICADO` y `403 ACCESO_DENEGADO` en los manejadores.
- [ ] T025 [G4] [Plan:5.3] Revisar ownership en `AccessControl.java` y `@PreAuthorize` de todos los controladores.
- [ ] T026 [G4] [Plan:5.4] Verificar que `ApiExceptionHandler.java` traduce validaciones, 404, 405, duplicados y errores de negocio.
- [ ] T027 [G4] [Plan:5.5] Actualizar OpenAPI, ejemplos, esquemas y botón Authorize.
- [ ] T028 [G4] [Plan:5.6] Exportar y validar `docs/openapi.json`.

### Paso 6: Evidencia de aceptación y entrega

- [ ] T029 [Cross-cutting] [Plan:6.1] Ejecutar los 14 escenarios en H2 o MySQL y registrar respuesta y código.
- [ ] T030 [Cross-cutting] [Plan:6.2] Completar `docs/CoworkHub.postman_collection.json` con usuarios, token y carpeta de escenarios.
- [ ] T031 [Cross-cutting] [Plan:6.3] Actualizar `README.md` con MySQL/Docker, ejecución, usuarios, Swagger y pruebas.
- [ ] T032 [Cross-cutting] [Plan:6.4] Revisar `docs/analisis.md` para trazabilidad y supuestos.
- [ ] T033 [Cross-cutting] [Plan:6.5] Ejecutar `mvnw test` y `mvnw package`, corregir fallas y conservar la salida verde.
- [ ] T034 [Cross-cutting] [Plan:6.6] Preparar evidencia final de commits, diff y archivos entregables.

## Secuencia de commits

Cada fase deberá cerrarse con un commit independiente y verificable:

1. `docs: agregar análisis y trazabilidad inicial`
2. `feat: completar modelo JPA y datos semilla`
3. `feat: completar catálogos y gestión de miembros`
4. `feat: completar reglas de reservas y disponibilidad`
5. `fix: cerrar seguridad, errores y contratos OpenAPI`
6. `test: agregar escenarios de aceptación y colección Postman`
7. `docs: completar README y exportar OpenAPI`

Los commits deben incluir el prefijo histórico del repositorio y excluir archivos generados por Maven salvo que ya formen parte del entregable.

## Requisitos y trazabilidad

| REQ | Descripción | Plan | Evidencia |
|---|---|---|---|
| RF-01 | CRUD completo de catálogos | 3, 5 | `CatalogController.java`, `CatalogServiceImpl.java` |
| RF-02 | Registro y administración de miembros | 3 | `MemberController.java`, `MemberServiceImpl.java` |
| RF-03 | Búsqueda de disponibilidad | 4 | `BookingServiceImpl.availability`, `BookingController.availability` |
| RF-04 | Crear reserva y calcular costo | 4 | `BookingServiceImpl.create`, `calculateRoomCost` |
| RF-05 | Consultar y listar reservas | 4 | `BookingServiceImpl.get/search` |
| RF-06 | Agregar/quitar servicios PENDING | 4 | `addService`, `removeService` |
| RF-07 | Confirmar reserva | 4 | `BookingServiceImpl.confirm` |
| RF-08 | Cancelar reserva | 4 | `BookingServiceImpl.cancel` |
| RF-09 | Completar reserva | 4 | `BookingServiceImpl.complete` |
| RF-10 | Consumo mensual | 3 | `MemberServiceImpl` |
| RF-11 | Login JWT | 5 | `AuthController.login`, `JwtService` |
| RF-12 | JWT y permisos por rol/dueño | 5 | `SecurityConfig`, `AccessControl` |
| RF-13 | Swagger | 5 | `OpenApiConfig`, `docs/openapi.json` |
| RF-14 | Perfil autenticado | 5 | `AuthController.profile` |
| RF-15 | Health público | 5 | Actuator y `application.properties` |
| RN-01 | Anti-solapamiento concurrente | 4 | bloqueo de sala y comprobación dentro de transacción |
| RN-02 | Duración y mismo día | 4 | `validateBookingWindow` |
| RN-03 | Horario, futuro y anticipación | 4 | `validateBookingWindow`, `Clock` |
| RN-04 | Sala activa y capacidad | 4 | `validateBookingWindow` |
| RN-05 | Miembro ACTIVE | 4 | `BookingServiceImpl.create` |
| RN-06 | Máximo de reservas activas | 4 | conteo de reservas activas futuras |
| RN-07 | Costo mensual y servicios | 4 | `calculateRoomCost`, `BookingItem` |
| RN-08 | Política de cancelación | 4 | `BookingServiceImpl.cancel` |
| RN-09 | Transiciones de estado | 4 | confirm/cancel/complete |
| RN-10 | Servicios solo en PENDING | 4 | add/remove service |
| RN-11 | Unicidad | 2, 3 | restricciones JPA/SQL y validación de servicio |
| RN-12 | Recursos en uso | 2, 3 | comprobaciones antes de DELETE |
| RN-13 | Permisos por rol | 5 | `@PreAuthorize` y `AccessControl` |
