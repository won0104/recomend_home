<script setup>
import { computed, nextTick, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BudgetStep from '../components/neighborhood/BudgetStep.vue'
import LifestyleStep from '../components/neighborhood/LifestyleStep.vue'
import NeighborhoodDetailMap from '../components/neighborhood/NeighborhoodDetailMap.vue'
import NeighborhoodResultMap from '../components/neighborhood/NeighborhoodResultMap.vue'
import PlaceSearchStep from '../components/neighborhood/PlaceSearchStep.vue'
import RadiusStep from '../components/neighborhood/RadiusStep.vue'
import StepIndicator from '../components/neighborhood/StepIndicator.vue'
import { fetchHouses } from '../api/houses'
import { recommendNeighborhood } from '../api/neighborhoods'
import { number } from '../utils/format'

const selectedPlace = ref(null)
const screen = ref('search')

const commuteRadius = ref(15)
const budgetAmount = ref(3)
const dealType = ref('매매')
const housingType = ref('아파트')
const dealAmountMin = ref(null)
const depositMin = ref(null)
const monthlyRentMin = ref(null)
const monthlyRentMax = ref(null)
const selectedLifestyleIds = ref([])
const lifestyleMood = ref('green')
const recommendLoading = ref(false)
const recommendations = ref([])
const totalCandidateCount = ref(0)
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
const highlightedAptSeq = ref(null) // 행 hover
const pinnedAptSeq = ref(null)      // 지도 핀 클릭으로 고정된 행

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
  { id: 'CULTURE',  icon: '🏃', title: '운동·여가' },
  { id: 'SHOPPING', icon: '🛒', title: '장보기·생활편의' },
  { id: 'SCHOOL',   icon: '🏫', title: '학교·교육' },
  { id: 'PARK',     icon: '🌳', title: '공원·산책' },
  { id: 'MEDICAL',  icon: '🏥', title: '의료' },
  { id: 'PET',      icon: '🐾', title: '반려인' },
]

const categoryLabel = {
  CAFE: '카페·외식', CULTURE: '운동·여가', SHOPPING: '장보기·생활편의',
  SCHOOL: '학교·교육', PARK: '공원·산책', MEDICAL: '의료', PET: '반려인',
}

const categoryDesc = {
  SCHOOL:   { high: '학교가 많아 교육 환경이 우수합니다.', mid: '교육 시설이 평균 수준입니다.', low: '학교가 다소 부족합니다.' },
  PARK:     { high: '공원이 가까워 산책·야외 활동이 편합니다.', mid: '공원 접근성이 보통입니다.', low: '공원이 다소 부족합니다.' },
  CAFE:     { high: '카페와 식당이 풍부해 외식이 편리합니다.', mid: '카페·외식 환경이 보통입니다.', low: '카페·식당이 다소 부족합니다.' },
  CULTURE:  { high: '문화·스포츠 시설이 풍부합니다.', mid: '문화시설이 평균 수준입니다.', low: '문화시설이 다소 부족합니다.' },
  SHOPPING: { high: '마트와 편의시설이 가까워 생활이 편리합니다.', mid: '생활편의시설이 평균 수준입니다.', low: '생활편의시설이 다소 부족합니다.' },
  MEDICAL:  { high: '병원·의원이 가까워 의료 접근성이 좋습니다.', mid: '의료시설이 평균 수준입니다.', low: '의료시설이 다소 부족합니다.' },
  PET:      { high: '동물병원·공원이 가까워 반려동물 생활이 쾌적합니다.', mid: '반려동물 인프라가 보통입니다.', low: '반려동물 관련 시설이 부족합니다.' },
}

function scoreLevel(score) {
  if (score >= 65) return 'high'
  if (score >= 35) return 'mid'
  return 'low'
}

const strengthLabel = {
  PARK: '공원 접근 우수', CAFE: '카페·외식 밀집', SCHOOL: '학교 인접',
  SHOPPING: '생활편의 풍부', MEDICAL: '의료 인프라', CULTURE: '문화시설 우수', PET: '반려인 환경',
}
const weaknessLabel = {
  PARK: '공원 다소 부족', CAFE: '카페 다소 부족', SCHOOL: '학교 다소 부족',
  SHOPPING: '편의시설 다소 부족', MEDICAL: '의료시설 다소 부족', CULTURE: '문화시설 다소 부족', PET: '반려 인프라 부족',
}

