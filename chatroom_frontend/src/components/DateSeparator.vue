<template>
  <div class="date-separator">
    <span class="label">{{ label }}</span>
  </div>
</template>

<script>
export default {
  name: 'DateSeparator',
  props: {
    label: { type: String, required: true }
  }
}
</script>

<style scoped>
.date-separator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 12px 0;
}
.date-separator::before,
.date-separator::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-color);
}
.label {
  background: var(--color-surface);
  color: var(--color-text-muted);
  border-radius: 9999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-1);
}
</style>