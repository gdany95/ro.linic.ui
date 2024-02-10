/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package ro.linic.ui.legacy.session;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages
 */
public class Messages extends NLS
{
	private static final String BUNDLE_NAME = "ro.linic.session.messages"; //$NON-NLS-1$

	public static String AnafReporter_UnauthorizedMessage;

	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages()
	{
	}
}
