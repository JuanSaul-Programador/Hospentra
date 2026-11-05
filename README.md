# Hospentra — Luxury Sanctuary Hotel PMS & Booking Platform

Plataforma integral fullstack para la gestión hotelera boutique de lujo, orquestando reservas directas de huéspedes, pasarela de pago bancaria tokenizada (PCI-DSS SAQ A), mostrador de recepción en tiempo real (PMS), gobernanza de camaristas (Housekeeping), conserjería digital en vivo (Concierge Chat SSE), liquidación de folio y Check-Out Express.

---

## Stack Tecnológico

- **Backend:** Java 25, Spring Boot 4, Spring Data JPA / Hibernate, Spring Security 6, JWT HMAC-SHA384, Flyway Migrations, RFC 7807 ProblemDetail.
- **Frontend:** Angular 22, Standalone Components, Angular Signals reactivas, SCSS Modular, View Transitions API.
- **Servidor Web y Proxy:** Nginx 1.27 Alpine con soporte para Server-Sent Events (SSE) y compresión Gzip.
- **Base de Datos:** PostgreSQL 17 Alpine con persistencia en volúmenes Docker y healthchecks nativos.
- **Contenerización:** Docker & Docker Compose con compilaciones multi-etapa (*multi-stage builds*) ligeras.
- **Testing:** 302 pruebas automatizadas (226 en Backend con JUnit/Mockito, 76 en Frontend con Vitest).

---

## Arquitectura de Contenedores

El proyecto está 100% contenerizado. Una red interna bridge privada conecta los tres microservicios:

```
[ Navegador Web ] ──── Puerto 80 ────> [ Nginx + Angular 22 ]
                                              │
                         Proxy /api/ y /uploads/
                                              ▼
[ Postman / Host ] ─── Puerto 8080 ──> [ Spring Boot 4 / Java 25 ]
                                              │
                                    Conexión JDBC interna
                                              ▼
[ DBeaver / Host ] ─── Puerto 5433 ──> [ PostgreSQL 17 Alpine ]
```

---

## Despliegue Rápido con Docker (3 Pasos)

Cualquier persona puede clonar y levantar el proyecto completo sin instalar Java, Maven, Node.js ni PostgreSQL:

### 1. Clonar el repositorio
```bash
git clone https://github.com/JuanSaul-Programador/Hospentra.git
cd Hospentra
```

### 2. Generar el archivo de entorno
```bash
copy .env.example .env
```
*(En Linux / macOS usa: `cp .env.example .env`)*

### 3. Construir y levantar contenedores
```bash
docker compose up -d --build
```

Una vez levantado, abre tu navegador en:
- **Portal de Huéspedes:** [http://localhost](http://localhost)
- **Panel Administrativo (PMS):** [http://localhost/admin/login](http://localhost/admin/login)
- **Estación de Cocina (KDS):** [http://localhost/kitchen/orders](http://localhost/kitchen/orders)
- **API REST Backend:** [http://localhost:8080/api/rooms](http://localhost:8080/api/rooms)

---

## Credenciales de Demostración (Seed Data)

El sistema incluye cuentas preconfiguradas con roles corporativos y datos sembrados:

| Rol | Usuario | Contraseña | Destino |
| :--- | :--- | :--- | :--- |
| **Super Administrador** | `admin` | `admin123` | `/admin/dashboard` |
| **Personal de Cocina** | `cocina` | `cocina123` | `/kitchen/orders` |
| **Huésped (Con Estancia Activa)** | `pepetest` | `Password123!` | `/my-reservations` |
| **Huésped (Historial de Reservas)** | `sofia_valdez` | `Password123!` | `/my-reservations` |

---

## Módulos Principales del Sistema

1. **Catálogo y Motor de Reservas:**
   - Selección dinámica de suites con filtrado de fechas sin solapamiento (*pessimistic locking*).
   - Pasarela tokenizada bancaria (Stripe Elements / PCI-DSS SAQ A) y modalidad de pago en recepción.
2. **PMS Front Desk y Rack Operativo:**
   - Visualización y gestión en tiempo real del estado de cada habitación (Disponible, Ocupada, En Limpieza, Mantenimiento).
   - Bloqueos técnicos preventivos y correctivos (*Out of Order*).
3. **Conserjería Digital en Vivo (Concierge Chat SSE):**
   - Canal bidireccional streaming en tiempo real entre huéspedes con estancia activa y el equipo de recepción.
   - Prevención de colisiones operativas mediante asignación concurrente (*Claiming* "Atendido por").
4. **Liquidación de Folio y Check-Out Express:**
   - Cierre de cuenta con conciliación de consumos extra (Room Service y Spa).
   - Check-Out Express digital para el huésped con confirmación de entrega de llaves.
5. **Kitchen Display System (KDS):**
   - Tablero visual de comandas en tiempo real con transiciones operativas (Pendiente, En Preparación, Entregado).

---

## Comandos Operativos Útiles

Estado de los contenedores:
```bash
docker compose ps
```

Logs en vivo del backend:
```bash
docker compose logs -f backend
```

Logs en vivo del frontend:
```bash
docker compose logs -f frontend
```

Pausar los contenedores preservando datos:
```bash
docker compose stop
```

Reanudar contenedores pausados:
```bash
docker compose start
```

Detener y remover contenedores y redes:
```bash
docker compose down
```

---

## Documentación Técnica Detallada

Para una revisión exhaustiva del diseño de software, diagramas relacionales, arquitectura criptográfica, catálogo de 23 migraciones Flyway y contratos de API, consulta el documento oficial:

👉 **[HOSPENTRA_DOCUMENTATION.md](HOSPENTRA_DOCUMENTATION.md)**
