package io.github.odalabasmaz.awsgenie.fetcher.iam;

import java.util.Objects;

public class IAMEntity {
    private String roleName;
    private String entityName;

    IAMEntity(String roleName, String entityName) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IAMEntity iamEntity = (IAMEntity) o;
        return Objects.equals(roleName, iamEntity.roleName) && Objects.equals(entityName, iamEntity.entityName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleName, entityName);
    }
}
