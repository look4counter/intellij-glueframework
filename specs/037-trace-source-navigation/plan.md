# 구현 계획: 추적 출처 이동과 탐색 기록

작성일: 2026-11-10
명세: [spec.md](spec.md)

## 요약

기존 SQL 추적 그래프의 간선을 역방향으로 조회해 직전 출처 후보를 얻고 SQL 에디터 Ctrl+클릭에서 이동한다. TRACE 패널에 뒤로·앞으로 버튼을 두고 기존 트리 이동도 함께 기록한다.

## 기술 맥락

Kotlin 2.3.20, IntelliJ 2025.3.6.1, JBR 21 및 기존 Swing 패널·백그라운드 분석기를 사용한다. 새 의존성·DB 호출·영속 저장은 없다. 기록은 문서별 메모리에서 최대 100개다. 분석 한도와 취소는 기존 추적기를 따른다. 검증은 기존 JUnit·IntelliJ 실제 편집기 테스트 환경을 사용한다.

## 헌장 확인

헌장은 미작성 템플릿이므로 별도 규범은 없다. 한글 Markdown, speckit 단계 및 기존 작업 보존을 준수한다. 설계 후에도 위반은 없다.

## 구조와 설계

- SqlTrace.kt: predecessors 함수를 추가한다. 기존 테이블 우선 분석 결과의 선택 노드로 향하는 직접 부모를 반환한다. 테이블 한정자가 선언 노드로 매핑된 경우 선언을 먼저 반환한다. 자기 오프셋과 중복은 제외한다. 다른 연결 단계로 임의로 건너뛰지 않는다.
- QueryColumnTracePanel.kt: EditorMouseListener를 에디터 수명에 묶고 편집 영역의 Ctrl+왼쪽 클릭을 처리한다. 텍스트·수정 번호·에디터를 스냅샷으로 분석하고 늦은 응답을 차단한다. 단일 후보는 즉시, 복수 후보는 JBPopupFactory 선택 목록으로 이동한다.
- 같은 패널에서 문서별 History를 관리한다. 실제 이동 전 현재 위치와 도착 위치를 저장하고 뒤로·앞으로 버튼 상태를 갱신한다. 트리 탐색은 기존 트리 상태 유지 규칙을 따르며 Ctrl 이동과 기록 탐색은 TRACE를 새 위치에 맞게 갱신한다. 문서 수정 번호가 바뀌면 해당 기록을 지운다.
- 새 SqlTraceNavigationTest.kt에서 출처 해석을, 기존 QuerySqlEditorTest.kt에서 실제 마우스·버튼·트리·수정·전환을 검증한다. README.md에 사용법을 추가한다.

## 단계와 검증

출처 해석 회귀 → 출처 함수 → 클릭·기록 통합 회귀 → 패널 구현 → SQL 추적·편집기 회귀 → 수렴 순서로 실행한다. 로컬 Gradle과 캐시 JBR 21·기존 오프라인 초기화 설정으로 검증한다. 코드·문서 diff 공백 검사도 수행한다.

## 후속 설계

SqlTrace.kt에 토큰 범위와 출처 후보를 함께 반환하는 링크 조회를 제공한다. QueryColumnTracePanel.kt는 Ctrl 마우스 단축키를 로컬 DumbAwareAction으로 등록하고 기존 mousePressed 직접 이동을 제거한다. 전체 이벤트는 IDE 마우스 디스패처로 검증한다.

새 QueryTraceHover.kt는 에디터 수명의 마우스·키·포커스 리스너, 짧은 지연 및 백그라운드 링크 분석으로 HYPERLINK_ATTRIBUTES 밑줄·손 커서를 표시한다. 문서·수정 번호·요청 번호로 오래된 결과를 버리고 Ctrl 해제·이탈·편집·해제 시 지운다.

SqlColumnTrace.kt의 Node에 원래 SqlIdentifier 컬럼 이름을 보관하고 Node.add에서 같은 이름만 연결한다. 기존 표시 이름의 한정자는 비교에 사용하지 않는다. 이름이 달라지는 경계의 기존 테스트 기대를 최신 사용자 계약으로 갱신한다. 회색 강조 오프셋은 출력 슬롯·참조 중 연결된 컬럼만 수집한다. 조건절 참조도 별도로 해석해 연결된 동일 소스와 이름인 항목을 강조 목록에만 추가하고 트리 단계는 유지한다.

