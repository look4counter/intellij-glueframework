# 구현 계획: 자바 쿼리 탐색

작성일: 2026-11-03 | 명세: [명세](spec.md)

## 요약

자바 문자열에 IntelliJ PSI 다중 참조를 제공한다. 기존 프로젝트 QueryIdIndexService의 정확 일치 후보 파일만 확인하고 실제 query 태그의 id 속성 값을 선언 대상으로 반환한다. 기본 Ctrl+클릭·선언 이동과 중복 대상 선택 UI를 재사용한다. 기존 쿼리 편집기의 오프셋 이동은 해당 선언의 쿼리를 선택한다.

## 기술 문맥

Kotlin/JVM, IntelliJ Platform 2025.3.6.1, com.intellij.java, Gradle과 BasePlatformTestCase를 사용한다. 새 의존성과 영속 데이터는 없다. 인덱스 스냅샷으로 후보를 제한하고 해석 중 전체 프로젝트 스캔, 쓰기 액션, 문서 커밋과 UI 호출을 하지 않는다. 미커밋 문서와 무효 파일은 대상에서 제외하여 오래된 PSI 이동을 방지한다.

## 헌장 점검

헌장은 미작성 템플릿이므로 규범 검사를 생략한다. 한국어 Markdown, 전체 speckit 단계와 기존 작업 보존을 준수한다. 설계 전후 위반 없음.

## 구조

- queryeditor/src/main/kotlin/com/look4counter/queryeditor/JavaQueryReferenceContributor.kt: 문자열 필터, 값 비교, 다중 대상 해석.
- queryeditor/src/main/resources/META-INF/queryeditor.xml: JAVA 언어의 참조 기여자 등록.
- src/test/kotlin/com/look4counter/JavaQueryNavigationTest.kt: 기본 선언 이동 액션의 후보, 읽기 전용 해석, 중복과 변경, 기존 편집기 오프셋 이동 검증.
- 기존 QueryIdIndexService와 QueryDesignEditor는 재사용하며 동작 정책은 바꾸지 않는다.

## 단계와 검증

1. 테스트에서 기본 선언 이동이 대상을 찾지 못하는 실패를 확인한다.
2. 참조 기여자와 플러그인 등록을 구현한다.
3. 정상·중복·음성·이스케이프·텍스트 블록·변경과 삭제를 검증한다.
4. 기존 쿼리 색인·탐색·스레드 회귀 테스트 후 수렴 점검한다.

## 복잡성

기본 PSI 참조 확장을 사용하므로 별도 마우스 이벤트 처리나 중복 후보 팝업을 만들지 않는다.
