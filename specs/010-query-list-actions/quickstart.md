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

## 쿼리 목록 기본 Speed Search 검증 (2026-11-08)

쿼리 JBList에 IntelliJ SDK의 ListSpeedSearch.installOn을 연결했다. 목록에 포커스가 있을 때 글자를 입력하면 플랫폼 기본 검색 입력·항목 이동·검색 종료 동작을 사용한다. 검색 대상은 QueryEntry.toString이 제공하는 표시 ID이며 목록 필터링·재정렬이나 별도 검색 UI를 구현하지 않았다. renderer·패딩과 기존 selection listener는 유지했다.

명세·명확화·계획·작업 목록과 구현 전 분석을 수행했고 FR-007~FR-009는 T014~T016에 모두 대응한다. 질문은 없으며 플랫폼 기본 검색으로 위임한다는 범위를 명시했다. 기존 구현에서 새 검색 등록 검사가 실패한 뒤 구현했다. 구현 후 speckit-converge 대조에서 현재 목록 참조·기존 선택 경로·입력 범위·읽기 전용 탐색과 문서 비변경의 누락은 없었다.

로컬 Gradle 9.7.1·JBR 21 및 기존 임시 오프라인 Maven 설정으로 QueryDesignEditorTest 37개가 실패·오류·건너뜀 없이 통과했다. .gradle-validation/query-speed-search-tests.log 및 build/test-results/test/TEST-com.look4counter.QueryDesignEditorTest.xml이 근거다. 기본 공급자 조회는 활성 팝업만 반환하므로 설치 검사는 getSupply(list, true)를 사용했다. 일치 없음의 선택 처리는 플랫폼 기본 결과를 그대로 허용한다. 검색·읽기 전용 탐색·ID 갱신·빈 목록·SQL/속성 동기화·문서 비변경과 기존 목록 회귀를 검증했다.

앞선 .gradle-validation/shortcuts-query-search.log 실행에서는 QuerySqlEditorTest 47개와 모델 관련 43개가 통과했다. 해당 실행의 검색 테스트 기대값만 고친 뒤 QueryDesignEditorTest 전체를 재실행하여 완료했다. 컴파일과 git diff --check도 통과했다. 실제 IDE에서 키 입력·검색 팝업·Esc 조작은 수동 검증하지 않았다.
