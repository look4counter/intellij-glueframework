# 구현 계획: 모델 다이어그램 편집

작성일: 2026-10-23 | 명세: [spec.md](spec.md)

## 요약

XML PSI 모델을 Swing 캔버스로 표시하고 공유 Source 문서를 통해 Child 위치·너비, Wire 꺾임점·연결을 편집한다. 표시·선택·Navigation은 XML을 변경하지 않는다.

## 기술 문맥

Kotlin/JVM, IntelliJ 2025.3 XML PSI, Swing Graphics2D와 플랫폼 WriteCommandAction·Undo를 사용한다. 새 의존성은 없다. XML 외부 엔터티를 읽지 않으며 전체 문서를 재직렬화하지 않는다. 관련 속성만 변경하고 알 수 없는 속성과 weight를 유지한다.

## 헌법 확인

헌법은 미작성 템플릿이므로 구체적 원칙을 추가하지 않는다. AGENTS.md의 한글 문서 및 speckit 절차를 준수한다. 설계 전후 위반 없음.

## 구조 및 결정

- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagram.kt: XML PSI 파서와 노드·Wire·꺾임점 모델. wire 선언 인덱스로 input/output을 해석하고 누락 끝점을 허용한다.
- 같은 디렉터리의 DiagramGeometry.kt: 사용자 정의 도형별 앵커 보정, 꺾임점 보간, 중앙 라벨 위치를 순수 함수로 계산한다. weight는 경로에 사용하지 않는다.
- DiagramEdits.kt: 절대 꺾임점 위치를 상대 벡터로 변환한다. 중점 삽입 시 다른 꺾임점의 절대 위치와 weight를 유지한다. 독립 캔버스에서도 연결과 노드 참조를 일관되게 갱신한다.
- ModelDiagramCanvas.kt: 도형·Wire·라벨·선택 핸들을 표시하고 클릭, 범위 선택, 우측 Navigation, 일괄 이동·너비 조절, Wire 편집 미리보기를 처리한다.
- ModelDesignEditorProvider.kt: 공유 문서 변경을 다시 표시하고 편집 release를 한 번의 WriteCommandAction으로 저장한다. 편집 시작의 문서 변경 번호와 읽기 전용 여부를 확인하며 Source가 바뀌면 편집을 취소한다.
- src/test/kotlin/com/look4counter/ModelDiagramTest.kt: 좌표·픽셀·입력·XML 편집·저장·Undo·동시 변경 및 읽기 전용을 검증한다. EditorMultiViewTest.kt로 Design/Source 탭 동작을 확인한다.

## 표시 및 화면 좌표

Activity는 XML 너비, 높이 30, 반경 8의 흰색 둥근 사각형이다. State는 20×20이며 FinalState는 흰색 채움 없이 외곽 원과 오프셋 (3,3)의 지름 15 내부 원을 표시한다. Wire·글자는 RGB 145 회색이다. 라벨의 기본 배경은 글꼴 높이-3이고 하단에 1px를 추가하며 글자는 최초 가운데 기준선보다 2px 위에 그린다.

최초 유효 모델에서 원점을 한 번만 정하고 이후 편집·Source 갱신에서는 유지한다. preferredSize를 줄이지 않아 viewport 위치 자동 보정을 방지한다. Navigation 이동량은 별도로 관리하고 현재 화면 좌측 상단에 표시한다. 렌더링과 Hit는 같은 좌표 변환을 사용한다. 마지막 선택 요소와 핸들은 가장 앞에 표시한다.

## 선택과 일괄 편집

선택 집합은 추가 순서를 유지한다. 이미 선택된 Child를 눌러도 다중 선택은 유지한다. Shift는 추가 선택이며 빈 공간 좌측 드래그는 네 모서리가 모두 포함된 Child만 선택한다. Child 핸들은 6/4px이고 마지막 대상 외에는 검정 테두리를 표시한다. Activity 좌우 중앙 핸들은 resize 커서를 표시한다.

이동은 선택 Child 전체에 같은 변화량을 적용한다. 너비 조절은 선택 Activity 전체에 같은 변화량을 적용하며 최소 너비 16에 맞춰 공통 변화량을 제한한다. 각 Activity의 반대쪽 변 및 XML size 높이는 유지한다.

## Wire 편집

Activity 직결 끝점 드래그의 공통 기준은 firstRelativePoint다. previewEndpoint에서 실제 대상에 연결한 후보가 꺾임점 없는 Activity↔Activity이면 시작·끝 구분 없이 마우스 좌표를 firstRelativePoint에 반영한다. end는 lastRelativePoint도 갱신한다. 기존 XML 편집 콜백이 두 속성을 저장하므로 미리보기와 release 후 경로가 일치한다. State와 꺾임점 경로에는 적용하지 않는다.

기존 bendpoint 드래그는 DiagramEdits.kt에서 해당 점을 제거한 후보를 만들고 남은 점의 절대 위치를 다시 인코딩한다. 후보 경로의 해당 선분 직선에서 5px 이내이면 제거 후보를, 초과하면 원본의 해당 점 이동을 반환한다. release 콜백에 삭제 인덱스를 전달하여 ModelDesignEditorProvider.kt가 정확한 XML bendpoint 하나만 삭제하고 남은 태그·속성·weight를 유지한다. 삽입·삭제 모두 한 번의 Undo 명령으로 처리한다.

중점 드래그는 DiagramEdits.kt에서 Down 당시 경로의 해당 선분 직선과 마우스의 수직 거리를 계산한다. 5px 이내는 원본 Wire를 그대로 반환하고, 초과하면 기존 꺾임점 삽입 함수를 호출한다. 캔버스는 매 이벤트에서 원본 스냅샷으로 판정하여 분할·병합을 반복한다. release에서도 동일한 판정을 적용하므로 원본과 같으면 XML 콜백을 호출하지 않는다. 기존 꺾임점 이동 경로는 유지한다.

꺾임점 없는 Activity 직결은 가로의 좌우 8px 교집합을 우선하여 수직 연결한다. 교집합이 없으면 세로의 위아래 8px 교집합으로 수평 연결한다. firstRelativePoint를 해당 구간으로 clamp하고 반대 축의 가장 가까운 변 쌍을 선택한다. 동률은 중심 방향의 마주보는 변을 우선한다. 둘 다 유효한 교집합이 없으면 기존 둥근 모서리 대각선 보정을 사용한다. State 및 꺾임점 경로는 유지한다.

빈 공간의 끝점 드래그는 DiagramGeometry.kt에서 마우스 좌표 중심의 가상 InitialState를 만들어 기존 path 함수를 재사용한다. 원래 bendpoint의 firstDimension과 secondDimension을 그대로 유지하고 변경된 중심으로 first+(second-first)×(i+1)/(n+1)을 계산한다. 꺾임점 절대 좌표를 보존하는 재인코딩은 끝점 드래그에서 제거한다. 캔버스는 이 계산 경로를 렌더링·핸들·Hit·라벨에 공통 사용한다. 실제 XML 모델과 재연결 콜백은 변경하지 않는다.

선분 Hit는 4px이며 경로점·중점 핸들 Hit는 모두 6×6이다. 경로점 표시는 6/4px, 중점 표시는 4/2px다. 핸들 hover는 move 커서다.

기존 꺾임점 드래그는 해당 점을 옮기고 중점 드래그는 새 꺾임점을 삽입한다. firstDimension과 secondDimension은 각각 노드 중심에서의 벡터로 기록한다. 삽입으로 보간 비율이 바뀌어도 다른 꺾임점 위치가 유지되도록 다시 인코딩한다.

끝점 드래그는 원본 Wire를 기준으로 계산한다. 빈 공간에서는 마우스를 따르고 유효한 Child 위에서는 해당 Child로 연결 규칙을 적용한다. 반대쪽 Child와 같으면 연결 대상으로 무시한다. release 시 유효 대상만 저장하며 그 외에는 원본을 복원한다. XML child input/output에서 해당 Wire 인덱스 참조만 이동하고 다른 참조·속성·Wire 순서는 유지한다.

## 검증 단계

명세·명확화 → 계획 → 작업 목록 → 일관성 분석 → 구현 → 좌표·픽셀·통합 테스트 → 이미지 확인 → converge 점검 순서로 진행한다.

## Child 관통 방지 병합 설계

DiagramEdits의 두 드래그 함수는 Wire의 start·end를 검사 대상으로 사용한다. 이동한 B의 인접 두 선분 A–B, B–C를 start·end의 실제 도형 내부와 검사한다. Activity는 두 중앙 사각형과 반경 8인 네 원의 합집합, State는 반경 10인 원으로 판정한다. 선분의 매개변수 구간과 원까지의 최소 거리로 내부 교차를 계산하여 접촉은 허용한다. ModelDiagramCanvas는 Wire의 start·end만 검사하여 미리보기와 release가 동일한 판단을 사용한다. XML 구조와 편집 콜백은 유지한다. Kotlin/JVM 및 기존 Gradle 테스트를 사용하며 추가 의존성은 없다. 헌법은 미작성 템플릿이므로 추가 제약이 없다.

