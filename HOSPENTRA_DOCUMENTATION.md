# Hospentra Luxury Sanctuary — Documentación Técnica, Funcional y Casos de Uso del Sistema

Documentación integral y oficial del ecosistema fullstack de **Hospentra**, plataforma hotelera boutique y sistema integral de gestión de propiedades (PMS - Property Management System) con arquitectura desacoplada, control de acceso basado en roles (RBAC) y pasarela de pago dual.

---

## 1. Ficha Técnica y Arquitectura del Sistema

### 1.1 Stack Tecnológico
* **Backend:** Java 25, Spring Boot 3.x, Spring Data JPA / Hibernate, Spring Security 6, Spring Boot Mail (JavaMailSender / SMTP Mailtrap), JWT (HMAC-SHA384), Flyway Migrations, RFC 7807 ProblemDetail.
* **Base de Datos:** PostgreSQL 18 / H2 Database, pool de conexiones HikariCP, transacciones ACID con bloqueo pesimista en reservaciones concurrentes.
* **Frontend:** Angular 22, Arquitectura Standalone Components, Reactividad basada en Señales (Angular Signals: `signal`, `computed`, `effect`), Formularios Reactivos con validación síncrona, SCSS Modular.
* **Diseño y UX:** Paleta de lujo boutique Hospentra (`#0F172A` Deep Slate, `#C59B27` Antique Gold, `#059669` Emerald, `#FFFFFF` Soft White), Google Material Symbols, diseño responsivo adaptado para Desktop, Tablet y Móvil.

### 1.2 Modelo de Roles y Seguridad RBAC
El sistema opera bajo un esquema estricto de cuatro roles corporativos:
1. **`ROLE_ADMIN` (Administrador General):**
   * Control maestro de la plataforma y auditoría global.
   * Gestión y asignación de turnos horarios (`MORNING`, `EVENING`, `NIGHT`, `FLEXIBLE`) para recepcionistas y personal de cocina.
   * Visualización de ingresos totales facturados, monitor de Room Service y métricas financieras.
   * Gestión completa de suites, catálogo y operaciones de Front Desk.
2. **`ROLE_RECEPTIONIST` (Recepcionista de Turno - Front Desk):**
   * Operación de recepción diaria: Check-In, entrega de llaves físicas, cobro en mostrador (POS / Efectivo).
   * Monitoreo de huéspedes en casa (*In-House*) y control de pedidos de Room Service.
   * Control de presencia y refrigerio personal: Alternancia entre *En Mostrador* y *En Break* con cronómetro en tiempo real.
   * Operación de Check-Out con consolidación automática de folio (Estancia base + Consumos de Room Service = Gran total a liquidar).
   * Gestión del Room Rack y ciclo de limpieza (*Housekeeping*).
   * Consulta e impresión de vouchers oficiales de recepción con desglose de consumos de suite.
   * **Restricción:** Acceso bloqueado a la creación de personal (`/api/users/**`) y a métricas financieras confidenciales.
3. **`ROLE_KITCHEN` (Estación de Cocina & Bar KDS):**
   * Terminal digital de comandas KDS (*Kitchen Display System*) en pantalla completa y tiempo real.
   * Recepción de pedidos gastronómicos enviados por huéspedes en estancia activa (`CHECKED_IN`).
   * Transición del flujo culinario: `RECEIVED` (Recibida) -> `PREPARING` (En Preparación) -> `READY_FOR_DELIVERY` (Lista para Despacho) -> `DELIVERED` (Entregada en Suite).
   * Visualización de temporizador de espera por comanda e instrucciones culinarias personalizadas del huésped.
   * **Restricción:** Acceso restringido exclusivamente a la terminal de comandas KDS y catálogo de extras.
4. **`ROLE_CUSTOMER` (Huésped / Cliente):**
   * Exploración del catálogo de suites y consulta de disponibilidad.
   * Reserva en línea con Pasarela Dual (Tarjeta Online vs Pago en Recepción).
   * Módulo "Mis Reservas" con cálculo dinámico de reembolsos y cancelación inteligente.
   * **Carta Boutique de Room Service:** Desbloqueo automático al ingresar en estado `CHECKED_IN` para ordenar gastronomía de autor, vinos finos y bienestar en suite.
   * Consulta y descarga de vouchers oficiales con sellos institucionales.
   * **Restricción:** Acceso bloqueado a los portales administrativos y terminales operativas del PMS.

---

## 2. Módulos y Funcionalidades del Ecosistema

### 2.1 Catálogo Boutique y Motor de Disponibilidad
* Catálogo visual de suites de alta gama con fotografías, capacidad, metraje y desglose de amenidades (King Size, Jacuzzi, Vista Panorámica, etc.).
* Selector interactivo de fechas de entrada (*Check-In*) y salida (*Check-Out*).
* Verificación en tiempo real de disponibilidad sin solapamiento de fechas mediante consultas JPA optimizadas y validación pesimista.

### 2.2 Pasarela de Pago Dual en el Checkout y Cumplimiento Normativo PCI-DSS SAQ A
El proceso de reserva incorpora una pasarela inteligente con dos modalidades de liquidación, diseñada bajo estrictos estándares de seguridad bancaria y tokenización certificada:

#### Modalidad A: Pago Online con Tarjeta Tokenizada (Stripe Elements / PCI-DSS SAQ A)
* **Aislamiento Criptográfico de Datos Sensibles:** Los campos de número de tarjeta, expiración y código CVV son renderizados directamente dentro de un `<iframe>` seguro gestionado de forma exclusiva por la pasarela bancaria oficial (Stripe Elements). El frontend de Hospentra nunca toca, almacena ni transmite números de tarjeta en texto plano, cumpliendo los lineamientos PCI-DSS de menor superficie de ataque (SAQ A).
* **Tokenización y Autorización en Servidor:** Al confirmar la reserva, la pasarela bancaria emite un identificador de método de pago (`pm_...`) o token de bóveda seguro. Este token viaja junto al `PaymentIntent` hacia el backend en `/api/payments/create-intent` y `/api/reservations`.
* **Registro de Auditoría Financiera:** La reserva persiste el `payment_intent_id`, el código de autorización bancaria (`auth_code`), la marca detectada (`VISA`, `MASTERCARD`, `AMEX`) y únicamente los últimos cuatro dígitos (`card_last4`), garantizando trazabilidad total para auditorías y conciliaciones bancarias.
* **Emisión Inmediata de Comprobante:** Voucher oficial sellado como **"PAGADA EN LÍNEA (Comprobante Liquidado)"** con saldo pendiente de `$0.00 USD`.

#### Modalidad B: Pago en Recepción (Front Desk)
* Confirmación inmediata de la suite sin debitar fondos de la tarjeta del huésped.
* Registro de la reserva con estado de pago `PENDING` y método `PAY_AT_CHECKIN`.
* Emisión de comprobante con el sello **"LIQUIDACIÓN AL CHECK-IN (Cobro en Front Desk)"**, indicando el importe exacto a liquidar al recibir las llaves en recepción.

### 2.2.1 Seguridad Avanzada de Sesión: Refresh Tokens y Streaming SSE Cifrado
* **Eliminación Total de Tokens en Query Parameters (SSE Seguro):** A diferencia de las implementaciones habituales de `EventSource` que exponen el JWT en la URL (`?token=...`) susceptible de quedar expuesto en logs de acceso de servidores web, proxies o balanceadores de carga (Nginx, AWS, Cloudflare), Hospentra implementa un cliente de streaming reactivo basado en `fetch()` nativo con `ReadableStream`, inyectando el token estrictamente a través de la cabecera estándar `Authorization: Bearer <token>`.
* **Renovación Transparente de Sesión (Refresh Token Pattern):**
  * Emisión de tokens de acceso de corta duración (24h) y Refresh Tokens criptográficamente firmados con validez de 7 días.
  * Interceptor reactivo Angular (`jwt.interceptor.ts`) que captura automáticamente cualquier código `401 Unauthorized` en peticiones autenticadas, solicita un nuevo par de tokens mediante `POST /api/auth/refresh` y reintenta de forma completamente transparente la petición original sin interrumpir la experiencia del huésped o del personal operativo.
  * Filtro de seguridad backend (`JwtAuthenticationFilter`) que bloquea el uso de Refresh Tokens en endpoints estándar de API y rechaza cualquier token enviado por query parameters.

### 2.3 Portal del Huésped (*Mis Reservas*) y Política Real de Reembolso
* Centro de autogestión para que los huéspedes registrados consulten el historial de sus reservas activas, pasadas y canceladas.
* Copia rápida de códigos de confirmación oficiales (`HOSP-XX-2026`).
* **Algoritmo de Cancelación Inteligente con Regla de 48 Horas:**
  * **Con 48 horas o más de anticipación** respecto a la hora oficial de ingreso (15:00 hrs del día de llegada):
    * Reembolso del 100% del importe liquidado.
    * Mensaje al huésped: *"Reembolso del 100% procesado a su tarjeta •••• XXXX."*
  * **Con menos de 48 horas de anticipación:**
    * Cancelación tardía: Se retiene el importe correspondiente a la primera noche de estancia por política de hotel boutique y se reembolsa automáticamente el saldo restante.
    * Mensaje al huésped: *"Cancelación tardía: Se retiene la primera noche por política de hotel boutique ($XX.XX USD) y se reembolsa el saldo restante ($YY.YY USD)."*
  * **Reservas con Pago en Recepción:**
    * Cancelación libre sin retenciones: *"Cancelación flexible sin cargos: No se efectuó ningún cobro previo."*
* Actualización inmediata del estado de la suite a `AVAILABLE` en caso de cancelación de reservas confirmadas.

### 2.4 Panel Administrativo y Front Desk PMS (`/admin/dashboard`)
Panel centralizado de control para personal con credenciales institucionales:

#### A. Navegación y Submenús Basados en Rol
* **Para Administradores:** Barra de navegación superior con pestañas:
  1. *Operaciones Front Desk & Room Rack*
  2. *Gestión de Personal & Recepcionistas*
* **Para Recepcionistas:** Vista enfocada exclusivamente a operaciones diarias de Front Desk, con badge identificador esmeralda *"Recepcionista Front Desk"*.

#### B. Indicadores Operativos en Tiempo Real (KPIs)
* **Llegadas de Hoy (Check-In):** Conteo de huéspedes que ingresan hoy a partir de las 15:00 hrs.
* **Huéspedes En Casa:** Total de estancias en curso con suites activas (`CHECKED_IN`).
* **Salidas de Hoy (Check-Out):** Conteo de huéspedes con salida programada al mediodía (12:00 hrs).
* **Ocupación & Habitaciones:** Medidor visual de suites Libres, Ocupadas y En Limpieza.
* **Ingresos en Libros:** Indicador financiero visible exclusivamente para el Administrador General.

#### C. Room Rack & Módulo de Limpieza & Gobernanta (Housekeeping - Migración V15)
* Centro de control operativo de suites con filtro multidimensional por nivel/piso (*Piso 1 - Jardín*, *Piso 2 - Océano*, *Piso 3 - Panorámico*, *Piso 4 - Penthouses*) y filtro rápido *"🧹 Requieren Limpieza"*.
* Buscador en vivo por número de habitación, categoría de suite, camarista asignada o notas.
* Tira de KPIs de estado en tiempo real: Total de suites, Disponibles, En Limpieza, En Casa y Alerta de Prioridad Alta.
* **Tarjeta Enriquecida de Suite:**
  * Indicador de piso y número de habitación con código de color operativo.
  * Insignia de estado: `AVAILABLE` (Lista / Entrega), `MAINTENANCE` (En Limpieza), `OCCUPIED` (En Casa).
  * Distintivo de **Prioridad Alta (Check-In Hoy)** con estrella distintiva para suites con llegadas inminentes.
  * Pastilla de **Camarista Asignada** con nombre del personal responsable o alerta *"Camarista Sin Asignar"*.
  * Snippet de **Notas de Inspección & Reporte de Incidencias** (resalta en rojo si existe reporte de daño o mantenimiento menor).
  * Registro de **Última Higienización** con fecha y hora formal (*ej. 14:30 hrs*).
* **Modal Boutique de Housekeeping & Inspección:**
  * Selector de Camarista Responsable asignada de la nómina hotelera.
  * Selector de Prioridad Operativa (`NORMAL`, `HIGH` - Llegada Hoy, `LOW` - Mantenimiento Preventivo).
  * **Checklist Rápido de Entrega (1 Clic):** Verificación interactiva de *Minibar auditado y reabastecido*, *Lencería egipcia y toallas renovadas*, *Jacuzzi & terraza higienizados*, *Amenities botánicos completos*, *Aromaterapia ambiental lista*.
  * **Conmutador de Reporte de Incidencia:** Permite a la camarista reportar si algo no está conforme (ej. filtración en jacuzzi, lámpara dañada), alertando de inmediato a Front Desk en rojo e impidiendo que se entregue la suite por error.
  * Área de observaciones libres de gobernanta.
* **Botón Rápido de 1 Clic ("✓ Marcar Lista"):** Devuelve instantáneamente la suite a estado `AVAILABLE`, actualiza la fecha de higienización y emite evento SSE `ROOM_STATUS_CHANGED` en tiempo real para sincronizar todas las terminales sin recargar la página.

#### D. Flujo Operativo de Llegadas (Check-In)
* Modal institucional de Check-In con validación de pago:
  * Si la reserva fue liquidada online con tarjeta: Muestra notificación de pago verificado y saldo `$0.00 USD`, procediendo a la entrega directa de llaves.
  * Si la reserva es con pago en recepción: Exige el cobro del total adeudado en mostrador, permitiendo seleccionar el canal de cobro (**Tarjeta POS / Datáfono** o **Efectivo en Caja**) antes de habilitar la entrega de llaves.
* Al confirmar el Check-In:
  * Estado de la reserva cambia a `CHECKED_IN`.
  * Habitación asociada pasa automáticamente a `OCCUPIED`.

#### E. Flujo Operativo de Salidas (Check-Out) y Consolidación de Folio
* Modal institucional de Check-Out con liquidación unificada de estancia y consumos extras:
  * **Estancia Base:** Importe pactado por las noches de hospedaje.
  * **Folio de Room Service:** Relación pormenorizada de comandas (`CMD-XX`) solicitadas desde la suite durante la estancia (gastronomía, bar y spa) con desglose de ítems y subtotal.
  * **Total Folio Consolidado:** Suma matemática exacta de estancia base + consumos de habitación.
  * **Saldo Pendiente de Liquidación al Salir:** Si la estancia fue saldada en línea con tarjeta, únicamente se exige el cobro del saldo de Room Service. Si la reserva fue con pago en recepción, se cobra el consolidado total.
* Al presionar *"Recibir Llaves y Completar Check-Out"*:
  * Estado de la reserva cambia a `COMPLETED`.
  * La suite pasa de forma automática a estado `MAINTENANCE` (En Limpieza).
  * Se genera una notificación en tiempo real para el equipo de Housekeeping.

#### F. Directorio de Personal, Turnos Horarios y Control de Pausas
* Pestaña exclusiva para el Administrador General que audita todas las cuentas corporativas registradas.
* Métricas de colaboradores en tiempo real: Total de cuentas corporativas, recepcionistas de turno, personal de cocina KDS y administradores.
* Tabla con avatares según rol, nombres de usuario, correos institucionales, rol corporativo, turno asignado (`MORNING`, `EVENING`, `NIGHT`, `FLEXIBLE`), estado operativo (`En Mostrador / Activo` vs `En Refrigerio / Break`) y fecha de alta.
* **Modal de Alta de Personal Corporativo & Turno:**
  * Campos: Nombre de usuario, correo electrónico, contraseña con alternador de visibilidad, selector de Rol (`ROLE_RECEPTIONIST`, `ROLE_KITCHEN`, `ROLE_ADMIN`) y selector de Turno Horario.
  * Encriptación segura con algoritmo **BCrypt** y persistencia atómica en base de datos.
* **Control de Guardia y Pausa / Break para Recepcionistas:**
  * Botón interactivo en el encabezado de recepción: Alterna entre *"🟢 En Mostrador • Tomar Break"* y *"☕ En Break (cronómetro mm:ss) • Reanudar"*.
  * Al iniciar el refrigerio, el sistema registra el timestamp de inicio y activa un cronómetro progresivo para control de turnos y cumplimiento laboral.

#### G. Monitor de Comandas de Room Service y Concierge en Front Desk
* Panel dedicado en el Control Center para supervisar en vivo los pedidos realizados por huéspedes en casa.
* Tarjetas KPI de comandas: Total de comandas registradas, comandas activas en preparación/despacho y comandas entregadas a suite.
* Tabla unificada con encabezado **Estado Operativo**, código `CMD-XX`, suite destino, nombre del huésped, artículos solicitados con cantidades y precios, instrucciones especiales y distinción clara por departamento mediante tags visuales:
  * `💆‍♀️ Spa`: Solicitudes de bienestar y relajación gestionadas directamente por el Concierge de recepción.
  * `🍳 Cocina`: Comandas gastronómicas y de bar procesadas por la estación de cocina.
* Botón de acceso directo para abrir la Terminal KDS de Cocina en pestaña independiente.
* Botón de acceso directo al **Directorio de Terapeutas Spa**.

#### H. Voucher Oficial de Recepción y Folio de Consumos
* Visor de comprobante corporativo en alta resolución con monograma de Hospentra y sellos digitales oficiales:
  * Sello verde esmeralda: **"PAGADA EN LÍNEA"** con código de autorización bancaria.
  * Sello dorado ámbar: **"LIQUIDACIÓN AL CHECK-IN"** con instrucción de cobro en Front Desk.
* Desglose de consumos extras de habitación cuando existen comandas de Room Service asociadas a la estancia.
* Botón de impresión directa con estilos CSS `@media print` optimizados para impresora de mostrador o exportación PDF.

### 2.5 Terminal Digital de Cocina KDS (`/kitchen/orders`)
Terminal de pantalla completa diseñada específicamente para la brigada de cocina y bar:
* **Autenticación Directa de Estación:** Inicio de sesión con credenciales de cocina (`ROLE_KITCHEN` o personal asignado) redirigiendo automáticamente a `/kitchen/orders`.
* **Aislamiento Operativo Inteligente:** El backend filtra las comandas activas de modo que la cocina **únicamente recibe pedidos gastronómicos y de bar**. Las solicitudes de masajes y tratamientos de spa quedan excluidas del KDS, evitando confusiones y saturación innecesaria en los fogones.
* **Tablero Kanban Digital de Tres Etapas:**
  1. **Comandas Recibidas (`RECEIVED`):** Pedidos entrantes de suites con sonido visual, código `CMD-XX`, número de suite e instrucciones de preparación. Botón de acción: *"Iniciar Elaboración"*.
  2. **En Preparación (`PREPARING`):** Comandas actualmente en cocina con temporizador transcurrido en vivo (minutos y segundos) y alertas visuales según tiempo de espera. Botón de acción: *"Marcar Lista para Despacho"*.
  3. **Listas para Entrega (`READY_FOR_DELIVERY`):** Pedidos listos en pase para que el room runner o camarero suba a la suite. Botón de acción: *"Confirmar Entrega en Suite"*.
* **Historial de Comandas Entregadas:** Pestaña de auditoría con registro de hora exacta de despacho y entrega.
* **Sincronización Automática:** Polling y reactividad para actualizar la pantalla sin necesidad de recargar la página.

### 2.6 Carta Boutique de Room Service y Bienestar (`/my-reservations`)
Módulo de hospitalidad digital para huéspedes en casa:
* **Desbloqueo Inteligente:** La carta boutique permanece oculta hasta que el recepcionista formaliza el Check-In de la reserva (`CHECKED_IN`).
* **Catálogo de Lujo en Tres Categorías:**
  * **Gastronomía de Autor:** Solomillo al Malbec, Tabla Ibérica de Bellota, Desayuno Imperial a la Suite.
  * **Bar & Vinos Finos:** Moët & Chandon Brut Impérial, Gran Reserva Privada, Cócteles de Autor.
  * **Bienestar & Spa:** Masaje relajante en suite, Baño de inmersión con aromaterapia botánica.
