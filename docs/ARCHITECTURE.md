# ArisuCast 아키텍처 문서

## 개요

ArisuCast는 **MVVM + Clean Architecture** 패턴과 **멀티 모듈** 구성을 따릅니다.
데이터는 단방향으로 흐르며, Room이 단일 진실 공급원(Single Source of Truth)입니다.

```
Network / RSS   →   Room DB   →   StateFlow   →   UI (Compose)
```

---

## 계층 구조

### 데이터 흐름

```
RSS / iTunes API
      │
      ▼
  core-network (RssParser, ItunesSearchApi)
      │ ParsedPodcast / ParsedEpisode
      ▼
  UseCase (feature-subscriptions)
      │ PodcastEntity / EpisodeEntity
      ▼
  core-database (Room DAOs)
      │ Flow<List<Entity>>
      ▼
  ViewModel (StateFlow<UiState>)
      │ collectAsStateWithLifecycle()
      ▼
  Composable Screen
```

### 레이어 책임

| 레이어 | 위치 | 책임 |
|--------|------|------|
| **UI** | feature-* `Screen.kt` | Compose UI, 사용자 이벤트 위임 |
| **ViewModel** | feature-* `ViewModel.kt` | UiState 변환, 비즈니스 로직 오케스트레이션 |
| **UseCase** | feature-* `domain/` | 단일 비즈니스 작업 캡슐화 |
| **Repository** | core-media `PlaybackRepository` | 재생 상태 관리 |
| **Data Source** | core-database, core-network, core-datastore | 네트워크/DB/설정 접근 |

---

## 모듈 상세

### Core 모듈

#### `core-common`
공유 도메인 모델과 유틸리티. 다른 모든 모듈이 의존합니다.

```kotlin
// 도메인 모델
data class Podcast(val id: String, val title: String, val feedUrl: String, ...)
data class Episode(val id: String, val podcastId: String, val audioUrl: String, ...)
data class PlaybackState(val currentEpisode: Episode?, val isPlaying: Boolean,
                         val positionMs: Long, val sleepTimerEndMs: Long, ...)

// 결과 타입
sealed class Result<T> { class Success<T>(val data: T), class Error(...), object Loading }

// 확장 함수
fun String.sha256(): String  // 안정적인 ID 생성에 사용
```

**핵심 설계**: `Episode.id`는 RSS GUID의 `sha256()` 해시 → 피드 새로고침 시 ID가 변하지 않음

#### `core-database`
Room 데이터베이스. 앱의 단일 진실 공급원.

```
ArisuCastDatabase
├── PodcastEntity + PodcastDao   (upsert, getById, observeAll)
├── EpisodeEntity + EpisodeDao   (insertAll[IGNORE], observeByPodcast, updatePlaybackPosition)
└── SubscriptionEntity + SubscriptionDao (insert, isSubscribed, observeAll)
```

**`insertAll(OnConflictStrategy.IGNORE)`**: 피드 새로고침 시 기존 에피소드의 재생 위치(`playbackPositionMs`)를 보존합니다.

#### `core-network`
RSS 파싱과 iTunes Search API.

```kotlin
// RSS 파싱 (Rome + iTunes 확장 모듈)
class RssParser {
    suspend fun parseFeed(url: String): Result<ParsedPodcast>
    // iTunes namespace: 아트워크, 재생 시간, 에피소드 번호 파싱
}

// 검색 API (인증 불필요)
interface ItunesSearchApi {
    @GET("search")
    suspend fun searchPodcasts(@Query("term") query: String): ItunesSearchResponse
}
// Base URL: https://itunes.apple.com/
```

#### `core-media`
오디오 재생 전체를 관리.

```kotlin
@Singleton
class PlaybackRepository(player: ExoPlayer, episodeDao: EpisodeDao) {
    val state: StateFlow<PlaybackState>

    fun playEpisode(episodeId, audioUrl, title, artworkUrl, startPositionMs)
    fun playPause()
    fun seekToFraction(fraction: Float)
    fun skipBack(ms: Long = 10_000L)
    fun skipForward(ms: Long = 30_000L)
    fun setPlaybackSpeed(speed: Float)
    fun setSleepTimer(durationMs: Long)  // 0 = 취소
}
```

- 재생 중 1초마다 Room에 위치 저장 (`EpisodeDao.updatePlaybackPosition`)
- `MediaSessionService` → 잠금 화면/알림 컨트롤 지원

#### `core-download`
WorkManager 기반 에피소드 다운로드.

```kotlin
class DownloadManager {
    fun downloadEpisode(episodeId, audioUrl, episodeTitle, wifiOnly: Boolean = true)
    fun cancelDownload(episodeId: String)
    fun getDownloadProgress(episodeId: String): Flow<Int>
}

@HiltWorker
class EpisodeDownloadWorker : CoroutineWorker {
    // OkHttp 스트리밍 다운로드, 진행률 setProgress(), Room 완료 업데이트
}
```

#### `core-datastore`
DataStore Preferences로 사용자 설정 저장.

```kotlin
data class UserPreferences(
    val wifiOnlyDownload: Boolean = true,
    val autoDownload: Boolean = false,
    val defaultPlaybackSpeed: Float = 1.0f,
    val darkTheme: Boolean = false
)
```

#### `core-ui`
공용 Compose 컴포넌트와 Material3 테마.

