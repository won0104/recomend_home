<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import BudgetStep from '../components/neighborhood/BudgetStep.vue'
import LifestyleStep from '../components/neighborhood/LifestyleStep.vue'
import NeighborhoodDetailMap from '../components/neighborhood/NeighborhoodDetailMap.vue'
import NeighborhoodResultMap from '../components/neighborhood/NeighborhoodResultMap.vue'
import PlaceSearchStep from '../components/neighborhood/PlaceSearchStep.vue'
import RadiusStep from '../components/neighborhood/RadiusStep.vue'
import StepIndicator from '../components/neighborhood/StepIndicator.vue'
import TransactionHousingStep from '../components/neighborhood/TransactionHousingStep.vue'
import { addFavorite, deleteFavorite, fetchFavorites } from '../api/favorites'
import { fetchHouses } from '../api/houses'
import { recommendNeighborhood, summarizeNeighborhood } from '../api/neighborhoods'
import { number } from '../utils/format'

// App.vue가 전달하는 로그인 정보를 이용한다. 비회원은 즐겨찾기 버튼을 보지 않는다.
const props = defineProps({
  user: { type: Object, default: null },
})

const route = useRoute()
const router = useRouter()

// 이 탭에서만 마지막 추천을 복원한다. 탭을 닫으면 자동으로 사라져 오래된 추천이 남지 않는다.
const RECOMMENDATION_SESSION_KEY = 'neighborhood-recommendation-state-v1'
const RESTORABLE_SCREENS = new Set(['search', 'radius', 'transaction', 'budget', 'lifestyle', 'result', 'detail'])

const selectedPlace = ref(null)
const screen = ref('search')

const commuteRadius = ref(15)
const budgetAmount = ref(3)
const dealType = ref('매매')
const housingType = ref('아파트')
// 월세 선택 시 월세 상한은 200만원을 기본값으로 사용한다.
const monthlyRentMax = ref(200)
const selectedLifestyleIds = ref([])
const lifestyleMood = ref('green')
const recommendLoading = ref(false)
const recommendations = ref([])
const totalCandidateCount = ref(0)
// 추천 결과와 별개로, 로그인 사용자가 이미 저장한 법정동 코드를 관리한다.
const favoriteKeys = ref([])
// 같은 동을 연속으로 등록/삭제하지 않도록 요청 중인 카드만 잠시 비활성화한다.
const favoriteBusyKeys = ref([])
const savedDealType = ref('매매') // 추천 요청 시점의 거래 유형 스냅샷

// 로딩 단계 애니메이션
const loadingStage = ref(0)
const loadingMessages = [
  '기준 위치 주변 동네를 탐색하는 중...',
  '후보 동네의 생활 환경을 분석하는 중...',
  '후보 동네 중 최적 3곳을 선정하는 중...',
]
let _loadingTimer = null

// 상세 화면
const selectedNeighborhood = ref(null)
const houseList = ref([])
const houseLoading = ref(false)
const aiSummary = ref(null)
const aiSummaryLoading = ref(false)
const highlightedAptSeq = ref(null) // 행 hover
const pinnedAptSeq = ref(null)      // 지도 핀 클릭으로 고정된 행

/** URL의 step은 브라우저 뒤로가기용, sessionStorage는 새로고침 복원용으로 역할을 나눈다. */
function moveToScreen(nextScreen, replaceHistory = false) {
  if (!RESTORABLE_SCREENS.has(nextScreen)) return
  screen.value = nextScreen

  const query = { ...route.query }
  if (nextScreen === 'search') delete query.step
  else query.step = nextScreen
  const navigate = replaceHistory ? router.replace : router.push
  navigate({ query })
}

/** API 응답을 다시 요청하지 않고 결과 화면을 복원할 수 있도록 필요한 화면 상태만 저장한다. */
function saveRecommendationState() {
  const state = {
    screen: screen.value,
    selectedPlace: selectedPlace.value,
    commuteRadius: commuteRadius.value,
    budgetAmount: budgetAmount.value,
    dealType: dealType.value,
    housingType: housingType.value,
    monthlyRentMax: monthlyRentMax.value,
    selectedLifestyleIds: selectedLifestyleIds.value,
    lifestyleMood: lifestyleMood.value,
    recommendations: recommendations.value,
    totalCandidateCount: totalCandidateCount.value,
    savedDealType: savedDealType.value,
    selectedNeighborhood: selectedNeighborhood.value,
    houseList: houseList.value,
    aiSummary: aiSummary.value,
  }
  sessionStorage.setItem(RECOMMENDATION_SESSION_KEY, JSON.stringify(state))
}

function clearRecommendationState() {
  sessionStorage.removeItem(RECOMMENDATION_SESSION_KEY)
}