* **Selector de Preferencias de Spa:** Al seleccionar servicios de relajación, el huésped puede elegir:
  * **Ubicación del Servicio:** *En mi Suite* o *En Cabina de Spa del Hotel*.
  * **Horario Preferido:** Selección de franjas horarias (10:00, 11:30, 14:00, 16:00, 18:00, 20:00 hrs).
* **Carrito de Pedidos Interactivo:** Selección de ítems, selector de cantidades, cálculo instantáneo de subtotal y caja de texto para instrucciones especiales.
* **Carga al Folio de la Suite:** El pedido se carga al folio consolidado de la suite sin requerir pago inmediato con tarjeta, liquidándose en el Check-Out.
* **Línea de Tiempo del Pedido en Suite:** El huésped visualiza el avance en vivo: *Recibido en Concierge/Cocina*, *Terapeuta Asignado / En Fogón*, *En Camino* y *Completado*, visualizando el nombre del terapeuta oficial asignado cuando corresponde.

### 2.7 Módulo de Concierge de Spa & Directorio Dinámico de Terapeutas
Herramienta de Front Desk para la gestión flexible de especialistas de bienestar:
* **Lógica Operativa Boutique:** En hoteles de lujo de 10 a 30 suites, el recepcionista actúa como Concierge y coordina directamente con los terapeutas. Se descarta un rol informático separado (`ROLE_SPA`) para evitar desatención de pedidos cuando los terapeutas están en cabina.
* **Directorio de Especialistas:** Modal interactivo para visualizar los terapeutas registrados, sus especialidades (*ej. Masajes Holísticos, Tejido Profundo*) y su enlace de contacto directo telefónico o por WhatsApp (`tel:`).
* **Disponibilidad Flexible (En Guardia / Descanso):** Conmutador ágil que permite marcar a un terapeuta como *Activo (En Guardia)* o *Inactivo (Descanso)*, garantizando que el personal en día libre no figure en las listas de asignación.
* **Altas y Bajas Dinámicas:** Permite registrar nuevos especialistas en el directorio o eliminarlos cuando ya no presten servicios para el hotel.
* **Modal de Asignación de Comandas:** Al recibir una solicitud de spa, Front Desk selecciona al terapeuta disponible, lo que avanza la comanda a `PREPARING` con el nombre del profesional y notifica de inmediato al huésped en su portal.

### 2.8 Módulo de Pasarela de Pagos Stripe & Trazabilidad Bancaria
Infraestructura de cobros en línea de estándar bancario PCI-DSS:
* **Integración con Stripe Elements:** Flujo seguro mediante `PaymentIntent` donde los datos confidenciales de tarjeta se tokenizan directamente contra los servidores de Stripe sin transitar por el backend hotelero.
* **Trazabilidad en Base de Datos (Migración V11):** La reserva persiste el identificador `payment_intent_id` y el código de autorización bancaria oficial `authorization_code` (ej. `AUTH-XXXXXX`).
* **Sincronización Dual:** Soporte para transacciones con liquidación inmediata (`CREDIT_CARD` $\rightarrow$ `PAID`) y reservas con garantía de pago al check-in (`AT_RECEPTION` $\rightarrow$ `PENDING`).
* **Voucher Digital con Sello de Transacción:** El comprobante oficial incluye el código de autorización generado, fecha/hora exacta del cobro y los últimos 4 dígitos de la tarjeta.

### 2.9 Motor de Reseñas Verificadas & Reputación Estilo Play Store
Módulo de satisfacción y retroalimentación de huéspedes con blindaje antifraude:
* **Certificación Estricta de Estadía Verificada (Verified Stay):** El sistema exige matemáticamente que la reserva asociada tenga estatus `COMPLETED`. No se admiten opiniones de usuarios sin reserva, con reservas pendientes o canceladas.
* **Inmutabilidad y Unicidad:** Cada reserva puede ser calificada una única vez (`UNIQUE(reservation_id)` en base de datos), previniendo ataques de distorsión o duplicación de opiniones.
* **Distribución de Estrellas Estilo Play Store:** Resumen analítico visual con desglose de porcentajes para 5★, 4★, 3★, 2★ y 1★, promedio ponderado global y métricas dimensionales (Limpieza, Personal, Ubicación, Confort y Relación Calidad-Precio).
* **Filtro Interactivo:** Permite a los futuros huéspedes explorar opiniones seleccionando una barra de estrellas específica o visualizando la totalidad.
* **Canal Oficial de Respuesta de Gerencia:** La administración del hotel puede responder formalmente a cualquier reseña con una réplica corporativa registrada (`managementResponse`).

### 2.10 Vista Detallada de Suite & Conector de Fechas Four Seasons / Ritz-Carlton
Experiencia inmersiva de exploración de habitaciones y reserva fluida:
* **Ruta Dedicada `/rooms/:id` (`RoomDetailComponent`):** Vista completa de la suite que incluye galería fotográfica interactiva, especificaciones de capacidad, superficie en m², amenidades exclusivas y caja de reserva flotante fija (*sticky sidebar*).
* **Conector de Fechas de Lujo (Arquitectura en 3 Columnas):** Rediseño visual sin superposiciones de texto. La tarjeta de Check-In y Check-Out se unen orgánicamente mediante una línea conectora dorada que sostiene la píldora central de noches (`🌙 X Noches`), eliminando colisiones de posicionamiento absoluto.
* **Humanización de Identidad de Usuario:** Eliminación de nomenclaturas técnicas internas (`ROLE_CUSTOMER`), presentando al cliente su identificación elegante como *Huésped Registrado*.

### 2.11 Panel de Moderación y Réplica de Reseñas en PMS Admin
Centro de mando corporativo para la gestión de la reputación hotelera:
* **Tablero de Control de Opiniones (`ReviewModerationComponent`):** Pestaña dedicada en la intranet PMS que consolida todas las reseñas verificadas del hotel con insignias de habitación, fecha y desglose de satisfacción.
* **4 Tarjetas KPI en Vivo:** Promedio global del resort, índice de satisfacción (% de opiniones 4★ y 5★), contador de opiniones pendientes de atención y tasa de respuesta oficial.
* **Editor Interactivo de Réplica Oficial:** Permite a la Gerencia General redactar respuestas corporativas que se publican instantáneamente bajo la opinión en el detalle de la suite.
* **Filtros Avanzados:** Filtrado ágil por calificación (5★ a 1★), estado de respuesta (Sin Respuesta vs. Respondidas) y motor de búsqueda en tiempo real por huésped, número de habitación o palabra clave.

### 2.12 Folio Fiscal Digital & Facturación Oficial en PDF
Documento fiscal y comprobante oficial de liquidación de estancia:
* **Modal Reutilizable de Alta Resolución (`HotelFolioModalComponent`):** Disponible con un solo clic tanto para el huésped en su portal como para el recepcionista en el tablero de operaciones y Room Rack.
* **Desglose Itemizado de Cargos:** Línea base de hospedaje (noches x tarifa unitaria) y agregación dinámica de todas las comandas de Room Service y tratamientos de Spa cargados a la habitación.
* **Transparencia Fiscal:** Cálculo formal de subtotal neto, IVA e impuesto al hospedaje (16%) y Gran Total consolidado.
* **Sello de Trazabilidad Bancaria:** Código de autorización oficial (`AUTH-XXXXXX`), referencia de transacción encriptada Stripe PCI-DSS o cobro en mostrador, y código de barras digital.
* **Exportación y Descarga Nativa:** Hoja de estilo `@media print` optimizada para generar directamente un PDF impecable en formato A4/Carta sin elementos superfluos de pantalla.

### 2.13 Business Intelligence & Métricas Ejecutivas Hoteleras
Módulo de analítica estratégica para la toma de decisiones gerenciales:
* **KPIs Maestros de la Industria Hotelera:**
  * **ADR (Average Daily Rate):** Tarifa promedio diaria por noche vendida.
  * **RevPAR (Revenue per Available Room):** Indicador clave de rendimiento económico por suite disponible.
  * **Tasa de Ocupación Global (%):** Porcentaje dinámico de ocupación sobre la capacidad instalada del resort.
  * **Facturación Bruta Consolidada:** Total generado desglosado entre alojamiento y consumos extras.
* **Gráfico de Tendencia Mensual:** Visualización en barras con desglose apilado de ingresos por hospedaje y Room Service para análisis estacional.
* **Desglose de Unidades de Negocio:** Participación porcentual de Gastronomía de Autor (KDS), Cava & Vinos Finos y Bienestar & Spa.
* **Ranking de Rendimiento por Suite:** Tabla ejecutiva de habitaciones ordenadas por facturación total, noches vendidas, ADR individual y tasa de ocupación.

### 2.14 Servicio Transaccional de Correo & Verificación OTP en Dos Pasos (Migración V13)
Infraestructura de comunicaciones y validación de identidad para huéspedes:
* **Verificación de Correo en Registro (Flujo OTP de 6 Dígitos):**
  * Al registrarse, el usuario se crea con `enabled = false` y `email_verified = false`.
  * El sistema genera criptográficamente un código aleatorio de 6 dígitos con ventana de expiración estricta de 15 minutos persistido en la tabla `verification_codes`.
  * La interfaz transiciona de inmediato a la pantalla interactiva de verificación con 6 casillas independientes, foco automático secuencial, soporte para pegado directo (`Ctrl+V`) y protección contra reenvío masivo (cooldown de 60 segundos).
  * Validación idempotente y protección contra condiciones de carrera (Race Conditions) para garantizar activación fluida sin parpadeos de error.
  * Al validar el código, la cuenta se habilita (`enabled = true`, `email_verified = true`) y el backend retorna automáticamente los tokens de sesión JWT y Refresh Token.
* **Plantillas de Correo HTML Boutique:**
  * Despacho asíncrono con `@Async` y `JavaMailSender` encapsulado en `EmailService`.
  * Diseño visual con paleta `#0F172A`, detalles dorados `#C59B27`, tipografía institucional y caja de código OTP destacada.
  * Compatibilidad con servidores SMTP de producción y bandejas de prueba para desarrollo (Mailtrap Sandbox).
  * Mecanismo de contingencia por consola: registro en logs del código OTP en entornos locales sin conectividad SMTP externa.
* **Notificaciones Transaccionales de Reserva:**
  * Al confirmarse una reserva exitosa, el sistema despacha automáticamente al correo del huésped el voucher digital con código de confirmación (`HOSP-XX-2026`), desglose de fechas, suite reservada, desglose económico y sello oficial de liquidación.

### 2.15 Almacenamiento Multimedia de Suites & Gestor de Galería Fotográfica (Migración V14)
Infraestructura desacoplada de medios y gestión fotográfica de autor para habitaciones:
* **Patrón de Almacenamiento Desacoplado (`MediaStorageService`):**
  * Interfaz de almacenamiento abstracta implementada mediante `LocalMediaStorageServiceImpl` para persistencia física local en disco (`uploads/rooms/`), preparada para conmutación transparente hacia Object Storage en la nube (AWS S3 / Cloudflare R2 / MinIO) sin impacto en controladores ni entidades relacionales.
  * Validación estricta de formato (`image/jpeg`, `image/png`, `image/webp`, `image/avif`), límite de tamaño de 10 MB, protección contra Path Traversal y generación criptográfica de identificadores seguros UUID.
  * Exposición de recursos estáticos en Spring Web (`WebMvcConfig`) mapeando `/uploads/**` a la ruta física local con permisos públicos en `SecurityConfig`.
* **Modelo Relacional de Galería Fotográfica (Migración `V14`):**
  * Tabla `room_images` vinculada por clave foránea a `room_types` con soporte de cascada (`ON DELETE CASCADE`), índice relacional e índice compuesto sobre `(room_type_id, is_cover)`.
  * Columna `cover_image_url` en `room_types` para consulta directa e instantánea en el catálogo público y el motor de disponibilidad.
* **Flujo Operativo de Portada vs. Fotos Referenciales:**
  * **Foto de Portada (`is_cover = true`):** Representa la imagen de impacto en el catálogo principal, miniaturas de reservas y encabezado de la suite.
  * **Fotos Referenciales (`is_cover = false`):** Alimentan la galería interactiva de ambientes interiores (baño, balcón, vistas, cama King).
  * **Intercambio Dinámico de Portada:** Endpoint `PATCH /api/room-images/{imageId}/set-cover` para promover cualquier foto referencial a portada principal en un solo clic, desmarcando automáticamente la portada anterior mediante actualización atómica en base de datos.
  * **Eliminación Segura y Reasignación Automática:** Al eliminar la foto de portada actual (`DELETE /api/room-images/{imageId}`), el sistema borra el archivo físico de disco y promueve automáticamente la siguiente foto disponible de la galería como nueva portada, preservando la continuidad visual.
* **Componente Interactivo PMS (`SuiteGalleryModalComponent`):**
  * Modal boutique integrado en el inventario de suites con zona Drag & Drop para carga de archivos, previsualización instantánea en memoria, casilla de verificación para marcar portada de inmediato, miniatura hero de la portada activa y cuadrícula de fotos referenciales con acciones rápidas.
* **Carga Directa de Foto de Portada en Alta de Suite (`SuiteInventoryComponent`):**
  * Integración de selector y zona Drag & Drop para foto de portada directamente en el formulario modal *"Nueva Categoría de Suite"*.
  * Al guardar una nueva categoría con archivo de portada seleccionado, el sistema encadena de forma atómica y transparente la subida del archivo mediante `uploadRoomImage(created.id, coverFile, true)`, asociándola como portada principal en un solo paso operativo.
* **Galería Multimedia y Visor Lightbox en Vista de Huésped (`/rooms/:id` / `RoomDetailComponent`):**
  * Conexión dinámica que consulta `/api/room-types/{id}/images` para presentar las fotos reales subidas por el hotel, priorizando la foto de portada y mostrando las fotografías referenciales en carrusel y cuadrícula.
  * Controles de navegación interactiva sobre la imagen principal (botones flotantes previo/siguiente y badge de posición).
  * **Visor Lightbox de Pantalla Completa:** Modal inmersivo de alta definición con fondo desenfocado (`backdrop-filter: blur(14px)`), navegación con teclas de flecha (`ArrowLeft`, `ArrowRight`), cierre con tecla `Escape`, tira interactiva de miniaturas y contador de fotografía activa estilo resort de ultra lujo.

### 2.16 Gestión de Colaboradores PMS, Turnos y Gobernanza de Acceso (Migración y Control Administrativo)
Gestión completa del personal operativo desde el panel del Administrador General (`/admin/dashboard`):
* **Control Integral de Usuarios Corporativos (`/api/users/**`):**
  * Creación y edición de cuentas de colaboradores con asignación de roles (`ROLE_RECEPTIONIST`, `ROLE_KITCHEN`, `ROLE_ADMIN`) y turnos horarios (`MORNING`, `EVENING`, `NIGHT`, `FLEXIBLE`).
  * Encriptación criptográfica con BCrypt y actualización segura de perfil con cambio opcional de contraseña.
  * Conmutación de estado activo/inactivo (`PATCH /api/users/{id}/toggle-status`).
* **Blindajes de Seguridad & Anti-Bloqueo de Dirección:**
  * **Protección del Último Administrador:** Se impide la desactivación, eliminación o revocación del rol de administrador al último usuario activo con privilegios de `ROLE_ADMIN` en el sistema, evitando el bloqueo permanente e irrevocable de la plataforma PMS.
  * **Impedimento de Auto-Desactivación:** Un administrador no puede desactivar su propia cuenta en sesión activa.
  * **Segregación de Roles:** Las cuentas de colaboradores corporativos no pueden degradarse al rol cliente (`ROLE_CUSTOMER`) a través de la interfaz administrativa sin pasar por el flujo de registro de huéspedes.

### 2.17 Módulo de Autogestión "Mi Perfil & Seguridad" y Protocolo de Gracia de 30 Días (Migración V16 - RGPD / Derecho al Olvido)
Centro unificado de configuración personal y gobernanza de privacidad accesible para todos los roles autenticados (`/account/settings`):
* **Pestaña 1: Perfil Personal:**
  * Consulta y actualización de datos personales de identidad (Nombre, Apellidos, Teléfono).
  * Sincronización bidireccional automática con la ficha de huésped (`Guest`) vinculada en base de datos.
* **Pestaña 2: Seguridad & Credenciales (Cambio de Contraseña Blindado por OTP):**
  * Flujo en dos pasos con código de seguridad OTP de 6 dígitos despachado vía `EmailService` al correo verificado del usuario.
  * Temporizador regresivo de enfriamiento (*cooldown*) de 60 segundos para evitar sobrecarga de peticiones SMTP.
  * Verificación criptográfica del código de un solo uso en backend (`verification_codes`), validación de coincidencia de contraseñas y actualización atómica en base de datos con BCrypt.
* **Pestaña 3: Privacidad & Eliminación de Cuenta con Protocolo de Gracia de 30 Días:**
  * **Periodo de Gracia de 30 Días (Soft Delete Regulado):** Al solicitar la eliminación de cuenta, la fecha programada de eliminación se establece en `ahora + 30 días` (`scheduled_deletion_date`) y `pendingDeletion = true`.
  * **Confirmación de Seguridad:** Requiere ingresar la contraseña actual del usuario y motivo opcional de baja.
  * **Protección Operativa de Front Desk:** Si el usuario mantiene reservaciones activas o en curso (`PENDING`, `CONFIRMED`, `CHECKED_IN`), el sistema bloquea inmediatamente la solicitud con código `HTTP 400 Bad Request` / `IllegalStateException`, informando al usuario que debe culminar o cancelar sus estancias antes de solicitar la eliminación, protegiendo las operaciones del hotel.
  * **Protección del Último Administrador:** Si el usuario es el último administrador activo, la solicitud de eliminación es rechazada de inmediato.
  * **Banner de Gracia y Cancelación en 1 Clic:** Durante la ventana de 30 días, el usuario puede seguir iniciando sesión normalmente o cancelar el protocolo en cualquier momento mediante `POST /api/account/cancel-deletion`, restaurando su estatus activo instantáneamente.
  * **Purga y Anonimización Automatizada a los 30 Días (`AccountPurgeScheduler`):**
    * Cron diario programado a las 03:00 AM (`0 0 3 * * ?`) con Spring Task Scheduling (`@EnableScheduling`).
    * Localiza todas las cuentas cuya fecha programada de eliminación haya expirado (`scheduledDeletionDate <= now()`).
    * **Preservación de Trazabilidad Fiscal y Contable:** Gracias a la regla relacional `users.guest_id ON DELETE SET NULL`, el registro `Guest` se conserva disociado y es anonimizado formalmente (`firstName = "Huésped Anonimizado"`, `lastName = "#G-ID"`, `phone = null`), preservando el histórico de facturación, ingresos, impuestos y auditoría sin vulnerar normativas de retención fiscal.
    * El registro `User` y sus códigos de verificación asociados se eliminan definitivamente de la base de datos sin violar restricciones foráneas.