| 컴포넌트 | 설명 |
|---------|------|
| `ArisuCastTheme` | 동적 색상(Android 12+), 다크/라이트 지원 |
| `PodcastCard` | 아트워크 + 제목 + 저자 카드 |
| `EpisodeItem` | 에피소드 목록 행, 현재 재생 중 강조 |
| `AnimatedMiniPlayer` | 하단 슬라이드 인 미니 플레이어 |
| `LoadingIndicator` / `ErrorMessage` | 공통 상태 UI |

---

### Feature 모듈

#### `feature-subscriptions` (라이브러리)

```kotlin
class SubscribeToFeedUseCase {
    // 1. URL trim + sha256() ID 생성
    // 2. 이미 구독 중이면 기존 데이터 반환
    // 3. RssParser.parseFeed()
    // 4. PodcastEntity + SubscriptionEntity + EpisodeEntity 저장
}

class RefreshFeedUseCase {
    // 기존 구독 피드 재파싱, EpisodeDao.insertAll(IGNORE)로 재생 위치 보존
}
```

딥 링크 `arisucast://subscribe?url=...` 수신 시 자동으로 `subscribeToFeed()` 호출.

#### `feature-search` (검색)

```kotlin
class SearchViewModel {
    // 400ms 디바운스 후 iTunes Search API 호출
    fun onQueryChange(query: String)  // 디바운스 트리거
    fun search(query: String)         // 즉시 검색

    // 검색 결과에서 바로 구독 가능 (RssParser + DAOs 직접 사용)
    fun subscribe(podcast: Podcast)
    val subscribingIds: StateFlow<Set<String>>  // 구독 중인 항목 추적
}
```

#### `feature-episodes` (에피소드 목록)

- `PodcastDao` + `EpisodeDao` + `PlaybackRepository` Flow를 `combine`으로 합산
- 다운로드된 에피소드 재생 시 로컬 파일 경로 우선 사용
- 다운로드/취소 버튼, 현재 재생 중인 에피소드 강조

#### `feature-player` (플레이어)

```kotlin
// 슬립 타이머 UI
// PlayerScreen: Timer 아이콘 → DropdownMenu (15분/30분/45분/1시간/취소)
fun setSleepTimer(minutes: Int)  // 0 = 취소

// 재생 속도 선택
// 0.5x / 1.0x / 1.5x / 2.0x 버튼
```

#### `feature-settings` (설정)

- DataStore에서 `Flow<UserPreferences>` 수집 → `StateFlow<SettingsUiState>`로 변환
- 다크 모드 토글 → `MainViewModel.darkTheme` → `ArisuCastTheme(darkTheme=...)` 즉시 적용

---

## App 모듈

### Navigation

```kotlin
// 4개 하단 탭
홈(home) ↔ 검색(search) ↔ 라이브러리(library) ↔ 설정(settings)

// 추가 화면
episodes/{podcastId}   // 에피소드 목록
player                 // 전체 플레이어

// 딥 링크
arisucast://subscribe?url={feedUrl}  →  library 화면
```

### 백그라운드 작업

```kotlin
// 앱 시작 시 등록 (ArisuCastApplication)
WorkManager.enqueueUniquePeriodicWork(
    "refresh_feeds",
    ExistingPeriodicWorkPolicy.KEEP,
    PeriodicWorkRequest(RefreshFeedsWorker, repeatInterval = 6.hours,
                        constraints = Constraints(CONNECTED))
)
```

`RefreshFeedsWorker`는 `app` 모듈에 위치 (core-network에 두면 core-database 의존성으로 순환 발생).

---

## 주요 설계 결정

### 1. Episode ID 안정성
```kotlin
// RSS GUID의 sha256() 해시를 ID로 사용
val id = episode.guid.sha256()
// → 피드 새로고침 시 동일한 에피소드가 동일한 ID를 가짐
// → Room의 IGNORE 전략과 함께 재생 위치가 보존됨
```

### 2. 단방향 데이터 흐름
```
Network → Room → Flow → StateFlow → Composable
```
- UI는 절대 네트워크에서 직접 읽지 않음
- 모든 변경은 Room을 통해 반영됨

### 3. 검색 API 선택
- Podcast Index API → API Key 인증 필요, 복잡한 HMAC 서명
- **iTunes Search API 채택** → 인증 불필요, 신뢰할 수 있는 데이터, 간단한 구현

### 4. 모듈 의존성 규칙
```
feature-* → core-* (가능)
feature-* → feature-* (금지 — 순환 방지)
core-* → core-* (제한적으로만)
core-network → core-database (금지 — 단방향 유지)
```

### 5. rome JAR 충돌 해결
```kotlin
// app/build.gradle.kts
packaging {
    resources {
        pickFirsts += listOf("rome-utils-*.jar", "**/*.jar")
    }
}
// rome와 rome-modules 모두 rome-utils JAR을 포함 → 첫 번째 것만 사용
```

---

## 테스트 전략

```
core-common/test/
└── StringExtensionsTest         sha256() 해시 정확성 검증

core-media/test/
└── PlaybackRepositoryTest       슬립 타이머, skipBack/Forward, 재생 제어

feature-subscriptions/test/
└── SubscribeToFeedUseCaseTest   성공/이미구독/오류/URL트리밍

feature-search/test/
└── SearchViewModelTest          디바운스, 상태 전환, feedUrl 필터링
```

**도구**: MockK (모킹), Turbine (Flow 테스트), kotlinx-coroutines-test (`runTest`, `StandardTestDispatcher`)

**Media3 관련**: `ExoPlayer`는 `mockk(relaxed = true)`로 모킹, `Dispatchers.setMain(UnconfinedTestDispatcher())`로 Main 디스패처 설정.
