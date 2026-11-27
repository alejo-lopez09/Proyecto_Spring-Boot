# CoworkHub — guía personal para entender el proyecto

CoworkHub es una aplicación web y API REST para administrar sedes, salas, equipamiento, planes, miembros, servicios y reservas. La interfaz está hecha con HTML, CSS y JavaScript, con textos en español.

Esta guía describe el código que existe hoy, cómo recorrerlo, por qué está dividido en capas y qué reglas ejecuta. Para seguirla, conviene abrir el repositorio en el orden de lectura recomendado.

## 1. Qué hace y cómo se usa

La API permite:

- Consultar y administrar catálogos de sedes, salas, equipamiento, servicios adicionales y planes.
- Registrar miembros, controlar su estado y consultar su consumo mensual.
- Buscar salas disponibles y crear, consultar, confirmar, cancelar o completar reservas.

La interacción normal es:

```text
Navegador (HTML + CSS + JavaScript) / Cliente HTTP
        │ JSON (sin inicio de sesión)
        ▼
Controller → Service → Repository → JPA/Hibernate → MySQL
        ▲                                                   │
        └──────────── respuesta JSON (DTO) ─────────────────┘
```

Los controladores reciben la solicitud HTTP, los servicios aplican reglas y coordinan transacciones, los repositorios consultan la base de datos y las entidades representan el modelo persistente. Las respuestas no exponen directamente entidades JPA: se convierten en DTO.

## 2. Cómo leer el código (orden recomendado)

Seguí este recorrido para entender una funcionalidad de punta a punta:

1. **Arranque:** [`Application.java`](src/main/java/com/proyecto/spring/Application.java) inicia Spring Boot y descubre los componentes bajo `com.proyecto.spring`.
2. **Contrato HTTP:** [`presentation/`](src/main/java/com/proyecto/spring/presentation/) define rutas, métodos HTTP y códigos de respuesta.
3. **Datos de entrada y salida:** [`ApiDtos.java`](src/main/java/com/proyecto/spring/presentation/dto/ApiDtos.java) contiene records JSON y validaciones declarativas (`@NotNull`, `@Positive`, etc.).
4. **Reglas:** [`business/`](src/main/java/com/proyecto/spring/business/) implementa los casos de uso. Para reservas, el punto principal es [`BookingServiceImpl.java`](src/main/java/com/proyecto/spring/business/BookingServiceImpl.java).
5. **Consultas y bloqueos:** [`persistence/`](src/main/java/com/proyecto/spring/persistence/) declara repositorios Spring Data, consultas JPQL y bloqueos pesimistas.
6. **Modelo:** [`domain/`](src/main/java/com/proyecto/spring/domain/) define las entidades, relaciones y enumeraciones persistidas.
7. **Configuración y base:** [`application.properties`](src/main/resources/application.properties) configura la aplicación y la conexión a la base; [`import.sql`](src/main/resources/import.sql) carga datos de demostración al crear el esquema.
8. **Interfaz:** [`static/`](src/main/resources/static/) contiene la aplicación web.
9. **Pruebas:** [`src/test/java/com/proyecto/spring/`](src/test/java/com/proyecto/spring/) verifica reglas y comportamiento HTTP con MockMvc.
10. **Contrato y escenarios manuales:** [`docs/openapi.json`](docs/openapi.json), [`docs/CoworkHub.postman_collection.json`](docs/CoworkHub.postman_collection.json) y documentación adicional en `docs/`.

### Mapa de carpetas

```text
src/main/java/com/proyecto/spring/
├── Application.java
├── config/          Beans compartidos, propiedades y OpenAPI
├── presentation/    Endpoints REST, manejo de errores y DTO
├── business/        Casos de uso, transacciones y reglas del negocio
├── persistence/     Repositorios y consultas JPA
├── domain/          Entidades, estados y enums

src/main/resources/
├── application.properties
└── import.sql

src/test/java/com/proyecto/spring/
├── ApplicationTests.java
├── CoworkingApiTests.java
└── BookingCancellationRulesTests.java
```

## 3. Por qué hay capas y en qué orden se ejecutan

La separación evita que una petición HTTP tenga que conocer directamente los detalles SQL o que las reglas queden dispersas entre endpoints.

### Presentation — entrada y salida HTTP

Los controladores convierten rutas y cuerpos JSON en llamadas a servicios. Por ejemplo, [`BookingController.java`](src/main/java/com/proyecto/spring/presentation/BookingController.java) recibe solicitudes de reserva; no calcula por sí mismo el precio ni decide si el horario se solapa. Las rutas no requieren iniciar sesión.