### 2.18 Registro Inmutable de Auditoría (Audit Trail) & Exportación Contable en CSV/Excel (Migración V17)
Infraestructura de control de cumplimiento corporativo y trazabilidad operativa para el Administrador General (`/admin/dashboard` $\rightarrow$ pestaña *"Auditoría & Libros"*):
* **Bitácora Inmutable de Actividad (`audit_logs`):**
  * Registro atómico de acciones críticas de seguridad y Front Desk: `CHECK_IN`, `CHECK_OUT`, `CANCEL_RESERVATION`, `CONFIRM_RESERVATION`, `CREATE_STAFF`, `UPDATE_STAFF`, `TOGGLE_USER_STATUS`, `CHANGE_PASSWORD`, `REQUEST_ACCOUNT_DELETION`, `CANCEL_ACCOUNT_DELETION` y `ASSIGN_THERAPIST`.
  * Captura de metadatos forenses: Operador, rol corporativo en sesión, entidad y folio afectado (`entityName`, `entityId`), dirección IP del cliente (con soporte para cabecera `X-Forwarded-For`), resultado (`SUCCESS` o `FAILURE`), detalle descriptivo en lenguaje natural y marca de tiempo exacta.
  * Aislamiento de fallos: El registro de auditoría se ejecuta de forma desacoplada y protegida, garantizando que un fallo eventual en el almacenamiento de la bitácora jamás interrumpa la transacción de negocio principal.
* **4 KPIs de Supervisión en Tiempo Real:**
  * **Eventos Históricos:** Conteo acumulado de acciones auditadas.
  * **Actividad de Hoy:** Registro de transacciones ejecutadas en la jornada actual.
  * **Incidentes o Fallos:** Conteo de intentos bloqueados o accesos fallidos.
  * **Tipos de Acciones:** Catálogo dinámico de eventos monitorizados.
* **Filtros Dinámicos Multidimensionales:**
  * Selector por tipo de acción operativa con glifos semánticos.
  * Buscador de texto reactivo por nombre de usuario del operador.
  * Rango de fechas con selector formal (*Desde* y *Hasta*).
  * Paginación controlada con vista previa y acordeón expandible de detalles del evento.
* **Motor Oficial de Exportación Contable (Excel & Hojas de Cálculo):**
  * **Libro Oficial de Reservaciones (`hospentra_reservas_YYYY-MM-DD.csv`):** Exportación detallada de todas las estancias con código de confirmación, nombre del huésped, email, teléfono, número de suite, categoría, fechas de Check-In/Check-Out, cómputo de noches, canal de pago, estatus de liquidación, tarifa por noche, total neto y estado operativo.
  * **Diario Financiero & Folios (`hospentra_diario_financiero_YYYY-MM-DD.csv`):** Consolidación contable de ingresos con desglose cronológico de Alojamiento y Servicios Extras (Room Service culinario y terapias de Spa vinculadas a habitación), indicando fecha, folio, huésped/suite, concepto, canal de cobro y monto en USD.
  * **Compatibilidad Universal con Microsoft Excel:** Archivos exportados con codificación **UTF-8 con BOM** (`\uFEFF`), garantizando apertura instantánea en Excel, Google Sheets y LibreOffice Calc con acentos, caracteres en español y separación de columnas nativa sin distorsión tipográfica.

---

## 3. Catálogo de Casos de Uso (Use Cases)

A continuación se detallan los casos de uso fundamentales que abarcan el 100% de los flujos del sistema:

---

### Caso de Uso CU-01: Registro e Inicio de Sesión de Huésped
* **Actor Principal:** Huésped / Cliente (`ROLE_CUSTOMER`).
* **Precondición:** El usuario se encuentra en el portal público de Hospentra.
* **Flujo Principal de Registro con Verificación OTP:**
  1. El cliente hace clic en "Registrarse" en la barra de navegación o modal de bienvenida.
  2. El sistema despliega el formulario solicitando: Nombre de usuario, Correo electrónico, Nombre, Apellido, Teléfono y Contraseña.
  3. El cliente ingresa los datos y presiona "Crear Cuenta".
  4. El sistema valida el formato de correo y que el usuario/correo no existan en base de datos.
  5. El backend registra la cuenta con rol `ROLE_CUSTOMER`, encripta la contraseña con BCrypt, asigna `enabled = false`, `email_verified = false` y persiste el perfil asociado en la entidad `Guest`.
  6. El sistema genera un código OTP de 6 dígitos con vigencia de 15 minutos, lo persiste en `verification_codes` y lo despacha al correo vía `EmailService`.
  7. El frontend transiciona de inmediato a la pantalla de verificación OTP (`step = 'OTP'`) solicitando los 6 dígitos.
  8. El cliente ingresa el código de 6 dígitos; el sistema valida contra `/api/auth/verify-email`, habilita la cuenta (`enabled = true`, `email_verified = true`) y emite los tokens JWT y Refresh Token.
  9. El cliente queda autenticado y redirigido al catálogo principal.
* **Flujo Principal de Inicio de Sesión:**
  1. El cliente ingresa su usuario y contraseña en el modal de inicio de sesión.
  2. El sistema valida credenciales contra `/api/auth/login`.
  3. El sistema almacena el token JWT y actualiza el estado reactivo del usuario en el Navbar.
* **Flujos Alternativos:**
  * **Cuenta no verificada:** Si el usuario intenta iniciar sesión antes de ingresar su código OTP, Spring Security lanza `DisabledException` y el sistema muestra un mensaje indicando que debe verificar su correo antes de acceder.
  * **Credenciales inválidas:** El sistema muestra alerta roja indicando error en credenciales sin interrumpir la navegación.

---

### Caso de Uso CU-02: Búsqueda de Suites y Reserva con Pasarela Dual
* **Actor Principal:** Huésped / Cliente (`ROLE_CUSTOMER`).
* **Precondición:** El cliente ha iniciado sesión o se autentica durante el proceso de reserva.
* **Flujo Principal:**
  1. El cliente explora el catálogo de habitaciones y selecciona las fechas de Check-In y Check-Out.
  2. El sistema verifica disponibilidad en base de datos y calcula el número de noches y el importe total.
  3. El cliente hace clic en "Reservar Suite" y es dirigido a la pantalla de Checkout (`/reservations/checkout`).
  4. El cliente selecciona la modalidad de pago deseada:
     * **Opción A (Tarjeta Online):**
       * El cliente introduce número de tarjeta, titular, expiración y CVV.
       * El sistema valida la máscara, detecta la marca de tarjeta (Visa, Mastercard, Amex) y simula autorización bancaria SSL.
       * La reserva se crea con `paymentMethod = CREDIT_CARD`, `paymentStatus = PAID` y se emite el voucher con sello "PAGADA EN LÍNEA".
     * **Opción B (Pago en Recepción):**
       * El cliente selecciona "Pagar al llegar a recepción".
       * El sistema confirma la suite con `paymentMethod = AT_RECEPTION`, `paymentStatus = PENDING`.
       * Se emite el voucher con sello "LIQUIDACIÓN AL CHECK-IN".
  5. La suite queda bloqueada en el sistema para evitar reservas duplicadas en las fechas seleccionadas.
  6. El cliente visualiza su código de confirmación oficial (`HOSP-XX-2026`).

---

### Caso de Uso CU-03: Consulta y Cancelación con Política Real de Reembolso
* **Actor Principal:** Huésped / Cliente (`ROLE_CUSTOMER`).
* **Precondición:** El cliente cuenta con al menos una reserva registrada en su historial.
* **Flujo Principal:**
  1. El cliente accede a "Mis Reservas" (`/reservations/my-reservations`).
  2. El sistema lista las reservas asociadas a su cuenta ordenadas cronológicamente.
  3. El cliente localiza la reserva y hace clic en "Cancelar Reserva".
  4. El sistema evalúa la fecha y hora actual contra las 15:00 hrs del día de Check-In de la reserva:
     * **Caso A (≥ 48 horas restantes):**
       * El sistema notifica: Reembolso completo del 100% procesado a la tarjeta.
       * Estado de la reserva cambia a `CANCELLED`.
     * **Caso B (< 48 horas restantes):**
       * El sistema calcula la penalización correspondiente a la tarifa de la primera noche.
       * El sistema notifica: Retención de la primera noche y reembolso del saldo remanente a la tarjeta.
       * Estado de la reserva cambia a `CANCELLED`.
     * **Caso C (Reserva con Pago en Recepción):**
       * El sistema libera la reserva sin ningún cargo ni débito bancario.
  5. La suite asociada vuelve a estar disponible para el público general.

---

### Caso de Uso CU-04: Autenticación Corporativa y Control de Acceso RBAC
* **Actores:** Administrador General (`ROLE_ADMIN`) o Recepcionista de Turno (`ROLE_RECEPTIONIST`).
* **Precondición:** Acceso al portal administrativo institucional (`/admin/login`).
* **Flujo Principal:**
  1. El colaborador introduce su nombre de usuario corporativo y contraseña de seguridad.
  2. El sistema valida las credenciales en `/api/auth/login`.
  3. El sistema comprueba el rol de la cuenta:
     * Si el rol es `ROLE_ADMIN`: Redirige a `/admin/dashboard` habilitando control total, métricas financieras y gestión de personal.
     * Si el rol es `ROLE_RECEPTIONIST`: Redirige a `/admin/dashboard` en modo Front Desk, ocultando gestión de personal y datos financieros confidenciales.
* **Flujo Alternativo (Intento de acceso de cliente):**
  * Si un usuario con rol `ROLE_CUSTOMER` intenta ingresar por `/admin/login`, el sistema bloquea el acceso con el mensaje: *"Acceso denegado: Esta cuenta de huésped no tiene permisos para el panel de gestión"*.

---

### Caso de Uso CU-05: Administración RBAC, Edición y Soft-Delete de Personal Corporativo
* **Actor Principal:** Administrador General (`ROLE_ADMIN`).
* **Precondición:** Sesión iniciada con rol de administrador en `/admin/dashboard`.
* **Flujos del Sistema:**
  * **Creación de Cuentas Corporativas (`POST /api/users`):**
    1. El administrador ingresa a "Directorio de Personal & Turnos de Guardia" y presiona "Registrar Nuevo Personal".
    2. Registra usuario, correo institucional, contraseña (mínimo 6 caracteres), turno (`MORNING`, `EVENING`, `NIGHT`, `FLEXIBLE`) y rol (`ROLE_RECEPTIONIST`, `ROLE_KITCHEN`, `ROLE_ADMIN`).
    3. El backend encripta la contraseña con BCrypt y persiste la cuenta habilitada.
  * **Edición de Perfil de Colaborador (`PUT /api/users/{id}`):**
    1. El administrador presiona "Editar" en cualquier fila del personal.
    2. Se abre el diálogo "Modificar Personal Corporativo" con el identificador inmutable por auditoría.
    3. Permite actualizar correo institucional, reasignar rol de estación, cambiar turno de guardia o restablecer opcionalmente la contraseña de seguridad.
    4. **Regla de Auto-Protección:** Si el administrador edita su propio perfil, el selector de rol queda restringido a `ROLE_ADMIN` impidiendo auto-revocación accidental.
  * **Desactivación / Reactivación Segura (Soft Delete - `PATCH /api/users/{id}/toggle-status`):**
    1. El administrador hace clic en "Desactivar" o "Reactivar".
    2. El backend conmuta el flag booleano `enabled` del usuario sin eliminar físicamente registros históricos o de auditoría.
    3. **Reglas de Blindaje:**
       * El administrador tiene bloqueada la desactivación de su propia cuenta en sesión activa.
       * El sistema impide desactivar al último administrador activo registrado para garantizar la continuidad operativa.
       * Spring Security rechaza de forma inmediata con `DisabledException` cualquier intento de autenticación de cuentas con `enabled = false`.

---

### Caso de Uso CU-06: Gestión de Llegadas y Registro de Check-In
* **Actores:** Recepcionista de Turno (`ROLE_RECEPTIONIST`) o Administrador (`ROLE_ADMIN`).
* **Precondición:** El huésped se presenta físicamente en la recepción del hotel en su fecha de llegada.
* **Flujo Principal:**
  1. El personal localiza la reserva en el filtro "Llegadas Hoy" o mediante el buscador por código o nombre del huésped.
  2. El personal hace clic en "Hacer Check-In".
  3. El sistema abre el modal de Check-In detallando el titular, suite asignada y situación de cobro:
     * Si fue **Pagada en Línea:** Se indica saldo adeudado de `$0.00 USD` y se entregan las llaves de la suite directamente.
     * Si fue **Pago en Recepción:** El sistema exige realizar el cobro total en ese instante, permitiendo registrar si el pago se efectúa mediante *Tarjeta POS* o *Efectivo en Caja*.
  4. El personal presiona "Entregar Llaves y Registrar Check-In".
  5. La reserva cambia a estado `CHECKED_IN` y la suite pasa inmediatamente a estado `OCCUPIED`.
  6. El huésped pasa al estado *En Casa*.

---

### Caso de Uso CU-07: Gestión de Salidas (Check-Out) y Consolidación de Folio
* **Actores:** Recepcionista de Turno (`ROLE_RECEPTIONIST`) o Administrador (`ROLE_ADMIN`).
* **Precondición:** El huésped acude a recepción a devolver las llaves y desocupar la habitación.
* **Flujo Principal:**
  1. El personal ubica al huésped en la lista o en la pestaña "En Casa" / "Salidas Hoy".
  2. El personal presiona "Hacer Check-Out".
  3. El sistema carga de forma reactiva las comandas de Room Service asociadas a la reserva.
  4. El modal de Check-Out presenta el desglose consolidado:
     * Estancia Base: Importe de las noches de hospedaje.
     * Consumos de Habitación: Lista de comandas `CMD-XX` con sus artículos y subtotal de extras.
     * Total Folio Consolidado: Estancia + Extras.
     * Saldo a Liquidar: Si pagó online con tarjeta, únicamente se cobra el saldo de Room Service; si fue pago en recepción, se cobra el total consolidado.
  5. El recepcionista recibe las llaves y confirma el cobro.
  6. El personal hace clic en "Recibir Llaves y Completar Check-Out".
  7. La reserva pasa a estado `COMPLETED`.
  8. La habitación asociada cambia de forma automática de `OCCUPIED` a `MAINTENANCE` (En Limpieza).
  9. El personal de piso/housekeeping recibe la alerta visual en el Room Rack.

---

### Caso de Uso CU-08: Gestión de Room Rack y Ciclo de Housekeeping
* **Actores:** Recepcionista de Turno (`ROLE_RECEPTIONIST`) o Administrador (`ROLE_ADMIN`).
* **Precondición:** La suite ha sido desocupada o se encuentra en proceso de mantenimiento.
* **Flujo Principal:**
  1. El colaborador visualiza la sección "Room Rack & Estado de Limpieza" en el panel.
  2. Las habitaciones en estado `MAINTENANCE` exhiben la insignia *"En Limpieza"* y el botón *"Marcar Limpia y Lista"*.
  3. Una vez que el personal de limpieza concluye el reacondicionamiento y desinfección de la suite, se presiona *"Marcar Limpia y Lista"*.
  4. El sistema ejecuta una petición `PATCH /api/rooms/{id}/status` con nuevo estado `AVAILABLE`.
  5. La suite queda certificada como limpia, cambiando su borde a verde y quedando inmediatamente disponible para nuevas asignaciones o ventas directas.

---

### Caso de Uso CU-09: Emisión, Consulta e Impresión de Vouchers Oficiales
* **Actores:** Huésped (`ROLE_CUSTOMER`), Recepcionista (`ROLE_RECEPTIONIST`) o Administrador (`ROLE_ADMIN`).
* **Precondición:** Reserva existente en base de datos.
* **Flujo Principal:**
  1. El usuario hace clic en el botón "Voucher" en la reserva correspondiente.
  2. El sistema despliega el comprobante oficial con:
     * Encabezado institucional de Hospentra Sanctuary.
     * Sello de certificación: **"PAGADA EN LÍNEA"** (con código de autorización bancaria) o **"LIQUIDACIÓN AL CHECK-IN"**.
     * Código oficial de confirmación (`HOSP-XX-2026`).
     * Datos del huésped, suite asignada, fechas de check-in / check-out, noches y desglose de precio.
     * Desglose de folio de consumos extras si la habitación solicitó Room Service.
  3. El usuario puede hacer clic en "Imprimir / Guardar PDF" para generar una copia física para el huésped o archivado administrativo.

---

### Caso de Uso CU-10: Asignación de Turnos y Gestión de Refrigerio / Break de Recepción
* **Actores:** Administrador General (`ROLE_ADMIN`) y Recepcionista (`ROLE_RECEPTIONIST`).
* **Precondición:** Recepcionista autenticado en el sistema.
* **Flujo Principal:**
  1. El Administrador asigna el turno correspondiente (`MORNING`, `EVENING`, `NIGHT`, `FLEXIBLE`) al crear o actualizar la cuenta.
  2. El recepcionista visualiza su estado de presencia en el encabezado (*"🟢 En Mostrador • Tomar Break"*).
  3. Al salir a su pausa reglamentaria (comida / refrigerio), el recepcionista hace clic en el botón.
  4. El sistema actualiza el estado a `ON_BREAK`, registra la hora de inicio en base de datos e inicia el cronómetro dinámico (*"☕ En Break (mm:ss) • Reanudar"*).
  5. El Administrador visualiza en el Directorio de Personal qué colaboradores están activos y quiénes están en break con indicador ámbar pulsante.
  6. Al retornar al puesto de atención, el recepcionista presiona *"Reanudar"* y el estado se restablece a `ON_DUTY`.

---

### Caso de Uso CU-11: Solicitud de Room Service desde Suite por Huésped en Casa
* **Actores:** Huésped con estancia activa (`ROLE_CUSTOMER`).
* **Precondición:** La reserva se encuentra en estado `CHECKED_IN` ("En Casa").
* **Flujo Principal:**
  1. El huésped ingresa a la sección *"Mis Reservas"*.
  2. El sistema detecta la estancia activa y desbloquea el módulo *"Carta Boutique de Room Service & Gastronomía"*.
  3. El huésped explora las categorías (*Gastronomía*, *Bar & Vinos*, *Bienestar & Spa*).
  4. Agrega los platos o amenidades deseadas al carrito de pedidos, ajustando las cantidades.
  5. Especifica instrucciones opcionales para la cocina (punto de carne, sin gluten, etc.).
  6. Presiona *"Confirmar Pedido a la Suite"*.
  7. El sistema crea la orden con estado inicial `RECEIVED`, calcula los importes y la carga automáticamente al folio de la habitación sin requerir tarjeta inmediata.
  8. La comanda se transmite instantáneamente al KDS de cocina y al monitor de recepción.

---

### Caso de Uso CU-12: Despacho y Operación en Terminal de Cocina KDS
* **Actores:** Personal de Cocina & Bar (`ROLE_KITCHEN`).
* **Precondición:** Usuario autenticado en `/admin/login` con credenciales de cocina.
* **Flujo Principal:**
  1. El personal de cocina ingresa y el sistema lo redirige de inmediato a la terminal `/kitchen/orders`.
  2. Las comandas entrantes aparecen en la columna *"Comandas Recibidas"*.
  3. El cocinero revisa los ítems, cantidades e instrucciones especiales, y presiona *"Iniciar Elaboración"*.
  4. La orden pasa a *"En Preparación"* y se activa el contador de tiempo de cocción.
  5. Cuando los platillos están listos en pase para entrega, el cocinero presiona *"Marcar Lista para Despacho"*.
  6. La orden se traslada a *"Listas para Entrega"*. El room runner o camarero de pisos recoge la bandeja y sube a la suite.
  7. Al entregar el pedido al huésped, se presiona *"Confirmar Entrega en Suite"*, registrando la hora de entrega y pasando la orden a `DELIVERED`.

---

