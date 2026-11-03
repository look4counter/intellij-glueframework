# 검증 절차

1. JavaQueryNavigationTest로 기본 선언 이동의 정확 일치·중복·음성·이스케이프·텍스트 블록·변경과 삭제를 검증한다.
2. 읽기 액션의 해석과 잠금 없는 편집기 이동에서 쓰기 스레드 오류와 원문 변경이 없는지 확인한다.
3. QueryIdIndexTest, QueryNavigationTest와 EditorOpenThreadTest를 회귀 검증한다.
4. IDE에서 자바의 문자열 예시 "sample.select"를 Ctrl+클릭하여 쿼리 편집기에서 해당 쿼리가 선택되는지 수동 확인한다. 수동 UI 검증과 자동 검증 결과는 구분한다.

## 실행 결과

2026-11-03 기준 JavaQueryNavigationTest 6개와 기존 QueryIdIndexTest 6개, QueryNavigationTest 1개, EditorOpenThreadTest 11개를 합한 총 24개가 통과했다. 모듈과 테스트 컴파일도 성공했다. 최초 정확 일치 선언 이동 테스트는 구현 전 대상이 없어 실패하는 것을 확인한 후 구현했다.

단일 대상은 GotoDeclarationAction.findAllTargetElements로 검증했다. 중복 대상은 플랫폼의 TargetElementUtil.getTargetCandidates와 CtrlMouseData의 탐색 가능 여부를 함께 확인했다. 구형 findAllTargetElements 단독 호출은 단일 해석 결과만 반환하므로 중복 탐색 검증에 사용하지 않았다. 자바 이스케이프와 텍스트 블록, 빈 값·부분 일치·대소문자 차이·숫자·문자·주석, ID 변경과 삭제, 미커밋 문서 제외를 검증했다. 실제 쓰기 의도 잠금을 해제한 읽기 액션에서 참조를 해석했고, 잠금 없는 선언 위치 이동으로 기존 편집기 쿼리 선택과 원문 보존을 확인했다.

검증은 캐시된 Gradle 9.7.1과 IntelliJ JBR 21을 이용한 오프라인 실행이며 기존 build/state-wire-check/local-repo.init.gradle로 로컬 ELK/EMF 저장소와 Guava 33.3.1-jre를 사용했다. 프로젝트 의존성은 변경하지 않았다. 실제 IDE 창에서 마우스로 Ctrl+클릭하는 수동 UI 검증은 수행하지 않았다.

## 분석과 최종 수렴

사전 분석에서 기능 요구사항 5개가 모두 작업에 연결되었고 미해결 모호성·중복·헌장 충돌은 없었다. 품질 체크리스트 6개가 모두 완료되었다. 헌장은 미작성 템플릿으로 규범 검사를 생략했다.

최종 수렴은 기능 요구사항 5개, 성공 기준 3개, 수용 시나리오 3개와 계획의 소프트 다중 참조·후보 파일 제한·읽기 전용 해석·기존 편집기 이동 계약을 코드 및 테스트에 대조했다. 미구현·부분 구현·모순 항목은 없으며 추가 작업이 필요하지 않다.
