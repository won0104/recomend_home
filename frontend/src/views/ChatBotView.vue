<script setup>
import { nextTick, ref } from 'vue'
import { resetChatSession, sendChatMessage } from '../api/chat'

const CHAT_SESSION_KEY = 'ssafy-home-chat-session-id'
const INITIAL_MESSAGE = {
  role: 'bot',
  content: '안녕하세요 😊\n원하는 동네를 함께 찾아드릴게요.\n\n📍 먼저 기준 위치를 알려주세요.\n예: 서울시청 근처, 강남역 주변, 삼성역에서 5km 안'
}

const input = ref('')
const loading = ref(false)
const resetting = ref(false)
const messages = ref([{ ...INITIAL_MESSAGE }])
const listRef = ref(null)
const sessionId = ref(getOrCreateSessionId())

function getOrCreateSessionId() {
  const existing = localStorage.getItem(CHAT_SESSION_KEY)
  if (existing) return existing

  const generated = crypto?.randomUUID
    ? crypto.randomUUID()
    : `chat-${Date.now()}-${Math.random().toString(36).slice(2)}`
  localStorage.setItem(CHAT_SESSION_KEY, generated)
  return generated
}

function createSessionId() {
  return crypto?.randomUUID
    ? crypto.randomUUID()
    : `chat-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

async function startNewChat() {
  if (loading.value || resetting.value) return

  const previousSessionId = sessionId.value
  resetting.value = true

  try {
    if (previousSessionId) {
      await resetChatSession(previousSessionId)
    }
  } catch (error) {
    console.warn('Failed to reset chat session.', error)
  } finally {
    const nextSessionId = createSessionId()
    localStorage.setItem(CHAT_SESSION_KEY, nextSessionId)
    sessionId.value = nextSessionId
    input.value = ''
    messages.value = [{ ...INITIAL_MESSAGE }]
    resetting.value = false
    await scrollToBottom()
  }
}

async function submit() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const history = messages.value.slice(0, -1).map(({ role, content }) => ({ role, content }))
    const response = await sendChatMessage({ message: text, history, sessionId: sessionId.value })
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

function renderMessage(content) {
  const escaped = String(content ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

  const renderInline = (text) => text.replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>')
  const lines = escaped.split(/\r?\n/)
  const blocks = []
  let paragraph = []
  let listItems = []

  const flushParagraph = () => {
    if (paragraph.length === 0) return
    blocks.push(`<p>${renderInline(paragraph.join(' '))}</p>`)
    paragraph = []
  }

  const flushList = () => {
    if (listItems.length === 0) return
    blocks.push(`<ul>${listItems.map((item) => `<li>${renderInline(item)}</li>`).join('')}</ul>`)
    listItems = []
  }

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) {
      flushParagraph()
      flushList()
      continue
    }

    const heading = line.match(/^#{2,4}\s+(.+)$/)
    if (heading) {
      flushParagraph()
      flushList()
      blocks.push(`<h3>${renderInline(heading[1])}</h3>`)
      continue
    }

    const bullet = line.match(/^[-*]\s+(.+)$/)
    if (bullet) {
      flushParagraph()
      listItems.push(bullet[1])
      continue
    }

    flushList()
    paragraph.push(line)
  }

  flushParagraph()
  flushList()
  return blocks.join('')
}
</script>

<template>
  <main class="chatbot-page">
    <section class="chatbot-shell" aria-label="맞춤 동네 추천 챗봇">
      <header class="chatbot-header">
        <div class="chatbot-header-top">
          <p class="chatbot-eyebrow">AI 동네 추천</p>
          <button class="chatbot-reset" type="button" :disabled="loading || resetting" @click="startNewChat">
            새 대화
          </button>
        </div>
        <h1>편하게 말하면 맞춤 동네를 찾아드릴게요 🏡</h1>
        <p class="chatbot-subtitle">이전 조건을 이어서 사용 중입니다. 처음부터 다시 시작하려면 새 대화를 눌러주세요.</p>
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
          <div class="chatbot-bubble" v-html="renderMessage(message.content)"></div>
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
  //background:
  //  linear-gradient(180deg, rgba(236, 253, 243, 0.92), rgba(249, 250, 251, 0.96) 42%),
  //  #f7faf8;
}

.chatbot-shell {
  display: flex;
  flex-direction: column;
  width: min(920px, 100%);
  height: 680px;
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid #d6ebe0;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 45px rgba(20, 83, 45, 0.1);
}

.chatbot-header {
  padding: 24px 26px 20px;
  border-bottom: 1px solid #d6ebe0;
  background: linear-gradient(135deg, #ffffff 0%, #ecfdf3 100%);
}

.chatbot-eyebrow {
  margin: 0;
  color: #079455;
  font-size: 15px;
  font-weight: 700;
}

.chatbot-header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.chatbot-reset {
  flex: 0 0 auto;
  height: 34px;
  padding: 0 12px;
  border: 1px solid #b7ddca;
  border-radius: 8px;
  color: #067647;
  background: #ffffff;
  font-size: 13px;
  font-weight: 700;
  transition:
    border-color 0.2s ease,
    background 0.2s ease;
}

.chatbot-reset:hover:not(:disabled) {
  border-color: #079455;
  background: #ecfdf3;
}

.chatbot-reset:disabled {
  cursor: not-allowed;
  opacity: 0.45;
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
  background: #f6fbf8;
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
  color: #078a4c;
  background: #e4f7ed;
  font-size: 13px;
  font-weight: 800;
}

.is-user .chatbot-avatar {
  order: 2;
  color: #ffffff;
  background: #079455;
}

.chatbot-bubble {
  max-width: min(72%, 620px);
  padding: 15px 16px;
  border-radius: 8px;
  line-height: 1.72;
  font-size: 15px;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.chatbot-bubble :deep(p) {
  margin: 0 0 12px;
}

.chatbot-bubble :deep(p:last-child) {
  margin-bottom: 0;
}

.chatbot-bubble :deep(h3) {
  margin: 4px 0 9px;
  color: #0f172a;
  font-size: 17px;
  line-height: 1.45;
  font-weight: 850;
}

.chatbot-bubble :deep(ul) {
  display: grid;
  gap: 7px;
  margin: 0 0 14px;
  padding: 0;
  list-style: none;
}

.chatbot-bubble :deep(li) {
  position: relative;
  padding-left: 17px;
}

.chatbot-bubble :deep(li::before) {
  position: absolute;
  left: 0;
  top: 0.72em;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  content: '';
}

.chatbot-bubble :deep(strong) {
  color: #111827;
  font-weight: 800;
}

.is-user .chatbot-bubble :deep(strong) {
  color: #ffffff;
}

.is-user .chatbot-bubble :deep(h3),
.is-user .chatbot-bubble :deep(p) {
  color: #ffffff;
}

.is-user .chatbot-bubble :deep(li::before) {
  background: #ffffff;
}

.is-user .chatbot-bubble {
  color: #ffffff;
  background: #079455;
  box-shadow: 0 8px 18px rgba(7, 148, 85, 0.2);
}

.is-bot .chatbot-bubble {
  color: #1f2937;
  background: #ffffff;
  border: 1px solid #d6ebe0;
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
  border-top: 1px solid #d6ebe0;
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
  border-color: #079455;
  outline: none;
  box-shadow: 0 0 0 3px rgba(7, 148, 85, 0.14);
}

.chatbot-send {
  width: 92px;
  border: 0;
  border-radius: 8px;
  color: #ffffff;
  background: #079455;
  font-weight: 700;
  transition: background 0.2s ease;
}

.chatbot-send:hover:not(:disabled) {
  background: #067647;
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
