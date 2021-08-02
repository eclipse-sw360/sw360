namespace java org.eclipse.sw360.datahandler.thrift.spdx.annotations
namespace php sw360.thrift.spdx.annotations

struct Annotations {
    1: optional string annotator,
    2: optional string annotationDate,
    3: optional string annotationType,
    4: optional string spdxRef,
    5: optional string annotationComment,
}