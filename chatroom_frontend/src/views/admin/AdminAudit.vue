<template>
  <div class="admin-audit">
    <div class="alert alert-info" role="alert">
      Auditoría y actividad del sistema — Próximamente.
      <br />
      Aquí verás acciones de administración (creación/edición/eliminación de usuarios y grupos), logs y filtros por fecha/acción.
    </div>

    <div class="card">
      <div class="card-header">Ideas de métricas</div>
      <div class="card-body">
        <ul>
          <li>Últimas 20 acciones de administración</li>
          <li>Conteo de usuarios por rol</li>
          <li>Grupos totales y miembros promedio</li>
          <li>Accesos al panel de administración</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminAudit'
}
</script>

<style scoped>
.admin-audit { padding: 8px; }
</style>