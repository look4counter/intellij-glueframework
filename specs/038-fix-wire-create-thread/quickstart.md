# 검증 안내

JDK 21, 로컬 Gradle 9.7.1 및 기존 오프라인 캐시로 ModelActivityCreationTest, ModelEditorFeaturesTest, ModelDiagram 계열, ModelServiceDirectEditorTest를 실행한다. 잠금 없는 클릭·드래그 추가, 양 끝 참조, 실행 취소·다시 실행과 지연 취소를 확인한다. 특정 PC의 실제 IDE에서는 Wire 추가를 반복해 오류 알림이 없는지 수동 확인한다.

## 자동 검증 결과 — 2026-11-18

- 로컬 JBR 21·Gradle 9.7.1·기존 오프라인 캐시로 실행했다.
- 수정 전 신규 Wire 회귀 4개가 모두 `Access is allowed from write thread only`로 실패했다. 스택은 createWire의 commitDocument 경로를 포함한다. 로그: build/wire-create-before.log.
- 수정 후 ModelActivityCreationTest 10개는 실패·오류·건너뜀 없이 통과했다. 클릭 및 놓기 연결, 양 끝 참조, 한 번의 실행 취소·다시 실행, 모드 취소·읽기 전용·폐기와 문서 변경 중 원문 보존을 확인했다.
- 생성·편집 기능·ModelDiagram 전체 계열·Service 직접 편집 총 213개 중 212개 통과, 기존 툴바 검증 1개 실패. 로그: build/wire-create-final.log. 결과 보존: .gradle-validation/wire-create-final-results.
- 실패는 ModelDiagramPropertiesTest.testCreationControlUsesNativeActionToolbarAndCustomControl의 849행이다. 아이콘 null을 기대하지만 기존 activity-add.svg가 설정되어 있다. 수정 전 소스와 git diff가 없는 상태로 해당 테스트만 실행해 동일 실패를 확인했다. 로그: build/wire-create-baseline-toolbar.log. Wire 수정으로 추가된 실패는 없다.
- 재현 명령: 아래에서 테스트 선택 인수를 위 클래스 또는 실패 메서드로 바꾸어 실행한다.

```powershell
$env:GRADLE_USER_HOME = "$PWD/.gradle-user-home"
$wireJbr = "$PWD/.gradle-user-home/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr"
$env:JAVA_HOME = $wireJbr
& .gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.ModelActivityCreationTest --tests com.look4counter.ModelEditorFeaturesTest --tests 'com.look4counter.ModelDiagram*' --tests com.look4counter.ModelServiceDirectEditorTest --offline --no-configuration-cache -I .gradle-validation/press-position.init.gradle "-Porg.gradle.java.installations.paths=$wireJbr"
```

특정 PC의 실제 IDE에서 수동 재현 및 배포 ZIP 생성은 수행하지 않았다.

최종 코드 복원 후 ModelActivityCreationTest 10개를 다시 실행해 BUILD SUCCESSFUL을 확인했다. 로그: build/wire-create-focused-final.log.

## 수렴 점검

FR-001~003, SC-001~002, 인수 시나리오 4개와 계획의 구현·검증 결정을 현재 코드와 대조했다. 최초 stamp 캡처, commitDocument 이전 예약, 폐기·유효성 보호, 기존 취소 조건, XML 및 실행 취소를 확인했다. 헌장 규범은 미작성 템플릿이므로 생략했다. Wire 추가 범위의 누락·부분 구현·모순·미요청 변경은 0건이며 수렴 작업 추가는 없다. 기존 툴바 아이콘 기대 불일치는 위 기준선 검증 결과로 별도 기록한다.

## 삭제·복사·붙여넣기 후속 검증 — 2026-11-18

명세 FR-004~006 및 SC-003을 추가하고 명확화·계획·작업 생성·분석·구현 순서로 수행했다. 품질 체크리스트 8개 항목은 갱신 명세에서도 모두 통과한다. 분석 결과 요구사항 6개와 성공 기준 3개가 작업에 연결되며 충돌·모호성·미연결 작업은 없다.

수정 전 신규 회귀 5개가 쓰기 스레드 오류로 실패했다. 로그: build/clipboard-thread-before.log. 수정 후 아래 테스트 53개가 실패·오류·건너뜀 없이 모두 통과했고 BUILD SUCCESSFUL을 확인했다. 로그: build/clipboard-thread-final.log.

- ModelDiagramClipboardTest 25개: 신규 5개 및 기존 연결 참조·XML·오프셋·삭제·실행 취소 회귀.
- ModelActivityCreationTest 10개: 이전 Wire 추가 보호 포함.
- EditorOpenThreadTest 11개: 잠금 없는 생성·열기 보호.
- ModelServiceDirectEditorTest 7개: Service 편집 회귀.

신규 검증은 잠금 없는 세 provider 호출, 문서·선택·모드·폐기·읽기 전용 변경 및 클립보드 교체를 포함한다. 읽기 전용 복사는 허용하고 삭제·붙여넣기는 취소한다. 삭제·붙여넣기의 단일 실행 취소와 다시 실행도 확인했다.

실행 환경과 명령은 앞선 검증 명령과 동일하며 테스트 선택을 다음 네 항목으로 지정했다.

```text
--tests com.look4counter.ModelDiagramClipboardTest --tests com.look4counter.ModelActivityCreationTest --tests com.look4counter.EditorOpenThreadTest --tests com.look4counter.ModelServiceDirectEditorTest
```

후속 수렴 점검: FR-004~006, SC-003, 사용자 이야기 2의 인수 시나리오 4개, 계획의 예약·스냅샷·활성 상태 재검증·기존 XML 보존 결정을 코드와 대조했다. 지연 람다는 최초 문서 시점과 선택을 유지하고 문서 커밋 전에 실행 문맥 및 상태를 검증한다. 누락·부분 구현·모순·미요청 변경 0건으로 수렴 작업 추가는 없다. 헌장은 미작성 템플릿이므로 규범 검사를 생략했다. diff 공백 검사 통과. 실제 IDE 수동 조작과 ZIP 재생성은 수행하지 않았다.
