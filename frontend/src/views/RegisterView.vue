<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { registerMember } from '../api/auth'
import PageHero from '../components/PageHero.vue'

const router = useRouter()
const form = ref({ userId: '', name: '', email: '', phone: '', password: '', passwordConfirm: '' })
const errorMessage = ref('')
const submitting = ref(false)

async function submitRegister() {
  errorMessage.value = ''
  if (form.value.password !== form.value.passwordConfirm) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.'
    return
  }
  submitting.value = true
  try {
    await registerMember({
      userId: form.value.userId.trim(),
      name: form.value.name.trim(),
      email: form.value.email.trim(),
      phone: form.value.phone.trim(),
      password: form.value.password
    })
    alert('회원가입이 완료되었습니다. 로그인해주세요.')
    router.push('/login')
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || '회원가입에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <PageHero
    :crumbs="['홈', '회원가입']"
    title="회원가입"
    lede="기본 정보를 입력하시면 관심지역 저장·게시판 작성 등 모든 기능을 이용할 수 있습니다."
  />

  <main class="container page-container">
    <div class="card register-card">
      <div class="card-header section-head">
        <div class="title"><span class="bar"></span>회원 정보 입력</div>
        <span class="chip is-accent">* 필수</span>
      </div>
      <div class="card-body">
        <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>
        <form class="grid register-grid" @submit.prevent="submitRegister">
          <div class="span-6">
            <label for="rId" class="form-label small">아이디 *</label>
            <input id="rId" v-model="form.userId" type="text" class="form-control" required autofocus />
          </div>
          <div class="span-6">
            <label for="rName" class="form-label small">이름 *</label>
            <input id="rName" v-model="form.name" type="text" class="form-control" required />
          </div>
          <div class="span-12">
            <label for="rEmail" class="form-label small">이메일 *</label>
            <input id="rEmail" v-model="form.email" type="email" class="form-control" required />
          </div>
          <div class="span-12">
            <label for="rPhone" class="form-label small">전화번호</label>
            <input id="rPhone" v-model="form.phone" type="text" class="form-control" placeholder="010-1234-5678" />
            <div class="small text-muted field-help">하이픈 포함 입력 (예: 010-1234-5678)</div>
          </div>
          <div class="span-6">
            <label for="rPw" class="form-label small">비밀번호 *</label>
            <input id="rPw" v-model="form.password" type="password" class="form-control" required />
            <div class="small text-muted field-help">영문/숫자 조합 8자 이상</div>
          </div>
          <div class="span-6">
            <label for="rPw2" class="form-label small">비밀번호 확인 *</label>
            <input id="rPw2" v-model="form.passwordConfirm" type="password" class="form-control" required />
          </div>
          <div class="span-12 form-actions">
            <RouterLink to="/login" class="btn btn-outline-secondary">취소</RouterLink>
            <button type="submit" class="btn btn-primary flex-grow-1" :disabled="submitting">회원가입 신청</button>
          </div>
        </form>
      </div>
    </div>
  </main>
</template>
