# 데이터 모델

- DiagramNode: 종류, ID, 이름, 사각 범위, input/output 인덱스 목록. Activity 높이는 30이며 State 크기는 20이다.
- DiagramWire: 선언 순서 번호, XML ID, 조건 이름, 시작·종료 노드, firstRelativePoint와 lastRelativePoint, Bendpoint 목록.
- DiagramBendpoint: firstDimension, secondDimension, weight. weight는 경로 계산에서 무시한다.
- Diagram: 노드 목록, 연결선 목록, 파싱 오류 안내. 누락된 양 끝은 null로 유지하고 렌더링에서 제외한다.
- DiagramPath: 보정된 시작점, 보간된 꺾임점 목록, 보정된 종료점. 좌표 계산은 확대·스크롤과 독립적이다.

원문 XML이 저장의 유일한 원본이며 Design은 파싱 결과만 보유한다. 알 수 없는 속성과 weight를 재직렬화하지 않는다.

## 화면 선택 상태

- 선택 목록: 추가 순서를 유지한 Child 집합이며 마지막 요소가 마지막 선택 대상이다. Source 재파싱 시 종류와 ID가 같은 Child로 다시 연결하고 삭제된 Child는 선택에서 제외한다.
- 범위 Box: 드래그 시작점과 현재점의 최소·최대 좌표로 구성하며 네 모서리가 모두 포함된 Child만 선택한다. Shift 드래그는 시작 시 선택을 유지한다.
- 탐색 이동량: 우측 드래그의 누적 화면 이동 벡터다. 선택과 Hit 검사에 같은 역변환을 사용하며 XML에 저장하지 않는다.
- 핸들: 선택 Child 경계의 8방향에 표시하는 6×6 사각형이다. 마지막 선택 대상은 중앙 4×4 표시를 추가하고 나머지는 검정 테두리를 표시한다.
- Wire 선택: 선언 순서 인덱스로 유지하며 양 끝과 꺾임점에 6/4px 핸들, 각 선분 중점에 4/2px 핸들을 가진다. Child 선택과 상호 배타적이다.
- 이동 상태: 드래그 시작 모델, 마우스 시작점, 이동할 Child 목록 및 Source 변경 번호를 보관한다. 화면 미리보기 후 release 시 이동은 location, 너비 조절은 size 너비와 필요한 location을 단일 편집으로 반영한다. 오른쪽 조절은 왼쪽 변을 고정하고 왼쪽 조절은 오른쪽 변을 고정하며 최소 너비는 16이다.
- 안정된 화면: 최초 유효 모델에서 정한 원점과 별도의 Navigation 이동량을 유지한다. 편집 후 캔버스 크기는 줄이지 않는다.
- Wire 편집 상태: 원본 Wire, 드래그 종류, 꺾임점 또는 선분 인덱스와 마우스 좌표를 보관한다. 빈 공간 끝점은 임시 좌표를 사용하고 유효 Child 위에서는 새 시작·끝으로 미리보기한다. release 시 유효 대상만 XML에 반영하고 잘못된 대상은 원복한다. 꺾임점은 상대 벡터로 변환하며 삽입 시 다른 점을 재인코딩한다.

- 일괄 편집 상태: 선택된 Child 집합을 유지하고 이동은 전체에, 너비 조절은 Activity 집합에 공통 변화량을 적용한다. 최소 너비 16에 맞춰 전체 변화량을 제한한다.
- Wire 참조 편집: 선언 인덱스로 child input/output 참조를 이동하고 관련 없는 참조 및 wire 순서를 유지한다.

- 가상 State 미리보기: 빈 공간 마우스 좌표는 20×20 State의 중심이다. 해당 도형은 모델에 추가하지 않고 기존 종류별 경로 계산에만 사용한다. 저장된 상대 벡터를 유지하고 변경된 시작·끝 중심을 기준으로 first+(second-first)×(i+1)/(n+1)을 계산한다. 원래 XML과 weight는 변경하지 않는다.

- 중점 분할 상태: Down 당시 원본 Wire와 선분 인덱스를 유지한다. 원래 직선까지의 수직 거리 5px 이내는 원본, 초과는 임시 삽입 모델이다. 임시 모델에서 다시 거리를 재지 않으며 안쪽 release는 편집 콜백을 호출하지 않는다.

