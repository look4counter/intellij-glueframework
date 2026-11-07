# 검증 안내

Query Design에서 사용자 예제의 원본 SS·중간 SELECT *·마지막 SS에 각각 커서를 놓고 우측 하단 목록이 항상 2·6·9행 전체를 원본 순서로 표시하는지 확인한다. TRACE 제목 라벨은 표시하지 않는다. 항목 클릭과 Enter로 해당 줄에 이동하고 원래 목록이 유지되는지 확인한다. 다른 컬럼·다른 쿼리로 이동하거나 SQL을 수정해 목록이 갱신되는지 확인한다.

자동 검증은 SqlColumnTraceTest와 QuerySqlEditorTest를 먼저 실행하고 전체 테스트·설치 ZIP·구조 검사를 수행한다. 기존 로컬 JBR·Gradle 캐시를 사용한다. IDE 소켓 제약 때문에 검색 옵션 생성은 제외하며 실제 사용자 IDE 확인과 자동 테스트 결과는 구분한다.

## 자동 검증 결과: 2026-11-07

- 분석기 11개와 편집기 테스트를 포함한 전체 477개가 통과했다. 실패·오류·건너뜀은 각각 0개다.
- 사용자 예제의 2·6·9행, 각 줄의 내용, 실제 컬럼 오프셋을 검증했다. 명시 컬럼 목록·파생 별칭·한정 별표·출력 별칭 선택·표현식·UNION·재귀·모호함·문장 격리·주석·문자열·길이 제한·CRLF를 포함한다.
- 실제 편집기 테스트에서 클릭과 Enter로 커서를 옮기고 TRACE 경로가 유지되며 SQL·Source가 변경되지 않는 것을 확인했다. 일반 커서 이동, SQL 수정 직후 목록 무효화, 수정 후 재분석과 쿼리 전환도 통과했다.
- 최초 UI 컴파일에서 Swing의 기존 메서드와 겹친 내부 이름을 수정한 뒤 집중 검증과 전체 검증을 통과했다. 테스트의 쿼리 목록 탐색은 새 TRACE 목록과 구분하도록 컴포넌트 이름으로 대상을 지정했다.
- `test buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. 기존 IDE 기동 소켓 권한 제약으로 검색 옵션 재생성만 제외했다. 실제 사용자 IDE의 수동 확인과 실제 DB 접속은 수행하지 않았다.
- 근거: `.gradle-validation/column-trace-analyzer.log`, `column-trace-focused.log`, `column-trace-final.log`, `build/reports/column-trace-test-summary.json`. 설치 파일은 `build/distributions/intellij-glueframework-1.0.3.zip`이며 SHA-256 파일을 함께 갱신했다.

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-validation'
$env:JAVA_HOME = Join-Path $PWD '.gradle-validation/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr'
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' test buildPlugin verifyPluginStructure -x buildSearchableOptions
```

## 수렴 검토

기능 요구사항 7개, 성공 기준 3개, 사용자 이야기 2개와 계획의 구현 단계 5개를 현재 추적기·화면·검증 결과에 대조했다. 요구사항 누락·부분 구현·충돌·범위 밖 추가 항목은 각각 0개다. 자동완성의 기존 범위 해석기를 재사용하며 외부 의존성과 JDBC 조회를 추가하지 않았다. 헌장은 미작성 템플릿으로 규범 검사를 생략했고 확장 훅은 없다. 새 수렴 작업은 필요하지 않다.

## 전체 경로와 제목 제거 보완 결과: 2026-11-07

- 커서까지의 역추적을 현재 문장의 컬럼 연결 그래프 탐색으로 바꿨다. 원본·중간·최종 컬럼 어느 곳을 선택해도 최초 출처부터 마지막 사용처와 분기까지 표시한다. 같은 별표 위치의 서로 다른 출력 컬럼과 독립된 물리 테이블 별칭은 합치지 않는다.
- CTE 명시 컬럼·출력 이름 변경·사용처 분기·별표 앞뒤 커서·독립 별칭·ORDER BY 출력 별칭 사용을 포함한 분석기 검증 17개가 통과했다. 기존 편집기 검증은 원본 컬럼에서 전체 2·6·9행을 확인하도록 바꿨으며 클릭·Enter·갱신·비변경 동작도 통과했다.
- 우측 하단 TRACE 제목 라벨을 제거했다. 목록과 상태 안내, 접근성 이름은 유지한다.
- 최종 전체 테스트 483개가 통과했다. 실패·오류·건너뜀은 각각 0개다. 기존과 동일한 명령으로 ZIP 생성과 플러그인 구조 검사를 통과했고 설치 ZIP과 SHA-256을 갱신했다.
- 근거: `.gradle-validation/column-trace-complete-focused.log`, `column-trace-complete-final.log`, `build/reports/column-trace-complete-test-summary.json`. 기존 IDE 소켓 제한으로 검색 옵션 재생성은 제외했다. 실제 사용자 IDE 수동 확인과 DB 접속은 수행하지 않았다.
- 후속 수렴 검토에서 요구사항 8개, 성공 기준 3개와 전체 경로 보완 설계를 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 0개이며 새 수렴 작업은 없다. 헌장 템플릿과 훅 부재에 대한 처리는 이전과 같다.

