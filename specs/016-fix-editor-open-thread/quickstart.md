# 검증 절차

1. EditorOpenThreadTest로 쓰기 의도 잠금 없는 생성·검색 이동·연속 이동·폐기·모델 표시를 검증한다.
2. QueryNavigationTest, QueryDesignEditorTest, QueryOpenListenerTest, ModelEditorRefreshTest, ModelServiceDirectEditorTest를 실행한다.
3. IDE에서 파일 검색과 본문 검색 결과로 쿼리·모델·서비스 파일을 열고 같은 파일의 다른 결과로 반복 이동한다. 실제 찾기창 UI 재현 여부는 자동 테스트 결과와 구분하여 보고한다.

## 실행 결과

2026-11-03 기준 새 회귀 테스트 11개와 기존 관련 테스트 60개, 총 71개가 통과했다. 두 모듈과 루트 테스트 소스의 컴파일도 성공했다. 처음 추가한 잠금 해제 재현 테스트 3개는 수정 전 실패했으며, 쿼리 ID 이동에서 사용자가 제보한 쓰기 스레드 오류 문구를 그대로 확인했다. 두 편집기 생성에서는 잠금 없는 문서 획득의 읽기 접근 오류도 확인했다.

검증 범위는 쿼리·모델 Design 및 Source 생성, 서비스 직접 편집 생성, 검색 오프셋과 ID 이동, 연속 요청, 미커밋 원문, 안전하지 않은 모달, 폐기와 파일 삭제, 읽기 전용, 쿼리·모델 참조 열기이다.

Gradle 래퍼 배포본 다운로드와 JDK 자동 검색은 제한된 네트워크에서 실패했다. 최종 검증은 저장소의 .gradle-validation/gradle-9.7.1 배포본, 캐시된 IntelliJ JBR 21, 오프라인 모드, 기존 build/state-wire-check/local-repo.init.gradle을 사용했다. 해당 초기화 스크립트는 로컬 ELK/EMF 저장소와 캐시된 Guava 33.3.1-jre를 사용한다. 프로젝트 의존성 설정은 변경하지 않았다.

실제 IDE 찾기창 UI 조작은 수행하지 않았다. 자동 검증은 해당 경로의 공개 편집기 생성·탐색 진입점을 실제 쓰기 의도 잠금 해제로 호출한 플랫폼 통합 테스트다.

## 분석과 수렴 점검

사전 분석에서 기능 요구사항 5개가 모두 작업에 연결되었고 모호성·중복·중대 충돌은 없었다. 헌장은 미작성 템플릿으로 규범 검사를 생략했다. 품질 체크리스트 6개를 모두 통과했다.

초기 수렴에서 모델 참조 열기와 경계 검증 2건을 발견해 T011·T012로 추가하고 구현했다. 최종 점검은 기능 요구사항 5개, 성공 기준 3개, 사용자 수용 시나리오 5개와 계획의 생성·탐색·모달·폐기 계약을 코드 및 테스트에 대조했다. 남은 구현 작업은 없다.

## 파일 열기 호환성 보완 검증 (2026-11-03)

네 생성 제공자를 Application.runWriteIntentReadAction으로 교체했다. EditorOpenThreadTest 11개와 EditorMultiViewTest 7개, 총 18개가 통과했고 두 모듈 및 테스트 컴파일과 buildPlugin이 성공했다. javap -c로 네 제공자 전부가 Application.runWriteIntentReadAction을 직접 호출하며 문제의 WriteIntentReadAction.compute 참조가 없음을 확인했다. 설치 파일은 build/distributions/intellij-glueframework-1.0.1.zip이다.

실행 명령은 기존 오프라인 환경에서 다음과 같다.

```powershell
./.gradle-validation/gradle-9.7.1/bin/gradle.bat --offline --init-script build/state-wire-check/local-repo.init.gradle test --tests com.look4counter.EditorOpenThreadTest --tests com.look4counter.EditorMultiViewTest buildPlugin
```

검증 SDK는 2025.3.6.1이며 실제 사용자 IDE와 최신 SDK 실행 검증은 수행하지 않았다. 공식 최신 소스와 로컬 SDK의 위임 대상 서명을 비교했다. 명세 품질 7개 모두 통과, 보완 사전 분석은 FR-006→T013·T014, SC-004→T015로 연결되며 전체 기능 요구사항 6개에 미연결 항목이나 중대 충돌은 없다.
