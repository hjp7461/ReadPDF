# Java Swing PDF Viewer 요구사항

## 1. 개발 환경

- **JDK 버전**: Azul Zulu JDK 21 (LTS 버전)
- **빌드 시스템**: Gradle 8.5 이상
- **IDE**: IntelliJ IDEA
- **대상 플랫폼**: macOS (Intel 및 Apple Silicon)

## 2. 라이브러리 및 의존성

### 2.1 UI 프레임워크
```groovy
implementation 'javax.swing:swing:1.0.0'
implementation 'javax.awt:awt:1.0.0'
```

### 2.2 PDF 처리 라이브러리
```groovy
// Apache PDFBox - PDF 렌더링 및 조작
implementation 'org.apache.pdfbox:pdfbox:2.0.27'

// PDF 렌더링 성능 향상을 위한 옵션
implementation 'org.apache.pdfbox:pdfbox-tools:2.0.27'
implementation 'org.apache.pdfbox:preflight:2.0.27'
```

### 2.3 데이터베이스
```groovy
// SQLite JDBC 드라이버
implementation 'org.xerial:sqlite-jdbc:3.42.0.0'
```

### 2.4 유틸리티 라이브러리
```groovy
// 로깅
implementation 'org.slf4j:slf4j-api:2.0.9'
implementation 'ch.qos.logback:logback-classic:1.4.11'

// 유틸리티
implementation 'org.apache.commons:commons-lang3:3.14.0'
implementation 'commons-io:commons-io:2.15.1'

// JDK 21 지원을 위한 최신 라이브러리
implementation 'com.google.guava:guava:32.1.3-jre'
```

### 2.5 테스트 라이브러리
```groovy
// 단위 테스트
testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.1'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.1'
testImplementation 'org.junit.jupiter:junit-jupiter-params:5.10.1'

// 모킹
testImplementation 'org.mockito:mockito-core:5.8.0'
testImplementation 'org.mockito:mockito-junit-jupiter:5.8.0'
```

## 3. 기능 요구사항

### 3.1 파일 관리
- **PDF 파일 열기**: 시스템 파일 선택 대화상자를 통해 PDF 파일 선택 및 로딩
- **최근 파일 기록**: 최근에 열었던 PDF 파일 목록을 저장하고 표시
    - 프로그램 시작 시 최근 파일 목록 표시
    - 최근 파일 목록에서 파일 선택 시 해당 파일 열기
    - 최근 파일 목록에서 특정 파일 제거 기능
    - 최근 파일 목록 전체 초기화 기능

### 3.2 PDF 탐색 및 보기
- **페이지 탐색**:
    - 방향키 좌/우를 이용한 이전/다음 페이지 이동
    - 페이지 번호 직접 입력을 통한 특정 페이지 이동
    - 페이지 썸네일 목록을 통한 페이지 탐색
- **보기 모드**:
    - 단일 페이지 모드: 한 화면에 1페이지 표시
    - 두 페이지 모드: 한 화면에 2페이지 나란히 표시
    - 연속 스크롤 모드: 페이지 구분선과 함께 연속적으로 표시
- **확대/축소**:
    - 줌 인/아웃 기능 (단축키, 버튼, 마우스 휠 지원)
    - 페이지에 맞추기/너비에 맞추기 옵션
    - 사용자 지정 확대/축소 비율 설정 (50% - 400%)

### 3.3 사용자 설정
- **폰트 관리**:
    - 사용자 정의 폰트 추가 기능
    - 뷰어 인터페이스 폰트 변경 기능
    - 폰트 크기 조절 기능
- **색상 테마**:
    - 기본 테마(라이트 모드)
    - 다크 모드
    - 세피아 모드(독서 최적화)
- **읽기 위치 기억**:
    - 파일별 마지막 읽은 위치 자동 저장
    - 파일 재개시 마지막 읽은 위치로 이동할지 확인 대화상자 표시
    - 설정에서 위치 기억 기능 활성화/비활성화 옵션

