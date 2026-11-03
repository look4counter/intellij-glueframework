# 검증 안내
1. 쓰기 의도 잠금이 없는 EDT에서 Activity·Transition 더블클릭과 Diagram 속성을 요청한다.
2. 예약 이벤트를 처리하고 잠금이 있는 문맥에서 대화상자가 호출되는지 검사한다.
3. 기존 속성 편집 테스트로 확인·취소·실행 취소·동시 변경 보호를 검증한다.
4. 실제 IDE에서 창 열기·값 변경·확인·실행 취소를 확인한다. 수동 검증 여부는 자동 검증과 구분한다.

실행 명령은 기존 로컬 캐시를 사용한다.

```powershell
& ./.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.ModelDiagramPropertiesTest --offline -g .gradle-user-home --no-daemon --no-configuration-cache '-Dorg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I build/state-wire-check/local-repo.init.gradle
```

## 검증 결과

2026-10-26: 수정 전 잠금 없는 Activity·Transition 진입 2개 테스트에서 보고된 오류 문구를 동일하게 재현했다. 증거는 build/activity-thread-before-results.xml과 build/activity-thread-before.log에 있다.

수정 후 ModelDiagramPropertiesTest 29개가 모두 통과했다. 실패·오류·건너뜀은 0개다. 새 검증은 잠금 없는 Activity·Transition·Diagram 요청과 Activity 취소·확인·실행 취소·재실행을 확인한다. 기존 검증은 동시 변경·읽기 전용 보호와 속성·완성 기능을 확인한다. 기존 빈 조건 테스트는 원문의 빈 값과 화면의 success 기본값을 각각 검사하도록 보정했다. 운영 파서 동작은 변경하지 않았다.

로그: build/activity-thread-after.log. 설치용 ZIP은 build/distributions/intellij-glueframework-1.0.0-SNAPSHOT.zip이다. buildPlugin 성공과 ZIP 내부 JAR 일치 및 잠금 가드 포함 여부를 확인했다.

실제 다른 PC의 IDE를 실행한 수동 검증은 수행하지 않았다. 자동 검증은 프로젝트의 IntelliJ 2025.3.6.1 테스트 환경에서 수행했다.
