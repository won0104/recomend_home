<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import { deleteNotice, fetchNotice } from '../api/notices'
import { authorName, formatDateTime } from '../utils/format'

const props = defineProps({ user: { type: Object, default: null } })
const route = useRoute()
const router = useRouter()
const item = ref(null)

async function load() {
  const response = await fetchNotice(route.params.id)
  item.value = response.data
}

async function remove() {
  if (!confirm('정말 삭제하시겠습니까?')) return
  await deleteNotice(route.params.id)
  router.push('/notices')
}

onMounted(load)
</script>

<template>
  <PageHero :crumbs="['홈', '공지사항', `공지 #${route.params.id}`]" :title="item?.title || '공지사항 상세'">
    <template #right><RouterLink to="/notices" class="btn btn-outline-secondary btn-sm">← 목록</RouterLink></template>
  </PageHero>
  <main class="container page-container">
    <div v-if="item" class="card">
      <div class="card-header article-header">
        <div class="article-meta">
          <span class="chip is-accent mono">공지 #{{ item.id }}</span>
          <span>작성자 <strong>{{ authorName(item) }}</strong></span>
          <span>작성일 <span class="mono">{{ formatDateTime(item.createdAt) }}</span></span>
          <span v-if="item.updatedAt && item.updatedAt !== item.createdAt">수정일 <span class="mono">{{ formatDateTime(item.updatedAt) }}</span></span>
          <span>조회수 <span class="mono fw-bold">{{ item.viewCount ?? 0 }}</span></span>
        </div>
      </div>
      <div class="card-body article-body">{{ item.content }}</div>
      <div class="card-footer flex-between">
        <RouterLink to="/notices" class="btn btn-outline-secondary btn-sm">← 목록으로</RouterLink>
        <div v-if="props.user?.admin" class="flex gap-2">
          <RouterLink :to="`/notices/${item.id}/edit`" class="btn btn-primary btn-sm">수정</RouterLink>
          <button class="btn btn-outline-danger btn-sm" @click="remove">삭제</button>
        </div>
      </div>
    </div>
  </main>
</template>
