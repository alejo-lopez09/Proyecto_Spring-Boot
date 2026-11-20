# Análisis inicial de CoworkHub

## Alcance

El proyecto implementa una API REST para sedes, salas, equipamiento, servicios, planes, miembros, cuentas y reservas. El modelo objetivo se toma del enunciado adjunto y el estado actual se verificó contra las entidades JPA y los repositorios existentes.

## Modelo JPA observado

- `Location` contiene colecciones LAZY de salas.
- `Room` pertenece a una sede, usa `equipment` N-N mediante `room_equipment` y mantiene una colección LAZY de reservas.
- `Equipment`, `MembershipPlan`, `AdditionalService` y sus catálogos tienen entidades JPA independientes.
- `Member` usa relación LAZY con `MembershipPlan`, una relación 1-1 con `UserAccount` y una colección LAZY de reservas.
- `UserAccount` conserva el inverso de la relación 1-1 y define usuario y contraseña únicos/cifrados por el flujo de negocio.
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
2. Verificar todos los endpoints contra el control de rol y ownership.
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
| A9 | MEMBER consulta reserva ajena | 403 |
| A10 | Endpoint protegido sin token o alterado | 401 |
| A11 | Eliminar sala con reservas futuras | 409 RN-12 |
| A12 | Crear sede sin nombre | 400 |
| A13 | Dos reservas concurrentes al mismo horario | Una 201 y otra 409 RN-01 |
| A14 | Listar miembros con `estado=SUSPENDED` | 200 solo suspendidos |

Además se mantienen las trazas RF-01 a RF-15 y RN-01 a RN-13 documentadas en `docs/plan.md`.

## Baseline

Comando: `.\mvnw.cmd test`

Resultado inicial: 13 pruebas ejecutadas, 13 aprobadas, 0 fallidas, 0 omitidas.

## Verificación del lote JPA y seed

- `CoworkingApiTests.importsRequiredSeedEntityCountsAndRelations` confirmó los mínimos de sedes, salas, equipos, planes, servicios, miembros, cuentas, reservas y detalles.
- También confirmó una sala inactiva y un miembro suspendido.
- Las 10 pruebas de `CoworkingApiTests` pasan.
- Docker no está ejecutándose en el entorno actual, por lo que el arranque físico con MySQL no pudo verificarse en este lote; la configuración Maven/H2 sí creó el esquema y importó los datos.
