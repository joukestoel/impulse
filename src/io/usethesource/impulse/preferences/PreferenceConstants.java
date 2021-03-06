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

package io.usethesource.impulse.preferences;

/**
 * Class that defines the set of preference keys used by the IMP editor. These keys can be
 * used to define/query preference values at any of the levels IMP supports: "instance"
 * (aka workspace), "configuration" (i.e. an Eclipse installation), or "project" (for a
 * project-specific setting). For several of these, it is possible to define a language-
 * specific setting that overrides the global setting. The JavaDoc for each such field
 * indicates that fact.
 * <p>To access these and other preferences, use the IPreferencesService interface and
 * its implementation class, PreferencesService.
 * @see IPreferencesService
 * @see PreferencesService
 * @author rfuhrer@watson.ibm.com
 */
public class PreferenceConstants {
    public static final String P_EMIT_MESSAGES= "emitMessages";

    /**
     * A named preference that controls whether the project explorer's selection is linked to the active editor.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public static final String LINK_EXPLORER_TO_EDITOR= "io.usethesource.impulse.ui.projects.linktoeditor"; //$NON-NLS-1$

    /**
     * A named preference that controls whether bracket matching highlighting is turned on or off.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public static final String EDITOR_MATCHING_BRACKETS= "matchingBrackets"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to highlight matching brackets.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a string 
     * using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     */
    public static final String EDITOR_MATCHING_BRACKETS_COLOR=  "matchingBracketsColor"; //$NON-NLS-1$

    /**
     * A named preference that controls whether builders should emit diagnostics. Can be overridden
     * by a language-specific builder preference of the same key.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public static final String P_EMIT_BUILDER_DIAGNOSTICS= "emitBuilderDiagnostics"; //$NON-NLS-1$
    
    /**
	 * A named preference that controls if correction indicators are shown in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_CORRECTION_INDICATION= "showTemporaryProblem"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether annotation roll over is used or not.
	 * <p>
	 * Value is of type <code>Boolean</code>. If <code>true</code> the annotation ruler column
	 * uses a roll over to display multiple annotations
	 * </p>
	 */
	public static final String EDITOR_ANNOTATION_ROLL_OVER= "editor_annotation_roll_over"; //$NON-NLS-1$

	/**
	 * A named preference that controls if quick assist light bulbs are shown.
	 * <p>
	 * Value is of type <code>Boolean</code>: if <code>true</code> light bulbs are shown
	 * for quick assists.
	 * </p>
	 */
	public static final String EDITOR_QUICKASSIST_LIGHTBULB="org.eclipse.jdt.quickassist.lightbulb"; //$NON-NLS-1$


    private PreferenceConstants() { }
}
