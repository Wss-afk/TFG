<template>
  <button
    :class="[
      'ui-btn',
      `ui-btn--${variant}`,
      `ui-btn--${size}`,
      { 'is-loading': loading }
    ]"
    :disabled="disabled || loading"
    :type="type"
  >
    <span v-if="loading" class="ui-btn__spinner" aria-hidden="true">⏳</span>
    <slot />
  </button>
</template>

<script>
export default {
  name: 'UiButton',
  props: {
    variant: { type: String, default: 'primary' }, // primary | secondary | success | danger | ghost
    size: { type: String, default: 'md' }, // sm | md | lg
    type: { type: String, default: 'button' },
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false }
  }
}
</script>

<style scoped>
.ui-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  font-weight: 600;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  cursor: pointer;
  transition: transform .12s ease, box-shadow .2s ease, filter .2s ease;
}
.ui-btn:disabled { cursor: not-allowed; opacity: .7; }
.ui-btn--sm { padding: 6px 10px; font-size: var(--font-size-sm); }
.ui-btn--md { padding: 10px 14px; font-size: var(--font-size-md); }
.ui-btn--lg { padding: 12px 18px; font-size: var(--font-size-lg); }

.ui-btn--primary { background: var(--color-primary); color: var(--color-primary-contrast); }
.ui-btn--primary:hover { filter: brightness(.97); }

.ui-btn--secondary { background: #e2e8f0; color: #111827; }
.ui-btn--secondary:hover { filter: brightness(.98); }

.ui-btn--success { background: #10b981; color: #fff; }
.ui-btn--danger { background: #ef4444; color: #fff; }
.ui-btn--ghost { background: transparent; border-color: var(--border-color); color: var(--text-primary); }

.ui-btn__spinner { font-size: 1em; opacity: .85; }
</style>
