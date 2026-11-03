# 검증 안내

ModelEditorServiceExportTest에서 끝점 누락·중복 참조·범위 밖 참조·금지 방향·모호한 시작점을 검증한다. 실제 문서 저장으로 생성된 Service 파일 및 열린 출력 문서를 확인한다. 기존 정상 출력 순서와 연결 외 오류 보호를 재검증한다.

```powershell
& ./.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.ModelEditorServiceExportTest --offline -g .gradle-user-home --no-daemon --no-configuration-cache '-Dorg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I build/state-wire-check/local-repo.init.gradle
```

## 검증 결과

2026-10-26: ModelEditorServiceExportTest 10개 통과, 실패·오류 0개. 끝점 누락·양방향 중복·여러 Child의 중복 참조·잘못된 State 방향·범위 밖 및 잘못된 토큰·모호한 시작점을 검사했다. 정상 모델의 정확한 출력 순서와 반복 결과, 실제 저장 이벤트의 파일·열린 출력 문서 갱신 및 원문 보존, 연결 외 오류에서 기존 출력 보존도 통과했다.

로그는 build/query-selection-before.log이며 Query 오류 재현 테스트 2개의 예상 실패와 함께 실행했다. Service 저장 테스트 10개 자체는 모두 통과했다. 새 통합 테스트의 디스크 비교는 Windows 줄바꿈을 정규화한다. 실제 IDE 수동 검증은 수행하지 않았다.

최종 통합 실행에서도 Service 저장 테스트 10개와 Query 관련 40개가 모두 통과했다. build/query-selection-after.log를 확인한다. 최신 설치용 ZIP에 두 변경을 포함했다.
