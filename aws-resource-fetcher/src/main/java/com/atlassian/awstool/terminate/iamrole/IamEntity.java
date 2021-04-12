package com.atlassian.awstool.terminate.iamrole;

public class IamEntity {
    private String roleName;
    private String entityName;

    IamEntity(String roleName, String entityName) {
        this.roleName = roleName;
        this.entityName = entityName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String toString() {
        return roleName + "/" + entityName;
    }
}
