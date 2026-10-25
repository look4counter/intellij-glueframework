# 검증 안내: Resize 보정과 Magnetic 원복

## 검증 시나리오

- 양 상태 종류의 중심 방향 외곽 연결을 확인한다.
- Magnetic은 중심이 어긋난 양 Activity의 최근접 후보를 선택하는지 확인한다.
- Activity 좌우 확대·축소 및 시작/끝/양쪽 노드의 Resize 전후 꺾임점 절대 좌표를 비교한다.
- 미리보기와 저장 후 경로가 일치하고 저장·재열기·실행 취소·재실행에서 보정 값이 유지되는지 확인한다.
- weight와 사용자 속성 보존, 기존 이동과 직선 연결을 회귀 검증한다.

## 오프라인 실행

```powershell
& ./.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.ModelDiagramTest --tests com.look4counter.ModelEditorFeaturesTest --offline -g .gradle-user-home --no-daemon --no-configuration-cache '-Dorg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I build/state-wire-check/local-repo.init.gradle
```

로컬 초기화 스크립트는 사용자 캐시의 ELK·EMF를 임시 Maven 저장소로 해석하고 허용 버전 범위 안의 Guava 33.3.1-jre를 지정한다. 프로젝트 의존성 선언은 유지한다.
이전 검증에서 읽기 전용 전환 후 미리보기 복원 테스트 1개는 수정 전 코드에서도 같은 실패가 확인되었다. 이번 Resize·Magnetic 범위와 별도로 기록한다.

ModelDiagramAlignmentTest도 실행하고 Snap 명칭·꺼짐·켜짐·Alt·드래그 중 끄기·최소 너비를 확인한다.


## 최종 검증과 수렴 결과

최종 관련 테스트는 ModelDiagramTest·ModelEditorFeaturesTest·ModelDiagramAlignmentTest 총 97개 중 96개 통과다.
실패 1개는 변경 전에도 재현된 읽기 전용 전환 후 미리보기 복원 문제다.
Resize의 좌우·시작/끝·양쪽 상대 벡터·직선 기본 접점·미리보기·XML 저장·weight와 추가 속성 보존·실행 취소·재실행이 통과했다.
Snap 명칭·양 Activity 최근접 후보·켜짐/꺼짐 이동·Resize·드래그 중 끄기·Alt·최소 너비 검증이 통과했다.
소스 및 테스트에서 이전 Magnetic 용어가 남아 있지 않음을 확인했고 git diff --check가 통과했다.
로그는 build/state-wire-resize-snap-final.log다.

요구사항 8개, 성공 기준 5개와 사용자 이야기 4개를 현재 코드 및 테스트와 대조했다. 추가 미구현이나 모순은 없다.
헌장은 미작성 템플릿으로 검사를 생략했다. 별도 기존 실패 외에 이번 범위의 남은 작업은 없다.

## 전역 Snap 최종 검증

ModelSnapSettingsTest 4개, ModelEditorFeaturesTest 15개, ModelDiagramAlignmentTest 14개가 모두 통과했다.
새 애플리케이션 설정의 분리된 상태 복사본·직렬화와 복원·중복 알림 방지·구독 해제를 확인했다.
동시에 열린 여러 파일의 Canvas와 툴바 양방향 동기화, 새 파일과 닫은 파일 재열기, 다른 파일의 토글로 진행 중 보조선 정리가 통과했다.
상태는 서비스의 애플리케이션 범위와 애플리케이션 메시지 버스를 사용하여 프로젝트와 무관하게 공유한다.

같이 실행한 QuerySqlEditorTest 13개도 통과하여 총 46개 검증의 실패·오류는 0개다. 로그는 build/global-snap-zero-hints-test.log다.
이번 변경의 검증 명령에는 `--tests com.look4counter.ModelSnapSettingsTest --tests com.look4counter.ModelEditorFeaturesTest --tests com.look4counter.ModelDiagramAlignmentTest`를 사용했다.
프로젝트 선언을 바꾸지 않는 기존 로컬 의존성 초기화 스크립트와 Java 21 경로를 사용했다.
