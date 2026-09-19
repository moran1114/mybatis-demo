# ApiFox 基础练习

实验一不开发 Web 接口，只需完成指导书要求的公开 GET 请求练习。

1. 在 ApiFox 新建项目 `mybatis-lab`。
2. 新建 HTTP 请求，方法选择 `GET`。
3. 请求地址填写 `https://jsonplaceholder.typicode.com/todos/1`。
4. 在 Headers 中加入 `Accept: application/json` 和 `X-Lab-Client: apifox`。
5. 发送请求，确认 HTTP 状态为 200，响应 JSON 至少包含 `userId`、`id`、`title`、`completed`。
6. 将请求保存为“实验一-GET基础请求”，并保存为测试用例。
7. 截取请求方法、URL、请求头、状态码和响应体同时可见的画面，放入实验报告。

该练习使用 JSONPlaceholder 的公开只读示例接口。若课堂网络无法访问外网，应按教师提供的可访问公开 GET 地址替换，不能伪造响应截图。
