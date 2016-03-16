/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import org.junit.Test;

public class FlowVarEnricherDataTypePropagatorTestCase extends AbstractVarAssignmentDataTypePropagatorTestCase
{

    public FlowVarEnricherDataTypePropagatorTestCase()
    {
        super(new FlowVarEnricherDataTypePropagator());
    }

    @Test
    public void propagatesVarDataTypeUsingMapSyntax() throws Exception
    {
        doFlowVarAssignmentDataTypePropagationTest(createAssignmentExpression("['" + PROPERTY_NAME + "']"));
    }

    @Test
    public void propagatesVarDataTypeUsingDotSyntax() throws Exception
    {
        doFlowVarAssignmentDataTypePropagationTest(createAssignmentExpression("." + PROPERTY_NAME + ""));
    }

    @Test
    public void propagatesVarDataTypeUsingEscapedDotSyntax() throws Exception
    {
        doFlowVarAssignmentDataTypePropagationTest(createAssignmentExpression(".'" + PROPERTY_NAME + "'"));
    }

    @Test
    public void doesNotChangesVarDataTypeUsingRecursiveMapSyntax() throws Exception
    {
        doFlowVarInnerAssignmentDataTypePropagationTest(createAssignmentExpression("['" + PROPERTY_NAME + "']['" + INNER_PROPERTY_NAME + "']"));
    }

    @Test
    public void doesNotChangesVarDataTypeUsingRecursiveDotSyntax() throws Exception
    {
        doFlowVarInnerAssignmentDataTypePropagationTest(createAssignmentExpression("." + PROPERTY_NAME + "." + INNER_PROPERTY_NAME));
    }

    @Test
    public void doesNotChangesVarDataTypeUsingRecursiveEscapedDotSyntax() throws Exception
    {
        doFlowVarInnerAssignmentDataTypePropagationTest(createAssignmentExpression(".'" + PROPERTY_NAME + "'.'" + INNER_PROPERTY_NAME + "'"));
    }

    private String createAssignmentExpression(String accessorExpression)
    {
        return "flowVars" + accessorExpression + " = 'unused'";
    }
}