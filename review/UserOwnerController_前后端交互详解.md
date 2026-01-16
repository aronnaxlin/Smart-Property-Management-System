# UserOwnerController 前后端交互详解

> 针对完全不了解前后端交互的同学准备的详细教程

## 一、什么是前后端交互？

### 1.1 比喻理解
想象一下你去餐厅吃饭：
- **前端（浏览器/网页）** = 服务员：负责和你（用户）直接交流，展示菜单，接收你的点单
- **后端（Java 服务器）** = 厨房：负责真正做菜，处理业务逻辑
- **HTTP 请求** = 点菜单：服务员把你的需求传递给后厨
- **HTTP 响应** = 上菜：后厨把做好的菜通过服务员送到你面前

### 1.2 技术原理
```
用户在浏览器中点击按钮
    ↓
前端发送 HTTP 请求（携带数据）
    ↓
后端 Controller 接收请求
    ↓
调用 Service 层处理业务逻辑
    ↓
Service 调用 DAO 操作数据库
    ↓
数据层层返回
    ↓
Controller 封装成 JSON 格式
    ↓
HTTP 响应返回给前端
    ↓
前端解析数据并展示给用户
```

---

## 二、Controller 的角色

`UserOwnerController` 就是**后厨的接待员**，它的职责是：
1. **监听特定的网址**（URL路径）
2. **接收前端传来的数据**（请求参数）
3. **调用业务逻辑**（Service 层）
4. **返回处理结果**（JSON 格式）

---

## 三、核心注解详解

### 3.1 `@RestController`
```java
@RestController
public class UserOwnerController {
```

**作用**：告诉 Spring 框架："这个类是一个 Web 接口控制器"
- 它会自动把方法的返回值转换成 JSON 格式
- 前端可以通过 HTTP 请求访问这个类中的方法

**类比**：相当于在餐厅门口挂牌子："我们是餐厅，可以点餐"

---

### 3.2 HTTP 方法注解

#### `@GetMapping` —— 查询数据
```java
@GetMapping("/api/user/list")
public Result<List<User>> getAllUsers() {
```

**作用**：监听 `GET` 类型的 HTTP 请求，访问路径为 `/api/user/list`

**前端如何调用**：
```javascript
// 前端 JavaScript 代码
fetch('http://localhost:8080/api/user/list')
  .then(response => response.json())
  .then(data => {
    console.log(data); // 打印返回的用户列表
  });
```

**浏览器地址栏**：
```
http://localhost:8080/api/user/list
```
直接在地址栏访问这个网址，就会触发 `getAllUsers()` 方法！

---

#### `@PostMapping` —— 创建数据
```java
@PostMapping("/api/user/create")
public Result<Long> createUser(@RequestBody User user) {
```

**作用**：监听 `POST` 类型的 HTTP 请求，访问路径为 `/api/user/create`

**前端如何调用**：
```javascript
// 前端 JavaScript 代码
fetch('http://localhost:8080/api/user/create', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    userName: 'zhangsan',
    password: '123456',
    name: '张三',
    userType: 'OWNER'
  })
})
.then(response => response.json())
.then(data => {
  console.log('创建成功，用户ID:', data.data);
});
```

---

#### `@PutMapping` —— 更新数据
```java
@PutMapping("/api/user/update")
public Result<String> updateUser(@RequestBody User user) {
```

**作用**：监听 `PUT` 类型的 HTTP 请求，用于修改现有数据

---

#### `@DeleteMapping` —— 删除数据
```java
@DeleteMapping("/api/user/{id}")
public Result<String> deleteUser(@PathVariable("id") Long id) {
```

**作用**：监听 `DELETE` 类型的 HTTP 请求，用于删除数据

**前端如何调用**：
```javascript
// 删除 ID 为 5 的用户
fetch('http://localhost:8080/api/user/5', {
  method: 'DELETE'
})
.then(response => response.json())
.then(data => {
  console.log(data.message); // "删除成功"
});
```

---

### 3.3 参数接收注解

#### `@PathVariable` —— 从 URL 路径中提取参数
```java
@GetMapping("/api/user/{id}")
public Result<User> getUserById(@PathVariable("id") Long id) {
```

**示例**：
- 前端访问：`http://localhost:8080/api/user/5`
- 后端接收：`id = 5`

**类比**：URL 路径就像门牌号，`{id}` 是动态变化的部分

---

#### `@RequestParam` —— 从 URL 查询参数中提取
```java
@GetMapping("/api/user/search")
public Result<List<User>> searchUsers(
    @RequestParam(value = "keyword", defaultValue = "") String keyword) {
```

**示例**：
- 前端访问：`http://localhost:8080/api/user/search?keyword=张三`
- 后端接收：`keyword = "张三"`

**类比**：查询参数就像附加的备注信息

---

#### `@RequestBody` —— 从请求体中提取 JSON 数据
```java
@PostMapping("/api/user/create")
public Result<Long> createUser(@RequestBody User user) {
```

**前端发送的数据**：
```json
{
  "userName": "zhangsan",
  "password": "123456",
  "name": "张三",
  "phone": "13800138000",
  "userType": "OWNER"
}
```

