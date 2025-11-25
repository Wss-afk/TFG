<template>
  <span :class="['ui-badge', `ui-badge--${variant}`, `ui-badge--${size}`]">
    <slot />
  </span>
</template>

<script>
export default {
  name: 'UiBadge',
  props: {
    variant: { type: String, default: 'neutral' }, // neutral | primary | success | warning | danger
    size: { type: String, default: 'md' } // sm | md | lg
  }
}
</script>

<style scoped>
.ui-badge { display: inline-flex; align-items: center; gap: var(--space-1); font-weight: 700; border-radius: 9999px; }
.ui-badge--sm { font-size: var(--font-size-xs); padding: 2px 6px; }
.ui-badge--md { font-size: var(--font-size-sm); padding: 4px 8px; }
.ui-badge--lg { font-size: var(--font-size-md); padding: 6px 10px; }

.ui-badge--neutral { background: #e5e7eb; color: #111827; }
.ui-badge--primary { background: #dbeafe; color: #1d4ed8; }
.ui-badge--success { background: #dcfce7; color: #166534; }
.ui-badge--warning { background: #fef3c7; color: #92400e; }
.ui-badge--danger { background: #fee2e2; color: #7f1d1d; }
</style>
