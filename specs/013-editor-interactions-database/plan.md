# 구현 계획: 편집기 조작과 DB 연결 개선

**작성일**: 2026-10-27 | **명세**: [spec.md](spec.md)

## 요약과 기술 맥락

Kotlin·Swing·IntelliJ Platform 2025.3.6.1, JUnit 플랫폼 테스트 및 JDBC 기반이다. 영속 설정은 유지한다. 기존 사용자 변경은 보존하고 각 이야기의 경로만 수정한다.

## 헌법 검사

헌법은 미작성 템플릿이다. 한글 문서와 speckit 단계 준수 외 추가 제약은 없다. 설계 전후 위반 없음.

## 조사와 설계

### 사용자 이야기 1

modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 press는 후보만 준비한다. DragSource.getDragThreshold 기준의 화면 이동이 발생한 첫 drag에서 onMoveStarted를 호출한 뒤 기존 미리보기를 수행한다. 후보도 기존 isDragging에 포함하여 갱신·취소 보호를 유지한다. 임계치를 넘으면 되돌아와도 활성 상태를 유지한다. 해당 제스처의 click은 차단하고 다음 press에서 초기화한다. 팬·박스 선택·생성 모드는 기존 동작을 유지한다.

### 사용자 이야기 2

queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlBraceHighlighting.kt를 추가한다. 기존 SqlLexer의 주석·문자열 구분을 활용하여 (), [], {}를 스택으로 대응시킨다. caret/document listener에서 현재 커서 또는 바로 이전 위치에 있는 괄호만 강조한다. CodeInsightColors의 MATCHED_BRACE_ATTRIBUTES를 사용하고 에디터에 종속된 Disposable로 정리한다. QuerySqlEditor.kt에서 설치한다. 파일 타입이 PlainText이고 독립 SQL 렉서만 사용하므로 전역 SQL 언어나 상용 SQL 플러그인에 의존하지 않는다. 불일치 시 짝 강조는 하지 않는다.

### 사용자 이야기 3

queryeditor/src/main/kotlin/com/look4counter/queryeditor/database/JdbcMetadataReader.kt에서 접속 설정의 DatabaseKind를 사용하여 Oracle을 분기한다. ALL_TABLES와 ALL_VIEWS를 합친 객체 목록을 한 번, ALL_TAB_COLUMNS를 COLUMN_ID 순으로 한 번 조회하여 OWNER와 이름으로 연결한다. ResultSet·Statement와 연결은 use로 닫는다. 조회 타임아웃과 fetchSize를 적용하고 행 처리 중 취소를 확인한다. 범용 readConnection 경로와 다른 DB는 유지한다.

### 사용자 이야기 4

JdbcMetadataReader의 공통 드라이버 연결 수명 관리를 재사용하는 testConnection을 제공한다. 연결 성공 후 즉시 닫으며 메타데이터를 읽지 않는다. GlueQueryEditorConfigurable.kt에서 DataSourceSettingsPanel의 현재 선택 초안을 받아 취소 가능한 진행 작업으로 실행한다. 비밀번호는 백그라운드에서 읽고 새 입력·삭제 정책을 적용한다. 결과는 일반 성공/실패 문구만 표시하여 접속 예외 속 비밀번호 노출을 방지한다. 패널 생성 시 콜백을 주입해 UI와 접속을 분리한다. 별도 영속 데이터는 없다.

## 구조와 검증

기존 ModelDiagramTest.kt, QuerySqlEditorTest.kt, DatabaseMetadataTest.kt, DataSourceSettingsTest.kt에 회귀를 추가한다. FixtureJdbcDriver.java로 실제 격리 로더·연결 해제를 확인한다. 테스트를 먼저 작성한 후 구현하며 관련 테스트를 일괄 실행한다. 실 Oracle 서버와 사용자 IDE의 수동 검증은 환경이 제공되지 않아 자동 검증 결과와 구분한다.

