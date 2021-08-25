namespace java org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements
namespace php sw360.thrift.spdx.relationshipsbetweenspdxelements

struct RelationshipsBetweenSPDXElements {
    1: optional string spdxElementId,       // 7.1
    2: optional string relationshipType,    // 7.1
    3: optional string relatedSpdxElement,  // 7.1
    4: optional string relationshipComment, // 7.2
}