<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { deleteMe, updateMe } from '../api/auth'
import { fetchMyBoards } from '../api/boards'
import { deleteFavorite, fetchFavorites } from '../api/favorites'
import { formatDate, formatDateTime } from '../utils/format'

const props = defineProps({ user: { type: Object, default: null } })
const emit = defineEmits(['user-updated'])
const router = useRouter()
const favorites = ref([])
const posts = ref([])
const alert = ref(null)
const editOpen = ref(false)
const passwordOpen = ref(false)
const editForm = ref({ name: '', email: '', phone: '' })
const passwordForm = ref({ password: '', confirm: '' })

const initial = computed(() => props.user?.name?.substring(0, 1) || 'U')

watch(() => props.user, (user) => {
  if (!user) return
  editForm.value = { name: user.name || '', email: user.email || '', phone: user.phone || '' }
}, { immediate: true })

async function load() {
  if (!props.user) return
  const [favoriteResponse, postResponse] = await Promise.allSettled([fetchFavorites(), fetchMyBoards()])
  favorites.value = favoriteResponse.value?.data || []
  posts.value = postResponse.value?.data || []
}

function show(type, message) {
  alert.value = { type, message }
}

async function saveProfile() {
  await updateMe(editForm.value)
  editOpen.value = false
  emit('user-updated')
  show('success', '회원 정보가 수정되었습니다.')
}

async function savePassword() {
  if (passwordForm.value.password !== passwordForm.value.confirm) {
    show('danger', '비밀번호가 일치하지 않습니다.')
    return
  }
  await updateMe({ ...editForm.value, password: passwordForm.value.password })
  passwordOpen.value = false
  passwordForm.value = { password: '', confirm: '' }
  show('success', '비밀번호가 변경되었습니다.')
}

async function removeFavorite(key) {
  if (!confirm('이 관심지역을 삭제하시겠습니까?')) return
  await deleteFavorite(key)
  await load()
}

async function withdraw() {
  if (!confirm('정말 탈퇴하시겠습니까? 관심지역 등 모든 데이터가 삭제됩니다.')) return
  await deleteMe()
  alert('탈퇴가 완료되었습니다.')
  router.push('/')
  emit('user-updated')
}

onMounted(load)
</script>

