<template>
  <div :class="['ui-input', `ui-input--${size}`, { 'has-icon': !!$slots.icon }]">
    <span v-if="$slots.icon" class="ui-input__icon"><slot name="icon" /></span>
    <input
      v-bind="$attrs"
      :type="type"
      :placeholder="placeholder"
      :disabled="disabled"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  </div>
</template>

<script>
export default {
  name: 'UiInput',
  inheritAttrs: false,
  props: {
    modelValue: { type: [String, Number], default: '' },
    type: { type: String, default: 'text' },
    placeholder: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
    size: { type: String, default: 'md' } // sm | md | lg
  }
}
</script>

<style scoped>
.ui-input { position: relative; display: inline-flex; align-items: center; }
.ui-input input {
  border: 1px solid var(--border-color);
  background: var(--color-surface);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  transition: box-shadow .2s ease, border-color .2s ease;
}
.ui-input--sm input { padding: 6px 10px; font-size: var(--font-size-sm); }
.ui-input--md input { padding: 10px 14px; font-size: var(--font-size-md); }
.ui-input--lg input { padding: 12px 16px; font-size: var(--font-size-lg); }
.ui-input input:focus { outline: 2px solid var(--color-primary); outline-offset: 2px; border-color: var(--color-primary); }

.ui-input.has-icon input { padding-left: 36px; }
.ui-input__icon { position: absolute; left: 10px; color: var(--text-muted); display: inline-flex; align-items: center; }
</style>
