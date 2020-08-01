/*
 * CommandBook
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) CommandBook team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook.component.inventory;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

import java.util.Collections;
import java.util.Set;

class InventoryComponentConfiguration extends ConfigurationBase {
    @Setting("item-permissions-only")
    public boolean useItemPermissionsOnly;
    @Setting("allowed-items")
    public Set<String> allowedItems = Collections.emptySet();
    @Setting("disllowed-items")
    public Set<String> disallowedItems = Collections.emptySet();
    @Setting("default-item-stack-size")
    public int defaultItemStackSize = 1;
}
