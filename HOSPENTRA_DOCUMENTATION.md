# Hospentra Luxury Sanctuary — Documentación Técnica, Funcional y Casos de Uso del Sistema

Documentación integral y oficial del ecosistema fullstack de **Hospentra**, plataforma hotelera boutique y sistema integral de gestión de propiedades (PMS - Property Management System) con arquitectura desacoplada, control de acceso basado en roles (RBAC) y pasarela de pago dual.

---

## 1. Ficha Técnica y Arquitectura del Sistema

### 1.1 Stack Tecnológico
* **Backend:** Java 25, Spring Boot 3.x, Spring Data JPA / Hibernate, Spring Security 6, JWT (HMAC-SHA384), Flyway Migrations, RFC 7807 ProblemDetail.
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

### 2.2 Pasarela de Pago Dual en el Checkout
El proceso de reserva incorpora una pasarela inteligente con dos modalidades de liquidación:

#### Modalidad A: Pago Online con Tarjeta (Visa / Mastercard / Amex)
* Tarjeta interactiva con renderizado dinámico de chip EMV y logotipo de red según los primeros dígitos:
  * Prefijo 4: Visa
  * Prefijo 5: Mastercard
  * Prefijo 34 o 37: American Express
* Máscara automática de 16 dígitos (`XXXX XXXX XXXX XXXX`), fecha de caducidad (`MM/AA`), código de seguridad CVV (3 o 4 dígitos) y nombre del titular.
* Simulación de autorización bancaria con verificación de fondos, latencia realista (1.4s) y generación de código de autorización (`AUTH-XXXXXX`).
* Emisión inmediata de comprobante con el sello **"PAGADA EN LÍNEA (Comprobante Liquidado)"**, saldo pendiente de `$0.00 USD` y registro de los últimos 4 dígitos (`•••• XXXX`).

#### Modalidad B: Pago en Recepción (Front Desk)
* Confirmación inmediata de la suite sin debitar fondos de la tarjeta del huésped.
* Registro de la reserva con estado de pago `PENDING` y método `AT_RECEPTION`.
* Emisión de comprobante con el sello **"LIQUIDACIÓN AL CHECK-IN (Cobro en Front Desk)"**, indicando el importe exacto a liquidar al recibir las llaves en recepción.

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

#### C. Room Rack & Ciclo de Limpieza (Housekeeping)
* Cuadrícula de tarjetas representativas de cada suite del hotel con código de habitación, categoría y estado operativo:
  * `AVAILABLE` (Lista / Disponible - borde verde y dot esmeralda).
  * `OCCUPIED` (Huésped en Casa - borde ámbar y dot dorado).
  * `MAINTENANCE` (En Limpieza - borde azul y dot índigo).
* **Botón de Acción Housekeeping:** En suites marcadas en limpieza, botón *"Marcar Limpia y Lista"* para certificar el reacondicionamiento y devolver la habitación a inventario disponible de inmediato.

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

#### G. Monitor de Comandas de Room Service en Front Desk
* Panel dedicado en el Control Center para supervisar en vivo los pedidos realizados por huéspedes en casa.
* Tarjetas KPI de comandas: Total de comandas registradas, comandas en preparación/despacho y comandas entregadas a suite.
* Tabla de órdenes con código `CMD-XX`, suite destino, nombre del huésped, artículos solicitados con cantidades y precios, instrucciones culinarias especiales y badge de estado de cocina.
* Botón de acceso directo para abrir la Terminal KDS de Cocina en pestaña independiente.

#### H. Voucher Oficial de Recepción y Folio de Consumos
* Visor de comprobante corporativo en alta resolución con monograma de Hospentra y sellos digitales oficiales:
  * Sello verde esmeralda: **"PAGADA EN LÍNEA"** con código de autorización bancaria.
  * Sello dorado ámbar: **"LIQUIDACIÓN AL CHECK-IN"** con instrucción de cobro en Front Desk.
* Desglose de consumos extras de habitación cuando existen comandas de Room Service asociadas a la estancia.
* Botón de impresión directa con estilos CSS `@media print` optimizados para impresora de mostrador o exportación PDF.

### 2.5 Terminal Digital de Cocina KDS (`/kitchen/orders`)
Terminal de pantalla completa diseñada específicamente para la brigada de cocina y bar:
* **Autenticación Directa de Estación:** Inicio de sesión con credenciales de cocina (`ROLE_KITCHEN` o personal asignado) redirigiendo automáticamente a `/kitchen/orders`.
* **Tablero Kanban Digital de Tres Etapas:**
  1. **Comandas Recibidas (`RECEIVED`):** Pedidos entrantes de suites con sonido visual, código `CMD-XX`, número de suite e instrucciones de preparación. Botón de acción: *"Iniciar Elaboración"*.
  2. **En Preparación (`PREPARING`):** Comandas actualmente en cocina con temporizador transcurrido en vivo (minutos y segundos) y alertas visuales según tiempo de espera. Botón de acción: *"Marcar Lista para Despacho"*.
  3. **Listas para Entrega (`READY_FOR_DELIVERY`):** Pedidos listos en pase para que el room runner o camarero suba a la suite. Botón de acción: *"Confirmar Entrega en Suite"*.