- 기존 꺾임점 병합: Down 당시 원본에서 드래그한 점을 제외한 후보 경로를 계산한다. 이웃점 직선 5px 이내이면 후보, 초과하면 원래 점의 이동 모델을 표시한다. release 시 삭제 인덱스를 XML 편집에 전달하고 다른 태그를 유지한다.

- Activity 직결 공통 기준: 시작·끝 드래그 모두 현재 마우스 좌표를 firstRelativePoint에 반영한다. end 드래그는 lastRelativePoint도 갱신한다. 렌더링과 XML이 같은 공통 기준을 사용하여 재파싱 후 위치를 유지한다.

## 병합 장애물

DiagramNode의 기존 위치·크기·종류로 장애물 도형을 계산한다. 추가 영속 필드는 없다. 병합 가능 여부는 임계치 충족과 드래그 위치 인접 두 선분의 Child 내부 비관통을 함께 만족해야 한다. State의 정상 원 외곽 접속은 허용하고 내부 통과는 차단한다.


연결 Child 검사 범위: 관통 검사 대상은 해당 Wire의 start·end만이며 다른 Child는 무시한다. 기존점·중점 모두 같은 규칙으로 동작한다.

## 직결 중점 이동과 범위 밖 분할

새 필드 없이 firstRelativePoint의 x 또는 y만 공통 유효 구간으로 제한해 갱신한다. 반대 축, lastRelativePoint, 연결 참조와 bendpoints는 유지한다. 꺾임점 없는 Activity 양 끝에만 적용하며 수직 연결을 우선한다.

## 범위 밖 임시 점

유효 구간 밖에서는 firstRelativePoint를 강제로 경계에 고정하지 않고 원본을 유지한다. 현재 마우스 위치를 firstDimension·secondDimension으로 인코딩한 bendpoint 하나를 weight 0.5로 삽입한다. 범위 안으로 돌아오면 원본 직결 Wire의 공통 좌표 이동으로 복원한다.

## 직결 복원 조건

공통 축 범위 안이라는 조건과 이동한 B의 A–B/B–C가 start·end 내부를 관통하지 않는다는 조건을 모두 만족해야 직결 좌표로 갱신한다. 실패하면 원본에 삽입한 bendpoint를 유지한다. 별도 저장 필드는 추가하지 않는다.

## 제스처 취소와 클립보드

- 제스처 스냅샷: 시작 선택 Child·Wire, 시작 Navigation, Child dragBase 또는 originalWire. Esc는 해당 값으로 복원하고 미리보기와 진행 상태를 제거한다.
- 복사 스냅샷: Child의 불변 모델·XML, 내부 Wire의 불변 모델·원래 선언 인덱스·XML, 원본 namespace 선언, 복사 화면 오프셋. 원본 문서 변경과 독립적이다.
- 붙여넣기 횟수: 복사본마다 성공한 횟수를 공유하며 15px씩 누적 이동한다. 새 복사본은 0으로 시작한다.
- ID 할당: 대상 문서 전체 상위 요소 숫자 ID의 BigInteger 최대치 다음부터 새 Child·Wire 순서로 할당한다. input/output은 원래 Wire 인덱스에서 대상 문서 추가 인덱스로 변환하고 외부 Wire는 제거한다.
- 좌표: Child location과 Wire firstRelativePoint·lastRelativePoint는 paste delta를 더한다. firstDimension·secondDimension과 weight는 유지한다. namespace·property·기타 속성을 그대로 복제한다.

## 삭제 집합과 남은 참조

- 삭제 Child: 현재 선택한 Child의 ID·종류에 대응하는 실제 XML 태그 집합.
- 삭제 Wire: 선택 Wire 인덱스와 삭제 Child의 input/output에 있는 유효한 Wire 인덱스의 합집합.
- 참조 매핑: 삭제되지 않은 Wire의 기존 선언 인덱스→새 선언 인덱스. Wire ID와 순서는 유지한다.
- 남은 모든 Child의 input/output에서 삭제 참조를 빼고 남은 참조 인덱스를 치환한다. 외부 의미의 기타 토큰과 속성은 유지한다. 상대편 Child 자체는 삭제하지 않는다.
- 전체 삭제는 한 Undo 명령이며 선택은 해제한다. Navigation·화면 원점과 기존 클립보드 스냅샷은 유지한다.

## 속성 입력과 생성 상태

