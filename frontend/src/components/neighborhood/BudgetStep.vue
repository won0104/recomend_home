<script setup>
import { computed } from 'vue'

const props = defineProps({
  budgetAmount: { type: Number, required: true }
})

const emit = defineEmits([
  'update:budgetAmount',
  'prev',
  'next'
])

const budget = computed({
  get: () => props.budgetAmount,
  set: (value) => emit('update:budgetAmount', Number(value))
})

const budgetLabel = computed(() => (budget.value >= 10 ? '10억 이상' : `${budget.value}억원 이하`))
</script>

<template>
  <div class="condition-card budget-card">
    <div class="budget-card-title">
      <span class="budget-icon" aria-hidden="true">▣</span>
      <h2>예산 범위</h2>
    </div>

    <!-- 예산 조건은 부모 View에 저장되어 최종 추천 조건으로 합쳐집니다. -->
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
        <span style="left: 15.8%">2억</span>
        <span style="left: 47.4%">5억</span>
        <span style="left: 100%">10억 이상</span>
      </div>
    </div>

    <div class="condition-actions budget-actions">
      <button type="button" class="btn btn-outline-secondary" @click="emit('prev')">이전</button>
      <button type="button" class="btn btn-primary" @click="emit('next')">다음 →</button>
    </div>
  </div>
</template>
