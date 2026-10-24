# 구현 계획: 전체 Datasource 자동 조회

작성일: 2026-10-23 | 명세: [spec.md](spec.md)

## 요약

프로젝트 서비스가 모든 Datasource 조회와 캐시를 소유한다. 프로젝트 시작 확장에서 백그라운드 조회를 실행한다. 전체 새로고침과 조회 상태는 Datasource 설정 화면에만 표시한다.

## 기술 문맥

Kotlin/JVM, IntelliJ 2025.3, JDBC, 기존 PasswordSafe를 사용한다. 영속 설정 형식은 유지하고 선택 값은 무시한다. 기존 JDBC 타임아웃과 자원 정리를 재사용한다. 조회는 UI 스레드 밖에서 실행하며 프로젝트별 모든 등록 DB가 범위다. Gradle 플랫폼 테스트로 검증한다.

## 헌법 확인

헌법은 미작성 템플릿으로 구체적 검사를 생략한다. 한글 문서와 speckit 전체 단계를 준수한다. 설계 후 위반 없음.

## 설계와 파일 구조

- queryeditor/src/main/kotlin/com/look4counter/queryeditor/database/DatabaseMetadataService.kt: 전체 조회 세대, DB별 캐시, 로딩·실패 상태와 취소를 소유한다. 설정 변경 시 재조회한다. 리스너는 잠금 밖에서 호출한다.
- 같은 디렉터리의 DatabaseMetadataStartupActivity.kt: 프로젝트 시작 시 refreshAll을 호출한다.
- settings/DataSourceMetadataToolbar.kt로 도구 모음을 옮기고 GlueQueryEditorConfigurable.kt에서 연결한다. 설정을 닫을 때 UI 리스너만 해제한다. QueryDesignEditor.kt에서 상단 도구 모음을 제거한다. 저장된 설정으로 조회하며 미적용 변경은 적용 후 조회하도록 안내한다.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlKeywordCompletionProvider.kt: 전체 스냅샷의 중복 없는 이름 후보를 사용한다.
- queryeditor/src/main/resources/META-INF/queryeditor.xml: 시작 확장을 등록한다.
- src/test/kotlin/com/look4counter/DatabaseMetadataTest.kt 및 QuerySqlEditorTest.kt: 다중 DB, 실패, 오래된 요청, UI 회귀를 검증한다.

## 구현 단계

전체 캐시 모델 → 백그라운드 조회 및 시작 연결 → UI와 자동완성 → 검증 순서로 진행한다. 새 의존성은 필요 없다.