### 3.4 북마크 및 주석
- **북마크 관리**:
    - 현재 페이지에 북마크 추가/제거
    - 북마크 목록 표시 및 북마크로 빠른 이동
    - 북마크에 제목 및 설명 추가 기능
- **하이라이트 및 주석**:
    - 텍스트 하이라이트 기능
    - 페이지에 주석 추가 기능
    - 주석 목록 보기 및 관리 기능

### 3.5 검색 기능
- **텍스트 검색**:
    - 현재 문서 내 텍스트 검색
    - 검색 결과 하이라이트
    - 검색 결과 간 이동 기능
- **고급 검색**:
    - 대소문자 구분 옵션
    - 정규식 지원
    - 검색 범위 지정 (전체 문서, 현재 페이지, 페이지 범위)

### 3.6 특수 PDF 파일 처리
- **확장자만 PDF인 파일 처리**:
    - PDF 파일 유효성 검증
    - 손상된 PDF 파일 감지 및 사용자 알림
    - 기본적인 PDF 복구 기능 제공
    - 유효하지 않은 PDF에 대한 상세 오류 정보 제공
- **이미지 형식 PDF 처리**:
    - 이미지 기반 PDF 자동 감지
    - 이미지 품질 향상 옵션 (대비, 선명도 조정)
    - OCR(광학 문자 인식) 기능 통합 (선택적)
    - 이미지 기반 PDF에 최적화된 렌더링 설정

## 4. 기술적 구현 요구사항

### 4.1 데이터 저장
- **SQLite 활용**:
    - 최근 파일 목록 저장
    - 파일별 북마크 및 주석 저장
    - 파일별 읽기 위치 및 설정 저장
    - 사용자 설정 저장

### 4.2 데이터베이스 스키마
```sql
-- 최근 파일 테이블
CREATE TABLE recent_files (
    id INTEGER PRIMARY KEY,
    file_path VARCHAR NOT NULL,
    file_name VARCHAR NOT NULL,
    last_opened TIMESTAMP NOT NULL,
    page_count INTEGER,
    last_page INTEGER DEFAULT 1,
    is_image_based BOOLEAN DEFAULT FALSE,
    pdf_validation_status VARCHAR,
    is_repaired BOOLEAN DEFAULT FALSE
);

-- 북마크 테이블
CREATE TABLE bookmarks (
    id INTEGER PRIMARY KEY,
    file_id INTEGER NOT NULL,
    page_number INTEGER NOT NULL,
    title VARCHAR,
    description VARCHAR,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (file_id) REFERENCES recent_files(id)
);

-- 주석 테이블
CREATE TABLE annotations (
    id INTEGER PRIMARY KEY,
    file_id INTEGER NOT NULL,
    page_number INTEGER NOT NULL,
    type VARCHAR NOT NULL, -- highlight, note, etc.
    content VARCHAR,
    x_position FLOAT,
    y_position FLOAT,
    width FLOAT,
    height FLOAT,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (file_id) REFERENCES recent_files(id)
);

-- 사용자 설정 테이블
CREATE TABLE user_settings (
    id INTEGER PRIMARY KEY,
    setting_key VARCHAR NOT NULL UNIQUE,
    setting_value VARCHAR NOT NULL
);

-- 사용자 폰트 테이블
CREATE TABLE custom_fonts (
    id INTEGER PRIMARY KEY,
    font_name VARCHAR NOT NULL,
    font_path VARCHAR NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- OCR 처리 정보 테이블
CREATE TABLE ocr_text_cache (
    id INTEGER PRIMARY KEY,
    file_id INTEGER NOT NULL,
    page_number INTEGER NOT NULL,
    ocr_text TEXT,
    last_updated TIMESTAMP NOT NULL,
    FOREIGN KEY (file_id) REFERENCES recent_files(id)
);

-- PDF 파일 복구 로그
CREATE TABLE pdf_repair_log (
    id INTEGER PRIMARY KEY,
    file_id INTEGER NOT NULL,
    original_path VARCHAR NOT NULL,
    repaired_path VARCHAR,
    repair_timestamp TIMESTAMP NOT NULL,
    repair_successful BOOLEAN,
    error_details VARCHAR,
    FOREIGN KEY (file_id) REFERENCES recent_files(id)
);
```

