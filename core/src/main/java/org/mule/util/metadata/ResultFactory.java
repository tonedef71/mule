/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.metadata;

import org.mule.api.metadata.FailureType;
import org.mule.api.metadata.Result;
import org.mule.extension.api.metadata.MetadataResolvingException;
import org.mule.util.ExceptionUtils;

import java.util.Optional;

public final class ResultFactory
{

    public static <T> Result<T> success(T payload)
    {
        return new Result<T>(){
            @Override
            public T get()
            {
                return payload;
            }

            @Override
            public boolean isSucess()
            {
                return true;
            }

            @Override
            public String getMessage()
            {
                return "Success";
            }

            @Override
            public FailureType getFailureType()
            {
                return FailureType.NONE;
            }

            @Override
            public String getStacktrace()
            {
                return "";
            }
        };
    }

    public static <T> Result<T> failure(Optional<? extends T> payload, String message, Exception e){
        return failure(payload, message, getExceptionFailure(e), Optional.of(e));
    }

    public static <T> Result<T> failure(Optional<? extends T> payload, String message, FailureType failure, Optional<Exception> e)
    {
        return failure(payload, message, failure, e.isPresent() ? ExceptionUtils.getStackTrace(e.get()) : "");
    }

    public static <T> Result<T> failure(Optional<? extends T> payload, String message, FailureType failure, String stackTrace)
    {
        return new Result<T>(){

            @Override
            public T get()
            {
                return payload.isPresent() ? payload.get() : null;
            }

            @Override
            public boolean isSucess()
            {
                return false;
            }

            @Override
            public String getMessage()
            {
                return message;
            }

            @Override
            public FailureType getFailureType()
            {
                return failure;
            }

            @Override
            public String getStacktrace()
            {
                return stackTrace;
            }
        };
    }

    private static FailureType getExceptionFailure(Exception e)
    {
        return e instanceof MetadataResolvingException ? ((MetadataResolvingException)e).getFailure() : FailureType.UNKNOWN;
    }

}
