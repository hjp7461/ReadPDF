# 자바 PDF 뷰어 프로젝트 코딩 스타일 가이드

PDF 뷰어 애플리케이션 개발을 위한 모던 자바 코딩 스타일 가이드입니다. 이 가이드는 JDK 21의 최신 기능을 활용하여 코드 품질, 일관성, 그리고 유지보수성을 높이기 위한 원칙들을 포함합니다.

## 1. 프로젝트 구조

### 1.1 패키지 구조
```
com.pdfviewer/
├── application/       # 애플리케이션 진입점 및 구성
├── controller/        # MVC 컨트롤러 클래스
├── model/             # 데이터 모델 클래스
│   ├── entity/        # 데이터베이스 엔티티
│   ├── service/       # 비즈니스 로직 서비스
│   └── repository/    # 데이터 액세스 객체
├── view/              # UI 컴포넌트
│   ├── panel/         # 주요 UI 패널
│   ├── dialog/        # 대화 상자
│   └── component/     # 재사용 가능한 UI 컴포넌트
├── util/              # 유틸리티 클래스
└── config/            # 설정 관련 클래스
```

### 1.2 파일 명명 규칙
- 클래스 파일: PascalCase (`PdfViewerFrame.java`, `BookmarkManager.java`)
- 인터페이스: PascalCase + 접두사 'I' 또는 의미있는 이름 (`PdfRenderer.java`, `Searchable.java`)
- 추상 클래스: PascalCase + 접두사 'Abstract' (`AbstractPdfPage.java`)

## 2. 코드 포맷팅

### 2.1 들여쓰기 및 공백
- 4칸 공백 사용 (탭 대신)
- 최대 줄 길이: 120자
- 줄 바꿈 시 8칸 들여쓰기
- 메서드 간 한 줄 공백
- 논리적 코드 블록 간 한 줄 공백

### 2.2 중괄호
```java
// 권장
if (condition) {
    doSomething();
} else {
    doSomethingElse();
}

// 피해야 할 방식
if (condition)
{
    doSomething();
}
else {
    doSomethingElse();
}
```

### 2.3 명명 규칙
- 클래스: PascalCase (`PdfDocument`, `BookmarkPanel`)
- 메서드 및 변수: camelCase (`openFile()`, `currentPageNumber`)
- 상수: UPPER_SNAKE_CASE (`MAX_ZOOM_LEVEL`, `DEFAULT_PAGE_MODE`)
- 패키지: 모두 소문자 (`com.pdfviewer.model`)

## 3. JDK 21 언어 기능 활용

### 3.1 람다 표현식과 스트림 API
```java
// 권장
List<Bookmark> activeBookmarks = bookmarks.stream()
        .filter(Bookmark::isActive)
        .sorted(Comparator.comparing(Bookmark::getPageNumber))
        .collect(Collectors.toList());

// 피해야 할 방식
List<Bookmark> activeBookmarks = new ArrayList<>();
for (Bookmark bookmark : bookmarks) {
    if (bookmark.isActive()) {
        activeBookmarks.add(bookmark);
    }
}
Collections.sort(activeBookmarks, new Comparator<Bookmark>() {
    @Override
    public int compare(Bookmark b1, Bookmark b2) {
        return Integer.compare(b1.getPageNumber(), b2.getPageNumber());
    }
});
```

### 3.2 Optional 사용
```java
// 권장
return Optional.ofNullable(getBookmarkByPage(pageNumber))
        .map(Bookmark::getName)
        .orElse("Unnamed Bookmark");

// 피해야 할 방식
Bookmark bookmark = getBookmarkByPage(pageNumber);
if (bookmark != null) {
    return bookmark.getName();
} else {
    return "Unnamed Bookmark";
}
```

### 3.3 향상된 for문과 forEach
```java
// 컬렉션 순회 시 권장
for (PdfPage page : document.getPages()) {
    renderPage(page);
}

// 또는
document.getPages().forEach(this::renderPage);
```