/** sessionStorage의 값은 사용자가 수정할 수도 있으므로 읽기 실패 시 안전하게 처음 화면으로 돌아간다. */
function restoreRecommendationState() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(RECOMMENDATION_SESSION_KEY) || 'null')
    if (!saved || !RESTORABLE_SCREENS.has(saved.screen)) return false
    selectedPlace.value = saved.selectedPlace ?? null
    commuteRadius.value = saved.commuteRadius ?? 15
    budgetAmount.value = saved.budgetAmount ?? 3
    dealType.value = saved.dealType ?? '매매'
    housingType.value = saved.housingType ?? '아파트'
    monthlyRentMax.value = saved.monthlyRentMax ?? 200
    selectedLifestyleIds.value = saved.selectedLifestyleIds ?? []
    lifestyleMood.value = saved.lifestyleMood ?? 'green'
    recommendations.value = saved.recommendations ?? []
    totalCandidateCount.value = saved.totalCandidateCount ?? 0
    savedDealType.value = saved.savedDealType ?? dealType.value
    selectedNeighborhood.value = saved.selectedNeighborhood ?? null
    houseList.value = saved.houseList ?? []
    aiSummary.value = saved.aiSummary ?? null
    screen.value = saved.screen
    return true
  } catch {
    clearRecommendationState()
    return false
  }
}

// 지도에 전달할 하이라이트: 핀 고정이 있으면 핀 우선, 없으면 hover
const mapHighlight = computed(() => pinnedAptSeq.value || highlightedAptSeq.value)

function handleMapSelect(aptSeq) {
  pinnedAptSeq.value = pinnedAptSeq.value === aptSeq ? null : aptSeq
  if (pinnedAptSeq.value) {
    nextTick(() => {
      document.querySelector(`[data-apt-seq="${aptSeq}"]`)
        ?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    })
  }
}

const lifestyleOptions = [
  { id: 'CAFE',     icon: '☕', title: '카페·외식' },
  { id: 'CULTURE',  icon: '🏃', title: '운동·취미' },
  { id: 'SHOPPING', icon: '🛒', title: '장보기·생활편의' },
  { id: 'SCHOOL',   icon: '🏫', title: '학교·교육' },
  { id: 'PARK',     icon: '🌳', title: '공원·산책' },
  { id: 'MEDICAL',  icon: '🏥', title: '의료' },
  { id: 'PET',      icon: '🐾', title: '반려동물' },
  { id: 'TRANSPORT', icon: '🚇', title: '교통' },
]

const categoryLabel = {
  CAFE: '카페·외식', CULTURE: '운동·여가', SHOPPING: '장보기·생활편의',
  SCHOOL: '학교·교육', PARK: '공원·산책', MEDICAL: '의료', PET: '반려인',
  TRANSPORT: '교통',
}

const categoryDesc = {
  SCHOOL:   { high: '학교가 많아 교육 환경이 우수합니다.', mid: '교육 시설이 평균 수준입니다.', low: '학교가 다소 부족합니다.' },
  PARK:     { high: '공원이 가까워 산책·야외 활동이 편합니다.', mid: '공원 접근성이 보통입니다.', low: '공원이 다소 부족합니다.' },
  CAFE:     { high: '카페와 식당이 풍부해 외식이 편리합니다.', mid: '카페·외식 환경이 보통입니다.', low: '카페·식당이 다소 부족합니다.' },
  CULTURE:  { high: '문화·스포츠 시설이 풍부합니다.', mid: '문화시설이 평균 수준입니다.', low: '문화시설이 다소 부족합니다.' },
  SHOPPING: { high: '마트와 편의시설이 가까워 생활이 편리합니다.', mid: '생활편의시설이 평균 수준입니다.', low: '생활편의시설이 다소 부족합니다.' },
  MEDICAL:  { high: '병원·의원이 가까워 의료 접근성이 좋습니다.', mid: '의료시설이 평균 수준입니다.', low: '의료시설이 다소 부족합니다.' },
  PET:      { high: '동물병원·공원이 가까워 반려동물 생활이 쾌적합니다.', mid: '반려동물 인프라가 보통입니다.', low: '반려동물 관련 시설이 부족합니다.' },
  TRANSPORT: { high: '지하철과 버스 등 대중교통 선택지가 풍부합니다.', mid: '대중교통 여건이 보통입니다.', low: '대중교통 시설이 다소 부족합니다.' },
}

function scoreLevel(score) {
  if (score >= 65) return 'high'
  if (score >= 35) return 'mid'
  return 'low'
}