### Caso de Uso CU-13: Solicitud Personalizada de Servicios de Bienestar & Spa
* **Actores:** Huésped con estancia activa (`ROLE_CUSTOMER`).
* **Precondición:** Reserva en estado `CHECKED_IN` ("En Casa").
* **Flujo Principal:**
  1. El huésped abre el catálogo boutique de Room Service en *"Mis Reservas"*.
  2. Al agregar servicios de la categoría Bienestar & Spa (*ej. Masaje Relajante en Suite, Baño de Sales*), se activa el selector de preferencias de spa:
     * **Ubicación:** Selección entre *"En mi Suite"* (atención privada) o *"En Cabina de Spa del Hotel"*.
     * **Horario de Cita:** Selección de franjas horarias disponibles (*10:00, 11:30, 14:00, 16:00, 18:00, 20:00 hrs*).
  3. El huésped envía la comanda, la cual se registra con estado `RECEIVED` ("Solicitud Recibida en Concierge").
  4. En su panel de seguimiento en vivo, el huésped visualiza la comanda con insignia morada de Spa, la ubicación elegida, la hora reservada y el estado en tiempo real.
  5. Una vez que Front Desk le asigna un especialista, la tarjeta se actualiza mostrando: *"Terapeuta Oficial Asignado: [Nombre del Terapeuta]"*.

---

### Caso de Uso CU-14: Gestión del Directorio de Especialistas & Terapeutas de Spa
* **Actores:** Recepcionista (`ROLE_RECEPTIONIST`) o Administrador (`ROLE_ADMIN`).
* **Precondición:** Colaborador autenticado en el sistema.
* **Flujo Principal:**
  1. El personal ingresa a la sección de Room Service en el panel PMS y presiona *"Directorio Terapeutas Spa"*.
  2. Se despliega el modal con la nómina de especialistas certificados del hotel:
     * Nombre completo y avatar.
     * Especialidad (*ej. Aromaterapia, Masajes Descontracturantes, Rituales de Inmersión*).
     * Enlace de llamada / WhatsApp directo (`tel:`).
     * Conmutador interactivo de estado de disponibilidad.
  3. **Control de Disponibilidad Inmediata:** Si un terapeuta se encuentra en descanso o vacaciones, se hace clic sobre su estado para alternar entre *"Activo (En Guardia)"* e *"Inactivo (Descanso)"*. Los terapeutas inactivos se excluyen automáticamente de los selectores de asignación.
  4. **Registro de Nuevo Especialista:** El recepcionista presiona *"Agregar Terapeuta"*, completa el formulario (Nombre, Especialidad y Teléfono) y guarda el registro de forma instantánea.
  5. **Baja de Terapeuta:** Si un profesional ya no colabora con el resort, se presiona el botón de eliminar para darlo de baja del directorio.

---

### Caso de Uso CU-15: Asignación y Despacho Operativo de Tratamientos de Spa
* **Actores:** Recepcionista de Turno / Concierge (`ROLE_RECEPTIONIST`) o Administrador (`ROLE_ADMIN`).
* **Precondición:** Existe una comanda de Spa en estado `RECEIVED` en la tabla de Room Service.
* **Flujo Principal:**
  1. En la fila de la comanda de Spa, el Concierge visualiza el botón *"Asignar Terapeuta"*.
  2. Al presionarlo, se abre el modal de asignación con el resumen de la solicitud (artículos, si es en suite o en cabina, y horario preferido del huésped).
  3. El Concierge selecciona de la lista al terapeuta activo en guardia que cubrirá el servicio.
  4. Al presionar *"Confirmar y Asignar Terapeuta"*, el sistema ejecuta `PATCH /api/room-orders/{id}/assign-therapist`.
  5. La orden transiciona a `PREPARING` con la etiqueta distintiva *"Terapeuta Asignado: [Nombre]"*.
  6. Cuando el terapeuta se traslada a la suite o cabina, el Concierge presiona *"En Camino"* (`READY_FOR_DELIVERY`).
  7. Al concluir la sesión de masaje, se presiona *"Completar"* (`DELIVERED`), dejando la comanda formalmente liquidada y registrada para auditoría.

---

### Caso de Uso CU-16: Cronograma Gantt y Visualización Continua con Tape Chart de Suites
* **Actores:** Recepcionista de Turno (`ROLE_RECEPTIONIST`) o Administrador General (`ROLE_ADMIN`).
* **Precondición:** Colaborador autenticado en el sistema con acceso a Operaciones de Front Desk.
* **Flujo Principal:**
  1. En el panel de Operaciones, el recepcionista selecciona la pestaña *"Tape Chart (Gantt)"*.
  2. El sistema despliega una matriz temporal continua de 14 días con navegación interactiva (`[ < 7 Días ]`, `[ Hoy ]`, `[ 7 Días > ]`):
     * **Filas:** Todas las suites del hotel ordenadas numéricamente, con tipo de habitación, tarifa por noche y estado físico de housekeeping.
     * **Columnas:** Días del calendario con nombres abreviados, fecha numérica, sombreado de fines de semana y resaltado distintivo del día actual (*HOY*).
     * **Bloques de Estancia:** Barras visuales continuas posicionadas en las fechas de check-in y check-out, con código de reserva, nombre del huésped y color distintivo de estado (En Casa, Confirmada, Pendiente, Completada).
     * **Fila de Ocupación Diaria:** Porcentaje consolidado de ocupación por día con barra de progreso para anticipar picos de afluencia y disponibilidad para reservas directas (walk-ins).
  3. Al hacer clic sobre cualquier bloque de reservación, se abre una tarjeta flotante de acciones rápidas para ver la ficha completa, imprimir el voucher oficial o ejecutar el Check-In / Check-Out de manera inmediata.

---

### Caso de Uso CU-17: Sistema KDS de Cocina con Notificaciones Sonoras Resonantes
* **Actores:** Personal de Cocina y Bar (`ROLE_KITCHEN`, `ROLE_ADMIN`).
* **Precondición:** Usuario autenticado en el terminal `/kitchen/orders`.
* **Flujo Principal:**
  1. La brigada culinaria mantiene activa la terminal KDS en la pantalla de la estación de cocina.
  2. El sistema cuenta con un selector de audio en cabecera (*Alerta Sonora Activa / Audio Silenciado*), con memoria persistente en el navegador (`localStorage`).
  3. Cuando un huésped envía una nueva orden de gastronomía o cava, el sondeo automático en tiempo real detecta la nueva comanda entrante.
  4. La terminal reproduce de forma instantánea una campana armónica dual (*chime* en frecuencias 587.33 Hz y 880 Hz mediante la Web Audio API nativa), acompañada de una animación oscilante del icono de campana y un mensaje flotante de notificación.
  5. El personal atiende el pedido de inmediato sin necesidad de monitorear visualmente la pantalla de manera ininterrumpida.

---

### Caso de Uso CU-18: Transmisión en Tiempo Real con Server-Sent Events (SSE)
* **Actores:** Personal de Cocina (`ROLE_KITCHEN`), Recepcionistas (`ROLE_RECEPTIONIST`), Administrador (`ROLE_ADMIN`).
* **Precondición:** Colaborador con sesión activa en el PMS o KDS.
* **Flujo Principal:**
  1. Al iniciar sesión, la aplicación cliente establece una conexión unidireccional persistente mediante `new EventSource('/api/notifications/stream?token=...')`.
  2. El servidor responde con evento `HANDSHAKE` y mantiene vivo el canal mediante latidos periódicos `PING` cada 20 segundos.
  3. **Eventos Transmitidos:**
     * `ORDER_CREATED`: Cuando un huésped emite una comanda, la cocina y recepción reciben la notificación al milisegundo (0 ms de desfase), disparando el timbre y actualizando el tablero KDS sin esperar el intervalo de sondeo.
     * `ORDER_STATUS_CHANGED`: Cuando cocina inicia o completa un pedido, o se asigna un terapeuta, el cambio se refleja en tiempo real en todas las pantallas simultáneas.
     * `ROOM_STATUS_CHANGED`: Cuando housekeeping marca una habitación como limpia (`AVAILABLE`) o un recepcionista realiza check-in (`OCCUPIED`) o check-out (`MAINTENANCE`), el Room Rack y el Tape Chart Gantt se actualizan instantáneamente sin recargar la página.
     * `RESERVATION_STATUS_CHANGED`: Nuevas reservas, confirmaciones o cancelaciones actualizan de inmediato los folios y el cronograma de ocupación.
  4. Si ocurre un micro-corte de red, el servicio cliente ejecuta reconexión automática transparente con reintentos exponenciales.

---

### Caso de Uso CU-19: Calificación de Estadía Verificada & Prevención Antifraude
* **Actores:** Huésped (`ROLE_CUSTOMER`), Administrador General (`ROLE_ADMIN`).
* **Precondición:** El huésped cuenta con una reserva en estado `COMPLETED` (check-out formalizado) y la reserva no ha sido calificada previamente.
* **Flujo Principal:**
  1. El cliente accede a su portal de estancias en `/reservations/my-reservations`.
  2. Localiza la estancia culminada y presiona el botón dorado *"★ Calificar mi Estadía"*.
  3. Se despliega el modal interactivo de calificación:
     * Selector interactivo de 1 a 5 estrellas con texto descriptor (*Excelente, Muy bueno, Regular, etc.*).
     * Caja de comentarios para relatar su experiencia personalizada.
     * Evaluaciones dimensionales complementarias (Limpieza, Confort, Servicio, Ubicación).
  4. El huésped envía la reseña, invocando `POST /api/reviews`.
  5. El backend ejecuta la matriz de validaciones antifraude:
     * Verifica que la reserva exista y que su estado sea estrictamente `COMPLETED` (bloqueando reservas `PENDING`, `CONFIRMED`, `CHECKED_IN` o `CANCELLED`).
     * Comprueba que el usuario autenticado sea el titular legítimo de la reserva (`guest_id` coincidente).
     * Asegura la ausencia de reseñas previas para el `reservation_id`.
  6. La reseña queda persistida con la marca `verifiedStay = true`.
  7. El frontend actualiza de inmediato la tarjeta de la reserva mostrando la insignia verde *"✓ Reseña Registrada"*.
* **Flujo Alternativo (Respuesta Oficial de Gerencia):**
  * El Administrador General ingresa al panel de control, localiza la opinión y emite una réplica formal mediante `POST /api/reviews/{id}/management-response`, visible públicamente bajo el comentario del cliente.

---

### Caso de Uso CU-20: Pago Seguro con Stripe Payment Intents & Trazabilidad
* **Actores:** Huésped (`ROLE_CUSTOMER`).
* **Precondición:** El cliente ha seleccionado fechas de estancia y se encuentra en `/reservations/checkout`.
* **Flujo Principal:**
  1. El cliente selecciona la modalidad *"Tarjeta en Línea (Stripe Direct)"*.
  2. El frontend solicita un token de intención de pago mediante `POST /api/payments/create-intent` enviando el importe exacto de la estancia.
  3. Stripe genera un `clientSecret` y un `paymentIntentId`.
  4. El cliente ingresa su información financiera en el elemento encriptado de Stripe.
  5. Al confirmar, Stripe procesa la transacción de forma directa con el banco emisor bajo protocolo 3D-Secure.
  6. Una vez aprobada, el backend confirma la reserva almacenando el `paymentIntentId` y emitiendo un código de autorización bancaria oficial (`authorizationCode`).
  7. El cliente recibe su comprobante digital en alta resolución con el sello *"PAGADA EN LÍNEA"* y los últimos 4 dígitos de la tarjeta.

---

### Caso de Uso CU-21: Moderación y Réplica Oficial de Reseñas en PMS Admin
* **Actores:** Administrador General (`ROLE_ADMIN`), Recepcionista (`ROLE_RECEPTIONIST`).
* **Precondición:** Usuario corporativo autenticado con acceso a la intranet PMS.
* **Flujo Principal:**
  1. El Administrador o Recepcionista hace clic en la pestaña *"Reseñas & Reputación"* del menú superior.
  2. El sistema consulta `/api/reviews/admin` y `/api/reviews/admin/metrics`, desplegando las 4 tarjetas KPI (Calificación General, Índice de Satisfacción, Opiniones Pendientes y Atendidas).
  3. El usuario puede filtrar por estrellas (1★ a 5★) o visualizar únicamente las opiniones sin respuesta.
  4. Para cualquier reseña pendiente, el Administrador presiona *"Responder como Gerencia"*.
  5. Se abre el editor interactivo; el Administrador redacta la réplica oficial y presiona *"Publicar Réplica"*.
  6. El sistema envía `POST /api/reviews/{id}/management-response` y actualiza inmediatamente la opinión con el sello dorado corporativo de Gerencia General.

---

### Caso de Uso CU-22: Generación y Descarga de Folio Fiscal / Factura Digital en PDF
* **Actores:** Huésped (`ROLE_CUSTOMER`), Recepcionista (`ROLE_RECEPTIONIST`), Administrador (`ROLE_ADMIN`).
* **Precondición:** Existe una reserva confirmada o completada en el sistema.
* **Flujo Principal:**
  1. En el portal del huésped (`/my-reservations`) o en el Room Rack/Operaciones de Front Desk, el usuario presiona el botón *"Folio / Factura"*.
  2. El sistema despliega el modal interactivo `HotelFolioModalComponent`, consultando al instante las comandas de Room Service y Spa asociadas mediante `/api/room-orders/reservation/{id}`.
  3. Se genera en pantalla el documento fiscal oficial con membrete de Hospentra Luxury Sanctuary, número de folio `INV-HOSP-XXXXX-2026`, desglose itemizado de alojamiento y consumos, desglose de IVA (16%) y código de autorización bancaria.
  4. El usuario presiona *"Descargar / Imprimir PDF"*, activando la vista de impresión optimizada para guardar el archivo PDF o imprimir en papel oficial A4.

---

### Caso de Uso CU-23: Análisis de Inteligencia de Negocio Hotelero (RevPAR / ADR)
* **Actores:** Administrador General (`ROLE_ADMIN`).
* **Precondición:** Administrador autenticado en el sistema.
* **Flujo Principal:**
  1. El Administrador hace clic en la pestaña *"Métricas & Business Intelligence"*.
  2. El sistema invoca `GET /api/admin/analytics`, calculando en tiempo real las métricas agregadas del resort:
     * Facturación bruta consolidada (Alojamiento vs. Consumos extras).
     * ADR (Average Daily Rate): Tarifa promedio por noche vendida.
     * RevPAR (Revenue Per Available Room): Eficiencia financiera de las habitaciones disponibles.
     * Tasa de Ocupación Global: Noches ocupadas frente a la capacidad instalada.
  3. El panel presenta el gráfico de barras apiladas de tendencia mensual de ingresos y la distribución porcentual de unidades de negocio (Suites, Cocina KDS, Cava y Spa).
  4. La tabla de Ranking de Rendimiento muestra el desempeño individual de cada suite ordenado por facturación, con insignias de medallas (Oro, Plata, Bronce) para los mejores desempeños.

---

### Caso de Uso CU-24: Verificación y Reenvío de Código OTP por Correo
* **Actores:** Huésped / Cliente (`ROLE_CUSTOMER`).
* **Precondición:** El usuario ha completado el formulario de registro o solicita un nuevo código.
* **Flujo Principal de Validación de Código:**
  1. El usuario visualiza la pantalla de verificación OTP con su correo receptor visible.
  2. El usuario introduce los 6 dígitos recibidos (mediante digitación secuencial o pegando el código con Ctrl+V).
  3. Al completarse el 6.° dígito, el frontend bloquea la entrada para evitar doble sumisión y despacha `POST /api/auth/verify-email`.
  4. El backend verifica la existencia, coincidencia y no caducidad del código activo en `verification_codes`.
  5. El código se marca como consumido (`used = true`), el usuario pasa a `enabled = true` y `email_verified = true`.
  6. El sistema emite la respuesta con `AuthResponse` que contiene tokens JWT y Refresh Token; el cliente es redirigido a la pantalla principal.
* **Flujo Alternativo (Reenvío de Código con Cooldown de 60 Segundos):**
  1. Si el código no ha llegado o ha expirado, el usuario presiona *"Reenviar código nuevo"*.
  2. El frontend desactiva el botón de reenvío y activa un temporizador regresivo de 60 segundos.
  3. El backend invalida códigos anteriores, genera un nuevo OTP de 6 dígitos y lo remite por correo HTML.
* **Flujo Alternativo (Corrección de Correo):**
  1. Si el usuario ingresó un correo erróneo, presiona *"Modificar correo"*.
  2. La vista regresa al paso inicial del formulario preservando los datos ya ingresados para su corrección.

---

### Caso de Uso CU-25: Gestión Operativa de Limpieza, Asignación de Camaristas e Inspección (Housekeeping)
* **Actores:** Recepcionista de Turno (`ROLE_RECEPTIONIST`), Administrador General (`ROLE_ADMIN`).
* **Precondición:** Colaborador corporativo autenticado con acceso al PMS Front Desk.
* **Flujo Principal:**
  1. El personal accede al Room Rack dentro de Operaciones de Front Desk.
  2. El sistema clasifica visualmente las suites por nivel de piso (Pisos 1 al 4) y permite filtrar suites en estado `MAINTENANCE` mediante la pestaña *"🧹 Requieren Limpieza"*.
  3. Para cualquier suite desocupada o en proceso de limpieza, el recepcionista presiona *"Inspección"* para desplegar la `Ficha de Housekeeping & Inspección`.
  4. Se asigna la camarista responsable de la nómina hotelera (*ej. María Cruz, Carmen Delgado*) y se define la prioridad operativa (`HIGH` para llegadas del día).
  5. Se activa el checklist de calidad (minibar, lencería, jacuzzi, amenities botánicos y aromaterapia).
  6. Al presionar *"Guardar y Marcar Lista"*, el backend persiste la información mediante `PATCH /api/rooms/{id}/housekeeping`, transiciona la suite a `AVAILABLE` y registra el timestamp de higienización.
  7. El sistema despacha un evento SSE `ROOM_STATUS_CHANGED` actualizando de inmediato el Room Rack, el Tape Chart Gantt y el catálogo público.
* **Flujo Alternativo (Reporte de Incidencia o Falla Técnica):**
  1. Si la camarista reporta una avería (ej. fuga en jacuzzi o desperfecto eléctrico), se activa el conmutador *"Reportar Incidencia o Mantenimiento Menor"*.
  2. Se redacta la observación y se presiona *"Guardar Ficha"*.
  3. La habitación permanece en `MAINTENANCE` con una etiqueta roja de alerta *"⚠️ Incidencia Reportada"*, bloqueando su asignación o entrega accidental a nuevos huéspedes.
* **Flujo Alternativo (Liberación Rápida de 1 Clic):**
  1. En la tarjeta de la suite en el Room Rack, el recepcionista presiona directamente *"✓ Marcar Lista"*, devolviendo la suite al inventario vendible sin abrir el modal.

---

### Caso de Uso CU-26: Galería Fotográfica de Suites, Carga Directa de Portada y Visor Lightbox
* **Actores:** Administrador General (`ROLE_ADMIN`), Huésped / Público (`ROLE_CUSTOMER`, Anónimo).
* **Precondición:** Suite registrada en inventario o cliente navegando en el catálogo.
* **Flujo Principal de Carga de Portada en Alta de Suite:**
  1. El Administrador abre *"Nueva Categoría de Suite"* en el inventario.
  2. Mediante la zona Drag & Drop integrada, selecciona o arrastra la fotografía principal de la suite.
  3. Al presionar *"Guardar Categoría"*, el sistema guarda la entidad y despacha automáticamente la carga atómica del archivo multipart (`uploadRoomImage`), enlazándola de inmediato como foto de portada oficial.
* **Flujo Principal de Exploración de Galería y Visor Lightbox para Huéspedes:**
  1. El huésped ingresa al detalle de una habitación en `/rooms/:id`.
  2. El sistema consulta `/api/room-types/{id}/images` y compone la galería priorizando la foto de portada y presentando los ambientes referenciales.
  3. El usuario puede navegar las imágenes con las flechas flotantes previa/siguiente sobre la fotografía hero.
  4. Al hacer clic sobre la foto o en el botón *"Ver Fotos en Pantalla Completa"*, se abre el visor Lightbox inmersivo con fondo oscuro desenfocado (`backdrop-filter: blur(14px)`), contador de foto, carrusel y tira inferior interactiva de miniaturas.
  5. El usuario puede controlar la visualización tanto con controles táctiles/ratón como mediante el teclado físico (teclas `ArrowLeft`, `ArrowRight` y cierre instantáneo con `Escape`).

