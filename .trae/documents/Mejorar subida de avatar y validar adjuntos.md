## Objetivo
Permitir al administrador subir una imagen y asignarla como `avatarUrl` de un usuario, manteniendo intacto el flujo de adjuntos.

## Cambios en frontend
1. `AdminUsers.vue`: añadir botón "Subir avatar" por usuario y `input` de archivo oculto.
2. Validación: solo `image/*`, tamaño ≤20MB, formatos comunes (JPEG/PNG/WebP).
3. Subida: reutilizar `POST /api/chat/upload` para obtener `url`.
4. Persistencia: `PUT /api/admin/users/{id}` con `avatarUrl`.
5. UX: previsualización, estados de carga, y mensajes de error.

## Cambios en backend
- No se requieren cambios (se reutiliza `/api/chat/upload` y `PUT /api/admin/users/{id}`).

## Pruebas
- Subir imagen válida e inválida; comprobar actualización y visualización inmediata en cabeceras/listas.

## Siguientes pasos (opcional)
- Fase 2: endpoint dedicado `POST /api/user/avatar` y UI de Perfil para usuario final.

Confirma que implementemos esta Fase 1 ahora.