# 작업 목록: 전체 Datasource 자동 조회

## 1단계: 준비

- [x] T001 specs/002-all-datasource-metadata/spec.md의 명세와 설계 일관성을 검증한다.

## 2단계: 공통 기반

- [x] T002 queryeditor/src/main/kotlin/com/look4counter/queryeditor/database/DatabaseMetadataService.kt에 전체 요청 세대 및 DB별 캐시 모델을 구현한다. (FR-001, FR-006)

## 3단계: 사용자 이야기 1

- [x] T003 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/database/QueryDataSourceToolbar.kt의 선택 UI를 제거하고 전체 상태·새로고침을 연결한다. 자동완성 공급자의 조회를 전체 스냅샷으로 변경한다. (FR-001, FR-002)

독립 검증: 두 DB의 고유 이름 후보 및 선택 컨트롤 부재를 확인한다.

## 4단계: 사용자 이야기 2

- [x] T004 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/database/DatabaseMetadataService.kt에 백그라운드 전체 조회, 설정 변경, 실패 격리와 취소를 구현한다. (FR-003~FR-006)
- [x] T005 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/database/DatabaseMetadataStartupActivity.kt와 queryeditor/src/main/resources/META-INF/queryeditor.xml에 시작 조회를 연결한다. (FR-003)

독립 검증: 에디터 없이 시작 조회하며 한 DB 실패에도 다른 결과를 제공한다.

## 5단계: 검증과 마무리

- [x] T006 src/test/kotlin/com/look4counter/DatabaseMetadataTest.kt와 QuerySqlEditorTest.kt를 전체 조회 동작에 맞게 갱신하고 회귀 검증한다.
- [x] T007 specs/002-all-datasource-metadata/quickstart.md의 검증을 수행하고 converge로 잔여 작업을 점검한다.

## 의존성과 병렬 실행

T001 → T002 → T003 → T004 → T005 → T006 → T007 순서로 진행한다. US1과 US2는 같은 서비스에 의존하므로 순차 실행한다. 문서 검토와 독립 테스트 결과 검토는 병렬 처리할 수 있다. 최소 제공 범위는 US1이며 최종 제공에는 두 이야기 모두 포함한다.

## 6단계: 새로고침을 설정 화면으로 이동

- [x] T008 [US1] QueryDesignEditor.kt에서 도구 모음을 제거하고 database/QueryDataSourceToolbar.kt를 settings/DataSourceMetadataToolbar.kt로 이동한다. (FR-002)
- [x] T009 [US2] settings/GlueQueryEditorConfigurable.kt에 전체 새로고침과 상태를 연결하고 UI 리스너 수명을 관리한다. (FR-004, FR-005)
- [x] T010 src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt의 조회 테스트를 설정 화면 기준으로 갱신하고 시작 조회 및 설정 회귀 테스트를 실행한다.

T008 → T009 → T010 순서로 진행한다. 설정에서 저장된 DB를 조회하며 시작 조회는 유지한다.
