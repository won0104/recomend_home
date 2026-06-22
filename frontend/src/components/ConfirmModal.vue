<script setup>
defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, required: true }
})
const emit = defineEmits(['close'])
</script>

<template>
  <div v-if="open" class="modal-backdrop-custom" @click.self="emit('close')">
    <div class="modal-card">
      <div class="modal-card-header">
        <h5>{{ title }}</h5>
        <button type="button" class="btn btn-link btn-sm" @click="emit('close')">닫기</button>
      </div>
      <slot />
    </div>
  </div>
</template>
