package org.eclipse.sw360.spdxdocument;

import org.eclipse.sw360.datahandler.thrift.spdxdocument.SPDXDocumentService;
import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.projects.Sw360ThriftServlet;

import java.net.MalformedURLException;

/**
 * Thrift Servlet instantiation
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public class SPDXDocumentServlet extends Sw360ThriftServlet {

    public SPDXDocumentServlet() throws MalformedURLException {
        // Create a service processor using the provided handler
        super(new SPDXDocumentService.Processor<>(new SPDXDocumentHandler()), new TCompactProtocol.Factory());
    }

}