## 드래그 인접 선분 관통 검사 정정

앞선 Child 병합 설계의 검사 대상은 사용자의 FR-040에 따라 정정한다. DiagramEdits에서 이동 또는 삽입된 wire를 먼저 만들고 그 path의 index→index+1 및 index+1→index+2를 검사한다. 5px 임계치는 기존 제거 후보 직선 또는 원래 중점 선분을 유지한다. 도형 교차 함수와 Canvas의 Wire의 start·end만 검사하도록 제한한다. 기존점과 중점 양쪽에서 이전 구현과 결과가 달라지는 회귀 사례를 추가한다.

## 연결 Child만 검사하도록 범위 정정

DiagramEdits에서 임의 nodes 목록 인수를 제거하고 canMergeAt이 moved.start와 moved.end만 수집하도록 한다. Canvas도 전체 diagram.nodes 전달을 제거한다. 전체 Child 검사 시나리오는 연결되지 않은 Child를 무시하는 Canvas 드래그 회귀 검증으로 교체한다. start·end 관통 및 경계·임계치 검증은 유지한다. 새 데이터나 의존성은 없다.

## 작은 중점 핸들로 직결 접점 이동

DiagramEdits.dragMidpoint에서 꺾임점 없는 Activity 양 끝의 8px 유효 구간 교집합을 수직 우선으로 계산한다. 해당 축의 마우스 좌표가 유효 구간 안이면 firstRelativePoint에 기록하고, 밖이면 원래 Wire에 bendpoint를 삽입한다. 다른 축은 기존 기준을 유지한다. 공통 좌표를 읽는 DiagramGeometry 및 기존 Canvas의 편집·XML·Undo 경로를 재사용한다. 추가 의존성이나 영속 필드는 없다. 기존 분할 테스트는 State 직선으로 전환해 기존 규칙도 회귀 검증한다.

## 제한 범위 밖 분할 정정

앞선 경계 고정 동작을 대체한다. DiagramEdits의 Activity 직결 처리에서 해당 축 좌표가 범위 안이면 공통 좌표를 갱신하고, 범위 밖이면 원본 Wire에 moveBendpoint(insert=true)를 적용한다. 범위 밖 판단은 5px 분할 임계치보다 우선한다. Canvas가 매 이벤트 원본 Wire를 사용하므로 반복 진입·이탈과 release에도 같은 계산을 적용한다. 기존 XML·Undo 저장 경로를 재사용한다.

## 직결 복원의 관통 방지

DiagramEdits.slideDirectActivityConnection에서 원본에 B를 삽입한 moved Wire를 만들고 기존 canMergeAt(moved, 0)을 호출한다. 공통 유효 범위 안이고 인접 두 선분이 start·end 내부를 통과하지 않을 때만 공통 좌표 갱신 Wire를 반환한다. 어느 조건이든 실패하면 moved를 반환한다. Canvas와 XML·Undo 경로는 그대로 사용한다. 기존 직결 이동 테스트는 B를 안전한 양 Child 사이에 두고, 별도 관통 회귀 테스트에서 start·end 내부의 B와 안전한 복원을 검증한다.

## Esc 및 복사·붙여넣기 설계

기술 기반은 기존 Kotlin/JVM·IntelliJ PSI·Swing이며 추가 의존성은 없다. 헌법은 미작성 템플릿이므로 별도 제약을 추가하지 않는다.

- ModelDiagramCanvas의 화면 이동·범위 선택 상태를 제스처 상태와 함께 관리한다. 시작 당시 선택·Navigation을 보관하고 cancelDrag에서 Child 원본 또는 Wire 원본을 복원한 뒤 상태를 초기화한다. display로 Source를 갱신할 때도 제스처 상태를 제거하여 오래된 상태를 복원하지 않는다. Esc는 DumbAwareAction과 CommonShortcuts.ESCAPE로 등록한다.
- Canvas는 UiDataProvider로 CopyProvider·PasteProvider를 제공하여 IDE 메뉴·키맵을 재사용한다. 드래그 중 편집 복사·붙여넣기는 비활성화한다. 모델 좌표와 화면 좌표 사이의 변환 오프셋은 origin+pan-visibleRect 원점으로 계산한다.
- DiagramClipboard.kt에 선택 시점의 Child·Wire XML, 모델 좌표, 원래 Wire 인덱스 및 namespace 선언을 스냅샷으로 보관하는 JVM 로컬 클립보드 형식을 추가한다. 운영체제 임의 문자열을 XML로 파싱하지 않는다. CopyPasteManager를 사용하며 같은 IDE의 다른 문서도 같은 복사본을 사용할 수 있다.
- ModelDesignEditorProvider는 현재 Source를 commit하고 선택을 확인한 뒤 XML 스냅샷을 생성한다. 붙여넣기는 한 WriteCommandAction 안에서 detached XML 태그를 준비하고 ID·location·input/output·절대 접점 좌표를 갱신한 뒤 기존 Child/Wire 선언 순서를 보존하며 추가한다. BigInteger로 숫자 ID 최대치를 계산하여 오버플로를 피한다. 상대 bendpoint 벡터와 weight는 유지한다.
- 새 Child를 선택하며 매 붙여넣기 성공 후에만 누적 이동 횟수를 증가시킨다. 기존 문서 리스너와 refresh를 통해 Source·Undo/Redo·저장을 연동한다. 기존 재연결 대상 선택은 범위 밖이며 완료 보고에서 설명한다.

검증은 실제 플랫폼 Copy/Paste provider와 Esc action, Canvas 이벤트 및 XML/Undo를 사용하는 회귀 테스트로 수행한다.

빈 대상 문서에 처음 붙여넣을 때도 기존 화면 원점을 유지해야 하므로 paste 후 refresh는 preserveOrigin을 사용한다. 그렇지 않으면 음수 모델 좌표의 첫 Child 추가가 원점을 자동 이동시켜 화면 기준 15px 위치를 깨뜨린다. 모델 편집·클립보드 테스트와 EditorMultiViewTest로 Source 탭 통합까지 검증한다.

## 선택 삭제와 참조 재매핑

추가 의존성 없이 기존 PSI·WriteCommandAction·UiDataProvider를 재사용한다. Canvas에 DeleteProvider를 추가하고 IDE DELETE_ELEMENT_PROVIDER 데이터와 Delete 키 액션으로 연결한다. provider는 EDT에서 선택·문서 쓰기 가능 여부·Source 갱신 대기·진행 중 제스처를 확인한다.

ModelDesignEditorProvider의 deleteSelection은 문서를 commit하고 파싱 결과가 Canvas와 같은지 확인한다. 선택 Child의 실제 XML 태그를 ID·종류로 찾고, 선택 Wire 인덱스 및 삭제 Child의 input/output Wire 인덱스를 삭제 집합으로 만든다. 기존 Wire 선언 순서에서 남은 항목의 기존→새 인덱스 매핑을 계산한다.

하나의 WriteCommandAction 안에서 남은 모든 child 태그(미지원 종류 포함)의 input/output을 정리·재매핑하고 삭제 Wire·Child 태그를 제거한다. 기타 참조 토큰·속성·property와 남은 Wire ID·상대 벡터는 그대로 둔다. 삭제 후 선택을 해제하고 preserveOrigin refresh를 적용한다. 상대편 Child는 삭제하지 않는다. 기존 재연결 판정은 변경하지 않는다.

검증은 ModelDiagramClipboardTest의 플랫폼 편집 액션 헬퍼를 재사용하여 Delete 키 액션·IDE Delete provider·PSI/XML·Undo/Redo·클립보드 보존을 확인하며 기존 모델 및 Design/Source 통합 테스트도 실행한다.

## 더블클릭 편집과 Child 추가 툴바

기존 Kotlin/JVM·IntelliJ DialogWrapper·Swing·PSI·WriteCommandAction을 사용하며 추가 의존성은 없다. 헌법은 미작성 템플릿이다.

