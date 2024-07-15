package com.example;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * 自动生成mybatisplus的相关代码
 */
public class GeneratorCodeConfig {

    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/springboot-mybatis?serverTimezone=Asia/Shanghai", "root", "abcd@123456")
                //~~~~~~~~~~~~~~~Part1: 全局配置~~~~~~~~~~开始~~~~~~~~~~
                .globalConfig((scanner, builder) -> {
                    builder.author("zhaojh") // 设置作者
                            .enableSwagger() // 开启 swagger 模式
//                            .outputDir(System.getProperty("user.dir") + "/src/main/java")
                            .outputDir("/Users/zhaojh/IdeaProjects/jdk21_projects/maven_projects/springboot-collection-jdk21/springboot-mybatis-plus/src/main/java")
                            .commentDate("yyyy-MM-dd")
                            .disableOpenDir(); //禁止打开文件目录，默认false
                })
                //~~~~~~~~~~~~~~~全局配置~~~~~~~~~~结束~~~~~~~~~~

                //~~~~~~~~~~~~~~~Part2: 包配置~~~~~~~~~~开始~~~~~~~~~~
                .packageConfig((scanner, builder) -> {
                    builder.parent("com.example.mybatis")
                            .moduleName(null)
                            .entity("entity")
                            .service("service")
                            .serviceImpl("service.impl")
                            .mapper("mapper")
                            .xml("mapper.xml")
                            .controller("controller")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper"));
                })
                //~~~~~~~~~~~~~~~包配置~~~~~~~~~~结束~~~~~~~~~~

                //~~~~~~~~~~~~~~~Part3: 策略配置~~~~~~~~~~开始~~~~~~~~~~
                .strategyConfig((scanner, builder) -> builder
                        .addInclude("user") // 设置需要生成的表名
                        .addTablePrefix("t_") // 设置过滤表前缀

                        /**
                         * 实体配置
                         */
                        .entityBuilder()
                        //.superClass(SuperCommomPO.class) // 设置实体类父类-父类中存在的字段不会在实体类中存在
                        .enableLombok()
                        .naming(NamingStrategy.underline_to_camel)// 数据表映射实体命名策略：默认下划线转驼峰underline_to_camel
                        .columnNaming(NamingStrategy.underline_to_camel)// 表字段映射实体属性命名规则：默认null，不指定按照naming执行
                        //.idType(IdType.AUTO)// 添加全局主键类型
                        .formatFileName("%s")// 格式化实体名称，%s取消首字母I,
                        .enableFileOverride()

                        /**
                         * mapper配置
                         */
                        .mapperBuilder()
                        //.enableMapperAnnotation()// 开启mapper注解
                        .enableBaseResultMap()// 启用xml文件中的BaseResultMap 生成
                        //.enableBaseColumnList()// 启用xml文件中的BaseColumnList
                        .formatMapperFileName("%sMapper")// 格式化Mapper类名称
                        .formatXmlFileName("%sMapper")// 格式化xml文件名称
                        .enableFileOverride()

                        /**
                         * service配置
                         */
                        .serviceBuilder()
                        .formatServiceFileName("%sService")// 格式化 service 接口文件名称
                        .formatServiceImplFileName("%sServiceImpl")// 格式化 service 接口文件名称
                        .controllerBuilder()
                        .enableRestStyle()
                        .enableFileOverride()//允许文件覆盖
                        //~~~~~~~~~~~~~~~策略配置~~~~~~~~~~结束~~~~~~~~~~

                        .build())

                // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
