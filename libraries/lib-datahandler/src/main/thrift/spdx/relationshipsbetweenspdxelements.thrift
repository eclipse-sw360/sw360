namespace java org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements
namespace php sw360.thrift.spdx.relationshipsbetweenspdxelements

struct RelationshipsBetweenSPDXElements {
    1: optional string spdxElementId,
    2: optional string relationshipType,
    3: optional string relatedSpdxElement,
    4: optional string relationshipComment,
}