/**
 * 백엔드가 내려주는 카테고리 점수는 0~100점 범위이므로,
 * 프론트에서는 이 값을 사용자가 이해하기 쉬운 "상위 비율" 문장으로 바꿔 보여준다.
 *
 * 예) 95점 → 서울시 기준 상위 약 5%
 * 예) 68점 → 서울시 기준 상위 약 32%
 *
 * 실제 DB 순위를 다시 계산하는 값은 아니기 때문에 "약"이라는 표현을 붙인다.
 * 정확한 n위가 필요해지면 백엔드에서 카테고리별 전체 순위를 계산해 내려주는 방식으로 확장하면 된다.
 */
function scoreTopPercent(score) {
  const normalizedScore = Number(score ?? 0)
  if (normalizedScore <= 0) return null

  return Math.max(1, Math.min(100, Math.round(100 - normalizedScore)))
}

const strengthLabel = {
  PARK: '공원 접근 우수', CAFE: '카페·외식 밀집', SCHOOL: '학교 인접',
  SHOPPING: '생활편의 풍부', MEDICAL: '의료 인프라', CULTURE: '문화시설 우수', PET: '반려인 환경',
  TRANSPORT: '대중교통 편의',
}
const weaknessLabel = {
  PARK: '공원 다소 부족', CAFE: '카페 다소 부족', SCHOOL: '학교 다소 부족',
  SHOPPING: '편의시설 다소 부족', MEDICAL: '의료시설 다소 부족', CULTURE: '문화시설 다소 부족', PET: '반려 인프라 부족',
  TRANSPORT: '교통시설 다소 부족',
}

function getStrengthTags(categoryScores) {
  return categoryScores.filter((cs) => cs.score >= 65).map((cs) => strengthLabel[cs.category] ?? cs.category)
}
function getWeaknessTags(categoryScores) {
  return categoryScores.filter((cs) => cs.score < 40).map((cs) => weaknessLabel[cs.category] ?? cs.category)
}

const RANK_COLORS = { 1: '#2563EB', 2: '#22C55E', 3: '#F97316' }

const currentStep = computed(() => {
  if (screen.value === 'transaction') return 2
  if (screen.value === 'budget') return 3
  if (screen.value === 'lifestyle') return 4
  return 1
})
const selectedAddress = computed(() =>
  selectedPlace.value?.road_address_name || selectedPlace.value?.address_name || ''
)
const activeBudgetMax = computed(() => {
  // 월세 보증금은 5억도 실제 상한값으로 전송한다. 매매·전세만 기존처럼 20억을 "제한 없음"으로 둔다.
  if (dealType.value !== '월세' && Number(budgetAmount.value) >= 20) return null
  return Math.round(Number(budgetAmount.value) * 10000)
})
const budgetLabel = computed(() => {
  const maxLabel = Number(budgetAmount.value) >= 20 ? '제한 없음' : `${budgetAmount.value}억원 이하`
  if (dealType.value === '매매') return `매매가 ${maxLabel}`
  if (dealType.value === '전세') return `보증금 ${maxLabel}`
  return `보증금 ${maxLabel}, 월세 ${number(monthlyRentMax.value ?? 200)}만원 이하`
})
/**
 * 매매·전세·월세는 금액 구조가 다르다. 매매는 평균 매매가, 전세는 평균 보증금,
 * 월세는 평균 보증금과 평균 월세를 함께 보여 사용자가 실제 계약 조건을 비교할 수 있게 한다.
 */
function averageTransactionText(house) {
  if (savedDealType.value === '매매') {
    if (!house.avgPrice) return '-'
    return house.avgPrice >= 10000
      ? `${(house.avgPrice / 10000).toFixed(1)}억`
      : `${number(house.avgPrice)}만`
  }

  if (!house.avgDeposit) return '-'
  const deposit = house.avgDeposit >= 10000
    ? `${(house.avgDeposit / 10000).toFixed(1)}억`
    : `${number(house.avgDeposit)}만`
  if (savedDealType.value === '전세') return `보증금 ${deposit}`
  return `보증금 ${deposit} / 월세 ${number(house.avgMonthlyRent || 0)}만`
}

const baseLocation = computed(() =>
  selectedPlace.value ? { lat: Number(selectedPlace.value.y), lng: Number(selectedPlace.value.x) } : null
)
const priorityLabels = computed(() =>
  selectedLifestyleIds.value.map((id) => lifestyleOptions.find((o) => o.id === id)?.title ?? id)
)
const recommendationCondition = computed(() => ({
  latitude: Number(selectedPlace.value?.y),
  longitude: Number(selectedPlace.value?.x),
  radiusKm: commuteRadius.value,
  dealType: dealType.value,
  dealAmountMax: dealType.value === '매매' ? activeBudgetMax.value : null,
  depositMax: dealType.value !== '매매' ? activeBudgetMax.value : null,
  monthlyRentMax: dealType.value === '월세' ? (monthlyRentMax.value ?? 200) : null,
  housingType: housingType.value,
  priorities: selectedLifestyleIds.value,
  lifestyleMood: lifestyleMood.value,
}))

