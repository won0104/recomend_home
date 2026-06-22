<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { login } from '../api/auth'
import PageHero from '../components/PageHero.vue'

const emit = defineEmits(['login-success'])
const router = useRouter()
const userId = ref('')
const password = ref('')
const rememberMe = ref(false)
const errorMessage = ref('')
const submitting = ref(false)

async function submitLogin() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await login({ userId: userId.value.trim(), password: password.value, rememberMe: rememberMe.value })
    emit('login-success')
    router.push('/')
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || '아이디 또는 비밀번호가 올바르지 않습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <PageHero
    :crumbs="['홈', '로그인']"
    title="로그인"
    lede="가입하신 아이디와 비밀번호로 로그인해주세요."
  />

  <main class="container page-container">
    <div class="card auth-card">
      <div class="card-header">
        <h2>로그인</h2>
        <div class="lede">관심지역 저장, 게시판 글쓰기는 로그인 후 이용 가능합니다.</div>
      </div>
      <div class="card-body">
        <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>
        <form class="grid" style="gap:12px;" autocomplete="on" @submit.prevent="submitLogin">
          <div>
            <label for="loginId" class="form-label">아이디</label>
            <input id="loginId" v-model="userId" name="userId" type="text" class="form-control" required autofocus
              autocomplete="username" placeholder="아이디" />
          </div>
          <div>
            <label for="loginPassword" class="form-label">비밀번호</label>
            <input id="loginPassword" v-model="password" name="password" type="password" class="form-control" required
              autocomplete="current-password" placeholder="비밀번호" />
          </div>
          <div class="login-options">
            <label class="remember-label">
              <input v-model="rememberMe" name="remember-me" type="checkbox" /> 로그인 상태 유지
            </label>
            <button type="button" class="btn btn-link btn-sm" @click="errorMessage = '아이디·비밀번호 찾기는 준비 중입니다.'">
              아이디·비밀번호 찾기
            </button>
          </div>
          <button type="submit" class="btn btn-primary auth-submit" :disabled="submitting">
            {{ submitting ? '로그인 중' : '로그인' }}
          </button>
        </form>
        <hr />
        <div class="auth-bottom-text">
          아직 회원이 아니신가요?
          <RouterLink to="/register" class="fw-bold auth-inline-link">회원가입</RouterLink>
        </div>
      </div>
    </div>

    <div class="auth-policy-text">
      본 시스템은 학습용 프로젝트이며, 입력된 정보는 보안 절차에 따라 처리됩니다.<br />
      공공데이터포털 OpenAPI 약관 및 본 사이트의 이용약관에 동의하신 것으로 간주됩니다.
    </div>
  </main>
</template>
