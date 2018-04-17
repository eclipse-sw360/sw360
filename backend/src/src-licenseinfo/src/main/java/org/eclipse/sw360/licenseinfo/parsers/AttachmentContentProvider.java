/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo.parsers;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.apache.thrift.TException;

/**
 * @author: alex.borodin@evosoft.com
 */
@FunctionalInterface
public interface AttachmentContentProvider {
    AttachmentContent getAttachmentContent(Attachment attachment) throws TException;
}
