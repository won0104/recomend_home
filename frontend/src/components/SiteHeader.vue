<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import logoUrl from '../assets/logo_nuki.png'

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

const userInitial = computed(() => {
  if (!props.user?.name) return '?'
  return props.user.name.charAt(0)
})
</script>

<template>
  <header class="site-header">
    <div class="site-header-inner">

      <RouterLink to="/" class="brand">
        <div class="brand-logo-mark">
          <img :src="logoUrl" alt="" aria-hidden="true" />
        </div>
        <div class="brand-text">
          <span class="brand-name">
            <span class="brand-name-dark">구해즈</span>&nbsp;<span class="brand-name-green">홈줘</span>
          </span>
          <span class="brand-sub">집 찾는 순간, 든든한 내 편</span>
        </div>
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
          <RouterLink to="/mypage" class="auth-avatar" :title="`${props.user.name} · 마이페이지`">
            {{ userInitial }}
          </RouterLink>
          <div class="auth-info">
            <span class="auth-name">{{ props.user.name }}</span>
            <span v-if="props.user.admin" class="auth-admin-badge">ADMIN</span>
          </div>
          <div class="auth-actions">
            <RouterLink v-if="props.user.admin" to="/admin/users" class="btn btn-sm btn-outline-secondary">사용자 관리</RouterLink>
            <button type="button" class="btn btn-sm btn-outline-secondary" @click="emit('logout')">로그아웃</button>
          </div>
        </template>
      </div>

    </div>
  </header>
</template>

<style scoped>
.site-header {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.site-header-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 68px;
  padding: 0 32px;
  display: grid;
  grid-template-columns: minmax(210px, 250px) 1fr minmax(200px, auto);
  align-items: center;
  gap: 16px;
}

/* ── 브랜드 로고 ── */
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  min-width: 0;
}
.brand:hover { text-decoration: none; }

.brand-logo-mark {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  transition: filter .2s, transform .2s;
}
.brand-logo-mark img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}
.brand:hover .brand-logo-mark {
  transform: translateY(-1px);
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.brand-name {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.1;
  white-space: nowrap;
  display: flex;
  align-items: baseline;
}
.brand-name-dark {
  color: #132842;
}
.brand-name-green {
  color: #168a49;
}
.brand-sub {
  font-size: 9.5px;
  color: #536172;
  font-weight: 600;
  letter-spacing: 0;
  white-space: nowrap;
}

/* ── 네비게이션 ── */
.main-nav {
  display: flex;
  align-items: stretch;
  justify-content: center;
  height: 100%;
  gap: 0;
  min-width: 0;
}

.main-nav a.nav-link {
  display: flex;
  align-items: center;
  padding: 0 14px;
  font-size: 13.5px;
  font-weight: 600;
  color: #6b7280;
  text-decoration: none;
  white-space: nowrap;
  position: relative;
  transition: color .15s;
}
.main-nav a.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 10px;
  right: 10px;
  height: 2.5px;
  background: #079455;
  border-radius: 2px 2px 0 0;
  transform: scaleX(0);
  transition: transform .2s cubic-bezier(.4, 0, .2, 1);
}
.main-nav a.nav-link:hover {
  color: #111827;
  text-decoration: none;
}
.main-nav a.nav-link.is-active {
  color: #079455;
  font-weight: 700;
}
.main-nav a.nav-link.is-active::after {
  transform: scaleX(1);
}

/* ── 인증 영역 ── */
.header-auth {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 0;
}

.auth-avatar {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #ecfdf3;
  border: 2px solid #6ee7b7;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  color: #065f46;
  text-decoration: none;
  transition: border-color .15s, box-shadow .15s;
}
.auth-avatar:hover {
  border-color: #079455;
  box-shadow: 0 0 0 3px rgba(7, 148, 85, 0.15);
  text-decoration: none;
}

.auth-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.auth-name {
  font-size: 13px;
  font-weight: 700;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 80px;
}
.auth-admin-badge {
  font-size: 10px;
  font-weight: 800;
  color: #b45309;
  background: #fef3c7;
  border-radius: 4px;
  padding: 1px 5px;
  letter-spacing: 0.04em;
  width: fit-content;
}

.auth-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 1024px) {
  .auth-info { display: none; }
}

@media (max-width: 980px) {
  .site-header-inner {
    grid-template-columns: 1fr;
    height: auto;
    padding: 12px 20px;
    gap: 10px;
  }
  .main-nav {
    justify-content: flex-start;
    overflow-x: auto;
    height: auto;
    padding-bottom: 2px;
  }
  .main-nav a.nav-link {
    padding: 8px 12px;
  }
  .main-nav a.nav-link::after {
    bottom: 0;
    left: 6px;
    right: 6px;
  }
  .header-auth {
    justify-content: flex-start;
  }
}
</style>
