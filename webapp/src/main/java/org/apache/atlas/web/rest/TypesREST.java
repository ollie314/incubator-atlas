/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.web.rest;

import com.google.inject.Inject;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef.AtlasClassificationDefs;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEntityDef.AtlasEntityDefs;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumDefs;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasStructDefs;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.web.util.Servlets;
import org.apache.http.annotation.Experimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;


@Path("v2/types")
@Singleton
public class TypesREST {
    private static final Logger LOG = LoggerFactory.getLogger(TypesREST.class);

    private AtlasTypeDefStore typeDefStore;

    @Context
    private HttpServletRequest httpServletRequest;

    @Inject
    public TypesREST(AtlasTypeDefStore typeDefStore) {
        LOG.info("new TypesREST");
        this.typeDefStore = typeDefStore;
    }

    /******* EnumDef REST calls *******/

    @POST
    @Path("/enumdef")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasEnumDef createEnumDef(AtlasEnumDef enumDef) throws Exception {
        AtlasEnumDef ret = null;

        try {
            ret = typeDefStore.createEnumDef(enumDef);
            return ret;
        } catch (AtlasBaseException ex) {
            throw new WebApplicationException(Servlets.getErrorResponse(ex, Response.Status.BAD_REQUEST));
        }
    }

    @GET
    @Path("/enumdef/name/{name}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasEnumDef getEnumDefByName(@PathParam("name") String name) throws Exception {
        AtlasEnumDef ret = null;

        ret = typeDefStore.getEnumDefByName(name);

        return ret;
    }

    @GET
    @Path("/enumdef/guid/{guid}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasEnumDef getEnumDefByGuid(@PathParam("guid") String guid) throws Exception {
        AtlasEnumDef ret = null;

        ret = typeDefStore.getEnumDefByGuid(guid);

        return ret;
    }

    @PUT
    @Path("/enumdef/name/{name}")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasEnumDef updateEnumDefByName(@PathParam("name") String name, AtlasEnumDef enumDef) throws Exception {
        AtlasEnumDef ret = null;

        ret = typeDefStore.updateEnumDefByName(name, enumDef);

        return ret;
    }

    @PUT
    @Path("/enumdef/guid/{guid}")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasEnumDef updateEnumDefByGuid(@PathParam("guid") String guid, AtlasEnumDef enumDef) throws Exception {
        AtlasEnumDef ret = null;

        ret = typeDefStore.updateEnumDefByGuid(guid, enumDef);

        return ret;
    }

    @DELETE
    @Path("/enumdef/name/{name}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public void deleteEnumDefByName(@PathParam("name") String name) throws Exception {
        typeDefStore.deleteEnumDefByName(name);
    }

    @DELETE
    @Path("/enumdef/guid/{guid}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public void deleteEnumDefByGuid(@PathParam("guid") String guid) throws Exception {
        typeDefStore.deleteEnumDefByGuid(guid);
    }

    @GET
    @Path("/enumdef")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasEnumDefs searchEnumDefs() throws Exception {
        AtlasEnumDefs ret = null;

        SearchFilter filter = getSearchFilter();

        ret = typeDefStore.searchEnumDefs(filter);

        return ret;
    }


    /******* StructDef REST calls *******/

    @POST
    @Path("/structdef")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasStructDef createStructDef(AtlasStructDef structDef) throws Exception {
        AtlasStructDef ret = null;

        try {
            ret = typeDefStore.createStructDef(structDef);
            return ret;
        } catch (AtlasBaseException ex) {
            throw new WebApplicationException(Servlets.getErrorResponse(ex, Response.Status.BAD_REQUEST));
        }

    }

    @GET
    @Path("/structdef/name/{name}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasStructDef getStructDefByName(@PathParam("name") String name) throws Exception {
        AtlasStructDef ret = null;

        ret = typeDefStore.getStructDefByName(name);

        return ret;
    }

    @GET
    @Path("/structdef/guid/{guid}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasStructDef getStructDefByGuid(@PathParam("guid") String guid) throws Exception {
        AtlasStructDef ret = null;

        ret = typeDefStore.getStructDefByGuid(guid);

        return ret;
    }

    @PUT
    @Path("/structdef/name/{name}")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasStructDef updateStructDefByName(@PathParam("name") String name, AtlasStructDef structDef) throws Exception {
        AtlasStructDef ret = null;

        ret = typeDefStore.updateStructDefByName(name, structDef);

        return ret;
    }

