# 검증 안내

## 자동 검증

JDK 21과 기존 의존성 캐시로 분석기·편집기 테스트를 먼저 실행하고 전체 test를 수행한다. SELECT·FROM·JOIN·WHERE·ON·GROUP BY·HAVING·ORDER BY와 테이블/CTE/파생 별칭, 중복 컬럼 삽입, 중첩·상관·분리 문장, 인용 이름, 미완성·과도한 입력을 확인한다. 편집기에서 실제 후보를 선택한 뒤 SQL·Source·Undo/Redo도 확인한다.

이 환경에서는 이전에 확인된 IDE 기동 소켓 제한에 따라 buildPlugin·verifyPluginStructure에 -x buildSearchableOptions를 사용하며 그 사실을 결과에 기록한다.

## 수동 확인

메타데이터를 조회한 프로젝트에서 SELECT e. 뒤의 컬럼, FROM 뒤의 테이블, WITH의 출력 컬럼을 확인한다. DB에 접속하지 않은 상태에서도 CTE의 명시적 컬럼·별칭과 키워드가 제시돼야 한다. 실제 사용자 IDE에서 수행한 확인과 자동 검증을 구분한다.

## 검증 결과: 2026-11-07

- 기존 구현에서 새 FROM 구역·별칭 삽입 통합 테스트 2개가 모두 실패하는 것을 확인했다.
- 분석기 24개와 편집기 18개를 포함한 전체 427개 테스트가 통과했다. 실패·오류·건너뜀은 각각 0개다. 구역·별칭·스키마·인용 이름·CTE·파생·중첩·상관·별칭 가림·문장 격리·별표·재귀 안전성·불완전 입력·보호·과도한 입력을 검증했다.
- 실제 편집기 검증은 FROM에 컬럼이 섞이지 않는 것, 커서 뒤 FROM 해석, 점 뒤 CTE 출력·중복 컬럼의 검색과 삽입, 선택한 쿼리만 Source에 반영되고 Undo/Redo되는 것을 포함한다.
- 기존 SELECT 메타데이터 기대값은 새 계약에 맞게 테이블을 제외하고 컬럼만 확인하도록 갱신했다. 점 입력의 자동 팝업 가능 여부와 한정자 변경 시 후보 재시작을 연결했다.
- `test buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. 검색 옵션 생성은 기존에 확인된 IDE 디렉터리 소켓 권한 제약 때문에 제외했다. 실제 사용자 IDE 수동 조작과 실제 DB 접속은 수행하지 않았다.

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-validation'
$env:JAVA_HOME = Join-Path $PWD '.gradle-validation/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr'
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' test buildPlugin verifyPluginStructure -x buildSearchableOptions
```

근거는 .gradle-validation/sql-context-red.log, sql-context-focused.log, sql-context-final.log와 build/reports/sql-context-test-summary.json, sql-context-results에 있다. 최초 집중 실행은 새 분석기·구역 검증은 통과하고 기존 평탄 후보 기대값 한 개가 실패했으며, 이를 새 계약으로 바꾼 뒤 전체 검증에 성공했다.

분석 단계는 요구사항 9개·작업 11개가 모두 연결돼 있고 중요한 모호함·충돌이 없었다. 품질 점검 6개가 모두 통과했다. 헌장은 미작성 템플릿이므로 규범 검사를 생략했다.

## 인접 파생 테이블 별칭 보완 검증: 2026-11-07

- 사용자 예제에서 A 뒤의 두 번째 SELECT 괄호를 A의 명시적 컬럼 목록으로 오인하는 문제를 재현했다. 새 분석기·편집기 테스트 2개가 수정 전에 모두 실패했다.
- 수정 후 메타데이터 없이 A는 DEPT·JOB·SALARY, B는 DEPT2·JOB·SALARY만 제시한다. 편집기에서 DEPT를 선택하면 한정자를 유지해 A.DEPT로 삽입한다. 정상적인 별칭 컬럼 목록과 WITH로 시작하는 인접 파생 쿼리도 검증했다.
- 분석기·편집기 집중 검증과 전체 462개 테스트가 통과했다. 실패·오류·건너뜀은 각각 0개이며 설치 ZIP 생성과 플러그인 구조 검사도 성공했다.
- 실행 명령은 위와 동일하다. 기존 IDE 소켓 제한으로 검색 옵션 재생성을 제외했으며, 사용자 IDE 수동 확인과 실제 DB 접속은 수행하지 않았다.
- 근거: `.gradle-validation/derived-alias-red.log`, `derived-alias-focused.log`, `derived-alias-final.log`, `build/reports/derived-alias-test-summary.json`. 최신 ZIP과 SHA-256 파일을 `build/distributions`에 갱신했다.
- 수렴 검토: 기능 요구사항 10개, 성공 기준 3개, 계획의 기존 단계 6개와 경계 보완 설계를 구현·회귀 테스트와 대조했다. 미구현·부분 구현·충돌·범위 밖 추가 항목은 없으며 남은 작업은 0개다. 헌장은 미작성 템플릿으로 검사를 생략했다.

