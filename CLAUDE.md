# ArisuCast — Claude Code 가이드

## 빌드 명령어

```bash
# 시스템 Java가 17/21이 아닌 경우 반드시 Android Studio JDK 사용
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleDebug        # 디버그 APK 빌드
./gradlew test                 # 전체 단위 테스트
./gradlew :feature:feature-search:compileDebugKotlin  # 특정 모듈만 컴파일 확인
```

## 프로젝트 구조

- `app/` — 진입점, Navigation, `RefreshFeedsWorker`
- `core/core-common/` — 도메인 모델(`Podcast`, `Episode`, `PlaybackState`, `PodcastSortOrder`), `Result<T>`, `sha256()`
- `core/core-database/` — Room DB v2, DAOs (PodcastDao, EpisodeDao, SubscriptionDao)
- `core/core-network/` — `RssParser` (Rome+iTunes), `ItunesSearchApi` (Retrofit)
- `core/core-media/` — `PlaybackRepository` (@Singleton), `PlaybackService` (MediaSessionService)
- `core/core-download/` — `EpisodeDownloadWorker` (HiltWorker), `DownloadManager`
- `core/core-datastore/` — `UserPreferencesDataStore` (DataStore Preferences)
- `core/core-ui/` — Compose 컴포넌트, `ArisuCastTheme`
- `feature/feature-*` — 각 화면의 Screen + ViewModel (+ UseCase)

## 핵심 패턴

### UiState
```kotlin
sealed class XxxUiState {
    object Loading : XxxUiState()
    data class Success(val data: ...) : XxxUiState()
    data class Error(val message: String) : XxxUiState()
}
// ViewModel에서 StateFlow<XxxUiState>로 노출
// Composable에서 collectAsStateWithLifecycle()로 수집
```

### Flow combine 타입 추론
```kotlin
combine(flow1, flow2) { a, b ->
    // 반환 타입을 명시적으로 캐스트 (Kotlin 추론 한계)
    if (...) UiState.Success(...) else UiState.Error(...) as UiState
}
```

### Episode ID
```kotlin
val id = episode.guid.sha256()  // com.arisucast.core.common.extensions
// GUID가 없으면 feedUrl + audioUrl 조합 사용
```

### Room insertAll 전략
```kotlin
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertAll(episodes: List<EpisodeEntity>)
// IGNORE: 피드 새로고침 시 기존 에피소드의 재생 위치 보존
// REPLACE 사용 금지 (재생 위치가 초기화됨)
```

### PlaybackRepository 재생 완료 처리
- `STATE_ENDED` 발생 시 DB의 `playbackPositionMs`를 0으로 초기화
  - 이유: 마지막 위치(= duration)가 그대로 저장되면 다음 재생 시 `seekTo(duration)` → 즉시 ENDED 반복
- `onPlayerError` 에서 `player.stop()` 호출 → IDLE 상태로 복구
- `playEpisode()` 진입 시 `player.stop()` 선행 → ENDED/에러 상태 클리어 후 새 미디어 설정

### ExoPlayer 버퍼 설정 (MediaModule)
```kotlin
DefaultLoadControl.Builder()
    .setBufferDurationsMs(50_000, 50_000, 500, 1_000)
    //                              ^bufferForPlaybackMs: 기본 2500 → 500ms
    //                                       ^rebuffer: 기본 5000 → 1000ms
```
팟캐스트 오디오는 저비트레이트이므로 500ms 버퍼로 충분 — 재생 시작 딜레이 ~2초 단축

### 팟캐스트 정렬 / 즐겨찾기
- `PodcastSortOrder` enum: `NAME_ASC` / `LAST_UPDATED` / `FAVORITES_FIRST` (core-common)
- `PodcastEntity` / `Podcast`에 `isFavorite: Boolean` 추가 → Room DB 버전 2
- `PodcastDao.updateFavorite(id, favorite)` 쿼리 추가
- 정렬 로직은 ViewModel에서 인메모리 처리 (목록이 작아 SQL 정렬 불필요)
- `PodcastCard`에 `isFavorite` / `onFavoriteToggle` optional 파라미터 추가

## 의존성 규칙

```
feature-* → core-* (허용)
feature-* → feature-* (금지)
core-network → core-database (금지 — 이 때문에 RefreshFeedsWorker는 app 모듈에)
```

## 주의사항

- **Hilt 모듈**: 새 `@Module` 파일이 있는 모듈은 반드시 build.gradle.kts에 Hilt 플러그인 + 의존성 추가 필요
  ```kotlin
  plugins { alias(libs.plugins.hilt); alias(libs.plugins.ksp) }
  dependencies { implementation(libs.hilt.android); ksp(libs.hilt.compiler) }
  ```

- **Worker 위치**: `@HiltWorker`가 다른 core 모듈을 필요로 하면 `app/worker/`에 배치

- **rome + rome-modules JAR 충돌**: `app/build.gradle.kts`에 `pickFirsts` 이미 설정됨 — 건드리지 말 것

- **Media3 테스트**: `Dispatchers.setMain(UnconfinedTestDispatcher())`를 `@Before`에 추가해야 `PlaybackRepository` 테스트가 동작함

- **consumer-rules.pro**: 모든 library 모듈에 빈 파일 필요 (없으면 빌드 경고)

- **Room 마이그레이션**: 컬럼 추가 시 반드시 `Migration` 객체 작성 후 `DatabaseModule`에 `.addMigrations(...)` 등록
  ```kotlin
  // ArisuCastDatabase.kt
  val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE podcasts ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
      }
  }
  // DatabaseModule.kt
  Room.databaseBuilder(...).addMigrations(ArisuCastDatabase.MIGRATION_1_2).build()
  ```

- **앱 아이콘**: `res/drawable/ic_launcher_background.xml` (그라데이션) + `res/drawable/ic_launcher_foreground.xml` (헤드폰+음파 벡터). `mipmap-anydpi-v26/`이 Android 8+ 어댑티브 아이콘의 기본 위치. `<monochrome>` 레이어는 Android 13+ 다이나믹 테마 대응.

## 버전 정보 (gradle/libs.versions.toml)

| 항목 | 버전 |
|------|------|
| minSdk | 26 |
| compileSdk / targetSdk | 35 |
| Kotlin | 2.1.0 |
| AGP | 8.8.0 |
| Hilt | 2.54 |
| Room | 2.7.0 (DB schema v2) |
| Media3 | 1.5.1 |
| Compose BOM | 2025.02.00 |