    @PUT
    @Path("/structdef/guid/{guid}")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasStructDef updateStructDefByGuid(@PathParam("guid") String guid, AtlasStructDef structDef) throws Exception {
        AtlasStructDef ret = null;

        ret = typeDefStore.updateStructDefByGuid(guid, structDef);

        return ret;
    }

    @DELETE
    @Path("/structdef/name/{name}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public void deleteStructDefByName(@PathParam("name") String name) throws Exception {
        typeDefStore.deleteStructDefByName(name);
    }

    @DELETE
    @Path("/structdef/guid/{guid}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public void deleteStructDefByGuid(@PathParam("guid") String guid) throws Exception {
        typeDefStore.deleteStructDefByGuid(guid);
    }

    @GET
    @Path("/structdef")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasStructDefs searchStructDefs() throws Exception {
        AtlasStructDefs ret = null;

        SearchFilter filter = getSearchFilter();
        ret = typeDefStore.searchStructDefs(filter);

        return ret;
    }

    /******* ClassificationDef REST calls *******/

    @POST
    @Path("/classificationdef")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasClassificationDef createClassificationDef(AtlasClassificationDef classificationDef) throws Exception {
        AtlasClassificationDef ret = null;

        try {
            ret = typeDefStore.createClassificationDef(classificationDef);
            return ret;
        } catch (AtlasBaseException ex) {
            throw new WebApplicationException(Servlets.getErrorResponse(ex, Response.Status.BAD_REQUEST));
        }
    }

    @GET
    @Path("/classificationdef/name/{name}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasClassificationDef getClassificationDefByName(@PathParam("name") String name) throws Exception {
        AtlasClassificationDef ret = null;

        ret = typeDefStore.getClassificationDefByName(name);

        return ret;
    }

    @GET
    @Path("/classificationdef/guid/{guid}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasClassificationDef getClassificationDefByGuid(@PathParam("guid") String guid) throws Exception {
        AtlasClassificationDef ret = null;

        ret = typeDefStore.getClassificationDefByGuid(guid);

        return ret;
    }

    @PUT
    @Path("/classificationdef/name/{name}")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasClassificationDef updateClassificationDefByName(@PathParam("name") String name, AtlasClassificationDef classificationDef) throws Exception {
        AtlasClassificationDef ret = null;

        ret = typeDefStore.updateClassificationDefByName(name, classificationDef);

        return ret;
    }

    @PUT
    @Path("/classificationdef/guid/{guid}")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasClassificationDef updateClassificationDefByGuid(@PathParam("guid") String guid, AtlasClassificationDef classificationDef) throws Exception {
        AtlasClassificationDef ret = null;

        ret = typeDefStore.updateClassificationDefByGuid(guid, classificationDef);

        return ret;
    }

    @DELETE
    @Path("/classificationdef/name/{name}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public void deleteClassificationDefByName(@PathParam("name") String name) throws Exception {
        typeDefStore.deleteClassificationDefByName(name);
    }

    @DELETE
    @Path("/classificationdef/guid/{guid}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public void deleteClassificationDefByGuid(@PathParam("guid") String guid) throws Exception {
        typeDefStore.deleteClassificationDefByGuid(guid);
    }

    @GET
    @Path("/classificationdef")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasClassificationDefs searchClassificationDefs() throws Exception {
        AtlasClassificationDefs ret = null;

        SearchFilter filter = getSearchFilter();
        ret = typeDefStore.searchClassificationDefs(filter);

        return ret;
    }

    /******* EntityDef REST calls *******/

    @POST
    @Path("/entitydef")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasEntityDef createEntityDef(AtlasEntityDef entityDef) throws Exception {
        AtlasEntityDef ret = null;

        try {
            ret = typeDefStore.createEntityDef(entityDef);
            return ret;
        } catch (AtlasBaseException ex) {
            throw new WebApplicationException(Servlets.getErrorResponse(ex, Response.Status.BAD_REQUEST));
        }
    }

    @GET
    @Path("/entitydef/name/{name}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasEntityDef getEntityDefByName(@PathParam("name") String name) throws Exception {
        AtlasEntityDef ret = null;

        ret = typeDefStore.getEntityDefByName(name);

        return ret;
    }

    @GET
    @Path("/entitydef/guid/{guid}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasEntityDef getEntityDefByGuid(@PathParam("guid") String guid) throws Exception {
        AtlasEntityDef ret = null;

        ret = typeDefStore.getEntityDefByGuid(guid);

        return ret;
    }

