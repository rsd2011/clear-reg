package com.example.admin.codemanage;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.common.cache.CacheNames;
import com.example.admin.codemanage.model.CodeManageKind;
import com.example.admin.codemanage.model.SystemCommonCode;
import com.example.admin.codemanage.model.SystemCommonCodeType;
import com.example.admin.codemanage.repository.SystemCommonCodeRepository;

@Service
@RequiredArgsConstructor
public class SystemCommonCodeService {

    private final SystemCommonCodeRepository repository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.SYSTEM_COMMON_CODES, key = "#codeType.toUpperCase()")
    public List<SystemCommonCode> findActive(String codeType) {
        return repository.findByCodeTypeOrderByDisplayOrderAscCodeValueAsc(normalizeType(codeType)).stream()
                .filter(SystemCommonCode::isActive)
                .map(SystemCommonCode::copy)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SystemCommonCode> findAll(String codeType) {
        return repository.findByCodeTypeOrderByDisplayOrderAscCodeValueAsc(normalizeType(codeType)).stream()
                .map(SystemCommonCode::copy)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES},
            key = "#codeType.toUpperCase()", allEntries = false)
    public SystemCommonCode create(String codeType, SystemCommonCode request) {
        String normalizedType = normalizeType(codeType);
        repository.findByCodeTypeAndCodeValue(normalizedType, request.getCodeValue())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("이미 존재하는 코드입니다.");
                });
        SystemCommonCode normalized = normalizeRequest(normalizedType, request);
        SystemCommonCode entity = SystemCommonCode.create(
                normalizedType,
                normalized.getCodeValue(),
                normalized.getCodeName(),
                normalized.getDisplayOrder(),
                normalized.getCodeKind(),
                normalized.isActive(),
                normalized.getDescription(),
                normalized.getMetadataJson(),
                normalized.getUpdatedBy(),
                now()
        );
        return repository.save(entity).copy();
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES},
            key = "#codeType.toUpperCase()", allEntries = false)
    public SystemCommonCode update(String codeType, String codeValue, SystemCommonCode request) {
        String normalizedType = normalizeType(codeType);
        SystemCommonCode entity = repository.findByCodeTypeAndCodeValue(normalizedType, codeValue)
                .orElseThrow(() -> new IllegalArgumentException("코드가 존재하지 않습니다."));
        SystemCommonCode normalized = normalizeRequest(normalizedType, request);
        entity.update(normalized.getCodeName(),
                normalized.getDisplayOrder(),
                normalized.getCodeKind(),
                normalized.isActive(),
                normalized.getDescription(),
                normalized.getMetadataJson(),
                normalized.getUpdatedBy(),
                now());
        return repository.save(entity).copy();
    }

    private SystemCommonCode normalizeRequest(String codeType, SystemCommonCode request) {
        SystemCommonCode clone = request.copy();
        CodeManageKind kind = clone.getCodeKind() == null
                ? SystemCommonCodeType.fromCode(codeType).map(SystemCommonCodeType::defaultKind).orElse(CodeManageKind.DYNAMIC)
                : clone.getCodeKind();
        enforceKind(codeType, kind);
        clone = SystemCommonCode.create(
                codeType,
                clone.getCodeValue(),
                clone.getCodeName(),
                clone.getDisplayOrder(),
                kind,
                clone.isActive(),
                clone.getDescription(),
                clone.getMetadataJson(),
                clone.getUpdatedBy() == null ? "system" : clone.getUpdatedBy(),
                clone.getUpdatedAt()
        );
        return clone;
    }

    private void enforceKind(String codeType, CodeManageKind requestedKind) {
        SystemCommonCodeType.fromCode(codeType)
                .filter(type -> type.defaultKind() == CodeManageKind.STATIC)
                .ifPresent(type -> {
                    if (requestedKind != CodeManageKind.STATIC) {
                        throw new IllegalArgumentException("정적 코드 유형은 STATIC 으로만 저장할 수 있습니다.");
                    }
                });
    }

    private String normalizeType(String codeType) {
        if (codeType == null) {
            throw new IllegalArgumentException("codeType must not be null");
        }
        return codeType.toUpperCase(Locale.ROOT);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    @CacheEvict(cacheNames = CacheNames.SYSTEM_COMMON_CODES, allEntries = true)
    public void evictAll() {
        // eviction only
    }
}
