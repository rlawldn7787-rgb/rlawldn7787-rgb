# 우행통신 보드판

건설·통신 현장용 사진 보드판 시스템입니다. Android 앱에서 촬영·기록 후 Railway 서버에 저장하고, PC 웹과 앱에서 조회·엑셀 다운로드할 수 있습니다.

## 구성

| 경로 | 설명 |
|------|------|
| `apps/api` | Express + PostgreSQL API (사진 업로드, JWT, 엑셀) |
| `apps/web` | Next.js PC 웹 (조회·엑셀·계정관리) |
| `apps/android` | Kotlin Compose Android 앱 |

## 로컬 실행

### 1) PostgreSQL

Docker가 있으면:

```bash
docker compose up -d
```

없으면 로컬 Postgres에 DB `woohaeng_board` 를 만들고 `apps/api/.env` 의 `DATABASE_URL` 을 맞춥니다.

### 2) API

```bash
cd apps/api
cp .env.example .env   # 이미 있으면 있으면 생략
npm install
npm run dev
```

기본 관리자: `admin` / `admin1234`  
API: http://localhost:4000

사진 저장은 R2 환경변수가 없으면 `apps/api/uploads` 로컬 폴더를 사용합니다.

### 3) 웹

```bash
cd apps/web
cp .env.local.example .env.local
npm install
npm run dev
```

웹: http://localhost:3000

### 4) Android

1. Android Studio에서 `apps/android` 폴더 열기
2. Gradle Sync 후 에뮬레이터/실기기 실행
3. 에뮬레이터 API 주소는 `http://10.0.2.2:4000` (이미 설정됨)
4. 실기기는 같은 Wi-Fi의 PC IP로 `app/build.gradle.kts` 의 `API_BASE_URL` 수정
5. release 빌드 시 Railway API URL로 교체

## Railway 배포

### API 서비스

1. New Project → 이 레포 연결
2. **Root Directory**: `apps/api`
3. Add Plugin → **PostgreSQL**
4. Variables:

```
DATABASE_URL=${{Postgres.DATABASE_URL}}
DATABASE_SSL=true
JWT_SECRET=<강한-비밀값>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<초기비번>
ADMIN_NAME=관리자
CORS_ORIGIN=https://<web-도메인>
PUBLIC_API_URL=https://<api-도메인>
```

R2 사용 시 추가:

```
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET=
R2_PUBLIC_BASE_URL=
```

R2가 없으면 Railway 볼륨을 `/app/uploads` 에 마운트하거나, 배포 환경에서는 R2 사용을 권장합니다.

### 웹 서비스

1. 같은 프로젝트에 서비스 추가
2. **Root Directory**: `apps/web`
3. Variables:

```
NEXT_PUBLIC_API_URL=https://<api-도메인>
```

Dockerfile 빌드 시 `NEXT_PUBLIC_API_URL` 을 build arg로도 전달됩니다.

## API 요약

- `POST /auth/login` · `GET /auth/me`
- `GET/POST /records` · `GET /records/:id`
- `GET /records/export.xlsx`
- `GET/POST /admin/users` · `PATCH /admin/users/:id`

엑셀 컬럼: 일자 | 공사명 | 공종 | 위치 | 내용 | 작성자 | 업로드시각 | 사진URL

## 보드판 항목

공사명, 공종, 위치, 내용, 일자, 작성자(로그인 계정)
