// script_v2.js - [Step 2] 문서 업로드 기능 추가

// ============================================
// 채팅 기능 (script_v1.js와 동일)
// ============================================

function sendMessage() {
  const inputElement = document.getElementById("user-input");
  const userMessage = inputElement.value;

  if (userMessage === "") return;

  addMessage("user", userMessage);
  inputElement.value = "";

  fetch("http://localhost:8000/chat", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ message: userMessage }),
  })
    .then(function (response) {
      return response.json();
    })
    .then(function (data) {
      addMessage("bot", data.answer);
    })
    .catch(function (error) {
      addMessage("bot", "서버 연결에 실패했습니다.");
    });
}

function addMessage(type, text) {
  const chatWindow = document.getElementById("chat-window");
  const messageDiv = document.createElement("div");
  messageDiv.className = "message " + type;
  messageDiv.innerText = text;
  chatWindow.appendChild(messageDiv);
  chatWindow.scrollTop = chatWindow.scrollHeight;
}

document
  .getElementById("user-input")
  .addEventListener("keypress", function (event) {
    if (event.key === "Enter") {
      sendMessage();
    }
  });

// ============================================
// [추가] 업로드 패널 열기/닫기
// ============================================

function toggleUpload() {
  const panel = document.getElementById("upload-panel");
  // hidden 클래스가 있으면 제거, 없으면 추가
  panel.classList.toggle("hidden");
}

// TODO: 03. 파일 업로드 기능을 AJAX로 구현해보자.
window.addEventListener("load", () => {
  const uploadButton = document.getElementById("upload-btn");
  if (!uploadButton) return;

  uploadButton.addEventListener("click", async (e) => {
    e.preventDefault();
    e.stopPropagation();

    const fileInfo = document.getElementById("file-input");
    const file = fileInfo.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await fetch("http://localhost:8000/upload", {
        method: "POST",
        body: formData,
        redirect: "manual",
        headers: {
          "x-requested-with": "XMLHttpRequest",
          Accept: "application/json",
        },
      });

      if (
        response.type === "opaqueredirect" ||
        (response.status >= 300 && response.status < 400)
      ) {
        throw new Error("서버가 업로드 요청을 리디렉트했습니다.");
      }

      const data = await response.json();
      console.log(data);
      addMessage("bot", data.message || "업로드가 완료되었습니다.");
      fileInfo.value = "";
    } catch (error) {
      addMessage("bot", "파일 업로드에 실패했습니다. " + error.message);
    }
  });
});
// END