SqlTraceResult에 컬럼 강조용 토큰 범위를 추가한다. 패널은 현재 분석 결과가 유효할 때 범위별 회색 배경 하이라이터를 적용하고 무효화 시 즉시 지운다. 전용 TextAttributesKey와 밝고 어두운 테마의 회색 기본값으로 기존 구문 색상을 보존한다. 테이블 분석 결과의 강조 범위는 비어 있다.

기존 의존성·오프라인 검증 환경을 유지한다. 새 규범·헌장 위반은 없다. 회귀 작성 → 분석기·UI 구현 → 기존 기대 갱신 → 전체 SQL·편집기 회귀 → 수렴 순서다.

## 동명 테이블 후속 설계

SqlTableTrace.kt의 Node에 표시 이름과 분리한 테이블 식별자 경로를 보관한다. CTE 정의·참조는 CTE 이름, 물리 테이블은 스키마 포함 path, 파생 테이블은 별칭을 사용한다. Node.add는 두 경로 길이·각 식별자가 일치할 때만 간선을 만든다. 기존 소스 객체 식별과 CTE 정의 공유는 유지하여 셀프 조인·중첩 이름 가림·문장 격리를 지킨다. 기존 graph·entries 생성과 순환 한도는 유지한다.

현재 관련 노드 집합에 속하는 CTE 정의 이름, source.nameTokens·aliasToken과 qualifier로 같은 소스 객체에 해석되는 토큰의 범위를 SqlTraceResult.occurrences로 제공한다. 패널의 기존 회색 하이라이터·무효화·비동기 결과 검증을 재사용하며 새로운 표시 장치나 설정은 추가하지 않는다. Ctrl 링크는 필터링된 그래프를 그대로 사용하여 정의에서 다른 이름의 입력으로 이동하지 않는다.

SqlTableTraceTest.kt·SqlTraceNavigationTest.kt와 실제 QuerySqlEditorTest.kt의 이전 이름 변경 전달 기대를 최신 계약으로 갱신한다. 기존 UI 트리 접기·재귀·표시 한도는 새 동명 예제로 보존한다. 검증 순서는 단위·UI 회귀 작성 → 테이블 간선·범위 구현 → 집중 실행 → 전체 SQL 회귀 → 수렴이다. 새 의존성·DB 호출·헌장 위반은 없다.

## 탐색 버튼 제거 설계

QueryColumnTracePanel.kt의 NORTH 버튼 패널과 JButton 필드·상태 갱신을 제거한다. SQL 에디터 수명에 묶인 로컬 DumbAwareAction 두 개를 등록하여 IDE ACTION_GOTO_BACK·ACTION_GOTO_FORWARD의 활성 키맵 단축키를 사용한다. 액션 update에서 기록 인덱스로 사용 가능 여부를 계산하고 actionPerformed에서 기존 moveHistory를 실행한다. 기존 이동 기록 자료형과 이동 과정은 유지한다. QuerySqlEditorTest.kt의 버튼 조작을 실제 등록 액션 조작으로 바꾸고 화면에서 두 버튼이 없는지 검증한다.


## 2026-11-14 계획: IDE 탐색 단축키 점유 해제

현재 기준은 FR-021~FR-022이며 위의 전용 이력 설계를 대체한다. QueryColumnTracePanel.kt의 뒤로·앞으로 로컬 액션·키맵 참조와 사용처가 사라진 History를 제거한다. Ctrl+클릭 마우스 액션·비동기 검증·트리 이동·강조는 유지한다. QuerySqlEditorTest.kt의 이력 조작 검증을 액션·단축키 점유 부재와 기존 출처·트리 이동 검증으로 바꾼다. README.md와 탐색 계약을 갱신하고 기존 오프라인 Gradle로 QuerySqlEditorTest를 실행한다. Kotlin·IntelliJ·Swing 및 기존 테스트 환경을 재사용한다. 새 의존성·데이터는 없고 헌장은 미작성 템플릿이며 한글 문서·speckit 절차를 준수한다.