- ModelDiagramCanvas의 더블클릭에서 guardcondition 라벨을 우선 확인하고 기존 앞쪽 요소 규칙으로 Activity 또는 Wire 편집 요청 콜백을 호출한다. 단일 클릭 선택·드래그와 분리한다.
- ModelDiagramDialogs 인터페이스와 기본 프로젝트 서비스를 추가한다. 기본 구현은 WireConditionDialog 및 ActivityPropertiesDialog를 제공한다. ActivityPropertiesPanel에는 별도 name 입력, key/value 테이블과 추가/삭제 버튼, description textarea를 배치한다. 일반 property 행은 원래 property 인덱스를 보관하여 key·value를 바꿔도 기타 속성을 유지한다. 테스트는 서비스 대체를 통해 실제 에디터의 확인/취소 저장 경로를 검증하고 패널 입력·검증도 직접 확인한다.
- ModelDesignEditorProvider는 창을 열기 전 문서 stamp와 XML 스냅샷을 보관하고 확인 후 stamp·쓰기 가능 여부를 다시 검사한다. 실제 변경된 속성/property만 단일 WriteCommandAction에서 바꾸고 refresh한다. 무변경과 취소는 XML 명령을 만들지 않는다.
- 상단 Swing 툴바에 종류 메뉴가 결합된 ChildCreationButton을 둔다. PropertiesComponent로 프로젝트의 마지막 종류를 기억한다. 토글 상태는 Canvas의 생성 종류와 동기화한다.
- Canvas는 생성 종류·마우스 모델 좌표를 보관하고 실제 도형과 같은 ghost를 그린다. 클릭은 생성 콜백으로 전달하며 Esc는 드래그 취소 후 생성 모드를 종료한다. 모드 중 Copy/Paste/Delete 및 일반 선택 편집은 비활성화한다. 마우스 출입·이동·Navigation 시 ghost를 갱신한다.
- 생성은 최대 숫자 ID+1의 child 태그를 적절한 xsi/type namespace와 기본 속성으로 만들고 첫 Wire 앞에 삽입한다. preserveOrigin refresh와 새 Child 선택을 사용한다. 클릭마다 독립 Undo를 제공하고 모드는 유지한다.

검증은 편집 서비스 대체·실제 Canvas 이벤트·Swing 패널·PSI/Source·Undo/Redo를 이용한 ModelDiagramPropertiesTest와 기존 모델/클립보드/Design 통합 테스트로 수행한다.

문자열 저장 보완: DiagramXmlText.kt에서 XML 사용자 속성의 엔티티 변환과 CR/LF/탭 보존을 공통 처리한다. ModelDiagram.kt의 name·guardcondition 파싱도 이를 사용하여 편집 전후 같은 실제 문자열을 표시한다.

## 편집 UI 개선·행 클립보드·클래스 열기·통합 생성 버튼

ModelDiagramDialogs.kt의 Transition 창 입력 폭을 45칸에서 22칸으로 줄이고 제목·라벨을 바꾼다. Activity 패널은 대문자 라벨·textarea 8px 내부 여백·GridLayout(1,5)의 균등 버튼과 1:3 Key/Value 선호 너비를 사용한다.

PropertyRowsClipboard.kt는 key/value 행을 JVM 로컬 DataFlavor와 quoted TSV로 보관한다. 테이블은 UiDataProvider로 IDE CopyProvider/PasteProvider를 제공하고 새 행의 sourceIndex는 null로 둔다. 활성 셀 편집 중에는 행 액션을 제공하지 않아 텍스트 복사와 분리한다. 붙여넣기는 선택 마지막 행 다음에 삽입하고 검증·확인은 기존 경로를 재사용한다.

ActivityClassNavigation.kt에서 JavaPsiFacade와 GlobalSearchScope.allScope로 찾은 클래스의 navigationElement를 연다. 패널은 현재 class 값을 전달하고 실패 시 인라인 메시지를 표시한다. Java 플러그인은 이미 루트 플러그인에 필수 의존성이며 modeleditor 컴파일에도 의존성을 명시한다. Open Query/Open Service는 비활성으로 둔다.

ChildCreationButton.kt의 한 JToggleButton에 본체 토글과 화살표 영역의 종류 JPopupMenu를 결합한다. Alt+Down도 종류 선택을 열며 마지막 종류 PropertiesComponent와 Canvas 생성 상태를 연동한다. 기존 별도 JComboBox를 제거한다. ghost는 기존 drawNode를 불투명·동일 stroke/안티앨리어싱으로 그려 실제 비선택 도형과 일치시킨다.

검증은 ModelDiagramPropertiesTest의 패널·플랫폼 Copy/Paste·클래스 에디터 열기 및 기존 생성 테스트를 갱신하고 배치/ghost의 이미지도 확인한다. 새로운 외부 서비스나 네트워크는 사용하지 않는다.

## 표준 툴바와 연결 접점 보완

IntelliJ SDK에서 기본 SplitButtonAction이 본체 액션·화살표 메뉴·ToggleAction 눌림 표시를 지원함을 확인했다. ChildCreationToolbar는 표준 ActionToolbar에 SplitButtonAction 하나를 넣고 고정 본체 ToggleAction과 종류 선택 ToggleAction 3개를 제공한다. useDynamicSplitButton=false로 메뉴 선택이 본체 액션을 바꾸지 않게 한다. 아이콘과 툴팁은 현재 종류를 표시하고 기존 PropertiesComponent·Canvas 상태를 사용한다. 기존 ChildCreationButton.kt를 제거하고 표준 액션을 테스트한다.

DiagramGeometry.toward에 x가 Activity 밖이고 y가 높이 범위 안인 경우 좌/우 변의 y를 8px 여백으로 제한하는 분기를 추가한다. State 외곽은 stateContact 헬퍼로 통합하고 유효한 firstRelativePoint/lastRelativePoint가 원 안에 있으면 그 방향, 없으면 이웃 점 방향으로 반경 10에 투영한다. 가상 State는 드래그 쪽 선호 좌표를 비운다.

State 이동 시 Canvas 미리보기와 ModelDesignEditorProvider의 XML 저장 모두 해당 firstRelativePoint/lastRelativePoint를 같은 delta로 이동한다. State↔State 중심 연결의 기존 테스트 기대값을 원 외곽으로 갱신하고 관련 선택/중점/병합 테스트의 좌표를 새 접점에 맞춘다. 외곽 접점에서도 Child 내부 관통 병합 금지를 유지한다.

## 수직 접점 제한·미니맵·스냅 가능성 검토

DiagramGeometry.toward의 수직 접속 분기는 x를 그대로 반환하므로 좌우 8px clamp를 적용한다. 너무 좁은 기존 Activity는 inset=min(8,width/2)로 처리한다. State/가상 State/bendpoint가 모두 같은 함수를 사용하므로 여기서 공통 수정한다.

DiagramMiniMap.kt에 순수 투영 계산을 분리한다. 입력은 노드 경계·Wire 경로·라벨 경계, visibleRect, 원점+Navigation 변환이다. 전체 내용 경계에 여백을 더하고 최대 0.5 배율로 우측 상단 최대 200×140 프레임 안에 맞춘다. 현재 뷰포트는 모델 좌표로 역변환해 표시하고 프레임 내부로 clip한다.

ModelDiagramCanvas는 모델 도형 그리기를 재사용해 반투명 미니맵을 화면 좌표에서 마지막에 그린다. 선택 핸들과 생성 ghost는 미니맵에 넣지 않는다. 편집 미리보기는 기존 diagram/paths를 사용하므로 즉시 반영한다. JViewport 변화 리스너는 스크롤 시 전체 visible 영역 repaint를 요청하고 removeNotify에서 해제한다. 미니맵에서 시작한 왼쪽 클릭/더블클릭/추가 ghost는 가로채되 오른쪽 Navigation과 이미 진행 중인 제스처는 유지한다.

스냅은 구현하지 않는다. 기존 dragBase·dragAnchor 기반 미리보기와 release의 onNodesMoved/onNodesResized 단일 명령 구조를 검토하여 alignment-review.md에 가능성, 가이드 기준, 임계치·우선순위, 다중 선택·최소 너비·Esc·Source 보호와의 결합을 제안한다.

검증은 투영 좌표·음수/퇴화 경계·뷰포트, 화면 고정·편집 반영·비활성 상태·왼쪽 클릭 보호·XML 무변경과 이미지 확인 및 기존 모델 통합 회귀 테스트로 수행한다.

## 실시간 정렬·미니맵 조정·클래스 자동완성 계획

이전 검토 전용 스냅 결정을 후속 승인으로 대체한다. DiagramAlignment.kt에서 선택 영역과 대상 Child의 변·중심을 비교하여 축별 최소 보정량을 계산한다. 임계치는 화면 5px이며 현재 줌이 없으므로 모델 좌표와 같다. 매번 dragBase와 원래 마우스 이동량으로 계산하여 누적 오차를 방지한다. 다중 선택은 공통 이동량·너비 변화량을 사용하고 대상에서 전체 선택을 제외한다. 너비는 움직이는 변과 동일 너비 후보를 비교하고 16px 최솟값을 위반하는 후보는 제외한다. 후보 동률은 입력 순서로 결정한다.