* **Historial de Comandas Entregadas:** Pestaña de auditoría con registro de hora exacta de despacho y entrega.
* **Sincronización Automática:** Polling y reactividad para actualizar la pantalla sin necesidad de recargar la página.

### 2.6 Carta Boutique de Room Service en Suite (`/my-reservations`)
Módulo de hospitalidad digital para huéspedes en casa:
* **Desbloqueo Inteligente:** La carta boutique permanece oculta hasta que el recepcionista formaliza el Check-In de la reserva (`CHECKED_IN`).
* **Catálogo de Lujo en Tres Categorías:**
  * **Gastronomía de Autor:** Solomillo al Malbec, Tabla Ibérica de Bellota, Desayuno Imperial a la Suite.
  * **Bar & Vinos Finos:** Moët & Chandon Brut Impérial, Gran Reserva Privada, Cócteles de Autor.
  * **Bienestar & Spa en Suite:** Masaje relajante en suite, Baño de inmersión con aromaterapia botánica.
* **Carrito de Pedidos Interactivo:** Selección de ítems, selector de cantidades, cálculo instantáneo de subtotal y caja de texto para instrucciones especiales al chef (alergias, punto de cocción, etc.).
* **Carga al Folio de la Suite:** El pedido no requiere tarjeta inmediata; se carga de forma transparente al folio consolidado de la suite para liquidarse al Check-Out.
* **Línea de Tiempo del Pedido en Suite:** El huésped visualiza en su portal el estado de su pedido en tiempo real (*Recibido en Cocina*, *En Preparación*, *En Camino* o *Entregado*).

---

## 3. Catálogo de Casos de Uso (Use Cases)

A continuación se detallan los casos de uso fundamentales que abarcan el 100% de los flujos del sistema:

---

### Caso de Uso CU-01: Registro e Inicio de Sesión de Huésped
* **Actor Principal:** Huésped / Cliente (`ROLE_CUSTOMER`).
* **Precondición:** El usuario se encuentra en el portal público de Hospentra.
* **Flujo Principal de Registro:**
  1. El cliente hace clic en "Registrarse" en la barra de navegación o modal de bienvenida.
  2. El sistema despliega el formulario solicitando: Nombre de usuario, Correo electrónico, Nombre, Apellido, Teléfono y Contraseña.
  3. El cliente ingresa los datos y presiona "Crear Cuenta".
  4. El sistema valida el formato de correo y que el usuario/correo no existan en base de datos.
  5. El backend registra la cuenta con rol `ROLE_CUSTOMER`, encripta la contraseña con BCrypt y crea el perfil asociado en la entidad `Guest`.
  6. El sistema genera el token JWT y autentica al cliente automáticamente.
* **Flujo Principal de Inicio de Sesión:**
  1. El cliente ingresa su usuario y contraseña en el modal de inicio de sesión.
  2. El sistema valida credenciales contra `/api/auth/login`.
  3. El sistema almacena el token JWT y actualiza el estado reactivo del usuario en el Navbar.
* **Flujo Alternativo (Credenciales inválidas):**
  * El sistema muestra alerta roja indicando error en credenciales sin interrumpir la navegación.

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

### Caso de Uso CU-05: Creación y Auditoría de Credenciales de Personal
* **Actor Principal:** Administrador General (`ROLE_ADMIN`).
* **Precondición:** Sesión iniciada con rol de administrador en `/admin/dashboard`.
* **Flujo Principal:**
  1. El administrador hace clic en la pestaña "Gestión de Personal & Recepcionistas".
  2. El sistema consulta `GET /api/users` y despliega el directorio de personal con las cuentas activas.
  3. El administrador presiona "Registrar Nueva Recepcionista".
  4. Se abre el modal de registro donde se ingresa: Usuario corporativo, correo electrónico, contraseña temporal (mínimo 6 caracteres) y confirmación.
  5. El administrador hace clic en "Crear y Habilitar Acceso".
  6. El sistema envía la petición a `POST /api/users` con rol `ROLE_RECEPTIONIST`.
  7. El backend valida unicidad de usuario y correo, encripta la contraseña con BCrypt y guarda el usuario.
  8. El sistema muestra mensaje de confirmación y añade a la recepcionista a la tabla en tiempo real.
