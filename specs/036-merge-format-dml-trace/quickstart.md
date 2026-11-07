# 검증 안내: MERGE 정렬과 컬럼 소스 추적

## 현재 동작

포맷은 기존 대소문자·줄바꿈을 유지하며 MERGE의 공백·들여쓰기를 정리한다. USING SELECT는 4칸·FROM은 6칸, ON 본문은 4칸, SET은 3칸, INSERT·VALUES 목록은 첫 항목 6칸·콤마 4칸이다.

SELECT·CTE·파생 쿼리의 컬럼 전달 경로는 기존 규칙을 따른다. 변경문 컬럼은 자신의 물리 테이블 선언 또는 SELECT 출처와 현재 선택한 컬럼 참조를 함께 보여준다. SET·INSERT 대상도 자기 소스로 해석하며 대입 반대편이나 VALUES 상수와 연결하지 않는다. 한정자가 없는 쓰기 대상은 대상 테이블 문맥을 사용한다. 일반 SELECT의 모든 물리 FROM 줄을 추가하지는 않으며 기존 조건 서브쿼리 제외와 테이블 추적 우선순위도 유지한다.

## 사용자 MERGE 예제 검증

- SET의 a.deptno 선택: 2행 INTO emp a → 13행·19행의 모든 a.deptno 참조.
- INSERT 목록의 a.deptno 선택: 동일하게 2행 → 13행·19행 전체.
- 4행 TEST·실제 값인 'B'·13행 b.TEST 선택: 모두 4행 SELECT의 TEST 출력 → 13행 b.TEST 참조.

선택한 참조의 토큰 위치가 selectedOffset과 트리 selectedNodeId에 유지되는 것도 검증했다. a.deptno와 b.TEST는 대입 관계로 연결되지 않는다.

## 자동 검증

2026-11-08에 로컬 Gradle 9.7.1·JBR 21·Kotlin 2.3.20·IDEA 2025.3.6.1 SDK로 메인·테스트 컴파일과 회귀를 실행했다. 기존 임시 로컬 Maven 설정을 사용했고 프로젝트 의존성은 변경하지 않았다.

```powershell
./gradlew.bat test --tests 'com.look4counter.Sql*Test' --tests com.look4counter.QuerySqlEditorTest
```

실제 실행에는 `--offline --no-configuration-cache -I .gradle-validation/press-position.init.gradle`을 추가했다. 근거는 .gradle-validation/full-reference-tests.log와 build/test-results/test의 해당 XML이다. 새 출처·자기 참조 기대가 이전 대상 제외 구현에서 실패함을 먼저 확인했다(.gradle-validation/full-reference-before.log).

- SqlColumnTraceTest 33개: 사용자 MERGE의 정확 행·선택 오프셋, UPDATE·INSERT 대상·일반 입력의 독립 소스, 인용 식별자·CTE·스칼라 SELECT·DELETE와 기존 SELECT 경로.
- SqlFormatterTest 40개: MERGE 예제·멱등성·대소문자·줄바꿈과 기존 포맷 보호.
- SqlTableTraceTest 15개와 SqlTraceGraphTest 4개: 테이블 선택과 트리 동작.
- SqlCompletionAnalyzerTest 26개와 SqlColumnValidatorTest 9개: 공통 분석기 회귀.
- QuerySqlEditorTest 47개: 실제 편집기 포맷·선택·설정·Source·Undo/Redo와 TRACE UI.

총 174개가 실패·오류·건너뜀 없이 통과했다. git diff --check도 통과했다. 사용자 IDE의 수동 조작과 배포 ZIP 생성은 수행하지 않았다.

## 명확화·분석·수렴

사용자의 “SELECT 추적”을 쓰기 대상 제외로 해석했던 이전 결정을 구체적인 MERGE 예제와 자기 참조 포함 요청으로 정정했다. 최신 명세·명확화·계획·작업 분석을 갱신했고 FR-002~FR-006·FR-008을 T016~T019에 대응시켰다. 구현 후 최신 요구사항 8개와 관련 계획·작업을 대조한 결과 추가 누락은 없다. 헌장은 미작성 템플릿으로 추가 규범은 없다.

## 전체 경로와 실제 값 선택 보완

출처에서 선택해도 사용처가 포함되도록 현재 문장의 변경문 참조 전체를 먼저 수집한다. SELECT의 실제 값·표현식 토큰을 선택하면 출력 슬롯을 시작점으로 사용한다. 사용자 MERGE 예제의 TEST·'B'·b.TEST, a.deptno의 모든 참조와 트리 선택 ID를 검증했다. 문자열·숫자·파라미터·CASE 값, CTE·파생 전달과 복수 분기, 다른 문장·VALUES·주석 격리 및 선언 별칭이 컬럼으로 잘못 수집되지 않는 것도 확인했다. 키워드 목록과 이름이 겹치는 b.result 같은 한정 컬럼도 포함한다.

명세·명확화·계획·작업을 FR-009·FR-010과 T020~T023으로 갱신하고 구현 전 대응을 확인했다. 구현 후 최신 요구사항 10개와 관련 계획·작업의 수렴을 대조했으며 추가 누락은 없다. 메인·테스트 컴파일과 174개 회귀가 실패·오류·건너뜀 없이 통과했고 diff 검사도 통과했다. 실제 사용자 IDE 수동 조작은 수행하지 않았다.