[`ApiDtos.java`](src/main/java/com/proyecto/spring/presentation/dto/ApiDtos.java) fija el contrato de las solicitudes/respuestas y evita serializar entidades JPA, sus relaciones o datos que no deban formar parte de la API.

### Business — casos de uso y reglas

Los servicios agrupan operaciones que deben ser coherentes y ejecutarse en transacciones. Registrar un miembro guarda sus datos de negocio; crear una reserva valida horario, miembro, sala, cupo, precio y servicios antes de guardar.

`BusinessException` representa errores esperados del negocio con estado HTTP y código funcional. [`ApiExceptionHandler.java`](src/main/java/com/proyecto/spring/presentation/ApiExceptionHandler.java) los traduce a una respuesta JSON uniforme.

### Persistence — acceso a datos

Los repositorios extienden `JpaRepository`, usan consultas derivadas por nombre o JPQL y ocultan cómo se almacenan los objetos. El bloqueo en `RoomRepository.findByIdForUpdate` permite serializar intentos concurrentes de reservar una misma sala.

### Domain — conceptos del sistema

Las entidades describen los datos que persisten y sus relaciones; los enums limitan los estados válidos. El modelo no debería depender de conceptos HTTP como `ResponseEntity`.

### Configuración — infraestructura compartida

[`AppConfig.java`](src/main/java/com/proyecto/spring/config/AppConfig.java) proporciona un `Clock` inyectable. Usarlo permite hacer deterministas las reglas temporales en pruebas. [`OpenApiConfig.java`](src/main/java/com/proyecto/spring/config/OpenApiConfig.java) configura Swagger/OpenAPI.

**Detalle de la implementación actual:** los servicios de negocio importan los DTO definidos dentro de `presentation`. Para este proyecto simplifica el mapeo de entrada/salida, aunque hace que `business` conozca el paquete de presentación; una separación arquitectónica más estricta movería los contratos de aplicación a un paquete neutral.

## 4. Modelo de datos y relaciones

| Entidad | Qué representa | Relaciones principales |
|---|---|---|
| `Location` | Sede, dirección y horario de apertura | Una sede contiene varias salas |
| `Room` | Sala, tipo, capacidad, tarifa y estado activo | Pertenece a una sede; tiene equipamiento y reservas |
| `Equipment` | Elemento disponible en una sala | Relación muchos-a-muchos con salas mediante `room_equipment` |
| `MembershipPlan` | Horas incluidas, descuento por exceso y máximo de reservas activas | Un plan puede pertenecer a varios miembros |
| `Member` | Persona, estado y plan | Puede tener varias reservas |
| `AdditionalService` | Servicio adicional y precio unitario actual | Puede aparecer en varios detalles de reserva |
| `Booking` | Horario, asistentes, estado y totales | Pertenece a una sala y un miembro; contiene detalles |
| `BookingItem` | Servicio, cantidad y precio unitario aplicado | Pertenece a una reserva; no se repite el mismo servicio dentro de ella |

Las relaciones se cargan principalmente en modo `LAZY` para no traer grafos enteros de datos en cada consulta. Las consultas detalladas del repositorio cargan explícitamente lo que se necesita para construir cada DTO. Los detalles de una reserva usan `cascade` y `orphanRemoval`: añadir/quitar un servicio actualiza el detalle desde la reserva.

Los precios de los servicios se copian a `BookingItem.appliedUnitPrice`. Así, si el precio del catálogo cambia después, no se reescribe retroactivamente el costo de una reserva existente. Los importes se manejan con `BigDecimal`, no con `float`/`double`.

### Estados

- Reserva: `PENDING` → `CONFIRMED` → `COMPLETED`; también puede pasar a `CANCELLED` si cumple la política de cancelación.
- Miembro: `ACTIVE` o `SUSPENDED`.
- Tipos de sala: `RoomType` (por ejemplo, sala de reunión, oficina privada o escritorio).

## 5. Recorrido de una reserva

Al recibir `POST /api/reservas`, la aplicación:

1. Valida la forma del JSON con Bean Validation.
2. El servicio carga/bloquea el miembro indicado por `miembroId`.
4. Bloquea la fila de la sala con `PESSIMISTIC_WRITE`.
5. Comprueba que la reserva sea en un solo día, dure entre 30 minutos y 8 horas en intervalos de 30 minutos, empiece al menos una hora en el futuro, respete el horario de la sede, use una sala activa y no exceda su capacidad.
6. Rechaza miembros suspendidos, miembros que superen el máximo de reservas activas del plan o una sala cuyo horario se cruce con una reserva `PENDING`/`CONFIRMED`.
7. Calcula el costo de sala aplicando horas mensuales incluidas y descuento del plan al excedente.
8. Añade servicios con su precio vigente congelado en cada detalle.
9. Guarda y hace `flush` dentro de la transacción, y devuelve el DTO de la reserva.

El criterio de cruce es de extremos abiertos: una reserva que termina exactamente cuando empieza la siguiente **no** se considera solapada. El bloqueo de la sala y la segunda comprobación dentro de la transacción protegen la creación frente a dos solicitudes concurrentes dirigidas a la misma sala.

### Reglas que conviene conocer

- `RN-01`: sala ocupada por cruce de reservas activas.
- `RN-02`: fecha o duración inválida (30 minutos a 8 horas, múltiplos de 30, mismo día).
- `RN-03`: anticipación mínima u horario de sede inválido.
- `RN-04`: sala inactiva o capacidad insuficiente.
- `RN-05`: miembro suspendido.
- `RN-06`: límite de reservas activas del plan.
- `RN-07`: horas incluidas, excedente, descuento y costo.
- `RN-08`: cancelación con menos de dos horas de anticipación no permitida.
- `RN-09`: transición de estado no permitida o reserva que aún no terminó.
- `RN-10`: los servicios de una reserva solo se modifican mientras está `PENDING`.
- `RN-11`: duplicados (por ejemplo, nombres de catálogo, documento, correo o servicio repetido en una reserva).
- `RN-12`: no borrar catálogos que todavía están referenciados/usan otros registros.

Cancelación: con 24 horas o más no tiene cargo; con al menos 2 y menos de 24 horas cobra el 50 % del costo de sala; por debajo de 2 horas se rechaza. Solo se puede cancelar una reserva `PENDING` o `CONFIRMED`.

El consumo mensual se agrupa por el mes de inicio de la reserva y suma reservas `PENDING`, `CONFIRMED` y `COMPLETED`; las canceladas no cuentan. El servicio informa horas incluidas, usadas, restantes y excedentes.

## 6. API y acceso

Las rutas de la aplicación y `/api/**` son públicas: no existe inicio de sesión, token, cuenta de acceso ni rol. Los miembros se conservan como registros de negocio; al crear una reserva se selecciona el miembro correspondiente.

| Funcionalidad | Rutas principales |
|---|---|
| Catálogos | `/api/sedes`, `/api/salas`, `/api/equipamientos`, `/api/servicios-adicionales`, `/api/planes` |
| Miembros y consumo | `/api/miembros` y `/api/miembros/{id}/consumo?mes=yyyy-MM` |
| Disponibilidad | `GET /api/disponibilidad` |
| Reservas | `/api/reservas`, `/api/reservas/{id}` y acciones `/confirmar`, `/cancelar`, `/completar`, `/servicios` |
| Salud | `GET /actuator/health` |

El `GET /api/reservas` exige al menos un filtro; la búsqueda por sala requiere fecha. Los filtros de disponibilidad admiten capacidad mínima y equipamientos requeridos.

### Formato de errores

Las respuestas de error tienen la forma:

```json
{
  "error": "La sala ya está reservada en ese horario",
  "codigo": "RN-01",
  "timestamp": "2030-06-03T13:00:00Z"
}
```

En general: `400` datos inválidos, `404` recurso inexistente y `409` conflicto de negocio. El `ApiExceptionHandler` centraliza las traducciones para evitar formatos distintos por endpoint.

## 7. Configuración y datos de demostración

El build se define en [`pom.xml`](pom.xml): Java 21, Spring Boot 4.1.2, Spring MVC, Spring Data JPA, Bean Validation, Actuator, springdoc/OpenAPI, MySQL y H2 para pruebas. Maven Wrapper (`mvnw.cmd`) permite ejecutar Maven sin instalarlo globalmente.

La aplicación toma estos valores de configuración:

| Variable | Propósito |
|---|---|
| `DB_URL` | URL JDBC; por defecto apunta a MySQL local en el puerto 3308 y base `academia` |
| `DB_USERNAME`, `DB_PASSWORD` | Credenciales de base de datos |
| `DDL_AUTO` | Estrategia de esquema Hibernate; por defecto `update` |

