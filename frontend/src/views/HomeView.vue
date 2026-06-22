<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api/client'
import { fetchCommercialCategories, fetchCommercialSubcategories, searchCommercials } from '../api/commercials'
import { fetchFavorites } from '../api/favorites'
import { fetchEmdList, fetchSidoList, fetchSigunguList } from '../api/regions'
import { number } from '../utils/format'
import KakaoMap from '../components/KakaoMap.vue'

const props = defineProps({
  user: { type: Object, default: null }
})

const search = ref({
  housingType: 'all',
  contractType: 'all',
  dealYmd: '',
  lawdCode: '',
  sido: '',
  sigungu: '',
  emd: '',
  keyword: '',
  priceMin: '',
  priceMax: '',
  areaMin: '',
  areaMax: '',
  sortBy: 'latest'
})
const results = ref([])
const commercials = ref([])
const favorites = ref([])
const sidoList = ref([])
const sigunguList = ref([])
const emdList = ref([])
const commercial = ref({
  sido: '',
  sigungu: '',
  lawdCd: '',
  dong: '',
  categoryL: '',
  categoryM: '',
  keyword: ''
})
const commercialSigunguList = ref([])
const commercialCategoryLList = ref([])
const commercialCategoryMList = ref([])
const commercialError = ref('')
const commercialLoading = ref(false)
const commercialSearched = ref(false)
const statusMessage = ref('')
const loading = ref(false)

const stats = computed(() => {
  const total = results.value.length
  const deal = results.value.filter((item) => item.contractType === 'deal' || item.type === 'deal').length
  const rent = total - deal
  const amounts = results.value.map((item) => Number(item.amount || item.dealAmount || 0)).filter(Number.isFinite)
  const avgDeal = amounts.length ? Math.round(amounts.reduce((a, b) => a + b, 0) / amounts.length) : 0
  return { total, deal, rent, avgDeal }
})
const selectedRegionText = computed(() => {
  const sido = sidoList.value.find((item) => item.code === search.value.sido)?.name
  const sigungu = sigunguList.value.find((item) => item.code === search.value.sigungu)?.name
  return [sido, sigungu].filter(Boolean).join(' ')
})