### 4.3 성능 최적화
- **페이지 렌더링 최적화**:
    - 현재 표시 중인 페이지만 완전히 렌더링
    - 이전/다음 페이지는 백그라운드에서 미리 로딩
    - 메모리 사용량 모니터링 및 관리
- **캐싱 전략**:
    - 최근 접근 페이지 캐싱
    - 사용자 패턴 분석을 통한 지능형 프리로딩
    - 메모리 사용량에 따른 캐시 크기 조절

### 4.4 멀티스레딩
- **UI 응답성 유지**:
    - 파일 로딩 및 렌더링을 별도 스레드에서 처리
    - JDK 21의 가상 스레드(Virtual Threads) 활용
    - 진행 상태 표시 및 작업 취소 옵션 제공
- **백그라운드 작업**:
    - 썸네일 생성
    - 검색 인덱싱
    - 메타데이터 추출

### 4.5 사용자 인터페이스
- **레이아웃**:
    - 메인 뷰어 영역
    - 사이드바 패널(북마크, 썸네일, 주석 등)
    - 도구 모음 및 상태 표시줄
- **접근성**:
    - 키보드 단축키 지원
    - 고대비 모드
    - 화면 읽기 프로그램 호환성

## 5. 사용자 인터페이스 설계

### 5.1 메인 창 레이아웃
```
+---------------------------------------------------------------+
|  파일  편집  보기  도구  도움말                               |
+---------------------------------------------------------------+
| [열기][저장] | [확대][축소] | [페이지: 1/42] | [검색...]      |
+---------------+-------------------------------------------+----+
| | 사이드바   ||                                          | |S||
| | - 썸네일   ||                                          | |c||
| | - 북마크   ||                                          | |r||
| | - 주석     ||          PDF 내용 표시 영역              | |o||
| | - 계층구조 ||                                          | |l||
| |            ||                                          | |l||
| |            ||                                          | |b||
| |            ||                                          | |a||
| |            ||                                          | |r||
+---------------+-------------------------------------------+----+
| 상태 표시줄: 파일명.pdf | 확대/축소: 100% | 페이지 모드: 단일 |
+---------------------------------------------------------------+
```

### 5.2 주요 대화상자
- **파일 열기 대화상자**: 시스템 파일 선택기 활용
- **설정 대화상자**: 탭 기반 설정 관리
- **북마크 관리 대화상자**: 북마크 추가/편집 인터페이스
- **최근 파일 대화상자**: 최근 파일 목록 및 관리 인터페이스
- **폰트 관리 대화상자**: 폰트 추가 및 설정 인터페이스

### 5.3 사용자 상호작용
- **마우스 제스처**:
    - 드래그: 페이지 내 이동
    - 더블 클릭: 텍스트 선택 또는 내용에 맞게 확대
    - 마우스 휠: 수직 스크롤 또는 확대/축소(Ctrl 키 조합)
- **키보드 단축키**:
    - Ctrl+O: 파일 열기
    - Ctrl+F: 검색
    - Ctrl+B: 북마크 추가/제거
    - 화살표 키: 페이지 이동 또는 스크롤
    - Ctrl+ / Ctrl-: 확대/축소
    - F11: 전체 화면 모드

### 5.4 특수 PDF 파일 처리 관련 UI

#### 5.4.1 확장자만 PDF인 파일 처리
```
+---------------------------------------------------------------+
|                     PDF 파일 오류 감지됨                       |
+---------------------------------------------------------------+
|                                                               |
|  선택한 파일에 문제가 감지되었습니다:                          |
|  "example.pdf"                                                |
|                                                               |
|  감지된 문제: 유효하지 않은 PDF 포맷 또는 손상된 파일          |
|                                                               |
|  [ ] 자동 복구 시도                                            |
|                                                               |
|  [복구 시도]     [다른 파일 선택]     [취소]                   |
+---------------------------------------------------------------+
```

