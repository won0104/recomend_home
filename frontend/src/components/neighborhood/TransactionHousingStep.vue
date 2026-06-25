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

/**
 * 현재 동네 추천 로직은 아파트 거래 데이터만 추천 조건으로 사용한다.
 * 그래서 오피스텔/주택 버튼은 UI에 남겨두되, 사용자가 누르면 준비 중 안내만 보여주고
 * 실제 선택값은 바꾸지 않는다. 이렇게 해야 아직 지원하지 않는 주거 형태가 API 요청에 섞이지 않는다.
 */
function selectHousingType(option) {
  if (option.value !== '아파트') {
    alert('아직 추가 중이에요!')
    return
  }

  emit('update:housingType', option.value)
}
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
          @click="selectHousingType(option)"
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