## SELECT 출력 전달 경로로 범위 제한: 2026-11-07

- TRACE 목록은 SELECT 출력과 CTE·파생 테이블의 전달 단계만 표시한다. WHERE·ON·HAVING·GROUP BY·ORDER BY 사용 위치를 제외하고, 조건절 안의 하위 SELECT·WITH 정의가 출력 그래프에 섞이는 것도 막았다.
- 조건절의 컬럼을 커서로 선택해도 그 컬럼의 SELECT 전달 경로만 보여준다. 조건에서만 쓰이고 출력 단계가 없는 컬럼은 빈 목록을 표시한다. SELECT CASE·계산식 입력 컬럼은 결과값의 출처로 계속 추적한다.
- 기존 구현에서 조건 위치 유입을 검출한 회귀 검증 6개가 실패하는 것을 확인한 뒤 수정했다. 새 단위 검증 3개와 편집기 검증 1개를 추가하고 기존 ON·ORDER 포함 기대값을 변경했다. 분석기 전체 20개와 편집기 집중 검증을 통과했다.
- 최종 전체 506개 테스트가 통과했고 실패·오류·건너뜀은 각각 0개다. 전체 경로·별칭·별표·범위 격리·항목 이동·문서 비변경·정렬·포매팅 회귀를 유지한다.
- 동일한 `test buildPlugin verifyPluginStructure -x buildSearchableOptions` 명령이 성공했다. ZIP과 SHA-256을 갱신했다. 기존 IDE 소켓 제약으로 검색 옵션 생성만 제외했고 사용자 IDE 수동 확인·실제 DB 접속은 수행하지 않았다.
- 근거: `.gradle-validation/trace-output-only-red.log`, `trace-output-only-focused.log`, `trace-output-only-final.log`, `build/reports/trace-output-only-test-summary.json`.
- 수렴 검토에서 요구사항 9개·성공 기준 3개와 출력 전달 범위 보완 설계를 현재 코드·검증과 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 0개이고 새 수렴 작업은 없다. 헌장은 미작성 템플릿이며 확장 훅은 없다.

## 빈 상태 안내 단순화: 2026-11-07

- 목록 아래 상태 라벨과 오류 툴팁을 제거했다. 목록 내부 안내는 생성 시 한 번만 `추적할 컬럼에 커서를 놓으세요.`로 설정하며 초기화·분석 중·완료 시 변경하지 않는다.
- 목록의 위쪽 치우침을 해제해 안내를 세로 가운데에 배치했다. 내부 분석 메시지와 경로 항목·클릭·Enter·수명 보호는 유지한다.
- 편집기 회귀 35개가 통과했으며 실패·오류·건너뜀은 각각 0개다. 새 검증은 고정 문구·세로 가운데 설정·하단 라벨 부재·출처 불명 컬럼 선택·정상 경로 복귀를 확인한다. 이번 변경은 표시 계층에 한정돼 관련 편집기 검증을 실행했고 전체 테스트 재실행으로 확대하지 않았다.
- `test --tests 'com.look4counter.QuerySqlEditorTest' buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. 설치 ZIP과 SHA-256을 갱신했다. 기존 IDE 소켓 제약으로 검색 옵션 생성은 제외했으며 실제 사용자 IDE 수동 확인은 수행하지 않았다.
- 근거: `.gradle-validation/trace-prompt-final.log`, `build/reports/trace-prompt-test-summary.json`.
- 후속 수렴 검토에서 FR-003·FR-010과 기존 경로·갱신·탐색 계약을 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 0개이며 새 수렴 작업은 없다. 헌장은 미작성 템플릿이며 확장 훅은 없다.

## 항목 패딩 통일: 2026-11-07

- TRACE 항목에 쿼리 목록과 동일한 기본 렌더러·합성 테두리·좌우 8 논리 픽셀 패딩을 적용했다. IDE 배율과 기본 선택·포커스 표시를 유지한다.
- 기존 편집기 회귀 35개가 통과했고 실패·오류·건너뜀은 각각 0개다. 클릭·Enter 이동·고정 안내·갱신 회귀를 확인했으며 낮은 영향도의 표시 변경에 대해 구현을 복제하는 새 테스트는 추가하지 않았다.
- `test --tests 'com.look4counter.QuerySqlEditorTest' buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. ZIP과 SHA-256을 갱신했다. 검색 옵션 생성은 기존 IDE 소켓 제약으로 제외했고 실제 사용자 IDE 수동 확인은 수행하지 않았다.
- 근거: `.gradle-validation/trace-padding-final.log`, `build/reports/trace-padding-test-summary.json`.
- 수렴 검토에서 FR-011의 기존 목록과 동일한 패딩·배율·선택·탐색 보존을 확인했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 0개이며 새 수렴 작업은 없다.
