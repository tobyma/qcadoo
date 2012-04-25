/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 1.1.5
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.model.integration;

import static com.qcadoo.model.integration.VerifyHooks.HookType.CREATE;

import java.util.Map;

import com.google.common.collect.Maps;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;

public class VerifyHooks {

    public enum HookType {
        SAVE, CREATE, UPDATE, COPY;
    }

    private Map<HookType, Integer> interactions = Maps.newHashMapWithExpectedSize(HookType.values().length);

    public void onSave(final DataDefinition dataDefinition, final Entity entity) {
        noticeInteraction(HookType.SAVE);
    }

    public void onUpdate(final DataDefinition dataDefinition, final Entity entity) {
        noticeInteraction(HookType.UPDATE);
    }

    public void onCopy(final DataDefinition dataDefinition, final Entity entity) {
        noticeInteraction(HookType.COPY);
    }

    public void onCreate(final DataDefinition dataDefinition, final Entity entity) {
        noticeInteraction(CREATE);
    }

    public void clear() {
        interactions = Maps.newHashMapWithExpectedSize(HookType.values().length);
        for (HookType type : HookType.values()) {
            interactions.put(type, 0);
        }
    }

    public int getNumOfInvocations(final HookType type) {
        return interactions.get(type);
    }

    private void noticeInteraction(final HookType type) {
        interactions.put(type, interactions.get(type) + 1);
    }
}