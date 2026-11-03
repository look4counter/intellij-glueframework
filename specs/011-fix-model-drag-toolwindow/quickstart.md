# 검증 안내

쓰기 의도 잠금을 해제한 실제 마우스 드래그로 오류를 재현하고 수정 후 이동·크기 조절·Wire 편집과 실행 취소·저장을 검사한다. 놓기와 예약 실행 사이 원문 변경도 검사한다. ToolWindowManager에 공통 ID와 플러그인 ID를 같이 등록하고 기존 팩토리 콘텐츠 생성을 확인한다.

```powershell
& ./.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.ModelDiagramTest --tests com.look4counter.ModelDiagramPropertiesTest --tests com.look4counter.ToolWindowRegistrationTest buildPlugin -x buildSearchableOptions --offline -g .gradle-user-home --no-daemon --no-configuration-cache '-Dorg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I build/state-wire-check/local-repo.init.gradle
```

## 검증 결과

2026-10-26: 수정 전 잠금 없는 Child·Wire 드래그 테스트 2개에서 보고된 쓰기 스레드 오류를 재현했다. 증거는 build/model-drag-before-results.xml과 build/model-drag-before.log다.

수정 후 ModelDiagramTest 71개, ModelDiagramPropertiesTest 29개, ToolWindowRegistrationTest 1개로 총 101개가 모두 통과했다. 실패·오류·건너뜀은 0개다. 실제 마우스 이벤트의 이동·너비 조절·꺾임점 변경과 실행 취소·재실행·저장, 예약 후 원문 변경 및 다음 제스처 시작 시 취소를 확인했다. 기존 읽기 전용 전환 테스트로 취소 후 미리보기 복구도 확인했다. 로그는 build/model-drag-after.log다.

Tool Window는 플러그인 ID 접두사를 가진 고유 ID인지 검증하고, ToolWindowManager에 MyToolWindow와 새 ID를 함께 등록하여 콘텐츠를 생성했다. ZIP의 plugin.xml에서 com.look4counter.intellij-glueframework.tools와 기존 Factory, 새 stripe 키와 클래스 포함 여부를 확인했다. 패키징된 JAR과 빌드 JAR이 일치한다. 설치용 ZIP은 build/distributions/intellij-glueframework-1.0.0-SNAPSHOT.zip이다.

환경 제한: 검색 옵션 수집용 IDE 보조 프로세스가 DirectoryLock의 소켓 bind 권한 거부로 종료되어 해당 재생성 작업만 제외했다. 변경과 무관한 기존 검색 메타데이터 JSON을 재사용했고 ZIP에 포함된 것을 확인했다. 실제 사용자 PC에서 IDE를 실행하는 수동 검증은 수행하지 않았다.
