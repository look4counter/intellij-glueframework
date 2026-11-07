# 구현 계획: SQL 테이블 추적

작성일: 2026-11-08
대상: [기능 명세](spec.md)

## 요약

기존 구조 파서에 이름 위치를 보존하고 테이블 관계 그래프를 추가한다. 통합 진입점에서 테이블 토큰이면 테이블 경로를, 나머지는 기존 컬럼 경로를 반환한다. 기존 목록과 비동기 수명 관리를 그대로 사용한다.

## 기술 맥락

Kotlin·IntelliJ 플랫폼·Swing·기존 Gradle 및 JBR 캐시를 사용한다. 새 의존성·설정·저장 형식·JDBC 요청은 없다. 기존 파서의 길이 262144·토큰 20000·깊이 48 제한을 유지하고 테이블 그래프도 취소 확인과 작업 수 제한을 적용한다. 분석은 백그라운드에서 실행하며 SQL은 읽기 전용이다.

## 헌장 확인

헌장은 미작성 템플릿이므로 규범 검사를 생략한다. 한글 문서와 전체 speckit 단계를 준수한다. 설계 전후에 추가 충돌은 없고 확장 훅은 없다.

## 구조와 접점

- `queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlQueryParser.kt`: CTE 이름 토큰·소스 경로 토큰·별칭 토큰·시작 위치 보존. 기존 이름 해석과 파싱 구조는 유지한다.
- 같은 디렉터리의 `SqlTableTrace.kt`: CTE·파생 테이블 정의와 소스 인스턴스를 객체 식별자로 구분하는 방향 그래프. 선택한 노드의 원본을 구한 뒤 그 원본들의 모든 소비자를 순회한다. 후속 소비자의 다른 입력을 원본으로 추가하지 않는다. 순환은 방문 집합으로 끊고 출처 우선 정렬 및 줄 중복 제거를 수행한다.
- 같은 디렉터리의 `SqlTrace.kt`: 테이블 선택 우선·컬럼 분석 대체 진입점. 기존 `SqlTraceResult`·`SqlTraceEntry`를 재사용한다.
- `queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryColumnTracePanel.kt`: 통합 진입점과 문구·접근성 이름 변경. 기존 generation·문서 stamp·커서·dispose 보호, 클릭·Enter 이동을 유지한다.
- `src/test/kotlin/com/look4counter/SqlTableTraceTest.kt`: 실제 SQL의 관계·격리·제한을 검증한다.
- `src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt`: 실제 편집기에서 컬럼↔테이블 전환·탐색·갱신·비변경을 검증한다.
- `README.md`와 본 기능 문서: 사용법과 검증 결과를 기록한다.

## 단계

1. 연구와 개체·UI 계약을 작성하고 중요한 불명확성이 없는지 재확인한다.
2. 관계 선택 및 추적의 의미 있는 회귀를 먼저 작성하고 파서 위치·분석기·통합 진입점을 구현한다.
3. 실제 목록을 통합하고 기존 컬럼·완성·오류 회귀를 포함해 검증한다.
4. 전체 테스트·ZIP·구조 검사를 실행한다. 기존 IDE 소켓 제약으로 검색 옵션 재생성만 제외하고 기록한다.
5. 명세·계획·작업과 구현을 수렴 검토한다.

## 복잡도와 대안

단순 문자열 검색은 이름 가림과 별칭을 구분하지 못해 제외했다. 기존 컬럼 그래프를 테이블 그래프로 재사용하면 메타데이터 없는 별표를 놓치므로 별도 분석기를 사용한다. CTE 이름이 아닌 실제 파서 객체로 연결해 동일 이름과 자기 JOIN을 격리한다. 분석 상태는 요청마다 새로 생성하고 저장하지 않는다.

## 후속 선택 동기화 및 마우스 누름 계획