---

### Caso de Uso CU-27: Autogestión de Perfil, Cambio de Contraseña Blindado por OTP y Protocolo de Gracia de 30 Días (Derecho al Olvido / RGPD)
* **Actores:** Cualquier usuario autenticado (`ROLE_CUSTOMER`, `ROLE_RECEPTIONIST`, `ROLE_KITCHEN`, `ROLE_ADMIN`).
* **Precondición:** El usuario tiene sesión iniciada y accede a `/account/settings` desde el avatar de la barra de navegación o drawer móvil.
* **Flujo Principal de Edición de Perfil:**
  1. El usuario visualiza sus datos actuales (nombre, apellido, teléfono, email, rol y fecha de creación).
  2. Modifica su nombre, apellido o teléfono y presiona "Guardar Cambios".
  3. El sistema valida los campos y actualiza atómicamente la entidad `User` y la entidad `Guest` asociada, emitiendo una notificación de éxito.
* **Flujo Principal de Cambio de Contraseña con Verificación OTP:**
  1. El usuario navega a la pestaña "Seguridad" y presiona "Solicitar Código de Seguridad".
  2. El backend genera un código criptográfico de 6 dígitos con vigencia de 15 minutos en `verification_codes` y lo despacha al correo registrado vía `EmailService`.
  3. La interfaz activa un temporizador de enfriamiento regresivo de 60 segundos para evitar saturación de envíos.
  4. El usuario ingresa el código de 6 dígitos, la nueva contraseña (mínimo 6 caracteres) y la confirmación.
  5. El backend valida el código, comprueba que coincidan las contraseñas, encripta con BCrypt y actualiza la cuenta.
* **Flujo Principal de Protocolo de Eliminación con Gracia de 30 Días:**
  1. El usuario navega a la pestaña "Privacidad" y presiona "Solicitar Eliminación de Cuenta".
  2. Se abre un diálogo modal de confirmación solicitando la contraseña actual del usuario y motivo opcional.
  3. El backend verifica la contraseña y ejecuta las validaciones de negocio:
     - Bloquea la eliminación si el usuario mantiene reservas activas (`PENDING`, `CONFIRMED`, `CHECKED_IN`).
     - Bloquea la eliminación si el usuario es el último administrador activo del sistema.
  4. Si cumple los requisitos, establece `scheduled_deletion_date = ahora + 30 días` y `pendingDeletion = true`.
  5. La interfaz presenta un banner persistente ámbar con el cálculo dinámico de días restantes y un botón de cancelación en un solo clic.
  6. Si el usuario presiona "Cancelar Eliminación", la fecha programada se anula y la cuenta regresa al estado activo normal.
  7. Al transcurrir los 30 días, el cron diario `AccountPurgeScheduler` anonimiza la ficha de `Guest` para preservar la trazabilidad fiscal y purga definitivamente el registro `User`.

---

### Caso de Uso CU-28: Supervisión de Auditoría Operativa y Exportación de Libros Financieros Oficiales
* **Actor Principal:** Administrador General (`ROLE_ADMIN`).
* **Precondición:** El usuario tiene sesión iniciada con credenciales de administrador y accede a `/admin/dashboard` $\rightarrow$ pestaña *"Auditoría & Libros"*.
* **Flujo Principal de Supervisión y Búsqueda Forense:**
  1. El administrador visualiza las 4 tarjetas KPI: Eventos Históricos, Actividad de Hoy, Fallos/Incidentes y Tipos de Acciones monitorizadas.
  2. El sistema presenta la tabla cronológica paginada con fecha/hora, usuario, rol, acción con glifo distintivo, entidad/folio, IP y resultado (`SUCCESS`/`FAILURE`).
  3. El administrador filtra por tipo de acción (ej. `CHECK_IN`, `CREATE_STAFF`, `CHANGE_PASSWORD`), busca por nombre de usuario o delimita un rango de fechas (*Desde* y *Hasta*).
  4. Al presionar el botón de expansión en cualquier fila, el sistema despliega el detalle completo en lenguaje natural y metadatos de auditoría.
* **Flujo Principal de Exportación Contable:**
  1. El administrador hace clic en *"Libro de Reservas (CSV)"* o *"Diario Financiero (CSV)"*.
  2. El backend procesa las estancias y comandas filtradas por fecha, formateando cada registro y anteponiendo el identificador de codificación **UTF-8 con BOM** (`\uFEFF`).
  3. El frontend recibe el flujo binario como `Blob`, genera el enlace temporal y dispara la descarga instantánea en el navegador con un nombre formal fechado (`hospentra_reservas_YYYY-MM-DD.csv`).
  4. El administrador abre el archivo en Microsoft Excel o Google Sheets con columnas, tildes y caracteres especiales perfectamente estructurados y listos para conciliación fiscal.

---

## 4. Matriz de Endpoints de la API REST

