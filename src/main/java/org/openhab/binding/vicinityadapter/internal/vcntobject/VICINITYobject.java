package org.openhab.binding.vicinityadapter.internal.vcntobject;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.smarthome.core.items.Item;
import org.openhab.binding.vicinityadapter.internal.VICINITYadapterAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VICINITYobject {

    private final transient Logger logger = LoggerFactory.getLogger(VICINITYadapterAPI.class);

    // as OpenHAB uses the concept of Things (physical Devices) and Items (virtual)
    // one VICINITY Object (physical Device) can have multiple Items (one per property/action/etc.)
    private transient Collection<Item> associatedItems;

    public VICINITYobject(Collection<Item> items) {
        this.associatedItems = items;
    }

    public String generateTD() {

        String name = associatedItems.iterator().next().getGroupNames().get(0);
        Iterator<String> props = associatedItems.iterator().next().getTags().iterator();
        String prop = props.next().toString();

        String td = new String("[" + "{" + "\"type\": \"Lightbulb\"," + "\"oid\": \"" + name + "\","
                + "\"name\": \"my fancy lightbulb\"," + "\"properties\": [" + "{" + "\"pid\": \"" + prop + "\","
                + "\"monitors\": \"Luminance\"," + "\"read_link\": " + "{"
                + "\"href\": \"/object/{oid}/properties/{pid}\"," + "\"output\":" + "{" + "\"type\":" + "\"object\","
                + "\"field\":" + "[" + "{" + "\"name\":" + "\"value\"," + "\"schema\":" + "{" + "\"type\":"
                + "\"integer\"" + "}" + "}" + "]" + "}" + "}" + "}" + "]" + "}" + "]");

        return td;
    }
}
