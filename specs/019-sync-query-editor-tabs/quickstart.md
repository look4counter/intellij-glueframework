# 검증 안내

1. JDK 21에서 `./gradlew.bat test --tests com.look4counter.QueryTabNavigationTest --tests com.look4counter.QueryDesignEditorTest --tests com.look4counter.EditorMultiViewTest --tests com.look4counter.EditorOpenThreadTest --tests com.look4counter.JavaQueryNavigationTest`를 실행한다.
2. `./gradlew.bat buildPlugin`으로 설치용 ZIP을 생성한다.
3. query 두 개 이상인 파일에서 두 번째 query를 Design에서 선택한 뒤 Source로 전환한다. 해당 여는 태그 시작에 커서가 있어야 한다.
4. Source의 다른 query 속성·SQL·닫는 태그 내부에서 Design으로 전환한다. 해당 query가 선택되어야 한다.
5. query 사이 공백 및 문서 끝에서 전환하면 첫 query가 선택되어야 한다. 빈 문서·오류 XML·읽기 전용도 확인한다.
6. 수정 직후 전환, 여러 파일 간 전환, 기존 외부 쿼리 탐색 및 실행 취소 동작을 확인한다.

## 실행 결과 — 2026-11-04

- QueryTabNavigationTest 7개, QueryDesignEditorTest 33개, EditorMultiViewTest 7개, EditorOpenThreadTest 11개, JavaQueryNavigationTest 6개로 총 64개 테스트 통과, 실패 0개.
- 양방향 전환, Source 속성·CDATA·닫는 태그, 쿼리 밖 위치의 첫 쿼리 대체, 중복 및 빈 ID, 수정 직후 전환, 읽기 전용, 파일별 상태 분리, 빈 문서·오류 XML·쿼리 삭제 및 쓰기 잠금 없는 전환·폐기를 검증했다.
- 기존 외부 쿼리 탐색 및 원문·실행 취소 검증도 통과했다.
- buildPlugin 성공. 설치 산출물: build/distributions/intellij-glueframework-1.0.1.zip.
- 검증 환경: JDK 21.0.6과 작업 폴더 내 Gradle 9.7.1. 이전 작업의 .gradle-validation/ghost-offline.init.gradle을 재사용하여 기존 ELK·EMF 캐시와 Guava 33.3.1-jre로 오프라인 검증했다. 저장소의 의존성 설정은 변경하지 않았다.
글자 위치 검증: 여러 줄 SQL 중간, XML 엔티티 및 분할 CDATA에서 Source·Design을 왕복하고 같은 글자 위치인지 확인한다. 저장 전 SQL 수정과 쿼리 간 전환 후에도 위치를 확인한다. 기존 탭·Design 테스트를 직접 컴파일·JUnitCore로 실행한다.

## SQL 글자 위치 보존 검증 결과

2026-11-06: QueryEditor 전체 소스 및 관련 테스트를 직접 컴파일했다. QueryTabNavigationTest 12개가 모두 통과했고 CDATA·XML 엔티티·분할 CDATA·한글·여러 줄·끝 위치·반복 왕복·저장 전 수정·쿼리별 복원·읽기 전용·비동기 폐기를 확인했다. 탐색 전후 원문도 유지된다.

QueryDesignEditorTest 33개를 포함한 JUnit 45개 검증 항목은 통과했으나 기존 화면 그리기 테스트에서 수동 테스트 런타임의 SVG 로더 IllegalAccessError 로그가 발생했다. 화면 렌더링 검증에는 이 환경 제약이 남아 있다. 분리 실행한 커서·탭 탐색 12개에는 해당 오류가 없다.

재실행 스크립트는 .gradle-validation/run-query-caret.ps1이다. 검증용 최신 QueryEditor 등록 리소스와 Java 보조 클래스를 포함한다. 컴파일 인자는 query-caret-compile.args, 전체 테스트 인자는 query-caret-test.args, 탐색 전용 인자는 query-caret-navigation.args이며 로그는 같은 이름의 .log 파일이다. 기존 IntelliJ 배포 ZIP 의존성 누락으로 플러그인 ZIP은 새로 만들지 못했다.
