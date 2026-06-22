<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import ArticleForm from '../components/ArticleForm.vue'
import { createNotice, fetchNotice, updateNotice } from '../api/notices'

const route = useRoute()
const router = useRouter()
const item = ref({ title: '', content: '' })
const submitting = ref(false)
const isEdit = computed(() => Boolean(route.params.id))
const pageTitle = computed(() => (isEdit.value ? '공지사항 수정' : '공지사항 등록'))

async function load() {
  if (!isEdit.value) return
  const response = await fetchNotice(route.params.id)
  item.value = response.data
}

async function submit(payload) {
  submitting.value = true
  try {
    const response = isEdit.value
      ? await updateNotice(route.params.id, payload)
      : await createNotice(payload)
    router.push(`/notices/${response.data.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <PageHero :crumbs="['홈', '공지사항', pageTitle]" :title="pageTitle" :lede="isEdit ? '기존 공지를 수정합니다.' : '회원에게 안내할 공지사항을 작성해 주세요.'" />
  <ArticleForm
    :title="item.title"
    :content="item.content"
    :page-title="pageTitle"
    :submit-text="isEdit ? '수정 저장' : '등록'"
    :cancel-to="isEdit ? `/notices/${route.params.id}` : '/notices'"
    :submitting="submitting"
    @submit="submit"
  />
</template>
