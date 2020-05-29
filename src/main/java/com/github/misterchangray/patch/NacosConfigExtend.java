package com.github.misterchangray.patch;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.RandomValuePropertySource;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Component
public class NacosConfigExtend implements ApplicationListener, Ordered {

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ApplicationStartingEvent) {
            System.out.println("[ INFO ] 加载本地[nacos.properties]配置文件, 可以在 [application.properties] 中进行配置或覆盖");
            // 读取本地 nacos 配置
            PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
            propertyPlaceholderConfigurer.setLocations();
            PropertiesFactoryBean f = new PropertiesFactoryBean();
            f.setLocation(new ClassPathResource("nacos.properties"));
            f.setSingleton(false);
            Properties p = null;
            try {
                p = f.getObject();
            } catch (IOException e) {
            }

            ((SpringApplication) applicationEvent.getSource()).setDefaultProperties(p);

        }

        if (applicationEvent instanceof ApplicationEnvironmentPreparedEvent) {
            System.out.println("[ INFO ] 加载远程[nacos.properties]配置文件 ");

            ApplicationEnvironmentPreparedEvent Preparedevent = (ApplicationEnvironmentPreparedEvent) applicationEvent;
            ConfigurableEnvironment configurableEnvironment = Preparedevent.getEnvironment();

            String namespace, addr, dataid, group;

            String patch = configurableEnvironment.getProperty("nacos.config.bootstrap.log-enable-patch");
            if(StringUtils.isEmpty(patch) || false == "true".equals(patch)) {
                System.out.println("[ INFO ] 未打开 [nacos.config.bootstrap.log-enable-patch] 配置, 不支持远程启动日志远程加载");
                return;
            }

            // 读取第一批配置
            namespace = configurableEnvironment.getProperty("nacos.namespace");
            addr  = configurableEnvironment.getProperty("nacos.server-addr");
            dataid  = configurableEnvironment.getProperty("nacos.config.data-id");
            group  = configurableEnvironment.getProperty("nacos.config.group");

            // 读取application.properties 配置
            if(StringUtils.isEmpty(namespace)) namespace = configurableEnvironment.getProperty("nacos.config.namespace");
            if(StringUtils.isEmpty(addr)) namespace = configurableEnvironment.getProperty("nacos.config.server-addr");
            if(StringUtils.isEmpty(dataid)) dataid = configurableEnvironment.getProperty("nacos.config.data-ids");

            // 如果没有配置则默认配置
            if(StringUtils.isEmpty(group)) group = "DEFAULT_GROUP";

            Assert.notNull(dataid, "nacos.config.data-ids 或者 nacos.config.data-id 不能未空");



            Map globalP = new HashMap<>();
            String[] configs = dataid.split(",");
            for(String config : configs) {
                // 读取远程 nacos 相关配置, 刷新本地配置
                Properties res = getRemoteConfig(namespace, addr, config, group);
                addAll(res, globalP);
            }

            MapPropertySource mapPropertySource = new MapPropertySource("defaultProperties", globalP);

            ((ApplicationEnvironmentPreparedEvent) applicationEvent).getEnvironment().getPropertySources().addLast(mapPropertySource);
        }
    }

    private void addAll(Properties res, Map globalP) {
        Set<String> names = res.stringPropertyNames();
        for(String t : names) {
            if(null == globalP.get(t) && t.startsWith("com.xd.log")) {
                globalP.put(t, res.getProperty(t));
            }
        }
    }

    private Properties getRemoteConfig(String space, String addr, String dataid, String group) {
        Properties p = new Properties();
        try {
            String url = "http://%s/nacos/v1/cs/configs?dataId=%s&group=%s&tenant=%s";

            URI uri = new URI(String.format(url, addr, dataid, group, space));
            SimpleClientHttpRequestFactory schr = new SimpleClientHttpRequestFactory();
            schr.setConnectTimeout(3000);
            schr.setReadTimeout(3000);
            ClientHttpRequest chr = schr.createRequest(uri, HttpMethod.GET);
            ClientHttpResponse res = chr.execute();
            InputStream is = res.getBody();
            InputStreamReader isr = new InputStreamReader(is);
            p.load(isr);

        } catch (Exception ae) {

        }
        return p;
    }

    @Override
    public int getOrder() {
        //因为ConfigFileApplicationListener 的顺序是 Ordered.HIGHEST_PRECEDENCE + 10
        //所以想要提前就比之前小1就可以了
        // 再config之后执行
        return  Ordered.HIGHEST_PRECEDENCE + 11;
    }
}
