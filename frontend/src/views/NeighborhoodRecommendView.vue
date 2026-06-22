<script setup>
import { computed, ref } from 'vue'
import BudgetStep from '../components/neighborhood/BudgetStep.vue'
import LifestyleStep from '../components/neighborhood/LifestyleStep.vue'
import PlaceSearchStep from '../components/neighborhood/PlaceSearchStep.vue'
import RadiusStep from '../components/neighborhood/RadiusStep.vue'
import StepIndicator from '../components/neighborhood/StepIndicator.vue'
import { recommendNeighborhood } from '../api/neighborhoods'

const selectedPlace = ref(null)
const screen = ref('search')

const commuteRadius = ref(30)
const budgetAmount = ref(3)
const selectedLifestyleIds = ref([])
const lifestyleMood = ref('green')
const recommendLoading = ref(false)

const lifestyleOptions = [
  { id: 'cafe', icon: '☕', title: '카페·외식' },
  { id: 'health', icon: '⌁', title: '운동·여가' },
  { id: 'market', icon: '🛒', title: '장보기·생활편의' },
  { id: 'school', icon: '▱', title: '학교·교육' },
  { id: 'park', icon: '♣', title: '공원·산책' },
  { id: 'culture', icon: '□', title: '문화생활' }
]

const currentStep = computed(() => {
  if (screen.value === 'budget') return 2
  if (screen.value === 'lifestyle') return 3
  return 1
})
const selectedAddress = computed(() => selectedPlace.value?.road_address_name || selectedPlace.value?.address_name || '')
const maxPrice = computed(() => Number(budgetAmount.value) >= 10 ? null : Math.round(Number(budgetAmount.value) * 10000))

const recommendationCondition = computed(() => ({
  latitude: Number(selectedPlace.value?.y),
  longitude: Number(selectedPlace.value?.x),
  radiusKm: commuteRadius.value,
  maxPrice: maxPrice.value,
  priorities: selectedLifestyleIds.value.map((category) => category.toUpperCase()),
  lifestyleMood: lifestyleMood.value
}))

function handlePlaceSelect(place) {
  selectedPlace.value = place
}

function openRadiusStep(place) {
  selectedPlace.value = place
  screen.value = 'radius'
}

function openBudgetStep() {
  screen.value = 'budget'
}

function openLifestyleStep() {
  screen.value = 'lifestyle'
}

function backToSearch() {
  screen.value = 'search'
}

function backToRadiusStep() {
  screen.value = 'radius'
}

function backToBudgetStep() {
  screen.value = 'budget'
}

async function submitRecommendation() {
  recommendLoading.value = true

  try {
    // 백엔드는 반경/예산으로 매매 후보를 걸러내고, 다음 추천 로직에 필요한 법정동 후보를 함께 내려줍니다.
    await recommendNeighborhood(recommendationCondition.value)
  } catch (error) {
    console.error(error?.response?.data?.message || '동네 추천 후보를 불러오지 못했습니다.')
  } finally {
    recommendLoading.value = false
  }
}
</script>

<template>
  <main class="place-finder-page">
    <template v-if="screen === 'search'">
      <section class="place-finder-hero">
        <div class="place-finder-badge">맞춤 동네 추천 서비스</div>
        <h1>
          나에게 딱 맞는<br />
          <span>완벽한 동네</span>를 찾아드립니다
        </h1>
      </section>

      <div class="place-finder-shell">
        <PlaceSearchStep @select-place="handlePlaceSelect" @next="openRadiusStep" />
      </div>
    </template>

    <template v-else>
      <section class="place-finder-hero place-condition-hero">
        <div class="place-finder-badge">동네 추천</div>
        <h1>
          나만의 조건으로<br />
          <span>완벽한 동네</span>를 찾아보세요
        </h1>
        <p>3단계 설정만으로 당신에게 딱 맞는 동네를 추천받으세요</p>
      </section>

      <section class="condition-setup-shell" aria-label="동네 추천 조건 설정">
        <StepIndicator :current-step="currentStep" />

        <RadiusStep
          v-if="screen === 'radius'"
          v-model:commute-radius="commuteRadius"
          :selected-place="selectedPlace"
          :selected-address="selectedAddress"
          @prev="backToSearch"
          @next="openBudgetStep"
        />

        <BudgetStep
          v-else-if="screen === 'budget'"
          v-model:budget-amount="budgetAmount"
          @prev="backToRadiusStep"
          @next="openLifestyleStep"
        />

        <LifestyleStep
          v-else
          v-model:selected-lifestyle-ids="selectedLifestyleIds"
          v-model:lifestyle-mood="lifestyleMood"
          :lifestyle-options="lifestyleOptions"
          :submitting="recommendLoading"
          @prev="backToBudgetStep"
          @submit="submitRecommendation"
        />
      </section>
    </template>
  </main>
</template>