function getStrengthTags(categoryScores) {
  return categoryScores.filter((cs) => cs.score >= 65).map((cs) => strengthLabel[cs.category] ?? cs.category)
}
function getWeaknessTags(categoryScores) {
  return categoryScores.filter((cs) => cs.score < 40).map((cs) => weaknessLabel[cs.category] ?? cs.category)
}

const RANK_COLORS = { 1: '#2563EB', 2: '#22C55E', 3: '#F97316' }

const currentStep = computed(() => {
  if (screen.value === 'budget') return 2
  if (screen.value === 'lifestyle') return 3
  return 1
})
const selectedAddress = computed(() =>
  selectedPlace.value?.road_address_name || selectedPlace.value?.address_name || ''
)
const activeBudgetMax = computed(() =>
  Number(budgetAmount.value) >= 10 ? null : Math.round(Number(budgetAmount.value) * 10000)
)
const maxPrice = computed(() => dealType.value === '매매' ? activeBudgetMax.value : null)
const budgetLabel = computed(() => {
  const maxLabel = Number(budgetAmount.value) >= 10 ? '제한 없음' : `${budgetAmount.value}억원 이하`
  if (dealType.value === '매매') return `매매가 ${dealAmountMin.value ? `${number(dealAmountMin.value)}만원 ~ ` : ''}${maxLabel}`
  if (dealType.value === '전세') return `보증금 ${depositMin.value ? `${number(depositMin.value)}만원 ~ ` : ''}${maxLabel}`
  const monthly = `${monthlyRentMin.value ? `${number(monthlyRentMin.value)} ~ ` : ''}${monthlyRentMax.value ? `${number(monthlyRentMax.value)}만원` : '제한 없음'}`
  return `보증금 ${depositMin.value ? `${number(depositMin.value)}만원 ~ ` : ''}${maxLabel}, 월세 ${monthly}`
})
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
  // maxPrice는 구 버전 요청 호환용이다. 새 후보 SQL은 아래 거래 유형별 금액 필드를 우선 사용한다.
  maxPrice: maxPrice.value,
  dealType: dealType.value,
  dealAmountMin: dealType.value === '매매' ? dealAmountMin.value : null,
  dealAmountMax: dealType.value === '매매' ? activeBudgetMax.value : null,
  depositMin: dealType.value !== '매매' ? depositMin.value : null,
  depositMax: dealType.value !== '매매' ? activeBudgetMax.value : null,
  monthlyRentMin: dealType.value === '월세' ? monthlyRentMin.value : null,
  monthlyRentMax: dealType.value === '월세' ? monthlyRentMax.value : null,
  housingType: housingType.value,
  priorities: selectedLifestyleIds.value,
  lifestyleMood: lifestyleMood.value,
}))

function handlePlaceSelect(place) { selectedPlace.value = place }
function openRadiusStep(place) { selectedPlace.value = place; screen.value = 'radius' }
function openBudgetStep() { screen.value = 'budget' }
function openLifestyleStep() { screen.value = 'lifestyle' }
function backToSearch() { screen.value = 'search' }
function backToRadiusStep() { screen.value = 'radius' }
function backToBudgetStep() { screen.value = 'budget' }
function backToLifestyleStep() { screen.value = 'lifestyle' }
function backToResult() { screen.value = 'result' }

async function openDetail(rec) {
  selectedNeighborhood.value = rec
  screen.value = 'detail'
  houseLoading.value = true
  houseList.value = []
  pinnedAptSeq.value = null
  highlightedAptSeq.value = null
  try {
    // 동네 후보를 걸러낸 예산·거래·주거 조건을 매물 목록에도 그대로 적용한다.
    const res = await fetchHouses({
      lawdCd: rec.lawdCd,
      maxPrice: maxPrice.value,
      dealType: dealType.value,
      housingType: housingType.value,
      dealAmountMin: dealType.value === '매매' ? dealAmountMin.value : undefined,
      dealAmountMax: dealType.value === '매매' ? activeBudgetMax.value : undefined,
      depositMin: dealType.value !== '매매' ? depositMin.value : undefined,
      depositMax: dealType.value !== '매매' ? activeBudgetMax.value : undefined,
      monthlyRentMin: dealType.value === '월세' ? monthlyRentMin.value : undefined,
      monthlyRentMax: dealType.value === '월세' ? monthlyRentMax.value : undefined,
    })
    houseList.value = Array.isArray(res.data) ? res.data : []
  } catch {
    houseList.value = []
  } finally {
    houseLoading.value = false
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
    screen.value = 'result'
  } catch (error) {
    console.error(error?.response?.data?.message || '동네 추천 후보를 불러오지 못했습니다.')
  } finally {
    clearInterval(_loadingTimer)
    recommendLoading.value = false
  }
}
</script>

