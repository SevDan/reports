/*
 * Copyright (c) 2008-2018 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.reports.libintegration;

import com.haulmont.yarg.formatters.factory.inline.DefaultInlinersProvider;

public class CubaInlinersProvider extends DefaultInlinersProvider {

    public CubaInlinersProvider(){
        super();
        addInliner(new FileStorageContentInliner());
    }
}
