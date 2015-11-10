/*
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com The software in this package is published
 * under the terms of the CPAL v1.0 license, a copy of which has been included with this distribution in the LICENSE.txt
 * file.
 */

/**
 * TODO <a href="https://www.mulesoft.org/jira/browse/MULE-9049">https://www.mulesoft.org/jira/browse/MULE-9049</a>
 * <p/>
 * Re-re-packaged class from spring's cglib that incorporates PermGen leak fix as in
 * <a href="https://github.com/cglib/cglib/pull/50">https://github.com/cglib/cglib/pull/50</a>. Refer to
 * <a href="https://www.mulesoft.org/jira/browse/MULE-9046">https://www.mulesoft.org/jira/browse/MULE-9046</a> for more
 * details.
 * <p/>
 * This should be removed when a spring version that repackages the cglib version containing this fix is upgraded to.
 */
package org.springframework.cglib;