<template>
  <PageHero :crumbs="['홈', '마이페이지']" title="마이페이지" lede="회원 정보, 관심지역, 작성한 글을 확인하고 관리할 수 있습니다.">
    <template #right><button type="button" class="btn btn-outline-danger btn-sm" @click="withdraw">회원 탈퇴</button></template>
  </PageHero>

  <main v-if="props.user" class="container page-container">
    <div v-if="alert" class="alert" :class="`alert-${alert.type}`">{{ alert.message }}</div>

    <div class="mypage-grid">
      <div class="card">
        <div class="card-header section-head"><div class="title"><span class="bar"></span>회원 정보</div><span class="chip is-primary">{{ props.user.role || 'USER' }}</span></div>
        <div class="card-body">
          <div class="profile-head">
            <div class="avatar is-primary profile-avatar">{{ initial }}</div>
            <div><div class="profile-name">{{ props.user.name }}</div><div class="small text-muted mono">@{{ props.user.userId }}</div></div>
          </div>
          <dl class="dl-gov detail-dl">
            <dt>아이디</dt><dd class="mono">{{ props.user.userId }}</dd>
            <dt>이메일</dt><dd>{{ props.user.email }}</dd>
            <dt>전화번호</dt><dd class="mono">{{ props.user.phone }}</dd>
            <dt>가입일</dt><dd class="mono small">{{ formatDateTime(props.user.createdAt) }}</dd>
            <dt>최근 수정</dt><dd class="mono small">{{ formatDateTime(props.user.updatedAt) }}</dd>
          </dl>
        </div>
        <div class="card-footer flex gap-2">
          <button type="button" class="btn btn-primary btn-sm" @click="editOpen = true">정보 수정</button>
          <button type="button" class="btn btn-outline-secondary btn-sm" @click="passwordOpen = true">비밀번호 변경</button>
          <RouterLink v-if="props.user.admin" to="/admin/users" class="btn btn-outline-warning btn-sm ms-auto">사용자 관리</RouterLink>
        </div>
      </div>

      <div class="card">
        <div class="card-header section-head"><div class="title"><span class="bar"></span>관심지역 <span class="badge text-bg-secondary">{{ favorites.length }}개</span></div><button class="btn btn-outline-primary btn-sm">+ 추가</button></div>
        <table class="table table-sm">
          <thead><tr><th>지역</th><th>LAWD_CD</th><th>추가일</th><th class="text-end">동작</th></tr></thead>
          <tbody>
            <tr v-if="favorites.length === 0"><td colspan="4" class="empty-state">등록된 관심지역이 없습니다.</td></tr>
            <tr v-for="favorite in favorites" :key="favorite.key">
              <td>{{ [favorite.sido, favorite.sigungu, favorite.emd].filter(Boolean).join(' ') }}</td>
              <td class="mono small">{{ favorite.lawdCd }}</td>
              <td class="mono small">{{ formatDate(favorite.createdAt) }}</td>
              <td class="text-end"><button class="btn btn-outline-danger btn-sm" @click="removeFavorite(favorite.key)">삭제</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card my-posts-card">
      <div class="card-header section-head"><div class="title"><span class="bar"></span>내가 쓴 글</div><RouterLink to="/boards" class="btn btn-link btn-sm">공유게시판 전체보기 →</RouterLink></div>
      <table class="table table-hover">
        <thead><tr><th class="text-center post-no">No</th><th>제목</th><th class="text-end post-views">조회</th><th class="post-date">작성일</th></tr></thead>
        <tbody>
          <tr v-if="posts.length === 0"><td colspan="4" class="empty-state">작성한 글이 없습니다.</td></tr>
          <tr v-for="post in posts" :key="post.id">
            <td class="text-center mono">{{ post.id }}</td>
            <td><RouterLink :to="`/boards/${post.id}`">{{ post.title }}</RouterLink></td>
            <td class="text-end mono">{{ post.viewCount || 0 }}</td>
            <td class="mono small">{{ formatDateTime(post.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <ConfirmModal :open="editOpen" title="회원 정보 수정" @close="editOpen = false">
      <form @submit.prevent="saveProfile">
        <div class="modal-card-body grid register-grid">
          <div class="span-6"><label class="form-label small">이름</label><input v-model="editForm.name" class="form-control form-control-sm" required /></div>
          <div class="span-6"><label class="form-label small">전화번호</label><input v-model="editForm.phone" class="form-control form-control-sm" /></div>
          <div class="span-12"><label class="form-label small">이메일</label><input v-model="editForm.email" type="email" class="form-control form-control-sm" required /></div>
        </div>
        <div class="modal-card-footer"><button type="button" class="btn btn-outline-secondary btn-sm" @click="editOpen = false">닫기</button><button class="btn btn-primary btn-sm">저장</button></div>
      </form>
    </ConfirmModal>

    <ConfirmModal :open="passwordOpen" title="비밀번호 변경" @close="passwordOpen = false">
      <form @submit.prevent="savePassword">
        <div class="modal-card-body grid">
          <div><label class="form-label small">새 비밀번호</label><input v-model="passwordForm.password" type="password" class="form-control form-control-sm" required minlength="4" /></div>
          <div><label class="form-label small">새 비밀번호 확인</label><input v-model="passwordForm.confirm" type="password" class="form-control form-control-sm" required minlength="4" /></div>
        </div>
        <div class="modal-card-footer"><button type="button" class="btn btn-outline-secondary btn-sm" @click="passwordOpen = false">닫기</button><button class="btn btn-primary btn-sm">변경</button></div>
      </form>
    </ConfirmModal>
  </main>
</template>