- SqlColumnTrace.kt의 SqlTraceResult에 선택 단계의 SQL 위치인 selectedOffset을 선택 필드로 추가한다. 컬럼은 선택 출력 단계의 위치, 표시 단계가 없는 조건은 원래 토큰 위치를 전달한다.
- SqlTableTrace.kt는 경로 분석의 시작 노드와 화면 선택 위치를 구분한다. FROM 참조 및 한정자는 소스 참조 위치, CTE 선언은 선언 위치를 selectedOffset으로 반환한다. 기존 경로 생성은 유지한다.
- QueryColumnTracePanel.kt는 최신 비동기 결과를 반영한 후 selectedOffset과 동일 위치의 항목을 선택한다. 정확한 위치가 없으면 해당 줄을 사용하고 대응 행이 없으면 선택을 비운다. ensureIndexIsVisible로 선택 행을 표시한다.
- 목록 이동은 mouseClicked 대신 왼쪽 mousePressed에서 수행한다. 기존 셀 영역·문서 stamp·수명 보호와 Enter 동작을 유지하며 이동 시 목록 선택도 명시적으로 맞춘다.
- SqlTableTraceTest.kt와 SqlColumnTraceTest.kt에서 선택 위치를 검증하고 QuerySqlEditorTest.kt에서 실제 커서 동기화·한정자 대응·누름 후 미세 이동·빈 영역·우클릭·Source 비변경을 검증한다. 관련 테스트와 패키지·구조 검사를 수행한다.

새 의존성이나 저장 상태는 없다. 기존 명세 품질 점검은 통과하고 헌장 및 훅 조건은 이전과 동일하다.

## 표준 트리 및 실제 경로 보존 계획

- sql/SqlTraceGraph.kt에 SQL 토큰 위치·표시 이름·직접 소비자 ID와 루트·선택 ID를 가진 불변 그래프를 추가한다. 표시 없는 중간 노드만 연결을 축약하고 같은 줄·같은 위치라도 노드 식별자는 합치지 않는다. 원래 분석기의 관계만 재사용한다.
- SqlColumnTrace.kt와 SqlTableTrace.kt는 기존 평탄 entries를 분석 회귀 호환용으로 유지하며 실제 UI용 그래프를 추가로 반환한다. 컬럼 출력 슬롯과 원본 관계의 객체 식별자를 유지한다. 테이블은 전체 이름·별칭 및 CTE 정의를, 컬럼은 출력 이름과 별표의 출처를 구분한다.
- QueryColumnTracePanel.kt는 com.intellij.ui.treeStructure.Tree·DefaultTreeModel·표준 셀 렌더러를 사용한다. 그래프를 원본→소비자 방향으로 나타내고 공유 정점은 각 경로에서 별개 트리 항목으로 만든다. 순환은 현재 경로의 재방문에서 표식을 붙이고 더 펼치지 않는다.
- UI 노드는 최대 2048개, 경로 깊이는 최대 96으로 제한하고 한도 표식을 표시한다. 그래프 변환에도 기존 분석 한도와 취소 확인을 적용한다.
- 기본 화살표·좌우 키·더블 클릭의 접기·펼치기는 플랫폼에 맡긴다. 본문 누름은 정확한 path bounds 안에서만 탐색하고 화살표·빈 영역은 제외한다. 마우스 탐색은 트리의 포커스를 유지하며 Enter는 편집기를 활성화한다.
- 문서와 stamp가 같은 동안 정점 위치·이름으로 구성된 경로 키를 사용해 접힘 상태를 보존한다. 현재 선택 노드가 숨겨졌다면 조상만 펼친다. 문서 편집·쿼리 전환은 상태를 초기화한다. 기존 비동기 세대·stamp·해제 보호는 유지한다.
- src/test/kotlin/com/look4counter/SqlTraceGraphTest.kt와 기존 SqlColumnTraceTest.kt·SqlTableTraceTest.kt는 실제 연결·분기·재합류·한 줄 다중 객체·별표·순환·격리를 검증한다. QuerySqlEditorTest.kt는 실제 IntelliJ Tree, 화살표·키보드·선택·상태 보존·탐색·무수정을 검증한다.

기존 분석과 UI 접점에 한정하며 새 의존성·DB 요청·파일 형식 변경은 없다. .specify 헌장은 미작성 템플릿이므로 규범 검사는 생략하고 확장 훅은 없다.
