/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.vicinityadapter.internal.vcntobject;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.smarthome.core.items.Item;
import org.openhab.binding.vicinityadapter.internal.VICINITYadapterAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This class acts as a REST resource to access and control all devices exposed to VICINITY and is registered with the
 * Jersey servlet.
 *
 * @author Christopher Heinz - Initial contribution
 */

public class VICINITYobject {

    private final transient Logger logger = LoggerFactory.getLogger(VICINITYadapterAPI.class);

    // as OpenHAB uses the concept of Things (physical Devices) and Items (virtual)
    // one VICINITY Object (physical Device) can have multiple Items (one per property/action/etc.)
    private transient ArrayList<Item> associatedItems;
    private transient String itemName;

    public VICINITYobject(String itemName, ArrayList<Item> items) {
        this.itemName = itemName;
        this.associatedItems = items;
    }

    public JsonObject getTDasJSON() {

        // object information
        JsonObject td = new JsonObject();
        td.addProperty("type", "adapters:Lightbulb");
        td.addProperty("oid", itemName);
        td.addProperty("name", itemName);
        td.addProperty("version", "0.0.1");

        // properties. each associated item represents one property
        JsonArray props = new JsonArray();

        for (Item item : associatedItems) {
            JsonObject prop = new JsonObject();
            prop.addProperty("pid", item.getName());

            Set<String> itemtags = item.getTags();
            // if no tags are set, we have no information on the kind of property. skip it.
            if (itemtags.isEmpty()) {
                continue;
            }

            // as tags are non empty, we can iterate to find a vcnt tag. if more than one, abort
            String propTag = null;
            for (String tag : itemtags) {
                if (tag.startsWith("vcntprop")) {
                    // check if no other property was found so far
                    if (propTag == null) {
                        // throw away the "vcntprop" part and assign property tag
                        propTag = tag.substring(8);
                    } else {
                        // two properties were found on the same item... abort
                        break;
                    }
                }
            }

            // we went through all assigned tags. if we have found one (and have reached this point), thats our property
            prop.addProperty("monitors", propTag);

            // read link:
            prop.add("read_link", constructReadLink());

            // write link:
            prop.add("write_link", constructWriteLink());

            // append to properties
            props.add(prop);
        }

        // add all properties to TD
        td.add("properties", props);
        td.add("actions", new JsonArray());
        td.add("events", new JsonArray());

        return td;
    }

    private JsonObject constructReadLink() {
        // not claiming that this is nice... also it is very much hard-coded for now... just proove of concept and it
        // works :)
        JsonObject readLink = new JsonObject();

        // hard coded read link. always use oid and pid options
        readLink.addProperty("href", "/objects/" + itemName + "/properties/{pid}");

        JsonObject output = new JsonObject();

        JsonArray fields = new JsonArray();

        JsonObject field = new JsonObject();
        field.addProperty("name", "value");

        JsonObject schema = new JsonObject();
        schema.addProperty("units", "percent");
        schema.addProperty("type", "integer");

        field.add("schema", schema);

        fields.add(field);

        output.add("field", fields);

        output.addProperty("type", "object");

        readLink.add("output", output);

        return readLink;
    }

    private JsonObject constructWriteLink() {
        // not claiming that this is nice... also it is very much hard-coded for now... just proove of concept and it
        // works :)
        JsonObject writeLink = new JsonObject();

        // hard coded read link. always use oid and pid options
        writeLink.addProperty("href", "/objects/" + itemName + "/properties/{pid}");

        JsonObject input = new JsonObject();

        JsonArray fields_in = new JsonArray();

        JsonObject field_in = new JsonObject();
        field_in.addProperty("name", "value");

        JsonObject schema_in = new JsonObject();
        schema_in.addProperty("units", "percent");
        schema_in.addProperty("type", "integer");

        field_in.add("schema", schema_in);

        fields_in.add(field_in);

        input.add("field", fields_in);

        input.addProperty("type", "object");

        writeLink.add("input", input);

        JsonObject output = new JsonObject();

        JsonArray fields_out = new JsonArray();

        JsonObject field_out = new JsonObject();
        field_out.addProperty("name", "success");

        JsonObject schema_out = new JsonObject();
        schema_out.addProperty("type", "boolean");

        field_out.add("schema", schema_out);

        fields_out.add(field_out);

        output.add("field", fields_out);

        output.addProperty("type", "object");

        writeLink.add("output", output);

        return writeLink;
    }
}
