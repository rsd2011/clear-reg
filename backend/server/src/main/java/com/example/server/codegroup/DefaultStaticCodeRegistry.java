package com.example.server.codegroup;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import com.example.admin.codegroup.dto.CodeGroupItem;
import com.example.admin.codegroup.registry.StaticCodeRegistry;
import com.example.admin.codegroup.util.CodeGroupUtils;
import com.example.common.codegroup.annotation.CodeValue;
import com.example.common.codegroup.annotation.ManagedCode;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * StaticCodeRegistry의 기본 구현체.
 *
 * <p>애플리케이션 시작 시 {@link ManagedCode} 어노테이션이 적용된 Enum들을 스캔하여
 * 자동 등록합니다.</p>
 *
 * <p>스캔 대상 패키지: {@code com.example}</p>
 */
@Slf4j
@Component
public class DefaultStaticCodeRegistry implements StaticCodeRegistry {

    private static final String BASE_PACKAGE = "com.example";

    /** groupCode -> Enum 클래스 매핑 */
    private final Map<String, Class<? extends Enum<?>>> enumRegistry = new ConcurrentHashMap<>();

    /** groupCode -> CodeGroupItem 목록 캐시 */
    private final Map<String, List<CodeGroupItem>> itemCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        scanManagedCodeEnums();
        log.info("DefaultStaticCodeRegistry initialized with {} enum(s): {}",
                enumRegistry.size(), enumRegistry.keySet());
    }

    /**
     * @ManagedCode 어노테이션이 적용된 Enum 클래스들을 스캔합니다.
     */
    @SuppressWarnings("unchecked")
    private void scanManagedCodeEnums() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ManagedCode.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(BASE_PACKAGE);

        for (BeanDefinition bd : candidates) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                if (clazz.isEnum()) {
                    ManagedCode annotation = clazz.getAnnotation(ManagedCode.class);
                    if (annotation != null && !annotation.hidden()) {
                        Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) clazz;
                        String groupCode = CodeGroupUtils.toGroupCode(enumClass);
                        enumRegistry.put(groupCode, enumClass);
                        log.debug("Registered @ManagedCode enum: {} -> {}", groupCode, clazz.getName());
                    }
                }
            } catch (ClassNotFoundException e) {
                log.warn("Failed to load class: {}", bd.getBeanClassName(), e);
            }
        }
    }

    @Override
    public Set<String> getGroupCodes() {
        return Collections.unmodifiableSet(enumRegistry.keySet());
    }

    @Override
    public List<CodeGroupItem> getItems(String groupCode) {
        String normalized = normalize(groupCode);
        return itemCache.computeIfAbsent(normalized, this::buildItems);
    }

    @Override
    public Map<String, List<CodeGroupItem>> getAllItems() {
        Map<String, List<CodeGroupItem>> result = new LinkedHashMap<>();
        for (String groupCode : enumRegistry.keySet()) {
            result.put(groupCode, getItems(groupCode));
        }
        return result;
    }

    @Override
    public Optional<CodeGroupItem> getItem(String groupCode, String itemCode) {
        return getItems(groupCode).stream()
                .filter(item -> item.itemCode().equals(itemCode))
                .findFirst();
    }

    @Override
    public boolean hasGroup(String groupCode) {
        return enumRegistry.containsKey(normalize(groupCode));
    }

    @Override
    public Optional<Class<? extends Enum<?>>> getEnumClass(String groupCode) {
        return Optional.ofNullable(enumRegistry.get(normalize(groupCode)));
    }

    @Override
    public Set<Class<? extends Enum<?>>> getRegisteredEnums() {
        return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(enumRegistry.values()));
    }

    @Override
    public List<CodeGroupItem> getCodeGroupItems(Class<? extends Enum<?>> enumClass) {
        String groupCode = CodeGroupUtils.toGroupCode(enumClass);
        return buildItemsFromEnum(groupCode, enumClass);
    }

    @Override
    public void invalidateCache(String groupCode) {
        if (groupCode != null) {
            itemCache.remove(normalize(groupCode));
            log.debug("Cache invalidated for groupCode: {}", groupCode);
        } else {
            itemCache.clear();
            log.debug("All caches invalidated");
        }
    }

    /**
     * groupCode에 해당하는 Enum에서 CodeGroupItem 목록을 생성합니다.
     */
    private List<CodeGroupItem> buildItems(String groupCode) {
        Class<? extends Enum<?>> enumClass = enumRegistry.get(groupCode);
        if (enumClass == null) {
            return Collections.emptyList();
        }
        return buildItemsFromEnum(groupCode, enumClass);
    }

    /**
     * Enum 클래스에서 CodeGroupItem 목록을 생성합니다.
     */
    private List<CodeGroupItem> buildItemsFromEnum(String groupCode, Class<? extends Enum<?>> enumClass) {
        List<CodeGroupItem> items = new ArrayList<>();
        Enum<?>[] constants = enumClass.getEnumConstants();

        for (int i = 0; i < constants.length; i++) {
            Enum<?> constant = constants[i];
            CodeValue codeValue = getCodeValueAnnotation(enumClass, constant.name());

            String label = constant.name();
            String description = null;
            int order = i;
            boolean deprecated = false;

            if (codeValue != null) {
                if (!codeValue.label().isEmpty()) {
                    label = codeValue.label();
                }
                if (!codeValue.description().isEmpty()) {
                    description = codeValue.description();
                }
                if (codeValue.order() != 0) {
                    order = codeValue.order();
                }
                deprecated = codeValue.deprecated();
            }

            items.add(CodeGroupItem.ofStaticEnum(
                    groupCode,
                    constant.name(),
                    label,
                    order,
                    description
            ));
        }

        // displayOrder 기준 정렬
        items.sort((a, b) -> {
            int orderA = a.displayOrder() != null ? a.displayOrder() : Integer.MAX_VALUE;
            int orderB = b.displayOrder() != null ? b.displayOrder() : Integer.MAX_VALUE;
            return Integer.compare(orderA, orderB);
        });

        return Collections.unmodifiableList(items);
    }

    /**
     * Enum 상수에 적용된 @CodeValue 어노테이션을 조회합니다.
     */
    private CodeValue getCodeValueAnnotation(Class<?> enumClass, String constantName) {
        try {
            Field field = enumClass.getDeclaredField(constantName);
            return field.getAnnotation(CodeValue.class);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
