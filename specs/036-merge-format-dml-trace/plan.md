# 구현 계획: MERGE 정렬과 변경문 컬럼 추적

## 기술 맥락과 헌장

Kotlin 2.3.20·IntelliJ 2025.3.6.1·JBR 21 및 기존 SQL 토큰·범위 분석기·슬롯 기반 추적 그래프를 사용한다. 새 의존성·DB 호출·설정은 없다. 헌장은 미작성 템플릿으로 추가 규범은 없으며 Markdown은 한글로 작성한다.

## 설계

SqlFormatter.kt에서 MERGE 기준 폭을 5칸으로, SET만 6칸 기준으로 처리한다. MERGE와 INTO는 각 토큰 경계의 원래 줄바꿈을 유지하며 별도로 정렬한다. USING 서브쿼리는 MERGE 기준+4칸에 SELECT를, 닫는 괄호는 MERGE 기준에 둔다. ON 괄호의 본문은 4칸, INSERT·VALUES 목록은 기존 6칸을 사용한다. 모든 줄 생성은 기존 Writer.before를 거치며 새로운 줄바꿈을 만들지 않는다.

SqlQueryParser.kt의 기존 includeDml 경로에 쓰기 대상 소스·대상 토큰·표현식·INSERT 입력 쿼리를 추가한다. UPDATE SET와 MERGE 분기 대입, INSERT 컬럼과 VALUES 위치를 토큰/괄호 깊이에 따라 대응한다. INSERT SELECT는 기존 SELECT 범위를 별도로 유지하고 쓰기 범위와 입력 쿼리를 연결해 UNION 슬롯도 보존한다. 테이블 추적과 자동완성·검증기의 기존 기본 모드는 유지한다.

SqlColumnTrace.kt만 includeDml을 활성화한다. 기존 출력 그래프에 쓰기 대상 노드를 추가하고 우변 참조와 INSERT 입력 출력 슬롯을 연결한다. 상수·파라미터는 값 위치를 입력 노드로 제공한다. USING·INSERT SELECT 및 대입 스칼라 쿼리만 출력 범위로 인정하고 조건 서브쿼리는 제외한다. 대상 참조와 우변 참조는 별도 역할을 유지해 자기 대입 순환을 만들지 않는다. SELECT 없는 DELETE 참조는 독립 노드이며 값 전달 관계는 만들지 않는다. 기존 SqlTrace·SqlTraceGraph와 UI를 재사용한다.

## 검증과 순서

SqlFormatterTest.kt·SqlColumnTraceTest.kt에 사용자 예제와 UPDATE·INSERT·MERGE·DELETE·별칭·슬롯·모호함·한도 회귀를 먼저 작성하고 실행한다. 포매터 → 구조 파서 → 컬럼 그래프 순서로 구현하고 관련 SQL 분석 및 QuerySqlEditorTest를 실행한다. 임시 로컬 캐시 설정은 이전 검증과 동일하며 결과를 quickstart.md에 기록한다. 새 변경 파일에 대해 diff 검사를 수행한다.

후속 설계: FR-007에 따라 SqlFormattingToken.printed는 원문을 반환하고 heading·CASE·WITH 출력에서도 원문 토큰을 사용한다. 분석용 uppercase 판별은 유지한다. SqlFormatterTest와 실제 편집기 포맷 기대 중 이전 대문자 변환 계약만 갱신하고 토큰 원문 보존을 별도 검증한다.

## 최신 명확화에 따른 SELECT 전용 설계

앞선 쓰기 대상·대입 값 계보 설계는 사용자의 SELECT 추적 의도로 대체한다. SqlColumnTrace.kt에서 writeNodes·writes·knownWidth 및 쓰기 대상 출력 슬롯 연결을 제거한다. 변경문 문맥을 읽는 includeDml은 유지하여 SET·VALUES의 스칼라 SELECT와 FROM·USING 파생 SELECT에 기존 SELECT 그래프를 적용한다. SELECT 밖 참조 노드는 표시 오프셋 없이 출처를 선택하는 데에만 사용한다. DELETE 전용 표시 노드와 INSERT 익명 출력의 쓰기 슬롯용 보완도 제거한다.

SqlQueryParser.kt의 쓰기 표현식·대상·INSERT 입력 쿼리 연결 데이터는 더 이상 필요하지 않으므로 제거한다. 테이블 추적에 필요한 기존 writeColumns와 소스 분석·CASE 경계 보정은 유지한다. 회귀 테스트는 쓰기 대상 연결 기대를 SELECT 경로만 표시하는 기대와 대상 선택 빈 결과로 갱신한다. 포매터 변경은 그대로 유지한다.

## 최신 설계: 변경문 컬럼의 소스와 자기 참조

사용자 예제의 명확화로 앞선 SET·INSERT 대상 제외와 SELECT 밖 참조 숨김은 대체한다. SqlColumnTrace.kt는 선택한 변경문 참조에 표시 오프셋을 부여한다. 물리 소스가 변경문 범위에 속하면 source 노드에 해당 테이블 선언 토큰의 오프셋과 소스·컬럼 이름을 부여한다. CTE·파생 소스는 기존 SELECT 경로를 그대로 사용한다. 대입 반대편과 연결하는 쓰기 계보는 만들지 않는다.

INSERT 대상 목록·SET 좌변을 제외하는 조기 반환은 제거한다. 한정자 없는 대상 컬럼은 변경문 첫 대상 소스를 사용하며 일반 RHS 참조는 기존 스코프 해석을 따른다. 현재 선택한 참조는 selectedOffset으로 유지한다. SELECT 자체의 기존 출력 단계 규칙은 유지하여 일반 SELECT에 FROM 선언 줄을 일괄 추가하지 않는다.

## 전체 참조 및 SELECT 값 선택 설계

SqlColumnTrace.kt에서 현재 커서의 외부 참조만 늦게 만드는 대신 모든 변경문 컬럼 참조를 먼저 source 노드에 연결한다. 파서가 기록한 테이블명·별칭·CTE 선언 토큰은 컬럼 수집에서 제외한다. 일반 SELECT는 기존 출력·조건 격리 규칙을 유지하고, 변경문 참조는 동일 소스·동일 컬럼 키를 공유해 출처에서 사용처로도 탐색할 수 있게 한다.

커서 토큰이 식별자가 아니어도 SELECT 출력 expression에 포함된 값이면 해당 출력 슬롯을 seed로 사용한다. 이름 없는 임의 토큰·SELECT 밖 값·주석은 빈 결과로 유지한다. 별칭·표현식 값·입력 컬럼에서 시작한 전체 경로와 그래프 선택 ID를 검증한다.