async function runSearch() {
  loading.value = true
  statusMessage.value = ''
  try {
    const response = await api.get('/properties/search', {
      params: {
        lawdCd: search.value.lawdCode || undefined,
        dealYmd: search.value.dealYmd || undefined,
        housingType: search.value.housingType,
        contractType: search.value.contractType
      }
    })
    results.value = Array.isArray(response.data.data) ? response.data.data : []
  } catch {
    results.value = []
    statusMessage.value = '거래 검색 API 응답을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadFavorites() {
  if (!props.user) {
    favorites.value = []
    return
  }
  try {
    const response = await fetchFavorites()
    favorites.value = Array.isArray(response.data) ? response.data : []
  } catch {
    favorites.value = []
  }
}

async function loadSidoOptions() {
  try {
    sidoList.value = await fetchSidoList()
    const seoul = sidoList.value.find((item) => item.name === '서울특별시')
    if (seoul && !search.value.sido) {
      search.value.sido = seoul.code
      await loadSigunguOptions()
      const jongno = sigunguList.value.find((item) => item.code === '11110')
      if (jongno) {
        search.value.sigungu = jongno.code
        search.value.lawdCode = jongno.code.slice(0, 5)
        await loadEmdOptions()
      }
    }
  } catch {
    statusMessage.value = '지역 목록을 불러오지 못했습니다. LAWD_CD를 직접 입력해 주세요.'
  }
}

async function loadSigunguOptions() {
  sigunguList.value = await fetchSigunguList(search.value.sido)
  search.value.sigungu = ''
  search.value.emd = ''
  emdList.value = []
}

async function loadEmdOptions() {
  emdList.value = await fetchEmdList(search.value.sigungu)
  search.value.emd = ''
  if (/^\d{5}/.test(search.value.sigungu)) {
    search.value.lawdCode = search.value.sigungu.slice(0, 5)
  }
}

async function loadCommercialSigunguOptions() {
  commercialSigunguList.value = await fetchSigunguList(commercial.value.sido)
  commercial.value.sigungu = ''
  commercial.value.lawdCd = ''
}

function onCommercialSigunguChange() {
  if (/^\d{5}/.test(commercial.value.sigungu)) {
    commercial.value.lawdCd = commercial.value.sigungu.slice(0, 5)
  }
}

async function loadCommercialCategories() {
  if (commercialCategoryLList.value.length) return
  try {
    commercialCategoryLList.value = await fetchCommercialCategories()
  } catch {
    commercialError.value = '상권 업종 대분류를 불러오지 못했습니다.'
  }
}

async function loadCommercialSubcategories() {
  commercial.value.categoryM = ''
  commercialCategoryMList.value = await fetchCommercialSubcategories(commercial.value.categoryL)
}

function useMainRegionForCommercial() {
  commercial.value.lawdCd = search.value.lawdCode
  commercial.value.dong = emdList.value.find((item) => item.code === search.value.emd)?.name || ''
}

async function runCommercialSearch() {
  commercialError.value = ''
  commercialSearched.value = true
  if (!commercial.value.lawdCd && !commercial.value.dong && !commercial.value.categoryL) {
    commercialError.value = '시군구코드, 법정동, 대분류 중 하나 이상을 지정해주세요. (상호명만으로는 너무 무겁습니다)'
    commercials.value = []
    return
  }
  commercialLoading.value = true
  try {
    commercials.value = await searchCommercials({ ...commercial.value, limit: 200 })
  } catch (error) {
    commercialError.value = error?.response?.data?.message || '상가 검색에 실패했습니다.'
    commercials.value = []
  } finally {
    commercialLoading.value = false
  }
}

function resetFilters() {
  search.value = {
    housingType: 'all',
    contractType: 'all',
    dealYmd: '',
    lawdCode: '',
    sido: '',
    sigungu: '',
    emd: '',
    keyword: '',
    priceMin: '',
    priceMax: '',
    areaMin: '',
    areaMax: '',
    sortBy: 'latest'
  }
  results.value = []
  loadSidoOptions()
}

onMounted(async () => {
  await Promise.all([loadSidoOptions(), loadFavorites()])
})
</script>

<template>
  <main class="container page-container home-page">
    <div v-if="statusMessage" class="alert alert-warning" role="alert">{{ statusMessage }}</div>

    <section class="grid stat-grid">
      <div class="card stat-card">
        <div class="stat-label">전체 거래 건수</div>
        <div class="stat-value"><span>{{ number(stats.total) }}</span><span class="stat-unit">건</span></div>
      </div>
      <div class="card stat-card">
        <div class="stat-label">매매 건수</div>
        <div class="stat-value"><span>{{ number(stats.deal) }}</span><span class="stat-unit">건</span></div>
      </div>
      <div class="card stat-card">
        <div class="stat-label">전월세 건수</div>
        <div class="stat-value"><span>{{ number(stats.rent) }}</span><span class="stat-unit">건</span></div>
      </div>
      <div class="card stat-card">
        <div class="stat-label">평균 매매가</div>
        <div class="stat-value"><span>{{ number(stats.avgDeal) }}</span><span class="stat-unit">만원</span></div>
      </div>
    </section>

    <section class="home-layout">
      <aside class="home-sidebar">
        <div class="card">
          <div class="card-header section-head">
            <div class="title"><span class="bar"></span><span>거래 검색</span></div>
          </div>
          <div class="card-body">
            <form class="grid search-grid" @submit.prevent="runSearch">
              <div class="span-6">
                <label class="form-label small">주택 유형</label>
                <select v-model="search.housingType" class="form-select form-select-sm">
                  <option value="all">전체</option>
                  <option value="apt">아파트</option>
                  <option value="home">연립/다세대</option>
                </select>
              </div>
              <div class="span-6">
                <label class="form-label small">거래 유형</label>
                <select v-model="search.contractType" class="form-select form-select-sm">
                  <option value="all">전체</option>
                  <option value="deal">매매</option>
                  <option value="rent">전월세</option>
                </select>
              </div>
              <div class="span-6">
                <label class="form-label small">거래년월 (DEAL_YMD)</label>
                <input v-model="search.dealYmd" type="text" maxlength="6" class="form-control form-control-sm mono" placeholder="예: 202602" />
              </div>
              <div class="span-6">
                <label class="form-label small">법정동코드 (LAWD_CD)</label>
                <input v-model="search.lawdCode" type="text" maxlength="5" class="form-control form-control-sm mono" placeholder="예: 11110" />
              </div>
              <div class="span-6">
                <label class="form-label small">시도</label>
                <select v-model="search.sido" class="form-select form-select-sm" @change="loadSigunguOptions">
                  <option value="">선택</option>
                  <option v-for="sido in sidoList" :key="sido.code" :value="sido.code">{{ sido.name }}</option>
                </select>
              </div>
              <div class="span-6">
                <label class="form-label small">시군구</label>
                <select v-model="search.sigungu" class="form-select form-select-sm" @change="loadEmdOptions">
                  <option value="">선택</option>
                  <option v-for="sigungu in sigunguList" :key="sigungu.code" :value="sigungu.code">{{ sigungu.name }}</option>
                </select>
              </div>
              <div class="span-12 search-actions">
                <button type="submit" class="btn btn-primary btn-sm flex-grow-1" :disabled="loading">검색</button>
                <button type="button" class="btn btn-outline-secondary btn-sm" @click="resetFilters">초기화</button>
                <button type="button" class="btn btn-outline-danger btn-sm">관심지역</button>
              </div>
            </form>
            <div class="small text-muted helper-text">※ DEAL_YMD는 YYYYMM 형식, LAWD_CD는 시군구 5자리 코드입니다.</div>
          </div>
        </div>

        <div class="card">
          <div class="card-header section-head">
            <div class="title"><span class="bar"></span><span>주변 상권</span>
              <span class="badge text-bg-secondary">{{ commercials.length }}건</span>
            </div>
          </div>
          <div class="card-body commercial-form">
            <div class="grid search-grid">
              <div class="span-6">
                <label class="form-label small">시도</label>
                <select v-model="commercial.sido" class="form-select form-select-sm" @change="loadCommercialSigunguOptions">
                  <option value="">선택</option>
                  <option v-for="sido in sidoList" :key="sido.code" :value="sido.code">{{ sido.name }}</option>
                </select>
              </div>
              <div class="span-6">
                <label class="form-label small">시군구</label>
                <select v-model="commercial.sigungu" class="form-select form-select-sm" @change="onCommercialSigunguChange">
                  <option value="">선택</option>
                  <option v-for="sigungu in commercialSigunguList" :key="sigungu.code" :value="sigungu.code">{{ sigungu.name }}</option>
                </select>
              </div>
              <div class="span-6"><label class="form-label small">시군구코드</label><input v-model="commercial.lawdCd" class="form-control form-control-sm mono" placeholder="예: 11650" /></div>
              <div class="span-6"><label class="form-label small">법정동</label><input v-model="commercial.dong" class="form-control form-control-sm" placeholder="예: 방배동" /></div>
              <div class="span-6">
                <label class="form-label small">대분류</label>
                <select v-model="commercial.categoryL" class="form-select form-select-sm" @focus="loadCommercialCategories" @mousedown="loadCommercialCategories" @change="loadCommercialSubcategories">
                  <option value="">{{ commercialCategoryLList.length ? '전체' : '(클릭하여 로드)' }}</option>
                  <option v-for="category in commercialCategoryLList" :key="category.code" :value="category.code">{{ category.name }}</option>
                </select>
              </div>
              <div class="span-6">
                <label class="form-label small">중분류</label>
                <select v-model="commercial.categoryM" class="form-select form-select-sm">
                  <option value="">대분류 선택 시 로드</option>
                  <option v-for="category in commercialCategoryMList" :key="category.code" :value="category.code">{{ category.name }}</option>
                </select>
              </div>
              <div class="span-12"><label class="form-label small">상호명</label><input v-model="commercial.keyword" class="form-control form-control-sm" placeholder="예: GS" /></div>
              <div class="span-12 search-actions">
                <button class="btn btn-primary btn-sm flex-grow-1" type="button" :disabled="commercialLoading" @click="runCommercialSearch">
                  {{ commercialLoading ? '검색 중...' : '상가 검색' }}
                </button>
                <button class="btn btn-outline-secondary btn-sm" type="button" title="거래 검색 영역의 시군구/읍면동 값 가져오기" @click="useMainRegionForCommercial">위 조건 사용</button>
              </div>
            </div>
          </div>
          <div class="table-responsive commercial-table">
            <table class="table table-sm">
              <thead><tr><th>업종</th><th>상호</th><th>법정동</th></tr></thead>
              <tbody>
                <tr v-if="commercialError"><td colspan="3" class="text-center text-danger py-3">{{ commercialError }}</td></tr>
                <tr v-else-if="commercialLoading"><td colspan="3" class="text-center text-secondary py-3"><span class="spinner-border spinner-border-sm me-2"></span>검색 중...</td></tr>
                <tr v-else-if="commercials.length === 0">
                  <td colspan="3" class="empty-state">
                    {{ commercialSearched ? '검색 결과가 없습니다.' : '검색 조건 입력 후 실행해 주세요.' }}
                  </td>
                </tr>
                <tr v-for="item in commercials" :key="`${item.shopName}-${item.roadAddress}-${item.jibunAddress}`">
                  <td class="small">
                    <span class="badge text-bg-light border">{{ item.categoryLName }}</span>
                    <div class="text-secondary mt-1">{{ [item.categoryMName, item.categorySName].filter(Boolean).join(' › ') }}</div>
                  </td>
                  <td>
                    {{ item.shopName }}
                    <span v-if="item.branchName" class="text-secondary small">({{ item.branchName }})</span>
                    <div v-if="item.roadAddress || item.jibunAddress" class="text-secondary small text-truncate" :title="item.roadAddress || item.jibunAddress">
                      {{ item.roadAddress || item.jibunAddress }}
                    </div>
                  </td>
                  <td class="small">{{ item.beopjeongName }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card">
          <div class="card-header section-head">
            <div class="title"><span class="bar"></span><span>관심지역</span></div>
          </div>
          <table class="table table-sm">
            <thead><tr><th>지역</th><th class="text-end">동작</th></tr></thead>
            <tbody>
              <tr v-if="!props.user"><td colspan="2" class="empty-state">로그인 후 이용 가능합니다.</td></tr>
              <tr v-else-if="favorites.length === 0"><td colspan="2" class="empty-state">등록된 관심지역이 없습니다.</td></tr>
              <tr v-for="favorite in favorites" :key="favorite.key">
                <td>{{ [favorite.sido, favorite.sigungu, favorite.emd].filter(Boolean).join(' ') }}</td>
                <td class="text-end"><button class="btn btn-outline-danger btn-sm">삭제</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </aside>

      <section class="home-main">
        <div class="card">
          <div class="card-header section-head">
            <div class="title"><span class="bar"></span><span>지도</span></div>
            <div class="flex gap-2">
              <span class="chip is-primary">매매</span>
              <span class="chip is-accent">전월세</span>
            </div>
          </div>
          <div class="map-card-body">
            <KakaoMap :records="results" :center-text="selectedRegionText" height="380px" />
          </div>
        </div>

        <div class="card">
          <div class="card-header section-head">
            <div class="title"><span class="bar"></span><span>검색 결과</span>
              <span class="badge text-bg-primary">{{ results.length }}건</span>
            </div>
          </div>
          <div class="filter-rail">
            <div class="grid result-filter-grid">
              <div class="span-3"><label class="form-label small">정렬</label><select v-model="search.sortBy" class="form-select form-select-sm"><option value="latest">최신순</option><option value="price_desc">금액 높은순</option><option value="price_asc">금액 낮은순</option><option value="area_desc">면적 큰순</option><option value="area_asc">면적 작은순</option></select></div>
              <div class="span-3">
                <label class="form-label small">읍면동</label>
                <select v-model="search.emd" class="form-select form-select-sm">
                  <option value="">전체</option>
                  <option v-for="emd in emdList" :key="emd.code" :value="emd.code">{{ emd.name }}</option>
                </select>
              </div>
              <div class="span-6"><label class="form-label small">아파트명/법정동</label><input v-model="search.keyword" class="form-control form-control-sm" placeholder="예: 광화문풍림" /></div>
              <div class="span-3"><label class="form-label small">최소 금액 (만원)</label><input v-model="search.priceMin" type="number" min="0" class="form-control form-control-sm" /></div>
              <div class="span-3"><label class="form-label small">최대 금액 (만원)</label><input v-model="search.priceMax" type="number" min="0" class="form-control form-control-sm" /></div>
              <div class="span-3"><label class="form-label small">최소 면적 (㎡)</label><input v-model="search.areaMin" type="number" min="0" step="0.1" class="form-control form-control-sm" /></div>
              <div class="span-3"><label class="form-label small">최대 면적 (㎡)</label><input v-model="search.areaMax" type="number" min="0" step="0.1" class="form-control form-control-sm" /></div>
            </div>
          </div>
          <div class="table-responsive result-table">
            <table class="table table-hover">
              <thead>
                <tr><th>거래일</th><th>유형</th><th>주택명</th><th>법정동</th><th class="text-end">면적 (㎡)</th><th class="text-end">금액</th><th class="text-end">층</th></tr>
              </thead>
              <tbody>
                <tr v-if="results.length === 0"><td colspan="7" class="empty-state">검색을 실행해 주세요.</td></tr>
                <tr v-for="item in results" :key="`${item.aptSeq || item.aptNm}-${item.date}-${item.floor}`">
                  <td class="mono">{{ item.date || item.dealDate || '' }}</td>
                  <td><span class="chip" :class="item.contractType === 'rent' ? 'is-accent' : 'is-primary'">{{ item.contractType || item.type || '매매' }}</span></td>
                  <td>{{ item.aptNm || item.houseName || item.name }}</td>
                  <td>{{ item.dong || item.umdNm }}</td>
                  <td class="text-end mono">{{ item.area }}</td>
                  <td class="text-end"><span class="money">{{ number(item.amount || item.dealAmount) }}</span></td>
                  <td class="text-end mono">{{ item.floor }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>