function handlePlaceSelect(place) { selectedPlace.value = place }
function openRadiusStep(place) { selectedPlace.value = place; moveToScreen('radius') }
function openTransactionStep() { moveToScreen('transaction') }
function openBudgetStep() {
  // 매매/전세에서 큰 예산을 골랐다가 월세로 바꿔도, 월세 전용 상한을 넘는 값이 남지 않게 보정한다.
  if (dealType.value === '월세') {
    budgetAmount.value = Math.min(Number(budgetAmount.value), 5)
    monthlyRentMax.value = Math.min(Number(monthlyRentMax.value ?? 200), 200)
  }
  moveToScreen('budget')
}
function openLifestyleStep() { moveToScreen('lifestyle') }
function backToSearch() { clearRecommendationState(); moveToScreen('search') }
function backToRadiusStep() { moveToScreen('radius') }
function backToTransactionStep() { moveToScreen('transaction') }
function backToBudgetStep() { moveToScreen('budget') }
function backToLifestyleStep() { moveToScreen('lifestyle') }
function backToResult() { moveToScreen('result') }

/**
 * 관심지역 테이블은 lawd_cd를 고유 식별자로 사용한다.
 * 추천 응답의 lawdCd도 같은 법정동 코드이므로 별도의 임의 key를 만들지 않는다.
 */
function favoriteKey(rec) {
  return String(rec.lawdCd)
}

function isFavorite(rec) {
  return favoriteKeys.value.includes(favoriteKey(rec))
}

function isFavoriteBusy(rec) {
  return favoriteBusyKeys.value.includes(favoriteKey(rec))
}

/** 로그인한 사용자의 기존 관심지역을 읽어 카드의 별표 상태를 맞춘다. */
async function loadFavorites() {
  if (!props.user) {
    favoriteKeys.value = []
    return
  }

  try {
    const response = await fetchFavorites()
    const favorites = Array.isArray(response.data) ? response.data : []
    favoriteKeys.value = favorites.map((favorite) => String(favorite.key))
  } catch {
    // 목록 로딩 실패가 추천 결과 화면 전체를 막지는 않도록 빈 목록으로 처리한다.
    favoriteKeys.value = []
  }
}

/**
 * 카드의 별표 버튼으로 관심지역을 등록하거나 해제한다.
 * 템플릿의 click.stop 덕분에 별표를 눌러도 카드 상세 화면으로 이동하지 않는다.
 */
async function toggleFavorite(rec) {
  if (!props.user || isFavoriteBusy(rec)) return

  const key = favoriteKey(rec)
  favoriteBusyKeys.value = [...favoriteBusyKeys.value, key]
  try {
    if (isFavorite(rec)) {
      await deleteFavorite(key)
      favoriteKeys.value = favoriteKeys.value.filter((favorite) => favorite !== key)
      return
    }

    await addFavorite({
      key,
      sido: rec.sidoName ?? '',
      sigungu: rec.sigunguName ?? '',
      emd: rec.dongName ?? '',
      lawdCd: key,
    })
    favoriteKeys.value = [...favoriteKeys.value, key]
  } catch (error) {
    // 다른 화면에서 이미 변경한 경우까지 반영하도록, 실패 후 서버의 실제 목록을 다시 읽는다.
    await loadFavorites()
    console.error(error?.response?.data?.message || '즐겨찾기를 변경하지 못했습니다.')
  } finally {
    favoriteBusyKeys.value = favoriteBusyKeys.value.filter((favorite) => favorite !== key)
  }
}

/**
 * 추천 상세 화면의 AI 요약을 백엔드에서 생성한다.
 * 백엔드는 이 요청에 담긴 추천 점수와 RAG 문서 검색 결과를 조합해
 * 사용자가 이해하기 쉬운 비유가 섞인 요약 문장으로 바꿔준다.
 */
async function loadAiSummary(rec) {
  if (!rec) {
    aiSummary.value = null
    return
  }

  aiSummaryLoading.value = true
  try {
    const response = await summarizeNeighborhood({
      neighborhood: rec,
      dealType: savedDealType.value || dealType.value,
      budgetLabel: budgetLabel.value,
      houseCount: houseList.value.length,
      priorities: selectedLifestyleIds.value,
    })
    aiSummary.value = response?.data ?? null
  } catch (error) {
    console.error(error?.response?.data?.message || 'AI 동네 요약을 불러오지 못했습니다.')
    aiSummary.value = {
      lead: `${rec.dongName}은 현재 추천 점수 기준으로 생활환경을 해석할 수 있는 후보지예요.`,
      points: [
        'AI/RAG 요약을 불러오지 못해 화면의 생활 분석 점수 중심으로 확인해주세요.',
        '점수는 공개 데이터 기반의 상대적 해석이며, 실제 생활 동선은 지도와 매물 목록을 함께 보는 것이 좋아요.',
      ],
      ragDocumentFound: false,
      caution: '요약 API 호출에 실패해 임시 안내 문구를 표시했습니다.',
    }
  } finally {
    aiSummaryLoading.value = false
  }
}

