# 검증 안내

ModelEditor를 처음 열고 툴바 초기화·반복 갱신·종류 및 프리셋 선택·생성 모드 전환·읽기 전용 전환·복수 에디터를 확인한다. 등록 컴포넌트를 초기화하는 오류가 없어야 한다.

ChildCreationToolbarTest와 기존 ModelDiagramPropertiesTest·ModelEditorCreationTraceTest·ModelEditorFeaturesTest·ModelEditorRefreshTest·ModelSnapSettingsTest·ModelServiceDirectEditorTest를 실행한다. 수정 전 재현과 수정 후 검증 로그, 새 IDE 로그 구간, 설치용 ZIP 결과는 구현 후 기록한다. 실제 사용자 IDE의 수동 검증과 자동 테스트를 구분한다.

## 자동 검증 결과

2026-10-27: 수정 전 ChildCreationToolbarTest의 등록 컴포넌트 반복 갱신 검사에서 사용자 보고와 같은 Trying to reset custom component in a presentation 오류 및 SplitButtonAction.update → Presentation.copyFrom 호출 경로를 재현했다. 로그는 build/child-toolbar-before.log, 결과는 build/child-toolbar-before-results.xml이다.

수정 후 ChildCreationToolbarTest 2개, ModelDiagramPropertiesTest 29개, ModelEditorCreationTraceTest 5개, ModelEditorFeaturesTest 15개, ModelEditorRefreshTest 12개, ModelSnapSettingsTest 4개, ModelServiceDirectEditorTest 10개로 총 77개가 모두 통과했다. 실패·오류·건너뜀은 0개다. 두 툴바의 등록된 컴포넌트 보존 10회 반복, 생성 모드 위임·종류·프리셋·선택 상태·읽기 전용 복원과 기존 생성·새로고침·저장 동작을 확인했다. 로그는 build/child-toolbar-after.log다.

검증 시작 전 IDE 로그의 파일 오프셋을 기록하고 이후 새 구간을 build/child-toolbar-fresh-idea.log로 추출했다. 약 380KB의 새 로그에서 보고된 컴포넌트 초기화 메시지, SplitButtonAction.update 호출 및 기존 생성 툴바 presentation 오류는 모두 0건이다. 검증 요약은 build/child-toolbar-validation.json이다. 로그를 억제하거나 컴포넌트 키를 제거하는 코드는 추가하지 않았다.

기존 텍스트 툴바 캡처 build/reports/model-text-toolbar.png를 확인했다. 실제 사용자 IDE에서 시작하는 수동 검증은 수행하지 않았다.

## 실행 명령

```powershell
& ./.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.ChildCreationToolbarTest --tests com.look4counter.ModelDiagramPropertiesTest --tests com.look4counter.ModelEditorCreationTraceTest --tests com.look4counter.ModelEditorFeaturesTest --tests com.look4counter.ModelEditorRefreshTest --tests com.look4counter.ModelSnapSettingsTest --tests com.look4counter.ModelServiceDirectEditorTest buildPlugin -x buildSearchableOptions --offline -g .gradle-user-home --no-daemon --no-configuration-cache '-Dorg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I build/state-wire-check/local-repo.init.gradle
```

## 설치용 산출물

build/distributions/intellij-glueframework-1.0.0-SNAPSHOT.zip을 생성했다. ZIP 내부 JAR에 CreationControlAction이 포함되고 ChildCreationToolbar 관련 클래스에서 기존 SplitButtonAction 의존이 제거된 것을 확인했다. 기존 환경의 검색 옵션 수집용 IDE 실행 제한에 따라 검색 옵션 재생성은 제외하고 기존 산출물을 사용했다.

## 최종 수렴 점검

기능 요구사항 4개·성공 기준 3개·인수 시나리오 4개와 커스텀 컴포넌트·표시 갱신·토글 위임 설계를 현재 코드와 테스트에 대조했다. 헌법은 미작성 템플릿으로 추가 원칙 검사를 생략했다. 누락·부분 구현·상충·범위 밖 변경은 없으며 추가 작업은 0개다.
