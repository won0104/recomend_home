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
  // id는 백엔드 SurveyCategory enum 이름과 동일해야 한다.
  // 이 값을 그대로 전송하면 별도의 화면용 코드 변환 없이 점수 계산기로 연결된다.
  { id: 'CAFE', icon: '☕', title: '카페·외식' },
  { id: 'CULTURE', icon: '⌁', title: '운동·문화' },
  { id: 'SHOPPING', icon: '🛒', title: '장보기·생활편의' },
  { id: 'SCHOOL', icon: '▱', title: '학교·교육' },
  { id: 'PARK', icon: '♣', title: '공원·산책' },
  { id: 'MEDICAL', icon: '⚕', title: '의료' },
  { id: 'PET', icon: '🐾', title: '반려인' }
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
  // id가 SurveyCategory 이름과 같으므로 선택 순서도 그대로 백엔드 1·2·3순위가 된다.
  priorities: selectedLifestyleIds.value,
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
  // 백엔드 점수 서비스는 1·2·3순위 가중치를 적용하므로 정확히 3개를 선택해야 한다.
  if (selectedLifestyleIds.value.length !== 3) {
    alert('생활 우선순위를 정확히 3개 선택해주세요.')
    return
  }

  recommendLoading.value = true

  try {
    // 응답의 data.recommendations에는 코드·지역명·점수·순위가 담긴 최종 상위 3개가 들어 있다.
    const response = await recommendNeighborhood(recommendationCondition.value)
    const recommendations = response?.data?.recommendations ?? []

    // 결과 화면을 붙이기 전에도 브라우저 개발자 도구에서 추천 결과를 즉시 확인할 수 있게 출력한다.
    console.table(recommendations.map((recommendation) => ({
      rank: recommendation.rank,
      dongCode: recommendation.lawdCd,
      name: [recommendation.sidoName, recommendation.sigunguName, recommendation.dongName]
        .filter(Boolean)
        .join(' '),
      score: recommendation.score
    })))
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