ModelDiagramCanvas는 드래그/최종 release에서 같은 미리보기를 사용하고 Alt로 우회한다. 보조선은 모델 좌표에 점선으로 그리며 미니맵 프레임을 제외한다. Esc·Source·release에서 임시 상태를 해제하고 기존 저장 명령을 재사용한다. 읽기 전용 전환 시 미리보기를 취소한다.

미니맵은 제목 그리기를 제거하고 기존 맞춤 배율에 0.7을 곱한다. 제목 자리도 기존 투영 영역으로 유지하여 종전 대비 정확한 축소 비율을 보장한다.

ActivityClassCellEditor.kt는 JavaReferenceEditorUtil의 클래스 참조 문서와 EditorTextField를 사용한다. ClassNameReferenceEditor의 클래스 이름 표시를 지정하여 표준 Java 자동완성을 활성화한다. PropertyRowsTable.getCellEditor에서 현재 모델 행의 key가 class이고 Value 열일 때만 선택한다. 프로젝트는 ActivityPropertiesDialog에서 주입하며 기존 패널 호출 호환성을 유지한다. JTable 편집 완료 시 전체 문자열을 모델에 전달하고 기존 확인/취소 저장을 유지한다.

검증: 순수 좌표·캔버스 제스처·Java 참조 문서 및 후보·일반 행 분리·편집 완료와 취소를 테스트하고 모델 관련 회귀를 실행한다. 외부 서비스나 새 라이브러리는 필요하지 않다. 헌장은 미작성 템플릿이므로 별도 원칙 검사를 생략한다.

## 같은 간격 이동 스냅 계획

DiagramAlignment.move에 기존 변·중심 후보와 간격 후보를 함께 비교하는 축별 후보 모델을 도입한다. 선택 영역은 기존 집합 경계이며 targets는 전체 선택을 제외한 목록이다. 각 축에서 직교 방향으로 선택 영역과 겹치는 대상들을 정렬하고 인접 쌍만 비교한다. 기준 쌍의 공통 직교 범위가 선택 영역과 겹치고 기준 축에서 겹치지 않는 경우에만 사용한다. 정렬 및 비교는 축마다 대상 수에 대해 O(n log n)이며 모든 쌍의 조합을 생성하지 않는다.

사이 후보의 시작점은 (첫 요소 끝+둘째 요소 시작-선택 너비)/2이고, 바깥 후보는 첫 요소 시작-기준 간격-선택 너비 또는 둘째 요소 끝+기준 간격이다. 음수 간격과 공간 부족은 제외한다. 외곽 간격 0은 허용한다. 모든 보정량은 dragBase 기준 raw 이동량과 비교한다. 축별 최소 보정량을 선택하고 동률은 기존 정렬 우선, 간격끼리는 좌표 정렬 순서를 사용한다. 두 축의 보정 후에도 기준 쌍의 공통 직교 범위를 만족하는지 재확인한다.

AlignmentGuide에 선택적 distance를 추가한다. 일반 정렬은 기존 점선이고 간격은 두 개의 치수선과 끝 눈금·화살표·px 거리 라벨로 그린다. 라벨은 소수 한 자리까지 표시하며 0.5px 등 실제 좌표 정밀도는 반올림하지 않는다. Canvas에서 가이드를 그리는 함수를 분리해 기존 미니맵 제외 clip과 테마 색상을 재사용한다. 현재 XML 저장 경로·제스처 보호·Alt·너비 조절은 변경하지 않는다.

테스트는 순수 계산에서 양 축의 사이/앞/뒤·서로 다른 크기·음수·0/음수 간격·다른 줄·인접 쌍·5px 경계·정렬 경쟁을 검증한다. 캔버스에서는 다중 선택·Alt·최종 release·Esc·Source 교체·읽기 전용·Navigation·치수선 이미지·한 번의 Undo/Redo를 확인한다. 모델 관련 기존 회귀를 실행한다. 헌장은 미작성 템플릿이며 추가 라이브러리·외부 API는 사용하지 않는다.

## 메모리 Query ID 인덱스와 sql-key 자동완성 계획

QueryEditor 모듈의 queryindex/QueryIdIndexService.kt를 프로젝트 서비스로 추가한다. 영속 상태 인터페이스나 FileBasedIndex를 사용하지 않는다. VirtualFile별 불변 ID 목록을 메모리에 보관하며 자동완성은 공개 snapshot(prefix) 메서드만 조회한다. 프로젝트 초기 ProjectActivity와 서비스 최초 사용 시 수집을 시작한다. 초기 순회는 ProjectFileIndex의 콘텐츠만 대상으로 하고 파일별 ReadAction에서 cached Document를 우선 읽어 미저장 변경도 반영한다.

EditorFactory 문서 리스너, VFS BulkFileListener, 프로젝트 루트 리스너를 서비스 수명에 연결한다. MergingUpdateQueue를 pooled thread에서 150ms 병합 처리하여 일반 변경은 해당 파일만 재파싱하고 디렉터리/루트 변경은 콘텐츠 목록을 다시 수집한다. 파싱에는 외부 엔터티·외부 DTD 접근을 끈 JDK XML 파서를 사용한다. queryMap의 같은 namespace인 직접 자식 query만 읽는다. 잘못된 XML은 빈 목록으로 교체하고 다음 유효 수정에서 복구한다. 파일별 읽기 오류는 해당 파일만 비우며 취소 예외는 전파한다. 서비스 dispose 시 메모리를 비운다.

modeleditor는 compileOnly(project(":queryeditor"))로 공개 메모리 서비스 타입을 참조하고 런타임은 기존 루트의 두 composed module을 사용한다. QueryIdCellEditor.kt는 TextFieldWithCompletion과 TextCompletionProvider를 사용한다. provider는 메모리 snapshot만 조회하며 ID 접두사는 점·하이픈을 포함해 전체 Value를 사용한다. Lookup에는 설명·파일을 표시하고 삽입 문자열은 ID다. PropertyRowsTable은 현재 모델 key가 sql-key인 Value에서만 이 편집기를 사용한다. class 전용 편집기와 기존 일반 텍스트/확인 저장 경로를 유지한다.

검증은 QueryIdIndexTest.kt에서 초기 구축·미저장/외부 변경·Undo·추가/삭제/rename/move·유효하지 않은 문서·namespace·중복·내용 범위를 확인한다. ModelDiagramPropertiesTest.kt에서 sql-key 편집기 분리·실제 자동완성 후보 선택·ID만 삽입·확인 전 원본 유지·실시간 후보 갱신을 검증한다. 관련 모델 및 QueryEditor 회귀를 함께 실행한다. 새 외부 라이브러리·디스크 인덱스는 없고 기존 모듈 간 컴파일 참조만 추가한다.

## 미니맵 현재 화면 포함 투영 보정 계획

기존 구현은 도형 경계만으로 배율을 계산하고 viewport와 contentArea의 교차 사각형을 그렸다. 화면이 내용보다 크거나 Navigation으로 멀어지면 파란 사각형이 잘리거나 사라져 실제 캔버스 크기·비율을 표현하지 못했다. 이 결정은 앞선 교차 clip 설계를 대체한다.

DiagramMiniMap.layout은 기존 modelBounds를 내용 전용으로 유지하고, 실제 visibleRect에서 origin+pan을 뺀 modelViewport와 modelBounds의 합집합 overviewBounds를 만든다. 배율은 overviewBounds를 기존 contentArea에 맞춘 뒤 0.7을 곱하고, offset은 합집합 중심을 기준으로 계산한다. 그러면 도형·현재 화면이 같은 투영을 사용하며 항상 내부 영역에 들어간다. 프레임·투명도·라벨 없음은 유지한다. 전체 문서 preferredSize를 현재 화면 크기로 사용하지 않는다.

Canvas.drawMiniMap은 projection.viewport 전체를 그린다. 모델 도형 렌더링의 contentArea clip은 안전 경계로 유지하지만 사각형을 교차 영역으로 다시 만들지 않는다. 현재 JViewport ChangeListener와 repaint 경로를 재사용하여 스크롤·크기 변경·Navigation·편집 미리보기를 반영한다. 별도 캐시·문서 상태·외부 의존성은 추가하지 않는다.

검증은 ModelDiagramMiniMapTest의 기존 배율 기준을 overviewBounds로 갱신하고, 작은 내용/큰 화면·가로세로 비율·크기 변경·큰 상하좌우 Navigation·스크롤·음수 원점에서 도형과 viewport가 내부에 포함됨을 확인한다. 실제 렌더링 이미지에서 사각형 네 변이 모두 나타나는지도 확인하고 기존 모델 관련 회귀를 실행한다. 헌장은 미작성 템플릿으로 구체 원칙 검사를 생략한다.

## 미니맵 추가 축소 취소 계획