### 3.4 var 키워드 활용
```java
// 타입이 명확한 경우 권장
var bookmarks = new ArrayList<Bookmark>();
var pdfDocument = pdfService.openDocument(filePath);

// 타입이 불명확한 경우 명시적 타입 사용
PdfRenderer renderer = factory.createRenderer();
```

### 3.5 패턴 매칭과 향상된 switch 문 (JDK 21)
```java
// 패턴 매칭을 활용한 인스턴스 체크
if (obj instanceof String s && s.length() > 0) {
    processString(s);
}

// 향상된 switch 표현식
ViewMode mode = switch (userPreference) {
    case "single" -> ViewMode.SINGLE_PAGE;
    case "double" -> ViewMode.DOUBLE_PAGE;
    case "continuous" -> ViewMode.CONTINUOUS_SCROLL;
    default -> ViewMode.SINGLE_PAGE;
};

// 패턴 매칭과 switch 조합
String description = switch (document) {
    case PdfDocument pdf when pdf.getPageCount() > 100 -> "대용량 PDF 문서";
    case PdfDocument pdf -> "일반 PDF 문서";
    case ImageDocument img -> "이미지 문서";
    case null -> "문서 없음";
    default -> "알 수 없는 문서 유형";
};
```

## 4. 클래스 디자인

### 4.1 레코드 활용 (JDK 21)
```java
// DTO나 불변 데이터 모델에 권장
public record BookmarkData(String id, String name, int pageNumber, LocalDateTime createdAt) {}

// 중첩 레코드를 활용한 복잡한 데이터 모델링
public record PdfDocumentMetadata(
    String filePath, 
    int pageCount, 
    PdfVersion version,
    Map<String, String> properties,
    List<PdfPageInfo> pages
) {
    // 중첩 레코드 정의
    public record PdfPageInfo(int pageNumber, Dimension size, boolean hasText) {}
    public record PdfVersion(int major, int minor) {}
}
```

### 4.2 불변 객체 선호
```java
public final class PageMetadata {
    private final int pageNumber;
    private final int width;
    private final int height;
    
    // 모든 필드 초기화하는 생성자
    public PageMetadata(int pageNumber, int width, int height) {
        this.pageNumber = pageNumber;
        this.width = width;
        this.height = height;
    }
    
    // getter만 제공 (setter 없음)
    public int getPageNumber() { return pageNumber; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
```

### 4.3 빌더 패턴 활용
```java
public class PdfViewerSettings {
    private final boolean rememberLastPosition;
    private final boolean darkMode;
    private final int defaultZoomLevel;
    private final ViewMode defaultViewMode;
    
    private PdfViewerSettings(Builder builder) {
        this.rememberLastPosition = builder.rememberLastPosition;
        this.darkMode = builder.darkMode;
        this.defaultZoomLevel = builder.defaultZoomLevel;
        this.defaultViewMode = builder.defaultViewMode;
    }
    
    public static class Builder {
        private boolean rememberLastPosition = true;
        private boolean darkMode = false;
        private int defaultZoomLevel = 100;
        private ViewMode defaultViewMode = ViewMode.SINGLE_PAGE;
        
        public Builder rememberLastPosition(boolean value) {
            this.rememberLastPosition = value;
            return this;
        }
        
        public Builder darkMode(boolean value) {
            this.darkMode = value;
            return this;
        }
        
        public Builder defaultZoomLevel(int value) {
            this.defaultZoomLevel = value;
            return this;
        }
        
        public Builder defaultViewMode(ViewMode value) {
            this.defaultViewMode = value;
            return this;
        }
        
        public PdfViewerSettings build() {
            return new PdfViewerSettings(this);
        }
    }
}

// 사용 예
var settings = new PdfViewerSettings.Builder()
        .darkMode(true)
        .defaultZoomLevel(120)
        .build();
```

