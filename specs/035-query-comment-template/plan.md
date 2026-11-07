# 구현 계획: 쿼리 주석 템플릿

**명세**: [spec.md](spec.md) | **작성일**: 2026-11-08

## 요약

프로젝트 서비스에 주석 원문을 저장하고 기존 설정 화면에 여러 줄 입력 및 변수 안내를 추가한다. 우측 패널의 별도 버튼이 최신 문맥을 치환하여 기존 QueryEdit 경로로 SQL을 변경한다.

## 기술 문맥

- 언어와 플랫폼: 기존 Kotlin/JVM 및 IntelliJ Platform 2025.3, Swing.
- 저장: 기존 glue-query-editor.xml에 별도 PersistentStateComponent 상태 저장.
- 검증: 기존 JUnit 및 IntelliJ 테스트 픽스처, Gradle 로컬 캐시.
- 제약: 네트워크·신규 의존성 없음. 날짜는 java.time, 문서 수정은 기존 쓰기 안전성 및 실행 취소 경로 사용.
- 규모와 성능: 프로젝트당 템플릿 하나, 클릭마다 템플릿을 한 번 순회한다.

## 헌장 확인

헌장은 미작성 템플릿이므로 규범 검사는 생략한다. 사용자 지시의 한글 문서 및 speckit 전체 단계는 준수한다. 설계 후 재검토 결과도 동일하다. 확장 훅은 없다.

## 구조

- queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/QueryCommentSettings.kt: 원문과 기본 예시의 저장.
- queryeditor/src/main/resources/META-INF/queryeditor.xml 및 messages/GlueFrameworkBundle.properties: GlueFramework·GlueQueryEditor 기본 설정 그룹의 설명과 하위 링크.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/GlueQueryToolConfigurable.kt: 포매팅·주석 입력 및 독립 수명주기.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/GlueQueryDatabaseConfigurable.kt: 데이터소스·연결·메타데이터 및 독립 수명주기.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryCommentTemplate.kt: 단일 패스 치환 및 머리말 결합.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt: 버튼 및 쓰기 안전한 삽입 동작.
- src/test/kotlin/com/look4counter/QueryCommentTemplateTest.kt: 치환 경계 검증.
- src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt: 저장·초기화 및 직렬화 검증.
- src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt: 배치·최신 편집·Source 동기화·실행 취소·편집 불가 검증.

## 설계 결정

설정은 기존 포맷 상태와 분리해 기존 생성자와 저장 호환성을 유지한다. 주석은 목록 구조 변경이 아니므로 QueryListAction에는 추가하지 않는다. formatSql과 같은 지연 쓰기 안전성, 선택 리비전 확인, pending 반영 및 최신 snapshot 검사를 사용한다. 치환된 값은 다시 처리하지 않는다. 알려진 변수 외에는 날짜 문자와 구분자로만 구성된 유효한 형식만 치환하고 나머지는 보존한다.

## 단계

1. 저장 상태 및 순수 치환 함수를 구현한다.
2. 사용자 이야기 1: 설정 UI를 연결한다.
3. 사용자 이야기 2: 버튼과 문서 변경을 연결한다.
4. 집중 검증과 관련 회귀를 실행하고 수렴 검사를 수행한다.

## 후속 안내 문구 변경 계획

GlueQueryEditorConfigurable.kt의 안내 라벨을 FR-007의 지정 문자열 한 줄로 교체한다. 단순 표시 문구 변경이므로 소스 문자열 대조와 차이 검사를 수행하며 새 테스트는 작성하지 않는다. 명확화 검토 결과 추가 질문은 없다.

## 설정 하위 항목 분리 계획

GlueQueryEditorConfigurable.kt는 상위 안내 화면으로 바꾸고 GlueQueryToolConfigurable.kt와 GlueQueryDatabaseConfigurable.kt를 같은 settings 디렉터리에 추가한다. queryeditor/src/main/resources/META-INF/queryeditor.xml에서 기존 상위 ID 아래에 .tool, .database ID의 프로젝트 설정을 등록한다. 기존 설정 서비스를 그대로 사용해 저장 호환성을 유지한다. Database는 기존 비밀번호·접속·메타데이터 수명주기를 가져오고 Tool은 포매팅·주석 UI 및 수명주기를 가져온다. GlueFrameworkConfigurable.kt의 안내 경로도 갱신한다.

