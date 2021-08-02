package org.eclipse.sw360.spdxpackageinfo;

import org.eclipse.sw360.datahandler.thrift.spdxpackageinfo.PackageInformationService;
import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.projects.Sw360ThriftServlet;

import java.net.MalformedURLException;

/**
 * Thrift Servlet instantiation
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public class PackageInformationServlet extends Sw360ThriftServlet {

    public PackageInformationServlet() throws MalformedURLException {
        // Create a service processor using the provided handler
        super(new PackageInformationService.Processor<>(new PackageInformationHandler()), new TCompactProtocol.Factory());
    }

}