FR-111에 따라 DiagramMiniMap.layout의 배율에 곱하던 0.7을 제거한다. min(0.5,contentArea.width/overviewBounds.width,contentArea.height/overviewBounds.height)의 기본 맞춤 배율을 사용하며 내용과 현재 화면의 합집합 및 중심 offset은 유지한다. 이전 계획의 70% 추가 여백은 이 결정으로 대체한다. 기존 ModelDiagramMiniMapTest의 배율 기대값을 갱신하고 11개 투영·Navigation·화면 크기·렌더링 회귀를 실행한다. 새 테스트나 의존성·영속 상태는 추가하지 않는다.

## Query 탐색·모델 설정·Navigation 확장 계획

QueryEditorNavigation 인터페이스를 QueryDesignEditor가 구현하고 NavigatableFileEditor를 함께 구현한다. QueryNavigation은 메모리 ID 목록에서 파일을 찾고 FileEditorManager로 Design을 선택한 뒤 해당 Query를 선택한다. 중복 ID는 파일 chooser를 제공한다. 일반 OpenFileDescriptor의 file/offset을 PSI의 query 범위로 해석해 같은 인터페이스로 선택하므로 Find in Files 결과도 재사용한다. PSI 접근은 ReadAction, UI 탐색은 EDT에서 수행한다. 준비 중/누락은 인라인 안내하고 기존 저장 경로를 건드리지 않는다.

ModelDiagram에 기본값이 있는 루트 property 목록을 추가하고 ModelDiagramParser가 직접 자식 property만 읽는다. Canvas는 좌표 표시 아래 screen 좌표로 key: value를 여러 줄 그린다. 내용 경계/미니맵/Child 속성에는 포함하지 않는다.

