/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.btm.api.internal.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.AddCorrelationIdAction;
import org.hawkular.btm.api.model.config.btxn.LiteralExpression;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AddCorrelationIdActionHandlerTest {

    /**  */
    private static final String TEST_VALUE_1 = "testvalue1";

    @Test
    public void testGlobal() {
        AddCorrelationIdAction action = new AddCorrelationIdAction();
        action.setScope(Scope.Global);
        action.setExpression(new LiteralExpression().setValue(TEST_VALUE_1));

        AddCorrelationIdActionHandler handler = new AddCorrelationIdActionHandler(action);

        handler.init(null);

        Consumer node = new Consumer();

        handler.process(null, node, Direction.In, null, null);

        assertEquals(1, node.getCorrelationIds().size());
        assertEquals(TEST_VALUE_1, node.getCorrelationIds().get(0).getValue());

        assertNull(handler.getIssues());
    }
}
