# Comment Project

## 用户登录——基于Session
为了保证线程安全。用户的信息需要线程安全。这需要用到ThreadLocal：当服务器收到用户发来的请求后，服务器会针对这个请求创建一个线程，
在拦截器(Interceptor)获得cookie之后，它可以用来找到对应的session， 服务器中存有若干session，session中匹配了合适的用户session
(通过用户发过来的Cookie，就是key是JSESSIONID的这个，比如JSESSIONID=19B2BCFF022976521321320D807253FB)。如果是首次访问，服务器
会给客户端发送一个cookie(key 是 JSESSIONID，value 是一串奇怪的东西)，并在服务器自身创建并储存一个session。session中存有该用户的信息(我们的
User实体类)，然后将它储存到ThreadLocal中即可保证线程安全。

ThreadLocal 创建的是线程的私有变量，每个线程都可以独立地改变自己的变量副本而不影响其他线程中的变量副本。一个线程可以创建多个不同的 ThreadLocal，
这些不同的 ThreadLocal 构成的键值对隶属于同一个 ThreadLocalMap。 
每个线程内部包含一个ThreadLocalMap变量，它是一个 HashMap，可以储存多个键值对，每个键值对是<ThreadLocal, Object>类型的键值对， 
键是 ThreadLocal 这个变量自身的引用(this/地址)，值就是不同 ThreadLocal 的值。
每个线程持有自己的 ThreadLocalMap 实例，这个映射表存储了该线程使用的所有 ThreadLocal 对象及其对应的值。
这种设计确保了不同线程的 ThreadLocal 变量互不干扰，实现了线程间的数据隔离。
提示：人类一般将 ThreadLocal 声明为 static，以实现同一个线程内的共享。

一个问题：为什么人类需要使用ThreadLocal，而不直接使用session获取用户信息？
有一种说法是：从线程中取用户信息会导致线程不安全
服务器（如Tomcat、Jetty等）会为每个进入的HTTP请求分配一个线程（从线程池中）来处理，也就是说：一个请求就是一个线程。

## 用户登录——基于Redis
那么这种通过session的办法会带来什么问题？
session共享问题：多台TOMCAT不共享session存储空间，请求切换到不同tomcat服务器会导致数据丢失
一些解决：session拷贝(浪费空间，拷贝延迟可能带来数据的不一致性)
session的替代方案应该满足：①数据共享；②内存存储；③key，value结构。所以需要用Redis

在保存验证码的过程中，替换为Redis后，如何设置key？
服务端只有一个 Redis ，而 session 是每个用户都有一个，如果我们将 key 储存为 "code"，那么有很多 user 的验证码都叫 code，
这会直接发生冲突，因此，每个用户的key需要是不同的。比如：手机号作为 key (tomcat会自动维护和创建session等)，value 就是验证码

随后，也需要保存用户到 Redis，用户对象需要 string --> json 序列化 // 另一个方案是 hash(map)，注意：
公司用的 string 是更多的。当然，hash 会更节省空间， string 会有括号、冒号、逗号等冗余。**本项目采用HASH**
此外，redis如何存储用户信息？答：随机生成一个 token 作为 key。value 再使用 hash 存储每个字段即可。
现在用户发送请求，需要带着 token，所以需要手动把 token 返回给前端(失去了JSSSIONID的自动匹配session)，使得客户端每次
请求应该都携带 token (这个携带 token 的功能由前端完成)

登录拦截器的优化：
如果用户访问的一直是无需登录(不经过拦截器)的页面或者路径，那么TOKEN不会被刷新，那么30分钟后，token会失效，这是不太合理的。
为此，需要一个解决方案：比如，再加一个拦截器，它专门用于刷新 token，保存用户，并放行所有请求；第二个拦截器再做拦截动作(第二个拦截器只做拦截)

```angular2html
Redis 登录流程总结：
①用户输入手机号，点击获取验证码，然后服务器生成验证码，将 手机号 + 验证码 存储在 Redis 中，2 min 内有效
②当用户输入验证码点击登录，(该过程不经过拦截器)，服务器获得验证码，与Redis中的验证码进行比对以验证其有效性，随后根据手机号查询用户信息(如果没有就注册)，
  然后将查询出的信息(脱敏，UserDTO)储存在 Redis 中，key 是 token，value 是UserDTO，30 min 有效
③随后访问需要登录的地方就经过拦截器(当然，更新后所有路径都经过第一个拦截器，它不负责拦截，但可以刷新token的有效期)，验证登录即可。如果访问了需要登录
  的路径，就把 UserDTO 存入 ThreadLocal 以实现线程安全。
```






