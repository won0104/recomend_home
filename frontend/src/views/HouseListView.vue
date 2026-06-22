<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import { fetchHouses } from '../api/houses'
import { number } from '../utils/format'

const props = defineProps({ user: { type: Object, default: null } })
const route = useRoute()
const router = useRouter()
const filters = ref({
  lawdCd: String(route.query.lawdCd || ''),
  dong: String(route.query.dong || ''),
  keyword: String(route.query.keyword || '')
})
const houses = ref([])

async function load() {
  const response = await fetchHouses({
    lawdCd: filters.value.lawdCd || undefined,
    dong: filters.value.dong || undefined,
    keyword: filters.value.keyword || undefined
  })
  houses.value = Array.isArray(response.data) ? response.data : []
}

function search() {
  router.replace({
    path: '/houses',
    query: Object.fromEntries(Object.entries(filters.value).filter(([, value]) => value))
  })
  load()
}

function reset() {
  filters.value = { lawdCd: '', dong: '', keyword: '' }
  search()
}

onMounted(load)
</script>

<template>
  <PageHero
    :crumbs="['홈', '주택 정보']"
    title="주택 정보 목록"
    lede="시군구·법정동·아파트명으로 등록된 주택을 검색합니다. 최대 200건까지 표시됩니다."
  />

  <main class="container page-container">
    <div class="card">
      <div class="filter-rail">
        <form class="grid house-filter-grid" @submit.prevent="search">
          <div class="span-3">
            <label class="form-label small">시군구코드 (LAWD_CD, 5자리 숫자)</label>
            <input v-model="filters.lawdCd" type="text" maxlength="5" class="form-control form-control-sm mono" placeholder="예: 11110" />
          </div>
          <div class="span-3">
            <label class="form-label small">법정동</label>
            <input v-model="filters.dong" type="text" class="form-control form-control-sm" placeholder="예: 청운동" />
          </div>
          <div class="span-4">
            <label class="form-label small">아파트명</label>
            <input v-model="filters.keyword" type="text" class="form-control form-control-sm" placeholder="예: 광화문풍림" />
          </div>
          <div class="span-2 house-search-actions">
            <button type="submit" class="btn btn-primary btn-sm flex-grow-1">검색</button>
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="reset">초기화</button>
          </div>
        </form>
      </div>

      <div class="card-header section-head list-section-head">
        <div class="title"><span class="bar"></span><span>검색 결과</span><span class="badge text-bg-secondary">총 {{ houses.length }}건</span></div>
        <span class="small text-muted">정렬: 거래수 많은순</span>
      </div>

      <table class="table table-hover">
        <thead>
          <tr>
            <th>아파트명</th><th>법정동</th><th>지번</th><th class="text-end">건축년도</th><th class="text-end">거래수</th><th class="text-end house-actions-cell">동작</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="houses.length === 0"><td colspan="6" class="empty-state">조건에 맞는 주택 정보가 없습니다.</td></tr>
          <tr v-for="house in houses" :key="house.aptSeq">
            <td>
              <RouterLink :to="`/houses/${house.aptSeq}`" class="fw-bold article-link">{{ house.aptNm }}</RouterLink>
              <div class="small text-muted">{{ house.sidoName }} {{ house.gugunName }}</div>
            </td>
            <td>{{ house.umdNm }}</td>
            <td class="small mono text-muted">{{ house.jibun }}</td>
            <td class="text-end mono">{{ house.buildYear ?? '-' }}</td>
            <td class="text-end"><span class="mono fw-semibold">{{ number(house.dealCount) }}</span><span class="small text-muted"> 건</span></td>
            <td class="text-end">
              <RouterLink :to="`/houses/${house.aptSeq}`" class="btn btn-outline-primary btn-sm">상세</RouterLink>
              <RouterLink v-if="props.user?.admin" :to="`/houses/${house.aptSeq}/edit`" class="btn btn-outline-warning btn-sm">수정</RouterLink>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="card-footer flex-between">
        <RouterLink to="/" class="btn btn-outline-secondary btn-sm">← 메인으로</RouterLink>
        <div class="small text-muted">총 {{ houses.length }}건 표시</div>
      </div>
    </div>
  </main>
</template>
