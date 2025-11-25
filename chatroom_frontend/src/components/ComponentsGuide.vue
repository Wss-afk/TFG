<template>
  <div class="components-guide">
    <h2>Guía de componentes</h2>

    <section>
      <h3>Botones</h3>
      <div class="row">
        <UiButton>Primario</UiButton>
        <UiButton variant="secondary">Secundario</UiButton>
        <UiButton variant="success">Éxito</UiButton>
        <UiButton variant="danger">Peligro</UiButton>
        <UiButton variant="ghost">Ghost</UiButton>
      </div>
      <div class="row">
        <UiButton size="sm">Pequeño</UiButton>
        <UiButton size="md">Mediano</UiButton>
        <UiButton size="lg">Grande</UiButton>
      </div>
    </section>

    <section>
      <h3>Inputs</h3>
      <div class="row">
        <UiInput v-model="demoText" placeholder="Escribe algo…" />
        <UiInput v-model="demoSearch" placeholder="Buscar…">
          <template #icon>
            <Icon name="search" :size="16" />
          </template>
        </UiInput>
      </div>
    </section>

    <section>
      <h3>Badges</h3>
      <div class="row">
        <UiBadge>Neutral</UiBadge>
        <UiBadge variant="primary">Primary</UiBadge>
        <UiBadge variant="success">Success</UiBadge>
        <UiBadge variant="warning">Warning</UiBadge>
        <UiBadge variant="danger">Danger</UiBadge>
      </div>
    </section>
  </div>
</template>

<script>
import UiButton from './design/Button.vue'
import UiInput from './design/Input.vue'
import UiBadge from './design/Badge.vue'
import Icon from './Icon.vue'

export default {
  name: 'ComponentsGuide',
  components: { UiButton, UiInput, UiBadge, Icon },
  data() {
    return { demoText: '', demoSearch: '' }
  }
}
</script>

<style scoped>
.components-guide { padding: var(--space-4); display: grid; gap: var(--space-4); }
.row { display: flex; gap: var(--space-3); flex-wrap: wrap; align-items: center; }
section { display: grid; gap: var(--space-2); }
</style>
