<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import ArticleList from '../components/ArticleList.vue'
import { fetchNotices } from '../api/notices'

const props = defineProps({ user: { type: Object, default: null } })
const route = useRoute()
const router = useRouter()
const items = ref([])
const keyword = ref(String(route.query.keyword || ''))

async function load() {
  const response = await fetchNotices(keyword.value)
  items.value = Array.isArray(response.data) ? response.data : []
}

function search() {
  router.replace({ path: '/notices', query: keyword.value ? { keyword: keyword.value } : {} })
  load()
}

onMounted(load)
</script>

<template>
  <PageHero :crumbs="['홈', '공지사항']" title="공지사항" lede="서비스 변경사항·점검·데이터 갱신 등 운영 공지를 안내합니다.">
    <template #right>
      <RouterLink v-if="props.user?.admin" to="/notices/new" class="btn btn-primary btn-sm">+ 새 공지 등록</RouterLink>
    </template>
  </PageHero>
  <main class="container page-container">
    <div class="card">
      <div class="filter-rail">
        <form class="list-filter-form" @submit.prevent="search">
          <div class="flex-grow-1"><label class="form-label small">검색</label><input v-model="keyword" class="form-control form-control-sm" placeholder="제목 또는 내용으로 검색" /></div>
          <select class="form-select form-select-sm filter-select"><option>제목</option><option>내용</option><option>작성자</option></select>
          <button class="btn btn-outline-primary btn-sm">검색</button>
          <button type="button" class="btn btn-outline-secondary btn-sm" @click="keyword = ''; search()">초기화</button>
        </form>
      </div>
      <div class="card-header section-head list-section-head">
        <div class="title"><span class="bar"></span><span>공지사항 목록</span><span class="badge text-bg-secondary">총 {{ items.length }}건</span></div>
        <select class="form-select form-select-sm sort-select"><option>최신순</option><option>조회순</option></select>
      </div>
      <ArticleList :items="items" base-path="/notices" empty-text="등록된 공지사항이 없습니다." />
      <div class="card-footer flex-between"><RouterLink to="/" class="btn btn-outline-secondary btn-sm">← 메인으로</RouterLink><span class="small text-muted">총 {{ items.length }}건</span></div>
    </div>
  </main>
</template>