ModelEditorSettings는 프로젝트 PersistentStateComponent로 서비스 경로와 프리셋(Name, property 목록, Description, #RRGGBB 색상)을 분리 복제하여 저장한다. GlueModelEditorConfigurable은 디렉터리 picker와 프리셋 목록·상세 편집을 제공하고 상세는 기존 ActivityPropertiesPanel을 재사용하되 탐색 버튼을 숨기고 ColorPanel을 붙인다. Apply 이전 로컬 상태만 변경한다. ServiceFiles는 현재 설정 폴더의 .xml 직접 자식 이름을 자동완성하고 .glue_uml_ad 직접 자식을 열어 경로 순회를 방지한다. 파일 이름 후보는 현재 VFS 자식에서 읽고 XML 본문 파싱은 하지 않는다.

ActivityPropertiesPanel은 선택적 Open Query/Service 콜백을 받아 현재 셀 편집을 완료한 값으로 탐색하고 실패 메시지를 공유한다. 기존 trailing lambda Open Class 호출과 테스트 호환성을 유지한다. PropertyRowsTable은 service-name 행에 ServiceNameCellEditor를 제공한다.

Canvas의 navigationOnly 모드에서는 Scrollable로 뷰포트 너비/높이를 추적한다. 편집기 JScrollPane은 양쪽 scrollbar NEVER와 wheel scrolling 비활성으로 유지하고 Canvas의 정밀 휠 리스너가 pan을 변경한다. 기존 단독 Canvas의 스크롤 테스트는 navigationOnly=false로 유지한다. 진행 중 제스처에서 휠은 미리보기를 취소한 후 이동하여 XML 오염을 막는다.

미니맵 click은 현재 투영을 역변환하고 해당 모델 점을 화면 중심으로 옮긴다. 기존 프레임 클릭 차단은 유지하며 내부에서만 Navigate한다. 초기 투영은 기존 modelBounds(8px 여백)+modelViewport 합집합 맞춤을 재사용한다. Range box는 alpha가 있는 하늘색 fill을 추가한다.

WireMagnet은 Activity 중심 기준 30px 격자 중 8px 안쪽 범위 및 가능한 공통 접속 구간에 들어가는 후보만 선택한다. Canvas의 endpoint 미리보기에서 적용하고 mouseReleased도 같은 함수를 사용한다. State 원 외곽과 일반 정렬 스냅은 유지한다. Activity↔State와 꺾인 경로는 선호 좌표만으로 실제 접점을 고정할 수 없어 인접 bendpoint를 보정하거나 하나 추가하고 기존 상대 좌표 형식으로 저장한다. ChildCreationToolbar에 표준 ToggleAction과 자석 아이콘을 추가하고 에디터가 Canvas 상태를 연결한다. 기본값은 off다.

검증 파일은 ModelEditorFeaturesTest.kt(루트 속성·자석·휠·범위·미니맵), ModelEditorSettingsTest.kt(설정·프리셋·서비스 후보/열기), QueryNavigationTest.kt(ID·offset·Find in Files 계약), ModelDiagramPropertiesTest.kt(버튼/자동완성)다. 기존 ModelDiagram*Test·Query*Test·EditorMultiViewTest와 새 모델 테스트를 실행하고 렌더링을 확인한다. 새 외부 라이브러리는 없고 헌장은 미작성 템플릿이다.

## 루트 편집·모든 핸들 자석·서비스 설정 후속 계획

ModelDiagramDialogs.editDiagram을 기본 null 구현으로 확장하고 DiagramPropertiesDialog를 추가한다. ActivityPropertiesPanel에 showName과 compact 옵션을 추가하여 Properties/Description만 재사용한다. ModelDesignEditor는 기존 Activity 편집과 같은 sourceIndex 보존 방식으로 루트 property만 변경하며 확인 후 modificationStamp를 검증한다. ChildCreationToolbar의 표준 AnAction으로 연다. Canvas HUD는 description을 걸러 key=value로 표시한다.

WireMagnet.dragPosition은 시작/끝 Activity에서 x/y별 후보를 만들고 가장 가까운 보정을 선택한다. x는 유효 8px 범위의 중심±30n, y는 유효 높이 범위 안에서만 중심으로 맞춘다. 직접 연결의 공통 범위를 벗어난 포인터는 강제로 되돌리지 않는다. Canvas.previewBendpoint가 원래 Wire와 raw 마우스 위치로 이 계산을 먼저 적용한 뒤 기존 DiagramEdits 병합 검사를 실행한다.

Canvas는 miniMapDrag 투영을 보관하고 isDragging에 포함한다. press/drag/release에서 동일한 스냅샷 역변환을 사용하되 화면 출력 투영은 기존 자동 맞춤을 유지한다. 입력은 contentArea에 clamp하고 gesturePan으로 Esc를 복원한다. Source 교체와 clearGesture에서 상태를 해제한다.

ServiceFiles는 프로젝트 기준 VirtualFile과 VFS 상대 경로로 통합한다. 내부 absolute/URL 경로는 호환하되 조상 검사로 외부를 거부하고 설정은 상대 경로로 저장한다. 디렉터리 picker의 roots도 프로젝트로 제한한다. 모델 열기는 .xml과 -service 접미사를 순서대로 제거한다. ServiceNameCellEditor는 TextFieldWithCompletion에서 자동 팝업·강제 팝업을 모두 활성화하고 파일 목록은 현재 설정에서 조회한다.

GlueModelEditorConfigurable은 Configurable.NoScroll로 변경하고 JSplitPane·900px 선호 폭을 제거한다. JBUI 여백·폭이 작은 목록과 BorderLayout 상세를 사용한다. compact Activity 패널은 최소 크기를 낮추고 Description 열/행 수도 줄인다. 설정 전체 스크롤은 없애되 목록/표의 내부 스크롤은 유지한다.

검증 파일은 ModelEditorFeaturesTest.kt, ModelEditorSettingsTest.kt, ModelDiagramPropertiesTest.kt와 ModelDiagramMiniMapTest.kt다. 명세 8개 요구사항과 작업을 대조하고 관련 전체 모델/Query 회귀 및 설정/캔버스 렌더링을 확인한다. 새 의존성은 없고 헌장은 미작성 템플릿이다.

## 프리셋 생성·Wire 생성·Service 검색 확장 계획

자석 SVG는 로컬 SDK의 expui/general/settings.svg 색상 #6C707E, dark #CED0D6와 맞추고 dark 리소스를 추가한다. ServiceFiles.find는 정리한 UML 파일명을 ProjectFileIndex 콘텐츠에서 검색하고 설정 폴더 직접 후보도 합친다. 설정 후보 우선 정렬·경로 중복 제거 후 open에서 여러 결과를 chooser로 제공한다.

ChildCreationToolbar의 종류 그룹을 동적 그룹으로 바꾸어 현재 Settings 프리셋 ToggleAction을 추가한다. selectedPreset은 분리 복제하고 일반 종류 선택 시 해제한다. ghost와 createChild는 프리셋 이름·속성·Description을 사용한다. DiagramNode 끝에 기본값이 있는 className을 추가하고 파서가 Child class를 읽는다. Canvas는 class 색상 콜백을 사용해 실제 도형·ghost·미니맵을 공통 렌더링한다. ModelEditorSettings는 class→색상 캐시와 프로젝트 Topic을 제공하며 변경 시 에디터가 메뉴·ghost·화면을 갱신한다. 첫 번째 비어 있지 않은 class가 우선이다.

Wire 추가는 Toolbar의 Activity 오른쪽 ToggleAction이다. Canvas에 wireCreationMode·시작 Child/점·현재 포인터/대상을 추가한다. 모드 press는 기존 편집을 차단하고 click에서 첫 Child 저장 또는 실제 끝 Child로 draft를 요청한다. mouseMoved는 실제 Child 또는 가상 State를 향한 Wire 미리보기를 만든다. 자석은 기존 WireMagnet을 사용한다. 모드 전환은 Child 생성과 배타적이며 Esc/clearGesture는 미완료 시작점을 해제한다.

ModelDesignEditorProvider.createWire는 최초 클릭의 문서 stamp와 원본 모델을 확인한 뒤 Wire 태그·전역 ID·Child output/input 참조·선호점/bendpoint를 단일 WriteCommandAction으로 저장한다. 편집 완료 후 모드를 유지하고 새 Wire를 선택한다. 복사/붙여넣기/삭제/속성은 Wire 생성 중 비활성화한다. Source 변경은 미완료 연결을 취소하고 읽기 전용은 생성 모드를 해제한다.

검증은 ModelEditorSettingsTest(프로젝트 검색), ModelDiagramPropertiesTest(프리셋/참조/Undo/모드), ModelEditorFeaturesTest(미리보기/취소/색상)와 기존 모델·Query 회귀다. 새 외부 의존성은 없고 헌장은 미작성 템플릿이다.
## 생성과 표시 개선 계획

ServiceFiles의 단일 파일·선택 콜백을 공통 열기 함수로 모아 openFile 후 glue-model-design을 선택한다. 메뉴 액션에서 종류·프리셋 갱신 후 onToggle(true)를 호출하고 프로그램에 의한 상태 복원은 자동 활성화하지 않는다. ActivityPreset의 기존 color 필드를 배경 호환용으로 보존하고 foreground 기본값 #919191을 추가한다. 설정 피커 두 개를 같은 행에 배치하고 class별 배경·글자색을 캐시하여 공통 drawNode에서 name에만 적용한다. 빈 화면의 가상 State 미리보기에는 WireMagnet.attach를 호출하지 않는다. 실제 목표에서는 기존 attach 규칙을 유지하여 최종 경로와 일치시킨다. 미리보기 알파·굵기 재설정을 제거해 실제 Wire 렌더러를 그대로 쓴다. 관련 회귀 테스트로 설정 직렬화·기본값·색상·메뉴 활성화·미리보기 꺾임·Design 선택을 검증한다. 헌법은 미작성 템플릿이므로 추가 제약은 없다.

## 텍스트 툴바·확대와 Service 변환 계획

ChildCreationToolbar는 표준 ActionToolbar의 텍스트 액션과 메뉴 화살표를 한 컴포넌트로 제공하고 KeepPopupOnPerform.Never를 지정한다. 선택 텍스트를 본체에 표시하고 Wire도 텍스트 액션으로 표시한다. ZoomControls는 한 패널 안의 세 버튼으로 Canvas zoom을 조절한다. Canvas의 모델→화면 변환은 origin+pan 이후 zoom 배율을 적용하고 역변환을 모든 입력과 이동량에 사용한다. 미니맵 layout에 배율을 전달하여 실제 viewport를 계산하며 HUD와 미니맵은 화면 좌표로 그린다. Clipboard에는 복사 당시 zoom을 저장하고 요소 묶음의 원점을 화면 위치로 환산해 같은 크기의 묶음으로 붙인다.

ModelServiceXmlExporter는 XmlFile에서 그래프와 속성을 읽고 유효성을 확인한 뒤 JDOM으로 안정적인 서비스 XML을 만든다. FileDocumentManagerListener 저장 완료 확장점은 모든 열린 프로젝트 중 파일이 속한 프로젝트 설정을 읽고 WriteAction으로 대상 VFS 파일을 갱신한다. 출력 문서가 열려 있으면 문서도 함께 갱신·저장한다. 잘못된 입력은 기존 파일을 유지하고 등록한 알림 그룹으로 원인을 알린다. .xml 출력 저장은 입력 확장자 검사로 재진입하지 않는다. 통합 테스트는 실제 파일 저장·재저장·열린 출력 문서·오류 보존과 제공 예시의 의미를 검증한다. 기존 미작성 헌법 템플릿에 따른 추가 제한은 없다.

## 안전한 Design 갱신 계획

ModelDesignEditorProvider.kt에 requestRefresh를 두어 예약 상태와 preserveOrigin을 병합하고 Application.invokeLater의 ModalityState.nonModal로 실행한다. selectNotify와 문서 변경 listener는 이 경로를 사용한다. refresh 자체도 UI 스레드 여부와 공개 TransactionGuard.isWritingAllowed를 확인하여 생성자 등 쓰기 불가 호출은 예약으로 전환한다. 안전한 기존 편집 명령은 동기 갱신을 유지한다. 예약 실행 전에 disposed·파일 유효성·프로젝트 종료를 검사한다. 테스트에서는 SDK TransactionGuardImpl.performActivity(false)를 사용하여 실제 쓰기 불가 문맥에서 생성·선택을 재현하고 문서 커밋 상태·최신 Canvas·폐기 후 무변경을 검사한다. 미작성 헌법 템플릿의 추가 제약은 없다.

## 예약 갱신 중 생성 개선 계획

ModelDesignEditorProvider.kt의 createChild에서 scheduled 차단과 Canvas 스냅샷 불일치 시 생성 취소를 제거한다. 쓰기 안전 문맥이면 문서를 커밋하고 최신 XmlFile을 파싱하여 유효성을 확인한 뒤 같은 문서에 생성한다. 쓰기 불가 문맥이면 비모달 예약으로 재시도하며 캡처한 프리셋과 생성 모드를 재검증한다. XML 오류·쓰기 권한·폐기 보호는 유지한다. ModelEditorRefreshTest.kt에 탭 갱신 직후 클릭·이벤트 큐 미처리 연속 클릭·Source 변경 즉시 생성·잘못된 Source·쓰기 불가 생성 검사를 추가한다. 명세와 실행 작업에 모두 대응하며 미작성 헌법 템플릿 외 추가 제약은 없다.

## 배율 선택 목록 계획

ZoomControls.kt의 세 버튼을 IntelliJ ComboBox 하나로 교체한다. 항목은 20..150 step 10의 백분율 문자열이며 선택 변경은 Canvas.setZoom을 호출한다. Canvas의 배율 범위는 0.2~1.5로 변경하고 0.1 단위로 정규화한다. 기존 화면 중앙 유지와 모든 좌표 변환은 재사용한다. ModelDiagramZoomTest.kt는 목록 내용·경계·100% 복원과 150% resize를 검증하고 Clipboard 테스트는 실제 선택 배율로 계산한다. Activity 추가 버튼의 텍스트 테스트에서 확대 Select를 기존 Child 종류 ComboBox 금지 조건과 구분한다.

## 생성 요청 큐 계획

ModelDesignEditorProvider.kt의 생성 콜백을 enqueueChildCreation에 연결하고 ArrayDeque에 요청 스냅샷을 저장한다. 단일 비모달 invokeLater에서 큐를 비워 최신 문서에 순차 적용한다. ModelDiagramCanvas의 creationRevision은 setCreationKind와 생성 취소 시 증가하여 껐다 다시 켠 같은 종류의 오래된 요청도 구분한다. 생성 직전에 세대·모드·권한·문서를 재검증하고 프리셋은 요청의 분리 복제본을 사용한다. dispose에서 큐를 비운다. ModelEditorRefreshTest.kt는 메뉴 직후 요청·FIFO 위치/ID·Source 변경·모드 취소/재활성화·폐기와 기존 더블클릭 정책을 검사한다. 클릭 횟수 예외 구현은 하지 않는다.

## Phase 53: 미세 이동 클릭 생성 보정

Child 추가는 운영체제가 click 이벤트를 생성하는지에 의존하지 않고 Canvas의 press/release 쌍을 기준으로 판정한다. 화면 좌표 이동이 작은 경우만 클릭으로 인정하고, 실제 드래그는 기존 이동 동작을 유지한다. 생성 요청은 큐와 비동기 drain 없이 검증 직후 기존 XML 쓰기 명령을 호출한다. 동일 제스처의 후속 click 이벤트는 입력 식별자와 시간·좌표를 이용해 중복 제거한다. refresh 예약과 모드 세대가 바뀌면 대기 중인 press 상태를 폐기한다.

## 생성 클릭 진단 계획

ChildCreationTrace.kt는 Logger INFO에 [ModelChildCreation]·session·state를 붙인다. IDE IdeEventQueue preprocessor에서 생성 모드의 캔버스 영역 press/release/click을 관찰하며 false를 반환하고 소유 에디터 Disposable로 등록한다. Canvas의 진단 콜백은 같은 MouseEvent identity와 when을 사용하고 clicked 입력 식별자를 큐 요청에 전달한다. ChildCreationToolbar는 선택과 팝업 생명주기를 기록한다. Provider는 요청 ID와 입력 ID를 큐 스냅샷에 보관하고 모든 조기 반환·세대 취소·실행/완료/예외와 갱신 예약 상태를 기록한다. 속성 본문과 move 기록은 제외한다. 검증은 캔버스 거부 이유·일반 클릭 상관관계·큐 취소와 기존 회귀를 포함한다. 헌법은 미작성 템플릿이며 추가 제한은 없다.


## Wire 조건과 동반 이동 계획

Kotlin·Swing·XML PSI 구조를 유지한다. ModelDiagram.kt에서 빈 조건 기본값과 라벨 표시 정책을 정의하고 Canvas의 모든 라벨 사용 경로에 적용한다. Provider는 새 Wire에 success를 저장한다. DiagramEdits.relocateNodes에서 크기가 유지된 양 끝의 동일 이동을 감지해 두 절대 기준점을 이동한다. 상대 bendpoint는 유지하며 기존 쓰기 명령으로 저장하여 Undo를 보장한다. ModelDiagramTest.kt에서 조건과 경로 이동을 검증한다. 헌법은 미작성 템플릿이다.

## 조건 편집 차단과 정수 좌표 계획

ModelDiagram의 조건 편집 가능 정책을 Canvas 더블클릭과 Provider 대화상자 호출 양쪽에서 검사한다. DiagramXmlText에 부호 있는 정수 반올림·좌표 쌍 직렬화·요소 좌표 정규화를 모은다. Provider의 모든 좌표 쓰기에 사용하고 수정 대상 child·wire 및 bendpoint의 기존 좌표도 정규화한다. weight 직렬화는 실수를 유지한다. 내부 기하 계산은 Double을 유지하고 XML 경계에서 반올림한다. 헌법은 미작성 템플릿이다. ModelDiagramTest에 정책·직렬화 회귀를 추가하고 컴파일 및 실행 가능한 검증을 수행한다.

## 기존 서비스 출력 순서 호환 계획

ModelServiceXmlExporter.kt에서 transition을 기본 HashMap에 output 참조 순서로 넣어 조건별 마지막 대상을 유지한다. property는 원본 전체 개수의 2배+1 버킷에 Java String 해시로 배치하는 안정 정렬 후 class·description을 제외한다. 순서 함수는 ModelServiceOutputOrder.kt에 분리하여 IDE 없이 회귀 검증한다. Activity 순서·XML 직렬화·그래프 검증은 기존 경로를 사용한다. ModelEditorServiceExportTest.kt에 사용자 예제를 포함한 순서·중복 검사를 추가한다. 헌법은 미작성 템플릿이며 추가 제약은 없다.

## 서비스 직렬화 형식 고정 계획

ModelServiceXmlWriter.kt에 서비스 스키마 전용 직렬화를 두고 태그별 속성 목록을 명시한다. service의 xmlns는 마지막에 기록하고 빈 태그는 />로 닫는다. 실제 속성 값 이스케이프는 JDOM XMLOutputter.escapeAttributeEntities를 사용한다. 요소는 현재 생성된 JDOM 순서를 유지하고 LF·네 칸 들여쓰기·UTF-8 선언을 적용한다. ModelServiceXmlExporter.kt의 마지막 출력만 새 직렬화기로 교체한다. ModelServiceXmlWriterTest.kt에서 전체 문자열·역순 속성·특수문자·공백 보존을 IDE 없이 검증한다. 미작성 헌법 템플릿 외 추가 제약은 없다.

## 서비스 XML 마지막 개행 제거 계획

ModelServiceXmlWriter.kt에서 완성된 출력의 마지막 LF 하나만 제거한다. 기존 내부 포맷과 값 이스케이프는 유지한다. ModelServiceXmlWriterTest.kt의 전체 기대 문자열에서 마지막 LF를 제거하고 기존 회귀를 실행한다. 헌법은 미작성 템플릿이다.

## 모달 Service Design 갱신 계획

ModelDesignEditorProvider.kt의 requestRefresh는 defaultModalityState를 캡처하고 TransactionGuard.isWriteSafeModality로 검사한 뒤 안전하면 그 상태로 invokeLater한다. 안전하지 않으면 nonModal로 예약한다. 예약 모달 상태와 세대 번호를 보관하여 같은 상태 요청은 병합하고 상태 변경 시 새 예약으로 교체한다. 이전 세대 콜백은 아무 작업도 하지 않는다. refresh의 쓰기 안전성 검사와 생명주기 검사는 유지한다. ModelEditorRefreshTest.kt에 LaterInvocator와 실제 TransactionGuard를 사용한 신규 모달 갱신 및 기존 비모달 예약 승격·Open Service 회귀를 추가한다. 미작성 헌법 템플릿 외 추가 제약은 없다.

## IDE 폴더 선택 창 계획

GlueModelEditorConfigurable.kt의 기존 단일 폴더 descriptor에 isForcedToUseIdeaFileChooser=true를 설정한다. 기존 withRoots와 상대 경로 변환은 유지한다. 모달 갱신 수정과 함께 컴파일하고 기존 경로 설정 회귀를 확인한다.

## 모델 파일 미사용 설정 항목 계획

ModelEditorSettings.kt의 ModelEditorState 마지막 필드에 noUseGlueUmlAdFile=false를 추가하여 기존 위치 인자 호환성을 유지한다. GlueModelEditorConfigurable.kt의 설정 패널 상단에 JBCheckBox를 추가하고 value·load에 연결한다. 기존 detached 복사·영속화·Apply·Reset 비교를 재사용한다. ModelEditorSettingsTest.kt에 체크 상태 적용·취소·화면 재생성·XML 설정 저장 및 이전 설정 기본값 회귀를 추가한다. 모델 열기나 서비스 변환에는 이 값을 사용하지 않는다. 헌법은 미작성 템플릿이다.

## Service 직접 Design 편집 계획

ServiceDiagramAdapter.kt에서 서비스 PSI를 비물리적 GlueDiagram PSI로 투영하여 기존 ModelDesignEditor를 재사용한다. ServiceDiagramLayout.kt는 BFS로 연결 단계를 정하고 동일 단계의 원본 순서대로 가로 배치한다. 순환 및 미연결 구성요소도 유한하게 배치한다. 임시 PSI에 원본 Activity·transition·property 대응 정보를 보관하고 변경 시 실제 서비스 PSI의 대응 부분만 갱신한다. 새 명령 래퍼는 실제 서비스 파일에 대한 기존 쓰기 명령 안에서 투영 편집과 원본 갱신을 수행한다. source modificationStamp가 바뀌면 투영을 재생성하고 좌표를 재배치한다. 기존 서비스 속성·주석·추가 요소는 부분 수정으로 유지한다. geometry만 편집하면 실제 서비스에 쓰지 않는다.

ModelDesignEditorProvider.kt와 ModelSourceEditorProvider.kt는 옵션 활성 상태의 -service.xml을 추가 수용한다. ServiceFiles.kt는 해당 옵션에 따라 서비스 파일 탐색으로 분기하며 ModelServiceSaveListener.kt는 직접 모드에서 자동 내보내기를 하지 않는다. 서비스 기본 구조와 initial·대상 참조·이름을 검증하고 오류는 Source 안내로 표시한다. ModelServiceDirectEditorTest.kt 및 ServiceDiagramLayoutTest.kt로 투영·편집·자동 배치·탐색·보호 회귀를 검증한다. 헌법은 미작성 템플릿이다.

## ELK Layered 배치 계획

버전 카탈로그와 modeleditor/build.gradle.kts에 org.eclipse.elk:org.eclipse.elk.alg.layered:0.12.0 구현 의존성을 고정한다. ServiceDiagramLayout.kt에서 ElkGraph에 시작·종료·Activity와 XML 순서의 연결을 만들고 RecursiveGraphLayoutEngine을 org.eclipse.elk.layered로 실행한다. 플러그인 ClassLoader로 metadata를 초기화한다. 방향 DOWN·routing ORTHOGONAL·seed=1과 중심 고정 입출력 포트를 사용하고 시작·종료 계층 제약을 둔다. 각 edge section의 시작·끝·굽힘점을 순서대로 반환한다.

ServiceDiagramAdapter.kt는 ELK 결과를 노드 위치와 firstRelativePoint·lastRelativePoint·상대 bendpoint로 인코딩한다. 이전 수동 lane 계산은 제거한다. 좌표는 메모리 투영에만 정수로 기록한다. ServiceDiagramLayoutTest.kt는 ELK의 비중첩·직각 경로·연결 수·순환·다중 연결·반복 재현성을 검증하고 ModelServiceDirectEditorTest.kt는 실제 Canvas 경로와 원본 보존을 검증한다. 공식 API 문서를 확인했으나 현재 Maven 네트워크가 프록시 오류로 차단되어 의존성 다운로드와 실행 검증에 환경 제약이 있다. 헌법은 미작성 템플릿이다.

## ELK 런타임 패키징 수정 계획

루트 build.gradle.kts의 pluginComposedModule(project(...))를 공식 문서의 pluginComposedModule(implementation(project(...))) 구성으로 바꾼다. 합성 모듈의 implementation 전이 의존성이 루트 runtimeClasspath에 연결되어 ELK·EMF 등의 라이브러리가 최종 lib에 포함되도록 한다. queryeditor도 같은 방식으로 통일한다. ELK를 plugin.xml의 Eclipse 플러그인 의존성으로 선언하지 않는다. 올바른 공유 읽기 전용 캐시 경로로 실제 Gradle help가 통과했으므로 컴파일·배치 회귀·prepareSandbox·buildPlugin으로 검증한다. 기존 T218 실행 검증을 이어서 완료한다.

## 선분 겹침 완화 계획

ServiceDiagramLayout.kt에서 연결별 고정 포트와 간격을 적용하고 실제 Activity 폭을 반환한다. State 접점은 원형 경계로 보정한다. ModelDiagram.kt에 기본 false인 explicitContacts와 메모리 전용 Key를 두고 DiagramGeometry.kt에서 서비스의 접점을 보존한다. Adapter와 Provider는 정책을 메모리로 전달하며 DiagramEdits와 Canvas는 단일 노드 이동·resize·재연결 시 기존 계산으로 전환한다. XML에는 기록하지 않는다. 실제 ELK와 표시 경로의 겹침·이동 및 기존 모델 회귀를 검증한다. 미작성 헌법 외 추가 제약은 없다.

## 서비스 직접 모드 구조 변경 제한 계획 (US75에서 대체됨)

ModelDesignEditorProvider에서 serviceAdapter 활성 여부를 구조 편집 허용 정책으로 노출한다. ChildCreationToolbar의 Activity/ Wire 생성 액션, Canvas 이동·resize와 wire 수정 콜백, child/wire 속성 편집 및 paste/delete 제공자를 일관되게 차단한다. copy와 선택·탐색은 유지한다. `.glue_uml_ad`는 기존 경로를 그대로 사용한다.

## ELK 서비스 모드 그래프 편집 활성화 계획 (US76에서 저장 시점 변경)

ModelDesignEditorProvider의 구조 편집 정책을 serviceAdapter 모드에서도 writable 문서에 허용한다. 생성 Toolbar는 Activity만 제공한다. 서비스 스키마에서 Initial/Final State는 그래프에서 합성하는 노드이기 때문이다. canvas 이동/resize, Activity/Wire 편집 콜백, paste/delete도 허용한다. ServiceDiagramAdapter의 투영/적용 기능으로 child와 transition 변경을 반영한다. 저장 시점은 US76에서 Commit 버튼 클릭으로 변경되어, Commit 전에는 메모리 projection에만 반영한다. 성공한 Commit 후 projection을 무효화하여 최신 XML로 ELK를 다시 실행하고, 위치만 바꾼 경우는 메모리 projection을 보존한다. Wire gesture는 양 끝 노드가 정해진 뒤에만 createWire 콜백을 호출하므로 미완성 초안은 메모리 미리보기로 남는다. 위치·경로 직렬화는 추가하지 않는다.

## 서비스 직접 편집 Commit 계획

serviceAdapter 모드의 writeModel은 projection만 수정하고 adapter에 pending 상태를 기록한다. 원본 Service XML은 바꾸지 않는다. ChildCreationToolbar는 변경이 있을 때 활성화되는 Commit 버튼을 제공한다. ServiceDiagramAdapter.validate가 문법·이름·wire 연결·property·transaction 조건 및 source modification stamp를 사전 확인한다. 검사 성공 시 단일 WriteCommandAction에서 apply로 원본을 갱신하고 projection을 무효화하여 refresh가 ELK 배치를 다시 계산한다. 실패하면 XML을 건드리지 않고 오류를 알리며 pending projection을 유지한다. 일반 `.glue_uml_ad` writeModel은 기존 즉시 저장 경로를 유지한다.

## 서비스 편집 Rollback·차이 색상·닫기 확인 계획

ServiceDiagramAdapter는 가장 최근에 읽은/Commit한 Diagram을 기준 상태로 기억하고 pending Diagram과 비교해 추가·삭제·수정 node/wire를 산출한다. Canvas는 추가 green, 삭제 red ghost, 수정 orange로 outline/path를 표시한다. ChildCreationToolbar에 pending일 때 활성화되는 Rollback을 두고 ServiceAdapter가 pending을 버린 뒤 원본 XML을 다시 읽어 ELK 재배치한다. `VirtualFilePreCloseCheck` extension point에서 대상 file의 열린 ModelDesignEditor에 pending이 있는지 확인하고, 대화상자 응답에 따라 rollback 후 허용하거나 닫기를 veto한다. `.glue_uml_ad`에는 체크가 영향을 주지 않는다.

## ELK 선 교차 최소화 계획

ServiceDiagramLayout.kt에서 노드의 PortConstraints를 FIXED_POS에서 FIXED_SIDE로 바꾼다. 포트별 x/y를 직접 지정하지 않고 입력은 NORTH, 출력은 SOUTH 면만 지정하여 ELK가 각 면의 포트 위치·순서를 교차 최소화 단계에서 정하도록 한다. 결정적 connections 순서는 서비스 XML의 transition 순서를 유지한다. crossing minimization LAYER_SWEEP을 명시해 실행 정책을 드러내며 직각 route 추출과 메모리 접점 보존은 유지한다. 교차 0개를 요구하거나 보장하지 않는다.

## 서비스 직접 편집 Undo/Redo 분리 계획 (2026-10-24)

- 편집 경계: ServiceDiagramAdapter의 임시 projection 텍스트 스냅샷으로 별도 undo/redo 스택을 유지한다. 각 쓰기 동작 전후의 projection만 기록한다.
- 명령 분리: `.glue_uml_ad`는 기존 WriteCommandAction을 유지한다. 서비스 직접 편집은 projection PSI를 쓰기 액션으로 수정하되 원본 service VirtualFile을 IntelliJ 명령의 대상에 넣지 않는다.
- 사용자 입력: 서비스 Design 툴바에 Undo/Redo를 제공하고, Design에 포커스가 있을 때 Ctrl+Z/Ctrl+Shift+Z를 전용 대기 기록에 연결한다. Source 탭에는 사용자 정의 단축키를 등록하지 않는다.
- 재생: 스냅샷 복원 뒤 projection PSI의 source tag 연결을 다시 구성하고 Design을 새로 그려 색상·Commit 활성화를 현재 상태에서 계산한다.
- 경계 초기화: Commit 성공 및 Rollback에서 대기 기록을 비운다. Commit 자체는 원본 XML의 단일 일반 IntelliJ 쓰기 명령으로 남긴다.
- 검증: 소스 변경 전후에 projection 전용 undo/redo, 기록 비움, 원본 XML 무변경, 표준 편집기 기록 분리를 점검한다. 테스트는 이번 작업에서 실행하지 않는다.
## 서비스 대기 Undo/Redo 비활성화 및 툴바 라벨 (2026-10-24)

이 결정은 앞선 US78의 staged Undo/Redo 구현 계획을 대체한다. ServiceDiagramAdapter는 projection 변경 상태와 하이라이트 비교만 유지하고 별도 undo/redo history를 보유하지 않는다. Service 직접 편집은 기존처럼 projection PSI에 쓰기 액션으로 반영하되 원본 service 파일 대상의 IntelliJ 명령을 만들지 않아 staged 변경이 XML Undo/Redo 기록에 섞이지 않게 한다. 서비스 Design의 Undo/Redo 버튼과 사용자 정의 단축키는 제거한다. Commit의 실제 XML 반영은 일반 XML 명령으로 남기며 Source 탭과 `.glue_uml_ad`의 기본 기록은 유지한다. 툴바 액션 라벨은 Magnetic, Properties로 고정하고 텍스트를 보이게 한다.
## 툴바 액션 아이콘 제거 (2026-10-24)

Magnetic ToggleAction과 Properties 액션에서 icon을 null로 지정하고, 텍스트 라벨 표시 설정은 유지한다. 다른 툴바 액션의 아이콘에는 영향이 없어야 한다.