## 두 번째 SELECT 항목 검증: 2026-11-07

- `SELECT A.DEPT` 다음 줄의 `, B.`와 사용자 예제의 전체 줄바꿈·서브쿼리를 그대로 분석기·편집기 회귀 테스트에 추가했다.
- 두 테스트는 기존 경계 보완 구현으로 처음부터 통과했다. B의 점 바로 뒤에서 후보는 DEPT2·JOB·SALARY이며 DEPT는 제외한다. DEPT2 선택 후 B.DEPT2가 삽입되고 앞의 A.DEPT와 나머지 SQL은 유지된다. 운영 코드는 추가 변경하지 않았다.
- 전체 464개 테스트가 통과했다. 실패·오류·건너뜀은 각각 0개다. `test buildPlugin verifyPluginStructure -x buildSearchableOptions`도 성공했다. 설치 ZIP 내용은 이전 수정본과 동일하며 SHA-256 파일을 확인했다.
- 근거: `.gradle-validation/second-projection-focused.log`, `second-projection-final.log`, `build/reports/second-projection-test-summary.json`.
- 기존 IDE 소켓 제약에 따라 검색 옵션 재생성은 제외했다. 실제 사용자 설치본의 버전·동작과 실제 DB 접속은 확인하지 않았다.
- 수렴 결과: 새 FR-011의 후보·오프셋·삽입·원문 보존과 계획의 추가 확인 단계를 모두 충족했다. 기존 요구사항 10개와 성공 기준 3개는 전체 회귀로 유지된다. 미구현·부분 구현·충돌·범위 밖 추가 항목은 0개이며 새 수렴 작업은 없다. 헌장은 미작성 템플릿으로 규범 검사를 생략했다.

## 종류·문맥 정렬 검증

- 명세·명확화·계획·작업 목록을 갱신하고 구현 전 일관성을 분석했다. FR-012~FR-014는 T018~T020과 대응하며 미해결 질문·누락·충돌은 없다. 기존 품질 점검 6개가 통과한다.
- 후보 종류 병합 후 종류와 이름의 비교기를 적용했다. 컬럼·출력 컬럼은 같은 우선 그룹이고 테이블 입력 위치의 CTE·테이블도 하나의 그룹이다. 그룹 안에서는 대소문자를 구분하지 않는 ABC순을 사용한다.
- 동일 삽입 문자열은 종류·검색 이름을 기존처럼 병합하고 가장 앞선 종류의 순위를 사용한다. 후보 생성·접두어 필터·삽입·저장 방식은 유지한다.
- SqlCompletionAnalyzerTest 26개와 QuerySqlEditorTest 41개로 주요 검증 67개가 통과했다. 컬럼 우선·CTE/테이블 혼합 ABC순·일반 후보 종류·한정자 ABC순과 기존 삽입·Source·실행 취소 및 Trace 회귀를 확인했다.
- 추가 경계 검증 2개에서 키워드와 같은 이름의 컬럼이 한 항목으로 병합되어 컬럼 우선순위를 유지하는지, 출력 별칭도 컬럼 우선 그룹에 들어가는지 확인했고 모두 통과했다. 정렬 및 관련 회귀는 총 69개이며 실패·오류는 0개다.
- buildPlugin·verifyPluginStructure가 성공했고 설치 ZIP 및 SHA-256을 갱신했다. 기존 환경 제약으로 buildSearchableOptions만 제외했다. 근거: .gradle-validation/completion-kind-order.log 및 .gradle-validation/completion-kind-edge.log. 실제 사용자 IDE 수동 조작은 수행하지 않았다.
- 최종 수렴에서 요구사항 14개 및 계획·작업을 대조했다. 미구현·부분 구현·충돌·범위 밖 추가 항목은 0개로 추가 작업이 없다. 수렴 단계에서 tasks.md는 변경하지 않는다.
