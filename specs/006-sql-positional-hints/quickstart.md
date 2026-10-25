# 검증 안내

QuerySqlEditorTest를 실행한다. 실제 편집기 토큰 색상과 ParameterHintsPresentationManager의 힌트 텍스트·오프셋을 검사한다.
밝은·어두운 테마, 여러 줄, 혼합 파라미터, 인용문과 주석, 물음표 추가·삭제·실행 취소·쿼리 전환을 확인한다.
SQL 문서와 저장 XML에 힌트 번호가 들어가지 않는지 확인한다.
UI 렌더링 이미지로 힌트의 가독성과 물음표 앞 표시를 점검한다.

```powershell
& ./.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.QuerySqlEditorTest --offline -g .gradle-user-home --no-daemon --no-configuration-cache '-Dorg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I build/state-wire-check/local-repo.init.gradle
```

임시 로컬 의존성 설정은 기존 모델 편집기 테스트와 동일하다. 프로젝트 의존성 선언은 변경하지 않는다.



## 0부터 시작하는 번호의 최종 검증

QuerySqlEditorTest 13개가 모두 통과했다. 실제 힌트가 0:, 1:부터 시작하며 편집·실행 취소·재실행·쿼리 전환에도 0부터 다시 계산됨을 확인했다.
기존 cyan 색상과 문자열·주석 제외, 원문과 XML 저장 보존 검증도 통과했다.
밝은·어두운 렌더링 이미지 .gradle-validation/sql-syntax-light.png와 .gradle-validation/sql-syntax-dark.png를 직접 열어 0:·1: 표시와 배치를 확인했다.
오프스크린 Swing 렌더링 검증이며 실제 IDE 창을 조작한 검증은 아니다.
같이 실행한 모델 Snap 관련 검증을 합쳐 46개 테스트의 실패·오류는 0개다. 로그는 build/global-snap-zero-hints-test.log다.
