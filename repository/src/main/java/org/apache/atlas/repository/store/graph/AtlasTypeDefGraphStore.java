/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.store.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.GraphTransaction;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.listener.ActiveStateChangeHandler;
import org.apache.atlas.listener.ChangedTypeDefs;
import org.apache.atlas.listener.TypeDefChangeListener;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef.AtlasClassificationDefs;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEntityDef.AtlasEntityDefs;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumDefs;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasStructDefs;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.util.FilterUtil;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeRegistry.AtlasTransientTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * Abstract class for graph persistence store for TypeDef
 */
public abstract class AtlasTypeDefGraphStore implements AtlasTypeDefStore, ActiveStateChangeHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AtlasTypeDefGraphStore.class);

    private final AtlasTypeRegistry typeRegistry;

    private final Set<TypeDefChangeListener> typeDefChangeListeners;

    protected AtlasTypeDefGraphStore(AtlasTypeRegistry typeRegistry,
                                     Set<TypeDefChangeListener> typeDefChangeListeners) {
        this.typeRegistry = typeRegistry;
        this.typeDefChangeListeners = typeDefChangeListeners;
    }

    protected abstract AtlasEnumDefStore getEnumDefStore(AtlasTypeRegistry typeRegistry);

    protected abstract AtlasStructDefStore getStructDefStore(AtlasTypeRegistry typeRegistry);

    protected abstract AtlasClassificationDefStore getClassificationDefStore(AtlasTypeRegistry typeRegistry);

    protected abstract AtlasEntityDefStore getEntityDefStore(AtlasTypeRegistry typeRegistry);

    @Override
    public void init() throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        AtlasTypesDef typesDef = new AtlasTypesDef(getEnumDefStore(ttr).getAll(),
                                                   getStructDefStore(ttr).getAll(),
                                                   getClassificationDefStore(ttr).getAll(),
                                                   getEntityDefStore(ttr).getAll());

        ttr.addTypes(typesDef);

        typeRegistry.commitTransientTypeRegistry(ttr);
    }

    @Override
    @GraphTransaction
    public AtlasEnumDef createEnumDef(AtlasEnumDef enumDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.addType(enumDef);

        AtlasEnumDef ret = getEnumDefStore(ttr).create(enumDef);

        ttr.updateGuid(ret.getName(), ret.getGuid());

        notifyListeners(TypeDefChangeType.CREATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public List<AtlasEnumDef> getAllEnumDefs() throws AtlasBaseException {
        List<AtlasEnumDef> ret = null;

        Collection<AtlasEnumDef> enumDefs = typeRegistry.getAllEnumDefs();

        ret = CollectionUtils.isNotEmpty(enumDefs) ?
                new ArrayList<>(enumDefs) : Collections.<AtlasEnumDef>emptyList();

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEnumDef getEnumDefByName(String name) throws AtlasBaseException {
        AtlasEnumDef ret = typeRegistry.getEnumDefByName(name);
        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }
        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEnumDef getEnumDefByGuid(String guid) throws AtlasBaseException {
        AtlasEnumDef ret = typeRegistry.getEnumDefByGuid(guid);
        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }
        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEnumDef updateEnumDefByName(String name, AtlasEnumDef enumDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.updateTypeByName(name, enumDef);

        AtlasEnumDef ret = getEnumDefStore(ttr).updateByName(name, enumDef);

        notifyListeners(TypeDefChangeType.UPDATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEnumDef updateEnumDefByGuid(String guid, AtlasEnumDef enumDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.updateTypeByGuid(guid, enumDef);

        AtlasEnumDef ret = getEnumDefStore(ttr).updateByGuid(guid, enumDef);

        notifyListeners(TypeDefChangeType.UPDATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public void deleteEnumDefByName(String name) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        AtlasEnumDef byName = typeRegistry.getEnumDefByName(name);

        ttr.removeTypeByName(name);

        getEnumDefStore(ttr).deleteByName(name);

        notifyListeners(TypeDefChangeType.DELETE, Lists.newArrayList(byName));

        typeRegistry.commitTransientTypeRegistry(ttr);
    }

    @Override
    @GraphTransaction
    public void deleteEnumDefByGuid(String guid) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        AtlasEnumDef byGuid = typeRegistry.getEnumDefByGuid(guid);

        ttr.removeTypeByGuid(guid);

        getEnumDefStore(ttr).deleteByGuid(guid);

        notifyListeners(TypeDefChangeType.DELETE, Lists.newArrayList(byGuid));

        typeRegistry.commitTransientTypeRegistry(ttr);
    }

    @Override
    @GraphTransaction
    public AtlasEnumDefs searchEnumDefs(SearchFilter filter) throws AtlasBaseException {
        AtlasEnumDefs search = getEnumDefStore(typeRegistry).search(filter);
        if (search == null || search.getTotalCount() == 0) {
            throw new AtlasBaseException(AtlasErrorCode.NO_SEARCH_RESULTS);
        }
        return search;
    }

    @Override
    @GraphTransaction
    public AtlasStructDef createStructDef(AtlasStructDef structDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.addType(structDef);

        AtlasStructDef ret = getStructDefStore(ttr).create(structDef, null);

        ttr.updateGuid(ret.getName(), ret.getGuid());

        notifyListeners(TypeDefChangeType.CREATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public List<AtlasStructDef> getAllStructDefs() throws AtlasBaseException {
        List<AtlasStructDef> ret = null;

        Collection<AtlasStructDef> structDefs = typeRegistry.getAllStructDefs();

        ret = CollectionUtils.isNotEmpty(structDefs) ?
                new ArrayList<>(structDefs) : Collections.<AtlasStructDef>emptyList();

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasStructDef getStructDefByName(String name) throws AtlasBaseException {
        AtlasStructDef ret = typeRegistry.getStructDefByName(name);
        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }
        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasStructDef getStructDefByGuid(String guid) throws AtlasBaseException {
        AtlasStructDef ret = typeRegistry.getStructDefByGuid(guid);
        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }
        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasStructDef updateStructDefByName(String name, AtlasStructDef structDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.updateTypeByName(name, structDef);

        AtlasStructDef ret = getStructDefStore(ttr).updateByName(name, structDef);

        notifyListeners(TypeDefChangeType.UPDATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasStructDef updateStructDefByGuid(String guid, AtlasStructDef structDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.updateTypeByGuid(guid, structDef);

        AtlasStructDef ret = getStructDefStore(ttr).updateByGuid(guid, structDef);

        notifyListeners(TypeDefChangeType.UPDATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public void deleteStructDefByName(String name) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        AtlasStructDef byName = typeRegistry.getStructDefByName(name);

        ttr.removeTypeByName(name);

        getStructDefStore(ttr).deleteByName(name, null);

        notifyListeners(TypeDefChangeType.DELETE, Lists.newArrayList(byName));

        typeRegistry.commitTransientTypeRegistry(ttr);
    }

    @Override
    @GraphTransaction
    public void deleteStructDefByGuid(String guid) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        AtlasStructDef byGuid = typeRegistry.getStructDefByGuid(guid);

        ttr.removeTypeByGuid(guid);

        getStructDefStore(ttr).deleteByGuid(guid, null);

        notifyListeners(TypeDefChangeType.DELETE, Lists.newArrayList(byGuid));

        typeRegistry.commitTransientTypeRegistry(ttr);
    }

    @Override
    @GraphTransaction
    public AtlasStructDefs searchStructDefs(SearchFilter filter) throws AtlasBaseException {
        AtlasStructDefs search = getStructDefStore(typeRegistry).search(filter);
        if (search == null || search.getTotalCount() == 0) {
            throw new AtlasBaseException(AtlasErrorCode.NO_SEARCH_RESULTS);
        }
        return search;
    }

    @Override
    @GraphTransaction
    public AtlasClassificationDef createClassificationDef(AtlasClassificationDef classificationDef)
        throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.addType(classificationDef);

        AtlasClassificationDef ret = getClassificationDefStore(ttr).create(classificationDef, null);

        ttr.updateGuid(ret.getName(), ret.getGuid());

        notifyListeners(TypeDefChangeType.CREATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public List<AtlasClassificationDef> getAllClassificationDefs() throws AtlasBaseException {
        List<AtlasClassificationDef> ret = null;

        Collection<AtlasClassificationDef> classificationDefs = typeRegistry.getAllClassificationDefs();

        ret = CollectionUtils.isNotEmpty(classificationDefs) ?
                new ArrayList<>(classificationDefs) : Collections.<AtlasClassificationDef>emptyList();

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasClassificationDef getClassificationDefByName(String name) throws AtlasBaseException {
        AtlasClassificationDef ret = typeRegistry.getClassificationDefByName(name);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }
        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasClassificationDef getClassificationDefByGuid(String guid) throws AtlasBaseException {
        AtlasClassificationDef ret = typeRegistry.getClassificationDefByGuid(guid);
        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }
        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasClassificationDef updateClassificationDefByName(String name, AtlasClassificationDef classificationDef)
        throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.updateTypeByName(name, classificationDef);

        AtlasClassificationDef ret = getClassificationDefStore(ttr).updateByName(name, classificationDef);

        notifyListeners(TypeDefChangeType.UPDATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasClassificationDef updateClassificationDefByGuid(String guid, AtlasClassificationDef classificationDef)
        throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.updateTypeByGuid(guid, classificationDef);

        AtlasClassificationDef ret = getClassificationDefStore(ttr).updateByGuid(guid, classificationDef);

        notifyListeners(TypeDefChangeType.UPDATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public void deleteClassificationDefByName(String name) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        AtlasClassificationDef byName = typeRegistry.getClassificationDefByName(name);

        ttr.removeTypeByName(name);

        getClassificationDefStore(ttr).deleteByName(name, null);

        notifyListeners(TypeDefChangeType.DELETE, Lists.newArrayList(byName));

        typeRegistry.commitTransientTypeRegistry(ttr);
    }

    @Override
    @GraphTransaction
    public void deleteClassificationDefByGuid(String guid) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        AtlasClassificationDef byGuid = typeRegistry.getClassificationDefByGuid(guid);

        ttr.removeTypeByGuid(guid);

        getClassificationDefStore(ttr).deleteByGuid(guid, null);

        notifyListeners(TypeDefChangeType.DELETE, Lists.newArrayList(byGuid));

        typeRegistry.commitTransientTypeRegistry(ttr);
    }

    @Override
    @GraphTransaction
    public AtlasClassificationDefs searchClassificationDefs(SearchFilter filter) throws AtlasBaseException {
        AtlasClassificationDefs search = getClassificationDefStore(typeRegistry).search(filter);
        if (search == null || search.getTotalCount() == 0) {
            throw new AtlasBaseException(AtlasErrorCode.NO_SEARCH_RESULTS);
        }
        return search;
    }

    @Override
    @GraphTransaction
    public AtlasEntityDef createEntityDef(AtlasEntityDef entityDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.addType(entityDef);

        AtlasEntityDef ret = getEntityDefStore(ttr).create(entityDef, null);

        ttr.updateGuid(ret.getName(), ret.getGuid());

        notifyListeners(TypeDefChangeType.CREATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public List<AtlasEntityDef> getAllEntityDefs() throws AtlasBaseException {
        List<AtlasEntityDef> ret = null;

        Collection<AtlasEntityDef> entityDefs = typeRegistry.getAllEntityDefs();

        ret = CollectionUtils.isNotEmpty(entityDefs) ?
                new ArrayList<>(entityDefs) : Collections.<AtlasEntityDef>emptyList();

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntityDef getEntityDefByName(String name) throws AtlasBaseException {
        AtlasEntityDef ret = typeRegistry.getEntityDefByName(name);
        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }
        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntityDef getEntityDefByGuid(String guid) throws AtlasBaseException {
        AtlasEntityDef ret = typeRegistry.getEntityDefByGuid(guid);
        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }
        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntityDef updateEntityDefByName(String name, AtlasEntityDef entityDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.updateTypeByName(name, entityDef);

        AtlasEntityDef ret = getEntityDefStore(ttr).updateByName(name, entityDef);

        notifyListeners(TypeDefChangeType.UPDATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntityDef updateEntityDefByGuid(String guid, AtlasEntityDef entityDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.updateTypeByGuid(guid, entityDef);

        AtlasEntityDef ret = getEntityDefStore(ttr).updateByGuid(guid, entityDef);

        notifyListeners(TypeDefChangeType.UPDATE, Arrays.asList(ret));

        typeRegistry.commitTransientTypeRegistry(ttr);

        return ret;
    }

    @Override
    @GraphTransaction
    public void deleteEntityDefByName(String name) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        AtlasEntityDef byName = typeRegistry.getEntityDefByName(name);

        ttr.removeTypeByName(name);

        getEntityDefStore(ttr).deleteByName(name, null);

        notifyListeners(TypeDefChangeType.DELETE, Lists.newArrayList(byName));

        typeRegistry.commitTransientTypeRegistry(ttr);
    }

    @Override
    @GraphTransaction
    public void deleteEntityDefByGuid(String guid) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        AtlasEntityDef byGuid = typeRegistry.getEntityDefByGuid(guid);

        ttr.removeTypeByGuid(guid);

        getEntityDefStore(ttr).deleteByGuid(guid, null);

        notifyListeners(TypeDefChangeType.DELETE, Lists.newArrayList(byGuid));

        typeRegistry.commitTransientTypeRegistry(ttr);
    }

    @Override
    @GraphTransaction
    public AtlasEntityDefs searchEntityDefs(SearchFilter filter) throws AtlasBaseException {
        AtlasEntityDefs search = getEntityDefStore(typeRegistry).search(filter);
        if (search == null || search.getTotalCount() == 0) {
            throw new AtlasBaseException(AtlasErrorCode.NO_SEARCH_RESULTS);
        }
        return search;
    }

    @Override
    @GraphTransaction
    public AtlasTypesDef createTypesDef(AtlasTypesDef typesDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasTypeDefGraphStore.createTypesDef(enums={}, structs={}, classfications={}, entities={})",
                      CollectionUtils.size(typesDef.getEnumDefs()),
                      CollectionUtils.size(typesDef.getStructDefs()),
                      CollectionUtils.size(typesDef.getClassificationDefs()),
                      CollectionUtils.size(typesDef.getEntityDefs()));
        }

        AtlasTypesDef ret = new AtlasTypesDef();

        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.addTypes(typesDef);

        AtlasEnumDefStore           enumDefStore     = getEnumDefStore(ttr);
        AtlasStructDefStore         structDefStore   = getStructDefStore(ttr);
        AtlasClassificationDefStore classifiDefStore = getClassificationDefStore(ttr);
        AtlasEntityDefStore         entityDefStore   = getEntityDefStore(ttr);

        List<Object> preCreateStructDefs   = new ArrayList<>();
        List<Object> preCreateClassifiDefs = new ArrayList<>();
        List<Object> preCreateEntityDefs   = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(typesDef.getEnumDefs())) {
            for (AtlasEnumDef enumDef : typesDef.getEnumDefs()) {
                AtlasEnumDef createdDef = enumDefStore.create(enumDef);

                ttr.updateGuid(createdDef.getName(), createdDef.getGuid());

                ret.getEnumDefs().add(createdDef);
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                preCreateStructDefs.add(structDefStore.preCreate(structDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            for (AtlasClassificationDef classifiDef : typesDef.getClassificationDefs()) {
                preCreateClassifiDefs.add(classifiDefStore.preCreate(classifiDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                preCreateEntityDefs.add(entityDefStore.preCreate(entityDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            int i = 0;
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                AtlasStructDef createdDef = structDefStore.create(structDef, preCreateStructDefs.get(i));

                ttr.updateGuid(createdDef.getName(), createdDef.getGuid());

                ret.getStructDefs().add(createdDef);
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            int i = 0;
            for (AtlasClassificationDef classifiDef : typesDef.getClassificationDefs()) {
                AtlasClassificationDef createdDef = classifiDefStore.create(classifiDef, preCreateClassifiDefs.get(i));

                ttr.updateGuid(createdDef.getName(), createdDef.getGuid());

                ret.getClassificationDefs().add(createdDef);
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            int i = 0;
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                AtlasEntityDef createdDef = entityDefStore.create(entityDef, preCreateEntityDefs.get(i));

                ttr.updateGuid(createdDef.getName(), createdDef.getGuid());

                ret.getEntityDefs().add(createdDef);
                i++;
            }
        }

        List<AtlasBaseTypeDef> createdTypeDefs = new ArrayList<>();
        createdTypeDefs.addAll(ret.getEnumDefs());
        createdTypeDefs.addAll(ret.getStructDefs());
        createdTypeDefs.addAll(ret.getClassificationDefs());
        createdTypeDefs.addAll(ret.getEntityDefs());

        ChangedTypeDefs changedTypeDefs = new ChangedTypeDefs();
        changedTypeDefs.setCreateTypeDefs(createdTypeDefs);

        notifyListeners(changedTypeDefs);

        typeRegistry.commitTransientTypeRegistry(ttr);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasTypeDefGraphStore.createTypesDef(enums={}, structs={}, classfications={}, entities={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()));
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasTypesDef updateTypesDef(AtlasTypesDef typesDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasTypeDefGraphStore.updateTypesDef(enums={}, structs={}, classfications={}, entities={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()));
        }

        AtlasTypesDef ret = new AtlasTypesDef();

        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.updateTypes(typesDef);

        AtlasEnumDefStore           enumDefStore     = getEnumDefStore(ttr);
        AtlasStructDefStore         structDefStore   = getStructDefStore(ttr);
        AtlasClassificationDefStore classifiDefStore = getClassificationDefStore(ttr);
        AtlasEntityDefStore         entityDefStore   = getEntityDefStore(ttr);

        if (CollectionUtils.isNotEmpty(typesDef.getEnumDefs())) {
            for (AtlasEnumDef enumDef : typesDef.getEnumDefs()) {
                ret.getEnumDefs().add(enumDefStore.update(enumDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                ret.getStructDefs().add(structDefStore.update(structDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            for (AtlasClassificationDef classifiDef : typesDef.getClassificationDefs()) {
                ret.getClassificationDefs().add(classifiDefStore.update(classifiDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                ret.getEntityDefs().add(entityDefStore.update(entityDef));
            }
        }

        List<AtlasBaseTypeDef> updatedTypeDefs = new ArrayList<>();
        updatedTypeDefs.addAll(ret.getEnumDefs());
        updatedTypeDefs.addAll(ret.getStructDefs());
        updatedTypeDefs.addAll(ret.getClassificationDefs());
        updatedTypeDefs.addAll(ret.getEntityDefs());

        ChangedTypeDefs changedTypeDefs = new ChangedTypeDefs();
        changedTypeDefs.setUpdatedTypeDefs(updatedTypeDefs);

        notifyListeners(changedTypeDefs);

        typeRegistry.commitTransientTypeRegistry(ttr);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasTypeDefGraphStore.updateTypesDef(enums={}, structs={}, classfications={}, entities={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()));
        }

        return ret;

    }

    @Override
    @GraphTransaction
    public void deleteTypesDef(AtlasTypesDef typesDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasTypeDefGraphStore.deleteTypesDef(enums={}, structs={}, classfications={}, entities={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()));
        }

        AtlasTransientTypeRegistry ttr = typeRegistry.createTransientTypeRegistry();

        ttr.addTypes(typesDef);

        AtlasEnumDefStore           enumDefStore     = getEnumDefStore(ttr);
        AtlasStructDefStore         structDefStore   = getStructDefStore(ttr);
        AtlasClassificationDefStore classifiDefStore = getClassificationDefStore(ttr);
        AtlasEntityDefStore         entityDefStore   = getEntityDefStore(ttr);

        List<Object> preDeleteStructDefs   = new ArrayList<>();
        List<Object> preDeleteClassifiDefs = new ArrayList<>();
        List<Object> preDeleteEntityDefs   = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                if (StringUtils.isNotBlank(structDef.getGuid())) {
                    preDeleteStructDefs.add(structDefStore.preDeleteByGuid(structDef.getGuid()));
                } else {
                    preDeleteStructDefs.add(structDefStore.preDeleteByName(structDef.getName()));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            for (AtlasClassificationDef classifiDef : typesDef.getClassificationDefs()) {
                if (StringUtils.isNotBlank(classifiDef.getGuid())) {
                    preDeleteClassifiDefs.add(classifiDefStore.preDeleteByGuid(classifiDef.getGuid()));
                } else {
                    preDeleteClassifiDefs.add(classifiDefStore.preDeleteByName(classifiDef.getName()));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                if (StringUtils.isNotBlank(entityDef.getGuid())) {
                    preDeleteEntityDefs.add(entityDefStore.preDeleteByGuid(entityDef.getGuid()));
                } else {
                    preDeleteEntityDefs.add(entityDefStore.preDeleteByName(entityDef.getName()));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            int i = 0;
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                if (StringUtils.isNotBlank(structDef.getGuid())) {
                    structDefStore.deleteByGuid(structDef.getGuid(), preDeleteStructDefs.get(i));
                } else {
                    structDefStore.deleteByName(structDef.getName(), preDeleteStructDefs.get(i));
                }
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            int i = 0;
            for (AtlasClassificationDef classifiDef : typesDef.getClassificationDefs()) {
                if (StringUtils.isNotBlank(classifiDef.getGuid())) {
                    classifiDefStore.deleteByGuid(classifiDef.getGuid(), preDeleteClassifiDefs.get(i));
                } else {
                    classifiDefStore.deleteByName(classifiDef.getName(), preDeleteClassifiDefs.get(i));
                }
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            int i = 0;
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                if (StringUtils.isNotBlank(entityDef.getGuid())) {
                    entityDefStore.deleteByGuid(entityDef.getGuid(), preDeleteEntityDefs.get(i));
                } else {
                    entityDefStore.deleteByName(entityDef.getName(), preDeleteEntityDefs.get(i));
                }
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEnumDefs())) {
            for (AtlasEnumDef enumDef : typesDef.getEnumDefs()) {
                if (StringUtils.isNotBlank(enumDef.getGuid())) {
                    enumDefStore.deleteByGuid(enumDef.getGuid());
                } else {
                    enumDefStore.deleteByName(enumDef.getName());
                }
            }
        }

        Iterable<AtlasBaseTypeDef> deleted = Iterables.concat(typesDef.getEnumDefs(), typesDef.getClassificationDefs(),
                typesDef.getClassificationDefs(), typesDef.getEntityDefs());

        notifyListeners(TypeDefChangeType.DELETE, Lists.newArrayList(deleted));

        typeRegistry.commitTransientTypeRegistry(ttr);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasTypeDefGraphStore.deleteTypesDef(enums={}, structs={}, classfications={}, entities={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()));
        }
    }

    @Override
    @GraphTransaction
    public AtlasTypesDef searchTypesDef(SearchFilter searchFilter) throws AtlasBaseException {
        AtlasTypesDef typesDef = new AtlasTypesDef();
        Predicate searchPredicates = FilterUtil.getPredicateFromSearchFilter(searchFilter);
        try {
            List<AtlasEnumDef> enumDefs = getEnumDefStore(typeRegistry).getAll();
            CollectionUtils.filter(enumDefs, searchPredicates);
            typesDef.setEnumDefs(enumDefs);
        } catch (AtlasBaseException ex) {
            LOG.error("Failed to retrieve the EnumDefs", ex);
        }

        try {
            List<AtlasStructDef> structDefs = getStructDefStore(typeRegistry).getAll();
            CollectionUtils.filter(structDefs, searchPredicates);
            typesDef.setStructDefs(structDefs);
        } catch (AtlasBaseException ex) {
            LOG.error("Failed to retrieve the StructDefs", ex);
        }

        try {
            List<AtlasClassificationDef> classificationDefs = getClassificationDefStore(typeRegistry).getAll();
            CollectionUtils.filter(classificationDefs, searchPredicates);
            typesDef.setClassificationDefs(classificationDefs);
        } catch (AtlasBaseException ex) {
            LOG.error("Failed to retrieve the ClassificationDefs", ex);
        }

        try {
            List<AtlasEntityDef> entityDefs = getEntityDefStore(typeRegistry).getAll();
            CollectionUtils.filter(entityDefs, searchPredicates);
            typesDef.setEntityDefs(entityDefs);
        } catch (AtlasBaseException ex) {
            LOG.error("Failed to retrieve the EntityDefs", ex);
        }

        if (typesDef.isEmpty()) {
            throw new AtlasBaseException(AtlasErrorCode.NO_SEARCH_RESULTS);
        }
        return typesDef;
    }

    @Override
    public void instanceIsActive() throws AtlasException {
        try {
            init();
        } catch (AtlasBaseException e) {
            LOG.error("Failed to init after becoming active", e);
        }
    }

    @Override
    public void instanceIsPassive() throws AtlasException {
        LOG.info("Not reacting to a Passive state change");
    }

    private void notifyListeners(TypeDefChangeType type, List<? extends AtlasBaseTypeDef> typeDefs)
            throws AtlasBaseException {
        ChangedTypeDefs changedTypeDefs = new ChangedTypeDefs();
        switch (type) {
            case CREATE:
                changedTypeDefs.setCreateTypeDefs(typeDefs);
                break;
            case UPDATE:
                changedTypeDefs.setUpdatedTypeDefs(typeDefs);
                break;
            case DELETE:
                changedTypeDefs.setDeletedTypeDefs(typeDefs);
                break;
        }

        notifyListeners(changedTypeDefs);
    }

    private void notifyListeners(ChangedTypeDefs changedTypeDefs) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(typeDefChangeListeners)) {
            for (TypeDefChangeListener changeListener : typeDefChangeListeners) {
                try {
                    changeListener.onChange(changedTypeDefs);
                } catch (AtlasBaseException e) {
                    LOG.error("OnChange failed for listener {}", changeListener.getClass().getName());
                    throw e;
                }
            }
        }
    }

    private enum TypeDefChangeType {
        CREATE, UPDATE, DELETE
    }
}