### 4.4 봉인된 클래스(Sealed Classes) 활용
```java
// 특정 하위 클래스만 허용하는 봉인된 클래스
public sealed abstract class PdfViewerCommand 
    permits OpenCommand, SaveCommand, PrintCommand, ExportCommand {
    
    private final LocalDateTime timestamp;
    
    protected PdfViewerCommand() {
        this.timestamp = LocalDateTime.now();
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    // 공통 메서드
    public abstract boolean execute();
}

// 허용된 구현 클래스
public final class OpenCommand extends PdfViewerCommand {
    private final Path filePath;
    
    public OpenCommand(Path filePath) {
        this.filePath = filePath;
    }
    
    @Override
    public boolean execute() {
        // 구현...
        return true;
    }
}

// 다른 허용된 구현 클래스들...
public final class SaveCommand extends PdfViewerCommand { /*...*/ }
public final class PrintCommand extends PdfViewerCommand { /*...*/ }
public non-sealed class ExportCommand extends PdfViewerCommand { /*...*/ }
```

## 5. 예외 처리

### 5.1 구체적인 예외 사용
```java
// 권장
try {
    pdfRenderer.renderPage(pageNumber);
} catch (PageNotFoundException e) {
    logger.warn("페이지를 찾을 수 없습니다: {}", pageNumber);
    showErrorMessage("존재하지 않는 페이지입니다: " + pageNumber);
} catch (RenderingException e) {
    logger.error("페이지 렌더링 오류", e);
    showErrorMessage("페이지를 표시할 수 없습니다. 상세: " + e.getMessage());
}

// 피해야 할 방식
try {
    pdfRenderer.renderPage(pageNumber);
} catch (Exception e) {
    logger.error("오류 발생", e);
    showErrorMessage("오류가 발생했습니다.");
}
```

### 5.2 예외 래핑
```java
try {
    duckDbConnection.executeQuery(sql);
} catch (SQLException e) {
    throw new DatabaseException("북마크 데이터 조회 실패", e);
}
```

## 6. 멀티스레딩 및 동시성

### 6.1 SwingWorker 활용 (UI 작업)
```java
private void loadPdfInBackground(Path filePath) {
    new SwingWorker<PdfDocument, Void>() {
        @Override
        protected PdfDocument doInBackground() throws Exception {
            return pdfService.openDocument(filePath);
        }

        @Override
        protected void done() {
            try {
                PdfDocument document = get();
                displayDocument(document);
                updateRecentFilesList(filePath);
            } catch (Exception e) {
                handleDocumentLoadError(e);
            }
        }
    }.execute();
}
```

### 6.2 JDK 21 가상 스레드 활용
```java
// 가상 스레드 팩토리
private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

// 가상 스레드를 사용한 복잡한 작업 처리
private void processLargePdf(PdfDocument document) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<?>> tasks = new ArrayList<>();
        
        // 각 페이지별 처리 작업 생성
        for (int i = 0; i < document.getPageCount(); i++) {
            final int pageIndex = i;
            tasks.add(executor.submit(() -> {
                processPdfPage(document, pageIndex);
                return null;
            }));
        }
        
        // 모든 작업 완료 대기
        for (Future<?> task : tasks) {
            task.get();
        }
    } catch (Exception e) {
        logger.error("PDF 처리 중 오류 발생", e);
    }
}
```

### 6.3 구조적 동시성 활용 (JDK 21)
```java
void processDocumentWithStructuredConcurrency(PdfDocument document) {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        // 썸네일 생성 작업
        Subtask<List<BufferedImage>> thumbnailTask = 
            scope.fork(() -> generateThumbnails(document));
        
        // 텍스트 추출 작업
        Subtask<Map<Integer, String>> textExtractionTask = 
            scope.fork(() -> extractTextFromAllPages(document));
            
        // 모든 작업 완료 또는 하나라도 실패할 때까지 대기
        scope.join();
        scope.throwIfFailed();
        
        // 결과 처리
        List<BufferedImage> thumbnails = thumbnailTask.get();
        Map<Integer, String> pageTexts = textExtractionTask.get();
        
        // UI 업데이트 (EDT에서 실행)
        SwingUtilities.invokeLater(() -> {
            updateThumbnailPanel(thumbnails);
            updateSearchIndex(pageTexts);
        });
    } catch (Exception e) {
        logger.error("문서 처리 오류", e);
        showErrorDialog("문서 처리 중 오류가 발생했습니다.");
    }
}
```