**后端接收**：Spring 会自动把 JSON 转换成 `User` 对象

---

## 四、完整请求流程示例

### 示例 1：获取所有用户列表

#### 前端代码
```javascript
fetch('http://localhost:8080/api/user/list')
  .then(response => response.json())
  .then(data => {
    console.log(data);
    // 输出：
    // {
    //   "code": 200,
    //   "message": "操作成功",
    //   "data": [
    //     {"userId": 1, "name": "张三", ...},
    //     {"userId": 2, "name": "李四", ...}
    //   ]
    // }
  });
```

#### 后端代码流程
```java
@GetMapping("/api/user/list")
public Result<List<User>> getAllUsers() {
    try {
        // 1. 调用 DAO 层查询数据库
        List<User> users = userDAO.findAll();

        // 2. 封装成功响应
        return Result.success(users != null ? users : List.of());
    } catch (Exception e) {
        // 3. 如果出错，返回错误信息
        return Result.error("获取用户列表失败：" + e.getMessage());
    }
}
```

#### 执行步骤
1. 前端发送 `GET` 请求到 `/api/user/list`
2. Spring 框架接收请求，找到对应的方法 `getAllUsers()`
3. 方法调用 `userDAO.findAll()` 查询数据库
4. 数据库返回用户列表
5. `Result.success()` 将数据封装成标准格式：
   ```json
   {
     "code": 200,
     "message": "操作成功",
     "data": [...]
   }
   ```
6. Spring 自动将 Java 对象转换成 JSON
7. HTTP 响应返回给前端

---

### 示例 2：创建新用户

#### 前端代码
```javascript
// 用户在页面上输入了表单数据
const formData = {
  userName: 'lisi',
  password: 'abc123',
  name: '李四',
  phone: '13900139000',
  gender: '男',
  userType: 'OWNER'
};

// 发送 POST 请求
fetch('http://localhost:8080/api/user/create', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(formData) // 将 JS 对象转换成 JSON 字符串
})
.then(response => response.json())
.then(data => {
  if (data.code === 200) {
    alert('创建成功！用户ID: ' + data.data);
  } else {
    alert('创建失败：' + data.message);
  }
});
```

#### 后端代码流程
```java
@PostMapping("/api/user/create")
public Result<Long> createUser(@RequestBody User user) {
    // 1. 参数校验
    if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
        return Result.error("用户名不能为空");
    }

    try {
        // 2. 检查用户名是否已存在
        User existingUser = userDAO.findByUserName(user.getUserName());
        if (existingUser != null) {
            return Result.error("用户名已存在，请使用其他用户名");
        }

        // 3. 插入新用户到数据库
        Long userId = userDAO.insert(user);

        // 4. 返回成功响应
        if (userId != null && userId > 0) {
            return Result.success(userId);
        }
        return Result.error("创建用户失败");
    } catch (Exception e) {
        return Result.error("创建用户失败：" + e.getMessage());
    }
}
```

#### 执行步骤
1. 前端发送 `POST` 请求，请求体包含 JSON 数据
2. `@RequestBody` 注解将 JSON 自动转换成 `User` 对象
3. 校验参数是否合法
4. 检查用户名是否已存在
5. 调用 `userDAO.insert()` 插入数据库
6. 返回新用户的 ID

---

### 示例 3：搜索业主

#### 前端代码
```javascript
const keyword = '张'; // 用户在搜索框输入的关键词

fetch(`http://localhost:8080/api/owner/search?keyword=${keyword}`)
  .then(response => response.json())
  .then(data => {
    // 渲染搜索结果到页面
    data.data.forEach(owner => {
      console.log(`业主：${owner.name}，房号：${owner.room_no}`);
    });
  });
```

#### 后端代码流程
```java
@GetMapping("/api/owner/search")
public Result<List<Map<String, Object>>> searchOwners(
        @RequestParam(value = "keyword", defaultValue = "") String keyword) {

    // 1. 防御性编程
    if (keyword == null) {
        keyword = "";
    }

    // 2. 安全校验
    if (keyword.length() > 50) {
        return Result.error("搜索词超出长度限制");
    }

    try {
        // 3. 调用业务层搜索
        List<Map<String, Object>> results = ownerService.searchOwners(keyword);

        // 4. 返回结果
        return Result.success(results != null ? results : List.of());
    } catch (Exception e) {
        return Result.error("检索链路异常：" + e.getMessage());
    }
}
```

---

## 五、Result 统一响应格式

所有接口返回的数据都遵循统一的格式：

```java
public class Result<T> {
    private Integer code;    // 状态码（200=成功，其他=失败）
    private String message;  // 提示信息
    private T data;          // 实际数据
}
```

### 成功响应示例
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "userName": "zhangsan",
    "name": "张三"
  }
}
```

### 失败响应示例
```json
{
  "code": 500,
  "message": "用户名已存在，请使用其他用户名",
  "data": null
}
```

**好处**：前端只需要检查 `code` 字段，就知道操作是否成功

---

## 六、完整的前后端交互地图

### 用户管理接口

