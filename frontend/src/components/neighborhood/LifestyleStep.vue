<script setup>
import { computed } from 'vue'

const props = defineProps({
  lifestyleOptions: { type: Array, required: true },
  selectedLifestyleIds: { type: Array, required: true },
  lifestyleMood: { type: String, required: true },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:selectedLifestyleIds',
  'update:lifestyleMood',
  'prev',
  'submit'
])

const rankedLifestyleOptions = computed(() => props.selectedLifestyleIds
  .map((id) => props.lifestyleOptions.find((option) => option.id === id))
  .filter(Boolean))

function toggleLifestyle(id) {
  const next = [...props.selectedLifestyleIds]
  const currentIndex = next.indexOf(id)

  if (currentIndex >= 0) {
    next.splice(currentIndex, 1)
  } else if (next.length < 3) {
    next.push(id)
  }

  // 선택 순서를 유지해 1, 2, 3순위 추천 조건으로 사용합니다.
  emit('update:selectedLifestyleIds', next)
}

function lifestyleRank(id) {
  const index = props.selectedLifestyleIds.indexOf(id)
  return index >= 0 ? `${index + 1}순위` : ''
}
</script>

<template>
  <div class="lifestyle-card">
    <aside class="lifestyle-side">
      <div>
        <h2>중요한 생활환경을 순서대로 선택해주세요.</h2>
        <p>선택한 순서가 우선순위가 되며, 최대 3개까지 선택할 수 있어요.</p>
      </div>

      <div class="ai-helper-box">
        선택이 어렵다면<br />
        <strong>AI에게 한 문장으로 물어보기 ✨</strong>
      </div>

      <div class="priority-box">
        <strong>선택 현황</strong>
        <p v-if="rankedLifestyleOptions.length === 0" class="priority-empty">아직 선택한 항목이 없습니다.</p>
        <ol>
          <li v-for="(option, index) in rankedLifestyleOptions" :key="option.id">
            <span>{{ index + 1 }}순위</span>{{ option.title }}
          </li>
        </ol>
      </div>
    </aside>

    <section class="lifestyle-main">
      <!--
        선택지 목록은 이 컴포넌트에 고정하지 않고 부모 View에서 받는다.
        현재는 카페·외식, 운동·문화, 장보기·생활편의, 학교·교육, 공원·산책, 의료, 반려인의 7개다.
        v-for를 사용하므로 부모 목록에 7개가 전달되면 화면도 자동으로 7개를 렌더링한다.
      -->
      <div class="lifestyle-grid">
        <button
          v-for="option in lifestyleOptions"
          :key="option.id"
          type="button"
          class="lifestyle-option"
          :class="{ 'is-selected': selectedLifestyleIds.includes(option.id) }"
          @click="toggleLifestyle(option.id)"
        >
          <span v-if="lifestyleRank(option.id)" class="rank-badge">{{ lifestyleRank(option.id) }}</span>
          <span class="lifestyle-icon" aria-hidden="true">{{ option.icon }}</span>
          <strong>{{ option.title }}</strong>
        </button>
      </div>

      <div class="lifestyle-question">
        <strong>공원·산책에서 무엇이 더 중요한가요?</strong>
        <div class="lifestyle-mood-options" role="group" aria-label="공원 산책 세부 선호">
          <button type="button" :class="{ 'is-selected': lifestyleMood === 'green' }" @click="emit('update:lifestyleMood', 'green')">자연풍경 공원</button>
          <button type="button" :class="{ 'is-selected': lifestyleMood === 'quiet' }" @click="emit('update:lifestyleMood', 'quiet')">조용히 걷는 동네</button>
          <button type="button" :class="{ 'is-selected': lifestyleMood === 'activity' }" @click="emit('update:lifestyleMood', 'activity')">규모가 큰 공원</button>
        </div>
      </div>

      <!-- 부모 View가 모든 조건을 모아 최종 추천 API를 호출할 수 있게 submit 이벤트만 올립니다. -->
      <div class="lifestyle-actions">
        <button type="button" class="btn btn-outline-secondary" @click="emit('prev')">이전</button>
        <button type="button" class="btn btn-primary" :disabled="submitting" @click="emit('submit')">
          {{ submitting ? '후보 찾는 중' : '동네 추천받기 ✨' }}
        </button>
      </div>

      <p class="lifestyle-note">
        선택한 항목과 우선순위에 따라 추천 상권과 환경을 분석하여 가장 적합한 동네를 추천해드립니다.
      </p>
    </section>
  </div>
</template>