### 6.4 ExecutorService 활용
```java
// 애플리케이션 초기화 시
ExecutorService pdfLoadExecutor = Executors.newFixedThreadPool(2);
ExecutorService thumbnailExecutor = Executors.newSingleThreadExecutor();

// 사용
public CompletableFuture<List<Image>> generateThumbnails(List<PdfPage> pages) {
    return CompletableFuture.supplyAsync(() -> 
        pages.stream()
            .map(thumbnailRenderer::createThumbnail)
            .collect(Collectors.toList()),
        thumbnailExecutor
    );
}

// 애플리케이션 종료 시
private void shutdownExecutors() {
    pdfLoadExecutor.shutdown();
    thumbnailExecutor.shutdown();
    try {
        if (!pdfLoadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            pdfLoadExecutor.shutdownNow();
        }
        if (!thumbnailExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            thumbnailExecutor.shutdownNow();
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

## 7. 데이터베이스 접근

### 7.1 리포지토리 패턴
```java
public interface BookmarkRepository {
    List<Bookmark> findAllByFileId(String fileId);
    Optional<Bookmark> findById(String bookmarkId);
    void save(Bookmark bookmark);
    void deleteById(String bookmarkId);
}

@Repository
public class DuckDbBookmarkRepository implements BookmarkRepository {
    private final DuckDbConnectionFactory connectionFactory;
    
    // 메서드 구현
    @Override
    public List<Bookmark> findAllByFileId(String fileId) {
        // SQL 쿼리 및 매핑 로직
    }
}
```

### 7.2 트랜잭션 관리
```java
public void addBookmarkAndUpdateFile(Bookmark bookmark, PdfFileInfo fileInfo) {
    try (Connection conn = connectionFactory.getConnection()) {
        try {
            conn.setAutoCommit(false);
            
            bookmarkDao.insert(conn, bookmark);
            fileInfoDao.updateLastModified(conn, fileInfo);
            
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw new DatabaseException("북마크 추가 트랜잭션 실패", e);
        } finally {
            conn.setAutoCommit(true);
        }
    } catch (SQLException e) {
        throw new DatabaseException("데이터베이스 연결 실패", e);
    }
}
```

## 8. 로깅

### 8.1 SLF4J와 로거 선언
```java
// 클래스 상단에 선언
private static final Logger logger = LoggerFactory.getLogger(PdfViewerFrame.class);

// 사용
logger.debug("PDF 파일 로드 시작: {}", filePath);
logger.info("PDF 뷰어 초기화 완료");
logger.warn("페이지 렌더링에 오랜 시간이 걸림: {} ms", renderTime);
logger.error("PDF 파일 로드 실패", exception);
```

### 8.2 적절한 로그 레벨 사용
- ERROR: 애플리케이션 동작을 방해하는 심각한 문제
- WARN: 잠재적인 문제 상황이지만 실행은 계속 가능
- INFO: 일반적인 애플리케이션 진행 상황
- DEBUG: 개발/디버깅에 유용한 상세 정보
- TRACE: 매우 상세한 진단 정보

### 8.3 구조화된 로깅
```java
// MDC(Mapped Diagnostic Context)를 활용한 구조화된 로깅
public void openPdf(File file) {
    // MDC에 파일 식별자 추가
    MDC.put("fileId", UUID.randomUUID().toString());
    MDC.put("fileName", file.getName());
    
    try {
        logger.info("PDF 파일 로드 시작");
        // PDF 로드 작업...
        logger.info("PDF 파일 로드 완료: 페이지 수={}", document.getPageCount());
    } catch (Exception e) {
        logger.error("PDF 파일 로드 실패", e);
    } finally {
        // MDC 정리
        MDC.clear();
    }
}
```

### 8.4 성능 모니터링 로깅
```java
public BufferedImage renderPage(int pageIndex) {
    long startTime = System.nanoTime();
    logger.debug("페이지 렌더링 시작: {}", pageIndex);
    
    try {
        BufferedImage result = pdfRenderer.renderImageWithDPI(pageIndex, 300);
        
        long endTime = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // 임계값(500ms) 이상 소요되면 경고 로그
        if (durationMs > 500) {
            logger.warn("페이지 {} 렌더링에 긴 시간 소요: {} ms", pageIndex, durationMs);
        } else {
            logger.debug("페이지 {} 렌더링 완료: {} ms", pageIndex, durationMs);
        }
        
        return result;
    } catch (Exception e) {
        logger.error("페이지 {} 렌더링 실패", pageIndex, e);
        throw new RenderingException("페이지 렌더링 실패", e);
    }
}

