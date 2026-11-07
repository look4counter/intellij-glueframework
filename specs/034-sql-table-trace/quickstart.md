# 검증 안내

기존 로컬 Gradle·JBR 캐시를 사용한다. 테이블 분석·컬럼 분석·QuerySqlEditor·SQL 완성·컬럼 오류 관련 검증 후 전체 테스트와 설치 ZIP·플러그인 구조 검사를 실행한다. 기존 IDE 소켓 제약으로 검색 옵션 재생성은 제외한다.

## 확인 예제

```sql
WITH a AS (
 SELECT * FROM physical
), b AS (
 SELECT * FROM a
)
SELECT b.id
FROM b
```

physical·a·b 및 b.id의 b를 선택하면 2행의 원본, 1행의 a 선언, 4행의 참조, 3행의 b 선언, 7행의 최종 참조가 나타난다. id를 선택하면 기존 컬럼 경로를 표시한다. 항목 클릭과 Enter로 토큰 이동을 확인하고 원본 SQL·Source가 바뀌지 않는지 확인한다.

분기·JOIN 격리·인용 이름·중첩 WITH·파생 테이블·재귀·UNION·다중 문장·주석/문자열·분석 한도도 자동 검증한다. 실제 IDE 수동 확인과 자동 검증을 구분해 결과를 기록한다.

## 자동 검증 결과: 2026-11-08

