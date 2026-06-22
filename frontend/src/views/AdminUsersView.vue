<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import { changeMemberRole, fetchMembers } from '../api/admin'
import { formatDateTime } from '../utils/format'

const props = defineProps({ user: { type: Object, default: null } })
const users = ref([])
const keyword = ref('')
const role = ref('')
const alert = ref(null)

const filtered = computed(() => users.value.filter((user) => {
  const matchesRole = !role.value || user.role === role.value
  const q = keyword.value.trim().toLowerCase()
  const matchesKeyword = !q || [user.userId, user.name, user.email].some((value) => String(value || '').toLowerCase().includes(q))
  return matchesRole && matchesKeyword
}))
const adminCount = computed(() => users.value.filter((user) => user.role === 'ADMIN').length)
const recentCount = computed(() => {
  const now = Date.now()
  const sevenDays = 7 * 24 * 60 * 60 * 1000
  return users.value.filter((user) => user.createdAt && now - new Date(user.createdAt).getTime() <= sevenDays).length
})

async function load() {
  const response = await fetchMembers()
  users.value = Array.isArray(response.data) ? response.data : []
}

async function setRole(target, nextRole) {
  if (!confirm(`${target.userId} 님의 권한을 ${nextRole} 으로 변경하시겠습니까?`)) return
  await changeMemberRole(target.userId, nextRole)
  alert.value = { type: 'success', message: `${target.userId} 의 권한이 ${nextRole} 으로 변경되었습니다.` }
  await load()
}

onMounted(load)
</script>

<template>
  <PageHero
    :crumbs="['홈', '관리자', '사용자 관리']"
    title="사용자 관리"
    lede="전체 회원 목록을 조회하고 권한을 변경할 수 있습니다. 본인 계정은 직접 강등할 수 없습니다."
  />

  <main class="container page-container">
    <div v-if="alert" class="alert" :class="`alert-${alert.type}`">{{ alert.message }}</div>
    <div class="grid stat-grid">
      <div class="card stat-card"><div class="stat-label">전체 회원</div><div class="stat-value"><span>{{ users.length }}</span><span class="stat-unit">명</span></div></div>
      <div class="card stat-card"><div class="stat-label">관리자</div><div class="stat-value"><span>{{ adminCount }}</span><span class="stat-unit">명</span></div></div>
      <div class="card stat-card"><div class="stat-label">일반 회원</div><div class="stat-value"><span>{{ users.length - adminCount }}</span><span class="stat-unit">명</span></div></div>
      <div class="card stat-card"><div class="stat-label">최근 7일 가입</div><div class="stat-value"><span>{{ recentCount }}</span><span class="stat-unit">명</span></div></div>
    </div>

    <div class="card">
      <div class="filter-rail">
        <form class="list-filter-form" @submit.prevent>
          <div class="flex-grow-1"><label class="form-label small">검색</label><input v-model="keyword" class="form-control form-control-sm" placeholder="아이디·이름·이메일로 검색" /></div>
          <select v-model="role" class="form-select form-select-sm filter-select"><option value="">전체 권한</option><option value="ADMIN">ADMIN</option><option value="USER">USER</option></select>
          <button class="btn btn-outline-primary btn-sm">검색</button>
        </form>
      </div>
      <div class="card-header section-head list-section-head"><div class="title"><span class="bar"></span><span>회원 목록</span><span class="badge text-bg-secondary">{{ filtered.length }}명</span></div><span class="small text-muted">정렬: 가입일 최신순</span></div>
      <table class="table table-hover">
        <thead><tr><th>아이디</th><th>이름</th><th>이메일</th><th>전화번호</th><th>권한</th><th>가입일</th><th class="text-end role-cell">권한 변경</th></tr></thead>
        <tbody>
          <tr v-if="filtered.length === 0"><td colspan="7" class="empty-state">사용자가 없습니다.</td></tr>
          <tr v-for="member in filtered" :key="member.userId">
            <td class="mono fw-semibold">{{ member.userId }}</td>
            <td>{{ member.name }}</td>
            <td class="small">{{ member.email }}</td>
            <td class="small mono text-muted">{{ member.phone || '—' }}</td>
            <td><span class="chip" :class="{ 'is-accent': member.role === 'ADMIN' }">{{ member.role }}</span></td>
            <td class="small text-muted mono">{{ formatDateTime(member.createdAt) }}</td>
            <td class="text-end">
              <span v-if="member.userId === props.user?.userId" class="small text-muted">본인</span>
              <button v-else-if="member.role === 'ADMIN'" class="btn btn-outline-warning btn-sm" @click="setRole(member, 'USER')">일반 회원으로 강등</button>
              <button v-else class="btn btn-outline-success btn-sm" @click="setRole(member, 'ADMIN')">관리자로 승격</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="card-footer"><RouterLink to="/mypage" class="btn btn-outline-secondary btn-sm">← 마이페이지</RouterLink></div>
    </div>
  </main>
</template>