## 9. 테스트

### 9.1 단위 테스트 명명 규칙
```java
@Test
void shouldOpenPdfFile_whenValidPathProvided() {
    // 테스트 구현
}

@Test
void shouldThrowException_whenInvalidPdfFormat() {
    // 테스트 구현
}
```

### 9.2 Mockito 활용
```java
@ExtendWith(MockitoExtension.class)
class PdfServiceTest {
    @Mock
    private PdfRenderer renderer;
    
    @Mock
    private PdfFileRepository fileRepository;
    
    @InjectMocks
    private PdfServiceImpl pdfService;
    
    @Test
    void shouldRenderPage_whenPageExists() {
        // given
        int pageNumber = 5;
        PdfDocument doc = mock(PdfDocument.class);
        when(doc.getPageCount()).thenReturn(10);
        
        // when
        pdfService.renderPage(doc, pageNumber);
        
        // then
        verify(renderer).render(eq(doc), eq(pageNumber), any(RenderOptions.class));
    }
}
```

## 10. Swing 특화 가이드라인

### 10.1 GUI 생성은 EDT에서
```java
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            PdfViewerFrame frame = new PdfViewerFrame();
            frame.setVisible(true);
        } catch (Exception e) {
            logger.error("애플리케이션 실행 실패", e);
        }
    });
}
```

### 10.2 컴포넌트 계층화
```java
public class BookmarkPanel extends JPanel {
    private final JList<Bookmark> bookmarkList;
    private final BookmarkListModel listModel;
    private final JButton addButton;
    private final JButton removeButton;
    
    // 생성자 및 메서드 구현
}
```

### 10.3 이벤트 리스너
```java
// 람다 활용 (간단한 리스너)
zoomInButton.addActionListener(e -> zoomManager.zoomIn());

// 별도 메서드 참조
openButton.addActionListener(this::handleOpenFile);

// 복잡한 리스너는 별도 클래스로
pageList.addListSelectionListener(new PageSelectionListener());

private class PageSelectionListener implements ListSelectionListener {
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            int selectedIndex = pageList.getSelectedIndex();
            if (selectedIndex >= 0) {
                navigateToPage(selectedIndex + 1);
            }
        }
    }
}
```

## 11. 빌드 및 패키징

### 11.1 Gradle 빌드 스크립트
```groovy
plugins {
    id 'java'
    id 'application'
    id 'org.beryx.runtime' version '1.13.0' // 커스텀 JRE 생성
}

group = 'com.example.pdfviewer'
version = '1.0-SNAPSHOT'
sourceCompatibility = '21'
targetCompatibility = '21'

repositories {
    mavenCentral()
}

dependencies {
    // 의존성 섹션은 라이브러리 목록 참조
}

application {
    mainClass = 'com.example.pdfviewer.Main'
}

// macOS 배포를 위한 설정
runtime {
    options = ['--strip-debug', '--compress', '2', '--no-header-files', '--no-man-pages']
    
    jpackage {
        // macOS 설정
        imageOptions = ['--icon', 'src/main/resources/icons/app.icns']
        installerOptions = [
            '--mac-package-name', 'PDF Viewer',
            '--mac-package-identifier', 'com.example.pdfviewer',
            '--mac-signing-key-user-name', 'Developer ID Application: Your Name'
        ]
    }
}
```

