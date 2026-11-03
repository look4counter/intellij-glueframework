# 편집기 조작과 DB 연결 검증

1. Child 이동·좌우 크기 조절·Wire 끝점과 꺾임점에 대해 작은 움직임과 실제 드래그를 수행한다. 더블클릭 편집과 중복되지 않아야 한다. 줌을 변경해 반복한다.
2. SQL의 중첩 괄호 앞뒤에 커서를 두고 문자열·주석을 제외한 짝 강조를 확인한다. 쿼리 전환과 문서 편집 후 갱신되어야 한다.
3. Oracle 테이블·뷰·컬럼 수집에서 조회 횟수가 객체 수와 무관하게 2회인지 자동 테스트로 확인한다. 실제 서버 속도는 별도 환경에서 측정한다.
4. GlueQueryEditor 설정의 미적용 정보로 연결 테스트를 수행한다. 성공·실패·취소와 새 비밀번호·저장 비밀번호·삭제 선택을 확인한다. 설정과 메타데이터 캐시는 바뀌지 않아야 한다.

## 검증 결과

2026-10-27: ModelDiagramTest 73개, ModelDiagramPropertiesTest 29개, ModelEditorSettingsTest 9개, DatabaseMetadataTest 5개, DataSourceSettingsTest 10개, QuerySqlEditorTest 15개로 총 141개 고유 테스트가 통과했다. 전체 실행의 140개 통과 이후 마지막 폐기 검사에서 이벤트 큐 정리를 기다리도록 보완하고 해당 검사 1개를 단독 재실행하여 통과했다. 제품 코드는 전체 실행 이후 변경하지 않았다. 전체 로그는 build/editor-improvements-final.log, 최종 폐기 검사 로그는 build/brace-lifecycle-test.log다. 사례별 결과는 build/editor-improvements-validation.json에 기록했다.

임계치 검증은 Child 이동·좌우 크기 조절·Wire 양 끝점·꺾임점에서 0.5배·1배·1.5배 줌을 검사했다. 작은 움직임에서 변경 없음, 임계치 이후 시작 1회, 드래그 후 더블클릭 차단, 다음 클릭 복원 및 취소를 확인했다. 기존 잠금 없는 마우스 이벤트·실행 취소·저장 회귀도 통과했다.

괄호 검증은 중첩·문자열·주석·짝 없음·불일치·문서 변경·쿼리 전환·에디터 폐기를 포함한다. 인접한 두 괄호 사이에서는 커서 위치의 괄호를 우선한다.

Oracle은 가짜 JDBC 연결에서 SQL 실행 2회, 테이블·뷰·여러 스키마 동명 객체의 구분, 컬럼 순서, 취소 시 ResultSet과 Statement 해제를 확인했다. 연결 테스트는 미저장 URL·새 비밀번호·저장 비밀번호·삭제·실패·취소·설정 미저장·연결 해제를 검사했다. 격리 JDBC 드라이버로 메타데이터 조회 없이 연결만 성공하고 취소 후에도 연결·드라이버·로더 정리가 수행되는 것을 확인했다.

기존 설정 계층 검사는 현재 등록된 GlueModelEditor와 GlueQueryEditor를 모두 확인하도록 기대값을 갱신했다. 설정 화면 캡처에서 연결 테스트 버튼의 배치도 확인했다.

## 실행 명령

```powershell
& ./.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.ModelEditorSettingsTest --tests com.look4counter.ModelDiagramTest --tests com.look4counter.ModelDiagramPropertiesTest --tests com.look4counter.QuerySqlEditorTest --tests com.look4counter.DatabaseMetadataTest --tests com.look4counter.DataSourceSettingsTest buildPlugin -x buildSearchableOptions --offline -g .gradle-user-home --no-daemon --no-configuration-cache '-Dorg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I build/state-wire-check/local-repo.init.gradle
```

최종 폐기 검사 재실행에는 위 명령에서 test와 --tests com.look4counter.QuerySqlEditorTest.testSqlBraceHighlightsClearOnQuerySwitchAndDisposal만 사용했다.

설치용 ZIP은 build/distributions/intellij-glueframework-1.0.0-SNAPSHOT.zip이다. SQL 괄호 강조·JDBC·Canvas 변경 클래스가 포함된 것을 확인했다. 기존 환경의 IDE 검색 옵션 수집 프로세스 실행 제한에 따라 검색 옵션 재생성은 제외하고 기존 산출물을 재사용했다.

실 Oracle 서버 접속·성능 수치와 실제 사용자 IDE에서의 수동 조작은 검증하지 않았다.

## 최종 수렴 점검

기능 요구사항 7개·성공 기준 4개·사용자 이야기 4개와 드래그·괄호·Oracle·연결 테스트 설계를 코드·테스트와 대조했다. 헌법은 미작성 템플릿으로 추가 제약 검사를 생략했다. 누락·부분 구현·상충·범위 밖 구현은 없으며 추가 작업은 0개다. 작업 목록을 변경할 수렴 작업은 없다.
