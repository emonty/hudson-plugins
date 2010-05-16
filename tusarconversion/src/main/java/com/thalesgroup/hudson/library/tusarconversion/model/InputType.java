/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
 * Author : Gr�gory Boissinot, Guillaume Tanier                                 *
 *                                                                              *
 * Permission is hereby granted, free of charge, to any person obtaining a copy *
 * of this software and associated documentation files (the "Software"), to deal*
 * in the Software without restriction, including without limitation the rights *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
 * copies of the Software, and to permit persons to whom the Software is        *
 * furnished to do so, subject to the following conditions:                     *
 *                                                                              *
 * The above copyright notice and this permission notice shall be included in   *
 * all copies or substantial portions of the Software.                          *
 *                                                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
 * THE SOFTWARE.                                                                *
 *******************************************************************************/

package com.thalesgroup.hudson.library.tusarconversion.model;

import com.thalesgroup.hudson.library.tusarconversion.Messages;
import org.jvnet.localizer.ResourceBundleHolder;


public class InputType {

    /**
     * The tool type as (coverage, tests, ...)
     */
    private String type;

    /**
     * The tool name
     */
    private String name;

    /**
     * The key label of the tool
     */
    private String keyLabel;

    /**
     * the XSL file associated to this type.
     */
    private String xsl;


    public InputType(String type, String name, String keyLabel, String xsl) {
        this.type = type;
        this.name = name;
        this.keyLabel = keyLabel;
        this.xsl = xsl;
    }

    @SuppressWarnings("unused")
    public String getLabel() {
        return ResourceBundleHolder.get(Messages.class).format(keyLabel);
    }

    public String getType() {
        return type;
    }

    public String getXsl() {
        return xsl;
    }

    public String getName() {
        return name;
    }

}