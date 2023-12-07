package org.example.start.file.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class ConfEnvironmentPostProcessor implements EnvironmentPostProcessor {
    /**
     * 默认的配置文件
     */
    private static final String DEAFULT_PROFILES = "classpath:application.properties";
    /**
     * 是否是相对位置，默认false
     */
    private static final String PROFILES_RELATIVE_POSITION = "spring.profiles.relative.position";
    /**
     * 配置文件的绝对路径
     */
    private static final String PROFILES_CONF_PATH = "spring.profiles.conf.path";
    /**
     * 配置文件的文件名，用逗号分开，只配置application-*.properties 星号代表的部分
     */
    private static final String PROFILES_INCLUDE_FILE = "spring.profiles.include";
    /**
     * 布尔TRUE字符串
     */
    private static final String BOOLEAN_TRUE = "true";
    /**
     * 自定义配置文件位置的路径名
     */
    private static final String CONFIG_PATH = "webapps";
    /**
     * 文件前缀
     */
    private static final String FILE_PREFIX = "application";
    /**
     * 横线
     */
    private static final String FILE_LINE = "-";
    /**
     * 文件后缀
     */
    private static final String FILE_SUFFIX = ".properties";
    /**
     * 英文逗号
     */
    private static final String EN_COMMA = ",";
    /**
     * @Description 加载配置文件方法
     * @Version  1.0
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try{
            File file = ResourceUtils.getFile(DEAFULT_PROFILES);
            Properties properties = loadProperties(file);
            String rootPath = null;
            //相对位置 优先级第一
            if(StringUtils.equalsIgnoreCase(properties.getProperty(PROFILES_RELATIVE_POSITION), BOOLEAN_TRUE)){
                rootPath = getRootPath();
            }

            MutablePropertySources propertySources = environment.getPropertySources();
            File defaultFile = new File(StringUtils.join(rootPath, File.separator, CONFIG_PATH, File.separator, FILE_PREFIX, FILE_SUFFIX));
            if(defaultFile.exists()){
                Properties defaultProperties = loadProperties(defaultFile);
                propertySources.addFirst(new PropertiesPropertySource(StringUtils.join(FILE_PREFIX, FILE_SUFFIX), defaultProperties));
                System.out.println("加载配置文件："+ StringUtils.join(FILE_PREFIX, FILE_SUFFIX));
                if(io.micrometer.common.util.StringUtils.isNotEmpty(defaultProperties.getProperty(PROFILES_INCLUDE_FILE))){
                    properties = defaultProperties;
                }
            }

            String[] list = org.springframework.util.StringUtils.split(properties.getProperty(PROFILES_INCLUDE_FILE), EN_COMMA);
            if(null == list || list.length == 0){
                return;
            }

            for(String fileName : list){
                File f = new File(StringUtils.join(rootPath, File.separator, CONFIG_PATH, File.separator, FILE_PREFIX, FILE_LINE, fileName, FILE_SUFFIX));
                if(f.exists()){
                    propertySources.addFirst(new PropertiesPropertySource(StringUtils.join(FILE_PREFIX, FILE_LINE, fileName, FILE_SUFFIX), loadProperties(f)));
                    System.out.println("加载配置文件："+ StringUtils.join(FILE_PREFIX, FILE_LINE, fileName, FILE_SUFFIX));
                }
            }
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    /**
     * @Description 加载配置文件
     */
    private Properties loadProperties(File f) throws IOException{
        FileSystemResource resource = new FileSystemResource(f);
        return PropertiesLoaderUtils.loadProperties(resource);
    }


    /**
     * @Description 获取当前项目的根路径
     */
    private String getRootPath() throws IOException{
        File rootFile = new File("");
        String rootPath = rootFile.getCanonicalPath();
        return StringUtils.substring(rootPath, 0, StringUtils.lastIndexOf(rootPath, File.separator));
    }
}