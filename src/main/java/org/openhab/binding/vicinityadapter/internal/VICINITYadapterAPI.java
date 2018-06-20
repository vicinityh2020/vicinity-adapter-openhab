package org.openhab.binding.vicinityadapter.internal;

import java.util.ArrayList;

/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.TypeParser;
import org.eclipse.smarthome.io.rest.JSONResponse;
import org.eclipse.smarthome.io.rest.LocaleUtil;
import org.eclipse.smarthome.io.rest.RESTResource;
import org.eclipse.smarthome.io.rest.core.config.ConfigurationService;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTOMapper;
import org.openhab.binding.vicinityadapter.internal.vcntobject.VICINITYobject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource to access and control all devices exposed to VICINITY and is registered with the
 * Jersey servlet.
 *
 * @author Christopher Heinz - Initial contribution
 */

@Path(VICINITYadapterAPI.PATH_BINDINGS)
@RolesAllowed({ Role.ADMIN })
@Api(value = VICINITYadapterAPI.PATH_BINDINGS)
@Component(service = { RESTResource.class, VICINITYadapterAPI.class })
public class VICINITYadapterAPI implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_BINDINGS = "objects";

    @Context
    UriInfo uriInfo;

    private final Logger logger = LoggerFactory.getLogger(VICINITYadapterAPI.class);

    private ConfigurationService configurationService;
    private ConfigDescriptionRegistry configDescRegistry;

    @NonNullByDefault({})
    private ItemRegistry itemRegistry;
    @NonNullByDefault({})
    private EventPublisher eventPublisher;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // @ApiOperation(value = "Get all objects.")
    // @ApiResponses(value = {
    // @ApiResponse(code = 200, message = "OK", response = BindingInfoDTO.class, responseContainer = "Set") })
    public Response getAll(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language) {

        final Locale locale = LocaleUtil.getLocale(language);

        // construct response...
        JsonObject response = new JsonObject();

        // add adapter-id...
        response.addProperty("adapter-id", "my-openhab-adapter");

        // get all VICINITY objects. Each Item of the same object, has to be in the same itemgroup.
        // hence all vcnt itemgroups equal all vcnt objects!
        ArrayList<String> vcntGroups = getItemGroups();

        JsonArray td = new JsonArray();
        // TODO: right now, we iterate over all items twice... reduce to one iteration!
        // for "getAll", we need to construct all available VICINITY objects
        for (String group : vcntGroups) {
            ArrayList<Item> groupitems = getAllItemsOfGroup(group);
            VICINITYobject obj = new VICINITYobject(group, groupitems);
            td.add(obj.getTDasJSON());
        }

        response.add("thing-descriptions", td);

        return JSONResponse.createResponse(Status.OK, response, "");

        // return Response.ok(new Stream2JSONInputStream(items.stream())).build();
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]*}/properties/{property: [a-zA-Z_0-9]*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieves the state of an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found"),
            @ApiResponse(code = 400, message = "Item state null") })
    public Response getItemState(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @PathParam("property") @ApiParam(value = "property", required = true) String property) {
        final Locale locale = LocaleUtil.getLocale(language);

        Collection<Item> items = getItems(null, "vicinity," + itemname + "," + property);
        Item item = items.iterator().next();

        // if it exists
        if (item != null) {
            logger.debug("Received HTTP GET request at '{}'.", uriInfo.getPath());

            JsonObject object = new JsonObject();
            object.addProperty("value", item.getState().toString());

            return JSONResponse.createResponse(Status.OK, object, null);
            // return Response.ok(item.getState().toFullString()).build();
        } else {
            logger.info("Received HTTP GET request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            return getItemNotFoundResponse(itemname);
        }
    }

    @PUT
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]*}/properties/{property: [a-zA-Z_0-9]*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates the state of an item.")
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted"),
            @ApiResponse(code = 404, message = "Item not found"),
            @ApiResponse(code = 400, message = "Item state null") })
    public Response putItemState(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @PathParam("property") @ApiParam(value = "property", required = true) String property,
            @ApiParam(value = "valid item state (e.g. ON, OFF)", required = true) JsonObject value) {
        final Locale locale = LocaleUtil.getLocale(language);

        Collection<Item> items = getItems(null, "vicinity," + itemname + "," + property);
        Item item = items.iterator().next();

        String requestvalue = value.get("value").getAsString();

        // if Item exists
        if (item != null) {
            // try to parse a State from the input
            Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), requestvalue);

            if (command != null) {
                // set State and report OK
                logger.debug("Received HTTP PUT request at '{}' with value '{}'.", uriInfo.getPath(), value);
                eventPublisher.post(ItemEventFactory.createCommandEvent(item.getName(), command));
                return getItemResponse(Status.ACCEPTED, null, locale, null);
            } else {
                // State could not be parsed
                logger.warn("Received HTTP PUT request at '{}' with an invalid status value '{}'.", uriInfo.getPath(),
                        value);
                return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "State could not be parsed: " + value);
            }
        } else {
            // Item does not exist
            logger.info("Received HTTP PUT request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            return getItemNotFoundResponse(itemname);
        }
    }

    /**
     * helper: Response to be sent to client if a Thing cannot be found
     *
     * @param thingUID
     * @return Response configured for 'item not found'
     */
    private static Response getItemNotFoundResponse(String itemname) {
        String message = "Item " + itemname + " does not exist!";
        return JSONResponse.createResponse(Status.NOT_FOUND, null, message);
    }

    /**
     * Prepare a response representing the Item depending in the status.
     *
     * @param status
     * @param item can be null
     * @param locale the locale
     * @param errormessage optional message in case of error
     * @return Response configured to represent the Item in depending on the status
     */
    private Response getItemResponse(Status status, @Nullable Item item, Locale locale, @Nullable String errormessage) {
        Object entity = null != item ? EnrichedItemDTOMapper.map(item, true, null, uriInfo.getBaseUri(), locale) : null;
        return JSONResponse.createResponse(status, entity, errormessage);
    }

    /**
     * convenience shortcut
     *
     * @param itemname
     * @return Item addressed by itemname
     */
    private @Nullable Item getItem(String itemname) {
        return itemRegistry.get(itemname);
    }

    private Collection<Item> getItems(@Nullable String type, @Nullable String tags) {
        Collection<Item> items;
        if (tags == null) {
            if (type == null) {
                items = itemRegistry.getItems();
            } else {
                items = itemRegistry.getItemsOfType(type);
            }
        } else {
            String[] tagList = tags.split(",");
            if (type == null) {
                items = itemRegistry.getItemsByTag(tagList);
            } else {
                items = itemRegistry.getItemsByTagAndType(type, tagList);
            }
        }

        return items;
    }

    private ArrayList<String> getItemGroups() {
        ArrayList<String> groups = new ArrayList<String>();

        // collect all vcnt* groups
        for (Item item : itemRegistry.getAll()) {
            for (String group : item.getGroupNames()) {
                if (group.startsWith("vcnt")) {
                    groups.add(group);
                }
            }
        }

        // now we have all available groups, relevant for VICINITY. Remove dublicates
        return new ArrayList<String>(groups.stream().distinct().collect(Collectors.toList()));
    }

    private ArrayList<Item> getAllItemsOfGroup(String groupname) {
        ArrayList<Item> items = new ArrayList<Item>();

        // collect respective items
        for (Item item : itemRegistry.getAll()) {
            if (item.getGroupNames().contains(groupname)) {
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public boolean isSatisfied() {
        return itemRegistry != null;
    }
}