### 11.2 백그라운드 작업 구현 가이드라인
```java
/**
 * PDF 파일 로딩 진행 상황 및 결과 리스너
 */
public interface PdfLoadListener {
    void onProgress(int percentage, String message);
    void onComplete(PdfDocument document);
    void onError(Exception error);
}

/**
 * 백그라운드에서 PDF 파일 로딩 처리
 * @param path PDF 파일 경로
 * @param listener 진행 상황 및 결과 통지 리스너
 */
public void loadPdfInBackground(Path path, PdfLoadListener listener) {
    // 가상 스레드 활용
    Thread.startVirtualThread(() -> {
        try {
            // 진행 상태 업데이트
            SwingUtilities.invokeLater(() -> 
                listener.onProgress(0, "PDF 파일 로딩 시작..."));
            
            // 파일 로드
            PDDocument doc = PDDocument.load(path.toFile());
            
            // 메타데이터 추출 (25%)
            SwingUtilities.invokeLater(() -> 
                listener.onProgress(25, "메타데이터 추출 중..."));
            var metadata = extractMetadata(doc);
            
            // 썸네일 생성 (50%)
            SwingUtilities.invokeLater(() -> 
                listener.onProgress(50, "썸네일 생성 중..."));
            generateThumbnails(doc);
            
            // 결과 생성 (75%)
            SwingUtilities.invokeLater(() -> 
                listener.onProgress(75, "뷰어 준비 중..."));
            PdfDocument pdfDocument = createViewerDocument(doc, metadata);
            
            // 완료 통지 (EDT에서)
            SwingUtilities.invokeLater(() -> {
                listener.onProgress(100, "로딩 완료");
                listener.onComplete(pdfDocument);
            });
            
        } catch (Exception e) {
            logger.error("PDF 로딩 오류", e);
            // 오류 통지 (EDT에서)
            SwingUtilities.invokeLater(() -> 
                listener.onError(e));
        }
    });
}
```

## 12. 성능 최적화 가이드라인

### 12.1 페이지 렌더링 최적화

```java
/**
 * 페이지 렌더링을 위한 최적화된 캐시 시스템
 */
public class PageRenderCache {
    private final int maxCacheSize;
    private final Map<Integer, SoftReference<BufferedImage>> pageCache;
    private final LRUTracker<Integer> lruTracker;
    
    public PageRenderCache(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        this.pageCache = new ConcurrentHashMap<>();
        this.lruTracker = new LRUTracker<>(maxCacheSize);
    }
    
    public BufferedImage getPage(int pageIndex) {
        SoftReference<BufferedImage> ref = pageCache.get(pageIndex);
        if (ref != null) {
            BufferedImage image = ref.get();
            if (image != null) {
                // 캐시 히트 - LRU 업데이트
                lruTracker.recordAccess(pageIndex);
                return image;
            }
        }
        return null;
    }
    
    public void cachePage(int pageIndex, BufferedImage image) {
        // 캐시 크기 관리
        if (pageCache.size() >= maxCacheSize) {
            // 가장 오래 사용되지 않은 페이지 제거
            Integer oldestPage = lruTracker.getOldestEntry();
            if (oldestPage != null) {
                pageCache.remove(oldestPage);
            }
        }
        
        // 새 페이지 캐시에 추가
        pageCache.put(pageIndex, new SoftReference<>(image));
        lruTracker.recordAccess(pageIndex);
    }
    
    public void clearCache() {
        pageCache.clear();
        lruTracker.clear();
    }
    
    // LRU 추적 도우미 클래스
    private static class LRUTracker<T> {
        private final int maxSize;
        private final Deque<T> queue;
        private final Set<T> set;
        
        public LRUTracker(int maxSize) {
            this.maxSize = maxSize;
            this.queue = new LinkedList<>();
            this.set = new HashSet<>();
        }
        
        public void recordAccess(T item) {
            if (set.contains(item)) {
                queue.remove(item);
            } else if (queue.size() >= maxSize) {
                T oldest = queue.removeLast();
                set.remove(oldest);
            }
            
            queue.addFirst(item);
            set.add(item);
        }
        
        public T getOldestEntry() {
            return queue.isEmpty() ? null : queue.getLast();
        }
        
        public void clear() {
            queue.clear();
            set.clear();
        }
    }
}
```

