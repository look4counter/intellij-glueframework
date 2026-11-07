# 구현 계획: SQL 포매팅 정렬 옵션

## 기술 맥락과 헌장

기존 Kotlin·Swing·IntelliJ PersistentStateComponent와 SqlFormatter를 사용한다. 새 외부 의존성은 없다. 헌장은 미작성 템플릿이므로 규범 검사를 생략하고 한글 문서·speckit 순서를 준수한다.

## 구현 단계와 파일

1. `queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/QueryFormattingSettings.kt`: 별도 프로젝트 서비스로 두 boolean을 기본 false로 저장한다. 기존 glue-query-editor.xml에 고유 상태 이름으로 저장하고 상태를 복사해 외부 변경을 방지한다. DataSource 상태와 수명·변경 알림은 분리한다.
2. `GlueQueryEditorConfigurable.kt`: 상단 포매팅 스타일 영역에 체크박스 두 개를 추가한다. 기존 reset·isModified·apply·dispose에 연결하며 DataSource·비밀번호 검증이 성공한 뒤 설정을 함께 적용한다.
3. `sql/SqlFormatter.kt`: 기본값이 해제인 옵션 인자를 추가한다. 첫 렌더링에서 구문 상태에 따라 정렬 그룹과 토큰 열을 수집하고 최대 열과의 차이를 계산한다. 두 번째 렌더링에서 토큰 앞 공백을 적용한다. 재렌더링으로 등호 뒤 서브쿼리의 여는 괄호가 이동하면 본문과 닫는 괄호도 함께 이동하도록 기존 들여쓰기 계산을 재사용한다. 옵션 해제 시 기존 한 번의 렌더링을 유지한다.
4. `QueryDesignEditor.kt`: 포매팅 명령 실행 시 저장 옵션을 읽어 전체·선택 포매팅에 전달한다.
5. `src/test/kotlin/com/look4counter/SqlFormatterTest.kt`, `DataSourceSettingsTest.kt`, `QuerySqlEditorTest.kt`: 결과·격리·보호·반복 실행·설정 저장/복원·검증 실패·실제 명령과 Undo/Redo를 검증한다. README와 quickstart에 사용법과 결과를 기록한다.

## 정렬 범위와 복잡성

SELECT·WHERE를 만날 때 새 그룹을 만들고 다른 절·문장에서는 종료한다. WHERE의 일반 괄호는 같은 그룹을 공유하되 서브쿼리는 독립 그룹을 사용한다. CASE 안의 비교식과 CAST AS는 제외한다. 같은 줄에서 여러 등호가 있어도 첫 단독 등호만 대상으로 한다. 문자열과 주석은 기존 토큰 보호를 따른다. 구문·토큰 제한과 취소 검사를 유지한다. 정렬 기준과 공백량은 그룹별 한 번씩 계산한다.

## 검증과 전달

집중 검증 후 전체 test·buildPlugin·verifyPluginStructure를 실행한다. 기존 IDE 소켓 제약 때문에 buildSearchableOptions를 제외하고 명시한다. 실제 사용자 IDE 수동 검증과 자동 테스트를 구분한다.

## IntelliJ 설정 화면 외형 보완

GlueQueryEditorConfigurable.kt에서 Swing 사각 TitledBorder를 제거한다. IntelliJ TitledSeparator를 영역 상단에 놓고 두 체크박스를 들여쓴 별도 패널에 배치한다. JBUI 간격·들여쓰기로 IDE 배율을 따르며 기본 컴포넌트의 테마 렌더링을 사용한다. 표시 변경에 대해 구현을 복제하는 새 테스트는 추가하지 않고 DataSourceSettingsTest의 기본값·저장·복원·검증 실패 회귀와 buildPlugin·verifyPluginStructure를 실행한다.
