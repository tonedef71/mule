/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.metadata;

/**
 * Represents that the given ExecutableId is invalid due that the Executable was not find
 * in the desired flow.
 */
public class InvalidExecutableIdException extends Exception
{

    InvalidExecutableIdException(String message, Exception cause)
    {
        super(message, cause);
    }

    InvalidExecutableIdException(String message)
    {
        super(message);
    }
}
