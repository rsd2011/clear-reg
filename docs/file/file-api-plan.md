# 파일 서비스 API/포트 사양

## 1. 목적
- 파일 업로드/목록/다운로드/삭제 기능을 `dw-gateway`와 외부 서비스에서 재사용할 수 있도록 DTO 기반 포트/REST 사양을 정의한다.
- 내부 JPA 엔티티(`StoredFile`, `StoredFileVersion`) 의존을 제거하고, `backend/platform`의 공용 DTO(`FileMetadataDto`, `FileDownload`, `FileStatus`)로 교체한다.

## 2. 공통 DTO
| DTO | 설명 |
| --- | --- |
| `FileMetadataDto` | 파일 메타데이터 (ID, 이름, MIME, 크기, 체크섬, 상태, 생성/갱신 시각) |
| `FileDownload` | `FileMetadataDto` + 실제 리소스 스트림(Resource) |
| `FileStatus` | `ACTIVE` / `DELETED` |

## 3. 포트 인터페이스 (서비스 계층)
```
FileMetadataDto upload(FileUploadCommand command)
List<FileMetadataDto> list()
FileMetadataDto getMetadata(UUID id)
FileDownload download(UUID id, String actor)
FileMetadataDto delete(UUID id, String actor)
```
- `FileUploadCommand` 는 기존 로직을 그대로 사용(입력 스트림 공급자 포함). 추후 REST 클라이언트는 Multipart → InputStream 으로 변환하여 위 포트를 호출한다.

## 4. REST API 사양 (dw-gateway)
| Method | Path | 설명 | 요청 | 응답 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/files` | 파일 업로드 | Multipart(`file`, `metadata`) | `FileMetadataDto` JSON |
| `GET` | `/api/files` | 파일 목록 | `page`,`size` 쿼리 | `List<FileMetadataDto>` |
| `GET` | `/api/files/{id}` | 메타데이터 조회 | Path ID | `FileMetadataDto` |
| `GET` | `/api/files/{id}/content` | 파일 다운로드 | Path ID | 바이너리 응답 + 헤더(`X-File-Metadata`) |
| `DELETE` | `/api/files/{id}` | 삭제 | Path ID | `FileMetadataDto` |

- 다운로드 응답 헤더에 `Content-Disposition`, `Content-Type`, `Content-Length` 를 포함하고, 추가로 `X-File-Metadata` 헤더에 JSON 직렬화된 `FileMetadataDto` 를 넣어 클라이언트가 메타 정보를 식별할 수 있게 한다.
- 인증/권한: `@RequirePermission(feature = FeatureCode.FILE, action = {UPLOAD, READ, DOWNLOAD, DELETE})` 적용.

## 5. 구현 단계
1. (완료) `FileManagementPort` → DTO 기반으로 재정의, 서버 어댑터 변환 로직 추가.
2. `dw-gateway` 모듈에 파일 컨트롤러/서비스 어댑터 작성 (추후 작업):
   - 파일 저장 로직(`FileService`)을 재사용할 수 있도록 별도 모듈로 추출하거나, REST 호출/메시지 큐를 통해 위임.
   - 다운로드 엔드포인트는 `ResponseEntity<Resource>` 를 반환하며, 메타데이터 헤더 추가.
3. `dw-gateway-client` 모듈에 파일 API 클라이언트 추가:
   - 업로드는 Multipart → `FileUploadCommand` 로 변환.
   - 다운로드는 응답 헤더에서 `X-File-Metadata` 파싱 후 `FileDownload`로 반환.
4. 서버/배치 등 소비자가 `dw-gateway-client`를 통해 파일 기능을 호출하도록 리팩터링.

## 6. 추가 고려사항
- **저장소 추상화**: 현재 `FileService` 는 로컬 스토리지를 사용하므로, S3 같은 외부 스토리지로 전환할 경우에도 DTO 계층은 변경되지 않는다.
- **보안**: 다운로드/삭제 시 감사 로그(`FileAccessLog`)는 계속 기록되며, dw-gateway에서도 동일 로직이 실행되어야 한다.
- **대역폭 보호**: 대용량 업로드는 presigned URL 전략을 병행할 수 있도록 향후 `FileUploadCommand`에 `storageStrategy` 필드 추가 예정.