| Método | Endpoint | Rol Requerido | Descripción |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/auth/register` | Público | Registro de nuevos huéspedes (`ROLE_CUSTOMER`) con generación y despacho de código OTP |
| **POST** | `/api/auth/verify-email` | Público | Validación de código OTP de 6 dígitos, activación de cuenta y emisión de tokens JWT |
| **POST** | `/api/auth/resend-verification` | Público | Reenvío de nuevo código OTP con invalidación de anteriores y cooldown de 60s |
| **POST** | `/api/auth/login` | Público | Autenticación y generación de JWT para todos los roles (Bloqueo si cuenta no está verificada) |
| **POST** | `/api/auth/refresh` | Público / Auth | Refresco y rotación de tokens JWT mediante `refreshToken` |
| **GET** | `/api/room-types` | Público | Catálogo de tipos de habitación boutique |
| **GET** | `/api/room-types/{id}/images` | Público | Galería de imágenes de la suite (portada y fotos referenciales) |
| **POST** | `/api/room-types/{id}/images` | `ROLE_ADMIN` | Subida de archivos multipart de imagen con opción de marcar portada |
| **PATCH**| `/api/room-images/{id}/set-cover` | `ROLE_ADMIN` | Promover fotografía referencial a foto de portada principal |
| **DELETE**| `/api/room-images/{id}` | `ROLE_ADMIN` | Eliminación de fotografía de la suite y del disco físico |
| **GET** | `/api/rooms` | Público / Auth | Listado de habitaciones y estados de ocupación |
| **GET** | `/api/rooms/{id}` | Público / Auth | Detalle de habitación específica |
| **PATCH**| `/api/rooms/{id}/status`| `ADMIN`, `RECEPTIONIST` | Actualizar estado de suite (`AVAILABLE`, `MAINTENANCE`) |
| **PATCH**| `/api/rooms/{id}/housekeeping`| `ADMIN`, `RECEPTIONIST` | Actualizar camarista asignada, notas de inspección, prioridad y liberación |
| **POST** | `/api/rooms` | `ROLE_ADMIN` | Alta de nuevas habitaciones en catálogo |
| **PUT** | `/api/rooms/{id}` | `ROLE_ADMIN` | Modificación de datos de habitación |
| **DELETE**| `/api/rooms/{id}`| `ROLE_ADMIN` | Baja de habitación del inventario |
| **GET** | `/api/reservations/my` | Autenticado | Listado exclusivo de reservas propias del huésped autenticado (Aislamiento de privacidad) |
| **GET** | `/api/reservations` | `ADMIN`, `RECEPTIONIST` | Listado global de todas las reservas del hotel para gestión operativa PMS en Front Desk |
| **POST** | `/api/reservations` | Autenticado | Creación de reserva con modalidad de pago |
| **GET** | `/api/reservations/{id}` | Autenticado | Detalle de una reserva específica (Valida propiedad si el invocador es huésped) |
| **PATCH**| `/api/reservations/{id}/confirm` | `ADMIN`, `RECEPTIONIST` | Confirmar reserva pendiente |
| **PATCH**| `/api/reservations/{id}/check-in` | `ADMIN`, `RECEPTIONIST` | Registrar check-in y entrega de llaves |
| **PATCH**| `/api/reservations/{id}/complete` | `ADMIN`, `RECEPTIONIST` | Registrar check-out y enviar suite a limpieza |
| **PATCH**| `/api/reservations/{id}/cancel` | Autenticado | Cancelar reserva aplicando política de reembolso (Valida propiedad si es huésped) |
| **GET** | `/api/guests` | `ADMIN`, `RECEPTIONIST` | Directorio de huéspedes y perfiles de contacto |
| **GET** | `/api/users` | `ROLE_ADMIN` | Directorio de cuentas corporativas con turnos y presencia |
| **GET** | `/api/users/{id}` | `ROLE_ADMIN` | Consulta de perfil de colaborador corporativo por identificador |
| **POST** | `/api/users` | `ROLE_ADMIN` | Creación de cuentas de staff (`RECEPTIONIST`, `KITCHEN`, `ADMIN`) con asignación de turno |
| **PUT** | `/api/users/{id}` | `ROLE_ADMIN` | Modificación de email, rol institucional, turno horario y cambio opcional de contraseña |
| **PATCH**| `/api/users/{id}/toggle-status` | `ROLE_ADMIN` | Soft delete / reactivación de cuenta (bloquea auto-desactivación y desactivación del último admin activo) |
| **PATCH**| `/api/users/{id}/work-status` | `ADMIN`, `RECEPTIONIST`, `KITCHEN` | Alternar estado de guardia y refrigerio (`ON_DUTY`, `ON_BREAK`) |
| **GET** | `/api/extra-services` | Público | Catálogo boutique de servicios de Room Service, Bar y Spa |
| **GET** | `/api/extra-services/category/{category}` | Público | Filtrar servicios por categoría (`GASTRONOMY`, `BAR_WINE`, `WELLNESS_SPA`) |
| **POST** | `/api/room-orders` | Autenticado | Crear comanda de Room Service o Spa vinculada a suite activa |
| **GET** | `/api/room-orders` | Autenticado | Historial general de comandas para administración y front desk |
| **GET** | `/api/room-orders/active` | `ADMIN`, `RECEPTIONIST`, `KITCHEN` | Comandas activas para cocina KDS (Filtro automático de gastronomía/bar) |
| **GET** | `/api/room-orders/reservation/{id}` | Autenticado | Comandas de Room Service asociadas a una reserva (Folio) |
| **PATCH**| `/api/room-orders/{id}/status` | `ADMIN`, `RECEPTIONIST`, `KITCHEN` | Actualizar estado operativo de comanda |
| **PATCH**| `/api/room-orders/{id}/assign-therapist` | `ADMIN`, `RECEPTIONIST` | Asignar especialista a solicitud de Spa y avanzar a `PREPARING` con validación de agenda |
| **GET** | `/api/room-orders/busy-therapists` | `ADMIN`, `RECEPTIONIST` | Consulta los IDs de terapeutas con citas asignadas en un horario determinado (`scheduledTime`) |
| **GET** | `/api/therapists` | `ADMIN`, `RECEPTIONIST` | Directorio completo de especialistas y terapeutas de spa |
| **GET** | `/api/therapists/active` | `ADMIN`, `RECEPTIONIST` | Listado exclusivo de terapeutas activos en guardia |
| **GET** | `/api/therapists/{id}` | `ADMIN`, `RECEPTIONIST` | Consulta de terapeuta específico por identificador |
| **POST** | `/api/therapists` | `ADMIN`, `RECEPTIONIST` | Alta de nuevo especialista en el directorio de spa |
| **PUT** | `/api/therapists/{id}` | `ADMIN`, `RECEPTIONIST` | Actualización de perfil, especialidad y teléfono del terapeuta |
| **PATCH**| `/api/therapists/{id}/toggle-status` | `ADMIN`, `RECEPTIONIST` | Alternar disponibilidad en guardia (`active: true / false`) |
| **DELETE**| `/api/therapists/{id}` | `ADMIN`, `RECEPTIONIST` | Eliminación definitiva de especialista del directorio |
| **GET** | `/api/payments/config` | Público / Auth | Obtiene la clave pública de Stripe para inicialización del frontend |
| **POST** | `/api/payments/create-intent` | Autenticado | Crea un PaymentIntent de Stripe con monto exacto en céntimos |
| **POST** | `/api/reviews` | Autenticado | Publicar calificación y opinión de estadía verificada (`COMPLETED`) con reglas antifraude |
| **GET** | `/api/reviews/room/{roomId}` | Público | Listado paginado de reseñas de una habitación con ordenamiento |
| **GET** | `/api/reviews/room/{roomId}/summary` | Público | Resumen estadístico estilo Play Store: promedio general, desglose 5★ a 1★ y métricas dimensionales |
| **GET** | `/api/reviews/reservation/{id}` | Autenticado | Consulta la reseña asociada a una reserva específica |
| **POST** | `/api/reviews/{id}/management-response` | `ROLE_ADMIN` | Registro de respuesta corporativa de gerencia a una reseña |
| **GET** | `/api/reviews/admin` | `ADMIN`, `RECEPTIONIST` | Listado paginado de reseñas de todo el resort con filtros por estrellas y estado de respuesta |
| **GET** | `/api/reviews/admin/metrics` | `ADMIN`, `RECEPTIONIST` | Métricas de moderación: total opiniones, promedio general, contador sin respuesta e índice de satisfacción |
| **GET** | `/api/admin/analytics` | `ROLE_ADMIN` | Business Intelligence hotelero: ADR, RevPAR, ocupación, facturación por categoría y ranking de suites |
| **GET** | `/api/notifications/stream` | Autenticado | Canal continuo de eventos en tiempo real mediante Server-Sent Events (SSE) con autenticación por header o parámetro token |
| **GET** | `/api/account/profile` | Autenticado | Consulta del perfil personal, datos de contacto y estado de eliminación programada |
| **PUT** | `/api/account/profile` | Autenticado | Actualización de nombre, apellido y teléfono con sincronización bidireccional a ficha de huésped |
| **POST** | `/api/account/change-password/request-otp` | Autenticado | Envío de código OTP de seguridad de 6 dígitos al correo verificado del usuario |
| **POST** | `/api/account/change-password` | Autenticado | Cambio seguro de contraseña validando el código OTP de un solo uso con encriptación BCrypt |
| **POST** | `/api/account/request-deletion` | Autenticado | Solicitud de eliminación con validación de contraseña actual y asignación de 30 días de gracia |
| **POST** | `/api/account/cancel-deletion` | Autenticado | Cancelación inmediata del periodo de gracia y restauración de cuenta a estado activo normal |
| **GET** | `/api/admin/audit/logs` | `ROLE_ADMIN` | Consulta paginada de la bitácora inmutable de auditoría con filtros por acción, operador y fechas |
| **GET** | `/api/admin/audit/actions` | `ROLE_ADMIN` | Listado dinámico de códigos de acción registrados para filtrado |
| **GET** | `/api/admin/audit/metrics` | `ROLE_ADMIN` | Indicadores KPI de auditoría: total de eventos, operaciones de hoy, fallos y tipos de acción |
| **GET** | `/api/admin/audit/export/reservations` | `ROLE_ADMIN` | Exportación del libro oficial de reservaciones en CSV codificado con UTF-8 BOM para Excel |
| **GET** | `/api/admin/audit/export/financial-journal` | `ROLE_ADMIN` | Exportación del diario financiero consolidado (estancias y consumos) en CSV con UTF-8 BOM |

---

## 5. Esquema de Base de Datos y Migraciones Flyway

* **`V1__init_schema.sql`**: Define las tablas primarias `room_types`, `rooms`, `guests` y `reservations`, con claves foráneas, índices de búsqueda y restricciones de integridad referencial.
* **`V2__create_users_table.sql`**: Define la tabla `users` con campos para autenticación: `username`, `email`, `password` (hash BCrypt), `role` (`ROLE_ADMIN`, `ROLE_RECEPTIONIST`, `ROLE_CUSTOMER`) y `enabled`.
* **`V3__link_users_and_guests.sql`**: Establece la relación de clave foránea `user_id` en la tabla `guests` para vincular perfiles de huésped con credenciales de acceso.
* **`V4__seed_rooms.sql`**: Poblamiento inicial del catálogo boutique con suites de autor (Penthouse Imperial, Royal Suite, Panoramic Luxury, etc.) y asignación de números de habitación físicos.
* **`V5__add_payment_fields.sql`**: Incorpora soporte nativo en la entidad `reservations` para las columnas `payment_method` (`CREDIT_CARD`, `AT_RECEPTION`), `payment_status` (`PAID`, `PENDING`, `REFUNDED`) y `card_last4`.
* **`V6__add_shifts_and_room_orders.sql`**: 
  * Agrega a `users` las columnas `shift` (`VARCHAR(20) DEFAULT 'MORNING'`), `work_status` (`VARCHAR(20) DEFAULT 'ON_DUTY'`) y `break_started_at` (`TIMESTAMP`).
  * Crea la tabla `extra_services` (catálogo boutique de gastronomía, bar y spa) y puebla 8 productos iniciales de alta gama.
  * Crea las tablas `room_orders` (cabecera de comanda con relación a reserva, suite, huésped, total, estado culinario y notas) y `room_order_items` (desglose de ítems, cantidades y subtotales).
* **`V7__create_therapists_and_spa_fields.sql`**:
  * Crea la tabla `therapists` (`id`, `name`, `specialty`, `phone`, `active`, `created_at`) y siembra 3 especialistas certificados de referencia.
  * Extiende la tabla `room_orders` con columnas operativas de spa: `therapist_id` (FK a `therapists`), `therapist_name` (`VARCHAR(150)`), `service_location` (`VARCHAR(50) DEFAULT 'IN_SUITE'`) y `scheduled_time` (`VARCHAR(50)`).
* **`V8__add_multilingual_fields.sql`**:
  * Añade las columnas `name_en` (`VARCHAR(150)`) y `description_en` (`TEXT`) a las tablas `room_types` y `extra_services`.
  * Siembra las traducciones al inglés de alta hospitalidad de autor para todas las suites emblemáticas y servicios iniciales.
* **`V9__seed_remaining_multilingual_services.sql`**:
  * Completa el catálogo bilingüe para los servicios de gastronomía gourmet restantes (`Solomillo de Autor al Malbec`, `Tabla Ibérica & Quesos Artesanales`, `Desayuno Imperial a la Suite`).
* **`V10__add_performance_indexes.sql`**:
  * Incorpora 10 índices de alto rendimiento en PostgreSQL para optimizar consultas de solapamiento de fechas (`idx_reservations_overlap`), claves foráneas y folios de Room Service.
* **`V11__add_payment_intent_and_auth_code.sql`**:
  * Extiende la tabla `reservations` con las columnas `payment_intent_id` (`VARCHAR(150)`) y `authorization_code` (`VARCHAR(50)`) para auditoría y trazabilidad con la pasarela Stripe.
* **`V12__create_reviews_table.sql`**:
  * Crea la tabla `reviews` con restricción de unicidad estricta `UNIQUE (reservation_id)` para impedir calificaciones múltiples sobre una misma estancia.
  * Define claves foráneas a `rooms`, `guests` y `reservations`.
  * Incorpora columnas para calificación global (`rating` de 1 a 5 con restricción `CHECK`), desglose dimensional (`cleanliness_rating`, `staff_rating`, `location_rating`, `facilities_rating`, `value_rating`), comentario, marca de verificación (`verified_stay`) y réplica oficial de gerencia (`management_response`, `management_responded_at`).
  * Siembra reseñas iniciales de referencia de 5★, 4★ y 3★ con respuestas de administración para alimentar el gráfico de reputación de la suite Penthouse Imperial.
* **`V13__add_email_verification.sql`**:
  * Extiende la tabla `users` incorporando la columna booleana `email_verified` (`BOOLEAN NOT NULL DEFAULT FALSE`).
  * Actualiza a `TRUE` las cuentas corporativas y administradores preexistentes.
  * Crea la tabla `verification_codes` (`id`, `user_id` FK a `users`, `code` `VARCHAR(10)`, `type` `VARCHAR(50)`, `expires_at` `TIMESTAMP`, `used` `BOOLEAN DEFAULT FALSE`, `created_at` `TIMESTAMP`).
  * Incorpora índices optimizados de búsqueda: `idx_verification_codes_lookup` (`user_id`, `code`, `used`) y `idx_verification_codes_type` (`type`).
* **`V14__create_room_images_table.sql`**:
  * Añade la columna `cover_image_url` (`VARCHAR(500)`) a la tabla `room_types` para disponibilidad y carga ultrarrápida en el catálogo.
  * Crea la tabla `room_images` (`id`, `room_type_id` FK a `room_types` con `ON DELETE CASCADE`, `image_url`, `is_cover`, `display_order`, `created_at`).
  * Añade los índices de búsqueda `idx_room_images_room_type` y `idx_room_images_cover`.
  * Siembra la colección inicial de 30 fotografías de autor en alta resolución para las 6 categorías de suites existentes (1 portada y 4 referenciales por suite).
* **`V15__add_housekeeping_to_rooms.sql`**:
  * Añade las columnas `housekeeping_priority`, `assigned_housekeeper`, `housekeeping_notes`, `last_cleaned_at` e `incident_reported` a la tabla `rooms`.
  * Añade el índice `idx_rooms_housekeeping_priority` para filtrado ágil en el Room Rack.
* **`V16__add_account_deletion_fields_to_users.sql`**:
  * Añade las columnas `deletion_requested_at` (`TIMESTAMP`) y `scheduled_deletion_date` (`TIMESTAMP`) a la tabla `users`.
  * Añade el índice parcial `idx_users_scheduled_deletion` sobre `scheduled_deletion_date` para acelerar las consultas del scheduler nocturno de purga.
* **`V17__create_audit_logs_table.sql`**:
  * Crea la tabla `audit_logs` (`id BIGSERIAL`, `username`, `user_role`, `action`, `entity_name`, `entity_id`, `details`, `ip_address`, `status`, `created_at`).
  * Añade los índices de alto rendimiento `idx_audit_logs_created_at`, `idx_audit_logs_action`, `idx_audit_logs_username` e `idx_audit_logs_entity` para búsquedas y auditorías forenses instantáneas.

---

## 6. Verificación, Seguridad y Calidad del Software

* **Compilación Backend:** Java 25 / Spring Boot 3 compila con `0` errores (`BUILD SUCCESS` en 3.5 s).
* **Compilación Frontend:** Angular 22 genera los paquetes de distribución de producción con `0` errores y `0` advertencias en 3.3 s.
* **Suite de Pruebas Automatizadas (Testing Suite JUnit 5 + Mockito + Vitest):**
  * **199 pruebas automatizadas ejecutadas y aprobadas al 100%** (166 pruebas en Backend Spring Boot y 33 pruebas unitarias en Frontend Angular).
  * [`AuditLogServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/AuditLogServiceImplTest.java) (8 tests): Verificación exhaustiva del motor de auditoría: registro de eventos con extracción de credenciales e IP de sesión, tolerancia a fallos de persistencia sin abortar la transacción de negocio, filtrado paginado multidimensional, cálculo de métricas de seguridad y generación de archivos CSV de reservaciones y diario financiero con codificación UTF-8 con BOM.
  * [`AuditControllerTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/web/controller/AuditControllerTest.java) (5 tests): Pruebas de integración HTTP MockMvc para los endpoints `/api/admin/audit/**`: consulta paginada de logs (`200 OK`), listado de acciones dinámicas, KPIs de auditoría y descarga de archivos CSV con cabeceras `Content-Disposition`.
  * [`AuditLogsComponent`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/features/admin/dashboard/components/audit-logs/audit-logs.component.spec.ts) (8 tests en Vitest): Pruebas del componente Angular con Signals: renderizado de KPIs, reseteo y aplicación reactiva de filtros, paginación, alternancia de detalles expandibles, descarga de libros de reservas y diarios financieros en el navegador, y mapeo de iconos semánticos.
  * [`AuditService`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/core/services/audit.service.spec.ts) (6 tests en Vitest): Pruebas del servicio HTTP con `HttpTestingController` para consultas paginadas y descarga de archivos binarios tipo `Blob`.
  * [`AccountServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/AccountServiceImplTest.java) (13 tests): Cobertura integral de la lógica de autogestión de usuario: obtención de perfil, actualización de datos personales con sincronización a `Guest`, solicitud de código OTP para cambio de contraseña, verificación y actualización de contraseña con BCrypt, solicitud de eliminación con verificación de contraseña actual y asignación de 30 días de gracia, **bloqueo estricto de eliminación ante reservas activas o pendientes** (`PENDING`, `CONFIRMED`, `CHECKED_IN`), **blindaje anti-bloqueo del último administrador activo**, y revocación / cancelación inmediata del protocolo de eliminación con retorno al estado activo normal.
  * [`AccountPurgeSchedulerTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/scheduler/AccountPurgeSchedulerTest.java) (2 tests): Verificación del cron diario automatizado (`0 0 3 * * ?`) que busca cuentas vencidas (`scheduledDeletionDate <= now()`), ejecuta la anonimización legal de datos PII en la entidad `Guest` asociada para proteger la integridad fiscal y contable, y purga físicamente el registro `User` sin violar restricciones foráneas.
  * [`AccountControllerTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/web/controller/AccountControllerTest.java) (9 tests): Pruebas de integración MockMvc para la API REST `/api/account/**`: `GET /profile`, `PUT /profile`, `POST /change-password/request-otp`, `POST /change-password`, `POST /request-deletion` y `POST /cancel-deletion`, verificando códigos HTTP `200 OK`, `400 Bad Request` ante reglas de negocio y `409 Conflict`.
  * [`AccountSettingsComponent`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/features/account/account-settings/account-settings.component.spec.ts) (11 tests en Vitest): Pruebas del componente de autogestión de perfil con Angular Signals: alternancia reactiva de pestañas (Perfil, Seguridad, Privacidad), validaciones síncronas de datos, despacho de OTP con temporizador de enfriamiento regresivo de 60s, coincidencia de nuevas credenciales, apertura y validación del modal de confirmación con contraseña, solicitud de gracia de 30 días, cálculo dinámico de `remainingGraceDays` y cancelación en un clic.
  * [`AccountService`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/core/services/account.service.spec.ts) (7 tests en Vitest): Pruebas del cliente HTTP Angular con `HttpTestingController` para todas las operaciones REST de `/api/account/**`.
  * [`UserServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/UserServiceImplTest.java) (19 tests): Cobertura completa de la lógica de negocio de personal y directores: creación de administradores con contraseña encriptada, bloqueo de rol cliente para staff, unicidad de username y email, actualización de perfil con reseteo opcional de contraseña, bloqueo de contraseñas cortas (< 6 caracteres), bloqueo estricto de auto-revocación del rol ADMIN, soft-delete con bloqueo de auto-desactivación, blindaje anti-bloqueo del último administrador activo, reactivación de cuentas desactivadas y control de marcas de tiempo de refrigerio (`WorkStatus`).
  * [`UserControllerTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/web/controller/UserControllerTest.java) (13 tests): Pruebas de integración HTTP MockMvc para la API de gestión de colaboradores: creación exitosa con `201 Created`, validaciones de email y longitud de contraseña (`400 Bad Request`), actualización de personal con sesión en `Principal`, mapeo de conflictos de correo (`409 Conflict`), mapeo de violaciones de reglas de negocio (`400 Bad Request`), conmutación de estado `PATCH /api/users/{id}/toggle-status`, transiciones de presencia de mostrador y consultas por ID.
  * [`RoomImageServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/RoomImageServiceImplTest.java) (5 tests): Cobertura del motor multimedia: consulta de imágenes por tipo de suite, subida y asignación automática de portada en primera fotografía, reasignación dinámica de portada con desmarcado de la anterior, y eliminación de archivos con reasignación en cascada de portada.
  * [`ReviewServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/ReviewServiceImplTest.java) (25 tests): Cobertura exhaustiva de las reglas de negocio y blindaje antifraude: rechazo estricto de calificaciones en reservas no culminadas (`PENDING`, `CONFIRMED`, `CHECKED_IN`, `CANCELLED` con `IllegalStateException`), rechazo de suplantación de identidad entre huéspedes (`IllegalArgumentException`), bloqueo de reseñas duplicadas sobre la misma estancia (`UNIQUE`), cálculo matemático de la distribución de barras Play Store (5★ a 1★), promedios multidimensionales, listados paginados de moderación administrativa con filtros por estrellas y pendientes, cálculo de KPIs de reputación corporativa y casos límite con cero reseñas.
  * [`ReviewControllerTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/web/controller/ReviewControllerTest.java) (12 tests): Pruebas de integración MockMvc con `PageableHandlerMethodArgumentResolver`: validación de rangos de estrellas (`@Min(1)`, `@Max(5)`), obligatoriedad de comentarios (`@NotBlank`), creación exitosa `HTTP 201 Created`, respuestas de error `HTTP 400 Bad Request`, endpoint de moderación `GET /api/reviews/admin` y métricas `GET /api/reviews/admin/metrics` con protección de roles (`ADMIN`, `RECEPTIONIST`).
  * [`AnalyticsServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/AnalyticsServiceImplTest.java) (5 tests): Verificación exhaustiva del motor de Business Intelligence y KPIs ejecutivos hoteleros: ADR (Average Daily Rate), RevPAR (Revenue Per Available Room), tasa de ocupación porcentual, desglose por departamentos de servicio (Habitaciones, Cocina/Gastronomía, Cava/Bar, Wellness Spa), exclusión estricta de reservaciones y órdenes canceladas en la facturación, cálculo resiliente ante repositorios vacíos (evitando divisiones por cero) y ordenamiento descendente del ranking de rentabilidad por suite.
  * [`AnalyticsControllerTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/web/controller/AnalyticsControllerTest.java) (2 tests): Pruebas de integración MockMvc para el endpoint de Business Intelligence `GET /api/admin/analytics` con validación de autorización de directores (`ROLE_ADMIN`) y manejo de respuestas predeterminadas.
  * [`AuthServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/AuthServiceImplTest.java) (6 tests): Pruebas del flujo de autenticación, generación y validación de códigos de verificación OTP de 6 dígitos, expiración, consumo de códigos, activación de usuarios (`enabled = true`, `emailVerified = true`), rotación de Refresh Tokens y emisión de JWT.
  * [`ReservationServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/ReservationServiceImplTest.java) (10 tests): Cobertura completa de la lógica financiera de cancelación con política de 48h (100% `REFUNDED` vs. 50% `PARTIALLY_REFUNDED`), cálculo matemático de estancias `noches × tarifa`, bloqueo de sobreventa por solapamiento de fechas (`existsOverlappingReservation`), y transiciones de housekeeping (Check-In $\rightarrow$ `OCCUPIED`, Check-Out $\rightarrow$ `MAINTENANCE`, Cancelación $\rightarrow$ `AVAILABLE`).
  * [`RoomOrderServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/RoomOrderServiceImplTest.java) (8 tests): Cobertura del motor de comandas de Room Service y Spa, cálculo de subtotales y totales, marcas de tiempo de entrega (`deliveredAt`), validación de inactividad de terapeutas y **bloqueo estricto de conflictos horarios de citas simultáneas**.
  * [`RoomServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/RoomServiceImplTest.java) (14 tests): Cobertura completa del ciclo CRUD de habitaciones físicas, asignación de tipo de suite, actualización de estado operativo (`AVAILABLE`, `OCCUPIED`, `MAINTENANCE`), filtrado por estado, emisión de eventos SSE y control de unicidad de números de suite.
  * [`RoomTypeServiceImplTest`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/application/services/RoomTypeServiceImplTest.java) (9 tests): Cobertura exhaustiva de la gestión de categorías de suite con campos bilingües nativos (`name`, `nameEn`, `description`, `descriptionEn`), capacidad máxima, tarificación base, control de nombres duplicados y eliminación segura.
  * [`ReservationBackendApplicationTests`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/test/java/com/hotel/ReservationBackendApplicationTests.java) (1 test): Verificación de arranque íntegro del contexto de Spring Boot.
* **Infraestructura de Verificación OTP y Notificaciones Transaccionales:**
  * Implementación de `EmailService` con `JavaMailSender` y plantillas de correo en HTML de lujo (`#0F172A`, `#C59B27`).
  * Generación segura de códigos OTP de 6 dígitos con expiración de 15 minutos en `verification_codes`.
  * Interfaz de usuario en dos pasos en Angular (`step = 'FORM'` vs. `step = 'OTP'`) con casillas numéricas independientes, foco secuencial, soporte para portapapeles (`Ctrl+V`), temporizador de enfriamiento (*cooldown*) de 60 segundos y protección idempotente contra condiciones de carrera.
  * Despacho automático de comprobantes de confirmación con folio oficial al completarse una reserva.
* **Política Estricta de Cero Comentarios:** Auditoría automatizada ejecutada sobre 167 archivos fuente (`.ts`, `.html`, `.scss`, `.java`, `.sql`): **0 comentarios encontrados** (`//`, `/* */`, `<!-- -->`, `#`). Código 100% autodocumentado, limpio y listo para producción empresarial.
* **Comunicación en Tiempo Real con Server-Sent Events (SSE):**
  * Implementación de `SseNotificationService` con `SseEmitter` y canal reactivo `GET /api/notifications/stream`.
  * Soporte dual de autenticación JWT (cabecera `Authorization: Bearer` o parámetro `?token=...` en `JwtAuthenticationFilter` para compatibilidad nativa con la API `EventSource` de los navegadores).
  * Latidos periódicos (`PING` cada 20s) con `@EnableScheduling` para prevenir caídas por inactividad de proxies o cortafuegos.
  * Eliminación del sondeo agresivo cada 10s en KDS y Dashboard PMS, reduciendo en más del 80% la carga sobre el pool HikariCP de la base de datos.
  * Despacho instantáneo de eventos de negocio: `ORDER_CREATED`, `ORDER_STATUS_CHANGED`, `ORDER_THERAPIST_ASSIGNED`, `ROOM_STATUS_CHANGED`, `RESERVATION_CREATED`, `RESERVATION_STATUS_CHANGED`.
  * Indicador visual en vivo con animación de pulso (`.live-stream-badge` / `.live-pulse-dot`) en la cabecera de la terminal de cocina y el dashboard de recepción.
* **Modularización de Componentes PMS (Fase 1):**
  * Descomposición de `AdminDashboardComponent` en 5 componentes hijos autónomos: `RoomRackComponent`, `VoucherModalComponent`, `TherapistDirectoryModalComponent`, `AssignTherapistModalComponent` y `TapeChartComponent`.
  * Reducción de más de 400 líneas HTML y 800 líneas SCSS en el componente principal, mejorando el rendimiento de renderizado y el aislamiento de estado.
* **Validación de Agenda de Terapeutas Spa (Fase 2):**
  * Validación dual (cliente y servidor) para prevenir solapamiento de turnos de especialistas. Endpoint `GET /api/room-orders/busy-therapists` y validación estricta en `RoomOrderServiceImpl.assignTherapist()`.
* **Experiencia Operativa Integral (Fase 3):**
  * Alertas sonoras en tiempo real para brigada culinaria en KDS mediante la Web Audio API (campana armónica dual sintetizada, sin dependencias externas, con almacenamiento local en `localStorage`).
  * Matriz Tape Chart (Gantt continuo de 14 días) en Front Desk con cálculo dinámico de ocupación diaria, navegación por semanas y popover de acciones directas (check-in, check-out, vouchers).
* **Corrección de Ligaduras Tipográficas en Iconografía Material Symbols:**
  * Se corrigió el error visual donde el identificador inexistente `person_celebrating` renderizaba el texto literal crudo `_CELEBRATING`.
  * Se estandarizó el uso del glifo oficial `self_improvement` (figura zen de bienestar/meditación) para los estados de asignación de spa en el monitor de Front Desk y en el stepper del portal de huéspedes.
* **Internacionalización Completa (i18n Español / Inglés) de Alto Rendimiento:**
  * **Servicio Reactivo Basado en Señales (`I18nService`):**
    * Diseñado en Angular moderno con `signal<'es' | 'en'>()` para conmutación de idioma instantánea, en memoria y con reactividad de cero recarga y cero parpadeo visual.
    * Persistencia automática de la preferencia del usuario en `localStorage` bajo la clave `hospentra_lang`.
    * Resolución recursiva de claves tipadas (`t('nav.home')`, `t('catalog.filterSuite')`, `t('checkout.confirmBtn')`) con interpolación dinámica de parámetros (`{n}`, `{year}`) y estrategia de rescate (*fallback*) transparente hacia el diccionario español en caso de claves ausentes.
  * **Diccionarios Estructurados (`translations.ts`):**
    * Cobertura exhaustiva de dominios de usuario: Navegación (`nav`), Banner Principal (`hero`), Catálogo de Suites (`catalog`), Experiencias y Amenidades (`amenities`), Checkout y Pagos (`checkout`), Portal de Huéspedes (`myReservations`), Room Service & Spa Concierge (`roomService`), y Pie de Página (`footer`).
    * Traducción bilingüe de nivel boutique con vocabulario hotelero de lujo (*5-Diamond Certified*, *Signature Gastronomy*, *Private Wellness & Spa*, *Live Kitchen*, *Front Desk*).
  * **Selector Elegante en Barra de Navegación (`NavbarComponent`):**
    * Componente selector flotante tipo píldora (`[ ES | EN ]`) integrado con la paleta de autor (#0F172A, #C59B27, #FFFFFF), con microinteracciones sutiles y soporte responsivo completo para escritorio y menú deslizable móvil.
  * **Estrategia Multilingüe de Base de Datos para Contenido Dinámico (Column-based Localization):**
    * Arquitectura de localización por columnas a nivel de base de datos (`name_en`, `description_en` en `room_types` y `extra_services`) con política de fallback transparente: `(i18n.currentLang() === 'en' && entity.nameEn) ? entity.nameEn : entity.name`.
    * Cero consumo de tokens de IA en tiempo de ejecución, cero latencia adicional en las consultas HTTP y protección absoluta contra fugas de seguridad o inyecciones de datos (*Prompt Injection*).
    * Integración bidireccional completa en DTOs, Mappers y entidades JPA de Spring Boot (`RoomType`, `ExtraService`, `RoomTypeResponse`, `RoomTypeRequest`), con migraciones versionadas y validadas mediante Flyway (`V8` y `V9`).
* **Optimización Crítica de Integridad y Rendimiento (P1 & P2):**
  * **Eliminación de Fallbacks Simulados en Checkout (P1):** Se erradicó la simulación de reservaciones falsas en [`checkout.component.ts`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/features/reservations/checkout/checkout.component.ts). Ante caídas del backend o errores de red, el sistema reporta limpiamente el error al usuario garantizando que nunca se confirme una reserva sin persistencia en base de datos.
  * **Resolución Segura de Claves Duplicadas en Validaciones:** Se actualizó [`GlobalExceptionHandler.java`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/main/java/com/hotel/web/handler/GlobalExceptionHandler.java) implementando el merge resolver `(existing, replacement) -> existing` en `Collectors.toMap`, previniendo errores `IllegalStateException: Duplicate key` ante fallos simultáneos de validación en DTOs.
  * **Erradicación del Problema N+1 con `@EntityGraph` (P2):** Anotación `@EntityGraph(attributePaths = ...)` en [`ReservationRepository.java`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/main/java/com/hotel/domain/repository/ReservationRepository.java), [`RoomRepository.java`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/main/java/com/hotel/domain/repository/RoomRepository.java) y [`RoomOrderRepository.java`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-backend/src/main/java/com/hotel/domain/repository/RoomOrderRepository.java). Las consultas de listado de reservas cargan `Guest`, `Room` y `RoomType` en un único `LEFT OUTER JOIN` de SQL en lugar de más de 300 consultas individuales.
  * **Migración Flyway V10 con Índices de Alto Rendimiento:** Archivo `V10__add_performance_indexes.sql` que crea 10 índices en PostgreSQL sobre claves foráneas (`idx_rooms_room_type_id`, `idx_reservations_guest_id`, `idx_reservations_room_id`, `idx_room_orders_reservation_id`, `idx_room_order_items_order_id`, `idx_users_guest_id`) y consultas compuestas frecuentes de solapamiento (`idx_reservations_overlap`, `idx_room_orders_status_ordered`, `idx_room_orders_therapist_slot`).
* **Modularización Arquitectónica de "Mis Reservas" (P3):**
  * Descomposición integral de [`MyReservationsComponent`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/features/reservations/my-reservations/my-reservations.component.ts) en 3 componentes hijos independientes:
    * [`GuestVoucherModalComponent`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/features/reservations/my-reservations/components/guest-voucher-modal/guest-voucher-modal.component.ts): Voucher digital imprimible de alta resolución con códigos de barras y sellos de pago.
    * [`CancellationModalComponent`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/features/reservations/my-reservations/components/cancellation-modal/cancellation-modal.component.ts): Diálogo de confirmación con cálculo dinámico de política de 48h (reembolso íntegro vs retención) y comprobante de resultado.
    * [`GuestRoomServiceModalComponent`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/features/reservations/my-reservations/components/guest-room-service-modal/guest-room-service-modal.component.ts): Catálogo completo de gastronomía y agendamiento de citas de Spa con monitor de seguimiento de estatus en tiempo real.
  * Reducción de más de 560 líneas HTML y 1,000 líneas SCSS en el componente principal, mejorando radicalmente la legibilidad, mantenimiento y rendimiento de carga.
* **Pruebas de Seguridad en Tiempo Real:**
  * Intentos no autorizados a `/api/users` por parte de recepcionistas o clientes son rechazados con código HTTP `403 Forbidden`.
  * La creación de recepcionistas por parte del Administrador General se ejecuta de forma exitosa y permite el inicio de sesión inmediato con encriptación BCrypt.
  * Todas las transiciones de Front Desk (Check-In y Check-Out) actualizan la reserva y el estado de la suite de forma atómica y sincronizada.
  * Aislamiento de privacidad estricto: Las cuentas de personal y huéspedes tienen segregados sus accesos, evitando colisiones de sesión en la navegación pública.
* **Conector de Fechas y Noches de Estándar Hotelero de Lujo (Four Seasons / Ritz-Carlton):**
  * Estructura natural de 3 columnas (`[ Check-In ] —— [ Píldora 🌙 X Noches ] —— [ Check-Out ]`) con línea conectora continua que une visualmente la llegada con la salida.
  * Eliminación de choques visuales de `position: absolute`, garantizando legibilidad tipográfica jerárquica (*Jue, 29 Octubre 2026*) sin superposiciones en cualquier resolución de pantalla.
* **Humanización y Refinamiento de Roles:**
  * Eliminación de términos técnicos de base de datos en vistas de usuario (como `ROLE_CUSTOMER`), reemplazados por rótulos amigables y elegantes (`Huésped Registrado`).
  * Estética visual natural y serena, evitando gradientes artificiales saturados y conservando la sobriedad del lujo boutique.
* **Motor Antifraude de Reseñas Verificadas y Distribución Play Store:**
  * Implementación y verificación integral del flujo de calificación exclusivo para reservas culminadas (`COMPLETED`), garantizando una capa de protección doble (lógica de servicio Java + restricción relacional `UNIQUE(reservation_id)` en PostgreSQL).
  * Renderizado del componente de barras de progreso con filtrado interactivo por estrellas en la nueva vista de detalle de habitación `/rooms/:id`.
  * **Cálculo Dinámico e Independiente de Reseñas por Suite:** Erradicación de promedios globales o valores fijos hardcodeados en el catálogo. Cada suite calcula de forma autónoma su propio promedio de estrellas ponderado en base a sus reseñas reales; las suites de reciente incorporación sin reseñas registradas muestran con fidelidad el estado inicial de bienvenida sin distorsión de calificaciones.
* **Panel de Moderación y Réplicas Oficiales de Reseñas en PMS (Opción 1):**
  * Vista dedicada en Front Desk y Gerencia ([`ReviewModerationComponent`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/features/admin/dashboard/components/review-moderation/review-moderation.component.ts)) con 4 KPIs en tiempo real: Puntuación General del Hotel, Tasa de Satisfacción (% opiniones de 4★ y 5★), Pendientes de Réplica con alerta ámbar y Reseñas Atendidas.
  * Filtros dinámicos por calificación (Todas, Sin Respuesta, 5★ a 1★) y buscador reactivo de texto libre sobre comentarios y nombres de clientes.
  * Formulario integrado de réplica oficial con control de longitud (hasta 2,000 caracteres) y actualización optimista e inmediata del estado de la reseña en la UI.
  * Protección de endpoints en Spring Boot (`GET /api/reviews/admin`, `GET /api/reviews/admin/metrics`, `POST /api/reviews/{id}/reply`) para roles `ADMIN` y `RECEPTIONIST`.
* **Folio Fiscal Digital de Hospedaje y Factura Oficial en PDF (Opción 2):**
  * Componente universal reutilizable ([`HotelFolioModalComponent`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/shared/components/hotel-folio-modal/hotel-folio-modal.component.ts)) accesible tanto desde el PMS de recepción como desde el portal de autogestión "Mis Reservas" del huésped.
  * Formato A4 oficial con membrete corporativo de Hospentra Luxury Resort, RFC fiscal `HLS-980124-LUX`, folio correlativo `INV-HOSP-XXXXX-2026` y timbres de pago digitales (`PAGADO EN LÍNEA` / `LIQUIDACIÓN EN RECEPCIÓN`).
  * Desglose financiero exhaustivo: Noches de hospedaje con tarifa diaria, servicios complementarios consumidos durante la estadía (Room Service y citas de Wellness Spa vinculadas automáticamente), subtotal neto, IVA/impuesto del 16% y gran total consolidado.
  * Estilos de impresión `@media print` optimizados que suprimen barras de navegación, botones y fondos oscuros para generar un archivo PDF impecable o imprimir físicamente en un solo clic.
* **Módulo de Métricas Ejecutivas y Business Intelligence (Opción 3):**
  * Tablero ejecutivo de alta dirección ([`ExecutiveAnalyticsComponent`](file:///C:/Users/USUARIO/Music/Hospentra/reservation-frontend/src/app/features/admin/dashboard/components/executive-analytics/executive-analytics.component.ts)) restringido exclusivamente al rol `ADMIN` (`GET /api/admin/analytics`).
  * KPIs hoteleros clave calculados en backend: Facturación Bruta Consolidada ($ USD), ADR (Tarifa Promedio Diaria), RevPAR (Ingreso por Habitación Disponible) y Tasa de Ocupación con barra de progreso estilizada.
  * Gráfico de barras apiladas de tendencia mensual con desglose interactivo de Ingresos por Alojamiento vs. Ingresos por Extras (Gastronomía, Bar y Spa).
  * Distribución porcentual por unidad de negocio (Alojamiento, Cocina KDS, Cava de Vinos, Wellness Spa) y tabla de clasificación de rendimiento por tipo de suite con medallas de honor para las categorías más rentables.


### 2.20 Módulo de Gestión Hotelera Avanzada: Bloqueos de Mantenimiento Mayor (Out of Order / Out of Service)

#### Descripción y Objetivo Operativo
Permite al equipo directivo y de recepción inhabilitar temporalmente suites por averías críticas imprevistas (*Out of Order - OOO*) o por reformas y mantenimiento preventivo programado (*Out of Service - OOS*), impidiendo la creación de reservas que solapen el periodo y reflejando las franjas de inhabilitación en el Tape Chart Gantt interactivo de 14 días.

#### Componentes de Backend Implementados
* **Migración Flyway V18 (`V18__create_maintenance_blocks_table.sql`):**
  * Tabla `maintenance_blocks` con identificador, `room_id` (FK hacia `rooms`), `block_type` (`OUT_OF_ORDER`, `OUT_OF_SERVICE`), `start_date`, `end_date`, `reason`, `contractor_name`, `status` (`ACTIVE`, `RESOLVED`, `CANCELLED`), `created_by`, `created_at` y `resolved_at`.
  * Índices relacionales de alto rendimiento sobre `room_id`, rango de fechas `(start_date, end_date)` y `status`.
* **Entidades y Enums JPA:**
  * `BlockType`: `OUT_OF_ORDER`, `OUT_OF_SERVICE`.
  * `BlockStatus`: `ACTIVE`, `RESOLVED`, `CANCELLED`.
  * `MaintenanceBlock`: Modelo entidad persistente en `com.hotel.domain.model`.
* **Repositorio de Datos (`MaintenanceBlockRepository`):**
  * Método `existsOverlappingActiveBlock(roomId, startDate, endDate)` para validación atómica de solapamientos.
  * Consultas JPQL optimizadas `findActiveBlocksInDateRange`, `findAllWithRoom`, `findByRoomIdOrderByStartDateDesc` y `findByStatusOrderByStartDateDesc`.
* **Lógica de Servicio (`MaintenanceBlockService` / `MaintenanceBlockServiceImpl`):**
  * **Prevención de Conflictos:** Bloquea la creación si existen reservaciones activas (`PENDING`, `CONFIRMED`, `CHECKED_IN`) en las fechas solicitadas, o si ya existe otro bloqueo activo en dicho intervalo.
  * **Sincronización Automática de Estado:** Si el bloqueo inicia hoy o se encuentra vigente, el estado de la suite en `rooms` pasa a `MAINTENANCE`.
  * **Levantamiento y Anulación:** Al marcar `RESOLVE` o `CANCEL`, si no existen otros bloqueos activos ni reservas vigentes hoy, la suite retorna a `AVAILABLE`.
  * **Trazabilidad Inmutable:** Cada operación (`CREATE_MAINTENANCE_BLOCK`, `RESOLVE_MAINTENANCE_BLOCK`, `CANCEL_MAINTENANCE_BLOCK`) genera automáticamente un registro inmutable en `audit_logs` con IP, usuario y detalle.
  * **Notificación en Tiempo Real:** Emisión automática de evento SSE `ROOM_STATUS_CHANGED` para refrescar los PMS abiertos en recepción.
* **Integración con Motor de Reservas (`ReservationServiceImpl`):**
  * Inyección de `MaintenanceBlockRepository` en `ReservationServiceImpl.create()`. Cualquier intento de reserva que solape un bloqueo activo es rechazado inmediatamente con excepción descriptiva.
* **Controlador REST (`MaintenanceBlockController`):**
  * `POST /api/admin/maintenance-blocks`: Registro de nuevo bloqueo técnico (201 Created).
  * `GET /api/admin/maintenance-blocks`: Listado histórico de bloqueos.
  * `GET /api/admin/maintenance-blocks/active`: Consulta de bloqueos activos con filtros opcionales de fecha.
  * `PATCH /api/admin/maintenance-blocks/{id}/resolve`: Resolución técnica y liberación de la suite.
  * `PATCH /api/admin/maintenance-blocks/{id}/cancel`: Cancelación administrativa del bloqueo.
  * Seguridad: Restringido a roles `ADMIN` y `RECEPTIONIST` en `SecurityConfig`.

#### Componentes de Frontend Implementados
* **Modelos y Servicios Angular:**
  * `src/app/core/models/maintenance.model.ts`: Tipos `BlockType`, `BlockStatus`, interfaces `MaintenanceBlock` y `CreateMaintenanceBlockRequest`.
  * `src/app/core/services/maintenance.service.ts`: Métodos `createBlock()`, `getBlocks()`, `getActiveBlocks()`, `resolveBlock()` y `cancelBlock()`.
* **Modal de Gestión Integral (`MaintenanceModalComponent`):**
  * Pestaña "Bloqueos Registrados": Filtros por estado (Activos, Todos, Resueltos, Cancelados), tarjetas de diagnóstico técnico con badges OOO/OOS y botones para levantar o anular el bloqueo.
  * Pestaña "Nuevo Bloqueo": Selector de suite, selector de tipo (OOO vs OOS), selectores de fecha con cálculo automático de noches y validación estricta de coherencia temporal.
* **Tape Chart Gantt Interactivo (`TapeChartComponent`):**
  * Cálculo dinámico de franjas de bloqueo por suite en el cronograma de 14 días.
  * Renderizado con patrón rayado de seguridad de lujo (`repeating-linear-gradient` en tonos grafito/rubí profundo), icono técnico e información de duración.
  * Popover al hacer clic sobre una franja de mantenimiento con detalles del contratista y acceso directo a gestión.
  * Botón en barra de herramientas `[ Bloqueos OOO / OOS ]` para apertura inmediata del modal.
* **Room Rack (`RoomRackComponent`):**
  * Acceso rápido en la barra superior de KPIs para gestión de bloqueos técnicos.
  * Botón directo de bloqueo técnico en cada tarjeta de suite disponible para reporte inmediato de averías.

#### Resumen de Pruebas Automatizadas
* **Backend:** 182 pruebas unitarias e integración ejecutadas con éxito (100% pasando, 0 fallos).
  * Incluye 9 pruebas en `MaintenanceBlockServiceImplTest`, 6 pruebas en `MaintenanceBlockControllerTest` y prueba de rechazo de reservas por bloqueo en `ReservationServiceImplTest`.
* **Frontend:** 55 pruebas unitarias ejecutadas con éxito bajo Vitest (100% pasando, 0 fallos).
  * Incluye pruebas para `maintenance.service.spec.ts`, `maintenance-modal.component.spec.ts` y `tape-chart.component.spec.ts`.

---

### 2.21 Optimización UX/UI y Exportación Contable Excel/CSV para PMS y Perfil de Usuario

#### Descripción y Objetivo Operativo
Refinamiento de alta fidelidad estética y compatibilidad contable internacional para la auditoría de actividad del PMS, los reportes en hoja de cálculo y el panel de configuración de cuenta de huéspedes.

#### Mejoras Implementadas
1. **Auditoría PMS & Trazabilidad Visual:**
   * Corrección tipográfica en el icono del KPI de seguridad (`shield`) eliminando el solapamiento de glifos en navegadores WebKit/Blink.
   * Actualización del cuarto KPI a **Acciones Registradas** (`metrics.distinctOperators`) con icono representativo de taxonomía operativa.
   * Migración Flyway `V19__seed_baseline_audit_logs.sql` incorporando registros iniciales de alta fidelidad histórica para supervisión inmediata desde el primer arranque.
2. **Exportador Contable Compatible con Microsoft Excel:**
   * Incorporación del prefijo BOM UTF-8 junto con la directiva estándar `\uFEFFsep=;\r\n` y delimitación por punto y coma (`;`).
   * Desacople y humanización de enums técnicos a nomenclatura ejecutiva en español (`Pago en Recepción (Front Desk)`, `Tarjeta de Crédito / Débito`, `Liquidado / Pagado`, `En Casa (Check-In)`, `Estancia Completada`, `Recibida en Cocina`, `Entregada al Huésped`).
   * Apertura nativa automática en columnas individuales en Microsoft Excel para Windows sin necesidad del asistente manual de texto en columnas.
3. **Ergonomía en Configuración de Cuenta & Privacidad:**
   * Supresión completa de identificadores internos técnicos de base de datos (`ID: #`) en la vista del usuario final.
   * Transformación de roles técnicos (`ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_RECEPTIONIST`) a denominaciones elegantes y claras: **Huésped Registrado**, **Administrador General** y **Front Desk / Recepción**.
   * Reorganización de las tarjetas informativas superiores con transiciones sutiles, microinteracciones hover y eliminación de parpadeos de carga.

#### Validación y Cobertura de Pruebas
* **Backend:** 182 pruebas unitarias e integración ejecutadas con `BUILD SUCCESS` (0 errores, 0 fallos).
* **Frontend:** 55 pruebas unitarias ejecutadas con Vitest (100% exitosas) y compilación de producción validada sin advertencias (`ng build`).

---

### 2.22 Módulo de Pre-Check-In Digital & Fast-Pass Express

#### Descripción y Objetivo Operativo
Permite a los huéspedes registrar con antelación a su viaje su documentación de identidad oficial (Pasaporte, DNI, Licencia o Cédula), nacionalidad, rango estimado de llegada (*ETA*) y solicitudes personalizadas para su suite. En recepción física, el Front Desk identifica las reservas con insignia dorada de *Fast-Pass*, reduciendo el tiempo de entrega de llaves a menos de 10 segundos.

#### Componentes de Backend Implementados
* **Migración Flyway V20 (`V20__add_pre_check_in_fields.sql`):**
  * Incorporación de columnas `pre_check_in_completed`, `pre_check_in_at`, `document_type`, `document_number`, `nationality`, `estimated_arrival_time` y `special_requests` a la tabla `reservations`.
* **Entidad & DTOs:**
  * `PreCheckInRequest`: Validación de campos con Bean Validation (`@NotBlank`, `@Size`).
  * `ReservationResponse`: Enriquecimiento del contrato de respuesta con los 7 campos de Pre-Check-In.
* **Lógica de Servicio (`ReservationServiceImpl.submitPreCheckIn`):**
  * Restricción de acceso: Solo el titular de la reserva (`ROLE_CUSTOMER`) o el personal (`ROLE_ADMIN`, `ROLE_RECEPTIONIST`) pueden registrar el Pre-Check-In.
  * Validación de estado: Exclusión atómica de reservas canceladas o completadas.
  * Trazabilidad: Emisión de evento SSE `PRE_CHECK_IN_COMPLETED` al canal `FRONT_DESK` y registro inmutable en `audit_logs`.
  * Notificación automática: Creación de notificación in-app para el huésped confirmando la activación de su Fast-Pass.
* **Controlador REST:**
  * Endpoint `PATCH /api/reservations/{id}/pre-check-in`.

#### Componentes de Frontend Implementados
* **Mis Reservaciones (`MyReservationsComponent`):**
  * Botón interactivo `[ Pre-Check-In (Fast Pass) ]` para reservas activas no tramitadas.
  * Modal digital con diseño de lujo Sanctuary (620px de ancho, radio de 18px y cabecera en gradiente azul pizarra oscuro `#0F172A` a `#1E293B`).
  * Formulario ergonómico con etiquetas estructuradas con `white-space: nowrap` y asterisco obligatorio `.req` en dorado pulido (`#C59B27`) sin saltos de línea huérfanos.
  * Controles de entrada unificados a 46px de altura, fondo `#F8FAFC`, chevron SVG minimalista personalizado para menús desplegables y anillos de enfoque en dorado.
  * Insignia `Fast-Pass Activado (ETA: XX:XX)` tras confirmación exitosa con actualización reactiva instantánea.
* **PMS Front Desk (`AdminDashboardComponent`):**
  * Distintivo `Fast-Pass • ETA XX:XX` con tooltip detallado en la tabla de operaciones para validación instantánea del recepcionista.

---

### 2.23 Centro de Notificaciones en Tiempo Real (Guest Bell Hub & SSE Push)

#### Descripción y Objetivo Operativo
Centro de notificaciones interactivo integrado en la cabecera principal (`NavbarComponent`) que mantiene informados a huéspedes y operadores sobre el ciclo de vida de su estancia: confirmación de reserva, activación de Fast-Pass, check-in completado y cambios de estado de comandas de Room Service en cocina.

#### Componentes de Backend Implementados
* **Migración Flyway V21 (`V21__create_notifications_table.sql`):**
  * Tabla `notifications` con clave foránea hacia `users`, tipo de evento, estado de lectura y fecha de creación con índices optimizados.
* **Modelo & Repositorio (`Notification`, `NotificationRepository`):**
  * Consultas paginadas por usuario y conteo de no leídas en $O(1)$.
  * Consulta de actualización masiva `markAllAsReadByUserId`.
* **Servicio In-App (`InAppNotificationServiceImpl`):**
  * Métodos `getMyNotifications`, `getUnreadCount`, `markAsRead`, `markAllAsRead` y `createNotification`.
  * Despacho dual: Persistencia transaccional en base de datos y push inmediato al canal del usuario vía `SseNotificationService.sendToUser`.
* **Disparadores de Negocio Integrados:**
  * Al crear reserva $\rightarrow$ Notificación de reserva confirmada.
  * Al registrar Pre-Check-In $\rightarrow$ Notificación de Fast-Pass activado.
  * Al hacer Check-In $\rightarrow$ Notificación de bienvenida y suite lista.
  * Al actualizar comanda en KDS (`RoomOrderServiceImpl`) $\rightarrow$ Notificación al huésped sobre su pedido (en preparación, en camino, entregado).
* **Controlador REST:**
  * `GET /api/notifications`
  * `GET /api/notifications/unread-count`
  * `PATCH /api/notifications/{id}/read`
  * `PATCH /api/notifications/read-all`

#### Componentes de Frontend Implementados
* **Servicio Reactivo (`InAppNotificationService`):**
  * Manejo de señales `notifications`, `unreadCount` y `activeToast`.
  * Escucha en vivo de eventos SSE con actualización reactiva sin recargar la página.
* **Componente Navbar (`NavbarComponent`):**
  * Campana de notificaciones con insignia de conteo de pendientes (badge dinámico).
  * Menú desplegable con listado cronológico de eventos, iconos contextuales por categoría, indicador de no leído y botón de marcado masivo.
  * Desacoplamiento del Toast Global: Posicionamiento flotante fuera del elemento con `backdrop-filter` para evitar confinamientos visuales en el header (`top: 5.75rem; right: 1.5rem`).
  * Prevención de colisiones: Supresión de toasts locales duplicados para canalizar avisos mediante un único banner global unificado con temporizador de 5.5s y descarte manual.

---

### 2.24 Optimización Ergonómica & Responsividad de Tablas PMS (Front Desk Operations)

#### Descripción y Objetivo Operativo
Reingeniería de la tabla central de operaciones de Front Desk en el PMS para garantizar densidad visual equilibrada, eliminación de saltos de línea inadecuados en códigos y etiquetas, y prevención del truncamiento o corte de botones de acción en cualquier resolución de pantalla.

#### Ajustes y Optimizaciones Técnicas
* **Contenedor Desplazable (`.table-wrap`):**
  * Incorporación de `overflow-x: auto` y `-webkit-overflow-scrolling: touch` en el contenedor de tabla.
  * Barra de desplazamiento delgada de 6px personalizada con pista `#F8FAFC` y tirador suave `#CBD5E1`, impidiendo que `.operations-panel` recorte columnas por su propiedad `overflow: hidden`.
* **Recuperación y Calibración de Espacio Horizontal:**
  * Reducción calibrada del relleno de celda: `0.65rem 0.45rem` en cabeceras y `0.75rem 0.45rem` en celdas de datos (con sangrado inicial y final de `0.875rem`).
  * Ganancia neta de más de 220px de espacio libre horizontal a lo largo de las 9 columnas de la tabla.
* **Calibración de Columnas y Prevención de Desbordamiento:**
  * **Columna de Código:** Ajustada a `min-width: 105px; width: 105px;` con `white-space: nowrap`, tipografía monoespaciada compacta (`0.75rem`) en píldora gris `#F1F5F9`, eliminando saltos de renglón en guiones (ej. `HOSP-20-2026`).
  * **Columna de Huésped:** Calibrada a `min-width: 155px`, manteniendo en una sola línea la pastilla `Fast-Pass • ETA XX:XX`.
  * **Columna de Acciones Operativas:** Ajustada a `width: 150px; min-width: 150px; max-width: 150px;` con botones ergonómicos de altura uniforme (`primary-action` de 26px y `secondary-actions-row` de 24px para *Voucher*, *Folio* y *Cancelar*), eliminando el corte o truncamiento de texto en pantallas convencionales (1280px / 1366px).
  * **Ancho Global Nativo:** Fijado en `min-width: 960px; width: 100%;`, asegurando visualización completa al 100% sin scroll en escritorio y navegación fluida garantizada en tablets.

---

### 2.25 Rediseño Integral de la Landing Page (Experiencia Editorial & Conversión Boutique)

#### Descripción y Objetivo Operativo
Transformación de la página de inicio (`HomeComponent`) desde un catálogo plano hacia una experiencia inmersiva de ultra lujo. La nueva arquitectura combina narrativa de marca, apetito sensorial por la gastronomía del resort, divulgación de la tecnología Fast-Pass y resolución de fricciones previas a la reserva mediante un acordeón de preguntas frecuentes interactivo y un banner VIP de conversión.

#### Nuevos Bloques Arquitectónicos Implementados
1. **Hero & Buscador Rápido de Disponibilidad:** Conservación del buscador por fechas y huéspedes con cálculo dinámico y garantías de reserva.
2. **Filosofía & Esencia del Santuario (`#filosofia`):**
   * Composición asimétrica en dos columnas: a la izquierda, fotografía editorial de la arquitectura del resort con tarjeta flotante de galardón (*Premio Excelencia Boutique 2026 - World Luxury Sanctuary Awards*).
   * A la derecha, tres pilares conceptuales con iconos dorados: *Diseño Biofílico & Privacidad Total*, *Mayordomía Personal 24/7* y *Sustentabilidad Consciente*.
   * Franja de métricas de prestigio: 98% de satisfacción, servicio 24/7 y más de 1,200 estancias registradas.
3. **Catálogo Boutique de Suites & Tarifas (`#catalogo`):** Filtros dinámicos por tipología, ordenamiento por precio o capacidad y tarjetas detalladas de suites.
4. **Gastronomía de Autor & Room Service en Suite (`#gastronomia`):**
   * Showcase de 3 experiencias culinarias con fotografía en alta resolución: *Desayuno Gourmet Flotante*, *Cena Tasting de 5 Tiempos en Terraza* y *Coctelería Botánica & Cava*.
   * Pastilla de llamado a la acción destacando la activación automática de la carta digital de Room Service tras el check-in.
5. **Tecnología Fast-Pass: Llegada sin Esperas (`#fastpass`):**
   * Sección en contraste oscuro (`#0F172A`) explicando el flujo en 3 pasos: *01. Reserve Online*, *02. Active su Pre-Check-In* y *03. Llaves en 10 Segundos*.
   * Sello de seguridad garantizando sincronización en tiempo real con el PMS de recepción.
6. **Santuario de Bienestar & Spa (`#bienestar`):** Cuadrícula de 4 experiencias holísticas (Circuito Hidrotermal de agua salada, Masajes terapéuticos, Yoga zen al amanecer y Piscina Infinity climatizada).
7. **Preguntas Frecuentes con Acordeón Reactivo (`#faq`):**
   * Acordeón controlado por Angular Signals (`openFaqIndex`, `toggleFaq`) resolviendo dudas clave sobre cancelación flexible (48h), pago dual, horarios de check-in, desayuno incluido y room service.
8. **Banner VIP de Conversión de Reserva Directa:**
   * Contenedor con gradiente profundo y borde dorado fino (`#C59B27`) ofreciendo incentivo de mejor tarifa y crédito de $50 USD.
   * Botón con desplazamiento suave hacia el catálogo (`scrollToCatalog()`) y enlace de contacto con concierge.

---

### 2.26 Módulo de Concierge en Vivo & Mensajería de Suites (Experiencia en Estancia)

#### Descripción y Objetivo Operativo
Canal interactivo de comunicación bidireccional y en tiempo real entre los huéspedes y el mostrador de Front Desk / Recepción (`ROLE_RECEPTIONIST` y `ROLE_ADMIN`). Permite resolver requerimientos de estancia (toallas extras, amenities, late check-out, citas de spa y consultas gastronómicas) de manera ágil y digital, elevando la experiencia de hospitalidad hacia el estándar de ultra lujo.

#### Arquitectura Fullstack del Sistema
1. **Modelo de Datos & Persistencia (Migración V22):**
   * Tabla `concierge_messages` con columnas: `id`, `sender_id`, `recipient_id`, `reservation_id`, `suite_number`, `message`, `read_status`, `sender_role` y `created_at`.
   * Llaves foráneas con eliminación restrictiva e índices optimizados en emisor, receptor y estado de lectura para consultas de baja latencia.
2. **Capa de Negocio y Transmisión SSE en Tiempo Real:**
   * Entidad JPA `ConciergeMessage` y repositorio `ConciergeMessageRepository` con consultas agrupadas por huésped y conteo de no leídos.
   * `ConciergeChatService` y `ConciergeChatServiceImpl`:
     * Cuando el huésped envía un mensaje, se difunde el evento SSE `CONCIERGE_MESSAGE` a los roles operativos `ROLE_RECEPTIONIST` y `ROLE_ADMIN`, registrándose en la bitácora de auditoría inmutable (`auditLogService.logAction`).
     * Cuando recepción responde, el evento SSE se enruta de forma focalizada al usuario titular de la suite (`sendToUser`).
3. **Endpoints REST API (`/api/concierge`):**
   * `GET /api/concierge/status`: Consulta el estado de estancia activa del huésped (`activeStay: boolean`, `suiteNumber`, `reservationId`, `guestName`).
   * `POST /api/concierge/messages`: Envío de mensaje validado (`@Valid`, máx. 2,000 caracteres), verificando estancia activa para clientes.
   * `GET /api/concierge/my-messages`: Historial del hilo de conversación acotado a la estancia activa (`reservation_id`).
   * `GET /api/concierge/conversations`: Resumen de suites actualmente en casa (`CHECKED_IN`) con badge de no leídos y estado de asignación (`claimedBy`).
   * `GET /api/concierge/conversations/{guestUserId}`: Hilo detallado de la suite para recepción.
   * `PATCH /api/concierge/conversations/{guestUserId}/claim`: Asignación atómica de atención a recepcionista con difusión SSE `CONCIERGE_CLAIM`.
   * `PATCH /api/concierge/conversations/{guestUserId}/read`: Actualización atómica de mensajes leídos.
   * `GET /api/concierge/unread-count`: Conteo dinámico de mensajes pendientes de lectura.
4. **Reglas de Negocio Estrictas de Acceso (In-House Only):**
   * Exclusividad para huéspedes con estancia activa: Sólo usuarios autenticados con rol `ROLE_CUSTOMER` que cuenten con una reserva en estado `CHECKED_IN` tienen acceso al servicio de Concierge.
   * Usuarios sin reserva, visitantes sin cuenta, reservas futuras en estado `CONFIRMED` o reservas pasadas en `COMPLETED` son bloqueadas a nivel de backend con `AccessDeniedException` y el widget FAB permanece oculto en frontend.
5. **Ciclo de Vida Efímero Acotado a la Estancia (Stay-Bound Scoping):**
   * Los mensajes se vinculan obligatoriamente a la reserva activa de la estancia en curso.
   * Al registrarse el Check-Out (`COMPLETED`), la conversación se archiva y se remueve de forma automática e inmediata de la bandeja activa de recepción.
6. **Concurrencia Multi-Recepcionista con Asignación Dinámica ("Atendido por"):**
   * Bandeja de entrada unificada de recepción donde las suites en-house exhiben su estado operativo: `[🟢 Atendido por: Nombre]` o `[🟡 Pendiente]`.
   * Al pulsar el botón "Atender Suite" o responder a un huésped, se actualiza el mapa concurrente de asignación y se emite el evento SSE `CONCIERGE_CLAIM`, sincronizando instantáneamente los tableros de todo el equipo de recepción para evitar colisiones operativas y permitiendo reasignaciones de guardia.
7. **Widget Flotante del Huésped (`GuestConciergeWidgetComponent`):**
   * Botón flotante FAB en esquina inferior derecha (`z-index: 9998`) condicionado a `isCustomer() && chatService.hasActiveStay()`.
   * Encabezado con número de Suite verificado en tiempo real (`Suite XX • Front Desk`).
   * Selector de chips rápidos corporativos y flujo de burbujas en tiempo real.
8. **Pestaña Operativa de Concierge en PMS Admin (`AdminDashboardComponent`):**
   * Vista dividida: lista de suites con indicador de asignación y contador de no leídos; visor de hilo con badge de suite, estado de recepcionista a cargo, botón interactivo "Atender Suite / Reasignarme", respuestas preconfiguradas y caja de respuesta instantánea.

---

### Módulo de Check-Out y Liquidación Contable de Folio (Fases 1 y 2)

#### 1. Fase 1: Cierre y Liquidación de Folio en Mostrador de Recepción (PMS)
* **Persistencia Contable (Migración Flyway V23):**
  * Se añadieron las columnas `settlement_method VARCHAR(50)`, `settlement_amount NUMERIC(10,2)`, `settlement_notes VARCHAR(500)` y `settled_at TIMESTAMP` a la tabla `reservations`.
  * Se añadió la columna `paid BOOLEAN DEFAULT false NOT NULL` a la tabla `room_orders`.
* **Cálculo Consolidado Dinámico:**
  * Estancia base (noches de habitación) + Consumos adicionales cargados a la habitación (`room_orders`).
  * Deducción automática del prepago online si la reserva se abonó previamente con tarjeta bancaria.
* **Opciones de Cobro en Mostrador:**
  * `POS_TERMINAL`: Registro de cobro físico por terminal datáfono con código de aprobación bancaria (`authCode`).
  * `CASH`: Cobro en efectivo con calculadora de cambio y vuelto inmediato.
  * `CARD_ON_FILE`: Cargo directo a la tarjeta guardada en el perfil de la reserva (`cardLast4`).
* **Acciones Atómicas de Cierre:**
  * Se marcan todas las comandas de la habitación como `paid = true`.
  * El estado de la reserva transiciona a `COMPLETED` y `paymentStatus = PAID`.
  * La habitación transiciona automáticamente a `MAINTENANCE` (En Limpieza para camaristas).
  * Emisión SSE en tiempo real de `RESERVATION_STATUS_CHANGED` y `ROOM_STATUS_CHANGED`.
  * Registro de auditoría inmutable (`auditLogService.logAction`) con acción `CHECK_OUT`.

#### 2. Fase 2: Check-Out Express Digital en el Portal del Huésped
* **Acceso y Visibilidad:**
  * Disponible exclusivamente para huéspedes con reserva activa en casa (`CHECKED_IN`) desde su panel de *Mis Reservas*.
* **Revisión Desglosada del Folio:**
  * Desglose ítem por ítem de cada pedido de room service o spa (CMD-X) cargado a la suite.
  * Cálculo transparente del saldo neto pendiente.
* **Liquidación Digital:**
  * Opción de liquidar con la tarjeta guardada en la reserva o con una nueva tarjeta bancaria.
  * Verificación obligatoria de entrega de llaves físicas en el escritorio de la suite.
* **Endpoint Transaccional (`POST /api/reservations/{id}/express-checkout`):**
  * Validación de propiedad de la reserva para el usuario autenticado.
  * Transición a `COMPLETED`, marcado de comandas como pagadas, suite enviada a `MAINTENANCE` y emisión SSE instantánea al rack de habitaciones de recepción.
  * Recibo digital de liquidación con código de autorización y descarga de folio consolidado.

---

#### Resumen Global de Pruebas Automatizadas
* **Backend:** 226 pruebas unitarias e integración ejecutadas con `BUILD SUCCESS` (0 errores, 0 fallos).
* **Frontend:** 76 pruebas unitarias ejecutadas bajo Vitest (100% pasando, 0 fallos).
* **Total:** 302 pruebas automatizadas blindando el sistema. Compilación de producción (`ng build`) completada con 0 errores.

---

### 10. Arquitectura de Despliegue y Orquestación con Docker

La plataforma cuenta con un entorno contenerizado de nivel producción, orquestado con Docker Compose y desacoplado mediante una red bridge privada (`hospentra_default`).

#### 10.1 Servicios Contenerizados

1. **`hospentra-db` (PostgreSQL 17 Alpine):**
   * Persistencia montada sobre el volumen nominal `hospentra_postgres_data` (`/var/lib/postgresql/data`).
   * Mecanismo de healthcheck nativo con `pg_isready` cada 5 segundos para arranque coordinado.
   * Puerto expuesto al host en `5433:5432` para evitar colisiones con bases de datos locales en el puerto estándar 5432.

2. **`hospentra-backend` (Spring Boot 4 / Java 25):**
   * Multi-stage build con Eclipse Temurin JDK 25 Alpine para compilación Maven y JRE 25 Alpine para ejecución reducida a menos de ~180MB.
   * `depends_on` con condición `service_healthy` hacia la base de datos, garantizando que Flyway ejecute las migraciones únicamente tras verificar la conectividad del motor SQL.
   * Volumen persistente `hospentra_uploads_data` (`/app/uploads`) para resguardo de imágenes subidas por el personal del hotel.
   * Exposición del puerto 8080 para acceso directo o mediante proxy.

3. **`hospentra-frontend` (Angular 22 + Nginx Alpine):**
   * Multi-stage build con Node 22 Alpine para `npm ci` y compilación optimizada (`ng build`), y Nginx 1.27 Alpine como servidor web de producción y reverse proxy.
   * Compresión dinámica Gzip para HTML, JS, CSS, JSON y SVG.
   * Enrutamiento SPA con directiva `try_files $uri $uri/ /index.html` para soporte íntegro de rutas profundas de Angular.
   * Proxy inverso inteligente hacia `http://backend:8080/api/` con soporte explícito para SSE y WebSockets (`proxy_buffering off; proxy_read_timeout 3600s;`).
   * Proxy inverso `/uploads/` para servir multimedia almacenada en el backend.

#### 10.2 Guía de Despliegue Rápido con Docker Compose

Para levantar el ecosistema completo en cualquier equipo con Docker Desktop o Docker Engine:

Paso 1: Crear el archivo de variables locales a partir de la plantilla
```powershell
copy .env.example .env
```

Paso 2: Compilar imágenes multi-etapa y levantar contenedores en segundo plano
```powershell
docker compose up -d --build
```

Paso 3: Verificar estado de los servicios
```powershell
docker compose ps
```

#### 10.3 Puertos y Puntos de Acceso

| Servicio | URL / Puerto | Descripción |
| :--- | :--- | :--- |
| **Frontend Web** | `http://localhost` (puerto 80) | Portal boutique del huésped, catálogo de suites y reservas |
| **PMS Admin Panel** | `http://localhost/admin/login` | Mostrador de recepción, rack de habitaciones y liquidación de folios |
| **Kitchen Display (KDS)** | `http://localhost/kitchen/orders` | Tablero de comandas de gastronomía y spa en tiempo real |
| **Backend REST API** | `http://localhost:8080/api` | API REST Spring Boot, endpoints de seguridad y streaming SSE |
| **PostgreSQL Host Port** | `localhost:5433` | Puerto expuesto para herramientas de administración (DBeaver, pgAdmin) |

#### 10.4 Credenciales Preconfiguradas (Seed Data)

| Rol | Usuario | Contraseña | Perfil y Permisos |
| :--- | :--- | :--- | :--- |
| **Super Administrador** | `admin` | `admin123` | Gestión total de reservas, tarifas, camaristas, auditoría y concierge |
| **Personal de Cocina** | `cocina` | `cocina123` | Despacho y actualización de comandas de Room Service |
| **Huésped Demo 1** | `pepetest` | `Password123!` | Cuenta verificada de huésped con reserva en-house |
| **Huésped Demo 2** | `sofia_valdez` | `Password123!` | Historial de reservas y reseñas de suites |