- ActivityPropertiesData: name, 일반 property 행(key/value/sourceIndex), description 문자열. sourceIndex는 원래 property 태그 위치이며 새 행은 null이다.
- 일반 property key는 공백만 있는 값·중복·description 예약 키를 허용하지 않는다. description은 전용 textarea에서 편집하고 비우면 해당 property를 삭제한다.
- 창이 열린 시점 문서 modificationStamp를 보관한다. 확인 때 stamp·쓰기 가능 여부를 검사하고 실제 변경만 한 명령으로 저장한다. 취소·무변경은 저장하지 않는다.
- 생성 종류는 Activity·InitialState·FinalState 중 하나 또는 모드 종료 null이다. 마지막 선택 종류는 프로젝트 설정에 보관하며 토글 상태는 편집기별로 유지한다.
- ghost는 마우스 모델 좌표를 중심으로 Activity 100×30 또는 State 20×20의 임시 도형이다. XML은 클릭에서만 생성한다. 새 ID는 모든 상위 요소 숫자 최대치+1이며 연결 참조는 만들지 않는다.
- 사용자 문자열은 DiagramXmlText에서 XML 엔티티로 저장·복원하여 특수문자와 description 줄바꿈을 보존한다.

## property 행 복제와 통합 버튼

- CopiedPropertyRows: key/value 목록만 보관하며 XML sourceIndex를 제거한다. 로컬 DataFlavor와 quoted TSV를 지원한다.
- 행 붙여넣기: 선택 마지막 행 다음 또는 끝에 새 행을 삽입하고 새 행들을 선택한다. duplicate key는 기존 확인 검증에서 처리한다. XML은 Activity 확인 전까지 바꾸지 않는다.
- Open Class 입력: 현재 테이블의 class 값(편집 셀 commit, 양끝 공백 제거). 성공은 파일 탐색, 실패는 인라인 메시지로 처리한다.
- ChildCreationButton: 마지막 selectedKind·토글 상태·종류 popup 메뉴. 별도 Select 컴포넌트는 사용하지 않는다. 화살표는 토글 상태를 바꾸지 않는다.
- ghost 데이터는 기존 위치·크기를 사용하며 반투명 상태를 제거하고 실제 도형과 같은 렌더러로 그린다.

## 표준 툴바 및 State 접점

- ChildCreationToolbar: ActionToolbar·고정 본체 ToggleAction·3종류 선택 ToggleAction·표준 SplitButtonAction. 종류는 PropertiesComponent에 보관하고 생성 on/off는 Canvas가 소유한다.
- State 접점: 시작 firstRelativePoint·끝 lastRelativePoint는 원 내부/경계의 선호 위치다. 실제 표시점은 중심에서 선호 방향으로 반경 10에 투영한다. 유효한 선호가 없으면 인접 점 방향을 사용한다.
- State 이동: 접점 선호도 중심 이동 delta만큼 갱신한다. XML과 Canvas 미리보기에 같은 DiagramEdits.relocateNodes를 적용한다. 상대 bendpoint 벡터는 그대로 유지한다.
- Activity 좌우 접점: 상대 x가 밖이고 y가 Activity 높이 범위 안이면 좌우 변에 연결하며 y를 상하 8px 여백으로 제한한다.

## 미니맵 투영

MiniMapProjection은 화면 고정 frame, 그리기 contentArea, 내용 경계 modelBounds, 역변환된 modelViewport, 두 경계의 합집합 overviewBounds, 축소 scale과 offsetX/Y를 보관한다. model 좌표는 point×scale+offset으로 변환한다. 내용 경계에는 라벨과 Wire 꺾임점도 포함하고 음수 좌표를 유지한다. overviewBounds를 기본 맞춤 배율로 투영하여 도형과 viewport 전체를 contentArea 안에 표시한다. viewport를 교차 사각형으로 바꾸지 않으며 가로·세로 비율을 보존한다.

미니맵은 별도 영속 모델이 아니며 현재 diagram/paths를 사용한다. 선택·ghost·Undo/XML 상태는 추가하지 않는다. Activity 수직 접점 x는 좌우 min(8,width/2) 범위로 제한한다.

## 정렬과 클래스 입력 상태

AlignmentResult는 보정된 delta와 AlignmentGuide 목록을 보관한다. 가이드는 모델 좌표의 from/to이며 영속화하지 않는다. Canvas는 제스처 대상·마지막 마우스 위치·가이드만 임시 보관한다. 다중 너비 조절 결과 delta.x는 모든 선택 Activity에 더할 너비 변화량이다.

