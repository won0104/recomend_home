<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import ArticleList from '../components/ArticleList.vue'
import { fetchBoards } from '../api/boards'

const props = defineProps({ user: { type: Object, default: null } })
const route = useRoute()
const router = useRouter()
const items = ref([])
const keyword = ref(String(route.query.keyword || ''))
const loading = ref(false)

async function load() {
  loading.value = true
  const response = await fetchBoards(keyword.value)
  items.value = Array.isArray(response.data) ? response.data : []
  loading.value = false
}

function search() {
  router.replace({ path: '/boards', query: keyword.value ? { keyword: keyword.value } : {} })
  load()
}

onMounted(load)
</script>

<template>
  <PageHero :crumbs="['홈', '공유게시판']" title="공유게시판" lede="주택·상권·실거래가에 대한 회원 간 정보 공유 공간입니다.">
    <template #right>
      <RouterLink v-if="props.user" to="/boards/new" class="btn btn-primary btn-sm">+ 글쓰기</RouterLink>
      <RouterLink v-else to="/login" class="btn btn-outline-primary btn-sm">로그인 후 글쓰기</RouterLink>
    </template>
  </PageHero>

  <main class="container page-container">
    <div class="card">
      <div class="filter-rail">
        <form class="list-filter-form" @submit.prevent="search">
          <div class="flex-grow-1">
            <label class="form-label small">검색</label>
            <input v-model="keyword" type="text" class="form-control form-control-sm" placeholder="제목 또는 내용으로 검색" />
          </div>
          <select class="form-select form-select-sm filter-select"><option>제목</option><option>내용</option><option>작성자</option></select>
          <button type="submit" class="btn btn-outline-primary btn-sm">검색</button>
          <button type="button" class="btn btn-outline-secondary btn-sm" @click="keyword = ''; search()">초기화</button>
        </form>
      </div>
      <div class="card-header section-head list-section-head">
        <div class="title"><span class="bar"></span><span>공유게시판 목록</span><span class="badge text-bg-secondary">총 {{ items.length }}건</span></div>
        <select class="form-select form-select-sm sort-select"><option>최신순</option><option>조회순</option></select>
      </div>
      <div>
        <ArticleList :items="items" base-path="/boards" empty-text="등록된 게시글이 없습니다." />
      </div>
      <div class="card-footer flex-between">
        <RouterLink to="/" class="btn btn-outline-secondary btn-sm">← 메인으로</RouterLink>
        <span class="small text-muted">{{ loading ? '불러오는 중...' : `총 ${items.length}건` }}</span>
      </div>
    </div>
  </main>
</template>
