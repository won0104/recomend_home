<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import { fetchHouse, fetchHouseDeals } from '../api/houses'
import { number } from '../utils/format'
import KakaoMap from '../components/KakaoMap.vue'

const props = defineProps({ user: { type: Object, default: null } })
const route = useRoute()
const house = ref(null)
const deals = ref([])
const sort = ref('latest')

const roadAddress = computed(() => {
  const h = house.value || {}
  return `${h.roadNm || ''} ${h.roadNmBonbun || ''}${h.roadNmBubun ? `-${h.roadNmBubun}` : ''}`.trim()
})
const sortedDeals = computed(() => {
  const copied = [...deals.value]
  if (sort.value === 'price_desc') return copied.sort((a, b) => Number(b.amount) - Number(a.amount))
  return copied.sort((a, b) => String(b.date || '').localeCompare(String(a.date || '')))
})
const houseMarker = computed(() => {
  if (!house.value?.latitude || !house.value?.longitude) return null
  return {
    lat: house.value.latitude,
    lng: house.value.longitude,
    content: `<div style="padding:8px 10px; min-width:160px; line-height:1.4;"><div style="font-weight:700;">${house.value.aptNm}</div><div>${house.value.umdNm || ''} ${house.value.jibun || ''}</div></div>`
  }
})

async function load() {
  const [houseResponse, dealResponse] = await Promise.all([
    fetchHouse(route.params.aptSeq),
    fetchHouseDeals(route.params.aptSeq)
  ])
  house.value = houseResponse.data
  deals.value = Array.isArray(dealResponse.data) ? dealResponse.data : []
}

function copyCoords() {
  if (!house.value?.latitude || !house.value?.longitude) {
    alert('위·경도 정보가 없습니다.')
    return
  }
  const text = `${house.value.latitude}, ${house.value.longitude}`
  navigator.clipboard?.writeText(text)
  alert(`좌표가 복사되었습니다: ${text}`)
}

onMounted(load)
</script>

<template>
  <PageHero
    :crumbs="['홈', '주택 정보', house?.aptNm || '상세']"
    :title="house?.aptNm || '주택 정보'"
    :lede="house ? `${house.sidoName || ''} ${house.gugunName || ''} ${house.umdNm || ''} · 건축 ${house.buildYear || '-'}년 · 총 거래 ${house.dealCount || 0}건` : ''"
  >
    <template #right>
      <RouterLink to="/houses" class="btn btn-outline-secondary btn-sm">← 목록</RouterLink>
      <RouterLink v-if="props.user?.admin && house" :to="`/houses/${house.aptSeq}/edit`" class="btn btn-outline-warning btn-sm">수정</RouterLink>
      <button v-else-if="props.user" class="btn btn-outline-primary btn-sm">관심지역 추가</button>
    </template>
  </PageHero>

  <main v-if="house" class="container page-container">
    <div class="house-detail-grid">
      <div class="card">
        <div class="card-header section-head"><div class="title"><span class="bar"></span>기본 정보</div><span class="chip mono">apt_seq · {{ house.aptSeq }}</span></div>
        <div class="card-body">
          <dl class="dl-gov detail-dl">
            <dt>시도 / 시군구</dt><dd>{{ house.sidoName }} {{ house.gugunName }}</dd>
            <dt>법정동</dt><dd>{{ house.umdNm }} <span class="text-muted mono small">({{ house.umdCd }})</span></dd>
            <dt>시군구 코드</dt><dd class="mono">{{ house.sggCd }}</dd>
            <dt>지번</dt><dd class="mono">{{ house.jibun }}</dd>
            <dt>도로명 주소</dt><dd>{{ roadAddress || '-' }}</dd>
            <dt>건축년도</dt><dd>{{ house.buildYear ? `${house.buildYear}년` : '-' }}</dd>
            <dt>위도 / 경도</dt><dd class="mono small">{{ house.latitude || '-' }} / {{ house.longitude || '-' }}</dd>
            <dt>총 거래 건수</dt><dd><span class="mono fw-bold">{{ number(house.dealCount) }}</span> 건</dd>
          </dl>
        </div>
      </div>

      <div class="card">
        <div class="card-header section-head"><div class="title"><span class="bar"></span>위치</div><span class="chip">{{ roadAddress || '-' }}</span></div>
        <KakaoMap class="detail-map" :marker="houseMarker" :center-text="`${house.sidoName || ''} ${house.gugunName || ''} ${house.umdNm || ''}`" height="340px" />
        <div class="card-footer flex gap-2">
          <RouterLink :to="{ path: '/', query: { commercialLawdCd: house.sggCd, commercialDong: house.umdNm } }" class="btn btn-outline-secondary btn-sm">주변 상권 보기</RouterLink>
          <button class="btn btn-link btn-sm ms-auto" type="button" @click="copyCoords">좌표 복사</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header section-head">
        <div class="title"><span class="bar"></span>최근 거래 내역 <span class="badge text-bg-secondary">총 {{ deals.length }}건</span></div>
        <select v-model="sort" class="form-select form-select-sm sort-select"><option value="latest">최신순</option><option value="price_desc">금액 높은순</option></select>
      </div>
      <table class="table table-hover">
        <thead><tr><th>거래일</th><th class="text-end">면적 (㎡)</th><th class="text-end">금액 (만원)</th><th class="text-end">층</th><th>동</th></tr></thead>
        <tbody>
          <tr v-if="sortedDeals.length === 0"><td colspan="5" class="empty-state">거래 내역이 없습니다.</td></tr>
          <tr v-for="deal in sortedDeals" :key="`${deal.date}-${deal.amount}-${deal.floor}`">
            <td class="mono">{{ deal.date }}</td>
            <td class="text-end mono">{{ Number(deal.area).toFixed(2) }}</td>
            <td class="text-end"><span class="money">{{ number(deal.amount) }}</span></td>
            <td class="text-end mono">{{ deal.floor }}F</td>
            <td class="small text-muted">{{ deal.dong }}</td>
          </tr>
        </tbody>
      </table>
      <div class="card-footer flex-between">
        <RouterLink to="/houses" class="btn btn-outline-secondary btn-sm">← 목록으로</RouterLink>
        <RouterLink v-if="props.user?.admin" :to="`/houses/${house.aptSeq}/edit`" class="btn btn-primary btn-sm">수정</RouterLink>
      </div>
    </div>
  </main>
</template>
