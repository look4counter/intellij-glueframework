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