`DB_USERNAME` y `DB_PASSWORD` solo conectan Spring con MySQL; no son cuentas de la aplicación. `DDL_AUTO=create` recrea las tablas y puede borrar datos; `update` conserva y actualiza el esquema, `validate` solo valida compatibilidad.

`import.sql` contiene datos de prueba para sedes, salas, equipamientos, planes, servicios, miembros y reservas. Las fechas de reserva se calculan respecto de la fecha/hora de carga. El archivo se ejecuta cuando Hibernate crea el esquema; no debe asumirse que vuelve a cargar los datos en cada arranque con un esquema ya existente.

En la configuración de prueba, [`src/test/resources/application.properties`](src/test/resources/application.properties), se usa H2 en memoria en modo compatible con MySQL y `create-drop`. Esto aísla los tests y permite que creen/carguen su propia base efímera.

### Arrancar en Windows / PowerShell

1. Inicia MySQL 8 y crea una base accesible para la aplicación. La configuración predeterminada del proyecto espera `localhost:3308/academia`.
2. Si tu base/usuario difieren, establece variables antes del proceso:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3308/academia?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="<usuario>"
$env:DB_PASSWORD="<contraseña>"
$env:DDL_AUTO="update"
```

3. Desde la raíz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

4. Comprueba [health](http://localhost:8080/actuator/health), abre [Swagger UI](http://localhost:8080/doc/swagger-ui.html) o importa la colección Postman.

Para una base nueva que deba recibir el seed, configura `DDL_AUTO=create` solo en ese primer arranque; luego vuelve a `update`. **No hagas esto si la base contiene datos que quieres conservar.**

### Swagger, contrato y Postman

- Swagger UI: `http://localhost:8080/doc/swagger-ui.html`.
- OpenAPI JSON en ejecución: `http://localhost:8080/v3/api-docs`.
- Contrato versionado: [`docs/openapi.json`](docs/openapi.json).
- Escenarios de aceptación manuales: [`docs/CoworkHub.postman_collection.json`](docs/CoworkHub.postman_collection.json).

Swagger UI sirve para explorar y probar endpoints. La interfaz web para administrar reservas está disponible en la raíz de la aplicación.

## 8. Pruebas y estado de validación

Ejecuta la suite:

```powershell
.\mvnw.cmd test
```

Empaqueta el JAR:

```powershell
.\mvnw.cmd package
```

Pruebas disponibles:

- `ApplicationTests`: verifica que el contexto Spring se pueda iniciar.
- `CoworkingApiTests`: pruebas de integración con Spring Boot y MockMvc: acceso público, endpoints, reglas de reserva, costos, filtros, seed y OpenAPI.
- `BookingCancellationRulesTests`: pruebas unitarias de política de cancelación y transiciones, con reloj fijo.

El informe más reciente guardado en [`docs/runtime-validation-report.md`](docs/runtime-validation-report.md) registra que la suite completa pasó (20 pruebas) y que el paquete se generó correctamente. También distingue esas pruebas MockMvc de pruebas HTTP contra un servidor real: en esa validación la aplicación no quedó escuchando en el puerto 8080, la colección Postman completa no se ejecutó y la concurrencia real de dos requests en MySQL quedó pendiente de verificación. El bloqueo está implementado, pero conviene no presentar esos tres puntos como evidencia ya obtenida.

## 9. Interfaz web

El frontend se sirve desde `src/main/resources/static/` y consume la API local. Incluye panel, disponibilidad, reservas y catálogos con una paleta morada y textos en español. No requiere instalar Node ni generar archivos antes de iniciar Spring Boot.

## 10. Guía rápida para investigar una duda

- **¿Qué URL recibe la acción?** Busca el mapping en el controller correspondiente dentro de `presentation/`.
- **¿Qué JSON espera/devuelve?** Busca el record en `ApiDtos.java`.
- **¿Por qué acepta o rechaza una operación?** Sigue la llamada a `business/`; para reservas, empieza en `BookingServiceImpl`.
- **¿Qué consulta ejecuta?** Sigue el repositorio inyectado en el servicio y revisa sus métodos `find...`, `exists...` o `@Query`.
- **¿Dónde se guarda cada dato?** Busca la entidad de `domain/` y sus anotaciones JPA.
- **¿Cómo se accede a las rutas?** La aplicación no tiene autenticación; las rutas están disponibles directamente.
- **¿Por qué el error tiene ese JSON?** Revisa `BusinessException` y `ApiExceptionHandler`.
- **¿Cómo se espera que funcione el proyecto completo?** Compara los tests con `docs/analisis.md`, `docs/plan.md` y la colección Postman.