async function openDetail(rec) {
  selectedNeighborhood.value = rec
  aiSummary.value = null
  moveToScreen('detail')
  houseLoading.value = true
  houseList.value = []
  pinnedAptSeq.value = null
  highlightedAptSeq.value = null
  try {
    // 동네 후보를 걸러낸 예산·거래·주거 조건을 매물 목록에도 그대로 적용한다.
    const res = await fetchHouses({
      lawdCd: rec.lawdCd,
      dealType: dealType.value,
      housingType: housingType.value,
      dealAmountMax: dealType.value === '매매' ? activeBudgetMax.value : undefined,
      depositMax: dealType.value !== '매매' ? activeBudgetMax.value : undefined,
      monthlyRentMax: dealType.value === '월세' ? (monthlyRentMax.value ?? 200) : undefined,
    })
    houseList.value = Array.isArray(res.data) ? res.data : []
  } catch {
    houseList.value = []
  } finally {
    houseLoading.value = false
    await loadAiSummary(rec)
  }
}

async function submitRecommendation() {
  if (selectedLifestyleIds.value.length !== 3) {
    alert('생활 우선순위를 정확히 3개 선택해주세요.')
    return
  }
  recommendLoading.value = true
  savedDealType.value = dealType.value
  loadingStage.value = 0
  _loadingTimer = setInterval(() => {
    if (loadingStage.value < loadingMessages.length - 1) loadingStage.value++
  }, 1800)
  try {
    const response = await recommendNeighborhood(recommendationCondition.value)
    recommendations.value = response?.data?.recommendations ?? []
    totalCandidateCount.value = response?.data?.totalCount ?? 0
    moveToScreen('result')
  } catch (error) {
    console.error(error?.response?.data?.message || '동네 추천 후보를 불러오지 못했습니다.')
  } finally {
    clearInterval(_loadingTimer)
    recommendLoading.value = false
  }
}

// 조건·결과가 바뀔 때마다 같은 탭의 sessionStorage에 저장한다. API 호출이나 DB 저장은 하지 않는다.
watch([
  screen, selectedPlace, commuteRadius, budgetAmount, dealType, housingType, monthlyRentMax,
  selectedLifestyleIds, lifestyleMood, recommendations, totalCandidateCount, savedDealType,
  selectedNeighborhood, houseList, aiSummary,
], saveRecommendationState, { deep: true })

// 브라우저 뒤로가기/앞으로가기는 URL query의 step만 바꾼다. 그 변경을 화면 단계에 반영한다.
watch(() => route.query.step, (step) => {
  if (!step) {
    screen.value = 'search'
    return
  }
  if (!RESTORABLE_SCREENS.has(step)) {
    moveToScreen('search', true)
    return
  }
  // 결과·상세는 저장된 추천 데이터가 있어야만 표시할 수 있다.
  if ((step === 'result' || step === 'detail') && recommendations.value.length === 0) {
    moveToScreen('search', true)
    return
  }
  if (step === 'detail' && !selectedNeighborhood.value) {
    moveToScreen('result', true)
    return
  }
  screen.value = step
})

// 로그인/로그아웃 직후에도 새로고침 없이 즐겨찾기 버튼 상태를 맞춘다.
watch(() => props.user, loadFavorites, { immediate: true })

onMounted(() => {
  const restored = restoreRecommendationState()
  const requestedStep = route.query.step

  // 새로고침은 URL의 단계와 sessionStorage의 결과를 조합해 즉시 복원한다.
  if (requestedStep && RESTORABLE_SCREENS.has(requestedStep)) {
    if ((requestedStep === 'result' || requestedStep === 'detail') && !restored) {
      moveToScreen('search', true)
      return
    }
    if (requestedStep === 'detail' && !selectedNeighborhood.value) {
      moveToScreen('result', true)
      return
    }
    screen.value = requestedStep
  } else if (restored && screen.value !== 'search') {
    // 주소에 step이 없더라도 같은 탭의 마지막 단계가 있다면 URL도 복원 상태와 일치시킨다.
    moveToScreen(screen.value, true)
  }

  // 상세 화면을 새로고침으로 복원했는데 요약 결과가 저장되어 있지 않다면,
  // 이미 복원된 추천 동네와 매물 수를 기준으로 AI/RAG 요약만 다시 요청한다.
  if (screen.value === 'detail' && selectedNeighborhood.value && !aiSummary.value) {
    loadAiSummary(selectedNeighborhood.value)
  }
})
</script>

