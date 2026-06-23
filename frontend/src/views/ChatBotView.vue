<script setup>
import { nextTick, ref } from 'vue'
import { sendChatMessage } from '../api/chat'

const input = ref('')
const loading = ref(false)
const messages = ref([
  {
    role: 'bot',
    content: '안녕하세요. 원하는 기준 위치부터 알려주세요. 예: 서울역 근처에서 살고 싶어요.'
  }
])
const listRef = ref(null)

async function submit() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const history = messages.value.slice(0, -1).map(({ role, content }) => ({ role, content }))
    const response = await sendChatMessage({ message: text, history })
    messages.value.push({
      role: 'bot',
      content: response?.data?.message || '응답을 가져오지 못했습니다.'
    })
  } catch (error) {
    messages.value.push({
      role: 'bot',
      content: error?.response?.data?.message || '챗봇 응답 중 오류가 발생했습니다.'
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}
</script>

<template>
  <main class="chatbot-page">
    <section class="chatbot-shell" aria-label="맞춤 동네 추천 챗봇">
      <header class="chatbot-header">
        <p class="chatbot-eyebrow">AI 동네 추천</p>
        <h1>대화로 조건을 입력하세요</h1>
      </header>

      <div ref="listRef" class="chatbot-messages">
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="chatbot-row"
          :class="message.role === 'user' ? 'is-user' : 'is-bot'"
        >
          <div class="chatbot-bubble">
            {{ message.content }}
          </div>
        </div>
        <div v-if="loading" class="chatbot-row is-bot">
          <div class="chatbot-bubble chatbot-bubble--loading">답변을 작성하는 중...</div>
        </div>
      </div>

      <form class="chatbot-form" @submit.prevent="submit">
        <input
          v-model="input"
          class="chatbot-input"
          type="text"
          placeholder="예: 서울역 5km 안, 매매 6억 이하 아파트를 찾고 싶어요"
          :disabled="loading"
        />
        <button class="chatbot-send" type="submit" :disabled="loading || !input.trim()">
          전송
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.chatbot-page {
  min-height: calc(100vh - 160px);
  padding: 48px 20px;
  background: #f6f7fb;
}

.chatbot-shell {
  display: flex;
  flex-direction: column;
  width: min(920px, 100%);
  height: 680px;
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.chatbot-header {
  padding: 22px 24px 18px;
  border-bottom: 1px solid #e5e7eb;
}

.chatbot-eyebrow {
  margin: 0 0 6px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.chatbot-header h1 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  font-weight: 800;
}

.chatbot-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f9fafb;
}

.chatbot-row {
  display: flex;
  margin-bottom: 14px;
}

.chatbot-row.is-user {
  justify-content: flex-end;
}

.chatbot-row.is-bot {
  justify-content: flex-start;
}

.chatbot-bubble {
  max-width: min(72%, 620px);
  padding: 12px 14px;
  border-radius: 8px;
  white-space: pre-wrap;
  line-height: 1.55;
  font-size: 15px;
}

.is-user .chatbot-bubble {
  color: #ffffff;
  background: #2563eb;
}

.is-bot .chatbot-bubble {
  color: #1f2937;
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

.chatbot-bubble--loading {
  color: #6b7280;
}

.chatbot-form {
  display: flex;
  gap: 10px;
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  background: #ffffff;
}

.chatbot-input {
  flex: 1;
  min-width: 0;
  height: 46px;
  padding: 0 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 15px;
}

.chatbot-input:focus {
  border-color: #2563eb;
  outline: none;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.14);
}

.chatbot-send {
  width: 86px;
  border: 0;
  border-radius: 8px;
  color: #ffffff;
  background: #111827;
  font-weight: 700;
}

.chatbot-send:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

@media (max-width: 640px) {
  .chatbot-page {
    padding: 20px 12px;
  }

  .chatbot-shell {
    height: calc(100vh - 150px);
  }

  .chatbot-bubble {
    max-width: 88%;
  }

  .chatbot-form {
    flex-direction: column;
  }

  .chatbot-send {
    width: 100%;
    height: 44px;
  }
}
</style>
