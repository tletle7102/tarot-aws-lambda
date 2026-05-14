# 타로셸 (TaroShell)

AI 타로 마스터가 당신의 고민을 카드로 해석해주는 서버리스 웹 서비스
<img src="docs/taroshell_main.png" alt="taroshell_main" width="720">

![Java](https://img.shields.io/badge/Java-21-blue)
![AWS Lambda](https://img.shields.io/badge/AWS-Lambda-FF9900)
![API Gateway](https://img.shields.io/badge/AWS-API%20Gateway-FF4F8B)
![DynamoDB](https://img.shields.io/badge/AWS-DynamoDB-4053D6)
![Gemini](https://img.shields.io/badge/Google-Gemini%20API-4285F4)
![Cloudflare](https://img.shields.io/badge/Cloudflare-Pages-F38020)

[![Live](https://img.shields.io/badge/배포_사이트-타로셸-8B5CF6?style=for-the-badge)](https://tarot-aws-lambda.pages.dev)
[![YouTube](https://img.shields.io/badge/시연_영상-YouTube-FF0000?style=for-the-badge&logo=youtube)](https://youtu.be/b8Qfs8rT6xs)

---

## 🏗️ 시스템 아키텍처

```
┌─────────────────┐         ┌──────────────────┐         ┌────────────────┐
│  Cloudflare     │  HTTPS  │  API Gateway     │         │  Lambda        │
│  Pages (CDN)    │         │  (HTTP API)      │────────▶│  (Java 21)     │
│                 │         │                  │         │                │
│  - index.html   │         │  POST /reading   │         │  TarotHandler  │
│  - style.css    │         │  GET  /reading/  │         │  GeminiClient  │
│  - script.js    │         │       {id}       │         │  TarotDeck     │
│  - images/      │         │                  │         │                │
└────────┬────────┘         └──────────────────┘         └───┬────────┬───┘
         │                                                   │        │
         │  정적 파일 서빙                          Gemini API│        │DynamoDB
         ▼                                                   ▼        ▼
    사용자 브라우저                                ┌──────────┐  ┌──────────┐
                                                  │ Google   │  │ DynamoDB │
                                                  │ Gemini   │  │ (결과    │
                                                  │ 2.5      │  │  저장)   │
                                                  │ Flash    │  │          │
                                                  │ Lite     │  │          │
                                                  └──────────┘  └──────────┘
                                                        ▲
                                                        │
                                                  ┌──────────┐
                                                  │ SSM      │
                                                  │ Parameter│
                                                  │ Store    │
                                                  │ (API Key)│
                                                  └──────────┘
```

---

## ⚙️ 기술 스택

### Backend

| 기술 | 버전 |
|------|------|
| Java | 21 |
| AWS Lambda | - |
| API Gateway (HTTP API) | - |
| DynamoDB | - |
| SSM Parameter Store | - |
| Google Gemini API | 2.5 Flash Lite |
| Gradle | 8.10 |

### Frontend

| 기술 | 버전 |
|------|------|
| HTML / CSS / JavaScript | - |
| Cloudflare Pages | - |
| Web Share API | - |

---

## 🎯 주요 기능

- **AI 타로 리딩** — 사용자의 고민을 입력받아 Gemini API가 맞춤형 타로 해석 생성
- **부채꼴 카드 선택** — 22장 메이저 아르카나를 부채꼴로 펼치고, 사용자가 직접 3장 선택
- **카드 셔플** — Fisher-Yates 알고리즘으로 매번 랜덤한 카드 배치
- **과거/현재/미래 스프레드** — 3장 카드를 시간 축으로 배치하여 해석
- **정방향/역방향** — 서버에서 랜덤 결정, 같은 카드도 다른 의미 제공
- **결과 공유** — DynamoDB에 결과 저장 + 단축 링크 생성, 카카오톡/문자/메일 공유
- **신비로운 로딩 연출** — 카드 발광 애니메이션 + 순환 메시지로 대기 시간 연출
- **반응형 UI** — 모바일/데스크톱 대응

---

## 🔄 서비스 플로우

```
1. 사용자 접속 → Cloudflare Pages에서 정적 파일 로드
2. 고민 입력 → [카드 선택하기] 클릭
3. 22장 카드 랜덤 셔플 → 부채꼴 UI로 뒷면 표시 (프론트엔드 처리)
4. 3장 선택 → [리딩하기] 클릭
5. POST /reading { concern, cardIds } → API Gateway → Lambda
6. Lambda: 카드 정역방향 결정 → 프롬프트 생성 → Gemini API 호출
7. Gemini 응답 수신 → DynamoDB에 결과 저장 (shareId 발급)
8. 결과 JSON 반환 → 프론트엔드에서 카드 이미지 + 리딩 텍스트 렌더링
9. [공유하기] → 단축 링크 생성 → 네이티브 공유 or 클립보드 복사
```

---

## 🚀 실행 방법

### 사전 요구사항

- JDK 21 (`java --version`으로 확인)
- AWS CLI (`aws --version`으로 확인, `aws configure` 완료)
- Wrangler CLI (`npm install -g wrangler`)

### Backend 빌드 & 배포

```bash
git clone https://github.com/tletle7102/tarot-aws-lambda.git
cd tarot-aws-lambda/backend

# Shadow JAR 빌드
./gradlew shadowJar

# Lambda 함수 업데이트
aws lambda update-function-code \
  --function-name tarot-reading \
  --zip-file fileb://build/libs/tarot-lambda.jar \
  --region ap-northeast-2
```

### Frontend 배포

```bash
wrangler pages deploy frontend --project-name=tarot-aws-lambda
```

---

## 👤 개발자

| 역할 | GitHub |
|------|--------|
| Backend / Frontend / Infra | [@tletle7102](https://github.com/tletle7102) |
