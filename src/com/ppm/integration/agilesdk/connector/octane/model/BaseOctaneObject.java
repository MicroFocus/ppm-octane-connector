package com.ppm.integration.agilesdk.connector.octane.model;

import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.provider.UserProvider;
import net.sf.json.JSONObject;

import java.lang.reflect.Method;

public abstract class BaseOctaneObject extends JSonBackedObject {

    public BaseOctaneObject(JSONObject obj) {
        super(obj);
    }

    public String getId() {
        return getString("id");
    }

    public String getName() {
        return getString("name");
    }

    public int getEstimatedHours() {
        return getInt("estimated_hours", 0);
    }

    public int getInvestedHours() {
        return getInt("invested_hours", 0);
    }

    public int getRemainingHours() {
        return getInt("remaining_hours", 0);
    }

    public String getOwnerId() {
        return getString("id", getObj("owner"));
    }

    public String getOwnerName() {
        return getString("name", getObj("owner"));
    }

    public String getOwnerFullName() {
        return getString("full_name", getObj("owner"));
    }

    public String getOwnerEmail() {

        String id = getOwnerId();
        if (id != null && id.contains("@")) {
            return id;
        }
        String name = getString("name", getObj("owner"));
        if (name != null && name.contains("@")) {
            return name;
        }

        return null;
    }

    public long getPPMUserId(UserProvider userProvider, Long projectId) {
        User user =  userProvider.getByEmail(getOwnerEmail());
        if (user == null) {
            user = userProvider.getByUsername(getOwnerName());
        }
        String fullName = getOwnerFullName();
        if (user == null && !org.apache.commons.lang.StringUtils.isBlank(fullName)) {
            // This code is complicated and use reflection because we want it to work on older versions of PPM where
            // UserProvider doesn't have the #getByFullName" method, which was introduced in PPM 23.3
            try {
                Method m = UserProvider.class.getMethod("getByFullName", String.class, Long.class, boolean.class);
                if (m != null) {
                    user = (User) m.invoke(userProvider, fullName.trim(), projectId, false);
                }
            } catch (Exception e) {
                // We do nothing, the method doesn't exist
            }
        }
            // Above reflection code will just call this on PPM 23.3+:
            // user = userProvider.getByFullName(fullName.trim(), projectId, false);

        return (user == null ? -1 : user.getUserId());
    }

}
