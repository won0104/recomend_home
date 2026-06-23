<script setup>
import { computed } from 'vue'

const props = defineProps({
  budgetAmount: { type: Number, required: true },
  dealType:     { type: String, default: '매매' },
  housingType:  { type: String, default: '아파트' },
  dealAmountMin: { type: Number, default: null },
  depositMin: { type: Number, default: null },
  monthlyRentMin: { type: Number, default: null },
  monthlyRentMax: { type: Number, default: null },
})

const emit = defineEmits([
  'update:budgetAmount',
  'update:dealType',
  'update:housingType',
  'update:dealAmountMin',
  'update:depositMin',
  'update:monthlyRentMin',
  'update:monthlyRentMax',
  'prev',
  'next',
])

const budget = computed({
  get: () => props.budgetAmount,
  set: (value) => emit('update:budgetAmount', Number(value)),
})

const budgetUnitLabel = computed(() => (props.dealType === '매매' ? '매매가' : '보증금'))
const budgetLabel = computed(() => (budget.value >= 10 ? '10억 이상' : `${budget.value}억원 이하`))

function optionalAmount(value) {
  return value === '' || value === null || value === undefined ? null : Number(value)
}

const dealAmountMin = computed({
  get: () => props.dealAmountMin ?? '',
  set: (value) => emit('update:dealAmountMin', optionalAmount(value)),
})
const depositMin = computed({
  get: () => props.depositMin ?? '',
  set: (value) => emit('update:depositMin', optionalAmount(value)),
})
const monthlyRentMin = computed({
  get: () => props.monthlyRentMin ?? '',
  set: (value) => emit('update:monthlyRentMin', optionalAmount(value)),
})
const monthlyRentMax = computed({
  get: () => props.monthlyRentMax ?? '',
  set: (value) => emit('update:monthlyRentMax', optionalAmount(value)),
})

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
        <strong>{{ budgetUnitLabel }} 최대</strong>
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

    <!-- 거래 유형별 최소 금액과 월세 범위를 별도로 받는다. 모든 금액 단위는 만원이다. -->
    <div class="budget-section">
      <template v-if="dealType === '매매'">
        <label class="budget-label" for="deal-amount-min">최소 매매가 (만원)</label>
        <input id="deal-amount-min" v-model="dealAmountMin" type="number" min="0" class="form-control" placeholder="예: 30000" />
      </template>
      <template v-else>
        <label class="budget-label" for="deposit-min">최소 보증금 (만원)</label>
        <input id="deposit-min" v-model="depositMin" type="number" min="0" class="form-control" placeholder="예: 1000" />
      </template>
    </div>

    <div v-if="dealType === '월세'" class="budget-section">
      <label class="budget-label">월세 범위 (만원)</label>
      <div class="grid grid-cols-2 gap-2">
        <input v-model="monthlyRentMin" type="number" min="0" class="form-control" placeholder="최소 월세" />
        <input v-model="monthlyRentMax" type="number" min="0" class="form-control" placeholder="최대 월세" />
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
