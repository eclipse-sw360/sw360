package org.eclipse.sw360.rest.resourceserver.project;

import org.eclipse.sw360.datahandler.thrift.projects.Project;
import java.util.Set;

public class CopyProjectRequest {
    private Set<String> fieldsToCopy;
    private Project overrideFields;
    
    public Set<String> getFieldsToCopy() { return fieldsToCopy; }
    public void setFieldsToCopy(Set<String> fieldsToCopy) { this.fieldsToCopy = fieldsToCopy; }
    public Project getOverrideFields() { return overrideFields; }
    public void setOverrideFields(Project overrideFields) { this.overrideFields = overrideFields; }
}
