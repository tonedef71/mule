/*
 * $Id: $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.providers.NullPayload;
import org.mule.routing.inbound.EventGroup;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.RoutingException;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: $
 */
public class AggregationException extends RoutingException
{
    private EventGroup eventGroup = null;

    public AggregationException(EventGroup eventGroup, UMOImmutableEndpoint endpoint) {
        super(new MuleMessage(new NullPayload()), endpoint);
        this.eventGroup = eventGroup;
    }

    public AggregationException(EventGroup eventGroup, UMOImmutableEndpoint endpoint, Throwable cause) {
        super(new MuleMessage(new NullPayload()), endpoint, cause);
        this.eventGroup = eventGroup;
    }

    public AggregationException(Message message, EventGroup eventGroup, UMOImmutableEndpoint endpoint) {
        super(message, new MuleMessage(new NullPayload()), endpoint);
        this.eventGroup = eventGroup;
    }

    public AggregationException(Message message, EventGroup eventGroup, UMOImmutableEndpoint endpoint, Throwable cause) {
        super(message, new MuleMessage(new NullPayload()), endpoint, cause);
        this.eventGroup = eventGroup;
    }

    public EventGroup getEventGroup() {
        return eventGroup;
    }
}