#### 5.4.2 파일 복구 진행 상태
```
+---------------------------------------------------------------+
|                      PDF 파일 복구 중                          |
+---------------------------------------------------------------+
|                                                               |
|  파일 복구 진행 중입니다...                                    |
|                                                               |
|  [====================                 ] 45%                  |
|                                                               |
|  [취소]                                                        |
+---------------------------------------------------------------+
```

#### 5.4.3 이미지 기반 PDF 감지
```
+---------------------------------------------------------------+
|                    이미지 기반 PDF 감지됨                      |
+---------------------------------------------------------------+
|                                                               |
|  현재 문서는 텍스트 대신 이미지로 구성된 PDF입니다.            |
|  최적의 표시 및 기능을 위해 다음 옵션을 선택하세요:            |
|                                                               |
|  [ ] 이미지 품질 향상 적용 (대비 및 선명도 개선)               |
|  [ ] OCR 텍스트 인식 수행 (검색 기능 활성화)                   |
|  [ ] 항상 자동으로 적용                                        |
|                                                               |
|  [적용]     [기본 모드로 열기]     [취소]                      |
+---------------------------------------------------------------+
```

#### 5.4.4 이미지 향상 도구
```
+---------------------------------------------------------------+
|  파일  편집  보기  도구  도움말                               |
+---------------------------------------------------------------+
| [이미지 향상 도구]                                             |
+---------------------------------------------------------------+
|                                                               |
|   밝기:    [----|------X-----] +25                            |
|   대비:    [--------X---------] +5                            |
|   선명도:  [----------X-------] +10                           |
|   노이즈 제거: [----X-----------] -15                         |
|                                                               |
|   [ ] 모든 페이지에 적용                                       |
|                                                               |
|   [미리보기]  [적용]  [기본값 복원]  [닫기]                    |
+---------------------------------------------------------------+
```

## 6. 개발 가이드라인

### 6.1 특수 PDF 파일 처리 구현

#### 6.1.1 확장자만 PDF인 파일 처리
```java
/**
 * PDF 파일 유효성 검증
 */
public ValidationResult validatePdfFile(File file) {
    ValidationResult result = new ValidationResult();

    try (PDDocument document = PDDocument.load(file)) {
        // 기본 유효성 검사: PDDocument 로드 성공
        result.setValid(true);
        result.setMessage("유효한 PDF 파일입니다.");

        // 추가 검증 가능: 페이지 수, 버전 등 확인
        result.setPdfVersion(document.getVersion());
        result.setPageCount(document.getNumberOfPages());

    } catch (IOException e) {
        result.setValid(false);
        result.setMessage("PDF 형식이 아니거나 손상된 파일입니다.");
        result.setError(e);
        logger.warn("유효하지 않은 PDF 파일: {}", file.getPath(), e);
    }

    return result;
}

/**
 * 손상된 PDF 파일 복구 시도
 */
public RepairResult attemptPdfRepair(File corruptedFile) {
    RepairResult result = new RepairResult();
    result.setOriginalFile(corruptedFile);

    try {
        // 손상된 파일을 비트스트림으로 읽음
        PDDocument doc = PDDocument.load(corruptedFile, MemoryUsageSetting.setupTempFileOnly());

        // 임시 파일 생성
        File repairedFile = File.createTempFile("repaired_", ".pdf");

        // 새 문서로 저장 시도 (저장 과정에서 일부 구조 복구)
        doc.save(repairedFile);
        doc.close();

        result.setSuccess(true);
        result.setRepairedFile(repairedFile);
        result.setMessage("PDF 파일이 성공적으로 복구되었습니다.");

        // 로그 기록
        logger.info("PDF 파일 복구 성공: {} -> {}", corruptedFile.getPath(), repairedFile.getPath());

        return result;
    } catch (IOException e) {
        result.setSuccess(false);
        result.setMessage("PDF 파일 복구에 실패했습니다: " + e.getMessage());
        result.setError(e);

        logger.error("PDF 파일 복구 실패: {}", corruptedFile.getPath(), e);
        return result;
    }
}
```

