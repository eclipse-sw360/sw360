/*
 * SPDX-FileCopyrightText: 2026 Eclipse SW360 Contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.migration.poc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Migrated data model class — plain Java POJO replacing the Thrift-generated User struct.
 *
 * <h3>Before (generated from users.thrift):</h3>
 * <pre>
 *   public class User implements TBase&lt;User, User._Fields&gt; {
 *       public enum _Fields implements TFieldIdEnum { ID, EMAIL, DEPARTMENT, ... }
 *       public void read(TProtocol iprot) throws TException { ... }   // binary deserialize
 *       public void write(TProtocol oprot) throws TException { ... }  // binary serialize
 *       public boolean isSet(_Fields field) { ... }
 *   }
 *
 *   // Usage: user.isSet(User._Fields.EMAIL)
 * </pre>
 *
 * <h3>After (this class):</h3>
 * <pre>
 *   // Usage: user.getEmail() != null
 * </pre>
 *
 * All getter/setter names are preserved — zero call-site changes required.
 * Jackson handles JSON serialization; {@code read()}/{@code write(TProtocol)} are gone.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    @JsonProperty("id")
    private String id;

    @JsonProperty("revision")
    private String revision;

    @JsonProperty("type")
    private String type = "user";

    @JsonProperty("email")
    private String email;

    @JsonProperty("fullname")
    private String fullname;

    @JsonProperty("givenname")
    private String givenname;

    @JsonProperty("lastname")
    private String lastname;

    @JsonProperty("department")
    private String department;

    @JsonProperty("userGroup")
    private UserGroup userGroup;

    @JsonProperty("externalid")
    private String externalid;

    public User() {}

    public User(String id, String email, String department) {
        this.id = id;
        this.email = email;
        this.department = department;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRevision() { return revision; }
    public void setRevision(String revision) { this.revision = revision; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getGivenname() { return givenname; }
    public void setGivenname(String givenname) { this.givenname = givenname; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public UserGroup getUserGroup() { return userGroup; }
    public void setUserGroup(UserGroup userGroup) { this.userGroup = userGroup; }

    public String getExternalid() { return externalid; }
    public void setExternalid(String externalid) { this.externalid = externalid; }
}