ActivityClassCellEditor는 현재 class 행의 Java 참조 문서를 편집하는 EditorTextField를 보관한다. 완료 시 텍스트를 PropertyTableModel에 전달하며 원본 ActivityProperty와 XML은 확인 전까지 유지한다. key를 수정하거나 행을 붙여넣은 다음에도 현재 모델 key를 기준으로 편집기를 다시 선택한다.

MiniMapProjection.scale은 기본 맞춤 배율이며 30% 추가 축소는 제거했다. 제목은 그리지 않는다.

## 거리 가이드와 메모리 Query 목록

AlignmentGuide.distance가 null이면 일반 정렬 점선이고 값이 있으면 외곽 거리 치수선이다. 동일한 두 간격을 각각 가이드로 생성한다. MoveMatch는 보정량·최종 경계에 따른 가이드 생성·최종 유효성 검사를 보관한다. 축별 간격 후보는 같은 줄/열의 인접 쌍에서 생성한다.

IndexedQueryId는 id·description·fileUrl·fileName을 보관한다. QueryIdIndexService는 VirtualFile별 불변 ID 목록을 메모리에 보관하고 변경 시 해당 파일 목록을 교체한다. isUpdating은 최초 수집/대기/처리 상태를 나타내며 snapshot(prefix)은 현재 메모리만 조회한다. 같은 파일 내 중복 ID는 하나로 묶고 다른 파일의 동일 ID는 각각 유지한다. 영속 상태는 없고 프로젝트를 닫으면 목록을 비운다.

QueryIdCellEditor는 sql-key Value만 PlainText 문서와 TextCompletionProvider를 사용한다. 후보 삽입값은 ID 문자열이며 설명과 파일 경로는 표시 정보다. 다른 셀과 확인/취소 규칙은 그대로 유지한다.

## 탐색과 모델 편집 설정

QueryLocation은 id·VirtualFile·offset이며 QueryNavigation은 find/open을 제공한다. QueryEditorNavigation은 ID 또는 offset으로 현재 Design 항목을 선택한다. pending Query ID는 VirtualFile의 일시 데이터이며 로딩 후 소비하고 XML에 저장하지 않는다.

ModelDiagram.properties는 GlueDiagram 직접 자식 property의 key/value 목록이다. Child 속성과 독립이며 선언 순서와 디코딩된 줄바꿈을 보존한다. HUD는 description을 제외하고 key=value로 표시하며, 루트 속성 편집은 Properties와 별도 Description에서 전체 값을 사용한다.

ModelEditorState는 프로젝트 상대 serviceDirectory와 ActivityPreset 목록을 가진다. 프로젝트 루트는 .이다. 기존 내부 절대/URL 경로는 설정 적용 시 상대 경로로 변환하고 외부 디렉터리는 거부한다. ActivityPreset은 name·PresetProperty 목록·description·색상 문자열이다. ModelEditorSettings는 프로젝트 glue-model-editor.xml에 영속 저장하고 입출력 복제본으로 설정 편집과 원본을 분리한다.

Canvas.navigationOnly는 뷰포트 크기 추적이고 wireMagnetEnabled는 로컬 토글 상태(기본 off)다. Navigation·Range box·미니맵 클릭은 XML과 별개다. 자석 접점은 기존 Wire 선호 좌표와 bendpoint 표현으로 저장한다.
- 확장 모델: DiagramNode.className은 class property를 공백 정리하여 읽는다. 설정의 첫 번째 일치 class 색상을 사용하고 빈 class는 제외한다. 생성 시 프리셋을 복제하며 Wire 저장은 새 ID와 양쪽 참조를 하나의 명령으로 반영한다.
프리셋 표시 확장: color는 Background 저장값으로 유지한다. foreground는 #919191 기본값을 갖는 새 저장값이다. class 매칭 우선순위는 기존대로 첫 항목이며 글자색은 Activity name에만 적용한다.

배율 확장: Canvas zoom은 0.25~2.0의 에디터 로컬 상태이다. ClipboardData에는 복사 당시 zoom을 기본 1.0으로 추가한다. Service 변환 결과는 정상 XML 문자열 또는 구체적인 검증 오류이며 Diagram/Activity description은 출력에서 제외한다.

안전한 갱신 상태: scheduled는 단일 예약 존재 여부이며 pendingPreserveOrigin은 요청 중 하나라도 위치 유지를 요구했는지 나타낸다. 실행 시 값을 비우고 최신 문서를 읽는다. 문서·모델 저장 형식은 바꾸지 않는다.

