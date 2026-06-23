<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 매매는 매매가, 전세·월세는 보증금의 최대 예산을 억 단위로 표현한다.
  budgetAmount: {
    type: Number,
    required: true,
  },

  dealType: {
    type: String,
    default: '매매',
  },

  // 월세를 선택한 경우 보증금과 별도로 월세 최대 금액을 받는다.
  monthlyRentMax: {
    type: Number,
    default: null,
  },
})

const emit = defineEmits([
  'update:budgetAmount',
  'update:monthlyRentMax',
  'prev',
  'next',
])

const isMonthlyRent = computed(() => props.dealType === '월세')

const budgetUnitLabel = computed(() =>
    props.dealType === '매매' ? '매매가' : '보증금',
)

// 매매·전세는 최대 20억, 월세 보증금은 최대 5억으로 제한한다.
const budgetSliderMax = computed(() =>
    isMonthlyRent.value ? 5 : 20,
)

// 월세 보증금은 500만원(0.05억) 단위,
// 매매·전세는 5천만원(0.5억) 단위로 선택한다.
const budgetSliderStep = computed(() =>
    isMonthlyRent.value ? 0.05 : 0.5,
)

const budgetLabel = computed(() =>
    `${props.budgetAmount}억원 이하`,
)

// 예산 최댓값 하나만 다룬다.
// 거래 유형이 변경되었을 때 기존 값이 새로운 상한을 넘지 않도록 제한한다.
const budget = computed({
  get: () => props.budgetAmount,

  set: (value) => {
    const normalized = Number(value)
    const limitedValue = Math.min(normalized, budgetSliderMax.value)

    emit('update:budgetAmount', limitedValue)
  },
})

const MONTHLY_RENT_SLIDER_MAX = 200

const monthlyRentMaxSlider = computed({
  get: () =>
      props.monthlyRentMax ?? MONTHLY_RENT_SLIDER_MAX,

  set: (value) => {
    const normalized = Number(value)

    emit('update:monthlyRentMax', normalized)
  },
})

const monthlyRentMaxLabel = computed(() =>
    `${props.monthlyRentMax ?? MONTHLY_RENT_SLIDER_MAX}만원 이하`,
)

// 슬라이더 실제 범위에 맞춘 중간 눈금 위치
const middleBudgetMarkPosition = computed(() => {
  const min = 0.5
  const middle = isMonthlyRent.value ? 2.5 : 10
  const max = budgetSliderMax.value

  return `${((middle - min) / (max - min)) * 100}%`
})

const middleBudgetMarkLabel = computed(() =>
    isMonthlyRent.value ? '2.5억' : '10억',
)

const maximumBudgetMarkLabel = computed(() =>
    isMonthlyRent.value ? '5억' : '20억',
)
</script>

<template>
  <div class="condition-card budget-card">
    <div class="budget-card-title">
      <span
          class="budget-icon"
          aria-hidden="true"
      >
        ▣
      </span>

      <h2>{{ dealType }} 예산 설정</h2>
    </div>

    <!-- 추천 조건은 최솟값 없이 최대 예산만 사용한다. -->
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
          :max="budgetSliderMax"
          :step="budgetSliderStep"
          :aria-label="`${budgetUnitLabel} 최대`"
      />

      <div
          class="budget-marks"
          aria-hidden="true"
      >
        <span style="left: 0%">
          5천만원
        </span>

        <span :style="{ left: middleBudgetMarkPosition }">
          {{ middleBudgetMarkLabel }}
        </span>

        <span style="left: 100%">
          {{ maximumBudgetMarkLabel }}
        </span>
      </div>
    </div>

    <!-- 월세일 때만 월세 자체의 최대 금액을 추가로 받는다. -->
    <div
        v-if="isMonthlyRent"
        class="budget-section"
    >
      <div class="radius-control-head">
        <strong>월세 최대</strong>
        <em>{{ monthlyRentMaxLabel }}</em>
      </div>

      <!-- 월세는 5만원 단위로 선택한다. -->
      <input
          v-model.number="monthlyRentMaxSlider"
          class="radius-slider budget-slider"
          type="range"
          min="10"
          :max="MONTHLY_RENT_SLIDER_MAX"
          step="5"
          aria-label="월세 최대"
      />

      <div
          class="budget-marks"
          aria-hidden="true"
      >
        <span style="left: 0%">
          10만원
        </span>

        <span style="left: 47.37%">
          100만원
        </span>

        <span style="left: 100%">
          200만원
        </span>
      </div>
    </div>

    <div class="condition-actions budget-actions">
      <button
          type="button"
          class="btn btn-outline-secondary"
          @click="emit('prev')"
      >
        이전
      </button>

      <button
          type="button"
          class="btn btn-primary"
          @click="emit('next')"
      >
        다음 →
      </button>
    </div>
  </div>
</template>
