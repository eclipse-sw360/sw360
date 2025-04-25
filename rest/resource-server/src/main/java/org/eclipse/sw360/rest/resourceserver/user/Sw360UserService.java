package org.eclipse.sw360.rest.resourceserver.user;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.springframework.stereotype.Service;

@Service
public class Sw360UserService {

    private final UserService.Iface sw360UserClient;

    public Sw360UserService(UserService.Iface sw360UserClient) {
        this.sw360UserClient = sw360UserClient;
    }

    public User getUserByEmail(String email) {
        try {
            return sw360UserClient.getByEmail(email);
        } catch (Exception e) {
            // Handle exceptions appropriately
            return null;
        }
    }

    // Implement other methods using available UserService.Iface methods
}