배율 최신 규칙: 에디터 로컬 zoom은 0.2~1.5이고 0.1 간격으로 정규화한다. 이전 0.25~2.0 범위를 대체하며 Clipboard 저장 형식과 좌표 변환식은 유지한다.

생성 요청 큐 상태: ChildCreationRequest는 kind·location·분리 복제한 preset·creationRevision을 보관한다. ArrayDeque는 FIFO이며 중복 좌표의 별개 요청도 병합하지 않는다. Canvas 모드 세대는 변경과 취소 때 증가한다. 배치 처리 직전에 세대를 검증하며 폐기 시 큐를 비운다.

진단 상관관계: 에디터 session과 생성 request는 단조 증가 ID를 사용한다. 입력은 MouseEvent identity와 when으로 연결한다. 요청 스냅샷에 requestId와 inputId를 추가하며 문서 저장 형식은 변경하지 않는다.


## Wire 보완

빈 condition의 기본값은 success이다. showsCondition은 InitialState 출발이면 거짓이다. 동반 이동 시 firstRelativePoint와 lastRelativePoint에 변화량을 더하고 상대 bendpoint와 weight는 보존한다.

## 정수 좌표와 편집 정책

location·size·끝점·bendpoint 좌표는 저장 시 부호 있는 정수 쌍이다. size=-1은 유지한다. canEditCondition은 InitialState 출발이면 거짓이며 표시 여부와 독립적으로 편집 요청을 차단한다.

## 서비스 출력 순서 모델

Activity 순서는 Child 순서이다. transition은 조건명을 키로 하는 맵이며 마지막 대상이 우선한다. property 정렬은 출력 제외 메타데이터를 포함한 개수로 버킷을 계산하고 안정 정렬한다. 값은 보존한다.

## 서비스 직렬화 형식

요소와 속성 값은 기존 JDOM 모델을 유지한다. 직렬화 시 태그별 속성 목록을 적용하고 service의 namespace 선언을 마지막에 배치한다. 요소 순서는 변경하지 않는다.

## 갱신 예약 상태 보완

scheduledRefreshModality는 현재 예약 모달 상태이며 refreshGeneration은 예약 교체 시 증가한다. pendingPreserveOrigin은 유효 예약 실행까지 병합한다. 이전 세대 콜백은 상태와 화면을 수정하지 않는다.

## 모델 파일 미사용 설정 값

ModelEditorState.noUseGlueUmlAdFile은 Boolean이며 기본값은 false이다. 설정 복사·영속화·UI 로드와 적용에서 유지한다. 현재 파일 기능에서 이 값을 읽지는 않는다.

## Service 직접 편집 모델

ServiceDiagramAdapter는 원본 XmlFile과 메모리 XmlFile, 원본 modificationStamp, child ID별 실제 Activity, wire ID별 실제 transition, 메모리 property별 원본 property 대응을 세션에서만 보관한다. 실제 프로젝트 파일이나 설정에는 저장하지 않는다. Source 변경·Undo·Redo로 stamp가 바뀌면 대응과 자동 배치를 다시 생성한다.

initial은 가상 InitialState의 단일 output에 대응하고 value=end는 가상 FinalState에 대응한다. Activity name·class와 property name/value, transition name/value, transaction-manager id/commit은 기존 모델의 이름·클래스·속성·연결·다이어그램 속성으로 투영한다. 세션 위치·크기·bendpoint는 원본 XML에 기록하지 않는다.

## ELK 배치 결과

ServiceDiagramPlacement에 실제 edge section 순서의 routes를 추가한다. ServiceDiagramRoute는 start·end·bends를 가진다. 순서는 initial 연결이 있으면 첫 번째이고 이후 Activity XML 순서 및 각 transition 순서이다. 이름·조건·실제 연결 관계와 wire 인덱스는 바꾸지 않는다. 좌표는 정수로 반올림한 메모리 값이며 파일이나 프로젝트 설정에 저장하지 않는다.

## 분리된 자동 접점 모델

ServiceDiagramPlacement.widths는 포트 수를 고려한 실제 폭이다. DiagramWire.explicitContacts는 기본 false이며 서비스 자동 wire의 XmlTag userData에만 true를 둔다. 접점 정책을 XML 속성으로 기록하지 않는다. 그룹 이동은 유지하고 한쪽 이동/resize/재연결은 정책을 해제한다.
