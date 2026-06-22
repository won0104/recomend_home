<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import ArticleForm from '../components/ArticleForm.vue'
import { createBoard, fetchBoard, updateBoard } from '../api/boards'

const route = useRoute()
const router = useRouter()
const item = ref({ title: '', content: '' })
const submitting = ref(false)
const isEdit = computed(() => Boolean(route.params.id))
const pageTitle = computed(() => (isEdit.value ? '게시글 수정' : '게시글 등록'))

async function load() {
  if (!isEdit.value) return
  const response = await fetchBoard(route.params.id)
  item.value = response.data
}

async function submit(payload) {
  submitting.value = true
  try {
    const response = isEdit.value
      ? await updateBoard(route.params.id, payload)
      : await createBoard(payload)
    router.push(`/boards/${response.data.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <PageHero :crumbs="['홈', '공유게시판', pageTitle]" :title="pageTitle" :lede="isEdit ? '기존 게시글을 수정합니다.' : '공유할 정보를 작성해 주세요.'" />
  <ArticleForm
    :title="item.title"
    :content="item.content"
    :page-title="pageTitle"
    :submit-text="isEdit ? '수정 저장' : '등록'"
    :cancel-to="isEdit ? `/boards/${route.params.id}` : '/boards'"
    :submitting="submitting"
    @submit="submit"
  />
</template>
