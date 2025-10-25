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
  justify-content: center;
  margin: 12px 0;
}
.label {
  background: linear-gradient(180deg, #e2e8f0 0%, #dde5ef 100%);
  color: #334155;
  border-radius: 9999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.8), 0 1px 1px rgba(0,0,0,0.04);
}
</style>