    @PUT
    @Path("/entitydef/name/{name}")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    @Experimental
    public AtlasEntityDef updateEntityDefByName(@PathParam("name") String name, AtlasEntityDef entityDef) throws Exception {
        AtlasEntityDef ret = null;

        ret = typeDefStore.updateEntityDefByName(name, entityDef);

        return ret;
    }

    @PUT
    @Path("/entitydef/guid/{guid}")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    @Experimental
    public AtlasEntityDef updateEntityDefByGuid(@PathParam("guid") String guid, AtlasEntityDef entityDef) throws Exception {
        AtlasEntityDef ret = null;

        ret = typeDefStore.updateEntityDefByGuid(guid, entityDef);

        return ret;
    }

    @DELETE
    @Path("/entitydef/name/{name}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    @Experimental
    public void deleteEntityDef(@PathParam("name") String name) throws Exception {
        typeDefStore.deleteEntityDefByName(name);
    }

    @DELETE
    @Path("/entitydef/guid/{guid}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    @Experimental
    public void deleteEntityDefByGuid(@PathParam("guid") String guid) throws Exception {
        typeDefStore.deleteEntityDefByGuid(guid);
    }

    @GET
    @Path("/entitydef")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasEntityDefs searchEntityDefs() throws Exception {
        AtlasEntityDefs ret = null;

        SearchFilter filter = getSearchFilter();
        ret = typeDefStore.searchEntityDefs(filter);

        return ret;
    }

    /******************************************************************/
    /** Bulk API operations                                          **/
    /******************************************************************/


    /**
     * Bulk retrieval API for retrieving all type definitions in Atlas
     * @return A composite wrapper object with lists of all type definitions
     * @throws Exception
     */
    @GET
    @Path("/typedefs")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasTypesDef getAllTypeDefs() throws Exception {
        SearchFilter searchFilter = getSearchFilter();

        AtlasTypesDef typesDef = null;

        try {
            typesDef = typeDefStore.searchTypesDef(searchFilter);
        } catch (AtlasBaseException ex) {
            throw new WebApplicationException(Servlets.getErrorResponse(ex, Response.Status.NOT_FOUND));
        }

        return typesDef;
    }

    /**
     * Bulk create APIs for all atlas type definitions, only new definitions will be created.
     * Any changes to the existing definitions will be discarded
     * @param typesDef A composite wrapper object with corresponding lists of the type definition
     * @return A composite wrapper object with lists of type definitions that were successfully
     * created
     * @throws Exception
     */
    @POST
    @Path("/typedefs")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasTypesDef createAtlasTypeDefs(final AtlasTypesDef typesDef) throws Exception {
        AtlasTypesDef ret = null;
        try {
            ret = typeDefStore.createTypesDef(typesDef);
        } catch (AtlasBaseException ex) {
            throw new WebApplicationException(Servlets.getErrorResponse(ex, Response.Status.BAD_REQUEST));
        }
        return ret;
    }

    /**
     * Bulk update API for all types, changes detected in the type definitions would be persisted
     * @param typesDef A composite object that captures all type definition changes
     * @return A composite object with lists of type definitions that were updated
     * @throws Exception
     */
    @PUT
    @Path("/typedefs")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    @Experimental
    public AtlasTypesDef updateAtlasTypeDefs(final AtlasTypesDef typesDef) throws Exception {
        AtlasTypesDef ret = null;

        try {
            ret = typeDefStore.updateTypesDef(typesDef);
        } catch (AtlasBaseException ex) {
            throw new WebApplicationException(Servlets.getErrorResponse(ex, Response.Status.NOT_MODIFIED));
        }

        return ret;
    }

    /**
     * Bulk delete API for all types
     * @param typesDef A composite object that captures all types to be deleted
     * @throws Exception
     */
    @DELETE
    @Path("/typedefs")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    @Experimental
    public void deleteAtlasTypeDefs(final AtlasTypesDef typesDef) {
        try {
            typeDefStore.deleteTypesDef(typesDef);
        } catch (AtlasBaseException ex) {
            throw new WebApplicationException(Servlets.getErrorResponse(ex, Response.Status.NOT_MODIFIED));
        }
    }

    /**
     * Populate a SearchFilter on the basis of the Query Parameters
     * @return
     */
    private SearchFilter getSearchFilter() {
        SearchFilter ret = new SearchFilter();
        Set<String> keySet = httpServletRequest.getParameterMap().keySet();
        for (String key : keySet) {
            ret.setParam(String.valueOf(key), String.valueOf(httpServletRequest.getParameter(key)));
        }

        return ret;
    }}