### 12.2 지능형 페이지 프리로딩

```java
/**
 * 사용자 탐색 패턴에 기반한 지능형 페이지 프리로더
 */
public class IntelligentPreloader {
    private static final Logger logger = LoggerFactory.getLogger(IntelligentPreloader.class);
    
    private final PDFRenderer renderer;
    private final PageRenderCache cache;
    private final ExecutorService executor;
    
    private final NavigationTracker navigationTracker = new NavigationTracker();
    private final AtomicInteger currentPage = new AtomicInteger(0);
    
    public IntelligentPreloader(PDFRenderer renderer, PageRenderCache cache) {
        this.renderer = renderer;
        this.cache = cache;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }
    
    public void setCurrentPage(int pageNumber) {
        int oldPage = currentPage.getAndSet(pageNumber);
        navigationTracker.recordNavigation(oldPage, pageNumber);
        preloadAdjacentPages(pageNumber);
    }
    
    private void preloadAdjacentPages(int currentPage) {
        // 먼저 항상 다음 페이지와 이전 페이지 프리로드
        List<Integer> pagesToPreload = new ArrayList<>();
        
        // 다음 페이지
        if (isValidPage(currentPage + 1)) {
            pagesToPreload.add(currentPage + 1);
        }
        
        // 이전 페이지
        if (isValidPage(currentPage - 1)) {
            pagesToPreload.add(currentPage - 1);
        }
        
        // 탐색 패턴에 따른 추가 페이지
        NavigationPattern pattern = navigationTracker.getNavigationPattern();
        if (pattern == NavigationPattern.FORWARD) {
            // 앞으로 더 많은 페이지 프리로드
            if (isValidPage(currentPage + 2)) pagesToPreload.add(currentPage + 2);
            if (isValidPage(currentPage + 3)) pagesToPreload.add(currentPage + 3);
        } else if (pattern == NavigationPattern.BACKWARD) {
            // 뒤로 더 많은 페이지 프리로드
            if (isValidPage(currentPage - 2)) pagesToPreload.add(currentPage - 2);
            if (isValidPage(currentPage - 3)) pagesToPreload.add(currentPage - 3);
        }
        
        // 비동기적으로 프리로딩
        for (Integer pageNumber : pagesToPreload) {
            if (cache.getPage(pageNumber) == null) {
                executor.submit(() -> {
                    try {
                        logger.debug("페이지 프리로딩: {}", pageNumber);
                        BufferedImage image = renderer.renderImageWithDPI(pageNumber, 150);
                        cache.cachePage(pageNumber, image);
                    } catch (Exception e) {
                        logger.warn("페이지 {} 프리로딩 실패", pageNumber, e);
                    }
                });
            }
        }
    }
    
    private boolean isValidPage(int pageNumber) {
        return pageNumber >= 0 && pageNumber < renderer.getPageCount();
    }
    
    // 탐색 패턴 추적기
    private static class NavigationTracker {
        private final Deque<Integer> recentNavigations = new LinkedList<>();
        private static final int HISTORY_SIZE = 10;
        
        public void recordNavigation(int fromPage, int toPage) {
            int direction = Integer.compare(toPage, fromPage);
            if (direction != 0) { // 실제 페이지 변경이 있을 때만
                recentNavigations.addFirst(direction);
                if (recentNavigations.size() > HISTORY_SIZE) {
                    recentNavigations.removeLast();
                }
            }
        }
        
        public NavigationPattern getNavigationPattern() {
            if (recentNavigations.size() < 3) {
                return NavigationPattern.RANDOM;
            }
            
            // 최근 탐색 방향 분석
            int forwardCount = 0;
            int backwardCount = 0;
            
            for (Integer direction : recentNavigations) {
                if (direction > 0) forwardCount++;
                else if (direction < 0) backwardCount++;
            }
            
            double forwardRatio = (double) forwardCount / recentNavigations.size();
            double backwardRatio = (double) backwardCount / recentNavigations.size();
            
            if (forwardRatio > 0.7) return NavigationPattern.FORWARD;
            if (backwardRatio > 0.7) return NavigationPattern.BACKWARD;
            return NavigationPattern.RANDOM;
        }
    }
    
    // 탐색 패턴 열거형
    private enum NavigationPattern {
        FORWARD, BACKWARD, RANDOM
    }
}
```

