# 모듈별 파일 목록

## :app

```
app/
├── ArisuCastApplication.kt          @HiltAndroidApp, WorkManager 설정, RefreshFeedsWorker 등록
├── MainActivity.kt                  @AndroidEntryPoint, 다크 테마 적용, 알림 권한 요청, ArisuCastApp() 호출
├── navigation/
│   ├── AppDestination.kt            sealed class (Home/Search/Library/Settings) + AppRoutes
│   └── AppNavHost.kt                NavHost, 딥링크 (arisucast://subscribe?url=...) 처리
├── viewmodel/
│   └── MainViewModel.kt             PlaybackState, darkTheme StateFlow
└── worker/
    └── RefreshFeedsWorker.kt        HiltWorker, 6시간 주기 피드 갱신
```

## :core:core-common

```
core-common/
├── model/
│   ├── Podcast.kt                   도메인 모델
│   ├── Episode.kt                   도메인 모델 (DownloadState sealed class 포함)
│   └── PlaybackState.kt             isPlaying, positionMs, sleepTimerEndMs, currentPodcastTitle, ...
├── result/
│   └── Result.kt                    sealed class Success/Error/Loading, runCatchingResult
├── dispatcher/
│   └── DispatcherProvider.kt        인터페이스 + DefaultDispatcherProvider
├── extensions/
│   ├── StringExtensions.kt          sha256()
│   └── FlowExtensions.kt            Flow<T>.asResult()
└── di/
    └── CommonModule.kt              @Provides DispatcherProvider
```

## :core:core-database

```
core-database/
├── ArisuCastDatabase.kt             RoomDatabase (@Database)
├── entity/
│   ├── PodcastEntity.kt
│   ├── EpisodeEntity.kt             downloadStatus, localFilePath, playbackPositionMs
│   └── SubscriptionEntity.kt
├── dao/
│   ├── PodcastDao.kt                upsert, getById, observeAll, observeById
│   ├── EpisodeDao.kt                insertAll(IGNORE), observeByPodcast, updatePlaybackPosition
│   └── SubscriptionDao.kt           insert, isSubscribed, observeAll, delete
├── mapper/
│   ├── PodcastMapper.kt             Entity ↔ Domain
│   └── EpisodeMapper.kt             Entity ↔ Domain
└── di/
    └── DatabaseModule.kt            @Provides ArisuCastDatabase, DAOs
```

## :core:core-network

```
core-network/
├── rss/
│   └── RssParser.kt                 Rome + iTunes 확장, ParsedPodcast/ParsedEpisode
├── api/
│   ├── ItunesSearchApi.kt           Retrofit @GET("search")
│   └── dto/
│       ├── ItunesSearchResponse.kt  @Serializable DTO
│       └── PodcastSearchResponse.kt (Podcast Index용, 미사용)
└── di/
    └── NetworkModule.kt             OkHttpClient, Json, Retrofit, ItunesSearchApi, RssParser
```

## :core:core-media

```
core-media/
├── PlaybackRepository.kt            @Singleton, ExoPlayer 래핑, StateFlow<PlaybackState>
│                                    1초마다 위치 저장, 슬립 타이머
├── service/
│   └── PlaybackService.kt           MediaSessionService (알림 컨트롤, setSessionActivity, onTaskRemoved)
└── di/
    └── MediaModule.kt               @Provides ExoPlayer (팟캐스트 오디오 속성 설정)
```

## :core:core-download

```
core-download/
├── EpisodeDownloadWorker.kt         @HiltWorker, OkHttp 스트리밍, 진행률 setProgress()
└── DownloadManager.kt               WorkManager 래핑, getDownloadProgress(): Flow<Int>
```

## :core:core-datastore

```
core-datastore/
├── UserPreferencesDataStore.kt      wifiOnlyDownload, autoDownload, defaultPlaybackSpeed, darkTheme
└── di/
    └── DataStoreModule.kt           @Provides UserPreferencesDataStore
```

## :core:core-ui

```
core-ui/
├── theme/
│   ├── ArisuCastTheme.kt            Material3, 동적 색상, darkTheme 파라미터
│   ├── Color.kt
│   └── Typography.kt
├── component/
│   ├── PodcastCard.kt               아트워크 + 제목 + 저자
│   ├── EpisodeItem.kt               현재 재생 중 강조 (isCurrentlyPlaying)
│   ├── MiniPlayer.kt                미니 플레이어 기본형
│   ├── AnimatedMiniPlayer.kt        슬라이드 인 애니메이션
│   ├── LoadingIndicator.kt
│   └── ErrorMessage.kt              재시도 버튼 포함
└── util/
    └── DurationFormatter.kt         ms → "HH:MM:SS"
```

## :feature:feature-home

```
feature-home/
├── HomeScreen.kt                    최근 에피소드 + 구독 팟캐스트 목록
├── HomeViewModel.kt                 combine(subscriptions, recentEpisodes)
└── HomeUiState.kt
```

## :feature:feature-subscriptions

```
feature-subscriptions/
├── domain/
│   ├── SubscribeToFeedUseCase.kt    URL → RssParser → Room (PodcastEntity + Episodes + Subscription)
│   └── RefreshFeedUseCase.kt        피드 재파싱, insertAll(IGNORE)
├── SubscriptionsScreen.kt           2열 그리드, FAB, Snackbar, 딥링크 자동 구독
├── SubscriptionsViewModel.kt        subscribeState (isLoading/error/success) 별도 StateFlow
├── SubscriptionsUiState.kt
└── AddFeedDialog.kt                 URL 입력 다이얼로그
```

## :feature:feature-episodes

```
feature-episodes/
├── EpisodeListScreen.kt             팟캐스트 헤더 + 에피소드 목록
├── EpisodeListViewModel.kt          combine(podcast, episodes, playbackState), toggleDownload()
└── EpisodeListUiState.kt            currentEpisodeId, isPlaying 포함
```

## :feature:feature-player

```
feature-player/
├── PlayerScreen.kt                  아트워크, 슬라이더, 재생 컨트롤, 속도 선택, 슬립 타이머 UI
├── PlayerViewModel.kt               PlaybackRepository 위임, setSleepTimer(minutes)
└── PlayerUiState.kt                 sleepTimerEndMs, sleepTimerActive 포함
```

## :feature:feature-search

```
feature-search/
├── SearchScreen.kt                  SearchBar, 결과 목록 (구독/구독중/로딩 버튼)
├── SearchViewModel.kt               400ms 디바운스, subscribe() (RssParser + DAOs 직접)
└── SearchUiState.kt                 Idle/Loading/Success/Error
```

## :feature:feature-settings

```
feature-settings/
├── SettingsScreen.kt                Wi-Fi다운로드, 자동다운로드, 재생속도, 다크모드, 버전
├── SettingsViewModel.kt             DataStore 읽기/쓰기
└── SettingsUiState.kt
```

## 테스트 파일

```
core/core-common/src/test/
└── StringExtensionsTest.kt          sha256() 5개 케이스

core/core-media/src/test/
└── PlaybackRepositoryTest.kt        14개 케이스 (슬립타이머, 재생제어, seek)

feature/feature-subscriptions/src/test/
└── SubscribeToFeedUseCaseTest.kt    4개 케이스 (성공/기구독/오류/URL트리밍)

feature/feature-search/src/test/
└── SearchViewModelTest.kt           7개 케이스 (디바운스, 상태전환, Turbine Flow 테스트)
```
