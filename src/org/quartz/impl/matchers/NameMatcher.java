/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */
package org.quartz.impl.matchers;

import org.quartz.Matcher;
import org.quartz.utils.Key;

/**
 * Matches on name (ignores group) property of Keys.
 *  
 * @author jhouse
 */
public class NameMatcher<T extends Key> extends StringMatcher<T> {

    protected NameMatcher(String compareTo, StringOperatorName compareWith) {
        super(compareTo, compareWith);
    }
    
    /**
     * Create a NameMatcher that matches names equaling the given string.
     */
    public static NameMatcher nameEquals(String compareTo) {
        return new NameMatcher(compareTo, StringOperatorName.EQUALS);
    }

    /**
     * Create a NameMatcher that matches names starting with the given string.
     */
    public static NameMatcher nameStartsWith(String compareTo) {
        return new NameMatcher(compareTo, StringOperatorName.STARTS_WITH);
    }

    /**
     * Create a NameMatcher that matches names ending with the given string.
     */
    public static NameMatcher nameEndsWith(String compareTo) {
        return new NameMatcher(compareTo, StringOperatorName.ENDS_WITH);
    }

    /**
     * Create a NameMatcher that matches names containing the given string.
     */
    public static NameMatcher nameContains(String compareTo) {
        return new NameMatcher(compareTo, StringOperatorName.CONTAINS);
    }

    @Override
    protected String getValue(T key) {
        return key.getName();
    }

}
