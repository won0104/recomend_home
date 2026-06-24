<script setup>
import { nextTick, ref } from 'vue'
import { sendChatMessage } from '../api/chat'

const input = ref('')
const loading = ref(false)
const messages = ref([
  {
    role: 'bot',
    content: '안녕하세요 😊\n원하는 동네를 함께 찾아드릴게요.\n\n📍 먼저 기준 위치를 알려주세요.\n예: 서울시청 근처, 강남역 주변, 삼성역에서 5km 안'
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
      content: response?.data?.message || '앗, 답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요 🙏'
    })
  } catch (error) {
    messages.value.push({
      role: 'bot',
      content: error?.response?.data?.message || '죄송해요. 답변을 만드는 중 문제가 생겼어요 🙏'
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
        <h1>편하게 말하면 맞춤 동네를 찾아드릴게요 🏡</h1>
        <p class="chatbot-subtitle">위치, 예산, 생활 우선순위를 대화로 알려주세요.</p>
      </header>

      <div ref="listRef" class="chatbot-messages">
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="chatbot-row"
          :class="message.role === 'user' ? 'is-user' : 'is-bot'"
        >
          <div class="chatbot-avatar" aria-hidden="true">
            {{ message.role === 'user' ? '나' : '🏡' }}
          </div>
          <div class="chatbot-bubble">
            {{ message.content }}
          </div>
        </div>
        <div v-if="loading" class="chatbot-row is-bot">
          <div class="chatbot-avatar" aria-hidden="true">🏡</div>
          <div class="chatbot-bubble chatbot-bubble--loading">조건을 살펴보는 중이에요...</div>
        </div>
      </div>

      <form class="chatbot-form" @submit.prevent="submit">
        <input
          v-model="input"
          class="chatbot-input"
          type="text"
          placeholder="예: 서울시청 5km 안, 매매 6억 이하 아파트를 찾고 싶어요"
          :disabled="loading"
        />
        <button class="chatbot-send" type="submit" :disabled="loading || !input.trim()">
          보내기
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.chatbot-page {
  min-height: calc(100vh - 160px);
  padding: 48px 20px;
  background:
    linear-gradient(180deg, rgba(229, 246, 255, 0.9), rgba(249, 250, 251, 0.95) 42%),
    #f6f7fb;
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
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.1);
}

.chatbot-header {
  padding: 24px 26px 20px;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
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
  font-size: 23px;
  font-weight: 800;
}

.chatbot-subtitle {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.chatbot-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f8fafc;
}

.chatbot-row {
  display: flex;
  align-items: flex-end;
  gap: 9px;
  margin-bottom: 14px;
}

.chatbot-row.is-user {
  justify-content: flex-end;
}

.chatbot-row.is-bot {
  justify-content: flex-start;
}

.chatbot-avatar {
  display: grid;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 50%;
  color: #2563eb;
  background: #e0f2fe;
  font-size: 13px;
  font-weight: 800;
}

.is-user .chatbot-avatar {
  order: 2;
  color: #ffffff;
  background: #2563eb;
}

.chatbot-bubble {
  max-width: min(72%, 620px);
  padding: 13px 15px;
  border-radius: 8px;
  white-space: pre-wrap;
  line-height: 1.68;
  font-size: 15px;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.is-user .chatbot-bubble {
  color: #ffffff;
  background: #2563eb;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.2);
}

.is-bot .chatbot-bubble {
  color: #1f2937;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.05);
}

.chatbot-bubble--loading {
  color: #6b7280;
  font-style: italic;
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
  width: 92px;
  border: 0;
  border-radius: 8px;
  color: #ffffff;
  background: #2563eb;
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

  .chatbot-avatar {
    width: 30px;
    height: 30px;
    flex-basis: 30px;
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
