# 검증 안내

실제 버튼 클릭으로 추가·삭제·복제·이동을 확인한다. 추가 입력과 취소는 프로젝트 대화상자 서비스를 대체하여 검증하고 실제 입력 패널의 검증도 검사한다. Source XML의 순서·속성·CDATA·주석·확장 요소 보존과 실행 취소·재실행, 읽기 전용·잘못된 XML·잠금 없는 버튼을 검사한다.

```powershell
& ./.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.QueryDesignEditorTest --tests com.look4counter.QuerySqlEditorTest buildPlugin --offline -g .gradle-user-home --no-daemon --no-configuration-cache '-Dorg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I build/state-wire-check/local-repo.init.gradle
```

## 검증 결과

2026-10-26: QueryDesignEditorTest 33개와 QuerySqlEditorTest 13개, 총 46개가 모두 통과했다. 실패·오류·건너뜀은 0개다. 로그는 build/query-list-actions-test.log다.

실제 버튼의 아이콘 참조·툴팁·접근성 이름, 추가 확인·취소·중복 ID 방지·네 필드 저장, 삭제·복제·순서 이동과 각 작업의 실행 취소·재실행을 검증했다. 대기 편집·복제 ID 충돌·CDATA·내부 주석·확장 내용 보존, 잠금 없는 클릭, 읽기 전용·잘못된 XML·팝업 중 원문 변경 방지도 검사했다.

표시 검증에서 테스트 SDK의 기본 플랫폼 아이콘이 빈 대체 객체인 것을 확인했다. 버튼이 올바른 AllIcons 상수를 참조하는지 검사한 뒤 동일 SVG 리소스를 직접 읽어 각 아이콘의 표시 픽셀을 검증했다. .gradle-validation/query-list-actions.png와 query-creation-form.png를 직접 열어 다섯 아이콘 및 네 입력 항목의 배치를 확인했다. 실제 IDE 창을 조작한 수동 검증은 수행하지 않았다.

buildPlugin 성공과 ZIP 내부 JAR 일치·대화상자 클래스·서비스 등록을 확인했다. 설치용 ZIP은 build/distributions/intellij-glueframework-1.0.0-SNAPSHOT.zip이다.

## 버튼 크기 보완 검증

2026-10-26: 버튼의 선호·최소 크기를 배율 적용 24×24로 지정하고 기본 margin을 제거했다. 기존 아이콘 렌더링·추가 팝업·삭제·복제·이동·실행 취소 테스트 3개가 모두 통과했다. build/query-compact-buttons-test.log에 기록했다. 갱신된 query-list-actions.png를 직접 열어 버튼 다섯 개가 약 120 너비에 배치되고 아이콘이 잘리지 않는 것을 확인했다. buildPlugin으로 설치용 ZIP을 갱신했다.

## 최신 32×32 패딩 검증

2026-10-26: 최신 요청에 따라 아이콘 크기는 유지하고 버튼 크기를 32×32, margin을 배율 적용 4로 변경했다. 기존 렌더링 높이를 36으로 조정했다. 아이콘·팝업·삭제·복제·이동·실행 취소 검증 3개가 모두 통과했고 buildPlugin도 성공했다. 로그는 build/query-padded-buttons-test.log다. query-list-actions.png에서 다섯 버튼이 160 너비로 표시되고 아이콘 주위 패딩과 잘림 없는 표시를 직접 확인했다. 최신 ZIP의 JAR과 빌드 JAR도 일치한다.
