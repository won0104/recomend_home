let chatHistory = [];

function renderMessage(type, text, source) {
  const chatWindow = document.getElementById("chat-window");
  const messageDiv = document.createElement("div");
  messageDiv.className = "message " + type;
  messageDiv.innerText = text;

  if (type === "bot" && source) {
    const sourceDiv = document.createElement("div");
    sourceDiv.className = "source-tag";
    sourceDiv.innerText = "출처: " + source;
    messageDiv.appendChild(sourceDiv);
  }

  chatWindow.appendChild(messageDiv);
  chatWindow.scrollTop = chatWindow.scrollHeight;
}

function addMessage(type, text, source) {
  renderMessage(type, text, source);
  chatHistory.push({ type, text, source: source || null });
}

function toggleUpload() {
  const panel = document.getElementById("upload-panel");
  if (!panel) return;
  panel.classList.toggle("hidden");
}

async function sendMessage() {
  const inputElement = document.getElementById("user-input");
  const userMessage = inputElement.value.trim();

  if (!userMessage) return;

  addMessage("user", userMessage);
  inputElement.value = "";

  try {
    const response = await fetch("http://localhost:8000/integrated-chat", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ message: userMessage }),
    });

    const data = await response.json();
    addMessage("bot", data.answer || "응답을 받지 못했습니다.", data.source);
  } catch (error) {
    addMessage("bot", "서버 연결에 실패했습니다.");
  }
}

async function uploadDocument() {
  const fileInput = document.getElementById("file-input");
  const file = fileInput?.files?.[0];

  if (!file) {
    addMessage("bot", "업로드할 파일을 먼저 선택해주세요.");
    return;
  }

  const formData = new FormData();
  formData.append("file", file);

  addMessage("bot", "파일을 업로드하고 있습니다.");

  try {
    const response = await fetch("http://localhost:8000/upload", {
      method: "POST",
      body: formData,
      headers: {
        Accept: "application/json",
        "x-requested-with": "XMLHttpRequest",
      },
    });

    const data = await response.json();
    addMessage("bot", data.message || "업로드가 완료되었습니다.");

    if (data.success) {
      fileInput.value = "";
    }
  } catch (error) {
    addMessage("bot", "파일 업로드에 실패했습니다.");
  }
}

window.addEventListener("DOMContentLoaded", () => {
  const sendButton = document.getElementById("send-btn");
  const uploadToggleButton = document.getElementById("toggle-upload-btn");
  const uploadButton = document.getElementById("upload-btn");
  const inputElement = document.getElementById("user-input");

  sendButton?.addEventListener("click", sendMessage);
  uploadToggleButton?.addEventListener("click", toggleUpload);
  uploadButton?.addEventListener("click", uploadDocument);

  inputElement?.addEventListener("keydown", (event) => {
    if (event.key !== "Enter" || event.isComposing) return;
    event.preventDefault();
    sendMessage();
  });
});