| 功能 | 请求方法 | URL | 前端传参方式 | 后端返回 |
|------|---------|-----|------------|---------|
| 获取所有用户 | GET | `/api/user/list` | 无 | 用户列表 |
| 获取单个用户 | GET | `/api/user/{id}` | 路径参数 | 用户详情 |
| 搜索用户 | GET | `/api/user/search?keyword=xxx` | 查询参数 | 用户列表 |
| 创建用户 | POST | `/api/user/create` | JSON body | 新用户ID |
| 更新用户 | PUT | `/api/user/update` | JSON body | 成功/失败消息 |
| 删除用户 | DELETE | `/api/user/{id}` | 路径参数 | 成功/失败消息 |

### 业主管理接口

| 功能 | 请求方法 | URL | 前端传参方式 | 后端返回 |
|------|---------|-----|------------|---------|
| 搜索业主 | GET | `/api/owner/search?keyword=xxx` | 查询参数 | 业主+房产列表 |
| 获取业主详情 | GET | `/api/owner/{id}` | 路径参数 | 业主+房产详情 |
| 房产过户 | POST | `/api/owner/property/transfer` | 查询参数 | 成功/失败消息 |

---

## 七、前端调用工具推荐

### 7.1 使用浏览器直接测试（仅 GET 请求）
在浏览器地址栏输入：
```
http://localhost:8080/api/user/list
```

### 7.2 使用 Postman 测试
1. 下载 Postman 软件
2. 新建请求，选择方法（GET/POST/PUT/DELETE）
3. 输入 URL
4. 如果是 POST/PUT，在 Body 中选择 `raw` + `JSON`，输入数据：
   ```json
   {
     "userName": "test",
     "password": "123"
   }
   ```
5. 点击 Send，查看响应

### 7.3 使用 curl 命令测试
```bash
# GET 请求
curl http://localhost:8080/api/user/list

# POST 请求
curl -X POST http://localhost:8080/api/user/create \
  -H "Content-Type: application/json" \
  -d '{"userName":"test","password":"123"}'
```

---

## 八、常见问题答疑

### Q1: 前端如何知道后端的 URL？
**答**：这是前后端开发时需要提前约定的"接口文档"。通常由后端开发人员编写 API 文档，告诉前端：
- URL 是什么
- 需要传什么参数
- 返回什么数据

### Q2: JSON 是什么？
**答**：JSON 是一种数据格式，类似于"标准化的打包方式"。前后端通过 JSON 来传递数据。

JavaScript 对象：
```javascript
{ name: "张三", age: 25 }
```

JSON 字符串（注意：键必须用双引号）：
```json
{ "name": "张三", "age": 25 }
```

### Q3: 为什么要用 HTTP 方法（GET/POST/PUT/DELETE）？
**答**：这是 RESTful API 的设计规范：
- **GET**：查询数据（不改变服务器状态）
- **POST**：创建新数据
- **PUT**：修改现有数据
- **DELETE**：删除数据

### Q4: Spring 如何知道调用哪个方法？
**答**：Spring 会扫描所有带 `@RestController` 的类，记录每个方法的 URL 和 HTTP 方法。当请求到来时，它会自动匹配并调用对应的方法。

### Q5: 为什么有些参数用 `@PathVariable`，有些用 `@RequestParam`？
**答**：
- `@PathVariable`：参数是 URL 路径的一部分，如 `/api/user/5`
- `@RequestParam`：参数在 URL 查询字符串中，如 `/api/user/search?keyword=张三`

---

## 九、调试技巧

### 9.1 查看请求日志
在 `application.properties` 中添加：
```properties
logging.level.org.springframework.web=DEBUG
```

### 9.2 使用浏览器开发者工具
1. 按 F12 打开开发者工具
2. 切换到 `Network` 标签
3. 刷新页面，可以看到所有的 HTTP 请求
4. 点击某个请求，可以查看：
   - 请求头（Headers）
   - 请求参数（Payload）
   - 响应数据（Response）

### 9.3 在代码中添加日志
```java
@GetMapping("/api/user/list")
public Result<List<User>> getAllUsers() {
    System.out.println("收到获取用户列表请求");
    List<User> users = userDAO.findAll();
    System.out.println("查询到 " + users.size() + " 个用户");
    return Result.success(users);
}
```

---

## 十、总结

1. **Controller 是前后端的桥梁**，它监听 HTTP 请求，调用业务逻辑，返回 JSON 数据
2. **注解是关键**：
   - `@RestController` 标记类
   - `@GetMapping` 等标记方法
   - `@PathVariable`、`@RequestParam`、`@RequestBody` 接收参数
3. **HTTP 方法有讲究**：GET 查询、POST 创建、PUT 更新、DELETE 删除
4. **统一响应格式**：所有接口都返回 `Result` 对象，包含 code、message、data
5. **前端通过 fetch、axios 等工具发送请求**，后端 Controller 接收并处理

记住这个核心流程：
```
用户点击按钮 → 前端发送HTTP请求 → Controller接收 → Service处理 → DAO查数据库 → 数据返回 → 前端展示
```
