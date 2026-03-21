# 개발 가이드

## 개발 환경 설정

### 필수 요건

- **Android Studio**: Hedgehog (2023.1.1) 이상
- **JDK**: Android Studio 내장 JBR (Java 21) 사용 권장
  - 시스템 JDK 17/21이 아닌 경우 빌드 실패 → JAVA_HOME 설정 필요

### 처음 시작하기

```bash
# 1. 저장소 클론
git clone https://github.com/yourname/arisucast.git
cd arisucast

# 2. Android Studio에서 열기
# File → Open → arisucast 폴더 선택

# 3. 또는 터미널에서 빌드 (JAVA_HOME 필수)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug

# 4. 에뮬레이터/기기에 설치
./gradlew installDebug
```

---

## 새 기능 추가 방법

### 새 Feature 화면 추가

1. `feature/feature-xxx/` 모듈 생성
2. `settings.gradle.kts`에 `include(":feature:feature-xxx")` 추가
3. `build.gradle.kts` 작성 (기존 feature 모듈 복사 후 namespace 수정)
4. `consumer-rules.pro` 빈 파일 생성
5. `app/build.gradle.kts`에 `implementation(project(":feature:feature-xxx"))` 추가
6. `AppDestination.kt` 또는 `AppRoutes`에 라우트 추가
7. `AppNavHost.kt`에 `composable()` 추가

### 새 Core 모듈 추가

위와 동일하되, Hilt를 사용하는 경우:
```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)         // 추가
    alias(libs.plugins.ksp)          // 추가
}
dependencies {
    implementation(libs.hilt.android)  // 추가
    ksp(libs.hilt.compiler)            // 추가
}
```

---

## 팟캐스트 구독 흐름

```
사용자 입력 (RSS URL)
    │
    ▼
SubscriptionsViewModel.subscribeToFeed(url)
    │
    ▼
SubscribeToFeedUseCase(url)
    ├─ url.trim().sha256() → podcastId
    ├─ subscriptionDao.isSubscribed(podcastId) → 이미 구독 시 기존 데이터 반환
    ├─ rssParser.parseFeed(url) → ParsedPodcast
    ├─ podcastDao.upsert(PodcastEntity)
    ├─ subscriptionDao.insert(SubscriptionEntity)
    └─ episodeDao.insertAll(episodes, IGNORE)
            │
            ▼
    Room Flow → SubscriptionsViewModel → SubscriptionsScreen 자동 업데이트
```

---

## 오디오 재생 흐름

```
EpisodeListScreen → 에피소드 탭
    │
    ▼
EpisodeListViewModel.playEpisode(episode)
    │ 로컬 파일 있으면 localFilePath 우선, 없으면 audioUrl
    ▼
PlaybackRepository.playEpisode(episodeId, url, title, artwork, startPosition)
    │ EpisodeDao에서 저장된 재생 위치 로드
    ▼
ExoPlayer.setMediaItem() → prepare() → seekTo() → play()
    │
    ▼
PlaybackState 업데이트 (StateFlow)
    ├─ MainViewModel.playbackState → AnimatedMiniPlayer
    └─ PlayerViewModel.uiState → PlayerScreen
```

---

## 검색 및 구독 흐름 (검색 화면)

```
사용자 타이핑
    │
    ▼
SearchViewModel.onQueryChange(text)
    ├─ 쿼리 비어있으면 → Idle 상태
    └─ 400ms 디바운스 후 doSearch(query)
            │
            ▼
        ItunesSearchApi.searchPodcasts(query)
            │
            ▼
        ItunesSearchResponse → feedUrl.sha256()로 Podcast 도메인 모델 변환
        (feedUrl 비어있는 결과 필터링)
            │
            ▼
        SearchUiState.Success(results)
            │
            ▼
        SearchScreen: 각 결과에 [구독] 버튼 표시

사용자 [구독] 탭
    │
    ▼
SearchViewModel.subscribe(podcast)
    ├─ subscribingIds에 podcast.id 추가 (로딩 표시)
    ├─ rssParser.parseFeed(podcast.feedUrl) → 전체 메타데이터 가져옴
    ├─ Room에 PodcastEntity + SubscriptionEntity + EpisodeEntity 저장
    ├─ 결과 목록에서 해당 팟캐스트를 isSubscribed=true로 업데이트
    └─ subscribingIds에서 podcast.id 제거
```

---

## 딥 링크 처리

### 지원하는 URI 패턴

```
arisucast://subscribe?url=<인코딩된_피드_URL>
```

### 처리 흐름

```
외부 앱/링크 → AndroidManifest intent-filter 수신
    │
    ▼
Navigation Compose 딥링크 처리 (AppNavHost)
    │ navDeepLink { uriPattern = "arisucast://subscribe?url={feedUrl}" }
    ▼
SubscriptionsScreen(deepLinkFeedUrl = feedUrl)
    │
    ▼
LaunchedEffect(deepLinkFeedUrl) { viewModel.subscribeToFeed(feedUrl) }
```

### ADB로 테스트

```bash
adb shell am start \
  -a android.intent.action.VIEW \
  -d "arisucast://subscribe?url=https%3A%2F%2Ffeeds.simplecast.com%2F54nAGcIl" \
  com.arisucast.app
```

---

## 슬립 타이머

```kotlin
// 설정 (PlayerScreen → ViewModel → Repository)
viewModel.setSleepTimer(minutes = 30)  // 30분 후 일시정지
viewModel.setSleepTimer(minutes = 0)   // 취소

// 내부 동작 (PlaybackRepository)
fun setSleepTimer(durationMs: Long) {
    sleepTimerJob?.cancel()
    _state.update { it.copy(sleepTimerEndMs = if (durationMs > 0) now + durationMs else 0L) }
    sleepTimerJob = scope.launch {
        delay(durationMs)
        player.pause()
        _state.update { it.copy(sleepTimerEndMs = 0L) }
    }
}
```

---

## 테스트 작성 가이드

### ViewModel 테스트 기본 구조

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `상태 전환 테스트`() = runTest {
        // Turbine으로 Flow 테스트
        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            viewModel.doSomething()
            assertEquals(UiState.Loading, awaitItem())
            assertTrue(awaitItem() is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Media3/ExoPlayer 모킹

```kotlin
val player: ExoPlayer = mockk(relaxed = true)
// relaxed = true: 모든 함수 호출 허용, 검증이 필요한 것만 verify로 확인

// PlaybackRepository 사용 시 반드시 Main 디스패처 설정
@Before fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    repository = PlaybackRepository(player, episodeDao)
}
```

### Room DAO 모킹

```kotlin
val episodeDao: EpisodeDao = mockk(relaxed = true)
// 필요한 것만 명시적으로 지정
coEvery { episodeDao.observeByPodcast(any()) } returns flowOf(emptyList())
```

---

## 알려진 제약사항

| 항목 | 현재 상태 | 향후 개선 방향 |
|------|----------|-------------|
| 에피소드 대기열 | 미구현 | 재생 순서 큐 추가 |
| 챕터 지원 | 미구현 | MP3 Chapter 메타데이터 파싱 |
| OPML 가져오기/내보내기 | 미구현 | 구독 목록 백업/복원 |
| 위젯 | 미구현 | Glance 위젯으로 미니 플레이어 |
| 검색 — 피드 기록 | 미구현 | 최근 검색어 DataStore 저장 |
| 알림 액션 | 기본 구현 | MediaSession 알림 커스터마이징 |
