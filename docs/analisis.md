# Análisis inicial de CoworkHub

## Alcance

El proyecto implementa una aplicación web y API REST para sedes, salas, equipamiento, servicios, planes, miembros y reservas. La interfaz se sirve desde Spring Boot y las rutas de la aplicación son públicas.

## Modelo JPA observado

- `Location` contiene colecciones LAZY de salas.
- `Room` pertenece a una sede, usa `equipment` N-N mediante `room_equipment` y mantiene una colección LAZY de reservas.
- `Equipment`, `MembershipPlan`, `AdditionalService` y sus catálogos tienen entidades JPA independientes.
- `Member` usa relación LAZY con `MembershipPlan` y una colección LAZY de reservas.
- `Booking` pertenece a una sala y un miembro. `booking_items` pertenece a la reserva mediante `cascade=ALL`, `orphanRemoval=true` y colección LAZY.
- `BookingItem` guarda `appliedUnitPrice`, evitando que cambios posteriores del catálogo alteren el costo existente.
- Las relaciones ManyToOne están marcadas como LAZY. Las consultas detalladas usan `@EntityGraph` o consultas fetch para evitar cargas bajo demanda.

## Reglas y concurrencia

- `BookingRepository.hasOverlappingBooking` usa el criterio abierto `(inicioA < finB && finA > inicioB)`, por lo que extremos compartidos no se solapan.
- La creación bloquea la sala con `PESSIMISTIC_WRITE`, vuelve a comprobar solapamientos dentro de la transacción y guarda con `saveAndFlush`.
- Las reglas temporales usan `Clock`, excepto donde se descubra una llamada directa durante auditorías posteriores.
- Las cancelaciones usan el instante controlado por `Clock`, con sin cargo desde 24 h, 50 % entre 2 y 24 h y rechazo por debajo de 2 h.
- Las transiciones y modificación de servicios están centralizadas en `BookingServiceImpl`.

## Brechas a comprobar en fases posteriores

1. Confirmar que todas las restricciones únicas y eliminaciones RN-11/RN-12 están implementadas y traducidas a 409/400.
2. Verificar que los endpoints públicos sigan disponibles sin sesión.
3. Verificar que todas las respuestas REST usan DTO y que el manejador global cubre validación, 404, 405 y seguridad.
4. Probar concurrencia real y el comportamiento del bloqueo pesimista.
5. Completar paginación de miembros y exportación final de OpenAPI.
6. Ejecutar los escenarios HTTP con datos semilla y registrar evidencias.

## Matriz funcional congelada

| ID | Ruta/acción | Resultado esperado |
|---|---|---|
| A1 | `POST /api/reservas` con sala libre y dos servicios | 201 y costo calculado |
| A2 | Reserva que solapa otra activa | 409 RN-01 |
| A3 | Reserva contigua de 10:00-11:00 después de 09:00-10:00 | 201 |
| A4 | Duración 45 min o fuera del horario | 409 RN-02/RN-03 |
| A5 | Cancelar con 30 h de anticipación | 200, cargo 0 |
| A6 | Cancelar con 5 h de anticipación | 200, cargo 50 % |
| A7 | Cancelar con 1 h de anticipación | 409 RN-08 |
| A8 | Confirmar reserva cancelada | 409 RN-09 |
| A9 | Consultar la reserva de otro miembro | 200 |
| A10 | Consultar un endpoint público sin iniciar sesión | 200 |
| A11 | Eliminar sala con reservas futuras | 409 RN-12 |
| A12 | Crear sede sin nombre | 400 |
| A13 | Dos reservas concurrentes al mismo horario | Una 201 y otra 409 RN-01 |
| A14 | Listar miembros con `estado=SUSPENDED` | 200 solo suspendidos |

Además se mantienen las trazas RF-01 a RF-15 y RN-01 a RN-13 documentadas en `docs/plan.md`.

## Evidencia final de aceptación

