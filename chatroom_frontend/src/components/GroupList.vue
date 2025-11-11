<template>
  <div class="group-list">
    <transition-group name="list-fade" tag="ul" class="list">
      <li
        v-for="group in groups"
        :key="group.id"
        @click="$emit('select', group)"
        class="row"
        :class="{ active: selectedGroup && selectedGroup.id === group.id }"
      >
        <div class="info">
          <div class="name">{{ group.name }}</div>
          <div class="sub">{{ group.users ? group.users.length : 0 }} miembros</div>
        </div>
        <div class="meta">
          <span v-if="groupUnreadCounts[group.id] > 0" class="unread">{{ groupUnreadCounts[group.id] }}</span>
        </div>
      </li>
    </transition-group>
  </div>
</template>

<script>
export default {
  name: 'GroupList',
  props: {
    groups: {
      type: Array,
      required: true
    },
    groupUnreadCounts: {
      type: Object,
      default: () => ({})
    },
    selectedGroup: {
      type: Object,
      default: null
    }
  }
}
</script>

<style scoped>
.group-list { padding: 0; }
.list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; }

.row {
  padding: 12px 16px;
  padding-right: 36px; /* espacio para el contador a la derecha */
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  background: transparent;
  border: none;
  border-bottom: 1px solid var(--border-color);
  position: relative;
  transition: transform .15s ease, color .15s ease;
}
.row:first-child { border-top: 1px solid var(--border-color); }
.row:hover { background: transparent; transform: translateX(2px); }
.row.active { background: transparent; }

.row::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 0;
  background: linear-gradient(180deg, var(--brand-gradient-start), var(--brand-gradient-end));
  transition: width .2s ease;
}
.row:hover::before, .row.active::before { width: 3px; }

.info { flex: 1; min-width: 0; }
.name { font-weight: 600; color: #334155; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; transition: color .15s ease; }
.sub { font-size: 12px; color: #64748b; transition: color .15s ease; }
.row:hover .name { color: var(--brand-gradient-start); }
.row:hover .sub { color: #4b5563; }

.meta { position: absolute; right: 12px; top: 12px; transform: none; display: flex; align-items: center; gap: 8px; z-index: 1; }
.unread { background: #ef4444; color: #fff; border-radius: 9999px; min-width: 18px; height: 18px; padding: 0 6px; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; animation: badgePulse 1.4s ease-in-out infinite; }

@media (max-width: 768px) {
  .row { padding: 10px 14px; padding-right: 34px; }
  .name { font-size: 13px; }
  .unread { min-width: 16px; height: 16px; font-size: 10px; }
}

@media (max-width: 480px) {
  .row { padding: 8px 12px; }
  .name { font-size: 12px; }
}

/* Animaciones */
.list-fade-enter-active, .list-fade-leave-active { transition: all .2s ease; }
.list-fade-enter-from, .list-fade-leave-to { opacity: 0; transform: translateY(4px); }

@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}
</style>