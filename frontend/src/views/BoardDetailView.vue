<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import { deleteBoard, fetchBoard } from '../api/boards'
import { authorName, formatDateTime } from '../utils/format'

const props = defineProps({ user: { type: Object, default: null } })
const route = useRoute()
const router = useRouter()
const item = ref(null)

const canModify = computed(() => props.user && item.value && (props.user.admin || props.user.userId === item.value.authorUserId))

async function load() {
  const response = await fetchBoard(route.params.id)
  item.value = response.data
}

async function remove() {
  if (!confirm('정말 삭제하시겠습니까?')) return
  await deleteBoard(route.params.id)
  router.push('/boards')
}

onMounted(load)
</script>

<template>
  <PageHero
    :crumbs="['홈', '공유게시판', `게시글 #${route.params.id}`]"
    :title="item?.title || '게시글 상세'"
  >
    <template #right><RouterLink to="/boards" class="btn btn-outline-secondary btn-sm">← 목록</RouterLink></template>
  </PageHero>

  <main class="container page-container">
    <div v-if="item" class="card">
      <div class="card-header article-header">
        <div class="article-meta">
          <span class="chip is-primary mono">#{{ item.id }}</span>
          <span>작성자 <strong>{{ authorName(item) }}</strong></span>
          <span>작성일 <span class="mono">{{ formatDateTime(item.createdAt) }}</span></span>
          <span v-if="item.updatedAt && item.updatedAt !== item.createdAt">수정일 <span class="mono">{{ formatDateTime(item.updatedAt) }}</span></span>
          <span>조회수 <span class="mono fw-bold">{{ item.viewCount ?? 0 }}</span></span>
        </div>
      </div>
      <div class="card-body article-body">{{ item.content }}</div>
      <div class="card-footer flex-between">
        <RouterLink to="/boards" class="btn btn-outline-secondary btn-sm">← 목록으로</RouterLink>
        <div v-if="canModify" class="flex gap-2">
          <RouterLink :to="`/boards/${item.id}/edit`" class="btn btn-primary btn-sm">수정</RouterLink>
          <button type="button" class="btn btn-outline-danger btn-sm" @click="remove">삭제</button>
        </div>
      </div>
    </div>
  </main>
</template>