| Escenario | Evidencia automatizada | Estado |
|---|---|---|
| 1. Reserva con dos servicios | `CoworkingApiTests.calculatesFrozenServiceCostsAndFiltersAvailability` | Cubierto |
| 2. Solapamiento | `roomBookingRejectsOverlapAndAcceptsAdjacentTime` | Cubierto |
| 3. Reserva contigua | `roomBookingRejectsOverlapAndAcceptsAdjacentTime` | Cubierto |
| 4. Duración/horario inválido | `validatesBookingDurationAsBusinessConflict`, `rejectsInvalidBookingDurationsAndOutsideLocationHours` | Cubierto |
| 5. Cancelación más de 24 h | `cancellationAtOrBeyondTwentyFourHoursIsFree` | Cubierto |
| 6. Cancelación a 5 h | `cancellationWithinTwentyFourHoursChargesHalfTheRoomPrice` | Cubierto |
| 7. Cancelación a 1 h | `cancellationLessThanTwoHoursBeforeStartIsRejected` | Cubierto |
| 8. Confirmar cancelada | `cancelledBookingCannotBeConfirmedOrCompleted`, prueba HTTP `rejectsConfirmOfCancelledBookingAndServiceChangeAfterConfirmation` | Cubierto |
| 9. Consultar reserva de otro miembro | Acceso público a `GET /api/reservas/{id}` | Cubierto |
| 10. Consultar endpoint sin sesión | Acceso público a `GET /api/planes` | Cubierto |
| 11. Eliminar sala con reservas | `filtersSuspendedMembersAndRejectsCatalogDuplicatesAndInUseDeletes` | Cubierto |
| 12. Sede sin nombre | Validación Bean Validation en `LocationRequest.name` y `CoworkingApiTests.mapsMissingResourcesAndInvalidParametersToErrorEnvelope` | Cubierto |
| 13. Solicitudes concurrentes | Bloqueo pesimista `RoomRepository.findByIdForUpdate` y relectura RN-01 | Implementado; las pruebas MySQL no enviaron requests simultáneos |
| 14. Filtrar miembros suspendidos | `filtersSuspendedMembersAndRejectsCatalogDuplicatesAndInUseDeletes` | Cubierto |

## Baseline

Comando: `.\mvnw.cmd test`

Resultado inicial: 13 pruebas ejecutadas, 13 aprobadas, 0 fallidas, 0 omitidas.

## Verificación del lote JPA y seed

- `CoworkingApiTests.importsRequiredSeedEntityCountsAndRelations` confirmó los mínimos de sedes, salas, equipos, planes, servicios, miembros, reservas y detalles.
- También confirmó una sala inactiva y un miembro suspendido.
- Las 10 pruebas de `CoworkingApiTests` pasan.
- Docker no estaba ejecutándose durante este lote inicial, por lo que el arranque físico con MySQL no pudo verificarse entonces; la configuración Maven/H2 sí creó el esquema e importó los datos.

## Revalidación MySQL — 2026-11-24

- Docker 29.9.3 y el contenedor `mysql-academia` están activos; MySQL 8.0.46 publica el puerto 3308 y acepta conexión a `academia`.
- `ApplicationTests` pasó con el contexto Spring/JPA conectado a MySQL, con `ddl-auto=update`.
- Se ejecutaron 12 pruebas API de `CoworkingApiTests` mediante MockMvc sobre MySQL: 12 aprobadas, 0 fallidas, 0 omitidas. Se excluyeron los tests que exigen conteos exactos del seed, ya que la base conserva una reserva adicional.
- Las pruebas API de la clase usan transacciones revertidas.
- La API no quedó escuchando en el puerto 8080. No se ejecutó la colección Postman completa ni la prueba de dos requests simultáneos; el bloqueo MySQL real sigue pendiente de evidencia concurrente.
- Ver [informe de validación de ejecución](./runtime-validation-report.md) para el detalle de comandos y resultados.