- 테이블 분석·기존 컬럼 분석·실제 QuerySqlEditor 집중 검증 69개가 통과했다. CTE 연쇄의 어느 지점에서 선택해도 전체 경로와 분기가 표시되고, 이후 JOIN의 다른 입력과 독립적인 같은 이름의 FROM 인스턴스가 섞이지 않는 것을 확인했다.
- 실제 편집기 테스트에서 한정자의 테이블 선택과 컬럼 선택 전환, 클릭·Enter의 정확한 위치 이동, SQL·Source 비변경, 편집 및 쿼리 전환 후 경로 갱신을 확인했다. 기존 가운데 안내·패딩·제목 및 하단 문구 없는 구성도 통과했다.
- 첫 전체 회귀 525개 통과 후 수렴 검토에서 구문 키워드의 별칭 오인 가능성을 발견했다. PIVOT·UNPIVOT·MODEL·MATCH_RECOGNIZE 선택 제외 및 인용 별칭 허용 검증을 추가했고 최종 전체 회귀 526개가 통과했다. 실패·오류·건너뜀은 각각 0개다.
- `test buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. 기존 IDE 소켓 제약으로 검색 옵션 재생성만 제외했고 설치 ZIP 및 SHA-256을 갱신했다. 실제 사용자 IDE 수동 조작은 수행하지 않았다.
- 근거: `.gradle-validation/table-trace-focused.log`, `.gradle-validation/table-trace-final.log`, `build/reports/table-trace-focused-test-summary.json`, `build/reports/table-trace-test-summary.json`.

## 구현 전 분석과 수렴

구현 전 요구사항 6개를 T003~T008에 대응시켰고 성공 기준 3개를 T004·T008·T009로 검증하도록 했다. 대응률 100%이며 중복·모호함·충돌·미대응 작업은 없었다. 품질 점검은 16/16 통과했다. 헌장은 미작성 템플릿이므로 규범 검사를 생략했고 확장 훅은 없다.

첫 수렴에서 FR-005의 구문 키워드 선택 제외가 부분 구현인 중간 심각도 항목 1개를 발견해 T011을 추가했다. 해당 작업을 구현하고 전체 회귀를 완료했다.

최종 수렴에서 요구사항 6개·성공 기준 3개·인수 시나리오 7개와 위치 보존·객체 식별·방향 그래프·기존 목록 및 컬럼 분석 재사용·분석 한도 결정을 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 각각 0개다. 수렴 단계에서 새 작업을 추가하지 않았고 이후 구현 완료 기록만 갱신했다. 실제 프로젝트 버전 1.0.4의 `build/distributions/intellij-glueframework-1.0.4.zip`과 같은 경로의 SHA-256 파일을 최종 산출물로 확인했다.

## 커서 선택 동기화와 마우스 누름 검증

- 기존 명세·명확화·계획·작업 목록을 갱신하고 FR-007·FR-008을 T012~T015와 대조했다. 품질 점검은 통과했고 미해결 질문·누락·충돌은 없다.
- 분석 결과에 selectedOffset을 추가해 테이블 선언·참조·한정자와 컬럼 출력의 선택 위치를 보존했다. UI는 정확한 위치를 우선하고 중복 줄은 줄 번호로 대응하며 대응 행이 없으면 임의 선택하지 않는다.
- 왼쪽 mousePressed에서 이동하므로 누름 뒤 미세 이동으로 mouseClicked가 생략돼도 이동한다. 목록 선택과 스크롤을 맞추고 기존 문서 stamp·세대·수명·Enter 보호를 유지한다.
- SqlColumnTraceTest 21개, SqlTableTraceTest 14개, QuerySqlEditorTest 38개로 총 73개가 통과했다. 실패·오류는 0개다. 커서와 선택 행, 조건절의 미선택, 별칭·한정자 대응, 미세 이동 후 무클릭, 우클릭·빈 영역, Source 비변경 및 기존 경로 회귀를 확인했다.
- 첫 컴파일의 Kotlin 증분 백업 복원 오류는 증분 컴파일을 끈 재실행으로 해소했다. 근거: .gradle-validation/trace-selection-press.log. 실제 사용자 IDE 수동 조작은 수행하지 않았다.
- 자동완성 정렬과 함께 수행한 후속 QuerySqlEditorTest에서도 Trace 회귀가 통과했다. 최종 buildPlugin·verifyPluginStructure가 성공했으며 검색 옵션 재생성만 기존 환경 제약으로 제외했다. .gradle-validation/completion-kind-order.log와 설치 ZIP을 확인했다.
- 최종 수렴에서 요구사항 8개·성공 기준 3개와 선택 위치·경로 유지·누름/Enter·오래된 결과 보호를 대조했다. 누락·부분 구현·충돌·범위 밖 추가는 0개다. 추가 작업이 없어 수렴 단계에서 작업 목록을 변경하지 않는다.

## 표준 트리 전환 검증

- 명세·명확화·계획·작업 목록을 갱신하고 FR-009~FR-013을 T016~T020과 대조했다. 기존 선택·탐색 요구도 새 트리 계약에 맞췄으며 미해결 질문·누락·충돌은 없다. 명세 품질 점검은 16/16 통과했다.
- 실제 inputs·consumers 관계를 그래프로 반환하고 표시 없는 연결만 축약했다. 같은 줄 및 같은 별표 위치의 출력 슬롯을 합치지 않으며 분기·재합류의 직접 연결을 보존한다. 공유 소비자는 각 실제 경로에 표시한다.
- IntelliJ Tree·기본 펼침 화살표·좌우 키를 사용한다. 본문 누름과 Enter는 정확한 SQL 위치로 이동한다. 순환은 해당 가지에서 표식과 함께 종료하며 반복 분기는 2048개 표시 노드와 깊이 96으로 제한하고 한도를 표시한다.
- 같은 문서·stamp·관계 그래프에서 펼침 및 선택 경로를 보존한다. 다른 그래프는 처음처럼 펼치고 현재 선택 노드의 필요한 조상만 펼친다. 편집·쿼리 전환·해제 후 오래된 결과는 적용하지 않는다.
- 집중 검증은 SqlTraceGraphTest 4개·SqlColumnTraceTest 23개·SqlTableTraceTest 15개·Trace UI 9개로 총 51개가 통과했다. 분기·재합류·동일 줄 객체·동일 위치 별표·재귀·한도·표준 화살표/키보드·접힘 유지·공유 경로 선택 및 Source 비변경을 확인했다.
- 최종 QuerySqlEditorTest 전체 47개가 통과했다. 새 그래프로 바뀔 때 초기 펼침 상태를 검증했고 기존 자동완성·포매팅·Source 및 실행 취소 회귀도 통과했다.
- ModelEditor 관련 31개와 함께 최종 검증 78개가 통과했고 buildPlugin·verifyPluginStructure가 성공했다. 기존 환경 제약으로 buildSearchableOptions만 제외했다. 설치 ZIP 및 SHA-256을 갱신했다.
- 근거: .gradle-validation/trace-tree-focused.log, .gradle-validation/trace-tree-toolbar-final.log. 실제 사용자 IDE의 수동 조작은 수행하지 않았다.

- 프로젝트 전체 회귀 552개가 통과했다. 실패·오류·건너뜀은 각각 0개다. 근거: .gradle-validation/trace-tree-toolbar-regression.log 및 build/test-results/test의 전체 XML 결과.
