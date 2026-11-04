# 검증 안내

1. JDK 21에서 `./gradlew.bat test --tests com.look4counter.ModelActivityCreationTest --tests "com.look4counter.ModelDiagram*" --tests com.look4counter.ModelEditorCreationTraceTest --tests com.look4counter.ModelEditorFeaturesTest --tests com.look4counter.ChildCreationToolbarTest`를 실행한다.
2. `./gradlew.bat buildPlugin`으로 설치용 ZIP을 생성한다.
3. 빈 Activity와 프리셋을 클릭 및 드래그로 추가해 오류가 없는지 확인하고 실행 취소 및 다시 실행을 확인한다.
4. Snap을 켜고 기존 도형 근처에서 Ghost와 안내선이 정렬되는지 확인한다. Snap 끄기와 Alt 우회, 동일 간격 및 확대·축소를 확인한다.
5. Ghost와 실제 추가된 위치가 일치하고 이탈·취소 후 안내선 잔상이 없는지 확인한다.

## 실행 결과 — 2026-11-04

- ModelActivityCreationTest 신규 6개를 포함하여 모델 다이어그램 계열, 생성 추적, 편집 기능, 도구 모음, Service 직접 편집 총 197개 테스트 통과, 실패 0개.
- 쓰기 의도 잠금 없는 클릭 및 놓기 추가를 실제 UI 이벤트로 검증했다. 프리셋 추가, 중복 방지, 모드·프리셋 변경 중 취소, 편집기 폐기, 한 번의 실행 취소 및 다시 실행도 통과했다.
- Ghost와 확정 좌표 일치, Snap 켜기·끄기·Alt, 정렬 안내선 정리, 동일 간격, 확대율 50%·100%·150%와 음수 모델 좌표를 검증했다.
- 기존 2,000개 노드 Ghost 부분 갱신·화면 비교 및 Service 편집 회귀 검증도 통과했다.
- buildPlugin 성공. 설치 파일: build/distributions/intellij-glueframework-1.0.1.zip.
- 환경: JDK 21.0.6, 작업 폴더 내 Gradle 9.7.1, 기존 .gradle-validation/ghost-offline.init.gradle을 사용한 오프라인 검증. 프로젝트 의존성 설정 변경 없음.
