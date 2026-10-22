# 작업 목록: 편집기 모듈 분리

**입력**: [명세](spec.md), [계획](plan.md), [조사](research.md), [계약](contracts/file-types.md)

## 1단계: 준비

- [x] T001 settings.gradle.kts에 queryeditor와 modeleditor 및 모듈 플러그인 버전을 등록한다. (FR-001)

## 2단계: 기반 구성

- [x] T002 queryeditor/build.gradle.kts와 modeleditor/build.gradle.kts에 모듈 빌드를 구성한다. (FR-001, FR-005)

## 3단계: 사용자 이야기 1 — 통합 배포

**독립 검증**: 두 모듈을 컴파일하고 단일 배포 ZIP의 모듈 포함 여부를 확인한다.

- [x] T003 [US1] build.gradle.kts에 두 모듈의 조립 의존성을 추가하고 src/main/resources/META-INF/plugin.xml에서 모듈별 등록 조각을 포함한다. (FR-002, FR-006)

## 4단계: 사용자 이야기 2 — 확장자 처리

**독립 검증**: 플랫폼 테스트에서 두 확장자와 비대상 확장자 및 텍스트 편집을 확인한다.

- [x] T004 [US2] src/test/kotlin/com/look4counter/EditorFileTypeTest.kt에 파일 형식 등록 및 빈 파일 편집·저장 통합 테스트를 작성한다. (FR-003, FR-004, FR-005)
- [x] T005 [P] [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/GlueSqlFileType.kt 및 queryeditor/src/main/resources/META-INF/queryeditor.xml을 구현한다. (FR-003)
- [x] T006 [P] [US2] modeleditor/src/main/kotlin/com/look4counter/modeleditor/GlueUmlAdFileType.kt 및 modeleditor/src/main/resources/META-INF/modeleditor.xml을 구현한다. (FR-004)

## 5단계: 문서와 검증

- [x] T007 README.md를 한글로 갱신하여 구조와 개발·배포 명령을 안내한다. (FR-006)
- [x] T008 specs/001-split-editor-modules/validation.md에 컴파일·테스트·배포 검증 결과와 환경 제한을 기록한다. (SC-001, SC-002, SC-003)

## 의존성과 실행 순서

T001 → T002 → T003 → T004 → T005/T006 → T007 → T008 순서로 진행한다. T005와 T006은 서로 다른 파일을 수정하므로 병렬 진행이 가능하다. 통합 배포 기반을 먼저 만든 뒤 두 확장자 처리를 추가하여 전체 기능을 검증한다. 테스트는 구현 전 실행을 시도하여 실패 원인을 확인한다.

## 6단계: 수렴

- [x] T009 build.gradle.kts에 루트 테스트의 모듈 컴파일 의존성을 명시하고 통합 테스트를 다시 실행하여 specs/001-split-editor-modules/validation.md를 갱신한다. (SC-003, 부분 구현)

## 7단계: 수렴 — 환경에서 남은 검증

- [x] T010 네트워크 사용이 가능한 환경에서 test를 실행하여 파일 형식 연결·비대상 확장자·빈 파일 편집 및 저장을 확인하고 specs/001-split-editor-modules/validation.md에 결과를 기록한다. (SC-003, 검증 완료: Bundled 프레임워크로 오프라인 실행 성공)

## 8단계: 멀티뷰 기반 및 XML 인식

- [x] T011 [US4] src/test/kotlin/com/look4counter/EditorFileTypeTest.kt와 EditorMultiViewTest.kt에 XML 인식·편집 기능 및 멀티뷰 통합 검증을 추가하고 구현 전 실패를 확인한다. (FR-007~FR-013)
- [x] T012 [US4] 두 모듈의 GlueSqlFileType.kt, GlueUmlAdFileType.kt 및 META-INF XML을 XML 언어 파일 형식으로 변경하고 src/main/resources/META-INF/plugin.xml에 XML 모듈 의존성을 선언한다. (FR-009, FR-012)

## 9단계: 사용자 이야기 3 — Design/Source 멀티뷰

**독립 검증**: 두 확장자의 제공자 목록과 편집기 이름·순서 및 다른 확장자의 비수락을 확인한다.

- [x] T013 [P] [US3] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditorProvider.kt와 QuerySourceEditorProvider.kt를 구현하고 queryeditor/src/main/resources/META-INF/queryeditor.xml에 등록한다. (FR-007, FR-008, FR-010, FR-011, FR-013)
- [x] T014 [P] [US3] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt와 ModelSourceEditorProvider.kt를 구현하고 modeleditor/src/main/resources/META-INF/modeleditor.xml에 등록한다. (FR-007, FR-008, FR-010, FR-011, FR-013)

## 10단계: 사용자 이야기 4 — XML 소스 편집 검증

**독립 검증**: XML 문서 분석·구문 강조·완성·서식·오류 탐지와 편집·탭 전환·실행 취소·다시 실행·저장·해제를 확인한다.

- [x] T015 [US4] src/test/kotlin/com/look4counter/EditorMultiViewTest.kt에서 제공자 상태 직렬화와 문서·자원 수명 검증을 완성하고 플랫폼 테스트를 실행한다. (FR-009, FR-011, FR-012)

## 11단계: 문서 및 배포 검증

- [x] T016 README.md와 specs/001-split-editor-modules/quickstart.md를 멀티뷰와 XML 기능 범위에 맞춰 갱신한다. (FR-006~FR-013)
- [x] T017 두 모듈과 루트 테스트를 컴파일하고 buildPlugin을 실행하여 배포 JAR의 파일 형식·제공자·등록·XML 의존성을 확인한 결과를 specs/001-split-editor-modules/validation.md에 기록한다. (SC-001~SC-006)

## 멀티뷰 확장 실행 순서

T011 → T012 → T013/T014 → T015 → T016 → T017 순서다. T013과 T014는 별도 모듈이므로 병렬 구현할 수 있으나 이번 구현은 순서대로 처리한다. 기존 T010은 실제 플랫폼 테스트가 성공할 때만 완료로 갱신한다.

## 12단계: 로컬 검증 보완

- [x] T018 build.gradle.kts에서 대상 IDE의 Bundled 테스트 프레임워크를 사용하여 플랫폼 테스트 실행을 검증하고 결과를 specs/001-split-editor-modules/validation.md에 기록한다. (SC-003~SC-006)

## 13단계: 사용자 이야기 5 — queryMap 조회·선택

**독립 검증**: 사용자 샘플의 각 query 선택에 목록·속성·SQL이 일치한다.

- [x] T019 [US5] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt에 샘플 조회·선택·빈/중복 id·크기 조절 및 편집 통합 테스트를 먼저 추가하고 실패를 확인한다. (FR-014~FR-023)
- [x] T020 [US5] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryMapDocumentModel.kt에 XML 스냅샷과 안전한 선택 태그 수정 기능을 구현한다. (FR-014, FR-018~FR-020, FR-022)
- [x] T021 [US5] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에 설명·입력·목록과 선택 동기화 UI를 구현하고 QueryDesignEditorProvider.kt에서 연결한다. (FR-014~FR-016, FR-022)

## 14단계: 사용자 이야기 6 — SQL·속성 편집

**독립 검증**: 선택 쿼리의 SQL과 네 속성 변경, Source 동기화, 실행 취소·저장 및 보존 검증이 통과한다.

- [x] T022 [US6] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QuerySqlEditor.kt에 문법 없는 다중 행 SQL 편집 컴포넌트를 구현한다. (FR-017)
- [x] T023 [US6] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에 태그 참조 기반 대기 수정·쿼리별 버퍼·Source 동기화·읽기 전용·자원 해제를 연결한다. (FR-018~FR-020, FR-022, FR-023)

## 15단계: 사용자 이야기 7 — 너비 조절

**독립 검증**: 오른쪽 320픽셀 선호·220픽셀 최소 너비와 사용자 분할 조절을 확인한다.

- [x] T024 [US7] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에 OnePixelSplitter와 사이드바 크기 기준을 적용한다. (FR-021)

## 16단계: 쿼리 Design 검증 및 문서

- [x] T025 README.md와 specs/001-split-editor-modules/quickstart.md에 쿼리 Design 사용 방법과 제한을 반영한다. (FR-014~FR-023)
- [x] T026 전체 플랫폼 테스트와 buildPlugin을 실행하고 specs/001-split-editor-modules/validation.md에 결과와 배포 내용 검증을 기록한다. (SC-001~SC-010)

## 쿼리 Design 실행 순서

T019 → T020 → T021/T022 → T023 → T024 → T025 → T026이다. T021의 화면과 T022의 SQL 컴포넌트는 별도 파일이므로 병렬 구현 가능하나 연결 전에는 독립 검증한다. 문서 조회·선택을 먼저 구축하고 수정·동기화·보존 기능을 추가한 뒤 전체 회귀 테스트로 완료한다.

## 17단계: 사용자 이야기 8 — 설명 편집과 화면 정리

**독립 검증**: 사용자 이야기 8의 네 인수 시나리오를 확인한다.

- [x] T027 [US8] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt에 루트 설명 편집·동기화·저장·쓰기 제한과 배치·개수 미표시 검증을 추가한다. (FR-024~FR-026)
- [x] T028 [US8] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryMapDocumentModel.kt에 루트 참조와 desc 전용 수정을 연결한다. (FR-024)
- [x] T029 [US8] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에 설명 입력·동기화·체크박스 이동·상태 표시를 구현한다. (FR-024~FR-026)
- [x] T030 README.md와 specs/001-split-editor-modules/quickstart.md를 갱신하고 전체 테스트·빌드 결과를 specs/001-split-editor-modules/validation.md에 기록한다. (SC-011)

실행 순서: T027 → T028 → T029 → T030. 같은 화면 경로이므로 순차 구현한다. 최소 제공 단위는 루트 설명 편집이며 세 변경을 함께 검증한다. 별도 병렬 작업은 없다.

## 18단계: 사용자 이야기 9 — 라벨과 Java 클래스 완성

**독립 검증**: 네 라벨과 동명 클래스 후보의 전체 이름 삽입 및 기존 편집 동작을 검증한다.

- [x] T031 [US9] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt에 라벨·클래스 후보·전체 이름 삽입·직접 입력·쓰기 제한 회귀 검증을 추가한다. (FR-027~FR-029)
- [x] T032 [US9] build.gradle.kts, queryeditor/build.gradle.kts, src/main/resources/META-INF/plugin.xml에 Java 번들·설치 의존성을 선언한다. (FR-028)
- [x] T033 [US9] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryResultTypeEditor.kt에 Java 클래스 참조 입력을 구현하고 QueryDesignEditor.kt에 수정·동기화·수명과 표시 라벨을 연결한다. (FR-027~FR-029)
- [x] T034 README.md와 specs/001-split-editor-modules/validation.md에 기능·환경 전제와 전체 test buildPlugin 결과를 기록한다. (SC-012)

순서: T031 → T032 → T033 → T034. 최소 제공 단위는 라벨 변경이며 클래스 완성과 함께 회귀 검증한다. 공통 입력과 테스트를 수정하므로 순차 실행하고 별도 병렬 작업은 두지 않는다.

## 19단계: 사용자 이야기 10 — 파일 형식 아이콘

**독립 검증**: 두 SVG의 원·색·흰색 문자 도형과 설치 배포 리소스를 확인한다.

- [x] T035 [US10] queryeditor/src/main/resources/icons/glueSql.svg와 GlueSqlFileType.kt에 하늘색 원·흰색 Q 파일 아이콘을 구현한다. (FR-031)
- [x] T036 [US10] modeleditor/src/main/resources/icons/glueUmlAd.svg와 GlueUmlAdFileType.kt에 연주황 원·흰색 M 파일 아이콘을 구현한다. (FR-030)
- [x] T037 README.md와 specs/001-split-editor-modules/validation.md에 아이콘 사용 및 기존 파일 형식 테스트·빌드·배포 리소스 검증 결과를 기록한다. (FR-032, SC-013)

T035 → T036 → T037 순서로 실행한다. 두 모듈 구현은 독립 파일이므로 병렬 처리 가능하지만 이번에는 순차 처리한다. 두 아이콘을 함께 배포하는 것이 최소 제공 범위다.

## 20단계: 사용자 이야기 11 — 속성 입력 외형 통일

**독립 검증**: 최종 라벨·무텍스트 체크박스·입력 스타일을 비교하고 기존 자동완성·편집 검증을 통과한다.

- [x] T038 [US11] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt의 기존 라벨과 입력 배치 검증을 최종 표시·스타일·접근성 기준으로 갱신한다. (FR-033~FR-035)
- [x] T039 [US11] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에 최종 라벨과 무텍스트 체크박스를 적용하고 QueryResultTypeEditor.kt의 스타일을 기준 입력과 통일한다. (FR-033~FR-035)
- [x] T040 README.md와 specs/001-split-editor-modules/validation.md에 최종 표시 및 자동완성 유지와 test buildPlugin 검증 결과를 기록한다. (SC-014)

T038 → T039 → T040 순서로 진행한다. 동일 화면과 검증 경로이므로 순차 실행하며 세 요청을 함께 제공한다. 새 테스트 파일은 만들지 않고 기존 동작 검증을 재사용한다.

## 21단계: 사용자 이야기 12 — ResultType 실제 표시 보정

**독립 검증**: 실제 입력 컴포넌트의 중앙 정렬·좌우 여백·일반 및 포커스 테두리를 렌더링으로 확인한다.

- [x] T041 [US12] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt의 기존 스타일 검증을 실제 배치·일반/포커스 렌더링 검증으로 보완한다. (FR-036~FR-038)
- [x] T042 [US12] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryResultTypeEditor.kt에 외부 테두리·패딩·내부 줄 중앙 배치와 포커스 갱신을 구현한다. (FR-036~FR-038)
- [x] T043 README.md와 specs/001-split-editor-modules/validation.md에 실제 표시 보정 및 이미지 확인·회귀 테스트·배포 빌드 결과를 기록한다. (SC-015)

T041 → T042 → T043 순서이며 같은 표시 경로를 수정하므로 순차 진행한다. 최소 제공 범위는 네 가지 표시 보정 전체와 기존 자동완성 유지다.

## 22단계: 사용자 이야기 13 — ResultType 치수 미세 조정

- [x] T044 [US13] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryResultTypeEditor.kt와 QueryDesignEditor.kt에 높이·내부 패딩·오른쪽 이동을 반영하고 src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt의 기존 치수 기대값을 갱신한다. (FR-039~FR-041)
- [x] T045 README.md와 specs/001-split-editor-modules/validation.md에 최종 치수 및 렌더링·회귀·배포 결과를 기록한다. (SC-016)

T044 → T045 순서로 진행한다. 기존 배치·렌더링·클래스 완성 테스트로 독립 검증하며 같은 입력을 수정하므로 순차 진행한다.

## 23단계: 사용자 이야기 14 — 왼쪽 패딩

- [x] T046 [US14] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryResultTypeEditor.kt의 왼쪽 내부 패딩을 3픽셀로 조정하고 src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt의 기존 배치 기대값을 갱신한다. (FR-042~FR-043)
- [x] T047 [US14] 기존 쿼리 편집 테스트를 실행하고 specs/001-split-editor-modules/validation.md에 검증 결과를 기록한다. (SC-017)

T046 → T047 순서로 실행한다. 동일 표시 경로이므로 순차 수행한다. 최소 제공 범위는 왼쪽 패딩 조정과 기존 기능 검증이다.

## 24단계: 사용자 이야기 15 — 입력 외형 일치

- [x] T048 [US15] QueryResultTypeEditor.kt와 QueryDesignEditor.kt에서 ID 기준 테두리·높이·여백·그리드 정렬을 적용한다. (FR-044~FR-045)
- [x] T049 [US15] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt의 기존 배치 검증을 세 입력 비교 캡처로 보완하고 테스트 실행 후 이미지를 확인한다. (FR-046, SC-018)
- [x] T050 [US15] specs/001-split-editor-modules/validation.md에 결과를 기록한다. (SC-018)

T048 → T049 → T050 순차 실행한다. 최소 제공 범위는 세 입력 정렬과 비교 캡처다.

## 25단계: 사용자 이야기 16 — 어두운 테마 여백 복구

- [x] T051 [US16] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryResultTypeEditor.kt에서 기준 입력의 실제 인셋과 전용 JBTextField 렌더링으로 배경·테두리·여백을 복구한다. (FR-047~FR-048)
- [x] T052 [US16] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt에서 밝은 테마·Darcula 테마의 비교 캡처와 경계·여백·테두리 검증 및 기존 회귀 테스트를 실행한다. (FR-049, SC-019)
- [x] T053 [US16] 테마별 캡처를 직접 확인하고 specs/001-split-editor-modules/validation.md에 결과를 기록한다. (SC-019)

T051 → T052 → T053 순차 수행한다. 두 테마에서 실제 입력 외형을 복구하고 검증하는 것이 최소 제공 범위다.

## 26단계: 사용자 이야기 17 — 쓰기 안전 갱신

- [x] T054 [US17] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt에서 쓰기 비안전 문맥의 생성·활성화·예약 직후 폐기를 재현하는 회귀 검증을 추가한다. (FR-050~FR-052)
- [x] T055 [US17] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryMapDocumentModel.kt의 조회 커밋을 제거하고 QueryDesignEditor.kt의 생명주기 갱신을 명시적 non-modal 예약으로 이동한다. 기존 테스트의 이벤트 대기를 조정한다. (FR-050~FR-052)
- [x] T056 [US17] 전체 테스트와 배포 빌드를 실행하고 specs/001-split-editor-modules/validation.md에 재현·수정·검증 결과를 기록한다. (SC-020)

T054 → T055 → T056 순서로 수행한다. 초기 생성과 탭 전환 모두를 포함한 수정이 최소 제공 범위다.

## 27단계: 사용자 이야기 18 — 저장 시 SQL 공백 정리

- [x] T057 [US18] src/test/kotlin/com/look4counter/QuerySaveListenerTest.kt에 Source 저장 정리·디스크·안전성 회귀 검증을 추가하고 QueryDesignEditorTest.kt에 Design 저장 동기화·실행 취소 검증을 추가한다. (FR-053~FR-055)
- [x] T058 [US18] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QuerySaveListener.kt와 META-INF/queryeditor.xml에 저장 리스너를 구현·등록하고 QueryMapDocumentModel.kt의 CDATA 직렬화 함수를 재사용한다. (FR-053~FR-055)
- [x] T059 [US18] 전체 테스트와 배포 빌드를 실행하고 README.md 및 specs/001-split-editor-modules/contracts/query-design.md, quickstart.md, validation.md에 저장 동작과 결과를 기록한다. (SC-021)

T057 → T058 → T059 순차 진행한다. 두 편집 탭의 저장 정리와 XML 보존 검증 전체가 최소 제공 범위다.

## 28단계: 사용자 이야기 19 — 파일 열기 공백 정리

- [x] T060 [US19] src/test/kotlin/com/look4counter/QueryOpenListenerTest.kt에 실제 열기·화면 반영·미저장 상태·디스크 보존·안전 문맥·제외 규칙 회귀 검증을 추가한다. (FR-056~FR-058)
- [x] T061 [US19] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QuerySqlNormalizer.kt로 저장 정리를 분리하고 QuerySaveListener.kt와 QueryOpenListener.kt에서 재사용하며 META-INF/queryeditor.xml에 파일 열기 리스너를 등록한다. (FR-056~FR-058)
- [x] T062 [US19] 전체 테스트와 배포 빌드를 실행하고 README.md 및 specs/001-split-editor-modules/contracts/query-design.md, quickstart.md, validation.md에 열기 동작과 결과를 기록한다. (SC-022)

T060 → T061 → T062 순차 수행한다. 파일 열기와 기존 저장 양쪽에서 동일한 정리 동작을 유지하는 것이 최소 제공 범위다.

## 29단계: 사용자 이야기 20 — SQL 강조와 완성

- [x] T063 [US20] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 실제 SQL 편집기 토큰·색상·완성 삽입·제외 문맥·선택 및 실행 취소 회귀 검증과 밝은/어두운 테마 캡처를 추가한다. (FR-059~FR-064)
- [x] T064 [US20] queryeditor/src/main/resources/sql/dbeaver-keywords.txt와 licenses/에 고정 출처 키워드 및 라이선스·고지를 추가하고 queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlKeywords.kt, SqlLexer.kt, SqlSyntaxHighlighter.kt를 구현한다. (FR-059~FR-060, FR-064)
- [x] T065 [US20] 같은 sql 경로의 SqlKeywordCompletionProvider.kt, SqlDocuments.kt를 구현하고 QuerySqlEditor.kt와 QueryDesignEditor.kt에 문서·강조·키워드 완성을 연결한다. (FR-061~FR-063)
- [x] T066 [US20] 테마별 PNG를 확인하고 전체 테스트·배포 리소스를 검증한 뒤 README.md 및 specs/001-split-editor-modules/contracts/query-design.md, data-model.md, quickstart.md, research.md, validation.md에 최종 동작·출처·검증 결과를 기록한다. (SC-023)

T063 → T064 → T065 → T066 순차 진행한다. 키워드 강조와 세 가지 색상 및 키워드 완성을 함께 제공하는 것이 최소 범위이며 SQL 실행·스키마 완성은 추가하지 않는다.

## 30단계: 사용자 이야기 21 — 파라미터 색상과 여백

- [x] T067 [US21] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt와 QueryDesignEditorTest.kt에 파라미터 토큰·완성 제외·목록 셀 여백·줄 번호 오른쪽 공간 및 테마 캡처 검증을 추가한다. (FR-065~FR-067)
- [x] T068 [US21] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlLexer.kt, SqlSyntaxHighlighter.kt, QuerySqlEditor.kt, QueryDesignEditor.kt에 cyan 파라미터·목록 8픽셀 여백·gutter 8픽셀 예약을 구현한다. (FR-065~FR-067)

## 31단계: 사용자 이야기 22 — DataSource 설정

- [x] T069 [US22] src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt에 설정 트리·다중 항목 CRUD·검증·직렬화·초안 취소·자격 증명 분리와 설정 화면 캡처 검증을 추가한다. (FR-068~FR-071)
- [x] T070 [US22] queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/DataSourceSettingsService.kt와 DataSourcePasswordManager.kt에 프로젝트 메타데이터 영속화와 배경 자격 증명 처리를 구현한다. (FR-069, FR-071)
- [x] T071 [US22] 같은 settings 경로의 GlueFrameworkConfigurable.kt, GlueQueryEditorConfigurable.kt, DataSourceSettingsPanel.kt 및 META-INF/queryeditor.xml에 설정 트리와 초안 기반 관리 화면을 구현한다. (FR-068~FR-071)
- [x] T072 [US22] 표시·설정 캡처를 직접 확인하고 전체 테스트·배포 빌드를 실행한 뒤 README.md와 specs/001-split-editor-modules의 계약·모델·검증 문서에 결과를 기록한다. (FR-072, SC-024~SC-025)

T067 → T068 → T069 → T070 → T071 → T072 순차 진행한다. 저장 범위는 사용자 선택으로 프로젝트별 관리이며, 표시 개선 세 항목과 다중 등록 설정을 함께 제공한다.
## 32단계: 사용자 이야기 23 — DB 메타데이터 완성

- [x] T073 [US23] src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt와 QueryDesignEditorTest.kt에 종류·고정 배치·18픽셀·직렬화 검증을 추가하고 settings/DatabaseKind.kt, DataSourceSettingsService.kt, DataSourceSettingsPanel.kt, QuerySqlEditor.kt를 확장한다. (FR-073~FR-074)
- [x] T074 [US23] src/test/kotlin/com/look4counter/DriverDownloaderTest.kt에 JAR/ZIP·실패·취소 검증을 추가하고 queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/DriverDownloader.kt와 설정 패널의 로컬 선택·자동 다운로드를 구현한다. (FR-075)
- [x] T075 [US23] src/test/kotlin/com/look4counter/DatabaseMetadataTest.kt에 관계·Distinct·자원 해제·캐시 전환/무효화/취소 검증을 추가하고 queryeditor/src/main/kotlin/com/look4counter/queryeditor/database/JdbcMetadataReader.kt, DatabaseMetadataService.kt 및 비밀번호 조회를 구현한다. (FR-076, FR-078)
- [x] T076 [US23] queryeditor/src/main/kotlin/com/look4counter/queryeditor/database/QueryDataSourceToolbar.kt와 QueryDesignEditor.kt, sql/SqlKeywordCompletionProvider.kt, SqlDocuments.kt에 선택 DB·조회·캐시 후보를 연결하고 src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에서 실제 삽입·제외·전환을 검증한다. (FR-077~FR-078)
- [x] T077 [US23] 전체 test buildPlugin과 캡처를 수행하고 README.md 및 specs/001-split-editor-modules/validation.md에 결과·다운로드 제약·실제 DB 검증 범위를 기록한다. (SC-026)

T073 → T074 → T075 → T076 → T077 순서다. 하나의 완성 기능으로 DB 종류·조회·캐시·후보까지 연결한 범위가 최소 전달 단위다. 독립 테스트 실행은 가능하나 공유 소스 변경은 순차 진행한다.

## 33단계: 수렴 점검

- [x] T078 [US23] README.md의 이전 키워드 전용 안내를 선택 DB의 테이블·컬럼 완성 동작으로 갱신하여 같은 문서의 새 사용 안내와 일치시킨다. (FR-077, SC-026, 부분 충족·낮음)

