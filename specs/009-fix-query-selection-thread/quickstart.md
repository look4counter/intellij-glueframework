# 검증 안내

잠금 없는 EDT에서 목록 선택, 설명·SQL 편집 직후 선택, 빠른 연속 선택, 편집기 폐기를 검사한다. QueryDesignEditorTest와 QuerySqlEditorTest 전체를 실행한다.

```powershell
& ./.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.QueryDesignEditorTest --tests com.look4counter.QuerySqlEditorTest --offline -g .gradle-user-home --no-daemon --no-configuration-cache '-Dorg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I build/state-wire-check/local-repo.init.gradle
```

## 검증 결과

2026-10-26: 수정 전 잠금 없는 목록 선택과 대기 편집 후 선택 테스트 2개에서 Access is allowed from write thread only 오류를 동일하게 재현했다. 증거는 build/query-selection-before-results.xml과 build/query-selection-before.log에 있다.

수정 후 QueryDesignEditorTest 27개, QuerySqlEditorTest 13개, ModelEditorServiceExportTest 10개로 총 50개가 모두 통과했다. 실패·오류·건너뜀은 0개다. 잠금 없는 선택, 설명·SQL 대기 편집의 원래 대상 보존, 연속 선택의 최종 대상 유지, 쓰기 안전하지 않은 문맥, 예약 후 폐기를 검증했다. 기존 저장·실행 취소·읽기 전용·SQL 편집 검증도 통과했다.

buildPlugin이 성공했고 ZIP 내부의 Query 및 모델 변환 클래스와 빌드 JAR 일치를 확인했다. 설치용 파일은 build/distributions/intellij-glueframework-1.0.0-SNAPSHOT.zip이다. 직전 Activity 쓰기 스레드 수정과 비정상 Wire 제외 저장도 포함한다. 로그는 build/query-selection-after.log다.

실제 다른 PC의 IDE를 조작하는 수동 검증은 수행하지 않았다. 자동 검증은 프로젝트 IntelliJ 2025.3.6.1 환경에서 수행했다.
