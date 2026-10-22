# 기술 조사

- 결정: 루트는 플랫폼 플러그인, 하위는 모듈 플러그인을 사용한다. 이유: 하위에 배포 작업을 중복 생성하지 않고 대상 IDE를 상속한다.
- 결정: pluginComposedModule로 모듈을 루트 JAR에 합친다. 별도 플러그인 배포는 요구와 다르고 콘텐츠 모듈은 현재 규모에 불필요하다.
- 근거: [공식 다중 모듈 문서](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html#multi-module-project-structure), [공식 의존성 문서](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html).
- 결정: 모듈별 META-INF XML을 XInclude로 포함한다. 구현과 등록을 함께 관리하면서 플러그인 ID를 하나로 유지한다.
- 근거: [플러그인 설명자 문서](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html).
- 결정: 비바이너리 FileType으로 기본 텍스트 편집을 지원한다. SQL 언어 의존성과 그래픽 편집기는 현재 요구에 포함되지 않는다.
- 검증: 플랫폼 테스트에서 확장자 연결과 빈 파일 편집·저장을 확인한다.

## 멀티뷰와 XML 확장 조사

- 결정: IDE 기본 FileEditorProvider 멀티뷰를 사용한다. 각 모듈에서 Design을 먼저, Source를 다음에 배치한다. 직접 Swing 탭을 구현하면 IDE 편집기 선택과 상태 복원 처리를 중복하므로 제외했다.
- 결정: Source는 TextEditorProvider가 생성한 편집기를 위임하고 이름을 Source로 제공한다. 기본 편집기는 HIDE_DEFAULT_EDITOR로 제거한다. TextEditorProvider를 상속하면 기본 편집기 마커까지 상속되어 제거될 수 있으므로 직접 FileEditorProvider를 구현한다.
- 결정: XmlLikeFileType(XMLLanguage.INSTANCE)으로 기존 확장자와 XML PSI를 연결한다. 현재 대상 IDE의 javap로 API 생성자와 클래스 위치를 확인했다.
- 결정: 원래 TextEditor를 Disposer 자식으로 등록하고 상태 읽기·쓰기는 원래 제공자에 위임한다.
- 근거: [공식 파일 형식 등록](https://plugins.jetbrains.com/docs/intellij/registering-file-type.html), [공식 편집기 문서](https://plugins.jetbrains.com/docs/intellij/editors.html), [제공자 선택 공식 소스](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/openapi/fileEditor/impl/FileEditorProviderManagerImpl.kt).
- 대안: TextEditorWithPreview는 미리보기·분할 도구 모음까지 제공하므로 정확히 Design/Source 두 탭을 요청한 현재 요구에는 사용하지 않는다.

## 실제 테스트에서 확인한 사항

- 대상 IDE의 Bundled 테스트 프레임워크와 OpenTest4J 런타임을 사용하면 외부 Platform 프레임워크의 누락 의존성 없이 테스트를 실행할 수 있다.
- Kotlin의 인터페이스 위임은 상위 Java 인터페이스의 기본 메서드를 자동 위임하지 않았다. getState, 상태 복원 오버로드, 활성화·비활성화, 하이라이터·구조 뷰·위치·파일·탭 액션·로딩 상태를 명시적으로 원래 TextEditor에 위임한다.
- 경량 픽스처의 파일 열기 관리자는 실제 탭 UI를 구성하지 않으므로, 제공자 목록의 필터·정렬과 각 편집기의 활성화·비활성화 수명주기를 검증한다. 실제 하단 탭의 육안 확인은 빠른 검증 안내에 남긴다.
- Windows에서는 문서의 LF가 저장 파일의 CRLF로 변환될 수 있다. 저장 검증은 기존 줄바꿈 정책을 허용하면서 내용을 비교한다.

## 쿼리 Design 조사

- 결정: 기존 XML PSI와 공통 Document를 사용한다. 별도 XML 파서로 전체 문서를 재직렬화하는 대안은 주석·CDATA·공백 및 네임스페이스 보존에 불리해 제외한다.
- 결정: SQL 표시에는 XmlTag.value.textElements의 XmlText.value를 연결한다. value.text는 CDATA 마커와 entity 원문을 포함하고 trimmedText는 공백을 없애므로 사용하지 않는다. 대상 IDE의 XmlTagValueImpl 바이트코드로 확인했다.
- 결정: SQL 본문 범위만 Document.replaceString으로 CDATA로 교체한다. setText는 다른 자식을 삭제하므로 주석·하위 요소가 있는 본문은 Source 편집을 안내한다.
- 결정: EditorTextField를 다중 행·일반 텍스트 모드로 확장해 향후 SQL 문법을 추가할 기반으로 사용한다.
- 결정: Source 리스너와 입력 리스너에서는 수정 의도만 모은다. EDT에서 커밋 후 선택 태그만 쓰고 재진입 방지로 표시를 갱신한다.
- 근거: [편집기 컴포넌트](https://plugins.jetbrains.com/docs/intellij/editor-components.html), [문서 규칙](https://plugins.jetbrains.com/docs/intellij/documents.html), [PSI 수정 규칙](https://plugins.jetbrains.com/docs/intellij/modifying-psi.html), [스레드 규칙](https://plugins.jetbrains.com/docs/intellij/threading-model.html).

## 설명 편집 조사

- 결정: 기존 스마트 참조와 QueryEdit를 루트 desc에 한정해 재사용한다. 이유: 기존 충돌 검사·이스케이프·실행 취소를 유지한다. 별도 쓰기 경로는 중복되어 제외한다.
- 결정: 기존 GridBagLayout에 체크박스 열을 추가하고 안내가 없으면 상태 레이블을 숨긴다. 기존 코드와 테스트로 경로를 확인했으며 미확정 기술 사항은 없다.

## Java 클래스 자동완성 조사

- 결정: JavaReferenceEditorUtil.createDocument와 EditorTextField를 사용한다. 대상 IDE의 java-impl.jar를 javap로 확인했다. 클래스 참조 문서는 ClassNameReferenceEditor가 사용하는 클래스명 완성 표식을 동일하게 적용한다. 이유: 기존 Java 완성을 재사용하고 전체 클래스명을 삽입한다.
- 대안: 모든 클래스 이름을 미리 수집하는 Swing 팝업은 인덱스 조회·동명 구분·취소·성능 처리를 중복하므로 제외한다. 클래스 선택 버튼은 요청 범위를 넘어 추가하지 않는다.
- 근거: [공식 편집기 컴포넌트 문서](https://plugins.jetbrains.com/docs/intellij/editor-components.html). 대상 IDE 바이트코드에서 참조 문서 생성과 클래스명 표식을 확인했다.
- 결정: 루트와 queryeditor에 bundledPlugin("com.intellij.java")를 선언하고 설치 설명자에 com.intellij.java 의존성을 추가한다. Java 지원은 기능의 필수 전제다. modeleditor에는 직접 Java 컴파일 의존성을 추가하지 않는다.

## 파일 아이콘 조사

- 결정: 모듈별 resources/icons에 16×16 SVG를 추가하고 FileType.getIcon에서 IconLoader.getIcon으로 읽는다. 이유: 기존 파일 형식 등록을 유지하면서 IDE 배율에 맞게 표시하고 모듈별 리소스 소유권을 지킨다.
- 결정: 글자는 SVG 도형으로 그린다. 실행 환경 글꼴에 의존하는 SVG text와 래스터 이미지는 작은 아이콘의 일관성과 확대 품질 때문에 제외한다.
- 기존 SVG는 전체 플러그인 아이콘뿐이므로 파일 형식 아이콘은 별도 이름으로 추가한다. 기술적 미확정 사항은 없다.

## 입력 외형 통일 조사

- 결정: Java 참조 문서와 기존 EditorTextField를 유지하고 ID의 JBTextField를 스타일 기준으로 사용한다. 이유: 완성 엔진과 수정 연결을 유지하면서 테두리·높이·글꼴·배경을 맞춘다. 모든 입력을 Java 편집기로 바꾸거나 독자 완성 팝업을 구현하는 대안은 범위를 넓히므로 제외한다.
- 대상 IDE app.jar의 EditorTextField 바이트코드에서 setupBorder·createEditor·getPreferredSize·updateUI 확장점을 확인했다. 일반 입력의 테두리를 내부 편집기에 적용하고 테마 변경 시 스타일을 다시 읽는다. Java 문서의 검사 표시와 구문 색상은 일반 텍스트 표시로 대체하며 클래스 완성용 PSI는 보존한다.
- 라벨은 XML 키와 별도 표시 문자열로 명시한다. 체크박스 텍스트는 비우고 접근성 이름과 툴팁은 isNamed로 유지한다. 기술 미해결 사항은 없다.

## ResultType 실제 표시 보정 조사

- 원인: 일반 Swing 입력의 테두리를 내부 편집기 스크롤 영역에 복사하는 방식은 외부 입력 경계·내부 포커스·실제 텍스트 위치를 보장하지 않는다. 기존 테스트도 테두리 클래스와 높이만 비교했다.
- 결정: QueryResultTypeEditor가 외부 입력 테두리와 배경을 직접 그리고 내부 편집기의 테두리는 비운다. 자식 편집 컴포넌트는 줄 높이로 제한하여 세로 중앙에 배치한다. 테두리 굵기와 패딩은 배율을 반영하고 테마 색상을 사용한다.
- 결정: 실제 포커스를 받는 편집기 contentComponent의 이벤트로 외부 경계를 다시 그린다. 포커스 굵기는 내부 여백을 바꾸지 않는다. 기존 Java 문서와 완성 엔진은 재사용한다.
- 대안: 다시 테두리 클래스만 복사하거나 모든 입력을 교체하는 방식은 실제 표시를 보장하지 않거나 범위를 넓혀 제외한다. 대상 IDE의 EditorTextField 초기화 순서와 설정 제공자 호출 시점을 로컬 바이트코드에서 확인했다.

## 치수 조정 결정

기존 레이아웃에서 그리드 좌우 인셋의 합을 유지하며 좌우로 2픽셀씩 재분배하면 입력 너비와 다른 열 위치를 유지할 수 있다. 내부 패딩 1픽셀은 포커스 테두리 공간과 구별하여 글자 겹침을 방지한다. 기존 렌더링 경로를 재사용하며 미확정 기술 사항은 없다.

## 왼쪽 패딩 결정

결정: 기존 테두리 인셋 계산을 재사용하고 왼쪽 값만 5픽셀로 지정한다. 근거: 테두리 공간 2픽셀과 요청한 내부 패딩 3픽셀을 합산한다. 대안인 그리드 여백 변경은 내부 패딩을 바꾸지 않으므로 적용하지 않는다. 데이터 모델과 외부 계약 변경은 없다.

## 입력 외형 일치 결정

기준 입력의 기본 테두리와 선호·최소 높이를 재사용하고 ResultType 전용 그리드 오프셋을 제거한다. 별도 고정 치수를 다시 조정하는 대안은 테마별 불일치를 반복할 수 있어 채택하지 않았다. 내부 편집기의 글꼴 이름과 실제 크기도 기준 입력에서 가져온다. 데이터 모델·외부 계약 변경은 없다.

## 어두운 테마 실제 여백 결정

결정: 전용 JBTextField로 배경과 테두리를 그리고 modelToView2D로 실제 텍스트 시작 위치를 측정한다. 근거: Darcula에서 기준 입력 텍스트 시작 위치는 9픽셀인데 테두리 인셋은 3픽셀로, 테두리 값만 복사하면 6픽셀의 UI 내부 여백이 누락된다. 실제 텍스트 좌표 회귀 검증에서 기대값 9·실제값 3으로 실패를 재현한 뒤 수정했다. 고정 픽셀 추가 대안은 테마와 배율에 따라 오차가 발생하므로 채택하지 않았다. 일반 텍스트 하이라이터의 전경색도 기준 입력에서 직접 가져온다.

## 쓰기 안전 갱신 결정

결정: 스냅샷에서 강제 커밋을 제거하고 생명주기 갱신을 명시적 non-modal invokeLater로 예약한다. 근거: 편집기 생성은 EDT와 쓰기 의도 읽기 락 안에서도 쓰기 비안전 문맥일 수 있다. 조회에서 커밋을 실행하거나 생명주기 콜백을 WriteAction으로 감싸는 방식은 TransactionGuard 제약을 해결하지 않는다. 실제 오류를 TransactionGuardImpl.performActivity(false)와 미커밋 문서로 재현했다. 갱신 준비 단계에서만 커밋하여 Source 최신 값을 반영한다. 목록의 사용자 선택 이벤트는 기존 동기 편집 적용과 선택 유지를 보존한다.

## 저장 정리 설계 결정

결정: 파일 저장 직전의 FileDocumentManagerListener에서 현재 텍스트를 분리된 XML PSI로 분석하고 변경할 본문 범위만 뒤에서부터 치환한다. 근거: Source 전용 편집에도 적용할 수 있고 원본 PSI 강제 커밋 없이 중복 id·네임스페이스·태그 바깥 영역을 보존한다. Design 입력 단계에서 항상 trim하는 대안은 입력 중 필요한 공백까지 제거하므로 채택하지 않았다. 문서 전체를 다시 직렬화하는 대안은 불필요한 서식 변경을 만들므로 채택하지 않았다. 기존 CDATA 직렬화 함수를 재사용하고 쓰기 명령으로 실행 취소를 지원한다.

## 파일 열기 정리 결정

결정: 프로젝트 FileEditorManagerListener의 fileOpened에서 명시적 non-modal invokeLater로 공용 정리 함수를 호출한다. 근거: 실제 파일 열기 범위에 적용하고 쓰기 비안전 편집기 생성 문맥에서 문서를 수정하지 않는다. Source·Design 생성자에 각각 중복 구현하는 대안은 편집기 종류와 생성 순서에 의존하므로 채택하지 않았다. 문서 로드 전체를 대상으로 하는 대안은 파일을 화면에서 열지 않는 경우까지 변경하므로 채택하지 않았다. 디스크 저장은 기존 저장 리스너에 맡기고 열 때는 문서만 변경하여 실행 취소를 지원한다.

## SQL 키워드와 완성 설계 결정

출처는 DBeaver SQLConstants.java의 커밋 f2e25edd7d2eecd4d6541cdce70fdb25c7b07ea9다. SQL2003_RESERVED_KEYWORDS·SQL_EX_KEYWORDS·QUERY_KEYWORDS·DDL_KEYWORDS 배열의 문자열 상수를 해석하고 주석 항목을 제외한 뒤 중복 제거·정렬하여 367개를 추출했다. BasicSQLDialect의 공통 키워드 구성도 확인했다. DB별 키워드·함수·자료형 목록은 추가하지 않았다. Apache-2.0 전문과 저작권·추출 변경·출처 고지를 리소스에 포함한다.

원문: https://github.com/dbeaver/dbeaver/blob/f2e25edd7d2eecd4d6541cdce70fdb25c7b07ea9/plugins/org.jkiss.dbeaver.model/src/org/jkiss/dbeaver/model/sql/SQLConstants.java

공통 방언 확인: https://github.com/dbeaver/dbeaver/blob/devel/plugins/org.jkiss.dbeaver.model/src/org/jkiss/dbeaver/model/impl/sql/BasicSQLDialect.java

결정: 직접 토큰화하는 렉서와 IntelliJ LexerEditorHighlighter, TextCompletionProvider를 사용한다. 근거: 현재 포함된 플랫폼 API로 지정 색상과 키워드 완성을 제공하면서 SQL 플러그인이나 DBeaver 런타임·DB 연결을 요구하지 않는다. 별도 SQL 언어 파서·DB 플러그인을 추가하는 대안은 이번 어휘 강조·키워드 완성 범위보다 크므로 채택하지 않았다.

완성에는 PSI와 연결된 임시 일반 텍스트 문서가 필요하다. 이벤트 지원 PSI 파일을 만들어 문서를 얻고 완성 제공자를 설치하며 기존 버퍼 리스너와 원문 XML 동기화를 재사용한다. 원본 PSI를 강제 커밋하는 초기화 경로는 추가하지 않는다. 실제 IntelliJ 완성 기여자가 원본 PSI의 제공자를 사용하는 것을 로컬 플랫폼 API에서 확인했다.
## 프로젝트 설정과 보안 저장소 결정

사용자가 프로젝트별 관리를 선택했다. 프로젝트 서비스와 PersistentStateComponent로 메타데이터를 저장하고 PasswordSafe로 비밀번호를 분리한다. 보안 저장소는 UI 스레드 밖에서 호출해야 하므로 동기 진행 창의 배경 작업을 사용한다. 실제 저장소 대신 주입 가능한 메모리 CredentialStore로 테스트한다.

공식 근거: [설정 구현](https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html), [상태 영속화](https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html), [민감 정보 저장](https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html). 사용자 요청에 따라 GlueFramework를 최상위 설정으로 등록하고 프로젝트 전용 GlueQueryEditor를 하위에 연결한다. 실제 플랫폼 설정 트리 구성 테스트로 연결을 확인한다.

## JDBC 메타데이터와 드라이버 배포

결정: JDBC DatabaseMetaData.getTables/getColumns를 사용한다. 테이블 키는 catalog/schema/name이며 컬럼은 해당 테이블에 연결한다. 전체 이름 목록은 정확한 이름 기준 Distinct로 만든다. 후보 요청에서 연결하지 않고 명시적인 배경 새로고침 결과를 재사용한다.

근거: [JDBC 메타데이터 계약](https://docs.oracle.com/en/java/javase/26/docs/api/java.sql/java/sql/DatabaseMetaData.html). getColumns 입력은 패턴이므로 이름의 %와 _를 escape하고 결과 테이블 키도 비교한다. 벤더 전용 SQL은 채택하지 않는다.

드라이버는 로컬 JAR 우선이며 Oracle/MySQL/MariaDB/PostgreSQL은 Maven Central HTTPS의 고정 버전을 기본값으로 다운로드한다. [Oracle 배포 안내](https://www.oracle.com/database/technologies/maven-central-guide.html), [MySQL Maven 안내](https://dev.mysql.com/doc/connector-j/en/connector-j-installing-maven.html), [MariaDB 안내](https://mariadb.com/docs/connectors/mariadb-connector-j/about-mariadb-connector-j), [PostgreSQL 안내](https://jdbc.postgresql.org/download/)를 참고한다. Impala는 [공식 공급자 배포](https://www.cloudera.com/downloads.html)의 직접 다운로드 주소를 입력받아 같은 처리로 받는다. 배포물에 JDBC 드라이버를 동봉하거나 인증/배포 조건을 우회하지 않는다.