DataSourceSettingsTest.kt는 기존 접속·비밀번호·저장 검증을 Database로 옮기고 Tool의 설정 검증 및 실제 Settings 트리와 페이지 간 상태 격리 검증을 수행한다. src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt의 메타데이터 도구 검증도 Database 페이지를 사용하도록 갱신한다. 새 의존성은 없으며 헌장 검사와 훅 결과는 이전과 같다.

## GlueFramework 기본 그룹 화면 계획

로컬 IntelliJ Platform의 groupConfigurable, ConfigurableGroupEP 및 ConfigurableEditor 기본 화면을 확인했다. groupConfigurable은 설명과 실제 하위 설정 목록을 받아 기본 배치의 링크 화면을 생성한다. 별도 링크 구현 대신 이 기능을 사용한다.

- queryeditor/src/main/resources/META-INF/queryeditor.xml의 applicationConfigurable을 groupConfigurable로 교체하고 GlueQueryEditor를 동일 그룹 ID의 groupId에 연결한다.
- modeleditor/src/main/resources/META-INF/modeleditor.xml의 GlueModelEditor도 같은 groupId에 연결한다. 두 하위 설정의 ID와 기존 자식 계층 및 저장 서비스는 유지한다.
- queryeditor/src/main/resources/messages/GlueFrameworkBundle.properties에 이름과 상단 설명을 정의한다.
- 사용하지 않는 queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/GlueFrameworkConfigurable.kt를 제거한다.
- src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt에서 실제 플랫폼 그룹·설명·기본 화면 위임·하위 항목 등록을 확인한다. 기본 링크 생성과 이동은 플랫폼 구현을 그대로 사용한다.

기존 명세 품질 점검은 통과하며 헌장은 미작성 템플릿, 확장 훅은 없다. 구현 전 요구사항 FR-010과 아래 작업의 대응 및 기존 설정 호환성을 검사한다.

## 기본 템플릿 및 변수 설명 변경 계획

QueryCommentSettings.kt의 기본 예시에서 QUERY ID를 QueryId로 바꾼다. GlueQueryToolConfigurable.kt의 하단 안내는 HTML 줄바꿈을 이용해 쿼리 ID·설명·현재 날짜와 시간 변수의 의미를 표시한다. 쿼리 설명은 실제 지원하는 QUERY_DESCRIPTION으로 안내한다. 기존 저장 상태와 치환 동작은 유지한다. 단순 기본 문자열과 표시 문구 변경이므로 새 테스트 없이 문자열 대조와 차이 검사를 수행한다.

## 쿼리 설정 그룹 및 ResultType 변수 계획

- queryeditor/src/main/resources/META-INF/queryeditor.xml에서 GlueQueryEditor를 기존 GlueFramework 기본 그룹 아래의 groupConfigurable로 등록하고 Tool·Database는 groupId로 연결한다. 기존 두 페이지 ID는 유지한다.
- messages/GlueFrameworkBundle.properties에 쿼리 설정 그룹 이름과 설명을 추가하고 사용하지 않는 settings/GlueQueryEditorConfigurable.kt를 제거한다.
- settings/QueryCommentSettings.kt의 기본 설명 변수를 달러 표기로 바꾸고 settings/GlueQueryToolConfigurable.kt의 안내도 달러 변수 네 개와 의미로 통일한다.
- QueryCommentTemplate.kt의 prepend에 기본값이 빈 문자열인 resultType 인자를 추가한다. 기존 now 위치 인자 호출의 호환성을 위해 뒤에 추가한다. QueryDesignEditor.kt는 최신 snapshot의 resultType을 이름 인자로 전달한다.
- src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt는 중첩 기본 그룹·설명·기본 화면 위임·하위 설정과 예시를 검증한다. QueryCommentTemplateTest.kt는 ResultType의 빈값·반복·치환값 재해석 방지를 검증하고 QueryDesignEditorTest.kt는 편집 직후 ResultType 반영 및 Source 동기화를 검증한다.

명확화 질문은 없고 헌장은 미작성 템플릿이다. 확장 훅은 없으며 기존 저장값과 샤프 문법 호환성은 유지한다.
