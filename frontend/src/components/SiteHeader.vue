<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

const props = defineProps({
  user: { type: Object, default: null }
})
const emit = defineEmits(['logout'])
const route = useRoute()

const active = computed(() => {
  const path = route.path
  if (path === '/' || path.startsWith('/neighborhood-recommend') || path.startsWith('/places')) return 'recommend'
  if (path.startsWith('/chat')) return 'chatbot'
  if (path.startsWith('/home')) return 'dashboard-home'
  if (path.startsWith('/houses')) return 'houseinfo'
  if (path.startsWith('/notices')) return 'notice'
  if (path.startsWith('/boards')) return 'board'
  return ''
})
</script>

<template>
  <header class="site-header">
    <div class="site-header-inner">
      <RouterLink to="/" class="brand">
        <span class="brand-mark">SH</span>
        <span>
          SSAFY HOME
          <br />
          <small>부동산 실거래가 정보</small>
        </span>
      </RouterLink>

      <nav class="main-nav">
        <RouterLink to="/" class="nav-link" :class="{ 'is-active': active === 'recommend' }">동네 추천</RouterLink>
        <RouterLink to="/chat" class="nav-link" :class="{ 'is-active': active === 'chatbot' }">AI 챗봇</RouterLink>
        <RouterLink to="/home" class="nav-link" :class="{ 'is-active': active === 'dashboard-home' }">실거래 검색</RouterLink>
        <RouterLink to="/houses" class="nav-link" :class="{ 'is-active': active === 'houseinfo' }">주택 정보</RouterLink>
        <RouterLink to="/notices" class="nav-link" :class="{ 'is-active': active === 'notice' }">공지사항</RouterLink>
        <RouterLink to="/boards" class="nav-link" :class="{ 'is-active': active === 'board' }">공유게시판</RouterLink>
      </nav>

      <div class="header-auth">
        <template v-if="!props.user">
          <RouterLink to="/login" class="btn btn-outline-primary btn-sm">로그인</RouterLink>
          <RouterLink to="/register" class="btn btn-primary btn-sm">회원가입</RouterLink>
        </template>
        <template v-else>
          <RouterLink to="/mypage" class="btn btn-outline-info btn-sm">마이페이지</RouterLink>
          <RouterLink v-if="props.user.admin" to="/admin/users" class="btn btn-outline-warning btn-sm">사용자 관리</RouterLink>
          <span class="badge text-bg-secondary">
            {{ props.user.name }}<template v-if="props.user.admin"> · ADMIN</template>
          </span>
          <button type="button" class="btn btn-outline-secondary btn-sm" @click="emit('logout')">로그아웃</button>
        </template>
      </div>
    </div>
  </header>
</template>
