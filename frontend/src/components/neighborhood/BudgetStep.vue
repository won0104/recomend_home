<script setup>
import { computed } from 'vue'

const props = defineProps({
  budgetAmount: { type: Number, required: true },
  dealType: { type: String, required: true },
  housingType: { type: String, required: true }
})

const emit = defineEmits([
  'update:budgetAmount',
  'update:dealType',
  'update:housingType',
  'prev',
  'next'
])

const budget = computed({
  get: () => props.budgetAmount,
  set: (value) => emit('update:budgetAmount', Number(value))
})

const selectedDealType = computed({
  get: () => props.dealType,
  set: (value) => emit('update:dealType', value)
})

const selectedHousingType = computed({
  get: () => props.housingType,
  set: (value) => emit('update:housingType', value)
})

const budgetLabel = computed(() => (budget.value >= 10 ? '10억 이상' : `${budget.value}억원 이하`))
</script>

<template>
  <div class="condition-card budget-card">
    <div class="budget-card-title">
      <span class="budget-icon" aria-hidden="true">▣</span>
      <h2>예산 및 거래 유형</h2>
    </div>

    <!-- 예산/거래 조건은 부모 View에 저장되어 최종 추천 조건으로 합쳐집니다. -->
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
      <div class="budget-marks" aria-hidden="true">
        <span>5천만원</span>
        <span>2억</span>
        <span>5억</span>
        <span>10억 이상</span>
      </div>
    </div>

    <div class="budget-section">
      <strong class="budget-label">거래 유형</strong>
      <div class="segmented-options" role="group" aria-label="거래 유형">
        <button type="button" class="segmented-option" :class="{ 'is-selected': selectedDealType === 'all' }" @click="selectedDealType = 'all'">✓ 전체</button>
        <button type="button" class="segmented-option" :class="{ 'is-selected': selectedDealType === 'rent' }" @click="selectedDealType = 'rent'">월세</button>
        <button type="button" class="segmented-option" :class="{ 'is-selected': selectedDealType === 'sale' }" @click="selectedDealType = 'sale'">매매</button>
      </div>
    </div>

    <div class="budget-section">
      <label class="budget-label" for="housing-type">주거 형태</label>
      <select id="housing-type" v-model="selectedHousingType" class="form-select housing-select">
        <option value="apartment">아파트</option>
        <option value="officetel">오피스텔</option>
        <option value="villa">빌라/연립</option>
        <option value="house">단독주택</option>
      </select>
    </div>

    <div class="condition-actions budget-actions">
      <button type="button" class="btn btn-outline-secondary" @click="emit('prev')">이전</button>
      <button type="button" class="btn btn-primary" @click="emit('next')">다음 →</button>
    </div>
  </div>
</template>
