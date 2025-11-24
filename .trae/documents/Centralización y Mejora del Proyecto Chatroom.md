## Objetivos
- Centralizar lógica y servicios para reducir duplicidad y acoplamiento.
- Sustituir cabeceras ad‑hoc por autenticación/autorización robusta basada en JWT.
- Homogeneizar respuestas y manejo de errores en backend y frontend.
- Mejorar seguridad, observabilidad, calidad y mantenibilidad.
- Añadir funcionalidades clave pendientes del chat y la administración.

## Alcance
- Backend (Spring Boot) y Frontend (Vue 3).
- WebSocket STOMP, REST, subida de archivos y auditoría.

## Arquitectura y Centralización (Backend)
- Core modular dentro de `chatroom_backend`:
  - `core/domain`: entidades y repositorios JPA.
  - `core/service`: interfaces y servicios de negocio (usuarios, grupos, chat, archivos, auditoría).
  - `core/dto` + `mapper` (MapStruct) para separar modelos de persistencia y transporte.
  - `core/security`: JWT, roles y autorización con `@PreAuthorize`.
  - `core/web`: respuesta uniforme `ApiResponse<T>` y `GlobalExceptionHandler` (manejo de validaciones y errores).
  - `core/events`: eventos de dominio (envío de mensaje, cambio de estado, acciones admin) para auditoría y notificaciones.
- Controladores por módulo: `user`, `admin`, `chat`, `group`, `files`, `audit` usando servicios centralizados.
- Validaciones y reglas de negocio en servicios (membresía, estados `enabled`, límites).
- Eliminar dependencia de `X-Admin-UserId` en `AdminController` y migrar a autorización por rol desde JWT.
- WebSocket: autenticación en handshake con JWT; establecer `Principal` y verificar permisos en `StompUserInterceptor`.

## Centralización (Frontend)
- Cliente HTTP común:
  - Instancia única de Axios con `baseURL=/api`, interceptores para `Authorization: Bearer` y manejo estandarizado de errores.
  - Renovación de token (refresh) y redirecciones 401/403.
- Servicio WebSocket unificado:
  - Gestión de conexión, reconexión, suscripciones y publicación.
  - Encapsular headers y verificación por JWT.
- Vuex por dominios: `auth`, `chat`, `groups`, `admin`, `audit` con acciones que consumen servicios centralizados.
- Componentes compartidos: listas (usuarios/grupos), mensajes, input/adjuntos, loader y toasts; reglas de UI reutilizables.
- Router con guards por rol, rutas protegidas y fallback consistente.

## Seguridad y Configuración
- JWT (access + refresh), roles `USER`/`SUPER_ADMIN` y claims.
- Perfiles `dev/prod` en `application.yml`; credenciales y límites vía variables de entorno.
- Subida de archivos: validación MIME/tamaño, nombres seguros, almacenamiento fuera de webroot, miniaturas y URLs controladas.
- CORS y CSRF conforme al flujo con tokens.

## Funcionalidades que faltan/mejoras
- Presencia y estado: última conexión, indicadores "escribiendo".
- Confirmaciones de lectura y conteo no leído por conversación/grupo.
- Búsqueda y paginación de mensajes; filtros.
- Adjuntos: imágenes/archivos con previsualización y límites; thumbnails.
- Moderación: borrar mensajes, bloquear usuarios, reportes.
- Notificaciones: Web Push y correos opcionales.
- Internacionalización (i18n) en frontend.

## Observabilidad y Calidad
- Tests unitarios e integración (controladores/servicios, WebSocket), y básicos en Vue.
- Logging estructurado, métricas (Micrometer), endpoints de salud.
- Docker Compose para entorno reproducible (backend + mysql + frontend proxy).
- CI con build, linters, tests y cobertura.

## Plan de Implementación por Fases
1. Seguridad y Base
   - Introducir JWT, `@PreAuthorize` y guards de rutas admin.
   - `GlobalExceptionHandler` y `ApiResponse<T>` en backend.
   - Axios común con interceptores y guards de router.
2. Chat y WebSocket
   - Handshake STOMP con JWT y `Principal`.
   - Servicio WebSocket unificado y eventos de presencia.
   - Lecturas (read receipts) y typing.
3. Archivos
   - Storage service, validaciones, thumbnails; servir por rutas controladas.
4. Observabilidad y CI
   - Tests, Docker Compose y pipeline CI.
5. Administración
   - Consola admin consolidada, gestión de roles y membresías con APIs unificadas.

## Entregables y Verificación
- Rutas protegidas y servicios centralizados verificables con tests automáticos.
- Scripts Docker Compose para arranque local fácil.
- Métricas y healthchecks comprobables.
- Documentación mínima de uso de tokens y endpoints (README del equipo, si lo solicitas).

¿Confirmas este plan para comenzar con la Fase 1 (JWT, manejo de errores y centralización de clientes)?