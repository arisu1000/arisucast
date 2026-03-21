# ArisuCast

RSS 피드 기반 Android 팟캐스트 앱.
팟캐스트를 구독하고, 백그라운드에서 재생하며, 오프라인으로 다운로드할 수 있습니다.

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **팟캐스트 구독** | RSS/Atom 피드 URL로 구독, iTunes 확장 메타데이터 파싱 |
| **검색** | iTunes Search API를 통한 팟캐스트 검색 및 즉시 구독 |
| **오디오 재생** | 백그라운드 재생, 재생 속도 조절 (0.5x ~ 2.0x), 슬립 타이머 |
| **오프라인 다운로드** | Wi-Fi 전용 또는 모바일 데이터 선택, 진행 상황 추적 |
| **자동 피드 갱신** | 6시간마다 백그라운드에서 구독 피드 자동 업데이트 |
| **딥 링크** | `arisucast://subscribe?url=<feed_url>` 로 외부에서 구독 실행 |
| **다크 모드** | 설정에서 토글 가능, 동적 색상(Android 12+) 지원 |

---

## 스크린샷 (화면 구성)

```
홈           검색          라이브러리       설정
[최근 에피소드]  [검색창]      [구독 팟캐스트]   [Wi-Fi 다운로드]
[팟캐스트 목록]  [결과 + 구독]  [+ FAB]         [자동 다운로드]
                                             [재생 속도]
                                             [다크 모드]
```

**플레이어 화면**: 아트워크, 제목, 진행 슬라이더, 재생 컨트롤, 속도 선택, 슬립 타이머

**미니 플레이어**: 다른 화면에서도 하단에 표시되는 축소 플레이어

---

## 시스템 요구사항

- **Android**: API 26 (Android 8.0 Oreo) 이상
- **개발**: Android Studio Hedgehog 이상, JDK 17

---

## 빌드 방법

```bash
# Android Studio의 JDK 사용 (시스템 Java 버전 충돌 방지)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# 디버그 APK 빌드
./gradlew assembleDebug

# 단위 테스트 실행
./gradlew test

# APK 위치
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 아키텍처

MVVM + Clean Architecture, 멀티 모듈 구성.

```
┌─────────────────────────────────────────────────────────┐
│                        :app                              │
│   MainActivity · AppNavHost · MainViewModel              │
│   RefreshFeedsWorker (6h 주기)                           │
└──────────────┬──────────────────────────────────────────┘
               │ depends on
┌──────────────▼──────────────────────────────────────────┐
│                   Feature Modules                        │
│  feature-home  feature-search  feature-subscriptions     │
│  feature-episodes  feature-player  feature-settings      │
└──────────────┬──────────────────────────────────────────┘
               │ depends on
┌──────────────▼──────────────────────────────────────────┐
│                    Core Modules                          │
│  core-common   core-database   core-network              │
│  core-media    core-download   core-datastore  core-ui   │
└─────────────────────────────────────────────────────────┘
```

상세 아키텍처는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)를 참조하세요.

---

## 모듈 구조

```
arisucast/
├── app/                        # 앱 진입점, Navigation, DI 조합
├── core/
│   ├── core-common/            # 도메인 모델, Result<T>, DispatcherProvider
│   ├── core-database/          # Room DB, DAOs, Entities, Mappers
│   ├── core-datastore/         # DataStore (사용자 설정)
│   ├── core-download/          # WorkManager 다운로드
│   ├── core-media/             # ExoPlayer, PlaybackRepository, MediaSessionService
│   ├── core-network/           # RssParser, iTunes Search API, Retrofit
│   └── core-ui/                # 공통 Compose 컴포넌트, 테마
└── feature/
    ├── feature-home/           # 홈 화면 (최근 에피소드)
    ├── feature-episodes/       # 에피소드 목록
    ├── feature-player/         # 전체 플레이어 화면
    ├── feature-search/         # 검색 + 구독
    ├── feature-subscriptions/  # 라이브러리 (구독 관리)
    └── feature-settings/       # 설정 화면
```

---

## 딥 링크

외부 앱이나 웹에서 팟캐스트를 자동 구독:

```
arisucast://subscribe?url=https://example.com/feed.rss
```

Android 터미널에서 테스트:
```bash
adb shell am start \
  -a android.intent.action.VIEW \
  -d "arisucast://subscribe?url=https://feeds.simplecast.com/54nAGcIl"
```

---

## 기술 스택

| 분류 | 라이브러리 | 버전 |
|------|-----------|------|
| UI | Jetpack Compose + Material3 | BOM 2025.02.00 |
| 아키텍처 | Hilt (DI), ViewModel, StateFlow | Hilt 2.54 |
| 데이터베이스 | Room | 2.7.0 |
| 미디어 | Media3 ExoPlayer | 1.5.1 |
| 네트워크 | OkHttp + Retrofit | 4.12.0 / 2.11.0 |
| RSS 파싱 | Rome + rome-modules | 2.1.0 |
| 이미지 | Coil | 3.0.4 |
| 백그라운드 | WorkManager | 2.10.0 |
| 설정 | DataStore Preferences | 1.1.2 |
| 직렬화 | kotlinx.serialization | 1.8.0 |
| 테스트 | MockK + Turbine + kotlinx-coroutines-test | — |

---

## 라이선스

This project is for personal/educational use.
