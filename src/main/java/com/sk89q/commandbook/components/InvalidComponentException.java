/*
 * CommandBook
 * Copyright (C) 2012 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook.components;

/**
 * @author zml2008
 */
public class InvalidComponentException extends Exception {
    private static final long serialVersionUID = 6023653129909836161L;
    private final Class<?> componentClass;
    
    public InvalidComponentException(Class<?> componentClass, String message) {
        super(message);
        this.componentClass = componentClass;
    }
    
    @Override
    public String getMessage() {
        return "Component " + componentClass.getCanonicalName() +
                " could not be loaded due to an error in the structure of the component: "
                + super.getMessage();
    }
}
