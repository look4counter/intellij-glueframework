# 구현 계획: SQL 컬럼 TRACE

## 기술 맥락과 헌장

기존 Kotlin·IntelliJ 플랫폼·Swing과 SqlQueryParser·SqlQueryScope를 재사용한다. 외부 의존성이나 JDBC 조회를 추가하지 않는다. 헌장은 미작성 템플릿으로 규범 검사를 생략한다. 한글 문서와 speckit 순서를 준수한다.

## 설계와 파일

1. `queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlColumnTrace.kt`: 출력 슬롯을 컬럼 이름·순서·별칭으로 매핑한다. CTE·파생·별표·표현식 참조를 재귀 순회하고 원본부터 오프셋을 수집한다. 모호한 출처와 재귀는 명시적으로 중단한다. UNION은 대응 출력 위치를 따라 분기를 수집한다.
2. `queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryColumnTracePanel.kt`: 제목 라벨 없이 상태·스크롤 목록을 제공한다. 커서·문서 리스너, 지연 갱신, 백그라운드 분석, 세대·문서·오프셋 검사로 늦은 결과를 폐기한다. 클릭·Enter 탐색에는 커서 재분석을 억제해 원래 경로를 보존한다.
3. `QueryDesignEditor.kt`: 우측 쿼리 목록 아래 TRACE 패널을 배치한다. `QuerySqlEditor` 설정 공급자로 생성·재생성된 편집기를 연결하고 쿼리 전환 시 무효화한다. 편집기와 리스너 수명은 기존 부모 Disposable을 따른다.
4. `src/test/kotlin/com/look4counter/SqlColumnTraceTest.kt`, `QuerySqlEditorTest.kt`: 사용자 예제, 별칭·명시 목록·별표·표현식·재귀·모호함·문장 격리와 UI 실제 클릭·Enter·갱신·비변경을 검증한다.
5. README와 quickstart에 사용·한계를 기록하고 전체 test·buildPlugin·verifyPluginStructure를 실행한다. 기존 IDE 소켓 제약으로 buildSearchableOptions를 제외하고 명시한다.

## 단계와 복잡성

먼저 순수 추적을 완성한 뒤 화면을 연결한다. 파서 제한을 공유하고 방문 집합·추적 단계 상한으로 순환과 과도한 확장을 막는다. 문서는 읽기 전용 스냅샷으로 분석하고 UI 적용 시 현재 문서와 요청 세대를 검사한다. 단위 검증과 통합 검증은 순차 실행한다.

## 전체 경로 보완 설계

SqlColumnTrace에서 현재 문장의 출력 슬롯·컬럼 참조·원본 관계를 연결한 그래프를 만든다. 화면 위치와 별개로 범위·슬롯별 노드를 구분하므로 같은 별표가 여러 컬럼을 전달해도 서로 섞이지 않는다. 선택한 컬럼 노드의 상위·하위 연결을 모두 탐색하고 의존 관계를 먼저 방문해 원본부터 출력한다. 같은 위치는 표시에서만 중복 제거한다. 순환과 모호함은 해당 연결에만 기록하고 무관한 컬럼의 오류는 결과에 전파하지 않는다. 기존 길이·토큰·깊이·단계 제한과 비동기 수명 보호를 유지한다.

SqlColumnTraceTest에 원본·중간·최종 위치의 동일한 전체 경로, 이름 변경, 분기와 별표 슬롯 격리를 추가한다. QuerySqlEditorTest는 원본 위치에서 최종 행까지 표시되는 동작을 확인한다. QueryColumnTracePanel에서 제목 JBLabel만 제거한다.

## 출력 전달 경로로 범위 제한

SqlColumnTrace에서 그래프 참조 수집과 표시를 SELECT 투영으로 제한한다. 제외 절의 컬럼을 커서로 선택하면 표시 위치가 없는 임시 참조로 대상을 해석하며 ORDER/GROUP 출력 별칭도 시작점으로만 해석한다. 구문 범위의 포함 관계를 통해 WHERE·ON·HAVING·GROUP·ORDER 아래의 하위 SELECT 및 WITH 정의도 제외한다. 일반 FROM/JOIN 파생 테이블과 CTE의 투영·표현식은 유지한다. 조건 서브쿼리 안에서 바깥 컬럼을 참조해도 출력 그래프에 유입되지 않도록 한다.

SqlColumnTraceTest의 기존 ON·ORDER 포함 기대값을 새 계약으로 바꾸고 각 제외 절·조건 서브쿼리·SELECT CASE 입력을 검증한다. QuerySqlEditorTest에서 SELECT와 조건절에 커서를 두더라도 목록은 같은 출력 단계만 보여주는지 검증한다. 기존 전체 경로·별표·이동·빈 상태 안내·수명 보호는 유지한다.

## 빈 상태 안내 단순화

QueryColumnTracePanel.kt에서 하단 JBLabel과 상태·툴팁 설정을 제거한다. JBList.emptyText를 생성 시 한 번만 고정 문구로 설정하고 위쪽 치우침을 해제해 세로 가운데로 배치한다. 초기화·분석 중·분석 완료 경로에서 빈 문구를 바꾸지 않는다. QuerySqlEditorTest.kt에서 하단 라벨 부재, 초기와 출처 불명 상태의 동일 문구, 가운데 배치 설정과 기존 경로 복구를 확인한다. 내부 SqlTraceResult.message는 변경하지 않는다.

## 항목 패딩 보완

QueryColumnTracePanel.kt의 JBList에 쿼리 목록과 같은 DefaultListCellRenderer를 설정한다. 기본 렌더링 후 기존 테두리 안쪽에 JBUI.Borders.empty(0, 8)을 합성해 선택·포커스와 IDE 배율을 보존한다. 새로운 구현 복제 테스트는 추가하지 않고 기존 QuerySqlEditorTest의 빈 상태·클릭·Enter·갱신 회귀와 ZIP·구조 검사로 검증한다.
