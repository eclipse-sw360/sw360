package org.eclipse.sw360.spdxdocumentcreationinfo;

import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformationService;
import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.projects.Sw360ThriftServlet;

import java.net.MalformedURLException;

/**
 * Thrift Servlet instantiation
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public class DocumentCreationInformationServlet extends Sw360ThriftServlet {

    public DocumentCreationInformationServlet() throws MalformedURLException {
        // Create a service processor using the provided handler
        super(new DocumentCreationInformationService.Processor<>(new DocumentCreationInformationHandler()), new TCompactProtocol.Factory());
    }

}