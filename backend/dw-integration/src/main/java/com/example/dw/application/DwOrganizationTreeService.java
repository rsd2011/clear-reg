package com.example.dw.application;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.common.cache.CacheNames;
import com.example.dw.domain.HrOrganizationEntity;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;

@Service
@RequiredArgsConstructor
public class DwOrganizationTreeService {

    private static final String TREE_KEY = "tree";

    private final HrOrganizationRepository organizationRepository;

    @Cacheable(cacheNames = CacheNames.DW_ORG_TREE, key = "'" + TREE_KEY + "'", sync = true)
    public OrganizationTreeSnapshot snapshot() {
        List<HrOrganizationEntity> entities = organizationRepository.findAll(Sort.by("organizationCode"));
        return OrganizationTreeSnapshot.from(entities);
    }

    @CacheEvict(cacheNames = CacheNames.DW_ORG_TREE, key = "'" + TREE_KEY + "'")
    public void evict() {
        // cache eviction only
    }

    public static final class OrganizationTreeSnapshot {

        private final Map<String, DwOrganizationNode> nodesByCode;
        private final Map<String, List<DwOrganizationNode>> childrenByCode;
        private final List<DwOrganizationNode> ordered;

        private OrganizationTreeSnapshot(Map<String, DwOrganizationNode> nodesByCode,
                                         Map<String, List<DwOrganizationNode>> childrenByCode,
                                         List<DwOrganizationNode> ordered) {
            this.nodesByCode = nodesByCode;
            this.childrenByCode = childrenByCode;
            this.ordered = ordered;
        }

        static OrganizationTreeSnapshot from(List<HrOrganizationEntity> entities) {
            Map<String, DwOrganizationNode> nodes = new HashMap<>();
            Map<String, List<DwOrganizationNode>> children = new HashMap<>();
            List<DwOrganizationNode> orderedNodes = new ArrayList<>(entities.size());

            entities.stream()
                    .sorted(Comparator.comparing(HrOrganizationEntity::getOrganizationCode))
                    .forEach(entity -> {
                        DwOrganizationNode node = DwOrganizationNode.fromEntity(entity);
                        nodes.put(node.organizationCode(), node);
                        orderedNodes.add(node);
                        String parent = node.parentOrganizationCode();
                        if (parent != null) {
                            children.computeIfAbsent(parent, key -> new ArrayList<>()).add(node);
                        }
                    });

            children.values().forEach(list -> list.sort(Comparator.comparing(DwOrganizationNode::organizationCode)));

            return new OrganizationTreeSnapshot(Collections.unmodifiableMap(nodes),
                    Collections.unmodifiableMap(children),
                    Collections.unmodifiableList(orderedNodes));
        }

        public static OrganizationTreeSnapshot fromNodes(List<DwOrganizationNode> nodes) {
            Map<String, DwOrganizationNode> nodesByCode = new HashMap<>();
            Map<String, List<DwOrganizationNode>> childrenByCode = new HashMap<>();
            List<DwOrganizationNode> ordered = new ArrayList<>(nodes.size());

            nodes.stream()
                    .sorted(Comparator.comparing(DwOrganizationNode::organizationCode))
                    .forEach(node -> {
                        nodesByCode.put(node.organizationCode(), node);
                        ordered.add(node);
                        String parent = node.parentOrganizationCode();
                        if (parent != null) {
                            childrenByCode.computeIfAbsent(parent, key -> new ArrayList<>()).add(node);
                        }
                    });

            childrenByCode.values().forEach(list -> list.sort(Comparator.comparing(DwOrganizationNode::organizationCode)));

            return new OrganizationTreeSnapshot(Collections.unmodifiableMap(nodesByCode),
                    Collections.unmodifiableMap(childrenByCode),
                    Collections.unmodifiableList(ordered));
        }

        public Optional<DwOrganizationNode> node(String organizationCode) {
            return Optional.ofNullable(nodesByCode.get(organizationCode));
        }

        public List<DwOrganizationNode> flatten() {
            return ordered;
        }

        public List<DwOrganizationNode> descendantsIncluding(String organizationCode) {
            DwOrganizationNode root = nodesByCode.get(organizationCode);
            if (root == null) {
                return List.of();
            }
            List<DwOrganizationNode> result = new ArrayList<>();
            ArrayDeque<DwOrganizationNode> stack = new ArrayDeque<>();
            stack.push(root);
            while (!stack.isEmpty()) {
                DwOrganizationNode current = stack.pop();
                result.add(current);
                List<DwOrganizationNode> children = childrenByCode.getOrDefault(current.organizationCode(), List.of());
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(children.get(i));
                }
            }
            return result;
        }

        public List<DwOrganizationNode> ancestors(String organizationCode) {
            DwOrganizationNode node = nodesByCode.get(organizationCode);
            if (node == null) {
                return List.of();
            }
            List<DwOrganizationNode> result = new ArrayList<>();
            String parent = node.parentOrganizationCode();
            while (parent != null) {
                DwOrganizationNode parentNode = nodesByCode.get(parent);
                if (parentNode == null) {
                    break;
                }
                result.add(parentNode);
                parent = parentNode.parentOrganizationCode();
            }
            return result;
        }
    }
}