### 12.3 메모리 사용량 모니터링

```java
/**
 * 메모리 사용량 모니터링 및 자동 조절
 */
public class MemoryMonitor {
    private static final Logger logger = LoggerFactory.getLogger(MemoryMonitor.class);
    
    private final Runtime runtime = Runtime.getRuntime();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<MemoryListener> listeners = new CopyOnWriteArrayList<>();
    
    // 자동 조절 임계값
    private static final double WARNING_THRESHOLD = 0.8; // 80%
    private static final double CRITICAL_THRESHOLD = 0.9; // 90%
    
    public void start() {
        // 5초마다 메모리 사용량 체크
        scheduler.scheduleAtFixedRate(this::checkMemoryUsage, 5, 5, TimeUnit.SECONDS);
    }
    
    public void stop() {
        scheduler.shutdown();
    }
    
    public void addListener(MemoryListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(MemoryListener listener) {
        listeners.remove(listener);
    }
    
    private void checkMemoryUsage() {
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double usageRatio = (double) usedMemory / maxMemory;
        
        // 메모리 사용량 로깅 (1분마다)
        if (System.currentTimeMillis() % 60000 < 5000) {
            logger.debug("메모리 사용량: {}/{} MB ({}%)", 
                usedMemory / (1024*1024), 
                maxMemory / (1024*1024), 
                (int)(usageRatio * 100));
        }
        
        // 임계값에 따른 알림
        if (usageRatio > CRITICAL_THRESHOLD) {
            logger.warn("심각한 메모리 사용량: {}%", (int)(usageRatio * 100));
            notifyListeners(MemoryStatus.CRITICAL);
        } else if (usageRatio > WARNING_THRESHOLD) {
            logger.info("높은 메모리 사용량: {}%", (int)(usageRatio * 100));
            notifyListeners(MemoryStatus.WARNING);
        }
    }
    
    private void notifyListeners(MemoryStatus status) {
        for (MemoryListener listener : listeners) {
            try {
                listener.onMemoryStatusChanged(status);
            } catch (Exception e) {
                logger.error("메모리 리스너 오류", e);
            }
        }
    }
    
    public enum MemoryStatus {
        NORMAL, WARNING, CRITICAL
    }
    
    public interface MemoryListener {
        void onMemoryStatusChanged(MemoryStatus status);
    }
}
```

## 13. 마무리

이 코딩 스타일 가이드는 JDK 21의 모던 자바 기능들(가상 스레드, 레코드, 패턴 매칭, 봉인 클래스 등)을 활용하여 PDF 뷰어 애플리케이션을 개발하는 데 도움이 될 것입니다.

특히 다음 사항에 주의하세요:

1. **가상 스레드 활용**: JDK 21의 가상 스레드를 활용하여 높은 동시성을 유지하면서도 시스템 리소스를 효율적으로 사용하세요.

2. **불변 객체 선호**: 특히 멀티스레딩 환경에서 불변 객체를 사용하여 동시성 문제를 예방하세요.

3. **모던 자바 API 활용**: Optional, Stream API, 패턴 매칭 등의 최신 기능을 적극적으로 활용하여 간결하고 명확한 코드를 작성하세요.

4. **성능 모니터링**: 대용량 PDF 처리 시 메모리 및 성능을 모니터링하고 최적화하는 방안을 마련하세요.

5. **적절한 예외 처리**: 구체적인 예외 처리를 통해 장애 상황에 대응하고 이를 로깅하세요.

팀 내에서 이 스타일 가이드를 공유하고 정기적으로 코드 리뷰를 통해 일관성을 유지하세요. 필요에 따라 이 가이드를 확장하거나 수정하여 프로젝트의 특성에 맞게 발전시켜 나가는 것이 좋습니다.