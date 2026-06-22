<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import { fetchHouse, updateHouse } from '../api/houses'

const route = useRoute()
const router = useRouter()
const house = ref(null)
const form = ref({ aptNm: '', buildYear: '', latitude: '', longitude: '' })

async function load() {
  const response = await fetchHouse(route.params.aptSeq)
  house.value = response.data
  form.value = {
    aptNm: house.value.aptNm || '',
    buildYear: house.value.buildYear || '',
    latitude: house.value.latitude || '',
    longitude: house.value.longitude || ''
  }
}

async function submit() {
  await updateHouse(route.params.aptSeq, {
    aptNm: form.value.aptNm,
    buildYear: form.value.buildYear ? Number(form.value.buildYear) : null,
    latitude: form.value.latitude,
    longitude: form.value.longitude
  })
  router.push(`/houses/${route.params.aptSeq}`)
}

onMounted(load)
</script>

<template>
  <PageHero :crumbs="['홈', '주택 정보', house?.aptNm || '', '수정']" title="주택 정보 수정" lede="관리자만 수정할 수 있습니다. 위치 정보는 변경할 수 없습니다." />
  <main v-if="house" class="container page-container">
    <div class="card house-form-card">
      <div class="card-header section-head"><div class="title"><span class="bar"></span>주택 정보 수정</div><span class="chip mono">apt_seq · {{ house.aptSeq }}</span></div>
      <div class="card-body">
        <form class="grid register-grid" @submit.prevent="submit">
          <div class="span-12"><label class="form-label small">아파트 식별자 (apt_seq)</label><input class="form-control form-control-sm mono" :value="house.aptSeq" disabled /></div>
          <div class="span-12"><label class="form-label small">위치 (수정 불가)</label><input class="form-control form-control-sm" :value="`${house.sidoName || ''} ${house.gugunName || ''} ${house.umdNm || ''} ${house.jibun || ''}`" disabled /></div>
          <div class="span-12"><label class="form-label">아파트명</label><input v-model="form.aptNm" maxlength="40" required class="form-control" /></div>
          <div class="span-4"><label class="form-label">건축년도</label><input v-model="form.buildYear" type="number" min="1900" max="2100" class="form-control mono" /></div>
          <div class="span-4"><label class="form-label">위도</label><input v-model="form.latitude" class="form-control mono" placeholder="예: 37.5665" /></div>
          <div class="span-4"><label class="form-label">경도</label><input v-model="form.longitude" class="form-control mono" placeholder="예: 126.978" /></div>
          <div class="span-12 form-actions flex-between">
            <RouterLink :to="`/houses/${house.aptSeq}`" class="btn btn-outline-secondary">취소</RouterLink>
            <button class="btn btn-primary">저장</button>
          </div>
        </form>
      </div>
    </div>
  </main>
</template>