#### 6.1.2 이미지 형식 PDF 처리
```java
/**
 * 이미지 기반 PDF 감지
 */
public boolean isImageBasedPdf(PDDocument document) {
    boolean containsText = false;

    PDFTextStripper stripper;
    try {
        stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(Math.min(5, document.getNumberOfPages())); // 처음 5페이지만 검사

        String text = stripper.getText(document);
        // 텍스트 양이 매우 적으면 이미지 기반 PDF로 간주
        containsText = text.trim().length() > 50;

    } catch (IOException e) {
        logger.error("PDF 텍스트 추출 오류", e);
    }

    return !containsText;
}

/**
 * 이미지 기반 PDF 렌더링 최적화
 */
public BufferedImage renderImageBasedPage(PDDocument document, int pageIndex) {
    try {
        PDFRenderer renderer = new PDFRenderer(document);
        // 이미지 기반 PDF의 경우 더 높은 해상도로 렌더링
        return renderer.renderImageWithDPI(pageIndex, 300); // 300 DPI로 렌더링
    } catch (IOException e) {
        logger.error("페이지 렌더링 오류: 페이지 {}", pageIndex, e);
        return null;
    }
}

/**
 * 이미지 향상 처리
 */
public BufferedImage enhanceImageQuality(BufferedImage original) {
    // 명암 개선
    RescaleOp rescaleOp = new RescaleOp(1.2f, 15.0f, null);
    BufferedImage enhanced = rescaleOp.filter(original, null);

    // 추가적인 이미지 향상 기법 적용 가능
    // - 노이즈 제거
    // - 이진화
    // - 기울기 보정 등

    return enhanced;
}

/**
 * OCR 처리 (Tesseract 라이브러리 필요)
 */
public String performOcrOnPage(BufferedImage pageImage) {
    // Tesseract OCR 인스턴스 설정
    ITesseract tesseract = new Tesseract();
    tesseract.setDatapath("/path/to/tessdata"); // 테서렉트 데이터 경로
    tesseract.setLanguage("kor+eng"); // 한국어+영어 인식

    try {
        return tesseract.doOCR(pageImage);
    } catch (TesseractException e) {
        logger.error("OCR 처리 오류", e);
        return "";
    }
}
```

#### 6.1.3 통합 서비스 구현
```java
@Service
public class PdfProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(PdfProcessingService.class);

    @Autowired
    private OcrTextCacheRepository ocrTextCacheRepository;

    @Autowired
    private PdfRepairLogRepository pdfRepairLogRepository;

    public OpenResult openPdfFile(File file) {
        OpenResult result = new OpenResult();

        // 1. 확장자 확인
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            result.setStatus(OpenStatus.INVALID_EXTENSION);
            result.setMessage("PDF 파일만 열 수 있습니다.");
            return result;
        }

        // 2. 유효성 검증
        try {
            PDDocument document = PDDocument.load(file);
            result.setDocument(document);

            // 3. 이미지 기반 PDF 감지
            boolean isImageBased = isImageBasedPdf(document);
            result.setImageBased(isImageBased);

            if (isImageBased) {
                // 이미지 기반 PDF 처리 설정 적용
                result.setStatus(OpenStatus.IMAGE_BASED_PDF);
                result.setMessage("이미지 기반 PDF가 감지되었습니다. 최적화된 설정을 적용합니다.");

                // OCR 필요 여부 확인 대화상자 표시 플래그 설정
                result.setShouldShowOcrDialog(true);
            } else {
                result.setStatus(OpenStatus.SUCCESS);
            }

        } catch (IOException e) {
            logger.error("PDF 파일 열기 오류: {}", file.getPath(), e);

            // 복구 시도
            RepairResult repairResult = attemptPdfRepair(file);
            if (repairResult.isSuccess()) {
                result.setStatus(OpenStatus.REPAIRED);
                result.setMessage("PDF 파일이 손상되었으나 복구되었습니다.");
                result.setRepairedFile(repairResult.getRepairedFile());

                // 복구 로그 저장
                savePdfRepairLog(file, repairResult);
            } else {
                result.setStatus(OpenStatus.INVALID_FORMAT);
                result.setMessage("유효하지 않은 PDF 파일입니다. 파일이 손상되었거나 PDF 형식이 아닙니다.");
            }
        }

        return result;
    }

    // 기타 메서드 구현...
}
```

