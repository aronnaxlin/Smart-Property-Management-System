# OwnerService 和 OwnerServiceImpl 核心逻辑解析

本文档提供了业主服务接口及其实现类的核心业务逻辑解释。

---

## 文件一：OwnerService.java (接口定义)

**文件路径**: [OwnerService.java](file:///e:/code/NUC-Java-Database-Course-Design/src/main/java/site/aronnax/service/OwnerService.java)

### 第 13 行
```java
public interface OwnerService {
```
**解释**: 声明公共接口 `OwnerService`，定义了业主服务的业务规范。

---

### 第 22 行
```java
    List<Map<String, Object>> searchOwners(String keyword);
```
**解释**: 定义多维度搜索业主的方法。输入关键字（姓名/手机/房号），返回包含业主详情及关联房产信息的列表。

---

### 第 30 行
```java
    Map<String, Object> getOwnerWithProperties(Long userId);
```
**解释**: 根据用户 ID 获取业主基本资料及其名下所有房产的完整视图。

---

### 第 40 行
```java
    boolean updatePropertyOwner(Long propertyId, Long newOwnerId);
```
**解释**: 更新房产归属（过户业务）。修改指定房产关联的用户 ID，并返回操作是否成功。

---

### 第 41 行
```java
}
```
**解释**: 接口定义结束。

---

## 文件二：OwnerServiceImpl.java (实现类)

**文件路径**: [OwnerServiceImpl.java](file:///e:/code/NUC-Java-Database-Course-Design/src/main/java/site/aronnax/service/impl/OwnerServiceImpl.java)

### 第 22-23 行
```java
@Service
public class OwnerServiceImpl implements OwnerService {
```
**解释**: 使用 `@Service` 标记为 Spring 服务组件，并实现 `OwnerService` 接口。

---

### 第 25-26 行
```java
    private final UserDAO userDAO;
    private final PropertyDAO propertyDAO;
```
**解释**: 声明用户（User）和房产（Property）的数据访问对象，使用 `final` 保证依赖不可变。

---

### 第 28-31 行
```java
    public OwnerServiceImpl(UserDAO userDAO, PropertyDAO propertyDAO) {
        this.userDAO = userDAO;
        this.propertyDAO = propertyDAO;
    }
```
**解释**: 通过构造函数注入所需的 DAO 实例。

---

### 第 38-39 行
```java
    public List<Map<String, Object>> searchOwners(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();
```
**解释**: 搜索业主方法实现。初始化结果列表。

---

### 第 42 行
```java
        List<User> users = userDAO.searchByKeyword(keyword);
```
**解释**: 调用 `userDAO` 根据关键词模糊匹配用户。

---

### 第 44 行
```java
        for (User user : users) {
```
**解释**: 遍历匹配到的用户列表。

---

### 第 46 行
```java
            List<Property> properties = propertyDAO.findByUserId(user.getUserId());
```
**解释**: 溯访该用户关联的所有房产档案。

---

### 第 49 行
```java
            for (Property property : properties) {
```
**解释**: 遍历该用户旗下的每一套房产。

---

### 第 50-60 行
```java
                Map<String, Object> ownerInfo = new HashMap<>();
                ownerInfo.put("user_id", user.getUserId());
                ownerInfo.put("name", user.getName());
                ownerInfo.put("phone", user.getPhone());
                ownerInfo.put("gender", user.getGender());
                ownerInfo.put("property_id", property.getpId());
                ownerInfo.put("building_no", property.getBuildingNo());
                ownerInfo.put("unit_no", property.getUnitNo());
                ownerInfo.put("room_no", property.getRoomNo());
                ownerInfo.put("area", property.getArea());
                ownerInfo.put("status", property.getpStatus());
```
**解释**: 封装“业主+房产”的复合信息。

---

### 第 62 行
```java
                results.add(ownerInfo);
```
**解释**: 将封装好的属性对存入结果集。

---

### 第 65 行
```java
        return results;
```
**解释**: 返回最终的搜索结果列表。

---

### 第 72-73 行
```java
    public Map<String, Object> getOwnerWithProperties(Long userId) {
        Map<String, Object> result = new HashMap<>();
```
**解释**: 实现获取业主全景信息。初始化结果映射。

---

### 第 75-76 行
```java
        User owner = userDAO.findById(userId);
        if (owner != null) {
```
**解释**: 查询用户基本信息，并校验用户是否存在。

---

### 第 77-82 行
```java
            result.put("user_id", owner.getUserId());
            result.put("user_name", owner.getUserName());
            result.put("name", owner.getName());
            result.put("phone", owner.getPhone());
            result.put("gender", owner.getGender());
            result.put("user_type", owner.getUserType());
```
**解释**: 填充业主的基本资料。

---

### 第 85-86 行
```java
            List<Property> properties = propertyDAO.findByUserId(userId);
            result.put("properties", properties);
```
**解释**: 拉取该业主的所有房产实体并存入结果。

---

### 第 89 行
```java
        return result;
```
**解释**: 返回包含人员及其名下资产的完整数据。

---

### 第 96-97 行
```java
    public boolean updatePropertyOwner(Long propertyId, Long newOwnerId) {
        Property property = propertyDAO.findById(propertyId);
```
**解释**: 实现房产过户逻辑。首先查询待变更的房产实体。

---

### 第 98-100 行
```java
        if (property == null) {
            return false;
        }
```
**解释**: 若房产不存在则终止操作并返回失败。

---

### 第 103-106 行
```java
        User newOwner = userDAO.findById(newOwnerId)
        if (newOwner == null) {
            return false;
        }
```
**解释**: 校验新业主 ID 的合法性（必须存在于用户表）。

---

### 第 108-109 行
```java
        property.setUserId(newOwnerId);
        return propertyDAO.update(property);
```
**解释**: 修正房产的归属权（UserId），并调用 DAO 持久化变更，返回执行结果。

---

## 总结

该服务层核心逻辑围绕 **"人-房"级联查询**（searchOwners, getOwnerWithProperties）和 **归属权变更**（updatePropertyOwner）展开，有效地将用户数据与资产数据进行了关联维护。
