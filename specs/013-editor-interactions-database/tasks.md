# 작업 목록: 편집기 조작과 DB 연결 개선

## 1단계: 준비

- [x] T001 specs/013-editor-interactions-database/research.md에 드래그·괄호·Oracle·연결 테스트의 기존 구조와 결정을 기록한다.

## 2단계: 기반

- [x] T002 specs/013-editor-interactions-database/contracts/ui.md에 이벤트·결과·취소·비밀번호 계약을 정의한다.

## 3단계: 사용자 이야기 1

- [x] T003 [US1] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 임계치 미만·이상·줌·더블클릭·취소 회귀를 추가한다. FR-001~002, SC-001.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 후보와 활성 드래그를 구분한다. FR-001~002.

## 4단계: 사용자 이야기 2

- [x] T005 [US2] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 중첩·문자열·주석·불일치·갱신 회귀를 추가한다. FR-003, SC-002.
- [x] T006 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlBraceHighlighting.kt를 구현하고 QuerySqlEditor.kt에 연결한다. FR-003.

## 5단계: 사용자 이야기 3

- [x] T007 [US3] src/test/kotlin/com/look4counter/DatabaseMetadataTest.kt에 Oracle 일괄 조회·스키마 구분·정렬·취소·자원 해제 회귀를 추가한다. FR-004~005, SC-003.
- [x] T008 [US3] queryeditor/src/main/kotlin/com/look4counter/queryeditor/database/JdbcMetadataReader.kt에 Oracle 2회 조회를 구현한다. FR-004~005.

## 6단계: 사용자 이야기 4

- [x] T009 [US4] src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt와 DatabaseMetadataTest.kt에 미적용 입력·비밀번호·성공·실패·취소·접속만 확인 회귀를 추가한다. FR-006~007, SC-004.
- [x] T010 [US4] queryeditor/src/main/kotlin/com/look4counter/queryeditor/database/JdbcMetadataReader.kt의 연결 수명 관리를 재사용하여 연결 테스트를 구현한다. FR-006~007.
- [x] T011 [US4] queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/DataSourceSettingsPanel.kt와 GlueQueryEditorConfigurable.kt에 버튼·백그라운드 진행·결과를 연결한다. FR-006~007.

## 7단계: 검증

- [x] T012 specs/013-editor-interactions-database/quickstart.md에 관련 테스트 실행과 수렴 결과를 기록한다. SC-001~004.

## 의존성과 제공 전략

T001 → T002 이후 각 이야기에서 테스트 → 구현 순서로 수행한다. 사용자 이야기 4의 JDBC 공통화는 T008 완료 후 수행한다. 최소 제공 범위는 사용자 이야기 1이며 네 이야기 모두 완료한다. 별도 파일의 테스트 검토는 병행 가능하지만 동일 JDBC 파일 변경과 빌드는 순차 수행한다.
