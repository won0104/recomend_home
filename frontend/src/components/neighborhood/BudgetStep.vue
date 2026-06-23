<script setup>
import { computed } from 'vue'

const props = defineProps({
  budgetAmount: { type: Number, required: true },
  dealType:     { type: String, default: '매매' },
  dealAmountMin: { type: Number, default: null },
  depositMin: { type: Number, default: null },
  monthlyRentMin: { type: Number, default: null },
  monthlyRentMax: { type: Number, default: null },
})

const emit = defineEmits([
  'update:budgetAmount',
  'update:dealAmountMin',
  'update:depositMin',
  'update:monthlyRentMin',
  'update:monthlyRentMax',
  'prev',
  'next',
])

const budgetUnitLabel = computed(() => (props.dealType === '매매' ? '매매가' : '보증금'))
const budgetLabel = computed(() => (props.budgetAmount >= 10 ? '10억 이상' : `${props.budgetAmount}억원 이하`))
const minimumBudgetLabel = computed(() => {
  const value = activeMinimumBudget.value
  if (value <= 0) return '제한 없음'
  return value >= 10 ? '10억원 이상' : `${value}억원 이상`
})

/** 화면의 억 단위 슬라이더 값을 API·DB의 만원 단위 값으로 바꾼다. */
function toTenThousandWon(value) {
  return Math.round(Number(value) * 10000)
}

function toEok(value) {
  return value == null ? 0 : Number(value) / 10000
}

function setActiveMinimumBudget(value) {
  const amount = Number(value) <= 0 ? null : toTenThousandWon(value)
  if (props.dealType === '매매') {
    emit('update:dealAmountMin', amount)
  } else {
    emit('update:depositMin', amount)
  }
}

const activeMinimumBudget = computed({
  get: () => props.dealType === '매매' ? toEok(props.dealAmountMin) : toEok(props.depositMin),
  set: (value) => {
    const normalized = Number(value)
    // 최소값이 최대값을 넘지 않도록 최대 바도 함께 올린다.
    if (normalized > props.budgetAmount && props.budgetAmount < 10) {
      emit('update:budgetAmount', normalized)
    }
    setActiveMinimumBudget(normalized)
  },
})

const budget = computed({
  get: () => props.budgetAmount,
  set: (value) => {
    const normalized = Number(value)
    // 최대값을 내릴 때 최소값도 같은 값 이하로 맞춘다.
    if (normalized < activeMinimumBudget.value) {
      setActiveMinimumBudget(normalized)
    }
    emit('update:budgetAmount', normalized)
  },
})

const MONTHLY_RENT_SLIDER_MAX = 500
const monthlyRentMinSlider = computed({
  get: () => props.monthlyRentMin ?? 0,
  set: (value) => {
    const normalized = Number(value)
    if (props.monthlyRentMax != null && normalized > props.monthlyRentMax) {
      emit('update:monthlyRentMax', normalized)
    }
    emit('update:monthlyRentMin', normalized <= 0 ? null : normalized)
  },
})
const monthlyRentMin = computed({
  get: () => props.monthlyRentMin ?? 0,
  set: (value) => emit('update:monthlyRentMin', Number(value) <= 0 ? null : Number(value)),
})
const monthlyRentMaxSlider = computed({
  get: () => props.monthlyRentMax ?? MONTHLY_RENT_SLIDER_MAX,
  set: (value) => {
    const normalized = Number(value)
    if (normalized < monthlyRentMin.value) {
      emit('update:monthlyRentMin', normalized <= 0 ? null : normalized)
    }
    // 맨 오른쪽은 상한선이 없는 상태로 해석해 기존 "제한 없음" 정책과 맞춘다.
    emit('update:monthlyRentMax', normalized >= MONTHLY_RENT_SLIDER_MAX ? null : normalized)
  },
})
const monthlyRentMinLabel = computed(() => monthlyRentMin.value <= 0 ? '제한 없음' : `${monthlyRentMin.value}만원 이상`)
const monthlyRentMaxLabel = computed(() => props.monthlyRentMax == null ? '제한 없음' : `${props.monthlyRentMax}만원 이하`)

</script>

<template>
  <div class="condition-card budget-card">
    <div class="budget-card-title">
      <span class="budget-icon" aria-hidden="true">▣</span>
      <h2>{{ dealType }} 예산 설정</h2>
    </div>


    <!-- 거래 유형별 금액 범위: 기존 예산 바와 같은 UI로 최소·최대를 조절한다. -->
    <div class="budget-section">
      <div class="radius-control-head">
        <strong>{{ budgetUnitLabel }} 최소</strong>
        <em>{{ minimumBudgetLabel }}</em>
      </div>
      <input
        v-model.number="activeMinimumBudget"
        class="radius-slider budget-slider"
        type="range"
        min="0"
        max="10"
        step="0.5"
        :aria-label="`${budgetUnitLabel} 최소`"
      />
      <div class="budget-marks" aria-hidden="true">
        <span style="left: 0%">제한 없음</span>
        <span style="left: 50%">5억</span>
        <span style="left: 100%">10억 이상</span>
      </div>
    </div>

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
        :aria-label="`${budgetUnitLabel} 최대`"
      />
      <div class="budget-marks" aria-hidden="true">
        <span style="left: 0%">5천만원</span>
        <span style="left: 47.4%">5억</span>
        <span style="left: 100%">10억 이상</span>
      </div>
    </div>

    <div v-if="dealType === '월세'" class="budget-section">
      <div class="radius-control-head">
        <strong>월세 최소</strong>
        <em>{{ monthlyRentMinLabel }}</em>
      </div>
      <input
        v-model.number="monthlyRentMinSlider"
        class="radius-slider budget-slider"
        type="range"
        min="0"
        :max="MONTHLY_RENT_SLIDER_MAX"
        step="10"
        aria-label="월세 최소"
      />
      <div class="budget-marks" aria-hidden="true">
        <span style="left: 0%">제한 없음</span>
        <span style="left: 50%">250만원</span>
        <span style="left: 100%">500만원 이상</span>
      </div>
    </div>

    <div v-if="dealType === '월세'" class="budget-section">
      <div class="radius-control-head">
        <strong>월세 최대</strong>
        <em>{{ monthlyRentMaxLabel }}</em>
      </div>
      <input
        v-model.number="monthlyRentMaxSlider"
        class="radius-slider budget-slider"
        type="range"
        min="10"
        :max="MONTHLY_RENT_SLIDER_MAX"
        step="10"
        aria-label="월세 최대"
      />
      <div class="budget-marks" aria-hidden="true">
        <span style="left: 0%">10만원</span>
        <span style="left: 50%">250만원</span>
        <span style="left: 100%">제한 없음</span>
      </div>
    </div>

    <div class="condition-actions budget-actions">
      <button type="button" class="btn btn-outline-secondary" @click="emit('prev')">이전</button>
      <button type="button" class="btn btn-primary" @click="emit('next')">다음 →</button>
    </div>
  </div>
</template>
