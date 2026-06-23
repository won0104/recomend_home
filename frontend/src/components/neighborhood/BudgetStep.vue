<script setup>
import { computed } from 'vue'

const props = defineProps({
  budgetAmount: { type: Number, required: true },
  dealType:     { type: String, default: '전세' },
  housingType:  { type: String, default: '아파트' },
})

const emit = defineEmits([
  'update:budgetAmount',
  'update:dealType',
  'update:housingType',
  'prev',
  'next',
])

const budget = computed({
  get: () => props.budgetAmount,
  set: (value) => emit('update:budgetAmount', Number(value)),
})

const budgetLabel = computed(() => (budget.value >= 10 ? '10억 이상' : `${budget.value}억원 이하`))

const dealOptions = ['매매', '전세', '월세']
const housingOptions = [
  { value: '아파트',   icon: '🏢', label: '아파트' },
  { value: '오피스텔', icon: '🏙️', label: '오피스텔' },
  { value: '주택',     icon: '🏠', label: '주택' },
]
</script>

<template>
  <div class="condition-card budget-card">
    <div class="budget-card-title">
      <span class="budget-icon" aria-hidden="true">▣</span>
      <h2>예산 및 거래 유형</h2>
    </div>

    <!-- 예산 슬라이더 -->
    <div class="budget-section">
      <div class="radius-control-head">
        <strong>예산 범위</strong>
        <em>{{ budgetLabel }}</em>
      </div>
      <input
        v-model.number="budget"
        class="radius-slider budget-slider"
        type="range"
        min="0.5"
        max="10"
        step="0.5"
        aria-label="예산 범위"
      />
      <!-- 각 left% = (값 - 0.5) / (10 - 0.5) * 100 -->
      <div class="budget-marks" aria-hidden="true">
        <span style="left: 0%">5천만원</span>
        <span style="left: 47.4%">5억</span>
        <span style="left: 100%">10억 이상</span>
      </div>
    </div>

    <!-- 거래 유형 -->
    <div class="budget-section">
      <label class="budget-label">거래 유형</label>
      <div class="segmented-options">
        <button
          v-for="opt in dealOptions"
          :key="opt"
          type="button"
          class="segmented-option"
          :class="{ 'is-selected': dealType === opt }"
          @click="emit('update:dealType', opt)"
        >
          {{ opt }}
        </button>
      </div>
    </div>

    <!-- 주거 형태 -->
    <div class="budget-section">
      <label class="budget-label">주거 형태</label>
      <div class="housing-options">
        <button
          v-for="opt in housingOptions"
          :key="opt.value"
          type="button"
          class="housing-option"
          :class="{ 'is-selected': housingType === opt.value }"
          @click="emit('update:housingType', opt.value)"
        >
          <span class="housing-option__icon">{{ opt.icon }}</span>
          <span class="housing-option__label">{{ opt.label }}</span>
        </button>
      </div>
    </div>

    <div class="condition-actions budget-actions">
      <button type="button" class="btn btn-outline-secondary" @click="emit('prev')">이전</button>
      <button type="button" class="btn btn-primary" @click="emit('next')">다음 →</button>
    </div>
  </div>
</template>
