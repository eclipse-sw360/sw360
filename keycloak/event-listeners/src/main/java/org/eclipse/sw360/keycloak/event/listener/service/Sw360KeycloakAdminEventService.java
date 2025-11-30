/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.listener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.keycloak.event.model.Group;
import org.eclipse.sw360.keycloak.event.model.UserEntity;
import org.jboss.logging.Logger;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Sw360KeycloakAdminEventService {
	private static final Logger log = Logger.getLogger(Sw360KeycloakAdminEventService.class);
	private final ObjectMapper objectMapper;
	private final Sw360UserService userService;
	private final KeycloakSession keycloakSession;
	private static final String REALM = "sw360";

	private static final String CUSTOM_ATTR_DEPARTMENT = "Department";
	private static final String CUSTOM_ATTR_EXTERNAL_ID = "externalId";

	public Sw360KeycloakAdminEventService(Sw360UserService sw360UserService, ObjectMapper objectMapper, KeycloakSession keycloakSession) {
		this.objectMapper = objectMapper;
		this.userService = sw360UserService;
		this.keycloakSession = keycloakSession;
	}

	/**
	 * This method is called when an admin changes the group membership of a user
	 * and updates the user group in the SW360 user database
	 *
	 * @param event to be triggered
	 */
	public void groupMembershipOperationAdminEvent(AdminEvent event) {
		log.info("Event Resource path" + event.getResourcePath());
		String resourcePath = event.getResourcePath();
		UserModel userModel = getUserModelFromSession(resourcePath);
		if (userModel.getGroupsStream().count() > 1) {
			throw new RuntimeException("User can not have multiple groups.");
		}
		log.info("Email--->: " + userModel.getEmail());
		if (OperationType.UPDATE.equals(event.getOperationType())) {
			log.info("Group Details:::(Group Membership Event)" + event.getRepresentation());
		} else if (OperationType.CREATE.equals(event.getOperationType())) {
			log.info("Group Details:::(Group Membership Event - CREATE)" + event.getRepresentation());
			Group userGroupModel = null;
			try {
				userGroupModel = objectMapper.readValue(event.getRepresentation(), Group.class);
				String userGroup = userGroupModel.getName();
				User userfromSw360DB = userService.getUserByEmail(userModel.getEmail());
				userfromSw360DB.setUserGroup(ThriftEnumUtils.stringToEnum(userGroup, UserGroup.class));
				userService.updateUser(userfromSw360DB);
			} catch (JsonProcessingException e) {
				log.error("CustomEventListenerSW360::onEvent(_,_)::Json processing error(GROUP)-->" + e);
			} catch (Exception e) {
				log.error("Error updating the user while updating the user group", e);
			}
		} else if (OperationType.DELETE.equals(event.getOperationType())) {
			log.info("Group Details:::(Group Membership Event - DELETE)" + event.getRepresentation());
		}
	}

	private UserModel getUserModelFromSession(String resourcePath) {
		String userId = getUserIdfromResourcePath(resourcePath);
		RealmModel realm = keycloakSession.realms().getRealmByName(REALM);
		return keycloakSession.users().getUserById(realm, userId);
	}

	private String getUserIdfromResourcePath(String resourcePath) {
		int startIndex = resourcePath.indexOf("users/") + "users/".length();
		int endIndex = resourcePath.indexOf("/", startIndex);
		return resourcePath.substring(startIndex, endIndex);
	}

	public void createUserOperation(AdminEvent event) {
		log.debugf("User Details:::(CREATE Event): %s" ,event.getRepresentation());
		try {
			UserEntity userEntity = objectMapper.readValue(event.getRepresentation(), UserEntity.class);
			log.debug("Converted Entity::" + convertEntityToUserThriftObj(userEntity));
			Optional<User> user = Optional.ofNullable(userService.addUser(convertEntityToUserThriftObj(userEntity)));
			user.ifPresentOrElse((u) -> {
				log.infof("Saved User Couchdb Id:: %s" ,u.getId());
			}, () -> {
				log.info("User not saved may be as it returned null!");
			});
		} catch (JsonMappingException e) {
			log.errorf("CustomEventListenerSW360::onEvent(_,_)::Json mapping error: %s" , e);
		} catch (JsonProcessingException e) {
			log.errorf("CustomEventListenerSW360::onEvent(_,_)::Json processing error: %s" , e);
		}
	}

	public void updateUserOperation(AdminEvent event) {
		log.debugf("User Details:::(Update Event): %s" ,event.getRepresentation());
		try {
			UserEntity userEntity = objectMapper.readValue(event.getRepresentation(), UserEntity.class);
			User user = convertEntityToUserThriftObj(userEntity);
			log.debugf("Converted Entity: %s" ,user);
			Optional<RequestStatus> rs;
			try {
				User existingUsrInCouchDB = userService.getUserByEmail(userEntity.getEmail());
				user.setId(existingUsrInCouchDB.getId());
				user.setRevision(existingUsrInCouchDB.getRevision());
				rs = Optional.ofNullable(userService.updateUser(user));
				rs.ifPresentOrElse((u) -> {
					log.debugf("Update Status: %s" ,u);
				}, () -> {
					log.debug("User not UPDATED may be as it returned null status!");
				});
			} catch (Exception e) {
				log.errorf("Something went wrong while updating the user %s", e);
			}
		} catch (JsonMappingException e) {
			log.errorf("CustomEventListenerSW360::onEvent(_,_)::Json mapping error--> %s" , e);
		} catch (JsonProcessingException e) {
			log.errorf("CustomEventListenerSW360::onEvent(_,_)::Json processing error--> %s" + e);
		}
	}

	public void actionUserOperation(AdminEvent event) {
		log.debugf("ActionEvent Triggered", event.getOperationType());
	}

	private User convertEntityToUserThriftObj(UserEntity userEntity) {
		User user = new User();
		user.setEmail(userEntity.getEmail());
		user.setFullname(userEntity.getFirstName() + " " + userEntity.getLastName());
		user.setGivenname(userEntity.getFirstName());
		user.setLastname(userEntity.getLastName());
		setDepartment(userEntity, user);
		setExternalId(userEntity, user);
		setUserGroup(userEntity, user);
		return user;
	}

	private void setUserGroup(UserEntity userEntity, User user) {
		Optional<List<String>> userGroups = Optional.ofNullable(userEntity.getGroups());
		log.debugf("User groups: %s", userGroups.map(List::toString).orElse("[]"));
		userGroups.ifPresent((ug) -> {
			ug.stream().findFirst().ifPresentOrElse((usergroup) -> {
				String groupName = usergroup.replaceFirst("/", "");
				user.setUserGroup(ThriftEnumUtils.stringToEnum(groupName, UserGroup.class));
			}, () -> {
				user.setUserGroup(ThriftEnumUtils.stringToEnum("USER", UserGroup.class));
			});
		});
	}

	private static void setDepartment(UserEntity userEntity, User user) {
		Map<String, List<String>> userAttributes = userEntity.getAttributes();
		Optional.ofNullable(userAttributes.get(CUSTOM_ATTR_DEPARTMENT)).ifPresentOrElse((d) -> {
			String department = d.stream().findFirst().get();
			user.setDepartment(department);
		}, () -> {
			user.setDepartment("Unknown");
		});
	}

	private static void setExternalId(UserEntity userEntity, User user) {
		Map<String, List<String>> userAttributes = userEntity.getAttributes();
		Optional.ofNullable(userAttributes.get(CUSTOM_ATTR_EXTERNAL_ID)).ifPresentOrElse((d) -> {
			String externalId = d.stream().findFirst().get();
			user.setExternalid(externalId);
		}, () -> {
			user.setExternalid("N/A");
		});
	}
}
