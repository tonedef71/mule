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

    public static <T> Result<T> failure(T payload, String message, Exception e){
        return failure(payload, message, getExceptionFailure(e), ExceptionUtils.getStackTrace(e));
    }

    public static <T> Result<T> failure(T payload, String message, FailureType failure, String stackTrace)
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

    public static <T> Result<T> mergeResults(T payload, Result first, Result second)
    {
        if (first.isSucess() && second.isSucess())
        {
            return ResultFactory.success(payload);
        }

        String msg = (first.isSucess() ? "" : "Result Message: " + first.getMessage()) +
                     (second.isSucess() ? "" : "\n\nResult Message: " + second.getMessage());

        String trace = (first.isSucess() ? "" : "Result Exception: " + first.getStacktrace()) +
                       (second.isSucess() ? "" : "\n\nResult Exception: " + second.getStacktrace());

        return ResultFactory.failure(payload, msg, FailureType.UNKNOWN, trace);
    }

    private static FailureType getExceptionFailure(Exception e)
    {
        return e instanceof MetadataResolvingException ? ((MetadataResolvingException)e).getFailure() : FailureType.UNKNOWN;
    }

}
