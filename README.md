###  springboot-collection-jdk21 项目下的介绍

> 项目当前的开发的环境： 基于jdk 21, springboot 3.2.0、maven的 3.9.4

#### 项目模块

#### 1、commons-server

```
这个是项目的 的公共依赖，包括项目的 统一返回值、异常码等的实体类的包装，以后所有的静态的工具类的风转之类
```

#### 2、mongodb-server

```
这个是基于 spring-boot-starter-data-mongodb的使用实现，使用 mongo 的repository和mongoTemplate 的使用
```

#### 3、redis-server

```
这个是基于spring-boot-starter-data-redis +lettuce的连接池使用方式
```

#### 4、any_position_start_file

```
这个模块是实现了 更改springboot的默认启动文件的位置，解决问题的场景是，项目打成war部署，每次部署不同的现场要修改不同的配置文件，很繁琐，而配置文件 是不需要频繁变动的，所以基于这种方式，实现了配置文件外置，每次部署只需要提供war 不在需要额外更改配置文件
```

