# SSAFY RAG 챗봇 클라이언트

이 폴더는 메인 프론트엔드(`frontend/`) 안에서 iframe으로 불러오는 챗봇 전용 클라이언트입니다.
FastAPI 서버(`../../servers/main.py`)와 통신해 문서 업로드와 RAG 기반 질의를 처리합니다.

## 폴더 구성

```text
frontend/clients
├─ index.html
├─ style.css
├─ script.js
├─ script_v1.js
├─ script_v2.js
├─ script_v3.js
├─ script_v4.js
└─ sample_data/
```

## 실행 방법

1. 루트에서 챗봇 서버를 실행합니다.

```powershell
cd servers
python main.py
```

2. 루트에서 메인 프론트엔드 또는 이 폴더를 Live Server로 실행합니다.

- 메인 화면: `frontend/index.html`
- 챗봇 단독 확인: `frontend/clients/index.html`

## API 엔드포인트

- `POST /chat`
- `POST /search`
- `POST /integrated-chat`
- `POST /upload`
