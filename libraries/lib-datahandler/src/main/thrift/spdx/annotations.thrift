namespace java org.eclipse.sw360.datahandler.thrift.spdx.annotations
namespace php sw360.thrift.spdx.annotations

struct Annotations {
    1: optional string annotator,           // 8.1
    2: optional string annotationDate,      // 8.2
    3: optional string annotationType,      // 8.3
    4: optional string spdxIdRef,           // 8.4
    5: optional string annotationComment,   // 8.5
}