<template>
  <main class="place-finder-page">

    <!-- ① 장소 검색 -->
    <template v-if="screen === 'search'">
      <section class="place-finder-hero">
        <div class="place-finder-badge">맞춤 동네 추천 서비스</div>
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
              <div class="nr-card__score-circle" :class="`nr-card__score-circle--rank${rec.rank}`">
                <span class="nr-card__score-num">{{ rec.score.toFixed(0) }}</span>
                <span class="nr-card__score-unit">점</span>
              </div>
            </li>
          </ul>

          <p v-if="totalCandidateCount > 0" class="nr-candidate-note">
            총 <strong>{{ totalCandidateCount }}</strong>개 동네를 분석하여 최적 <strong>{{ recommendations.length }}</strong>곳을 선정했습니다.
          </p>
          <p class="nr-disclaimer">추천 점수는 공개된 공공데이터를 기반으로 분석되며, 실제 생활 환경과 다를 수 있습니다.</p>
          <button class="nr-retry-btn" @click="backToLifestyleStep">다시 설정하기</button>
        </section>

        <aside class="nr-result-right">
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
          <div
            class="nr-detail-total-score"
            :style="{ color: RANK_COLORS[selectedNeighborhood.rank], borderColor: RANK_COLORS[selectedNeighborhood.rank] }"
          >
            <span class="nr-detail-total-score__num">{{ selectedNeighborhood.score.toFixed(0) }}</span>
            <span class="nr-detail-total-score__unit">점</span>
          </div>
        </div>

        <!-- 2단 본문 -->
        <div class="nr-detail-body">

          <!-- 왼쪽: 강점 분석 -->
          <section class="nr-detail-left">
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
                  {{ categoryDesc[cs.category]?.[scoreLevel(cs.score)] }}
                </p>
              </li>
            </ul>
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
                      <th v-if="savedDealType === '매매'" class="text-end">거래수</th>
                      <th v-if="savedDealType === '매매'" class="text-end">평균 매매가</th>
                      <th v-else class="text-center">조건</th>
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
                        <RouterLink :to="`/houses/${house.aptSeq}`" class="fw-bold article-link">
                          {{ house.aptNm }}
                        </RouterLink>
                        <div class="small text-muted">{{ house.sidoName }} {{ house.gugunName }}</div>
                      </td>
                      <td class="small">{{ house.umdNm }}</td>
                      <td class="text-end mono">{{ house.buildYear ?? '-' }}</td>
                      <td class="text-center">
                        <span class="nr-deal-badge">{{ savedDealType }}</span>
                      </td>
                      <td v-if="savedDealType === '매매'" class="text-end">
                        <span class="mono fw-semibold">{{ number(house.dealCount) }}</span>
                        <span class="small text-muted"> 건</span>
                      </td>
                      <td v-if="savedDealType === '매매'" class="text-end mono">
                        <template v-if="house.avgPrice">
                          {{ house.avgPrice >= 10000
                              ? (house.avgPrice / 10000).toFixed(1) + '억'
                              : number(house.avgPrice) + '만' }}
                        </template>
                        <template v-else>-</template>
                      </td>
                      <td v-else class="text-center"><span class="small text-success">조건 일치</span></td>
                      <td class="text-end">
                        <RouterLink :to="`/houses/${house.aptSeq}`" class="btn btn-outline-primary btn-sm">
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
          v-model:deal-type="dealType"
          v-model:housing-type="housingType"
          v-model:deal-amount-min="dealAmountMin"
          v-model:deposit-min="depositMin"
          v-model:monthly-rent-min="monthlyRentMin"
          v-model:monthly-rent-max="monthlyRentMax"
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
