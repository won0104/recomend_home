<script setup>
const props = defineProps({
  dealType: { type: String, default: '매매' },
  housingType: { type: String, default: '아파트' },
})

const emit = defineEmits(['update:dealType', 'update:housingType', 'prev', 'next'])

const dealOptions = ['매매', '전세', '월세']
const housingOptions = [
  { value: '아파트', icon: '🏢', label: '아파트' },
  { value: '오피스텔', icon: '🏙️', label: '오피스텔' },
  { value: '주택', icon: '🏠', label: '주택' },
]
</script>

<template>
  <div class="condition-card budget-card">
    <div class="budget-card-title">
      <span class="budget-icon" aria-hidden="true">▣</span>
      <h2>거래 유형 및 주거 형태</h2>
    </div>

    <div class="budget-section">
      <label class="budget-label">거래 유형</label>
      <div class="segmented-options">
        <button
          v-for="option in dealOptions"
          :key="option"
          type="button"
          class="segmented-option"
          :class="{ 'is-selected': dealType === option }"
          @click="emit('update:dealType', option)"
        >
          {{ option }}
        </button>
      </div>
    </div>

    <div class="budget-section">
      <label class="budget-label">주거 형태</label>
      <div class="housing-options">
        <button
          v-for="option in housingOptions"
          :key="option.value"
          type="button"
          class="housing-option"
          :class="{ 'is-selected': housingType === option.value }"
          @click="emit('update:housingType', option.value)"
        >
          <span class="housing-option__icon">{{ option.icon }}</span>
          <span class="housing-option__label">{{ option.label }}</span>
        </button>
      </div>
    </div>

    <div class="condition-actions budget-actions">
      <button type="button" class="btn btn-outline-secondary" @click="emit('prev')">이전</button>
      <button type="button" class="btn btn-primary" @click="emit('next')">다음 →</button>
    </div>
  </div>
</template>