<template>
  <main class="place-finder-page">

    <!-- ① 장소 검색 -->
    <template v-if="screen === 'search'">
      <section class="place-finder-hero">
<!--        <div class="place-finder-badge">맞춤 동네 추천 서비스</div>-->
        <h1>나에게 딱 맞는<br /><span>완벽한 동네</span>를 찾아드립니다</h1>
        <p>출퇴근 거리, 예산, 생활 성향까지 고려한<br />맞춤형 동네 추천으로 당신의 새로운 보금자리를 찾아보세요.</p>
      </section>
      <div class="place-finder-shell">
        <PlaceSearchStep @select-place="handlePlaceSelect" @next="openRadiusStep" />
      </div>
    </template>

    <!-- ② 추천 결과 -->
    <template v-else-if="screen === 'result'">
      <div class="nr-result-page">
        <section class="nr-result-left">
          <p class="nr-result-eyebrow">동네 추천</p>
          <h2 class="nr-result-title">회원님의 조건에 맞는 <strong>추천 동네</strong>에요</h2>
          <div class="nr-conditions">
            <span>기준 위치까지 직선거리 {{ commuteRadius }}km 이내</span>
            <span class="nr-cond-sep">|</span>
            <span>예산 {{ budgetLabel }}</span>
            <span class="nr-cond-sep">|</span>
            <span>거래: {{ dealType }}</span>
            <span class="nr-cond-sep">|</span>
            <span>주거형태: {{ housingType }}</span>
          </div>
          <div class="nr-priorities">
            <span v-for="(label, i) in priorityLabels" :key="i" class="nr-priority-tag">
              <em>{{ ['①', '②', '③'][i] }}</em> {{ label }}
            </span>
          </div>

          <div v-if="recommendations.length === 0" class="nr-empty">
            조건에 맞는 동네를 찾지 못했습니다. 반경이나 예산을 조정해보세요.
          </div>

          <ul v-else class="nr-card-list">
            <li
              v-for="rec in recommendations"
              :key="rec.lawdCd"
              class="nr-card"
              :class="`nr-card--rank${rec.rank}`"
              role="button"
              tabindex="0"
              @click="openDetail(rec)"
              @keydown.enter="openDetail(rec)"
            >
              <div class="nr-card__rank">{{ rec.rank }}위</div>
              <div class="nr-card__body">
                <div class="nr-card__name-row">
                  <span class="nr-card__dong">{{ rec.dongName }}</span>
                  <span class="nr-card__district">{{ [rec.sidoName, rec.sigunguName].filter(Boolean).join(' ') }}</span>
                  <!-- 카드 전체의 상세 이동과 분리하기 위해 click.stop을 사용한다. -->
                  <button
                    v-if="user"
                    type="button"
                    class="nr-card__favorite"
                    :class="{ 'nr-card__favorite--active': isFavorite(rec) }"
                    :disabled="isFavoriteBusy(rec)"
                    :aria-label="isFavorite(rec) ? '즐겨찾기 삭제' : '즐겨찾기 추가'"
                    @click.stop="toggleFavorite(rec)"
                  >
                    {{ isFavorite(rec) ? '★' : '☆' }}
                  </button>
                </div>
                <div class="nr-card__meta">
                  <span>생활 성향 적합도 <strong>{{ rec.score.toFixed(0) }}%</strong></span>
                </div>
                <div class="nr-card__tags" v-if="getStrengthTags(rec.categoryScores).length">
                  <span class="nr-card__tags-label">주요 강점:</span>
                  <span v-for="tag in getStrengthTags(rec.categoryScores)" :key="tag" class="nr-card__tag nr-card__tag--strength">{{ tag }}</span>
                </div>
                <div class="nr-card__tags" v-if="getWeaknessTags(rec.categoryScores).length">
                  <span class="nr-card__tags-label">주요 약점:</span>
                  <span v-for="tag in getWeaknessTags(rec.categoryScores)" :key="tag" class="nr-card__tag nr-card__tag--weakness">{{ tag }}</span>
                </div>
                <span class="nr-card__detail-hint">클릭해서 매물 보기 →</span>
              </div>
            </li>
          </ul>

          <!-- 추천 점수의 해석 기준은 카드 목록을 읽은 직후 확인할 수 있게 카드 아래에 둔다. -->
          <p class="nr-disclaimer">추천 점수는 공개된 공공데이터를 기반으로 분석되며, 실제 생활 환경과 다를 수 있습니다.</p>
          <button class="nr-retry-btn" @click="backToLifestyleStep">다시 설정하기</button>
        </section>

        <aside class="nr-result-right">
          <!-- 지도와 관련된 분석 안내는 지도 바로 위에 배치해, 카드 목록과 시각적으로 분리한다. -->
          <div class="nr-map-summary">
            <p v-if="totalCandidateCount > 0" class="nr-candidate-note">
              <svg class="nr-candidate-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 21s7-6.1 7-12a7 7 0 1 0-14 0c0 5.9 7 12 7 12Z" />
                <circle cx="12" cy="9" r="2.5" />
              </svg>
              총 <strong>{{ totalCandidateCount }}</strong>개 동네를 분석하여 최적 <strong>{{ recommendations.length }}</strong>곳을 선정했습니다.
            </p>
          </div>
          <NeighborhoodResultMap :base-location="baseLocation" :neighborhoods="recommendations" class="nr-result-map" />
        </aside>
      </div>
    </template>

    <!-- ③ 동네 상세 -->
    <template v-else-if="screen === 'detail' && selectedNeighborhood">
      <div class="nr-detail-page">

        <!-- 헤더 -->
        <div class="nr-detail-header">
          <button class="nr-back-btn" @click="backToResult">← 추천 목록으로</button>
          <div class="nr-detail-header__title">
            <span
              class="nr-detail-rank-badge"
              :style="{ background: RANK_COLORS[selectedNeighborhood.rank] }"
            >{{ selectedNeighborhood.rank }}위</span>
            <h2>{{ selectedNeighborhood.dongName }}</h2>
            <span class="nr-detail-district">
              {{ [selectedNeighborhood.sidoName, selectedNeighborhood.sigunguName].filter(Boolean).join(' ') }}
            </span>
          </div>
        </div>

        <!-- 2단 본문 -->
        <div class="nr-detail-body">

          <!-- 왼쪽: 생활 분석 + AI 요약 -->
          <section class="nr-detail-left">
            <div class="nr-detail-analysis-card">
              <h3 class="nr-detail-section-title">동네 생활 분석</h3>

              <ul class="nr-detail-score-list">
                <li
                  v-for="cs in selectedNeighborhood.categoryScores"
                  :key="cs.category"
                  class="nr-detail-score-item"
                >
                  <div class="nr-detail-score-item__head">
                    <span class="nr-detail-score-item__name">{{ categoryLabel[cs.category] ?? cs.category }}</span>
                    <span
                      class="nr-detail-score-item__level"
                      :class="`nr-detail-score-item__level--${scoreLevel(cs.score)}`"
                    >{{ { high: '우수', mid: '보통', low: '부족' }[scoreLevel(cs.score)] }}</span>
                    <span class="nr-detail-score-item__num">{{ cs.score.toFixed(0) }}점</span>
                  </div>
                  <div class="nr-detail-score-bar-wrap">
                    <div
                      class="nr-detail-score-bar"
                      :class="`nr-detail-score-bar--${scoreLevel(cs.score)}`"
                      :style="{ width: cs.score.toFixed(0) + '%' }"
                    ></div>
                  </div>
                  <p class="nr-detail-score-item__desc">
                    서울시 기준 상위
                    <strong v-if="scoreTopPercent(cs.score)" class="nr-detail-score-item__percent">
                      {{ scoreTopPercent(cs.score) }}%
                    </strong>
                    <span v-else class="nr-detail-score-item__percent">비교 데이터 부족</span>
