/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

/*
 * Created on Mar 8, 2007
 */
package io.usethesource.impulse.services;

import io.usethesource.impulse.language.ILanguageService;
import io.usethesource.impulse.parser.IParseController;

public interface IDocumentationProvider extends ILanguageService {
    String getDocumentation(Object target, IParseController parseController);
}