* **Flujo Alternativo:**
  * Si el usuario o correo ya existen, el backend responde con error 409 Conflict y el formulario muestra el mensaje específico de duplicado.

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

## 4. Matriz de Endpoints de la API REST

| Método | Endpoint | Rol Requerido | Descripción |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/auth/register` | Público | Registro de nuevos huéspedes (`ROLE_CUSTOMER`) |
| **POST** | `/api/auth/login` | Público | Autenticación y generación de JWT para todos los roles |
| **GET** | `/api/room-types` | Público | Catálogo de tipos de habitación boutique |
| **GET** | `/api/rooms` | Público / Auth | Listado de habitaciones y estados de ocupación |
| **GET** | `/api/rooms/{id}` | Público / Auth | Detalle de habitación específica |
| **PATCH**| `/api/rooms/{id}/status`| `ADMIN`, `RECEPTIONIST` | Actualizar estado de suite (`AVAILABLE`, `MAINTENANCE`) |
| **POST** | `/api/rooms` | `ROLE_ADMIN` | Alta de nuevas habitaciones en catálogo |
| **PUT** | `/api/rooms/{id}` | `ROLE_ADMIN` | Modificación de datos de habitación |
| **DELETE**| `/api/rooms/{id}`| `ROLE_ADMIN` | Baja de habitación del inventario |
| **GET** | `/api/reservations` | Autenticado | Listado de reservas (filtrado por usuario o global para staff) |
| **POST** | `/api/reservations` | Autenticado | Creación de reserva con modalidad de pago |
| **GET** | `/api/reservations/{id}` | Autenticado | Detalle de una reserva específica |
| **PATCH**| `/api/reservations/{id}/confirm` | `ADMIN`, `RECEPTIONIST` | Confirmar reserva pendiente |
| **PATCH**| `/api/reservations/{id}/check-in` | `ADMIN`, `RECEPTIONIST` | Registrar check-in y entrega de llaves |
| **PATCH**| `/api/reservations/{id}/complete` | `ADMIN`, `RECEPTIONIST` | Registrar check-out y enviar suite a limpieza |
| **PATCH**| `/api/reservations/{id}/cancel` | Autenticado | Cancelar reserva aplicando política de reembolso |
| **GET** | `/api/guests` | `ADMIN`, `RECEPTIONIST` | Directorio de huéspedes y perfiles de contacto |
| **GET** | `/api/users` | `ROLE_ADMIN` | Directorio de cuentas corporativas con turnos y presencia |
| **POST** | `/api/users` | `ROLE_ADMIN` | Creación de cuentas de staff (`RECEPTIONIST`, `KITCHEN`, `ADMIN`) con asignación de turno |
| **PATCH**| `/api/users/{id}/work-status` | `ADMIN`, `RECEPTIONIST`, `KITCHEN` | Alternar estado de guardia y refrigerio (`ON_DUTY`, `ON_BREAK`) |
| **GET** | `/api/extra-services` | Público | Catálogo boutique de servicios de Room Service y Bar |
| **GET** | `/api/extra-services/category/{category}` | Público | Filtrar servicios por categoría (`GASTRONOMY`, `BAR_WINE`, `WELLNESS_SPA`) |
| **POST** | `/api/room-orders` | Autenticado | Crear comanda de Room Service vinculada a suite activa |
| **GET** | `/api/room-orders` | Autenticado | Historial general de comandas para administración y front desk |
| **GET** | `/api/room-orders/active` | `ADMIN`, `RECEPTIONIST`, `KITCHEN` | Comandas activas para la terminal KDS de cocina |
| **GET** | `/api/room-orders/reservation/{id}` | Autenticado | Comandas de Room Service asociadas a una reserva (Folio) |
| **PATCH**| `/api/room-orders/{id}/status` | `ADMIN`, `RECEPTIONIST`, `KITCHEN` | Actualizar estado de cocina (`RECEIVED`, `PREPARING`, `READY_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`) |

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

---

## 6. Verificación y Calidad del Software

* **Compilación Backend:** Java 25 / Spring Boot 3 compila con `0` errores (`BUILD SUCCESS`).
* **Compilación Frontend:** Angular 22 genera los paquetes de distribución con `0` errores y `0` advertencias de presupuesto.
* **Pruebas de Seguridad en Tiempo Real:**
  * Intentos no autorizados a `/api/users` por parte de recepcionistas o clientes son rechazados con código HTTP `403 Forbidden`.
  * La creación de recepcionistas por parte del Administrador General se ejecuta de forma exitosa y permite el inicio de sesión inmediato con encriptación BCrypt.
  * Todas las transiciones de Front Desk (Check-In y Check-Out) actualizan la reserva y el estado de la suite de forma atómica y sincronizada.