<!--                    :-->
<!--                    {{ categoryDesc[cs.category]?.[scoreLevel(cs.score)] }}-->
                  </p>
                </li>
              </ul>
            </div>

            <section class="nr-detail-ai-summary-card">
              <div class="nr-detail-ai-summary-card__head">
                <h3 class="nr-detail-section-title">AI가 분석한 동네 요약</h3>
              </div>
              <div v-if="aiSummaryLoading" class="nr-detail-loading">AI가 점수와 RAG 문서를 함께 읽는 중...</div>
              <template v-else>
                <p class="nr-detail-ai-summary-lead">
                  {{ aiSummary?.lead || `${selectedNeighborhood.dongName}은 현재 설정한 조건을 기준으로 생활환경을 해석할 수 있는 후보지예요.` }}
                </p>
                <ul class="nr-detail-ai-summary-list">
                  <li
                    v-for="point in (aiSummary?.points || ['생활 분석 점수와 매물 조건을 함께 비교해 보세요.'])"
                    :key="point"
                  >
                    {{ point }}
                  </li>
                </ul>
                <p v-if="aiSummary?.caution" class="nr-detail-ai-summary-caution">
                  {{ aiSummary.caution }}
                </p>
              </template>
            </section>
          </section>

          <!-- 오른쪽: 지도 + 매물 목록 -->
          <section class="nr-detail-right">

            <!-- 지도 -->
            <div class="nr-detail-map-card">
              <NeighborhoodDetailMap
                :houses="houseList"
                :dong-address="[selectedNeighborhood.sidoName, selectedNeighborhood.sigunguName, selectedNeighborhood.dongName].filter(Boolean).join(' ')"
                :highlighted-apt-seq="mapHighlight"
                @select="handleMapSelect"
              />
            </div>

            <!-- 매물 목록 -->
            <div class="nr-detail-house-card">
              <h3 class="nr-detail-section-title">
                이 동네 매물
                <span class="nr-detail-house-count">{{ houseList.length }}건</span>
              </h3>

              <div v-if="houseLoading" class="nr-detail-loading">매물 불러오는 중...</div>

              <div v-else-if="houseList.length === 0" class="nr-detail-empty">
                등록된 매물이 없습니다.
              </div>

              <div v-else class="nr-detail-house-table-wrap">
                <table class="table table-hover nr-detail-house-table">
                  <thead>
                    <tr>
                      <th>건물명</th>
                      <th>법정동</th>
                      <th class="text-end">건축년도</th>
                      <th class="text-center">거래 유형</th>
                      <th class="text-end">거래수</th>
                      <th class="text-end">평균 거래 조건</th>
                      <th class="text-end"></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="house in houseList"
                      :key="house.aptSeq"
                      :data-apt-seq="house.aptSeq"
                      :class="{
                        'nr-detail-row--active': highlightedAptSeq === house.aptSeq,
                        'nr-detail-row--pinned': pinnedAptSeq === house.aptSeq,
                      }"
                      @mouseenter="highlightedAptSeq = house.aptSeq"
                      @mouseleave="highlightedAptSeq = null"
                    >
                      <td>
                        <!-- 거래 유형을 URL에 남겨 상세 페이지를 새로고침해도 같은 전월세/매매 내역을 조회한다. -->
                        <RouterLink :to="{ path: `/houses/${house.aptSeq}`, query: { dealType: savedDealType } }" class="fw-bold article-link">
                          {{ house.aptNm }}
                        </RouterLink>
                        <div class="small text-muted">{{ house.sidoName }} {{ house.gugunName }}</div>
                      </td>
                      <td class="small">{{ house.umdNm }}</td>
                      <td class="text-end mono">{{ house.buildYear ?? '-' }}</td>
                      <td class="text-center">
                        <span class="nr-deal-badge">{{ savedDealType }}</span>
                      </td>
                      <td class="text-end">
                        <span class="mono fw-semibold">
                          {{ number(savedDealType === '매매' ? house.dealCount : house.rentCount) }}
                        </span>
                        <span class="small text-muted"> 건</span>
                      </td>
                      <td class="text-end"><span class="money money--rent">{{ averageTransactionText(house) }}</span></td>
                      <td class="text-end">
                        <RouterLink :to="{ path: `/houses/${house.aptSeq}`, query: { dealType: savedDealType } }" class="btn btn-outline-primary btn-sm">
                          상세
                        </RouterLink>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </div>
      </div>
    </template>

    <!-- ④ 추천 로딩 화면 -->
    <template v-else-if="recommendLoading">
      <div class="nr-loading-page">
        <div class="nr-loading-card">
          <div class="nr-loading-spinner"></div>
          <p class="nr-loading-stage">{{ loadingMessages[loadingStage] }}</p>
          <p class="nr-loading-count-hint">
            분석된 후보 동네 중 <strong>최적 3곳</strong>을 선정하고 있습니다
          </p>
          <p class="nr-loading-sub">잠시만 기다려 주세요</p>
        </div>
      </div>
    </template>

    <!-- ⑤ 조건 설정 단계 -->
    <template v-else>
      <section class="place-finder-hero place-condition-hero">
        <div class="place-finder-badge">동네 추천</div>
        <h1>나만의 조건으로<br /><span>완벽한 동네</span>를 찾아보세요</h1>
        <p>4단계 설정으로 당신에게 딱 맞는 동네를 추천받으세요</p>
      </section>

      <section class="condition-setup-shell" aria-label="동네 추천 조건 설정">
        <StepIndicator :current-step="currentStep" />
        <RadiusStep
          v-if="screen === 'radius'"
          v-model:commute-radius="commuteRadius"
          :selected-place="selectedPlace"
          :selected-address="selectedAddress"
          @prev="backToSearch"
          @next="openTransactionStep"
        />
        <TransactionHousingStep
          v-else-if="screen === 'transaction'"
          v-model:deal-type="dealType"
          v-model:housing-type="housingType"
          @prev="backToRadiusStep"
          @next="openBudgetStep"
        />
        <BudgetStep
          v-else-if="screen === 'budget'"
          v-model:budget-amount="budgetAmount"
          :deal-type="dealType"
          v-model:monthly-rent-max="monthlyRentMax"
          @prev="backToTransactionStep"
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
