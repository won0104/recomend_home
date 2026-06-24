<script setup>
import { onMounted, ref } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { fetchMe, logout, refresh } from './api/auth'
import SiteHeader from './components/SiteHeader.vue'
import SiteFooter from './components/SiteFooter.vue'

const router = useRouter()
const user = ref(null)
const loading = ref(true)

async function loadMe() {
  try {
    await refresh()
    const response = await fetchMe()
    user.value = response.data
  } catch {
    user.value = null
  } finally {
    loading.value = false
  }
}

async function handleLogout() {
  await logout()
  user.value = null
  router.push('/')
}

onMounted(loadMe)
</script>

<template>
  <div class="page-bg">
    <SiteHeader :user="user" @logout="handleLogout" />
    <RouterView :user="user" :auth-loading="loading" @login-success="loadMe" @user-updated="loadMe" />
    <SiteFooter />
  </div>
</template>