### 6.2 코드 구조
- **패키지 구조**:
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

### 6.2 코딩 스타일
- **명명 규칙**:
    - 클래스: PascalCase (`PdfViewerFrame`, `BookmarkManager`)
    - 메서드 및 변수: camelCase (`openFile()`, `currentPageNumber`)
    - 상수: UPPER_SNAKE_CASE (`MAX_ZOOM_LEVEL`, `DEFAULT_PAGE_MODE`)
- **최신 Java 기능 활용**:
    - 람다 표현식 및 스트림 API
    - Optional 활용
    - 레코드 타입 (불변 데이터 구조)
    - 패턴 매칭 및 스위치 표현식
    - 가상 스레드 및 구조적 동시성

### 6.3 예외 처리 및 로깅
- **사용자 친화적 오류 메시지**: 기술적 상세 정보는 로그에만 기록
- **로그 레벨 적절히 활용**: ERROR, WARN, INFO, DEBUG, TRACE
- **예외 계층 구조**: 애플리케이션 특화 예외 클래스 설계 및 활용

## 7. 테스트 요구사항

### 7.1 테스트 유형
- **단위 테스트**: 개별 클래스 및 메서드 기능 테스트
- **통합 테스트**: 컴포넌트 간 상호작용 테스트
- **UI 테스트**: 사용자 인터페이스 동작 검증

### 7.2 테스트 범위
- **PDF 렌더링 테스트**: 다양한 유형의 PDF 파일에 대한 정확한
  렌더링 검증
- **성능 테스트**: 대용량 PDF 처리 성능 측정
- **메모리 누수 테스트**: 장시간 사용 시 메모리 관리 검증
- **멀티스레딩 테스트**: 동시성 이슈 검증

### 7.3 테스트 환경
- **운영체제**: macOS 12 (Monterey) 이상
- **하드웨어**: 다양한 CPU/메모리 구성에서 테스트
- **모니터**: 다양한 해상도 및 DPI 설정에서 테스트

## 8. 배포 및 유지보수

### 8.1 배포 패키지
- **macOS 애플리케이션 번들**: .app 형식 패키지
- **디스크 이미지 파일**: .dmg 배포 패키지
- **자동 업데이트 메커니즘**: 새 버전 확인 및 업데이트 기능

### 8.2 배포 프로세스
- **버전 관리**: 시맨틱 버저닝 (X.Y.Z)
- **릴리스 노트**: 각 버전별 변경 사항 문서화
- **자동화된 빌드**: CI/CD 파이프라인 구축

### 8.3 유지보수 계획
- **정기 업데이트**: 분기별 기능 업데이트
- **버그 수정**: 중요 버그는 즉시 패치 릴리스
- **피드백 시스템**: 사용자 피드백 및 오류 보고 메커니즘

## 9. 시스템 요구사항 (사용자 환경)

- **운영체제**: macOS 12 (Monterey) 이상
- **메모리**: 최소 8GB RAM, 권장 16GB 이상 (대용량 PDF 처리 시)
- **저장 공간**: 최소 500MB의 설치 공간
- **프로세서**: 64비트 멀티코어 프로세서 (Apple Silicon 또는 Intel)
- **Java 런타임**: 내장 JRE (Azul Zulu JDK 21 기반)로 패키징
